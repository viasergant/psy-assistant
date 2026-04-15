package com.psyassistant.notifications.template.dto;

/**
 * Response DTO carrying the rendered subject and body after variable substitution.
 */
public record PreviewResponse(
        String renderedSubject,
        String renderedBody
) { }
