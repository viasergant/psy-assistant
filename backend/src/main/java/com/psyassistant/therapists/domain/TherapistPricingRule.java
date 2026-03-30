package com.psyassistant.therapists.domain;

import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a pricing rule for a specific service type offered by a therapist.
 */
@Entity
@Table(name = "therapist_pricing_rule")
public class TherapistPricingRule extends BaseEntity {

    /** The therapist profile this pricing rule belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "therapist_profile_id", nullable = false)
    private TherapistProfile therapistProfile;

    /** The service type this rule applies to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_type_id", nullable = false)
    private ServiceType serviceType;

    /** Fee or rate for this service type. */
    @Column(name = "rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal rate;

    /** Currency code (e.g., "USD", "EUR"). */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    /** Date this pricing rule becomes effective. */
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /** Optional user ID that created this rule (for audit). */
    @Column(name = "created_by", length = 255, updatable = false)
    private String createdByUser;

    /** Optional user ID that last updated this rule (for audit). */
    @Column(name = "updated_by", length = 255)
    private String updatedByUser;

    // Constructors
    public TherapistPricingRule() {}

    public TherapistPricingRule(TherapistProfile therapistProfile, ServiceType serviceType,
                               BigDecimal rate, String currency, LocalDate effectiveFrom) {
        this.therapistProfile = therapistProfile;
        this.serviceType = serviceType;
        this.rate = rate;
        this.currency = currency;
        this.effectiveFrom = effectiveFrom;
    }

    // Getters and setters
    public TherapistProfile getTherapistProfile() {
        return therapistProfile;
    }

    public void setTherapistProfile(TherapistProfile therapistProfile) {
        this.therapistProfile = therapistProfile;
    }

    public ServiceType getServiceType() {
        return serviceType;
    }

    public void setServiceType(ServiceType serviceType) {
        this.serviceType = serviceType;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
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
