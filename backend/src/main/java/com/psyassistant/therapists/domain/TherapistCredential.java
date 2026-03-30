package com.psyassistant.therapists.domain;

import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/**
 * Represents a professional credential (license, certification, etc.)
 * held by a therapist.
 */
@Entity
@Table(name = "therapist_credential")
public class TherapistCredential extends BaseEntity {

    /** The therapist profile this credential belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "therapist_profile_id", nullable = false)
    private TherapistProfile therapistProfile;

    /** Type of credential (e.g., "Licensed Professional Counselor"). */
    @Column(name = "credential_type", nullable = false, length = 128)
    private String credentialType;

    /** Organization that issued the credential. */
    @Column(name = "issuer", nullable = false, length = 255)
    private String issuer;

    /** Date the credential was issued. */
    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    /** Optional expiry date; null if credential does not expire. */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    /** Optional user ID that created this credential (for audit). */
    @Column(name = "created_by", length = 255, updatable = false)
    private String createdByUser;

    /** Optional user ID that last updated this credential (for audit). */
    @Column(name = "updated_by", length = 255)
    private String updatedByUser;

    // Constructors
    public TherapistCredential() {}

    public TherapistCredential(TherapistProfile therapistProfile, String credentialType,
                              String issuer, LocalDate issueDate, LocalDate expiryDate) {
        this.therapistProfile = therapistProfile;
        this.credentialType = credentialType;
        this.issuer = issuer;
        this.issueDate = issueDate;
        this.expiryDate = expiryDate;
    }

    // Getters and setters
    public TherapistProfile getTherapistProfile() {
        return therapistProfile;
    }

    public void setTherapistProfile(TherapistProfile therapistProfile) {
        this.therapistProfile = therapistProfile;
    }

    public String getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(String credentialType) {
        this.credentialType = credentialType;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getCreatedByUser() {
        return createdByUser;
    }

    public void setCreatedByUser(String createdByUser) {
        this.createdByUser = createdByUser;
    }

    public String getUpdatedByUser() {
        return updatedByUser;
    }

    public void setUpdatedByUser(String updatedByUser) {
        this.updatedByUser = updatedByUser;
    }
}
