package com.psyassistant.notifications.telegram;

import com.psyassistant.common.tenant.TenantContext;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Handles everything sent to the <strong>client</strong> bot: linking by invite token, {@code /stop}, and the
 * session cancel buttons. This bot has its own token, so a client chat can never reach therapist functions.
 *
 * <p>Button payloads only name an appointment; the client and organization always come from the stored
 * link of the chat the press arrived in.
 */
@Component
public class ClientBotUpdateHandler implements TelegramUpdateSink {

    /** Callback prefixes; the payload is {@code <prefix><appointment id>} (well under Telegram's 64 bytes). */
    static final String CANCEL_ASK = "cx:";
    private static final String CANCEL_YES = "cy:";
    private static final String CANCEL_NO = "cn:";
    private static final Logger LOG = LoggerFactory.getLogger(ClientBotUpdateHandler.class);
    private static final String PRIVATE = "private";

    private final TelegramLinkService linkService;
    private final ClientTelegramLinkRepository clientLinks;
    private final TelegramClient client;
    private final TelegramMessageFormatter formatter;
    private final ClientSessionCancellationService cancellation;

    /**
     * Creates the handler.
     *
     * @param linkService  link service
     * @param clientLinks  client link repository
     * @param client       client bot API
     * @param formatter    text source
     * @param cancellation client self-service cancellation
     */
    public ClientBotUpdateHandler(final TelegramLinkService linkService,
                                  final ClientTelegramLinkRepository clientLinks,
                                  @Qualifier(TelegramConfig.CLIENT_BOT) final TelegramClient client,
                                  final TelegramMessageFormatter formatter,
                                  final ClientSessionCancellationService cancellation) {
        this.linkService = linkService;
        this.clientLinks = clientLinks;
        this.client = client;
        this.formatter = formatter;
        this.cancellation = cancellation;
    }

    @Override
    public void handle(final TelegramUpdate update) {
        final String text = update.text().trim();
        if (!PRIVATE.equals(update.chatType())) {
            reply(update.chatId(), "telegram.bot.private", Locale.ENGLISH);
        } else if (text.equals("/stop")) {
            final Locale locale = localeOf(update.chatId());
            linkService.unlinkClientByChat(update.chatId());
            reply(update.chatId(), "telegram.bot.stopped", locale);
        } else if (text.startsWith("/start ") && text.length() > "/start ".length()) {
            redeem(update.chatId(), text.substring("/start ".length()).trim());
        } else {
            reply(update.chatId(), "telegram.client.bot.help", localeOf(update.chatId()));
        }
    }

    @Override
    public void handleCallback(final TelegramCallback callback) {
        try {
            client.answerCallbackQuery(callback.callbackId());
        } catch (TelegramApiException e) {
            LOG.warn("Telegram callback ack failed: {}", e.getMessage());
        }
        if (!PRIVATE.equals(callback.chatType())) {
            return;
        }
        final Optional<ClientTelegramLink> link =
                clientLinks.findByChatIdHash(TelegramLinkService.sha256(callback.chatId()));
        final String data = callback.data();
        if (link.isEmpty() || data.length() < CANCEL_ASK.length() + 1) {
            return;
        }
        final String action = data.substring(0, CANCEL_ASK.length());
        final UUID appointmentId;
        try {
            appointmentId = UUID.fromString(data.substring(CANCEL_ASK.length()));
        } catch (IllegalArgumentException e) {
            return;
        }
        final ClientTelegramLink l = link.get();
        final ClientSessionCancellationService.Reply reply = switch (action) {
            case CANCEL_ASK -> inTenant(l, () -> cancellation.askConfirmation(l, appointmentId));
            case CANCEL_YES -> inTenant(l, () -> cancellation.cancel(l, appointmentId));
            case CANCEL_NO -> inTenant(l, () -> cancellation.keep(l, appointmentId));
            default -> null;
        };
        if (reply != null) {
            edit(callback, l, reply, appointmentId);
        }
    }

    private void redeem(final String chatId, final String token) {
        final TelegramLinkService.RedeemResult result = linkService.redeemClient(token, chatId);
        final String key = switch (result) {
            case CLIENT_LINKED, LINKED -> "telegram.bot.client.linked";
            case CHAT_ALREADY_LINKED -> "telegram.client.bot.already";
            case INVALID_TOKEN -> "telegram.client.bot.invalid";
        };
        reply(chatId, key, localeOf(chatId));
    }

    private static <T> T inTenant(final ClientTelegramLink link, final Supplier<T> work) {
        final UUID previous = TenantContext.get();
        TenantContext.set(link.getOrgId());
        try {
            return work.get();
        } finally {
            if (previous == null) {
                TenantContext.clear();
            } else {
                TenantContext.set(previous);
            }
        }
    }

    private void edit(final TelegramCallback callback, final ClientTelegramLink link,
                      final ClientSessionCancellationService.Reply reply, final UUID appointmentId) {
        final Locale locale = Locale.forLanguageTag(link.getLanguage());
        final List<TelegramClient.Button> buttons = switch (reply.buttons()) {
            case NONE -> List.of();
            case CANCEL -> List.of(new TelegramClient.Button(
                    formatter.text("telegram.client.btn.cancel", locale), CANCEL_ASK + appointmentId));
            case CONFIRM -> List.of(
                    new TelegramClient.Button(formatter.text("telegram.client.btn.yes", locale),
                            CANCEL_YES + appointmentId),
                    new TelegramClient.Button(formatter.text("telegram.client.btn.no", locale),
                            CANCEL_NO + appointmentId));
        };
        try {
            client.editMessage(callback.chatId(), callback.messageId(), reply.text(), buttons);
        } catch (TelegramApiException e) {
            LOG.warn("Telegram message edit failed: {}", e.getMessage());
        }
    }

    private Locale localeOf(final String chatId) {
        return clientLinks.findByChatIdHash(TelegramLinkService.sha256(chatId))
                .map(c -> Locale.forLanguageTag(c.getLanguage()))
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
