package com.psyassistant.notifications.telegram;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link TherapistNotificationSettings}.
 */
@Repository
public interface TelegramSettingsRepository extends JpaRepository<TherapistNotificationSettings, UUID> {

    Optional<TherapistNotificationSettings> findByUserId(UUID userId);

    Optional<TherapistNotificationSettings> findByChatIdHash(String chatIdHash);

    /**
     * Cross-organization scan used only by the scheduler, which then binds the tenant per row.
     *
     * @return all settings with a linked chat
     */
    List<TherapistNotificationSettings> findByChatIdHashIsNotNull();
}
