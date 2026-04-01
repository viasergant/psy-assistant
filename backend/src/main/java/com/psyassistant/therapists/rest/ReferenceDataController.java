package com.psyassistant.therapists.rest;

import com.psyassistant.therapists.dto.IdNameDto;
import com.psyassistant.therapists.repository.LanguageRepository;
import com.psyassistant.therapists.repository.SpecializationRepository;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for reference data (specializations, languages).
 * These are lookup/dropdown values used when creating/editing therapist profiles.
 */
@RestController
@RequestMapping("/api/v1")
public class ReferenceDataController {

    private final SpecializationRepository specializationRepository;
    private final LanguageRepository languageRepository;

    /**
     * Constructs a new ReferenceDataController.
     *
     * @param specializationRepository the specialization repository
     * @param languageRepository the language repository
     */
    public ReferenceDataController(final SpecializationRepository specializationRepository,
                                   final LanguageRepository languageRepository) {
        this.specializationRepository = specializationRepository;
        this.languageRepository = languageRepository;
    }

    /**
     * GET /api/v1/specializations
     * Returns all available specializations for dropdown lists.
     *
     * <p>Required role: SYSTEM_ADMINISTRATOR (for creating therapist profiles)
     *
     * @return 200 OK with list of specializations
     */
    @GetMapping("/specializations")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR') or hasRole('ADMIN')")
    public ResponseEntity<List<IdNameDto>> getSpecializations() {
        List<IdNameDto> specializations = specializationRepository.findAll().stream()
            .map(spec -> IdNameDto.from(spec.getId(), spec.getName()))
            .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
            .toList();
        return ResponseEntity.ok(specializations);
    }

    /**
     * GET /api/v1/languages
     * Returns all available languages for dropdown lists.
     *
     * <p>Required role: SYSTEM_ADMINISTRATOR (for creating therapist profiles)
     *
     * @return 200 OK with list of languages
     */
    @GetMapping("/languages")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR') or hasRole('ADMIN')")
    public ResponseEntity<List<IdNameDto>> getLanguages() {
        List<IdNameDto> languages = languageRepository.findAll().stream()
            .map(lang -> IdNameDto.from(lang.getId(), lang.getName()))
            .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
            .toList();
        return ResponseEntity.ok(languages);
    }
}
