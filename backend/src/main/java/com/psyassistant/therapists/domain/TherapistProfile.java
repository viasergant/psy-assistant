package com.psyassistant.therapists.domain;

import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a therapist's professional profile, including personal information,
 * qualifications, specializations, languages, and pricing rules.
 *
 * <p>Extends {@link BaseEntity} to inherit UUID primary key and Spring Data Auditing
 * fields ({@code createdAt}, {@code updatedAt}, {@code createdBy}).
 *
 * <p>Uses optimistic locking via {@code version} column to prevent concurrent
 * conflicting updates. All modifications are recorded in {@code TherapistProfileAuditEntry}
 * with field-level details in {@code TherapistProfileAuditChange}.
 */
@Entity
@Table(name = "therapist_profile")
public class TherapistProfile extends BaseEntity {

    /** Unique contact email address for the therapist. */
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /** Full name of the therapist. */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Primary contact phone number. */
    @Column(name = "phone", length = 64)
    private String phone;

    /** Employment status (e.g., "ACTIVE", "ON_LEAVE", "RETIRED"). */
    @Column(name = "employment_status", nullable = false, length = 20)
    private String employmentStatus = "ACTIVE";

    /** Professional bio or description visible to clients. */
    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    /** Contact phone number for direct client communication (may differ from professional phone). */
    @Column(name = "contact_phone", length = 64)
    private String contactPhone;

    /** Whether the profile is active (true) or deactivated (false). */
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /** Optimistic locking version for profile updates. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    /** Optional user ID that marked this therapist updated (for audit purposes). */
    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    // ========== Relationships ==========

    /** Set of specializations this therapist possesses. */
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinTable(
        name = "therapist_specialization",
        joinColumns = @JoinColumn(name = "therapist_profile_id"),
        inverseJoinColumns = @JoinColumn(name = "specialization_id")
    )
    private Set<Specialization> specializations = new HashSet<>();

    /** Set of languages this therapist can communicate in. */
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinTable(
        name = "therapist_language",
        joinColumns = @JoinColumn(name = "therapist_profile_id"),
        inverseJoinColumns = @JoinColumn(name = "language_id")
    )
    private Set<Language> languages = new HashSet<>();

    /** Professional credentials (e.g., licenses, certifications). */
    @OneToMany(
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        mappedBy = "therapistProfile"
    )
    private Set<TherapistCredential> credentials = new HashSet<>();

    /** Pricing rules for different service types. */
    @OneToMany(
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        mappedBy = "therapistProfile"
    )
    private Set<TherapistPricingRule> pricingRules = new HashSet<>();

    /** Professional photo stored separately. */
    @OneToOne(mappedBy = "therapistProfile", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private TherapistPhoto photo;

    // Constructors
    public TherapistProfile() { }

    public TherapistProfile(String email, String name, String phone) {
        this.email = email;
        this.name = name;
        this.phone = phone;
    }

    // Getters and setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
    }

    public void setEmploymentStatus(String employmentStatus) {
        this.employmentStatus = employmentStatus;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Set<Specialization> getSpecializations() {
        return specializations;
    }

    public void setSpecializations(Set<Specialization> specializations) {
        this.specializations = specializations;
    }

    public Set<Language> getLanguages() {
        return languages;
    }

    public void setLanguages(Set<Language> languages) {
        this.languages = languages;
    }

    public Set<TherapistCredential> getCredentials() {
        return credentials;
    }

    public void setCredentials(Set<TherapistCredential> credentials) {
        this.credentials = credentials;
    }

    public Set<TherapistPricingRule> getPricingRules() {
        return pricingRules;
    }

    public void setPricingRules(Set<TherapistPricingRule> pricingRules) {
        this.pricingRules = pricingRules;
    }

    public TherapistPhoto getPhoto() {
        return photo;
    }

    public void setPhoto(TherapistPhoto photo) {
        this.photo = photo;
    }
}
