package com.psyassistant.therapists.service;

import com.psyassistant.therapists.domain.Language;
import com.psyassistant.therapists.domain.Specialization;
import com.psyassistant.therapists.domain.TherapistProfile;
import com.psyassistant.therapists.repository.LanguageRepository;
import com.psyassistant.therapists.repository.SpecializationRepository;
import com.psyassistant.therapists.repository.TherapistProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.CONFLICT;

/**
 * Service layer for therapist profile CRUD operations, lifecycle management,
 * and audit trail emission.
 *
 * <p>Coordinates with {@link TherapistAuditService} to record field-level
 * changes in an immutable audit trail. Uses optimistic locking via the
 * {@code version} column to prevent concurrent conflicting updates.
 */
@Service
public class TherapistProfileService {

    private final TherapistProfileRepository profileRepository;
    private final SpecializationRepository specializationRepository;
    private final LanguageRepository languageRepository;
    private final TherapistAuditService auditService;

    public TherapistProfileService(TherapistProfileRepository profileRepository,
                                   SpecializationRepository specializationRepository,
                                   LanguageRepository languageRepository,
                                   TherapistAuditService auditService) {
        this.profileRepository = profileRepository;
        this.specializationRepository = specializationRepository;
        this.languageRepository = languageRepository;
        this.auditService = auditService;
    }

    /**
     * Creates a new therapist profile with initial specializations, languages,
     * and employment status.
     *
     * @param email the therapist's email (must be unique)
     * @param name the therapist's name
     * @param phone the therapist's phone
     * @param employmentStatus initial employment status
     * @param bio optional bio
     * @param specializationIds IDs of specializations
     * @param languageIds IDs of languages
     * @return the created profile
     * @throws IllegalArgumentException if email is not unique
     */
    @Transactional
    public TherapistProfile createProfile(String email, String name, String phone,
                                          String employmentStatus, String bio,
                                          List<UUID> specializationIds,
                                          List<UUID> languageIds) {
        // Validate email uniqueness
        if (profileRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(
                "A therapist with this email address already exists"
            );
        }

        TherapistProfile profile = new TherapistProfile(email, name, phone);
        profile.setEmploymentStatus(employmentStatus);
        profile.setBio(bio);
        profile.setActive(true);

        // Load and associate specializations
        if (specializationIds != null && !specializationIds.isEmpty()) {
            for (UUID specId : specializationIds) {
                Specialization spec = specializationRepository.findById(specId)
                    .orElseThrow(() -> new EntityNotFoundException(
                        "Specialization not found: " + specId
                    ));
                profile.getSpecializations().add(spec);
            }
        }

        // Load and associate languages
        if (languageIds != null && !languageIds.isEmpty()) {
            for (UUID langId : languageIds) {
                Language lang = languageRepository.findById(langId)
                    .orElseThrow(() -> new EntityNotFoundException(
                        "Language not found: " + langId
                    ));
                profile.getLanguages().add(lang);
            }
        }

        TherapistProfile saved = profileRepository.save(profile);

        // Record creation audit
        UUID actorId = getCurrentUserId();
        String actorName = getCurrentUsername();
        List<TherapistAuditService.FieldChange> changes = new ArrayList<>();
        changes.add(new TherapistAuditService.FieldChange("email", null, email));
        changes.add(new TherapistAuditService.FieldChange("name", null, name));
        changes.add(new TherapistAuditService.FieldChange("phone", null, phone));
        changes.add(new TherapistAuditService.FieldChange("employmentStatus", null, employmentStatus));
        if (bio != null) {
            changes.add(new TherapistAuditService.FieldChange("bio", null, bio));
        }

        auditService.recordAuditEntry(saved.getId(), actorId, actorName, "CREATE", changes, null);

        return saved;
    }

