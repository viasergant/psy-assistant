package com.psyassistant.riskflags.service;

import com.psyassistant.riskflags.domain.RiskFlagType;
import com.psyassistant.riskflags.dto.RiskFlagTypeResponse;
import com.psyassistant.riskflags.repository.RiskFlagTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for risk flag type configuration management.
 *
 * <p>RBAC rules:
 * <ul>
 *   <li>{@code listActive()} — no permission check; all authenticated staff may call this to
 *       populate the create-flag dropdown.</li>
 *   <li>{@code listAll()}, {@code create()}, {@code deactivate()} — require
 *       {@code MANAGE_RISK_FLAG_TYPES} (SYSTEM_ADMINISTRATOR only).</li>
 * </ul>
 */
@Service
public class RiskFlagTypeService {

    private final RiskFlagTypeRepository flagTypeRepository;

    public RiskFlagTypeService(final RiskFlagTypeRepository flagTypeRepository) {
        this.flagTypeRepository = flagTypeRepository;
    }

    // =========================================================================
    // Queries
    // =========================================================================

    /**
     * Returns all active flag types ordered by {@code displayOrder} ascending.
     * No permission check — intended for the create-flag type dropdown.
     */
    @Transactional(readOnly = true)
    public List<RiskFlagTypeResponse> listActive() {
        return flagTypeRepository.findAllByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns all flag types (active and inactive).
     * Requires {@code MANAGE_RISK_FLAG_TYPES}.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('MANAGE_RISK_FLAG_TYPES')")
    public List<RiskFlagTypeResponse> listAll() {
        requireAuthority("MANAGE_RISK_FLAG_TYPES");
        return flagTypeRepository.findAll(Sort.by(Sort.Direction.ASC, "displayOrder")).stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================================
    // Mutations
    // =========================================================================

    /**
     * Creates a new active flag type.
     * Requires {@code MANAGE_RISK_FLAG_TYPES}.
     *
     * @param name         unique display label (max 100 chars)
     * @param displayOrder relative sort position
     * @return the persisted flag type as a response DTO
     */
    @Transactional
    @PreAuthorize("hasAuthority('MANAGE_RISK_FLAG_TYPES')")
    public RiskFlagTypeResponse create(final String name, final int displayOrder) {
        requireAuthority("MANAGE_RISK_FLAG_TYPES");
        final RiskFlagType flagType = new RiskFlagType(name, (short) displayOrder);
        return toResponse(flagTypeRepository.save(flagType));
    }

    /**
     * Soft-deletes a flag type so it no longer appears in the creation form.
     * Requires {@code MANAGE_RISK_FLAG_TYPES}.
     *
     * @param id UUID of the flag type to deactivate
     * @return the updated flag type as a response DTO
     * @throws EntityNotFoundException if the flag type is not found
     */
    @Transactional
    @PreAuthorize("hasAuthority('MANAGE_RISK_FLAG_TYPES')")
    public RiskFlagTypeResponse deactivate(final UUID id) {
        requireAuthority("MANAGE_RISK_FLAG_TYPES");
        final RiskFlagType flagType = flagTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Risk flag type not found: " + id));
        flagType.deactivate();
        return toResponse(flagTypeRepository.save(flagType));
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void requireAuthority(final String authority) {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final boolean hasAuthority = auth != null && auth.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
        if (!hasAuthority) {
            throw new AccessDeniedException("Access denied: missing authority " + authority);
        }
    }

    private RiskFlagTypeResponse toResponse(final RiskFlagType flagType) {
        return new RiskFlagTypeResponse(
                flagType.getId(),
                flagType.getName(),
                flagType.getDisplayOrder(),
                flagType.isActive()
        );
    }
}
