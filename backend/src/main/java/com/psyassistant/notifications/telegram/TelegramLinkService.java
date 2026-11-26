package com.psyassistant.notifications.telegram;

import com.psyassistant.common.tenant.TenantContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates one-time link tokens and binds Telegram chats to users.
 *
 * <p>Isolation guarantees: a token is created only for the authenticated caller; redeeming binds the
 * chat to exactly the user/org stored on the token; a chat already linked to someone else is rejected.
 */
@Service
public class TelegramLinkService {

    private static final Logger LOG = LoggerFactory.getLogger(TelegramLinkService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Outcome of a redeem attempt. */
    public enum RedeemResult { LINKED, CLIENT_LINKED, INVALID_TOKEN, CHAT_ALREADY_LINKED }

    private final TelegramProperties props;
    private final TelegramLinkTokenRepository tokens;
    private final TelegramSettingsRepository settings;
    private final TelegramDeliveryLogRepository deliveries;
    private final ClientTelegramLinkRepository clientLinks;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param props      configuration
     * @param tokens     token repository
     * @param settings   settings repository
     * @param deliveries delivery repository
     * @param clientLinks client link repository
     * @param clock      clock
     */
    public TelegramLinkService(final TelegramProperties props, final TelegramLinkTokenRepository tokens,
                               final TelegramSettingsRepository settings,
                               final TelegramDeliveryLogRepository deliveries,
                               final ClientTelegramLinkRepository clientLinks, final Clock clock) {
        this.props = props;
        this.tokens = tokens;
        this.settings = settings;
        this.deliveries = deliveries;
        this.clientLinks = clientLinks;
        this.clock = clock;
    }

    /**
     * Issues a deep link for the current (authenticated) user.
     *
     * @param userId the authenticated user
     * @return {@code https://t.me/<bot>?start=<token>}
     */
    @Transactional
    public String createDeepLink(final UUID userId) {
        final UUID orgId = TenantContext.require();
        final String token = newToken();
        tokens.save(new TelegramLinkToken(orgId, userId, sha256(token),
                clock.instant().plus(Duration.ofMinutes(props.getLinkTokenTtlMinutes()))));
        return "https://t.me/" + props.getBotUsername() + "?start=" + token;
    }

    /**
     * Issues a deep link that binds a Telegram chat to a client. The caller must already have
     * verified (through an org-filtered load) that the client is visible to the authenticated user.
     *
     * @param clientId the client to invite
     * @param language en or uk, used for bot replies to the client
     * @param timezone IANA zone used to render session times for the client
     * @return {@code https://t.me/<bot>?start=<token>}
     */
    @Transactional
    public String createClientDeepLink(final UUID clientId, final String language, final String timezone) {
        final UUID orgId = TenantContext.require();
        final String token = newToken();
        tokens.save(TelegramLinkToken.forClient(orgId, clientId, language, timezone, sha256(token),
                clock.instant().plus(Duration.ofMinutes(props.getLinkTokenTtlMinutes()))));
        return "https://t.me/" + props.getClientBot().getBotUsername() + "?start=" + token;
    }

    /**
     * Redeems a therapist token received via {@code /start} on the therapist bot. Client tokens are
     * rejected here. Runs outside any HTTP request, so it binds the tenant from the token itself.
     *
     * @param rawToken token from the deep link
     * @param chatId   private chat id
     * @return the outcome
     */
    @Transactional
    public RedeemResult redeem(final String rawToken, final String chatId) {
        final Optional<TelegramLinkToken> found = tokens.findByTokenHash(sha256(rawToken));
        final Instant now = clock.instant();
        if (found.isEmpty() || !found.get().isRedeemable(now) || found.get().getClientId() != null) {
            return RedeemResult.INVALID_TOKEN;
        }
        final TelegramLinkToken token = found.get();
        final String chatHash = sha256(chatId);
        final Optional<TherapistNotificationSettings> owner = settings.findByChatIdHash(chatHash);
        if (owner.isPresent() && !owner.get().getUserId().equals(token.getUserId())) {
            return RedeemResult.CHAT_ALREADY_LINKED;
        }
        final TherapistNotificationSettings s = settings.findByUserId(token.getUserId())
                .orElseGet(() -> new TherapistNotificationSettings(token.getOrgId(), token.getUserId()));
        s.link(chatId, chatHash, now);
        settings.save(s);
        token.markUsed(now);
        tokens.save(token);
        LOG.info("Telegram linked for user {}", token.getUserId());
        return RedeemResult.LINKED;
    }

    /**
     * Redeems a client invite token received via {@code /start} on the client bot. Therapist tokens are
     * rejected here, so the two bots can never be used to link the other audience. Chat ids are only
     * unique per bot, so a person who is both a therapist and a client may use both bots.
     *
     * @param rawToken token from the deep link
     * @param chatId   private chat id
     * @return the outcome
     */
    @Transactional
    public RedeemResult redeemClient(final String rawToken, final String chatId) {
        final Optional<TelegramLinkToken> found = tokens.findByTokenHash(sha256(rawToken));
        final Instant now = clock.instant();
        if (found.isEmpty() || !found.get().isRedeemable(now) || found.get().getClientId() == null) {
            return RedeemResult.INVALID_TOKEN;
        }
        final TelegramLinkToken token = found.get();
        final String chatHash = sha256(chatId);
        final Optional<ClientTelegramLink> owner = clientLinks.findByChatIdHash(chatHash);
        if (owner.isPresent() && !owner.get().getClientId().equals(token.getClientId())) {
            return RedeemResult.CHAT_ALREADY_LINKED;
        }
        final Optional<ClientTelegramLink> existing = clientLinks.findByClientId(token.getClientId());
        if (existing.isPresent()) {
            existing.get().rebind(chatId, chatHash, token.getLanguage(), token.getTimezone(), now);
            clientLinks.save(existing.get());
        } else {
            clientLinks.save(new ClientTelegramLink(token.getOrgId(), token.getClientId(), chatId, chatHash,
                    token.getLanguage(), token.getTimezone(), now));
        }
        token.markUsed(now);
        tokens.save(token);
        LOG.info("Telegram linked for client {}", token.getClientId());
        return RedeemResult.CLIENT_LINKED;
    }

    /**
     * Unlinks a client's Telegram chat.
     *
     * @param clientId client to unlink
     */
    @Transactional
    public void unlinkClient(final UUID clientId) {
        clientLinks.findByClientId(clientId).ifPresent(clientLinks::delete);
        deliveries.cancelPendingForClient(clientId);
        LOG.info("Telegram unlinked for client {}", clientId);
    }

    /**
     * Returns whether the client has a linked chat.
     *
     * @param clientId client id
     * @return true when linked
     */
    @Transactional(readOnly = true)
    public boolean isClientLinked(final UUID clientId) {
        return clientLinks.findByClientId(clientId).isPresent();
    }

    private static String newToken() {
        final byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    /**
     * Unlinks the user and cancels their undelivered messages.
     *
     * @param userId user to unlink
     */
    @Transactional
    public void unlink(final UUID userId) {
        settings.findByUserId(userId).ifPresent(s -> {
            s.unlink();
            settings.save(s);
        });
        deliveries.cancelPendingForUser(userId);
        LOG.info("Telegram unlinked for user {}", userId);
    }

    /**
     * Unlinks a therapist by chat (the {@code /stop} command on the therapist bot).
     *
     * @param chatId the chat id
     */
    @Transactional
    public void unlinkByChat(final String chatId) {
        settings.findByChatIdHash(sha256(chatId)).ifPresent(s -> unlink(s.getUserId()));
    }

    /**
     * Unlinks a client by chat (the {@code /stop} command on the client bot).
     *
     * @param chatId the chat id
     */
    @Transactional
    public void unlinkClientByChat(final String chatId) {
        clientLinks.findByChatIdHash(sha256(chatId)).ifPresent(l -> unlinkClient(l.getClientId()));
    }

    /**
     * Returns the SHA-256 hex of a value.
     *
     * @param value input
     * @return lowercase hex digest
     */
    public static String sha256(final String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
