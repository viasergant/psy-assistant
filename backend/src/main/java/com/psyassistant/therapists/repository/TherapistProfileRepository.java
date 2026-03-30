package com.psyassistant.therapists.repository;

import com.psyassistant.therapists.domain.TherapistProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link TherapistProfile} persistence operations.
 */
@Repository
public interface TherapistProfileRepository extends JpaRepository<TherapistProfile, UUID> {

    /**
     * Finds a therapist profile by email address (case-insensitive).
     *
     * @param email the email to search for
     * @return an Optional containing the profile if found
     */
    Optional<TherapistProfile> findByEmailIgnoreCase(String email);

    /**
     * Checks if a therapist with the given email already exists.
     *
     * @param email the email to check
     * @return true if exists, false otherwise
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Checks if a therapist with the given email exists, excluding a specific ID.
     * Used for update validation to allow users to keep their own email.
     *
     * @param email the email to check
     * @param profileId the profile ID to exclude from the check
     * @return true if another profile with this email exists
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM TherapistProfile p WHERE LOWER(p.email) = LOWER(:email) AND p.id != :profileId")
    boolean existsByEmailIgnoreCaseExcludingId(@Param("email") String email, @Param("profileId") UUID profileId);

    /**
     * Finds all therapist profiles with pagination, eagerly fetching specializations and languages
     * to avoid lazy initialization exceptions when mapping to DTOs.
     *
     * @param pageable pagination information
     * @return a page of therapist profiles with specializations and languages loaded
     */
    @EntityGraph(attributePaths = {"specializations", "languages"})
    Page<TherapistProfile> findAllBy(Pageable pageable);
}
