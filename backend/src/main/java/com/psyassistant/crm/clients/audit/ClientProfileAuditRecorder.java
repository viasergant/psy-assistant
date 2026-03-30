package com.psyassistant.crm.clients.audit;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Persists immutable field-level audit events for client profile updates.
 */
@Service
public class ClientProfileAuditRecorder {

    /** Event type used for structured profile update operations. */
    private static final String EVENT_TYPE = "PROFILE_UPDATED";

    /** Logical section name for slice-one profile updates. */
    private static final String SECTION = "PROFILE";

    private final ClientProfileAuditEntryRepository auditEntryRepository;

    /**
     * Constructs the recorder with required repository dependency.
     */
    public ClientProfileAuditRecorder(final ClientProfileAuditEntryRepository auditEntryRepository) {
        this.auditEntryRepository = auditEntryRepository;
    }

    /**
     * Records one immutable audit entry for a profile update.
     */
    public void recordProfileUpdate(final UUID clientId,
                                    final UUID actorUserId,
                                    final String actorName,
                                    final List<FieldChange> changes) {
        if (changes == null || changes.isEmpty()) {
            return;
        }

        ClientProfileAuditEntry entry = new ClientProfileAuditEntry(
                clientId,
                actorUserId,
                actorName,
                EVENT_TYPE,
                SECTION,
                null
        );

        for (FieldChange change : changes) {
            entry.addChange(
                    change.fieldName(),
                    safeToString(change.oldValue()),
                    safeToString(change.newValue())
            );
        }

        auditEntryRepository.save(entry);
    }

    private String safeToString(final Object value) {
        return Objects.toString(value, null);
    }

    /**
     * One changed field in a profile update operation.
     */
    public record FieldChange(String fieldName, Object oldValue, Object newValue) {
    }
}
