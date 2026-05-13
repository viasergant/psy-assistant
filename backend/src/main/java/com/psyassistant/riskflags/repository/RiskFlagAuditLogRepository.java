package com.psyassistant.riskflags.repository;

import com.psyassistant.riskflags.domain.RiskFlagAuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Append-only repository for {@link RiskFlagAuditLog} entries.
 *
 * <p>Only {@code save()} and read operations are exposed. No update or delete
 * methods are declared at this layer or any layer above it, enforcing the
 * immutability of the audit trail.
 */
@Repository
public interface RiskFlagAuditLogRepository extends JpaRepository<RiskFlagAuditLog, UUID> {

    /**
     * Returns the full audit history for a client's risk flags, newest first.
     *
     * @param clientId client to query
     * @return audit entries ordered by action timestamp descending
     */
    List<RiskFlagAuditLog> findAllByClientIdOrderByActionTimestampDesc(UUID clientId);
}
