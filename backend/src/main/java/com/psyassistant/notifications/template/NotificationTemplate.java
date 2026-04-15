package com.psyassistant.notifications.template;

import com.psyassistant.common.audit.BaseEntity;
import com.psyassistant.notifications.NotificationEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Configurable message template for a notification event type, channel, and language.
 *
 * <p>At most one template per (event_type, channel, language) combination can be ACTIVE at a time.
 * This invariant is enforced by a partial unique index ({@code uq_notification_template_active})
 * in addition to the application-level SELECT FOR UPDATE in the service layer.
 */
@Entity
@Table(name = "notification_templates")
public class NotificationTemplate extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private NotificationEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 8)
    private NotificationLanguage language;

    /** Email subject line (null for SMS templates). */
    @Column(name = "subject", columnDefinition = "TEXT")
    private String subject;

    /** HTML for EMAIL; plain text for SMS. */
    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TemplateStatus status = TemplateStatus.INACTIVE;

    @Column(name = "has_unknown_variables", nullable = false)
    private boolean hasUnknownVariables;

    /** Required by JPA. */
    protected NotificationTemplate() { }

    /**
     * Creates a new template in {@link TemplateStatus#INACTIVE} state.
     *
     * @param eventType           notification event this template covers
     * @param channel             delivery channel
     * @param language            content language
     * @param subject             email subject (may be null for SMS)
     * @param body                message body
     * @param hasUnknownVariables true if the body contains unrecognized tokens
     */
    public NotificationTemplate(
            final NotificationEventType eventType,
            final NotificationChannel channel,
            final NotificationLanguage language,
            final String subject,
            final String body,
            final boolean hasUnknownVariables) {
        this.eventType = eventType;
        this.channel = channel;
        this.language = language;
        this.subject = subject;
        this.body = body;
        this.hasUnknownVariables = hasUnknownVariables;
        this.status = TemplateStatus.INACTIVE;
    }

    // ---- business methods ------------------------------------------------

    /** Activates this template. */
    public void activate() {
        this.status = TemplateStatus.ACTIVE;
    }

    /** Deactivates this template. */
    public void deactivate() {
        this.status = TemplateStatus.INACTIVE;
    }

    /**
     * Updates the mutable content fields (only allowed while INACTIVE).
     *
     * @param subject             new subject (may be null for SMS)
     * @param body                new body
     * @param hasUnknownVariables re-validated unknown-variables flag
     */
    public void updateContent(final String subject, final String body,
                              final boolean hasUnknownVariables) {
        this.subject = subject;
        this.body = body;
        this.hasUnknownVariables = hasUnknownVariables;
    }

    // ---- accessors -------------------------------------------------------

    public NotificationEventType getEventType() {
        return eventType;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public NotificationLanguage getLanguage() {
        return language;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public TemplateStatus getStatus() {
        return status;
    }

    public boolean isHasUnknownVariables() {
        return hasUnknownVariables;
    }

    public boolean isActive() {
        return TemplateStatus.ACTIVE == status;
    }
}
