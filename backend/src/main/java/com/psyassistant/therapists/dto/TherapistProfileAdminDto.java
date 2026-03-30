package com.psyassistant.therapists.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.psyassistant.therapists.domain.Language;
import com.psyassistant.therapists.domain.Specialization;
import com.psyassistant.therapists.domain.TherapistProfile;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Response DTO for therapist profile, projected for admin view (full details).
 */
public record TherapistProfileAdminDto(
    @JsonProperty("id")
    UUID id,

    @JsonProperty("email")
    String email,

    @JsonProperty("name")
    String name,

    @JsonProperty("phone")
    String phone,

    @JsonProperty("employmentStatus")
    String employmentStatus,

    @JsonProperty("bio")
    String bio,

    @JsonProperty("active")
    Boolean active,

    @JsonProperty("version")
    Long version,

    @JsonProperty("specializations")
    List<IdNamePair> specializations,

    @JsonProperty("languages")
    List<IdNamePair> languages,

    @JsonProperty("createdAt")
    Instant createdAt,

    @JsonProperty("createdBy")
    String createdBy,

    @JsonProperty("updatedAt")
    Instant updatedAt
) {
    /**
     * Converts a JPA entity to admin DTO (full access).
     */
    public static TherapistProfileAdminDto fromAdmin(TherapistProfile profile) {
        return new TherapistProfileAdminDto(
            profile.getId(),
            profile.getEmail(),
            profile.getName(),
            profile.getPhone(),
            profile.getEmploymentStatus(),
            profile.getBio(),
            profile.getActive(),
            profile.getVersion(),
            mapSpecializations(profile.getSpecializations()),
            mapLanguages(profile.getLanguages()),
            profile.getCreatedAt(),
            profile.getCreatedBy(),
            profile.getUpdatedAt()
        );
    }

    private static List<IdNamePair> mapSpecializations(Set<Specialization> specs) {
        return specs.stream()
            .map(s -> new IdNamePair(s.getId(), s.getName()))
            .collect(Collectors.toList());
    }

    private static List<IdNamePair> mapLanguages(Set<Language> langs) {
        return langs.stream()
            .map(l -> new IdNamePair(l.getId(), l.getName()))
            .collect(Collectors.toList());
    }
}

/**
 * Simple ID + Name pair for reference entities.
 */
record IdNamePair(
    @JsonProperty("id")
    UUID id,

    @JsonProperty("name")
    String name
) {}
