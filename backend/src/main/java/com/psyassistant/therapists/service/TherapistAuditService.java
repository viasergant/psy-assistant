package com.psyassistant.therapists.service;

import com.psyassistant.therapists.domain.TherapistProfileAuditChange;
import com.psyassistant.therapists.domain.TherapistProfileAuditEntry;
import com.psyassistant.therapists.repository.TherapistProfileAuditEntryRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for recording field-level changes to therapist profiles.
 * Emits immutable audit entries and changes.
 */
@Service
public class TherapistAuditService {

    private final TherapistProfileAuditEntryRepository auditEntryRepository;

    public TherapistAuditService(TherapistProfileAuditEntryRepository auditEntryRepository) {
        this.auditEntryRepository = auditEntryRepository;
    }

    /**
     * Records a profile update with field-level change tracking.
     *
     * @param profileId the therapist profile ID
     * @param actorUserId the user ID who performed the action (may be null)
     * @param actorName the display name of the actor
     * @param eventType the type of event (e.g., "CREATE", "UPDATE", "DEACTIVATE")
     * @param fieldChanges list of field changes (old value → new value)
     * @param requestId optional request ID for tracing
     */
    @Transactional
    public void recordAuditEntry(UUID profileId, UUID actorUserId, String actorName,
                                 String eventType, java.util.List<FieldChange> fieldChanges,
                                 String requestId) {
        TherapistProfileAuditEntry entry = new TherapistProfileAuditEntry(
            profileId, actorUserId, actorName, eventType, requestId
        );

        for (FieldChange change : fieldChanges) {
            TherapistProfileAuditChange auditChange = new TherapistProfileAuditChange(
                entry, change.fieldName(), change.oldValue(), change.newValue()
            );
            entry.getChanges().add(auditChange);
        }

        auditEntryRepository.save(entry);
    }

    /**
     * Represents a single field change within an audit event.
     *
     * @param fieldName the name of the field that changed
     * @param oldValue the previous value (null if new)
     * @param newValue the new value (null if deleted)
     */
    public record FieldChange(String fieldName, String oldValue, String newValue) { }
}
