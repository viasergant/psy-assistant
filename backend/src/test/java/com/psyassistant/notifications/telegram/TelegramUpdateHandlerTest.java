package com.psyassistant.notifications.telegram;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TelegramUpdateHandlerTest {

    private TelegramLinkService linkService;
    private TelegramSettingsRepository settings;
    private TelegramClient client;
    private TelegramMessageFormatter formatter;
    private TelegramUpdateHandler handler;

    @BeforeEach
    void setUp() {
        linkService = mock(TelegramLinkService.class);
        settings = mock(TelegramSettingsRepository.class);
        client = mock(TelegramClient.class);
        formatter = mock(TelegramMessageFormatter.class);
        when(settings.findByChatIdHash(anyString())).thenReturn(Optional.empty());
        when(formatter.text(anyString(), any(Locale.class))).thenReturn("reply");
        handler = new TelegramUpdateHandler(linkService, settings, client, formatter);
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
    void aClientInviteTokenIsReportedAsInvalidOnTheTherapistBot() {
        when(linkService.redeem("tok", "1")).thenReturn(TelegramLinkService.RedeemResult.INVALID_TOKEN);

        handler.handle(new TelegramUpdate(1, "1", "private", "/start tok"));

        verify(formatter).text(eq("telegram.bot.invalid"), any(Locale.class));
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

    @Test
    void buttonPressesAreIgnoredOnTheTherapistBot() {
        handler.handleCallback(new TelegramCallback("cb", "1", "private", 5L, "cy:x"));

        verifyNoInteractions(client);
    }
}
