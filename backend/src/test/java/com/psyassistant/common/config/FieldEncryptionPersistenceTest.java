package com.psyassistant.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import com.psyassistant.crm.leads.Lead;
import com.psyassistant.crm.leads.LeadRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies end to end through Hibernate that a converted column holds ciphertext in the database
 * while the entity still exposes plaintext, using {@code leads.notes} as a representative field.
 */
@SpringBootTest
@ActiveProfiles("test")
class FieldEncryptionPersistenceTest {

    @Autowired
    private LeadRepository leads;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void storesCiphertextButReadsPlaintext() {
        final Lead lead = new Lead("Test Lead");
        lead.setOrgId(UUID.randomUUID());
        lead.setNotes("Panic attacks since March");
        final UUID id = leads.saveAndFlush(lead).getId();

        final String raw = jdbc.queryForObject("SELECT notes FROM leads WHERE id = ?", String.class, id);

        assertThat(raw).startsWith("enc:").doesNotContain("Panic");
        assertThat(leads.findById(id)).get().extracting(Lead::getNotes).isEqualTo("Panic attacks since March");
    }
}
