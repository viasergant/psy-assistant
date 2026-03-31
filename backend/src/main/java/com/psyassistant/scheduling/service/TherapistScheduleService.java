package com.psyassistant.scheduling.service;

import com.psyassistant.scheduling.domain.TherapistRecurringSchedule;
import com.psyassistant.scheduling.domain.TherapistScheduleOverride;
import com.psyassistant.scheduling.repository.TherapistRecurringScheduleRepository;
import com.psyassistant.scheduling.repository.TherapistScheduleOverrideRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing therapist recurring schedules and one-off overrides.
 *
 * <p>Handles CRUD operations for:
 * <ul>
 *     <li>Recurring weekly working hours (day of week + time range)</li>
 *     <li>Schedule overrides for specific dates</li>
 * </ul>
 *
 * <p>All mutations are recorded in the audit trail via {@link TherapistScheduleAuditService}.
 */
@Service
public class TherapistScheduleService {

    private final TherapistRecurringScheduleRepository recurringScheduleRepository;
    private final TherapistScheduleOverrideRepository overrideRepository;
    private final TherapistScheduleAuditService auditService;

    /**
     * Constructs the schedule service.
     *
     * @param recurringScheduleRepository repository for recurring schedules
     * @param overrideRepository repository for overrides
     * @param auditService audit service
     */
    public TherapistScheduleService(final TherapistRecurringScheduleRepository recurringScheduleRepository,
                                     final TherapistScheduleOverrideRepository overrideRepository,
                                     final TherapistScheduleAuditService auditService) {
        this.recurringScheduleRepository = recurringScheduleRepository;
        this.overrideRepository = overrideRepository;
        this.auditService = auditService;
    }

    // ========== Recurring Schedule Operations ==========

    /**
     * Creates a new recurring schedule entry for a therapist.
     *
     * @param therapistProfileId therapist profile UUID
     * @param dayOfWeek day of week (1=Monday, 7=Sunday)
     * @param startTime start time of availability
     * @param endTime end time of availability
     * @param timezone IANA timezone identifier
     * @return created recurring schedule entry
     */
    @Transactional
    public TherapistRecurringSchedule createRecurringSchedule(final UUID therapistProfileId,
                                                                final Integer dayOfWeek,
                                                                final LocalTime startTime,
                                                                final LocalTime endTime,
                                                                final String timezone) {
        validateTimeRange(startTime, endTime);
        validateDayOfWeek(dayOfWeek);

        final var schedule = new TherapistRecurringSchedule(
            therapistProfileId,
            dayOfWeek,
            startTime,
            endTime,
            timezone
        );

        final var saved = recurringScheduleRepository.save(schedule);

        // Record audit entry
        auditService.recordChange(
            therapistProfileId,
            "RECURRING_SCHEDULE",
            saved.getId(),
            "CREATE",
            null
        );

        return saved;
    }

    /**
     * Updates an existing recurring schedule entry.
     *
     * @param scheduleId schedule entry UUID
     * @param startTime new start time
     * @param endTime new end time
     * @param timezone new timezone
     * @return updated schedule entry
     * @throws IllegalArgumentException if schedule not found
     */
    @Transactional
    public TherapistRecurringSchedule updateRecurringSchedule(final UUID scheduleId,
                                                                final LocalTime startTime,
                                                                final LocalTime endTime,
                                                                final String timezone) {
        validateTimeRange(startTime, endTime);

        final var schedule = recurringScheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("Recurring schedule not found: " + scheduleId));

        final String oldStartTime = schedule.getStartTime().toString();
        final String oldEndTime = schedule.getEndTime().toString();
        final String oldTimezone = schedule.getTimezone();

        schedule.setStartTime(startTime);
        schedule.setEndTime(endTime);
        schedule.setTimezone(timezone);

        final var updated = recurringScheduleRepository.save(schedule);

        // Record audit entry with field changes
        auditService.recordChangeWithDetails(
            schedule.getTherapistProfileId(),
            "RECURRING_SCHEDULE",
            scheduleId,
            "UPDATE",
            new TherapistScheduleAuditService.FieldChange[]{
                new TherapistScheduleAuditService.FieldChange("startTime", oldStartTime, startTime.toString()),
                new TherapistScheduleAuditService.FieldChange("endTime", oldEndTime, endTime.toString()),
                new TherapistScheduleAuditService.FieldChange("timezone", oldTimezone, timezone)
            },
            null
        );

