package com.psyassistant.crm.clients;

import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Represents a fully converted client record created from a CRM lead.
 *
 * <p>Extends {@link BaseEntity} to inherit the UUID primary key and Spring Data Auditing
 * fields ({@code createdAt}, {@code updatedAt}, {@code createdBy}).
 *
 * <p>The {@code sourceLeadId} establishes a back-link to the originating lead and carries
 * a UNIQUE database constraint — this is the authoritative concurrency guard that prevents
 * a lead from being converted twice.
 */
@Entity
@Table(name = "clients")
public class Client extends BaseEntity {

    /** Internal human-friendly identifier for support operations. */
    @Column(name = "client_code", length = 32, unique = true)
    private String clientCode;

    /** Optimistic locking version for profile updates. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** Full display name for the client. */
    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    /** Preferred display name for communication (may differ from legal full name). */
    @Column(name = "preferred_name", length = 255)
    private String preferredName;

    /** Date of birth (optional). */
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /** Free-text gender identity, if provided by the client. */
    @Column(name = "sex_or_gender", length = 64)
    private String sexOrGender;

    /** Preferred pronouns. */
    @Column(name = "pronouns", length = 64)
    private String pronouns;

    /** UUID of the staff member responsible for this client. */
    @Column(name = "owner_id")
    private UUID ownerId;

    /** UUID of the therapist assigned to this client. */
    @Column(name = "assigned_therapist_id")
    private UUID assignedTherapistId;

    /** Free-text notes about the client (may include pre-conversion lead notes). */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Primary contact email for client communication. */
    @Column(name = "email", length = 255)
    private String email;

    /** Primary contact phone number. */
    @Column(name = "phone", length = 64)
    private String phone;

    /** Secondary contact phone number. */
    @Column(name = "secondary_phone", length = 64)
    private String secondaryPhone;

    /** First address line. */
    @Column(name = "address_line_1", length = 255)
    private String addressLine1;

    /** Second address line. */
    @Column(name = "address_line_2", length = 255)
    private String addressLine2;

    /** City or locality. */
    @Column(name = "city", length = 128)
    private String city;

    /** Region/state/province. */
    @Column(name = "region", length = 128)
    private String region;

    /** Postal or ZIP code. */
    @Column(name = "postal_code", length = 32)
    private String postalCode;

    /** Country name or ISO-like value used by the UI. */
    @Column(name = "country", length = 128)
    private String country;

    /** Referral source description, such as campaign or partner. */
    @Column(name = "referral_source", length = 255)
    private String referralSource;

    /** Referral contact person name. */
    @Column(name = "referral_contact_name", length = 255)
    private String referralContactName;

    /** Additional referral details. */
    @Column(name = "referral_notes", columnDefinition = "TEXT")
    private String referralNotes;

    /** Preferred communication method (PHONE, SMS, EMAIL). */
    @Column(name = "preferred_communication_method", length = 20)
    private String preferredCommunicationMethod;

    /** Whether voice calls are allowed. */
    @Column(name = "allow_phone")
    private Boolean allowPhone;

    /** Whether SMS messages are allowed. */
    @Column(name = "allow_sms")
    private Boolean allowSms;

    /** Whether email messages are allowed. */
    @Column(name = "allow_email")
    private Boolean allowEmail;

    /** Whether voicemail messages are allowed. */
    @Column(name = "allow_voicemail")
    private Boolean allowVoicemail;

    /** Emergency contact full name. */
    @Column(name = "emergency_contact_name", length = 255)
    private String emergencyContactName;

    /** Relationship between contact and client. */
    @Column(name = "emergency_contact_relationship", length = 255)
    private String emergencyContactRelationship;

    /** Emergency contact phone number. */
    @Column(name = "emergency_contact_phone", length = 64)
    private String emergencyContactPhone;

    /** Emergency contact email address. */
    @Column(name = "emergency_contact_email", length = 255)
    private String emergencyContactEmail;

    /** Opaque key used to load the client profile photo from storage. */
    @Column(name = "photo_storage_key", length = 255)
    private String photoStorageKey;

    /** MIME type of the profile photo. */
    @Column(name = "photo_mime_type", length = 64)
    private String photoMimeType;

    /** Timestamp of the latest profile photo upload. */
    @Column(name = "photo_updated_at")
    private OffsetDateTime photoUpdatedAt;

    /** Treatment status for caseload visibility (ACTIVE, ON_HOLD, DISCHARGED). */
    @Column(name = "treatment_status", nullable = false, length = 20)
    private String treatmentStatus = "ACTIVE";

