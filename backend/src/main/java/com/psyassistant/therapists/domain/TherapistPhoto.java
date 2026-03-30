package com.psyassistant.therapists.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Stores the therapist's profile photo as binary data (JPEG/PNG).
 * Uses {@code therapist_profile_id} as both the ID and the foreign key.
 */
@Entity
@Table(name = "therapist_photo")
public class TherapistPhoto {

    /** The therapist profile ID (serves as PK and FK). */
    @Id
    @Column(name = "therapist_profile_id")
    private UUID therapistProfileId;

    /** The therapist profile this photo belongs to. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "therapist_profile_id", nullable = false)
    @MapsId
    private TherapistProfile therapistProfile;

    /** MIME type of the photo (e.g., "image/jpeg", "image/png"). */
    @Column(name = "mime_type", nullable = false, length = 32)
    private String mimeType;

    /** Binary photo data. */
    @Lob
    @Column(name = "data", nullable = false)
    private byte[] data;

    /** Size of the photo in bytes. */
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    /** Timestamp when the photo was uploaded or last updated. */
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    /** Optional user ID that uploaded the photo (for audit). */
    @Column(name = "uploaded_by", length = 255)
    private String uploadedBy;

    // Constructors
    public TherapistPhoto() {
        this.uploadedAt = Instant.now();
    }

    public TherapistPhoto(UUID therapistProfileId, String mimeType, byte[] data,
                         Long sizeBytes, String uploadedBy) {
        this.therapistProfileId = therapistProfileId;
        this.mimeType = mimeType;
        this.data = data;
        this.sizeBytes = sizeBytes;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = Instant.now();
    }

    // Getters and setters
    public UUID getTherapistProfileId() {
        return therapistProfileId;
    }

    public void setTherapistProfileId(UUID therapistProfileId) {
        this.therapistProfileId = therapistProfileId;
    }

    public TherapistProfile getTherapistProfile() {
        return therapistProfile;
    }

    public void setTherapistProfile(TherapistProfile therapistProfile) {
        this.therapistProfile = therapistProfile;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
}
