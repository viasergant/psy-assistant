package com.psyassistant.notifications.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.notifications.telegram.TelegramDeliveryLog.Kind;
import com.psyassistant.notifications.telegram.TelegramDeliveryLog.Status;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TelegramOutboxProcessorTest {

    private static final Instant NOW = Instant.parse("2026-11-26T10:00:00Z");

    private final UUID org = UUID.randomUUID();
    private final UUID user = UUID.randomUUID();
    private TelegramDeliveryLogRepository deliveries;
    private TelegramSettingsRepository settings;
    private TelegramClient client;
    private TelegramLinkService linkService;
    private TelegramOutboxProcessor processor;
    private TelegramDeliveryLog row;

    @BeforeEach
    void setUp() {
        deliveries = mock(TelegramDeliveryLogRepository.class);
        settings = mock(TelegramSettingsRepository.class);
        client = mock(TelegramClient.class);
        linkService = mock(TelegramLinkService.class);
        final TelegramProperties props = new TelegramProperties();
        processor = new TelegramOutboxProcessor(deliveries, settings, client, linkService, props,
                Clock.fixed(NOW, ZoneOffset.UTC));
        row = new TelegramDeliveryLog(org, user, Kind.REMINDER, "k", "hello");
        when(deliveries.findNextBatch(any(), anyInt())).thenReturn(List.of(row));
        final TherapistNotificationSettings s = new TherapistNotificationSettings(org, user);
        s.link("555", "hash", NOW);
        when(settings.findByUserId(user)).thenReturn(Optional.of(s));
    }

    @Test
    void sendsToTheOwnersChatAndMarksSent() {
        processor.processBatch();

        verify(client).sendMessage("555", "hello");
        assertThat(row.getStatus()).isEqualTo(Status.SENT);
        assertThat(row.getSentAt()).isEqualTo(NOW);
    }

    @Test
    void rowOfAnotherOrgIsCancelledNotSent() {
        final var foreign = new TherapistNotificationSettings(UUID.randomUUID(), user);
        foreign.link("999", "h", NOW);
        when(settings.findByUserId(user)).thenReturn(Optional.of(foreign));

        processor.processBatch();

        verify(client, never()).sendMessage(anyString(), anyString());
        assertThat(row.getStatus()).isEqualTo(Status.CANCELLED);
    }

    @Test
    void unlinkedUserIsCancelled() {
        when(settings.findByUserId(user)).thenReturn(Optional.of(new TherapistNotificationSettings(org, user)));

        processor.processBatch();

        assertThat(row.getStatus()).isEqualTo(Status.CANCELLED);
    }

    @Test
    void rateLimitHonoursRetryAfter() {
        doThrow(new TelegramApiException("429", 429, 30)).when(client).sendMessage(anyString(), anyString());

        processor.processBatch();

        assertThat(row.getStatus()).isEqualTo(Status.RETRY);
        assertThat(row.getNextRetryAt()).isEqualTo(NOW.plusSeconds(30));
        assertThat(row.getRetryCount()).isEqualTo(1);
    }

    @Test
    void blockedBotFailsAndUnlinksTheUser() {
        doThrow(new TelegramApiException("403 blocked", 403, 0)).when(client).sendMessage(anyString(), anyString());

        processor.processBatch();

        assertThat(row.getStatus()).isEqualTo(Status.FAILED);
        verify(linkService).unlink(user);
    }

    @Test
    void givesUpAfterMaxRetries() {
        doThrow(new TelegramApiException("500", 500, 0)).when(client).sendMessage(anyString(), anyString());
        row.setRetryCount(4);

        processor.processBatch();

        assertThat(row.getStatus()).isEqualTo(Status.FAILED);
    }

    @Test
    void permanentClientErrorIsNotRetried() {
        doThrow(new TelegramApiException("400 bad request", 400, 0)).when(client).sendMessage(anyString(), anyString());

        processor.processBatch();

        assertThat(row.getStatus()).isEqualTo(Status.FAILED);
        verify(linkService, never()).unlink(any());
    }
}
