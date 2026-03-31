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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
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
@RequestMapping("/api/v1/therapists/{therapistProfileId}")
public class AvailabilityController {

    private final AvailabilityQueryService availabilityQueryService;

    /**
     * Constructs the availability controller.
     *
     * @param availabilityQueryService availability query service
     */
    public AvailabilityController(final AvailabilityQueryService availabilityQueryService) {
        this.availabilityQueryService = availabilityQueryService;
    }

    /**
     * Resolves "me" token to the actual therapist profile ID from JWT.
     *
     * @param pathParam the path parameter (either "me" or a UUID string)
     * @return the resolved UUID
     * @throws IllegalArgumentException if pathParam is "me" but no therapistProfileId in JWT
     */
    private UUID resolveTherapistProfileId(final String pathParam) {
        if ("me".equals(pathParam)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                String therapistProfileId = jwt.getClaimAsString("therapistProfileId");
                if (therapistProfileId != null) {
                    return UUID.fromString(therapistProfileId);
                }
            }
            throw new IllegalArgumentException(
                "Cannot resolve 'me': therapistProfileId not found in JWT token");
        }
        return UUID.fromString(pathParam);
    }

    /**
     * Retrieves available 30-minute slots for a therapist across a date range.
     *
     * @param therapistProfileIdParam therapist profile UUID or "me"
     * @param startDate query start date (inclusive)
     * @param endDate query end date (inclusive)
     * @return list of available slots
     */
    @GetMapping("/availability")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'RECEPTION_ADMIN_STAFF', 'THERAPIST')")
    public ResponseEntity<List<AvailabilitySlotResponse>> getAvailableSlots(
        @PathVariable("therapistProfileId") final String therapistProfileIdParam,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate endDate
    ) {
        final UUID therapistProfileId = resolveTherapistProfileId(therapistProfileIdParam);
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
                true  // Future: check against booked appointments
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