    /**
     * Retrieves a therapist profile by ID.
     *
     * @param profileId the profile ID
     * @return the profile
     * @throws EntityNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public TherapistProfile getProfile(UUID profileId) {
        return profileRepository.findById(profileId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Therapist profile not found: " + profileId
            ));
    }

    /**
     * Updates a therapist profile with optimistic locking.
     * Tracks all field changes and emits an audit entry.
     *
     * @param profileId the profile ID
     * @param updates a function that applies updates to the profile
     * @param expectedVersion the version expected by the client (for optimistic locking)
     * @return the updated profile
     * @throws ObjectOptimisticLockingFailureException if version mismatch
     */
    @Transactional
    public TherapistProfile updateProfile(UUID profileId, java.util.function.Consumer<TherapistProfile> updates,
                                          Long expectedVersion) {
        TherapistProfile profile = getProfile(profileId);

        // Check optimistic locking version
        if (!Objects.equals(profile.getVersion(), expectedVersion)) {
            throw new ResponseStatusException(CONFLICT,
                "Concurrent modification detected. Please refresh and retry.");
        }

        // Capture old values for audit
        TherapistProfile snapshot = new TherapistProfile();
        snapshot.setEmail(profile.getEmail());
        snapshot.setName(profile.getName());
        snapshot.setPhone(profile.getPhone());
        snapshot.setEmploymentStatus(profile.getEmploymentStatus());
        snapshot.setBio(profile.getBio());
        snapshot.setContactPhone(profile.getContactPhone());

        // Apply updates
        updates.accept(profile);
        TherapistProfile saved = profileRepository.save(profile);

        // Track field changes for audit
        List<TherapistAuditService.FieldChange> changes = new ArrayList<>();
        if (!Objects.equals(snapshot.getEmail(), saved.getEmail())) {
            changes.add(new TherapistAuditService.FieldChange("email", snapshot.getEmail(), saved.getEmail()));
        }
        if (!Objects.equals(snapshot.getName(), saved.getName())) {
            changes.add(new TherapistAuditService.FieldChange("name", snapshot.getName(), saved.getName()));
        }
        if (!Objects.equals(snapshot.getPhone(), saved.getPhone())) {
            changes.add(new TherapistAuditService.FieldChange("phone", snapshot.getPhone(), saved.getPhone()));
        }
        if (!Objects.equals(snapshot.getEmploymentStatus(), saved.getEmploymentStatus())) {
            changes.add(new TherapistAuditService.FieldChange(
                "employmentStatus", snapshot.getEmploymentStatus(), saved.getEmploymentStatus()
            ));
        }
        if (!Objects.equals(snapshot.getBio(), saved.getBio())) {
            changes.add(new TherapistAuditService.FieldChange("bio", snapshot.getBio(), saved.getBio()));
        }
        if (!Objects.equals(snapshot.getContactPhone(), saved.getContactPhone())) {
            changes.add(new TherapistAuditService.FieldChange(
                "contactPhone", snapshot.getContactPhone(), saved.getContactPhone()
            ));
        }

        UUID actorId = getCurrentUserId();
        String actorName = getCurrentUsername();
        auditService.recordAuditEntry(saved.getId(), actorId, actorName, "UPDATE", changes, null);

        return saved;
    }

    /**
     * Deactivates a therapist profile (soft delete).
     *
     * @param profileId the profile ID
     * @param reason optional reason for deactivation
     * @return the deactivated profile
     */
    @Transactional
    public TherapistProfile deactivateProfile(UUID profileId, String reason) {
        TherapistProfile profile = getProfile(profileId);

        if (!profile.getActive()) {
            throw new IllegalArgumentException("Profile is already inactive");
        }

        profile.setActive(false);
        TherapistProfile saved = profileRepository.save(profile);

        // Record deactivation audit
        List<TherapistAuditService.FieldChange> changes = new ArrayList<>();
        changes.add(new TherapistAuditService.FieldChange("active", "true", "false"));
        if (reason != null) {
            changes.add(new TherapistAuditService.FieldChange("deactivationReason", null, reason));
        }

        UUID actorId = getCurrentUserId();
        String actorName = getCurrentUsername();
        auditService.recordAuditEntry(saved.getId(), actorId, actorName, "DEACTIVATE", changes, null);

        return saved;
    }

    /**
     * Checks if a therapist email is unique (excluding a specific profile).
     *
     * @param email the email to check
     * @param excludeProfileId profile ID to exclude from check (for updates)
     * @return true if email is unique
     */
    @Transactional(readOnly = true)
    public boolean isEmailUnique(String email, UUID excludeProfileId) {
        if (excludeProfileId == null) {
            return !profileRepository.existsByEmailIgnoreCase(email);
        }
        return !profileRepository.existsByEmailIgnoreCaseExcludingId(email, excludeProfileId);
    }

    // ========== Helper Methods ==========

    /**
     * Gets the current authenticated user's ID.
     *
     * @return the user ID or null if not authenticated
     */
    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            // Assuming principal is a UUID string (from JWT)
            try {
                return UUID.fromString(auth.getName());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Gets the current authenticated user's display name.
     *
     * @return the username or "System" if not authenticated
     */
    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return auth.getName();
        }
        return "System";
    }

    /**
     * Checks if the current user has a specific authority.
     *
     * @param authority the authority to check
     * @return true if user has the authority
     */
    protected boolean hasAuthority(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + authority));
    }
}
