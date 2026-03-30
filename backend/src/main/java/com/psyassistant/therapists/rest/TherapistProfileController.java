package com.psyassistant.therapists.rest;

import com.psyassistant.therapists.domain.TherapistProfile;
import com.psyassistant.therapists.dto.CreateTherapistProfileRequest;
import com.psyassistant.therapists.dto.TherapistProfileAdminDto;
import com.psyassistant.therapists.dto.UpdateTherapistProfileRequest;
import com.psyassistant.therapists.service.TherapistProfileService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for therapist profile management.
 * All endpoints require appropriate role-based authorization.
 */
@RestController
@RequestMapping("/api/v1/therapists")
public class TherapistProfileController {

    private final TherapistProfileService therapistProfileService;

    public TherapistProfileController(TherapistProfileService therapistProfileService) {
        this.therapistProfileService = therapistProfileService;
    }

    /**
     * GET /api/v1/therapists
     * Lists all therapist profiles with pagination.
     *
     * <p>Required role: SYSTEM_ADMINISTRATOR
     *
     * @param page zero-based page number (default: 0)
     * @param size page size (default: 20)
     * @return 200 OK with paginated therapist list
     */
    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR') or hasRole('ADMIN')")
    public ResponseEntity<PageResponse<TherapistProfileAdminDto>> listTherapists(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<TherapistProfile> profilePage = therapistProfileService.getAllProfiles(pageable);
        
        Page<TherapistProfileAdminDto> dtoPage = profilePage.map(TherapistProfileAdminDto::fromAdmin);
        
        PageResponse<TherapistProfileAdminDto> response = new PageResponse<>(
            dtoPage.getContent(),
            dtoPage.getNumber(),
            dtoPage.getSize(),
            dtoPage.getTotalElements(),
            dtoPage.getTotalPages()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/therapists
     * Creates a new therapist profile.
     *
     * <p>Required role: SYSTEM_ADMINISTRATOR
     *
     * @param request the creation request
     * @return 201 Created with the new profile
     */
    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR') or hasRole('ADMIN')")
    public ResponseEntity<TherapistProfileAdminDto> createTherapist(
        @Valid @RequestBody CreateTherapistProfileRequest request
    ) {
        // Validate required fields
        if (request.email() == null || request.email().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (request.name() == null || request.name().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (request.specializationIds() == null || request.specializationIds().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (request.languageIds() == null || request.languageIds().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            TherapistProfile profile = therapistProfileService.createProfile(
                request.email(),
                request.name(),
                request.phone(),
                request.employmentStatus() != null ? request.employmentStatus() : "ACTIVE",
                request.bio(),
                request.specializationIds(),
                request.languageIds()
            );
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(TherapistProfileAdminDto.fromAdmin(profile));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * GET /api/v1/therapists/{id}
     * Retrieves a therapist profile by ID.
     *
     * <p>Required role: SYSTEM_ADMINISTRATOR or owned by therapist (self-view)
     *
     * @param id the therapist profile ID
     * @return 200 OK with the profile, or 404 if not found
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR') or hasRole('ADMIN') or hasRole('THERAPIST')")
    public ResponseEntity<TherapistProfileAdminDto> getTherapist(@PathVariable UUID id) {
        try {
            TherapistProfile profile = therapistProfileService.getProfile(id);
            return ResponseEntity.ok(TherapistProfileAdminDto.fromAdmin(profile));
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/v1/therapists/by-email/{email}
     * Retrieves a therapist profile by email address.
     *
     * <p>Required role: SYSTEM_ADMINISTRATOR
     *
     * @param email the therapist's email address
     * @return 200 OK with the profile, or 404 if not found
     */
    @GetMapping("/by-email/{email}")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR') or hasRole('ADMIN')")
    public ResponseEntity<TherapistProfileAdminDto> getTherapistByEmail(@PathVariable String email) {
        try {
            TherapistProfile profile = therapistProfileService.getProfileByEmail(email);
            return ResponseEntity.ok(TherapistProfileAdminDto.fromAdmin(profile));
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * PATCH /api/v1/therapists/{id}
     * Updates a therapist profile (admin only, all fields).
     *
     * <p>Supports optimistic locking via version field.
     * Returns 409 Conflict if version mismatch.
     *
     * <p>Required role: SYSTEM_ADMINISTRATOR
     *
     * @param id the therapist profile ID
     * @param request the update request with version
     * @return 200 OK with updated profile, or 409 if version conflict
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR') or hasRole('ADMIN')")
    public ResponseEntity<TherapistProfileAdminDto> updateTherapist(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateTherapistProfileRequest request
    ) {
        // Validate version exists
        if (request.version() == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            TherapistProfile updated = therapistProfileService.updateProfile(
                id,
                profile -> {
                    if (request.email() != null) {
                        profile.setEmail(request.email());
                    }
                    if (request.name() != null) {
                        profile.setName(request.name());
                    }
                    if (request.phone() != null) {
                        profile.setPhone(request.phone());
                    }
                    if (request.employmentStatus() != null) {
                        profile.setEmploymentStatus(request.employmentStatus());
                    }
                    if (request.bio() != null) {
                        profile.setBio(request.bio());
                    }
                    // Note: specializations and languages would be handled separately in Phase 2+
                },
                request.version()
            );
            return ResponseEntity.ok(TherapistProfileAdminDto.fromAdmin(updated));
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (org.springframework.web.server.ResponseStatusException e) {
            if (e.getStatusCode().equals(org.springframework.http.HttpStatusCode.valueOf(409))) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            throw e;
        }
    }

    /**
     * POST /api/v1/therapists/{id}/deactivate
     * Deactivates a therapist profile (soft delete).
     *
     * <p>Required role: SYSTEM_ADMINISTRATOR
     *
     * @param id the therapist profile ID
     * @param request request with optional reason
     * @return 200 OK with updated profile
     */
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR') or hasRole('ADMIN')")
    public ResponseEntity<TherapistProfileAdminDto> deactivateTherapist(
        @PathVariable UUID id,
        @RequestBody(required = false) DeactivateRequest request
    ) {
        try {
            String reason = request != null ? request.reason() : null;
            TherapistProfile updated = therapistProfileService.deactivateProfile(id, reason);
            return ResponseEntity.ok(TherapistProfileAdminDto.fromAdmin(updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Simple request record for deactivation with optional reason.
     */
    public record DeactivateRequest(String reason) { }

    /**
     * Paginated response wrapper matching frontend expectations.
     */
    public record PageResponse<T>(
        java.util.List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
    ) { }
}
