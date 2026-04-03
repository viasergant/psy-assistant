package com.psyassistant.scheduling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Immutable audit log entry for appointment changes.
 *
 * <p>Records all significant actions performed on appointments, including:
 * <ul>
 *     <li>Creation with or without conflict override</li>
 *     <li>Rescheduling with reason</li>
 *     <li>Cancellation with type and reason</li>
 *     <li>Status changes</li>
 *     <li>Notes updates</li>
 * </ul>
 *
 * <p><strong>Immutability enforcement</strong>:
 * <ul>
 *     <li>No setter methods provided (except for JPA internal use)</li>
 *     <li>Database-level revocation of UPDATE/DELETE (see migration V26)</li>
 *     <li>Service layer only performs INSERT operations</li>
 * </ul>
 *
 * <p>Does not extend {@link com.psyassistant.common.audit.BaseEntity} to avoid
 * unnecessary createdAt/updatedAt fields (audit entries are never updated).
 */
@Entity
@Table(name = "appointment_audit")
public class AppointmentAudit {

    /** Surrogate primary key for audit entry. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Reference to the appointment being audited.
     *
     * <p>Intentionally NOT a foreign key constraint - audit trail must survive
     * even if the appointment is deleted (for compliance and litigation purposes).
     */
    @Column(name = "appointment_id", nullable = false)
    private UUID appointmentId;

    // ========== Action Metadata ==========

    /** Type of action performed (CREATED, RESCHEDULED, CANCELLED, etc.). */
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private AuditActionType actionType;

    /** When the action was performed. */
    @Column(name = "action_timestamp", nullable = false)
    private Instant actionTimestamp;

    /** User who performed the action. */
    @Column(name = "actor_user_id", nullable = false)
    private UUID actorUserId;

    /** Snapshot of actor's display name at time of action. */
    @Column(name = "actor_name", nullable = false, length = 255)
    private String actorName;

    // ========== Request Context ==========

    /** Correlation ID for tracing related audit entries (e.g., UUID or trace ID). */
    @Column(name = "request_id", length = 100)
    private String requestId;

    /** Client IP address (IPv4 or IPv6). */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** HTTP User-Agent header for forensics. */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    // ========== Change Details ==========

    /** Specific field that changed (for granular tracking, e.g., "status", "start_time"). */
    @Column(name = "field_name", length = 100)
    private String fieldName;

    /** Value before the change (JSON or string representation). */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /** Value after the change (JSON or string representation). */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    // ========== Metadata ==========

    /** Human-readable notes about the action. */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ========== Recurring Series Fields (PA-33) ==========

    /**
     * Scope of the edit operation (SINGLE / FUTURE_SERIES / ENTIRE_SERIES).
     *
     * <p>Null for non-series audit entries.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "edit_scope", length = 20)
    private EditScope editScope;

    /**
     * Parent series ID when this audit entry relates to a recurring appointment.
     *
     * <p>Intentionally NOT a foreign key — audit trail must survive series deletion.
     * Null for non-series audit entries.
     */
    @Column(name = "series_id")
    private Long seriesId;

    /**
     * Additional structured context as JSON.
     *
     * <p>Examples:
     * <ul>
     *     <li>Conflict override: details of the conflicting appointment</li>
     *     <li>Cancellation: full cancellation type and reason</li>
     *     <li>Reschedule: original and new time slots</li>
     * </ul>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    /**
     * Default constructor for JPA.
     */
    protected AppointmentAudit() {
    }

    /**
     * Creates a new immutable audit log entry.
     *
     * @param appointmentId appointment UUID being audited
     * @param actionType type of action performed
     * @param actorUserId user who performed the action
     * @param actorName display name of actor (snapshot)
     */
    public AppointmentAudit(final UUID appointmentId,
                              final AuditActionType actionType,
                              final UUID actorUserId,
                              final String actorName) {
        this.appointmentId = appointmentId;
        this.actionType = actionType;
        this.actionTimestamp = Instant.now();
        this.actorUserId = actorUserId;
        this.actorName = actorName;
    }

    /**
     * Builder pattern for optional fields.
     */
    public static class Builder {
        private final AppointmentAudit audit;

        public Builder(final UUID appointmentId,
                       final AuditActionType actionType,
                       final UUID actorUserId,
                       final String actorName) {
            this.audit = new AppointmentAudit(appointmentId, actionType, actorUserId, actorName);
        }

        public Builder requestId(final String requestId) {
            audit.requestId = requestId;
            return this;
        }

        public Builder ipAddress(final String ipAddress) {
            audit.ipAddress = ipAddress;
            return this;
        }

        public Builder userAgent(final String userAgent) {
            audit.userAgent = userAgent;
            return this;
        }

        public Builder fieldChange(final String fieldName, final String oldValue, final String newValue) {
            audit.fieldName = fieldName;
            audit.oldValue = oldValue;
            audit.newValue = newValue;
            return this;
        }

        public Builder notes(final String notes) {
            audit.notes = notes;
            return this;
        }

        public Builder metadata(final String metadata) {
            audit.metadata = metadata;
            return this;
        }

        public Builder editScope(final EditScope editScope) {
            audit.editScope = editScope;
            return this;
        }

        public Builder seriesId(final Long seriesId) {
            audit.seriesId = seriesId;
            return this;
        }

        public AppointmentAudit build() {
            return audit;
        }
    }

    // ========== Getters Only (Immutable) ==========

    public UUID getId() {
        return id;
    }

    public UUID getAppointmentId() {
        return appointmentId;
    }

    public AuditActionType getActionType() {
        return actionType;
    }

    public Instant getActionTimestamp() {
        return actionTimestamp;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getActorName() {
        return actorName;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getNotes() {
        return notes;
    }

    public String getMetadata() {
        return metadata;
    }

    public EditScope getEditScope() {
        return editScope;
    }

    public Long getSeriesId() {
        return seriesId;
    }
}
