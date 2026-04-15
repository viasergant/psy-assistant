package com.psyassistant.notifications.template.dto;

import com.psyassistant.notifications.NotificationEventType;
import com.psyassistant.notifications.template.NotificationChannel;
import com.psyassistant.notifications.template.NotificationLanguage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new notification template.
 */
public record CreateTemplateRequest(

        @NotNull(message = "eventType is required")
        NotificationEventType eventType,

        @NotNull(message = "channel is required")
        NotificationChannel channel,

        @NotNull(message = "language is required")
        NotificationLanguage language,

        /** Required for EMAIL; should be null for SMS. */
        @Size(max = 500, message = "subject must not exceed 500 characters")
        String subject,

        @NotNull(message = "body is required")
        @Size(min = 1, message = "body must not be blank")
        String body
) { }
