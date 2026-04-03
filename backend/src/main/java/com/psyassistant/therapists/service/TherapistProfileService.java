package com.psyassistant.therapists.service;

import static org.springframework.http.HttpStatus.CONFLICT;
import com.psyassistant.therapists.domain.Language;
import com.psyassistant.therapists.domain.Specialization;
import com.psyassistant.therapists.domain.TherapistProfile;
import com.psyassistant.therapists.dto.TherapistProfileAdminDto;
import com.psyassistant.therapists.dto.TherapistWithAccountResponseDto;
import com.psyassistant.therapists.repository.LanguageRepository;
import com.psyassistant.therapists.repository.SpecializationRepository;
import com.psyassistant.therapists.repository.TherapistProfileRepository;
import com.psyassistant.users.UserManagementService;
import com.psyassistant.users.UserRepository;
import com.psyassistant.users.UserRole;
import com.psyassistant.users.dto.CreateUserRequest;
import com.psyassistant.users.dto.UserCreationResponseDto;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service layer for therapist profile CRUD operations, lifecycle management,
 * and audit trail emission.
 *
 * <p>Coordinates with {@link TherapistAuditService} to record field-level
 * changes in an immutable audit trail. Uses optimistic locking via the
 * {@code version} column to prevent concurrent conflicting updates.
 *
 * <p>When creating a new therapist profile, this service automatically creates
 * a corresponding user account with THERAPIST role if one doesn't already exist.
 */
@Service
public class TherapistProfileService {

    private final TherapistProfileRepository profileRepository;
    private final SpecializationRepository specializationRepository;
    private final LanguageRepository languageRepository;
    private final TherapistAuditService auditService;
    private final UserRepository userRepository;
    private final UserManagementService userManagementService;

    public TherapistProfileService(TherapistProfileRepository profileRepository,
                                   SpecializationRepository specializationRepository,
                                   LanguageRepository languageRepository,
                                   TherapistAuditService auditService,
                                   UserRepository userRepository,
                                   UserManagementService userManagementService) {
        this.profileRepository = profileRepository;
        this.specializationRepository = specializationRepository;
        this.languageRepository = languageRepository;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.userManagementService = userManagementService;
    }

    /**
     * Creates a new therapist profile with initial specializations, languages,
     * and employment status.
     *
     * <p>If no user account exists with the provided email, this method automatically
     * creates a user account with THERAPIST role. The user will receive a password reset
     * token to set up their account.
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
        // Validate email uniqueness for therapist profiles
        if (profileRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(
                "A therapist with this email address already exists"
            );
        }

        // Create user account if it doesn't exist
        UUID actorId = getCurrentUserId();
        if (!userRepository.existsByEmail(email)) {
            CreateUserRequest userRequest = new CreateUserRequest(
                email,
                name,
                UserRole.THERAPIST
            );
            userManagementService.createUser(userRequest, actorId);
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

        // Initialize lazy collections to avoid LazyInitializationException
        Hibernate.initialize(saved.getSpecializations());
        Hibernate.initialize(saved.getLanguages());

        return saved;
    }

    /**
     * Creates a therapist user account with temporary password AND therapist profile atomically.
     * This is the preferred method for the streamlined therapist onboarding workflow.
     *
     * <p>The user is created with {@code mustChangePassword=true} and a secure temporary password.
     * The therapist profile is created with the provided basic information and primary specialization.
     *
     * @param email the therapist's email (must be unique)
     * @param fullName the therapist's full name
     * @param phone optional phone number
     * @param employmentStatus employment status (e.g., "FULL_TIME", "PART_TIME")
     * @param primarySpecializationId UUID of the primary specialization
     * @return response containing user details with temp password and therapist profile
     * @throws IllegalArgumentException if email already exists or specialization not found
     */
    @Transactional
    public TherapistWithAccountResponseDto createTherapistWithAccount(
            String email, String fullName, String phone,
            String employmentStatus, UUID primarySpecializationId) {

        // Validate email uniqueness
        if (profileRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(
                "A therapist with this email address already exists"
            );
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                "A user with this email address already exists"
            );
        }

