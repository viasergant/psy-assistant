package com.psyassistant.crm.leads;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Spring Data JPA repository for {@link Lead} entities.
 *
 * <p>Extends {@link JpaSpecificationExecutor} to support dynamic {@code Specification}-based
 * queries (status/owner filters).
 */
public interface LeadRepository extends JpaRepository<Lead, UUID>, JpaSpecificationExecutor<Lead> {
}
