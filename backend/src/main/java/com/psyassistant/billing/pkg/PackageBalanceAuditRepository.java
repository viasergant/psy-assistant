package com.psyassistant.billing.pkg;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link PackageBalanceAudit} — append-only log. */
public interface PackageBalanceAuditRepository extends JpaRepository<PackageBalanceAudit, UUID> {
}