        // Validate specialization exists
        Specialization primarySpec = specializationRepository.findById(primarySpecializationId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Specialization not found: " + primarySpecializationId
            ));

        // Create user account with temporary password
        UUID actorId = getCurrentUserId();
        CreateUserRequest userRequest = new CreateUserRequest(
            email,
            fullName,
            UserRole.THERAPIST
        );
        UserCreationResponseDto userResponse = userManagementService
            .createUserWithTemporaryPassword(userRequest, actorId);

        // Create therapist profile
        TherapistProfile profile = new TherapistProfile(email, fullName, phone);
        profile.setEmploymentStatus(employmentStatus);
        profile.setActive(true);
        profile.getSpecializations().add(primarySpec);

        // Use saveAndFlush to ensure the INSERT is immediately flushed to the DB
        // so that the @Version field in the returned entity reflects the committed value.
        TherapistProfile savedProfile = profileRepository.saveAndFlush(profile);

        // Record creation audit
        String actorName = getCurrentUsername();
        List<TherapistAuditService.FieldChange> changes = new ArrayList<>();
        changes.add(new TherapistAuditService.FieldChange("email", null, email));
        changes.add(new TherapistAuditService.FieldChange("name", null, fullName));
        if (phone != null) {
            changes.add(new TherapistAuditService.FieldChange("phone", null, phone));
        }
        changes.add(new TherapistAuditService.FieldChange("employmentStatus", null, employmentStatus));
        changes.add(new TherapistAuditService.FieldChange("primarySpecialization", null,
            primarySpec.getName()));

        auditService.recordAuditEntry(
            savedProfile.getId(),
            actorId,
            actorName,
            "CREATE",
            changes,
            null
        );

        // Re-fetch the profile after all operations to ensure the version in the response
        // matches what is committed in the DB (guards against any version increment during flush).
        TherapistProfile freshProfile = getProfile(savedProfile.getId());

        return new TherapistWithAccountResponseDto(
            userResponse,
            TherapistProfileAdminDto.fromAdmin(freshProfile)
        );
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
        TherapistProfile profile = profileRepository.findById(profileId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Therapist profile not found: " + profileId
            ));
        // Initialize lazy collections to avoid LazyInitializationException
        Hibernate.initialize(profile.getSpecializations());
        Hibernate.initialize(profile.getLanguages());
        return profile;
    }

    /**
     * Retrieves all therapist profiles with pagination.
     *
     * @param pageable pagination information
     * @return a page of therapist profiles
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<TherapistProfile> getAllProfiles(
        org.springframework.data.domain.Pageable pageable
    ) {
        return profileRepository.findAllBy(pageable);
    }

    /**
     * Finds a therapist profile by email address (case-insensitive).
     *
     * @param email the email to search for
     * @return the profile if found
     * @throws EntityNotFoundException if no profile with that email exists
     */
    @Transactional(readOnly = true)
    public TherapistProfile getProfileByEmail(String email) {
        TherapistProfile profile = profileRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new EntityNotFoundException(
                "Therapist profile not found for email: " + email
            ));
        // Initialize lazy collections to avoid LazyInitializationException
        Hibernate.initialize(profile.getSpecializations());
        Hibernate.initialize(profile.getLanguages());
        return profile;
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

        // Initialize lazy collections to avoid LazyInitializationException
        Hibernate.initialize(saved.getSpecializations());
        Hibernate.initialize(saved.getLanguages());

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

        // Initialize lazy collections to avoid LazyInitializationException
        Hibernate.initialize(saved.getSpecializations());
        Hibernate.initialize(saved.getLanguages());

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

    // ========== Profile Completion Methods ==========

    /**
     * Checks and updates the completion status of a therapist profile.
     *
     * <p>A profile is considered complete when it meets these criteria:
     * <ul>
     *   <li>Name is not blank</li>
     *   <li>Email is not blank</li>
     *   <li>At least one specialization is specified</li>
     *   <li>At least one language is specified</li>
     *   <li>Professional bio is present</li>
     * </ul>
     *
     * @param profileId the therapist profile UUID
     * @return the updated profile completion status
     * @throws EntityNotFoundException if profile not found
     */
    @Transactional
    public TherapistProfile.ProfileCompletionStatus checkAndUpdateProfileCompletion(UUID profileId) {
        TherapistProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Therapist profile not found: " + profileId));

        boolean isComplete = isProfileComplete(profile);
        TherapistProfile.ProfileCompletionStatus newStatus = isComplete
                ? TherapistProfile.ProfileCompletionStatus.COMPLETE
                : TherapistProfile.ProfileCompletionStatus.INCOMPLETE;

        if (profile.getProfileCompletionStatus() != newStatus) {
            profile.setProfileCompletionStatus(newStatus);
            profileRepository.save(profile);
        }

        return newStatus;
    }

    /**
     * Marks a therapist profile as complete if it meets all requirements.
     *
     * <p>This method validates that all required fields are filled before
     * marking the profile as complete. If validation fails, an exception is thrown.
     *
     * @param profileId the therapist profile UUID
     * @return the updated profile
     * @throws EntityNotFoundException if profile not found
     * @throws IllegalStateException if profile doesn't meet completion requirements
     */
    @Transactional
    public TherapistProfile markProfileComplete(UUID profileId) {
        TherapistProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Therapist profile not found: " + profileId));

        if (!isProfileComplete(profile)) {
            List<String> missingFields = getMissingRequiredFields(profile);
            throw new IllegalStateException(
                    "Profile cannot be marked complete. Missing required fields: "
                            + String.join(", ", missingFields));
        }

        profile.setProfileCompletionStatus(TherapistProfile.ProfileCompletionStatus.COMPLETE);
        TherapistProfile saved = profileRepository.save(profile);
        
        // Initialize lazy collections to avoid LazyInitializationException
        Hibernate.initialize(saved.getSpecializations());
        Hibernate.initialize(saved.getLanguages());
        
        return saved;
    }

    /**
     * Gets the current completion status of a therapist profile.
     *
     * @param profileId the therapist profile UUID
     * @return the completion status
     * @throws EntityNotFoundException if profile not found
     */
    @Transactional(readOnly = true)
    public TherapistProfile.ProfileCompletionStatus getProfileCompletionStatus(UUID profileId) {
        TherapistProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Therapist profile not found: " + profileId));
        return profile.getProfileCompletionStatus();
    }

    /**
     * Checks if a profile meets all completion requirements.
     *
     * @param profile the therapist profile
     * @return true if profile is complete
     */
    private boolean isProfileComplete(TherapistProfile profile) {
        return profile.getName() != null && !profile.getName().isBlank()
                && profile.getEmail() != null && !profile.getEmail().isBlank()
                && profile.getSpecializations() != null && !profile.getSpecializations().isEmpty()
                && profile.getLanguages() != null && !profile.getLanguages().isEmpty()
                && profile.getBio() != null && !profile.getBio().isBlank();
    }

    /**
     * Returns a list of required fields that are missing or incomplete.
     *
     * @param profile the therapist profile
     * @return list of missing field names
     */
    private List<String> getMissingRequiredFields(TherapistProfile profile) {
        List<String> missing = new ArrayList<>();
        if (profile.getName() == null || profile.getName().isBlank()) {
            missing.add("name");
        }
        if (profile.getEmail() == null || profile.getEmail().isBlank()) {
            missing.add("email");
        }
        if (profile.getSpecializations() == null || profile.getSpecializations().isEmpty()) {
            missing.add("specializations (at least 1)");
        }
        if (profile.getLanguages() == null || profile.getLanguages().isEmpty()) {
            missing.add("languages (at least 1)");
        }
        if (profile.getBio() == null || profile.getBio().isBlank()) {
            missing.add("bio");
        }
        return missing;
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
