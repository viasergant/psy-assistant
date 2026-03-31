package com.psyassistant.scheduling.repository;

import com.psyassistant.scheduling.domain.TherapistRecurringSchedule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link TherapistRecurringSchedule} entities.
 *
 * <p>Provides CRUD operations and custom queries for therapist recurring weekly schedules.
 */
@Repository
public interface TherapistRecurringScheduleRepository extends JpaRepository<TherapistRecurringSchedule, UUID> {

    /**
     * Finds all recurring schedule entries for a specific therapist.
     *
     * @param therapistProfileId therapist profile UUID
     * @return list of recurring schedule entries, empty if none found
     */
    List<TherapistRecurringSchedule> findByTherapistProfileId(UUID therapistProfileId);

    /**
     * Finds all recurring schedule entries for a specific therapist and day of week.
     *
     * @param therapistProfileId therapist profile UUID
     * @param dayOfWeek day of week (1=Monday, 7=Sunday)
     * @return list of recurring schedule entries for that day, empty if none found
     */
    List<TherapistRecurringSchedule> findByTherapistProfileIdAndDayOfWeek(
        UUID therapistProfileId,
        Integer dayOfWeek
    );

    /**
     * Deletes all recurring schedule entries for a specific therapist.
     *
     * @param therapistProfileId therapist profile UUID
     */
    void deleteByTherapistProfileId(UUID therapistProfileId);

    /**
     * Deletes all recurring schedule entries for a specific therapist and day of week.
     *
     * @param therapistProfileId therapist profile UUID
     * @param dayOfWeek day of week (1=Monday, 7=Sunday)
     */
    void deleteByTherapistProfileIdAndDayOfWeek(UUID therapistProfileId, Integer dayOfWeek);
}
