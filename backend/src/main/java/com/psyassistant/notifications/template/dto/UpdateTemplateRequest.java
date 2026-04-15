package com.psyassistant.notifications.template.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing notification template.
 * Only content fields are updatable; event_type, channel, and language are immutable.
 */
public record UpdateTemplateRequest(

        @Size(max = 500, message = "subject must not exceed 500 characters")
        String subject,

        @NotNull(message = "body is required")
        @Size(min = 1, message = "body must not be blank")
        String body
) { }
