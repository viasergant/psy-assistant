package com.psyassistant.common.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import com.psyassistant.crm.leads.Lead;
import com.psyassistant.crm.leads.LeadRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Proves the Hibernate {@code orgFilter} is really activated for repository calls,
 * i.e. that {@link TenantHibernateJpaDialect} is wired into the transaction manager.
 */
@SpringBootTest
@ActiveProfiles("test")
class TenantFilterIntegrationTest {

    @Autowired
    private LeadRepository leadRepository;

    @AfterEach
    void cleanUp() {
        TenantContext.clear();
        leadRepository.deleteAllInBatch();
    }

    @Test
    void repositoryQueriesSeeOnlyTheCurrentOrganization() {
        UUID orgA = UUID.randomUUID();
        UUID orgB = UUID.randomUUID();
        leadRepository.save(leadFor("Lead of A", orgA));
        leadRepository.save(leadFor("Lead of B", orgB));

        TenantContext.set(orgA);
        assertThat(leadRepository.findAll()).extracting(Lead::getFullName).containsExactly("Lead of A");

        TenantContext.set(orgB);
        assertThat(leadRepository.findAll()).extracting(Lead::getFullName).containsExactly("Lead of B");
    }

    private static Lead leadFor(final String name, final UUID orgId) {
        Lead lead = new Lead(name);
        lead.setOrgId(orgId);
        return lead;
    }
}
