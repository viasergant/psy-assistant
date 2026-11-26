package com.psyassistant.notifications.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.common.tenant.TenantContext;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TelegramUpdateHandlerTest {

    private TelegramLinkService linkService;
    private TelegramSettingsRepository settings;
    private ClientTelegramLinkRepository clientLinks;
    private TelegramClient client;
    private TelegramMessageFormatter formatter;
    private ClientSessionCancellationService cancellation;
    private TelegramUpdateHandler handler;

    @BeforeEach
    void setUp() {
        linkService = mock(TelegramLinkService.class);
        settings = mock(TelegramSettingsRepository.class);
        clientLinks = mock(ClientTelegramLinkRepository.class);
        client = mock(TelegramClient.class);
        formatter = mock(TelegramMessageFormatter.class);
        cancellation = mock(ClientSessionCancellationService.class);
        when(settings.findByChatIdHash(anyString())).thenReturn(Optional.empty());
        when(formatter.text(anyString(), any(Locale.class))).thenReturn("reply");
        handler = new TelegramUpdateHandler(linkService, settings, clientLinks, client, formatter,
                cancellation);
    }

    @Test
    void startWithTokenInPrivateChatRedeems() {
        when(linkService.redeem("tok", "1")).thenReturn(TelegramLinkService.RedeemResult.LINKED);

        handler.handle(new TelegramUpdate(1, "1", "private", "/start tok"));

        verify(linkService).redeem("tok", "1");
        verify(formatter).text(eq("telegram.bot.linked"), any(Locale.class));
        verify(client).sendMessage("1", "reply");
    }

    @Test
    void clientStartRepliesWithTheClientMessage() {
        when(linkService.redeem("tok", "1")).thenReturn(TelegramLinkService.RedeemResult.CLIENT_LINKED);

        handler.handle(new TelegramUpdate(1, "1", "private", "/start tok"));

        verify(formatter).text(eq("telegram.bot.client.linked"), any(Locale.class));
    }

    @Test
    void groupChatsNeverLink() {
        handler.handle(new TelegramUpdate(1, "-100", "group", "/start tok"));

        verify(linkService, never()).redeem(anyString(), anyString());
        verify(formatter).text(eq("telegram.bot.private"), any(Locale.class));
    }

    @Test
    void stopUnlinksTheChat() {
        handler.handle(new TelegramUpdate(1, "1", "private", "/stop"));

        verify(linkService).unlinkByChat("1");
    }

    @Test
    void bareStartShowsHelpAndDoesNotRedeem() {
        handler.handle(new TelegramUpdate(1, "1", "private", "/start"));

        verify(linkService, never()).redeem(anyString(), anyString());
        verify(formatter).text(eq("telegram.bot.help"), any(Locale.class));
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private ClientTelegramLink linkedClient(final UUID org) {
        final ClientTelegramLink link = new ClientTelegramLink(org, UUID.randomUUID(), "9",
                TelegramLinkService.sha256("9"), "uk", "UTC", java.time.Instant.EPOCH);
        when(clientLinks.findByChatIdHash(TelegramLinkService.sha256("9"))).thenReturn(Optional.of(link));
        return link;
    }

    @Test
    void cancelButtonAsksForConfirmationInTheClientsTenant() {
        final UUID org = UUID.randomUUID();
        final UUID appointment = UUID.randomUUID();
        final ClientTelegramLink link = linkedClient(org);
        when(cancellation.askConfirmation(link, appointment)).thenAnswer(inv -> {
            assertThat(TenantContext.get()).isEqualTo(org);
            return new ClientSessionCancellationService.Reply("Sure?",
                    ClientSessionCancellationService.Buttons.CONFIRM);
        });

        handler.handleCallback(new TelegramCallback("cb", "9", "private", 5L, "cx:" + appointment));

        verify(client).answerCallbackQuery("cb");
        verify(client).editMessage("9", 5L, "Sure?", List.of(
                new TelegramClient.Button("reply", "cy:" + appointment),
                new TelegramClient.Button("reply", "cn:" + appointment)));
        assertThat(TenantContext.get()).isNull();
    }

    @Test
    void confirmedCancellationRemovesTheButtons() {
        final UUID appointment = UUID.randomUUID();
        final ClientTelegramLink link = linkedClient(UUID.randomUUID());
        when(cancellation.cancel(link, appointment)).thenReturn(
                new ClientSessionCancellationService.Reply("Done", ClientSessionCancellationService.Buttons.NONE));

        handler.handleCallback(new TelegramCallback("cb", "9", "private", 5L, "cy:" + appointment));

        verify(client).editMessage("9", 5L, "Done", List.of());
    }

    @Test
    void declinedConfirmationRestoresTheCancelButton() {
        final UUID appointment = UUID.randomUUID();
        final ClientTelegramLink link = linkedClient(UUID.randomUUID());
        when(cancellation.keep(link, appointment)).thenReturn(
                new ClientSessionCancellationService.Reply("Reminder",
                        ClientSessionCancellationService.Buttons.CANCEL));

        handler.handleCallback(new TelegramCallback("cb", "9", "private", 5L, "cn:" + appointment));

        verify(client).editMessage("9", 5L, "Reminder",
                List.of(new TelegramClient.Button("reply", "cx:" + appointment)));
    }

    @Test
    void buttonsFromUnlinkedChatsGroupsAndGarbagePayloadsAreIgnored() {
        final UUID appointment = UUID.randomUUID();

        handler.handleCallback(new TelegramCallback("cb", "unknown", "private", 5L, "cy:" + appointment));
        linkedClient(UUID.randomUUID());
        handler.handleCallback(new TelegramCallback("cb", "9", "group", 5L, "cy:" + appointment));
        handler.handleCallback(new TelegramCallback("cb", "9", "private", 5L, "cy:not-a-uuid"));
        handler.handleCallback(new TelegramCallback("cb", "9", "private", 5L, "zz:" + appointment));
        handler.handleCallback(new TelegramCallback("cb", "9", "private", 5L, "cy:"));

        verify(client, never()).editMessage(anyString(), anyLong(), anyString(), any());
        verify(cancellation, never()).cancel(any(), any());
    }
}
