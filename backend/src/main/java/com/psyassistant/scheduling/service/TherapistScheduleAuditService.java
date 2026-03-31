package com.psyassistant.scheduling.service;

import com.psyassistant.scheduling.domain.TherapistScheduleAuditEntry;
import com.psyassistant.scheduling.repository.TherapistScheduleAuditEntryRepository;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for recording audit trail of schedule changes.
 *
 * <p>Creates immutable audit entries with field-level change details
 * for all schedule-related mutations (recurring, overrides, leave).
 */
@Service
public class TherapistScheduleAuditService {

    private final TherapistScheduleAuditEntryRepository auditEntryRepository;

    /**
     * Constructs the audit service.
     *
     * @param auditEntryRepository repository for audit entries
     */
    public TherapistScheduleAuditService(final TherapistScheduleAuditEntryRepository auditEntryRepository) {
        this.auditEntryRepository = auditEntryRepository;
    }

    /**
     * Records a schedule change event.
     *
     * @param therapistProfileId therapist profile UUID
     * @param entityType entity type (RECURRING_SCHEDULE, OVERRIDE, LEAVE)
     * @param entityId UUID of the modified entity
     * @param eventType event type (CREATE, UPDATE, DELETE, APPROVE, REJECT)
     * @param requestId optional request correlation ID
     * @return created audit entry
     */
    @Transactional
    public TherapistScheduleAuditEntry recordChange(final UUID therapistProfileId,
                                                      final String entityType,
                                                      final UUID entityId,
                                                      final String eventType,
                                                      final String requestId) {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        final UUID actorUserId = extractUserIdFromAuthentication(authentication);
        final String actorName = authentication != null ? authentication.getName() : "system";

        final var auditEntry = new TherapistScheduleAuditEntry(
            therapistProfileId,
            entityType,
            entityId,
            actorUserId,
            actorName,
            eventType,
            requestId
        );

        return auditEntryRepository.save(auditEntry);
    }

    /**
     * Records a schedule change event with field-level changes.
     *
     * @param therapistProfileId therapist profile UUID
     * @param entityType entity type (RECURRING_SCHEDULE, OVERRIDE, LEAVE)
     * @param entityId UUID of the modified entity
     * @param eventType event type (CREATE, UPDATE, DELETE, APPROVE, REJECT)
     * @param fieldChanges array of field changes (fieldName, oldValue, newValue)
     * @param requestId optional request correlation ID
     * @return created audit entry with changes
     */
    @Transactional
    public TherapistScheduleAuditEntry recordChangeWithDetails(final UUID therapistProfileId,
                                                                 final String entityType,
                                                                 final UUID entityId,
                                                                 final String eventType,
                                                                 final FieldChange[] fieldChanges,
                                                                 final String requestId) {
        final var auditEntry = recordChange(therapistProfileId, entityType, entityId, eventType, requestId);

        for (final FieldChange change : fieldChanges) {
            auditEntry.addChange(change.fieldName(), change.oldValue(), change.newValue());
        }

        return auditEntryRepository.save(auditEntry);
    }

    /**
     * Extracts user ID from Spring Security authentication context.
     *
     * @param authentication authentication object
     * @return user UUID or null if not available
     */
    private UUID extractUserIdFromAuthentication(
            final org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        // Attempt to extract UUID from principal details
        // This assumes JWT contains "sub" claim with user UUID string
        final Object principal = authentication.getPrincipal();
        if (principal instanceof String principalStr) {
            try {
                return UUID.fromString(principalStr);
            } catch (IllegalArgumentException e) {
                // Principal is not a valid UUID, return null
                return null;
            }
        }

        return null;
    }

    /**
     * Record for a single field change.
     */
    public record FieldChange(
        /**
         * Name of the changed field.
         */
        String fieldName,
        /**
         * Previous value as string.
         */
        String oldValue,
        /**
         * New value as string.
         */
        String newValue
    ) {
    }
}
