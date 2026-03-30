package com.psyassistant.therapists.dto;

import com.psyassistant.users.dto.UserCreationResponseDto;

/**
 * Response DTO for creating a therapist account and profile atomically.
 * Contains both the user creation details (including temporary password)
 * and the created therapist profile.
 */
public record TherapistWithAccountResponseDto(
    UserCreationResponseDto userDetails,
    TherapistProfileAdminDto therapistProfile
) { }
