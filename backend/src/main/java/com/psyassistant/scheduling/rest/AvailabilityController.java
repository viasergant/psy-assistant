package com.psyassistant.scheduling.rest;

import com.psyassistant.scheduling.dto.AvailabilitySlotResponse;
import com.psyassistant.scheduling.service.AvailabilityQueryService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for querying therapist availability.
 *
 * <p>Computes 30-minute slots respecting recurring schedule, overrides, and approved leave.
 */
@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

    private final AvailabilityQueryService availabilityQueryService;

    public AvailabilityController(final AvailabilityQueryService availabilityQueryService) {
        this.availabilityQueryService = availabilityQueryService;
    }

    /**
     * Retrieves available 30-minute slots for a therapist across a date range.
     *
     * @param therapistProfileId therapist profile UUID
     * @param startDate query start date (inclusive)
     * @param endDate query end date (inclusive)
     * @return list of available slots
     */
    @GetMapping("/therapists/{therapistProfileId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'RECEPTION_ADMIN_STAFF', 'THERAPIST')")
    public ResponseEntity<List<AvailabilitySlotResponse>> getAvailableSlots(
        @PathVariable final UUID therapistProfileId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate endDate
    ) {
        final var slots = availabilityQueryService.computeAvailableSlots(
            therapistProfileId,
            startDate,
            endDate
        );

        final List<AvailabilitySlotResponse> response = slots.stream()
            .map(slot -> new AvailabilitySlotResponse(
                slot.date(),
                slot.startTime(),
                slot.endTime(),
                "available"  // Future: check against booked appointments
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
