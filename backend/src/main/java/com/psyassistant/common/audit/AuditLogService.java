package com.psyassistant.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes security-event records to the {@code audit_log} table.
 *
 * <p>Each save runs in its own transaction (REQUIRES_NEW) so that a DB failure
 * does not roll back the enclosing business transaction.  Any exception from
 * the save is swallowed and logged as a warning so that audit failures never
 * prevent a login response from reaching the caller.
 */
@Service
public class AuditLogService {

    private static final Logger LOG = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository repository;

    /**
     * Constructs the service with its required repository.
     *
     * @param repository audit log repository
     */
    public AuditLogService(final AuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Persists an audit log entry.
     *
     * <p>Runs in a fresh transaction so DB failures are isolated. Exceptions
     * are caught and logged — they must never bubble up to the caller.
     *
     * @param entry the pre-built audit log record
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(final AuditLog entry) {
        try {
            repository.save(entry);
        } catch (Exception ex) {
            LOG.warn("Failed to persist audit log entry [event={}]: {}",
                    entry.getEventType(), ex.getMessage());
        }
    }
}
