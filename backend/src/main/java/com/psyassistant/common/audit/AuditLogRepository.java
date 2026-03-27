package com.psyassistant.common.audit;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link AuditLog} entries.
 *
 * <p>Audit rows are append-only; no update or delete operations are provided.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}
