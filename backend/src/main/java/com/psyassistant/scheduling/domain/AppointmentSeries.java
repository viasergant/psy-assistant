package com.psyassistant.scheduling.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a recurring appointment series.
 *
 * <p>A series is the parent record that groups all generated occurrences.
 * Each individual {@link Appointment} that belongs to this series holds a
 * {@code series_id} foreign key and a {@code recurrence_index} (0-based).
 *
 * <p>Key constraints:
 * <ul>
 *     <li>{@code total_occurrences} is capped at 20 (enforced by DB CHECK)</li>
 *     <li>Optimistic locking via {@code @Version} to guard concurrent edits</li>
 *     <li>Series uses a BIGSERIAL PK (not UUID) to keep FK joins compact</li>
 * </ul>
 */
@Entity
@Table(name = "appointment_series")
public class AppointmentSeries {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    /** Pattern that determines the gap between consecutive occurrences. */
    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_type", nullable = false, length = 20)
    private RecurrenceType recurrenceType;

    /** Calendar date of the first occurrence (anchor date). */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Requested number of occurrences (max 20). */
    @Column(name = "total_occurrences", nullable = false)
    private int totalOccurrences;

    /** Actual number of appointments created (may be less than requested if slots were skipped). */
    @Column(name = "generated_occurrences", nullable = false)
    private int generatedOccurrences;

    /** Therapist this series is booked for. */
    @Column(name = "therapist_profile_id", nullable = false)
    private UUID therapistProfileId;

    /** Client this series is booked for. */
    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    /** Session type for all occurrences. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "session_type_id", nullable = false)
    private SessionType sessionType;

    /** Duration in minutes applied to every occurrence. */
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    /** IANA timezone used when generating occurrence wall-clock times. */
    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone;

    /** Series lifecycle status. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SeriesStatus status = SeriesStatus.ACTIVE;

    /** User who created the series. */
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    /** Optimistic locking version. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    /**
     * Default constructor for JPA.
     */
    protected AppointmentSeries() {
    }

    /**
     * Creates a new recurring appointment series.
     *
     * @param recurrenceType spacing between occurrences
     * @param startDate anchor date for the first occurrence
     * @param totalOccurrences requested occurrence count (1-20)
     * @param therapistProfileId therapist UUID
     * @param clientId client UUID
     * @param sessionType session type entity
     * @param durationMinutes duration of every occurrence in minutes
     * @param timezone IANA timezone identifier
     * @param createdBy user who created the series
     */
    public AppointmentSeries(final RecurrenceType recurrenceType,
                              final LocalDate startDate,
                              final int totalOccurrences,
                              final UUID therapistProfileId,
                              final UUID clientId,
                              final SessionType sessionType,
                              final int durationMinutes,
                              final String timezone,
                              final UUID createdBy) {
        this.recurrenceType = recurrenceType;
        this.startDate = startDate;
        this.totalOccurrences = totalOccurrences;
        this.generatedOccurrences = 0;
        this.therapistProfileId = therapistProfileId;
        this.clientId = clientId;
        this.sessionType = sessionType;
        this.durationMinutes = durationMinutes;
        this.timezone = timezone;
        this.createdBy = createdBy;
        this.status = SeriesStatus.ACTIVE;
    }

    // ========== Business Methods ==========

    /**
     * Records that {@code count} occurrences were successfully saved.
     *
     * @param count number of appointments created for this series
     */
    public void setGeneratedOccurrences(final int count) {
        this.generatedOccurrences = count;
        this.updatedAt = Instant.now();
    }

    /**
     * Transitions the series status and updates the {@code updatedAt} timestamp.
     *
     * @param newStatus target status
     */
    public void updateStatus(final SeriesStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    // ========== Getters ==========

    public Long getId() {
        return id;
    }

    public RecurrenceType getRecurrenceType() {
        return recurrenceType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public int getTotalOccurrences() {
        return totalOccurrences;
    }

    public int getGeneratedOccurrences() {
        return generatedOccurrences;
    }

    public UUID getTherapistProfileId() {
        return therapistProfileId;
    }

    public UUID getClientId() {
        return clientId;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getTimezone() {
        return timezone;
    }

    public SeriesStatus getStatus() {
        return status;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
