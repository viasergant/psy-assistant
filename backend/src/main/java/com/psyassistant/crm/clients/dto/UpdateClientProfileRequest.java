package com.psyassistant.crm.clients.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request body for a full update of client profile fields managed in PA-23 slice one.
 *
 * @param version                      optimistic locking version from the latest GET response
 * @param fullName                     required full display name
 * @param preferredName                optional preferred display name
 * @param dateOfBirth                  optional date of birth
 * @param sexOrGender                  optional free-text gender identity
 * @param pronouns                     optional preferred pronouns
 * @param ownerId                      optional owner UUID
 * @param assignedTherapistId          optional assigned therapist UUID
 * @param notes                        optional notes
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
 * @param preferredCommunicationMethod optional preferred method
 * @param allowPhone                   optional allow phone flag
 * @param allowSms                     optional allow sms flag
 * @param allowEmail                   optional allow email flag
 * @param allowVoicemail               optional allow voicemail flag
 * @param emergencyContactName         optional emergency contact name
 * @param emergencyContactRelationship optional emergency contact relationship
 * @param emergencyContactPhone        optional emergency contact phone
 * @param emergencyContactEmail        optional emergency contact email
 */
public record UpdateClientProfileRequest(
        @NotNull(message = "Version is required")
        Long version,

        @NotBlank(message = "Full name must not be blank")
        @Size(max = 255, message = "Full name must not exceed 255 characters")
        String fullName,

        @Size(max = 255, message = "Preferred name must not exceed 255 characters")
        String preferredName,

        LocalDate dateOfBirth,

        @Size(max = 64, message = "Gender must not exceed 64 characters")
        String sexOrGender,

        @Size(max = 64, message = "Pronouns must not exceed 64 characters")
        String pronouns,

        UUID ownerId,

        UUID assignedTherapistId,

        String notes,

        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @Size(max = 64, message = "Phone must not exceed 64 characters")
        String phone,

        @Size(max = 64, message = "Secondary phone must not exceed 64 characters")
        String secondaryPhone,

        @Size(max = 255, message = "Address line 1 must not exceed 255 characters")
        String addressLine1,

        @Size(max = 255, message = "Address line 2 must not exceed 255 characters")
        String addressLine2,

        @Size(max = 128, message = "City must not exceed 128 characters")
        String city,

        @Size(max = 128, message = "Region must not exceed 128 characters")
        String region,

        @Size(max = 32, message = "Postal code must not exceed 32 characters")
        String postalCode,

        @Size(max = 128, message = "Country must not exceed 128 characters")
        String country,

        @Size(max = 255, message = "Referral source must not exceed 255 characters")
        String referralSource,

        @Size(max = 255, message = "Referral contact name must not exceed 255 characters")
        String referralContactName,

        String referralNotes,

        @Size(max = 20, message = "Preferred communication method must not exceed 20 characters")
        String preferredCommunicationMethod,

        Boolean allowPhone,
        Boolean allowSms,
        Boolean allowEmail,
        Boolean allowVoicemail,

        @Size(max = 255, message = "Emergency contact name must not exceed 255 characters")
        String emergencyContactName,

        @Size(max = 255, message = "Emergency contact relationship must not exceed 255 characters")
        String emergencyContactRelationship,

        @Size(max = 64, message = "Emergency contact phone must not exceed 64 characters")
        String emergencyContactPhone,

        @Size(max = 255, message = "Emergency contact email must not exceed 255 characters")
        String emergencyContactEmail
) {
}
