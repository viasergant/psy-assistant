package com.psyassistant.notifications.template;

import com.psyassistant.notifications.NotificationEventType;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA repository for {@link NotificationTemplate}.
 */
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    /**
     * Returns all templates matching the given optional filters.
     * Null values are treated as "all" (no filter applied for that field).
     */
    @Query("""
            SELECT t FROM NotificationTemplate t
            WHERE (:eventType IS NULL OR t.eventType = :eventType)
              AND (:channel   IS NULL OR t.channel   = :channel)
              AND (:language  IS NULL OR t.language  = :language)
              AND (:status    IS NULL OR t.status    = :status)
            ORDER BY t.eventType, t.channel, t.language, t.createdAt DESC
            """)
    List<NotificationTemplate> findByFilters(
            @Param("eventType") NotificationEventType eventType,
            @Param("channel")   NotificationChannel channel,
            @Param("language")  NotificationLanguage language,
            @Param("status")    TemplateStatus status);

    /**
     * Finds the currently ACTIVE template for a specific event/channel/language combination.
     */
    Optional<NotificationTemplate> findByEventTypeAndChannelAndLanguageAndStatus(
            NotificationEventType eventType,
            NotificationChannel channel,
            NotificationLanguage language,
            TemplateStatus status);

    /**
     * Finds the active template for a given combination and locks it for update,
     * preventing concurrent activations from creating duplicate ACTIVE entries.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT t FROM NotificationTemplate t
            WHERE t.eventType = :eventType
              AND t.channel   = :channel
              AND t.language  = :language
              AND t.status    = 'ACTIVE'
            """)
    Optional<NotificationTemplate> findActiveForUpdateLocked(
            @Param("eventType") NotificationEventType eventType,
            @Param("channel")   NotificationChannel channel,
            @Param("language")  NotificationLanguage language);
}
