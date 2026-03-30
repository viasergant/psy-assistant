package com.psyassistant.crm.clients.audit;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for client profile audit entries.
 */
public interface ClientProfileAuditEntryRepository
        extends JpaRepository<ClientProfileAuditEntry, UUID> {
}
