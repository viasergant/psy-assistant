package com.psyassistant.notifications.template.dto;

/**
 * Response DTO for a deactivate operation, carrying a warning flag when
 * no other active template exists for the same event_type/channel/language.
 */
public record DeactivateResponse(
        TemplateResponse template,
        boolean noActiveReplacement
) { }
