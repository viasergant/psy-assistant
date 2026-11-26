package com.psyassistant.notifications.telegram;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handles bot commands from Telegram. Shared by the webhook and polling modes.
 *
 * <p>This class only links/unlinks chats and replies with static text; it never reads appointment
 * or client data.
 */
@Component
public class TelegramUpdateHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TelegramUpdateHandler.class);
    private static final String PRIVATE = "private";

    private final TelegramLinkService linkService;
    private final TelegramSettingsRepository settings;
    private final TelegramClient client;
    private final TelegramMessageFormatter formatter;

    /**
     * Creates the handler.
     *
     * @param linkService link service
     * @param settings    settings repository
     * @param client      Telegram API
     * @param formatter   text source
     */
    public TelegramUpdateHandler(final TelegramLinkService linkService, final TelegramSettingsRepository settings,
                                 final TelegramClient client, final TelegramMessageFormatter formatter) {
        this.linkService = linkService;
        this.settings = settings;
        this.client = client;
        this.formatter = formatter;
    }

    /**
     * Processes one update.
     *
     * @param update parsed update
     */
    public void handle(final TelegramUpdate update) {
        final String text = update.text().trim();
        if (!PRIVATE.equals(update.chatType())) {
            reply(update.chatId(), "telegram.bot.private", Locale.ENGLISH);
            return;
        }
        if (text.equals("/stop")) {
            final Locale locale = localeOf(update.chatId());
            linkService.unlinkByChat(update.chatId());
            reply(update.chatId(), "telegram.bot.stopped", locale);
        } else if (text.startsWith("/start ") && text.length() > "/start ".length()) {
            redeem(update.chatId(), text.substring("/start ".length()).trim());
        } else {
            reply(update.chatId(), "telegram.bot.help", localeOf(update.chatId()));
        }
    }

    private void redeem(final String chatId, final String token) {
        final TelegramLinkService.RedeemResult result = linkService.redeem(token, chatId);
        final String key = switch (result) {
            case LINKED -> "telegram.bot.linked";
            case CHAT_ALREADY_LINKED -> "telegram.bot.already";
            case INVALID_TOKEN -> "telegram.bot.invalid";
        };
        reply(chatId, key, localeOf(chatId));
    }

    private Locale localeOf(final String chatId) {
        return settings.findByChatIdHash(TelegramLinkService.sha256(chatId))
                .map(s -> Locale.forLanguageTag(s.getLanguage()))
                .orElse(Locale.ENGLISH);
    }

    private void reply(final String chatId, final String key, final Locale locale) {
        try {
            client.sendMessage(chatId, formatter.text(key, locale));
        } catch (TelegramApiException e) {
            LOG.warn("Telegram reply failed: {}", e.getMessage());
        }
    }
}
