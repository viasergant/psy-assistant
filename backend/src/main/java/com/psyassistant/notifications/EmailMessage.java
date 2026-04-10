package com.psyassistant.notifications;

/**
 * Immutable value object representing a single outbound email to be sent.
 *
 * @param recipientAddress  the destination email address (plaintext; encrypted at persistence)
 * @param eventType         the notification category that triggered this email
 * @param subjectTemplateKey i18n key for the email subject line
 * @param renderedBody      pre-rendered HTML body (may be empty string in Phase 1)
 */
public record EmailMessage(
        String recipientAddress,
        NotificationEventType eventType,
        String subjectTemplateKey,
        String renderedBody) {
}
