package com.psyassistant.notifications.template.dto;

import com.psyassistant.notifications.NotificationEventType;
import com.psyassistant.notifications.template.NotificationChannel;
import com.psyassistant.notifications.template.NotificationLanguage;
import com.psyassistant.notifications.template.NotificationTemplate;
import com.psyassistant.notifications.template.TemplateStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO representing a single notification template record.
 */
public record TemplateResponse(
        UUID id,
        NotificationEventType eventType,
        NotificationChannel channel,
        NotificationLanguage language,
        String subject,
        String body,
        TemplateStatus status,
        boolean hasUnknownVariables,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Constructs a {@link TemplateResponse} from a {@link NotificationTemplate} entity.
     *
     * @param t the entity
     * @return response DTO
     */
    public static TemplateResponse from(final NotificationTemplate t) {
        return new TemplateResponse(
                t.getId(),
                t.getEventType(),
                t.getChannel(),
                t.getLanguage(),
                t.getSubject(),
                t.getBody(),
                t.getStatus(),
                t.isHasUnknownVariables(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
