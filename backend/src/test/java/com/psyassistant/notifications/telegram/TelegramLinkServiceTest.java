package com.psyassistant.notifications.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.common.tenant.TenantContext;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TelegramLinkServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-26T10:00:00Z");

    private final UUID org = UUID.randomUUID();
    private final UUID user = UUID.randomUUID();
    private TelegramLinkTokenRepository tokens;
    private TelegramSettingsRepository settings;
    private TelegramDeliveryLogRepository deliveries;
    private TelegramLinkService service;

    @BeforeEach
    void setUp() {
        tokens = mock(TelegramLinkTokenRepository.class);
        settings = mock(TelegramSettingsRepository.class);
        deliveries = mock(TelegramDeliveryLogRepository.class);
        final TelegramProperties props = new TelegramProperties();
        props.setBotUsername("psy_bot");
        service = new TelegramLinkService(props, tokens, settings, deliveries, Clock.fixed(NOW, ZoneOffset.UTC));
        TenantContext.set(org);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void deepLinkStoresOnlyTheHashOfTheToken() {
        final String url = service.createDeepLink(user);

        final ArgumentCaptor<TelegramLinkToken> saved = ArgumentCaptor.forClass(TelegramLinkToken.class);
        verify(tokens).save(saved.capture());
        final String raw = url.substring(url.indexOf("start=") + 6);
        assertThat(url).startsWith("https://t.me/psy_bot?start=");
        assertThat(saved.getValue().getTokenHash()).isEqualTo(TelegramLinkService.sha256(raw)).isNotEqualTo(raw);
        assertThat(saved.getValue().getUserId()).isEqualTo(user);
        assertThat(saved.getValue().getOrgId()).isEqualTo(org);
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plusSeconds(600));
    }

    @Test
    void redeemBindsChatToTheTokenOwnerOnly() {
        final TelegramLinkToken token = new TelegramLinkToken(org, user, TelegramLinkService.sha256("t"),
                NOW.plusSeconds(60));
        when(tokens.findByTokenHash(TelegramLinkService.sha256("t"))).thenReturn(Optional.of(token));
        when(settings.findByChatIdHash(any())).thenReturn(Optional.empty());
        when(settings.findByUserId(user)).thenReturn(Optional.empty());

        assertThat(service.redeem("t", "555")).isEqualTo(TelegramLinkService.RedeemResult.LINKED);

        final ArgumentCaptor<TherapistNotificationSettings> s = ArgumentCaptor.forClass(
                TherapistNotificationSettings.class);
        verify(settings).save(s.capture());
        assertThat(s.getValue().getUserId()).isEqualTo(user);
        assertThat(s.getValue().getOrgId()).isEqualTo(org);
        assertThat(s.getValue().getChatId()).isEqualTo("555");
        assertThat(token.getUsedAt()).isEqualTo(NOW);
    }

    @Test
    void expiredOrUsedOrUnknownTokenIsRejected() {
        final TelegramLinkToken expired = new TelegramLinkToken(org, user, "h", NOW.minusSeconds(1));
        when(tokens.findByTokenHash(TelegramLinkService.sha256("old"))).thenReturn(Optional.of(expired));
        final TelegramLinkToken used = new TelegramLinkToken(org, user, "h", NOW.plusSeconds(60));
        used.markUsed(NOW.minusSeconds(5));
        when(tokens.findByTokenHash(TelegramLinkService.sha256("used"))).thenReturn(Optional.of(used));
        when(tokens.findByTokenHash(TelegramLinkService.sha256("nope"))).thenReturn(Optional.empty());

        for (final String t : new String[] {"old", "used", "nope"}) {
            assertThat(service.redeem(t, "555")).isEqualTo(TelegramLinkService.RedeemResult.INVALID_TOKEN);
        }
        verify(settings, never()).save(any());
    }

    @Test
    void chatAlreadyLinkedToAnotherUserIsRejected() {
        final TelegramLinkToken token = new TelegramLinkToken(org, user, "h", NOW.plusSeconds(60));
        when(tokens.findByTokenHash(TelegramLinkService.sha256("t"))).thenReturn(Optional.of(token));
        final TherapistNotificationSettings other = new TherapistNotificationSettings(org, UUID.randomUUID());
        when(settings.findByChatIdHash(TelegramLinkService.sha256("555"))).thenReturn(Optional.of(other));

        assertThat(service.redeem("t", "555")).isEqualTo(TelegramLinkService.RedeemResult.CHAT_ALREADY_LINKED);
        verify(settings, never()).save(any());
        assertThat(token.getUsedAt()).isNull();
    }

    @Test
    void unlinkClearsChatAndCancelsPendingMessages() {
        final TherapistNotificationSettings s = new TherapistNotificationSettings(org, user);
        s.link("555", "hash", NOW);
        when(settings.findByUserId(user)).thenReturn(Optional.of(s));

        service.unlink(user);

        assertThat(s.isLinked()).isFalse();
        assertThat(s.getChatIdHash()).isNull();
        verify(deliveries).cancelPendingForUser(user);
    }
}
