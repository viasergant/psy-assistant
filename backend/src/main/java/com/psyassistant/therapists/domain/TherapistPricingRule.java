package com.psyassistant.therapists.domain;

import com.psyassistant.common.audit.BaseEntity;
import com.psyassistant.scheduling.domain.SessionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a pricing rule for a specific session type offered by a therapist.
 *
 * <p>The {@code sessionType} FK references the canonical {@code session_type} lookup table,
 * replacing the legacy {@code service_type_id} column removed in V50.
 */
@Entity
@Table(name = "therapist_pricing_rule")
public class TherapistPricingRule extends BaseEntity {

    /** The therapist profile this pricing rule belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "therapist_profile_id", nullable = false)
    private TherapistProfile therapistProfile;

    /** The canonical session type this rule applies to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_type_id", nullable = false)
    private SessionType sessionType;

    /** Fee or rate for this session type. */
    @Column(name = "rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal rate;

    /** Currency code (e.g., "USD", "EUR"). */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    /** Date this pricing rule becomes effective. */
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /** Optional user ID that last updated this rule (for audit). */
    @Column(name = "updated_by", length = 255)
    private String updatedByUser;

    // Constructors

    /** Default constructor for JPA. */
    public TherapistPricingRule() { }

    /**
     * Creates a new pricing rule.
     *
     * @param therapistProfile the owning therapist profile
     * @param sessionType      the canonical session type
     * @param rate             fee charged for this session type
     * @param currency         ISO-4217 currency code
     * @param effectiveFrom    date the rule becomes active
     */
    public TherapistPricingRule(final TherapistProfile therapistProfile,
                                final SessionType sessionType,
                                final BigDecimal rate,
                                final String currency,
                                final LocalDate effectiveFrom) {
        this.therapistProfile = therapistProfile;
        this.sessionType = sessionType;
        this.rate = rate;
        this.currency = currency;
        this.effectiveFrom = effectiveFrom;
    }

    // Getters and setters

    public TherapistProfile getTherapistProfile() {
        return therapistProfile;
    }

    public void setTherapistProfile(final TherapistProfile therapistProfile) {
        this.therapistProfile = therapistProfile;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(final SessionType sessionType) {
        this.sessionType = sessionType;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(final BigDecimal rate) {
        this.rate = rate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(final String currency) {
        this.currency = currency;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(final LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public String getUpdatedByUser() {
        return updatedByUser;
    }

    public void setUpdatedByUser(final String updatedByUser) {
        this.updatedByUser = updatedByUser;
    }
}