        return updated;
    }

    /**
     * Deletes a recurring schedule entry.
     *
     * @param scheduleId schedule entry UUID
     * @throws IllegalArgumentException if schedule not found
     */
    @Transactional
    public void deleteRecurringSchedule(final UUID scheduleId) {
        final var schedule = recurringScheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("Recurring schedule not found: " + scheduleId));

        final UUID therapistProfileId = schedule.getTherapistProfileId();

        recurringScheduleRepository.deleteById(scheduleId);

        // Record audit entry
        auditService.recordChange(
            therapistProfileId,
            "RECURRING_SCHEDULE",
            scheduleId,
            "DELETE",
            null
        );
    }

    /**
     * Retrieves all recurring schedule entries for a therapist.
     *
     * @param therapistProfileId therapist profile UUID
     * @return list of recurring schedule entries, empty if none found
     */
    @Transactional(readOnly = true)
    public List<TherapistRecurringSchedule> getRecurringSchedule(final UUID therapistProfileId) {
        return recurringScheduleRepository.findByTherapistProfileId(therapistProfileId);
    }

    /**
     * Retrieves recurring schedule entries for a specific day of the week.
     *
     * @param therapistProfileId therapist profile UUID
     * @param dayOfWeek day of week (1=Monday, 7=Sunday)
     * @return list of recurring schedule entries for that day
     */
    @Transactional(readOnly = true)
    public List<TherapistRecurringSchedule> getRecurringScheduleForDay(final UUID therapistProfileId,
                                                                         final Integer dayOfWeek) {
        validateDayOfWeek(dayOfWeek);
        return recurringScheduleRepository.findByTherapistProfileIdAndDayOfWeek(therapistProfileId, dayOfWeek);
    }

    // ========== Schedule Override Operations ==========

    /**
     * Creates a schedule override marking a date as unavailable.
     *
     * @param therapistProfileId therapist profile UUID
     * @param overrideDate date to override
     * @param reason optional explanation
     * @return created override
     */
    @Transactional
    public TherapistScheduleOverride createUnavailableOverride(final UUID therapistProfileId,
                                                                 final LocalDate overrideDate,
                                                                 final String reason) {
        validateNotPastDate(overrideDate);

        final var override = new TherapistScheduleOverride(therapistProfileId, overrideDate, reason);
        final var saved = overrideRepository.save(override);

        auditService.recordChange(therapistProfileId, "OVERRIDE", saved.getId(), "CREATE", null);

        return saved;
    }

    /**
     * Creates a schedule override with custom working hours for a specific date.
     *
     * @param therapistProfileId therapist profile UUID
     * @param overrideDate date to override
     * @param startTime custom start time
     * @param endTime custom end time
     * @param reason optional explanation
     * @return created override
     */
    @Transactional
    public TherapistScheduleOverride createCustomHoursOverride(final UUID therapistProfileId,
                                                                 final LocalDate overrideDate,
                                                                 final LocalTime startTime,
                                                                 final LocalTime endTime,
                                                                 final String reason) {
        validateNotPastDate(overrideDate);
        validateTimeRange(startTime, endTime);

        final var override = new TherapistScheduleOverride(
            therapistProfileId,
            overrideDate,
            startTime,
            endTime,
            reason
        );

        final var saved = overrideRepository.save(override);

        auditService.recordChange(therapistProfileId, "OVERRIDE", saved.getId(), "CREATE", null);

        return saved;
    }

    /**
     * Updates an existing schedule override.
     *
     * @param overrideId override UUID
     * @param isAvailable whether the therapist is available
     * @param startTime custom start time (null if unavailable)
     * @param endTime custom end time (null if unavailable)
     * @param reason optional explanation
     * @return updated override
     */
    @Transactional
    public TherapistScheduleOverride updateOverride(final UUID overrideId,
                                                      final Boolean isAvailable,
                                                      final LocalTime startTime,
                                                      final LocalTime endTime,
                                                      final String reason) {
        final var override = overrideRepository.findById(overrideId)
            .orElseThrow(() -> new IllegalArgumentException("Override not found: " + overrideId));

        if (isAvailable && (startTime == null || endTime == null)) {
            throw new IllegalArgumentException("Start and end times are required when available");
        }

        if (isAvailable) {
            validateTimeRange(startTime, endTime);
        }

        override.setIsAvailable(isAvailable);
        override.setStartTime(startTime);
        override.setEndTime(endTime);
        override.setReason(reason);

        final var updated = overrideRepository.save(override);

        auditService.recordChange(
            override.getTherapistProfileId(),
            "OVERRIDE",
            overrideId,
            "UPDATE",
            null
        );

        return updated;
    }

    /**
     * Deletes a schedule override.
     *
     * @param overrideId override UUID
     */
    @Transactional
    public void deleteOverride(final UUID overrideId) {
        final var override = overrideRepository.findById(overrideId)
            .orElseThrow(() -> new IllegalArgumentException("Override not found: " + overrideId));

        final UUID therapistProfileId = override.getTherapistProfileId();

        overrideRepository.deleteById(overrideId);

        auditService.recordChange(therapistProfileId, "OVERRIDE", overrideId, "DELETE", null);
    }

    /**
     * Retrieves all overrides for a therapist within a date range.
     *
     * @param therapistProfileId therapist profile UUID
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return list of overrides within the date range
     */
    @Transactional(readOnly = true)
    public List<TherapistScheduleOverride> getOverridesInRange(final UUID therapistProfileId,
                                                                 final LocalDate startDate,
                                                                 final LocalDate endDate) {
        return overrideRepository.findByTherapistProfileIdAndDateBetween(therapistProfileId, startDate, endDate);
    }

    /**
     * Finds a specific override by therapist and date.
     *
     * @param therapistProfileId therapist profile UUID
     * @param overrideDate the specific date
     * @return optional override
     */
    @Transactional(readOnly = true)
    public Optional<TherapistScheduleOverride> getOverrideForDate(final UUID therapistProfileId,
                                                                    final LocalDate overrideDate) {
        return overrideRepository.findByTherapistProfileIdAndOverrideDate(therapistProfileId, overrideDate);
    }

    // ========== Validation Helpers ==========

    private void validateTimeRange(final LocalTime startTime, final LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start and end times cannot be null");
        }

        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }

        // Validate 30-minute increment alignment
        if (startTime.getMinute() % 30 != 0 || endTime.getMinute() % 30 != 0) {
            throw new IllegalArgumentException("Times must be aligned to 30-minute increments");
        }
    }

    private void validateDayOfWeek(final Integer dayOfWeek) {
        if (dayOfWeek == null || dayOfWeek < 1 || dayOfWeek > 7) {
            throw new IllegalArgumentException("Day of week must be between 1 (Monday) and 7 (Sunday)");
        }
    }

    private void validateNotPastDate(final LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot create override for past date: " + date);
        }
    }
}
