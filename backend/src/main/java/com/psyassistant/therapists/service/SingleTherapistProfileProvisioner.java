package com.psyassistant.therapists.service;

import com.psyassistant.therapists.domain.TherapistProfile;
import com.psyassistant.therapists.repository.TherapistProfileRepository;
import com.psyassistant.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Ensures the owner of a single-therapist organization (solo / SaaS) has a linked
 * {@link TherapistProfile}.
 *
 * <p>Without a profile the JWT carries no {@code therapistProfileId} claim, so booking,
 * schedule and reminders cannot resolve the therapist. Idempotent and independent of
 * {@code TenantContext}: the organization is taken from the user.
 */
@Component
public class SingleTherapistProfileProvisioner {

    private static final Logger LOG = LoggerFactory.getLogger(SingleTherapistProfileProvisioner.class);

    private final TherapistProfileRepository profileRepository;

    /**
     * Constructs the provisioner.
     *
     * @param profileRepository therapist profile data access
     */
    public SingleTherapistProfileProvisioner(final TherapistProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    /**
     * Creates and links a therapist profile for the user unless one already exists.
     *
     * @param user a persisted user that has an id and an organization
     * @return the existing or newly created profile
     */
    public TherapistProfile ensureProfile(final User user) {
        return profileRepository.findByUserId(user.getId())
                .or(() -> profileRepository.findByEmailIgnoreCase(user.getEmail()))
                .orElseGet(() -> create(user));
    }

    private TherapistProfile create(final User user) {
        String name = user.getFullName() == null || user.getFullName().isBlank()
                ? user.getEmail() : user.getFullName();
        TherapistProfile profile = new TherapistProfile(user.getEmail(), name, null);
        profile.setOrgId(user.getOrgId());
        profile.setUserId(user.getId());
        profile.setActive(true);
        profile.setProfileCompletionStatus(TherapistProfile.ProfileCompletionStatus.COMPLETE);
        TherapistProfile saved = profileRepository.save(profile);
        LOG.info("event=SINGLE_THERAPIST_PROFILE_CREATED userId={} profileId={}", user.getId(), saved.getId());
        return saved;
    }
}