    /**
     * UUID of the lead this client was converted from.
     * Carries a UNIQUE constraint enforced at the database level.
     */
    @Column(name = "source_lead_id", unique = true)
    private UUID sourceLeadId;

    /** All contact methods (email and/or phone) registered for this client. */
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClientContactMethod> contactMethods = new ArrayList<>();

    /** Required by JPA. */
    protected Client() {
    }

    /**
     * Creates a new client with a required full name.
     *
     * @param fullName the client's full display name
     */
    public Client(final String fullName) {
        this.fullName = fullName;
    }

    /** Returns the internal support identifier. */
    public String getClientCode() {
        return clientCode;
    }

    /** Sets the internal support identifier. */
    public void setClientCode(final String clientCode) {
        this.clientCode = clientCode;
    }

    /** Returns the optimistic locking version. */
    public Long getVersion() {
        return version;
    }

    /** Returns preferred display name. */
    public String getPreferredName() {
        return preferredName;
    }

    /** Sets preferred display name. */
    public void setPreferredName(final String preferredName) {
        this.preferredName = preferredName;
    }

    /** Returns date of birth. */
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    /** Sets date of birth. */
    public void setDateOfBirth(final LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    /** Returns gender identity value. */
    public String getSexOrGender() {
        return sexOrGender;
    }

    /** Sets gender identity value. */
    public void setSexOrGender(final String sexOrGender) {
        this.sexOrGender = sexOrGender;
    }

    /** Returns preferred pronouns. */
    public String getPronouns() {
        return pronouns;
    }

    /** Sets preferred pronouns. */
    public void setPronouns(final String pronouns) {
        this.pronouns = pronouns;
    }

    /** Returns the client's full display name. */
    public String getFullName() {
        return fullName;
    }

    /** Sets the client's full display name. */
    public void setFullName(final String fullName) {
        this.fullName = fullName;
    }

    /** Returns the owning staff member's UUID (may be null). */
    public UUID getOwnerId() {
        return ownerId;
    }

    /** Sets the owning staff member's UUID. */
    public void setOwnerId(final UUID ownerId) {
        this.ownerId = ownerId;
    }

    /** Returns assigned therapist UUID. */
    public UUID getAssignedTherapistId() {
        return assignedTherapistId;
    }

    /** Sets assigned therapist UUID. */
    public void setAssignedTherapistId(final UUID assignedTherapistId) {
        this.assignedTherapistId = assignedTherapistId;
    }

    /** Returns the free-text notes (may be null). */
    public String getNotes() {
        return notes;
    }

    /** Sets the free-text notes. */
    public void setNotes(final String notes) {
        this.notes = notes;
    }

    /** Returns primary email. */
    public String getEmail() {
        return email;
    }

    /** Sets primary email. */
    public void setEmail(final String email) {
        this.email = email;
    }

    /** Returns primary phone. */
    public String getPhone() {
        return phone;
    }

    /** Sets primary phone. */
    public void setPhone(final String phone) {
        this.phone = phone;
    }

    /** Returns secondary phone. */
    public String getSecondaryPhone() {
        return secondaryPhone;
    }

    /** Sets secondary phone. */
    public void setSecondaryPhone(final String secondaryPhone) {
        this.secondaryPhone = secondaryPhone;
    }

    /** Returns address line 1. */
    public String getAddressLine1() {
        return addressLine1;
    }

    /** Sets address line 1. */
    public void setAddressLine1(final String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    /** Returns address line 2. */
    public String getAddressLine2() {
        return addressLine2;
    }

    /** Sets address line 2. */
    public void setAddressLine2(final String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    /** Returns city. */
    public String getCity() {
        return city;
    }

    /** Sets city. */
    public void setCity(final String city) {
        this.city = city;
    }

    /** Returns region/state/province. */
    public String getRegion() {
        return region;
    }

    /** Sets region/state/province. */
    public void setRegion(final String region) {
        this.region = region;
    }

    /** Returns postal code. */
    public String getPostalCode() {
        return postalCode;
    }

    /** Sets postal code. */
    public void setPostalCode(final String postalCode) {
        this.postalCode = postalCode;
    }

    /** Returns country. */
    public String getCountry() {
        return country;
    }

    /** Sets country. */
    public void setCountry(final String country) {
        this.country = country;
    }

    /** Returns referral source. */
    public String getReferralSource() {
        return referralSource;
    }

    /** Sets referral source. */
    public void setReferralSource(final String referralSource) {
        this.referralSource = referralSource;
    }

    /** Returns referral contact name. */
    public String getReferralContactName() {
        return referralContactName;
    }

    /** Sets referral contact name. */
    public void setReferralContactName(final String referralContactName) {
        this.referralContactName = referralContactName;
    }

    /** Returns referral notes. */
    public String getReferralNotes() {
        return referralNotes;
    }

    /** Sets referral notes. */
    public void setReferralNotes(final String referralNotes) {
        this.referralNotes = referralNotes;
    }

    /** Returns preferred communication method. */
    public String getPreferredCommunicationMethod() {
        return preferredCommunicationMethod;
    }

    /** Sets preferred communication method. */
    public void setPreferredCommunicationMethod(final String preferredCommunicationMethod) {
        this.preferredCommunicationMethod = preferredCommunicationMethod;
    }

    /** Returns allow-phone preference. */
    public Boolean getAllowPhone() {
        return allowPhone;
    }

    /** Sets allow-phone preference. */
    public void setAllowPhone(final Boolean allowPhone) {
        this.allowPhone = allowPhone;
    }

    /** Returns allow-sms preference. */
    public Boolean getAllowSms() {
        return allowSms;
    }

    /** Sets allow-sms preference. */
    public void setAllowSms(final Boolean allowSms) {
        this.allowSms = allowSms;
    }

    /** Returns allow-email preference. */
    public Boolean getAllowEmail() {
        return allowEmail;
    }

    /** Sets allow-email preference. */
    public void setAllowEmail(final Boolean allowEmail) {
        this.allowEmail = allowEmail;
    }

    /** Returns allow-voicemail preference. */
    public Boolean getAllowVoicemail() {
        return allowVoicemail;
    }

    /** Sets allow-voicemail preference. */
    public void setAllowVoicemail(final Boolean allowVoicemail) {
        this.allowVoicemail = allowVoicemail;
    }

    /** Returns emergency contact name. */
    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    /** Sets emergency contact name. */
    public void setEmergencyContactName(final String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    /** Returns emergency contact relationship. */
    public String getEmergencyContactRelationship() {
        return emergencyContactRelationship;
    }

    /** Sets emergency contact relationship. */
    public void setEmergencyContactRelationship(final String emergencyContactRelationship) {
        this.emergencyContactRelationship = emergencyContactRelationship;
    }

    /** Returns emergency contact phone. */
    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    /** Sets emergency contact phone. */
    public void setEmergencyContactPhone(final String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    /** Returns emergency contact email. */
    public String getEmergencyContactEmail() {
        return emergencyContactEmail;
    }

    /** Sets emergency contact email. */
    public void setEmergencyContactEmail(final String emergencyContactEmail) {
        this.emergencyContactEmail = emergencyContactEmail;
    }

    /** Returns photo storage key. */
    public String getPhotoStorageKey() {
        return photoStorageKey;
    }

    /** Sets photo storage key. */
    public void setPhotoStorageKey(final String photoStorageKey) {
        this.photoStorageKey = photoStorageKey;
    }

    /** Returns photo MIME type. */
    public String getPhotoMimeType() {
        return photoMimeType;
    }

    /** Sets photo MIME type. */
    public void setPhotoMimeType(final String photoMimeType) {
        this.photoMimeType = photoMimeType;
    }

    /** Returns timestamp of the latest photo update. */
    public OffsetDateTime getPhotoUpdatedAt() {
        return photoUpdatedAt;
    }

    /** Sets timestamp of the latest photo update. */
    public void setPhotoUpdatedAt(final OffsetDateTime photoUpdatedAt) {
        this.photoUpdatedAt = photoUpdatedAt;
    }

    /** Returns the UUID of the originating lead (may be null). */
    public UUID getSourceLeadId() {
        return sourceLeadId;
    }

    /** Sets the originating lead UUID. */
    public void setSourceLeadId(final UUID sourceLeadId) {
        this.sourceLeadId = sourceLeadId;
    }

    /** Returns the treatment status (ACTIVE, ON_HOLD, DISCHARGED). */
    public String getTreatmentStatus() {
        return treatmentStatus;
    }

    /** Sets the treatment status. */
    public void setTreatmentStatus(final String treatmentStatus) {
        this.treatmentStatus = treatmentStatus;
    }

    /**
     * Returns an unmodifiable view of the contact methods list.
     * Mutate via {@link #setContactMethods(List)}.
     */
    public List<ClientContactMethod> getContactMethods() {
        return Collections.unmodifiableList(contactMethods);
    }

    /**
     * Replaces all existing contact methods with the provided list.
     *
     * <p>Uses in-place clear + addAll so that JPA orphan removal fires correctly.
     *
     * @param methods the new contact methods to associate
     */
    public void setContactMethods(final List<ClientContactMethod> methods) {
        contactMethods.clear();
        contactMethods.addAll(methods);
    }
}
