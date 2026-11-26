package com.psyassistant.notifications.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.psyassistant.common.tenant.TenantContext;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Proves two organizations never see each other's Telegram settings or outbox rows,
 * and that one chat cannot be linked to two users.
 */
@SpringBootTest
@ActiveProfiles("test")
class TelegramIsolationIntegrationTest {

    @Autowired
    private TelegramSettingsRepository settings;
    @Autowired
    private TelegramDeliveryLogRepository deliveries;
    @Autowired
    private PlatformTransactionManager transactionManager;

    /** The org filter is enabled when a transaction begins, as in the real services. */
    private <T> T inTx(final Supplier<T> work) {
        return new TransactionTemplate(transactionManager).execute(status -> work.get());
    }

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
        settings.deleteAllInBatch();
        deliveries.deleteAllInBatch();
    }

    @Test
    void settingsAndOutboxAreInvisibleAcrossOrganizations() {
        final UUID orgA = UUID.randomUUID();
        final UUID orgB = UUID.randomUUID();
        final UUID userA = UUID.randomUUID();
        final UUID userB = UUID.randomUUID();
        link(orgA, userA, "chat-a");
        link(orgB, userB, "chat-b");
        deliveries.save(new TelegramDeliveryLog(orgA, userA, TelegramDeliveryLog.Kind.DIGEST, "ka", "a"));
        deliveries.save(new TelegramDeliveryLog(orgB, userB, TelegramDeliveryLog.Kind.DIGEST, "kb", "b"));

        TenantContext.set(orgA);
        assertThat(inTx(() -> settings.findAll().stream().map(TherapistNotificationSettings::getUserId).toList()))
                .containsExactly(userA);
        assertThat(inTx(() -> settings.findByUserId(userB))).isEmpty();
        assertThat(inTx(() -> deliveries.findAll().stream().map(TelegramDeliveryLog::getDedupKey).toList()))
                .containsExactly("ka");

        TenantContext.set(orgB);
        assertThat(inTx(() -> settings.findByUserId(userA))).isEmpty();
        assertThat(inTx(() -> deliveries.findAll().stream().map(TelegramDeliveryLog::getDedupKey).toList()))
                .containsExactly("kb");
        assertThat(inTx(() -> settings.findByChatIdHash(TelegramLinkService.sha256("chat-a")))).isEmpty();
    }

    @Test
    void oneChatCannotBeLinkedToTwoUsers() {
        final UUID org = UUID.randomUUID();
        link(org, UUID.randomUUID(), "same-chat");

        assertThatThrownBy(() -> link(org, UUID.randomUUID(), "same-chat"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void link(final UUID org, final UUID user, final String chat) {
        final var s = new TherapistNotificationSettings(org, user);
        s.link(chat, TelegramLinkService.sha256(chat), Instant.now());
        settings.saveAndFlush(s);
    }
}
