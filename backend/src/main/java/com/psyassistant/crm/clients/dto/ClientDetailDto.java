package com.psyassistant.crm.clients.dto;

import com.psyassistant.crm.clients.Client;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Detail view of a single client record.
 *
 * @param id           client UUID
 * @param fullName     full display name
 * @param clientCode                   internal client identifier
 * @param version                      optimistic locking version
 * @param sourceLeadId                 UUID of the originating lead
 * @param ownerId                      optional owner user UUID
 * @param assignedTherapistId          optional assigned therapist UUID
 * @param notes                        optional free-text notes
 * @param preferredName                optional preferred display name
 * @param dateOfBirth                  optional date of birth
 * @param sexOrGender                  optional free-text gender identity
 * @param pronouns                     optional pronouns
 * @param email                        optional email
 * @param phone                        optional primary phone
 * @param secondaryPhone               optional secondary phone
 * @param addressLine1                 optional address line 1
 * @param addressLine2                 optional address line 2
 * @param city                         optional city
 * @param region                       optional region/state
 * @param postalCode                   optional postal code
 * @param country                      optional country
 * @param referralSource               optional referral source
 * @param referralContactName          optional referral contact name
 * @param referralNotes                optional referral notes
 * @param preferredCommunicationMethod optional preferred communication method
 * @param allowPhone                   optional allow-phone preference
 * @param allowSms                     optional allow-sms preference
 * @param allowEmail                   optional allow-email preference
 * @param allowVoicemail               optional allow-voicemail preference
 * @param emergencyContactName         optional emergency contact name
 * @param emergencyContactRelationship optional emergency contact relationship
 * @param emergencyContactPhone        optional emergency contact phone
 * @param emergencyContactEmail        optional emergency contact email
 * @param tags                         client tags
 * @param photoUrl                     profile photo download URL
 * @param canEditProfile               capability flag for profile edits
 * @param canEditTags                  capability flag for tag edits
 * @param canUploadPhoto               capability flag for photo upload
 * @param createdAt                    record creation timestamp
 * @param updatedAt                    record last-modified timestamp
 * @param createdBy                    email/name of the principal that created the record
 */
public record ClientDetailDto(
        UUID id,
        String fullName,
        String clientCode,
        Long version,
        UUID sourceLeadId,
        UUID ownerId,
        UUID assignedTherapistId,
        String notes,
        String preferredName,
        LocalDate dateOfBirth,
        String sexOrGender,
        String pronouns,
        String email,
        String phone,
        String secondaryPhone,
        String addressLine1,
        String addressLine2,
        String city,
        String region,
        String postalCode,
        String country,
        String referralSource,
        String referralContactName,
        String referralNotes,
        String preferredCommunicationMethod,
        Boolean allowPhone,
        Boolean allowSms,
        Boolean allowEmail,
        Boolean allowVoicemail,
        String emergencyContactName,
        String emergencyContactRelationship,
        String emergencyContactPhone,
        String emergencyContactEmail,
        List<String> tags,
        String photoUrl,
        boolean canEditProfile,
        boolean canEditTags,
        boolean canUploadPhoto,
        Instant createdAt,
        Instant updatedAt,
        String createdBy
) {

    /**
     * Builds a {@link ClientDetailDto} from a {@link Client} entity.
     *
     * @param client the client entity
    * @param tags tags for the client profile
    * @param photoUrl profile photo download URL
     * @param canEditProfile capability flag for profile edits
     * @param canEditTags    capability flag for tag edits
     * @param canUploadPhoto capability flag for photo upload
     * @return populated DTO
     */
    public static ClientDetailDto from(final Client client,
                                final List<String> tags,
                                final String photoUrl,
                                       final boolean canEditProfile,
                                       final boolean canEditTags,
                                       final boolean canUploadPhoto) {
        return new ClientDetailDto(
                client.getId(),
                client.getFullName(),
                client.getClientCode(),
                client.getVersion(),
                client.getSourceLeadId(),
                client.getOwnerId(),
                client.getAssignedTherapistId(),
                client.getNotes(),
                client.getPreferredName(),
                client.getDateOfBirth(),
                client.getSexOrGender(),
                client.getPronouns(),
                client.getEmail(),
                client.getPhone(),
                client.getSecondaryPhone(),
                client.getAddressLine1(),
                client.getAddressLine2(),
                client.getCity(),
                client.getRegion(),
                client.getPostalCode(),
                client.getCountry(),
                client.getReferralSource(),
                client.getReferralContactName(),
                client.getReferralNotes(),
                client.getPreferredCommunicationMethod(),
                client.getAllowPhone(),
                client.getAllowSms(),
                client.getAllowEmail(),
                client.getAllowVoicemail(),
                client.getEmergencyContactName(),
                client.getEmergencyContactRelationship(),
                client.getEmergencyContactPhone(),
                client.getEmergencyContactEmail(),
                tags,
                photoUrl,
                canEditProfile,
                canEditTags,
                canUploadPhoto,
                client.getCreatedAt(),
                client.getUpdatedAt(),
                client.getCreatedBy()
        );
    }
}
