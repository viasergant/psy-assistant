package com.psyassistant.scheduling.service;

import com.psyassistant.scheduling.domain.Appointment;
import com.psyassistant.scheduling.dto.RecurringConflictCheckResponse.ConflictDetail;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Performs batch conflict detection for all slots in a recurring series.
 *
 * <p>Delegates to the existing {@link ConflictDetectionService} for each slot.
 * Batching in Java (rather than a single DB query) keeps the logic reusable and
 * consistent with the existing GIST-index-based single-slot detection.
 *
 * <p>Performance note: for 20 occurrences each check is an indexed GIST query
 * ({@literal <} 5 ms each), so 20 × 5 ms = 100 ms well within the 3-second budget.
 */
@Service
public class BatchConflictDetectionService {

    private final ConflictDetectionService conflictDetectionService;

    public BatchConflictDetectionService(final ConflictDetectionService conflictDetectionService) {
        this.conflictDetectionService = conflictDetectionService;
    }

    /**
     * Checks each slot in {@code slots} for conflicts on the given therapist's schedule.
     *
     * @param therapistProfileId therapist UUID
     * @param slots ordered list of occurrence start times (index = position in list)
     * @param durationMinutes duration of each occurrence
     * @return map keyed by slot index (0-based); only conflicting indices appear in the map
     */
    @Transactional(readOnly = true)
    public Map<Integer, ConflictDetail> detectBatch(final UUID therapistProfileId,
                                                      final List<ZonedDateTime> slots,
                                                      final int durationMinutes) {
        final Map<Integer, ConflictDetail> conflicts = new HashMap<>();

        for (int i = 0; i < slots.size(); i++) {
            final List<Appointment> found = conflictDetectionService.findConflictingAppointments(
                    therapistProfileId, slots.get(i), durationMinutes);

            if (!found.isEmpty()) {
                final Appointment first = found.get(0);
                conflicts.put(i, new ConflictDetail(
                        first.getId(),
                        null, // Client name join deferred — consistent with existing mapper pattern
                        first.getStartTime()
                ));
            }
        }

        return conflicts;
    }
}
