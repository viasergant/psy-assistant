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
    public enum RedeemResult { LINKED, INVALID_TOKEN, CHAT_ALREADY_LINKED }

    private final TelegramProperties props;
    private final TelegramLinkTokenRepository tokens;
    private final TelegramSettingsRepository settings;
    private final TelegramDeliveryLogRepository deliveries;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param props      configuration
     * @param tokens     token repository
     * @param settings   settings repository
     * @param deliveries delivery repository
     * @param clock      clock
     */
    public TelegramLinkService(final TelegramProperties props, final TelegramLinkTokenRepository tokens,
                               final TelegramSettingsRepository settings,
                               final TelegramDeliveryLogRepository deliveries, final Clock clock) {
        this.props = props;
        this.tokens = tokens;
        this.settings = settings;
        this.deliveries = deliveries;
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
        final byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        final String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        tokens.save(new TelegramLinkToken(orgId, userId, sha256(token),
                clock.instant().plus(Duration.ofMinutes(props.getLinkTokenTtlMinutes()))));
        return "https://t.me/" + props.getBotUsername() + "?start=" + token;
    }

    /**
     * Redeems a token received via {@code /start}. Runs outside any HTTP request, so it binds the
     * tenant from the token itself.
     *
     * @param rawToken token from the deep link
     * @param chatId   private chat id
     * @return the outcome
     */
    @Transactional
    public RedeemResult redeem(final String rawToken, final String chatId) {
        final Optional<TelegramLinkToken> found = tokens.findByTokenHash(sha256(rawToken));
        final Instant now = clock.instant();
        if (found.isEmpty() || !found.get().isRedeemable(now)) {
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
     * Unlinks by chat (the {@code /stop} command or a blocked bot).
     *
     * @param chatId the chat id
     */
    @Transactional
    public void unlinkByChat(final String chatId) {
        settings.findByChatIdHash(sha256(chatId)).ifPresent(s -> unlink(s.getUserId()));
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
