package com.psyassistant.riskflags.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A risk flag raised against a specific client.
 *
 * <p>Status lifecycle: {@code ACTIVE} → {@code RESOLVED}.
 * Once resolved a flag is never re-opened; a new flag must be created instead.
 * Clinical notes are stored in plain text and their visibility is enforced at the
 * service/controller layer based on the caller's {@code READ_RISK_FLAG_NOTES} permission.
 */
@Entity
@Table(name = "client_risk_flags")
public class ClientRiskFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Column(name = "flag_type_id", nullable = false, updatable = false)
    private UUID flagTypeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClientRiskFlagStatus status;

    @Column(name = "clinical_note", columnDefinition = "TEXT")
    private String clinicalNote;

    @Column(name = "review_date", nullable = false)
    private LocalDate reviewDate;

    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private UUID createdByUserId;

    /** Server-managed creation timestamp; set by DB DEFAULT. */
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_by_user_id")
    private UUID resolvedByUserId;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    // ---- constructors ----

    protected ClientRiskFlag() { }

    /**
     * Creates a new ACTIVE risk flag.
     *
     * @param clientId          client the flag is raised against
     * @param flagTypeId        type of risk being flagged
     * @param clinicalNote      optional clinical note (visible only to privileged roles)
     * @param reviewDate        mandatory date by which the flag must be reviewed
     * @param createdByUserId   user who raised the flag
     */
    public ClientRiskFlag(final UUID clientId, final UUID flagTypeId,
                          final String clinicalNote, final LocalDate reviewDate,
                          final UUID createdByUserId) {
        this.clientId = clientId;
        this.flagTypeId = flagTypeId;
        this.clinicalNote = clinicalNote;
        this.reviewDate = reviewDate;
        this.createdByUserId = createdByUserId;
        this.status = ClientRiskFlagStatus.ACTIVE;
    }

    // ---- business methods ----

    /**
     * Resolves this flag.
     *
     * @param resolvedByUserId  user performing the resolution
     * @param resolutionNote    explanation of how the flag was resolved
     */
    public void resolve(final UUID resolvedByUserId, final String resolutionNote) {
        this.status = ClientRiskFlagStatus.RESOLVED;
        this.resolvedByUserId = resolvedByUserId;
        this.resolvedAt = Instant.now();
        this.resolutionNote = resolutionNote;
    }

    // ---- getters ----

    public UUID getId() {
        return id;
    }

    public UUID getClientId() {
        return clientId;
    }

    public UUID getFlagTypeId() {
        return flagTypeId;
    }

    public ClientRiskFlagStatus getStatus() {
        return status;
    }

    public String getClinicalNote() {
        return clinicalNote;
    }

    public LocalDate getReviewDate() {
        return reviewDate;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getResolvedByUserId() {
        return resolvedByUserId;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }
}
