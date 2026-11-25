package com.psyassistant.common.config;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-shot, idempotent job that brings every encrypted column to the active key.
 *
 * <p>It encrypts rows that still hold plaintext (columns introduced with
 * {@link EncryptedTextConverter}), upgrades legacy un-prefixed ciphertext
 * ({@link EncryptedStringConverter} columns) and re-encrypts rows written with an older key id,
 * which is how key rotation is completed. Rows already on the active key are skipped, so
 * re-running is safe. Enable with {@code app.encryption.backfill.enabled=true}, run once, then
 * disable again. Values are never logged; only counts per column.
 */
@Component
@ConditionalOnProperty(name = "app.encryption.backfill.enabled", havingValue = "true")
public class EncryptionBackfillRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(EncryptionBackfillRunner.class);
    private static final int BATCH_SIZE = 500;
    private static final UUID MIN_UUID = new UUID(0L, 0L);

    /**
     * Column handled by the job.
     *
     * @param table            table name
     * @param column           column name
     * @param legacyCiphertext {@code true} if un-prefixed values are legacy ciphertext, {@code false}
     *                         if they are plaintext
     */
    record ColumnSpec(String table, String column, boolean legacyCiphertext) { }

    static final List<ColumnSpec> COLUMNS = List.of(
            plain("clients", "notes"), plain("clients", "referral_notes"),
            plain("leads", "notes"),
            plain("client_risk_flags", "clinical_note"), plain("client_risk_flags", "resolution_note"),
            plain("care_plan", "description"), plain("care_plan_goal", "description"),
            plain("care_plan_intervention", "description"), plain("care_plan_milestone", "description"),
            plain("goal_progress_note", "note_text"), plain("outcome_measure_entry", "notes"),
            plain("care_plan_audit", "old_value"), plain("care_plan_audit", "new_value"),
            plain("session_record", "session_notes"), plain("attendance_audit_log", "note"),
            plain("appointment", "notes"),
            plain("appointment_audit", "notes"), plain("appointment_audit", "old_value"),
            plain("appointment_audit", "new_value"),
            plain("client_profile_audit_change", "old_value"), plain("client_profile_audit_change", "new_value"),
            legacy("session_note", "content"), legacy("session_note", "structured_fields"),
            legacy("session_note_version", "content"), legacy("session_note_version", "structured_fields"),
            legacy("email_delivery_log", "recipient_address_encrypted"));

    private final JdbcTemplate jdbc;
    private final FieldEncryptor encryptor;

    /**
     * Creates the runner.
     *
     * @param jdbc      JDBC access that bypasses the Hibernate tenant filter on purpose
     * @param encryptor shared versioned encryptor
     */
    public EncryptionBackfillRunner(final JdbcTemplate jdbc, final FieldEncryptor encryptor) {
        this.jdbc = jdbc;
        this.encryptor = encryptor;
    }

    @Override
    public void run(final ApplicationArguments args) {
        LOG.info("Encryption backfill started, active key id={}", encryptor.activeKeyId());
        long total = 0;
        for (final ColumnSpec spec : COLUMNS) {
            final long updated = backfill(spec);
            LOG.info("Encryption backfill {}.{}: {} row(s) updated", spec.table(), spec.column(), updated);
            total += updated;
        }
        LOG.info("Encryption backfill finished, {} row(s) updated", total);
    }

    /**
     * Brings one column to the active key.
     *
     * @param spec column to process
     * @return number of rows rewritten
     */
    long backfill(final ColumnSpec spec) {
        final String select = "SELECT id, " + spec.column() + " AS v FROM " + spec.table()
                + " WHERE id > ? AND " + spec.column() + " IS NOT NULL ORDER BY id LIMIT " + BATCH_SIZE;
        final String update = "UPDATE " + spec.table() + " SET " + spec.column() + " = ? WHERE id = ?";
        long updated = 0;
        UUID last = MIN_UUID;
        while (true) {
            final List<Row> rows = jdbc.query(select,
                    (rs, i) -> new Row(rs.getObject("id", UUID.class), rs.getString("v")), last);
            if (rows.isEmpty()) {
                return updated;
            }
            for (final Row row : rows) {
                if (!encryptor.activeKeyId().equals(encryptor.keyIdOf(row.value()))) {
                    jdbc.update(update, reencrypt(row.value(), spec), row.id());
                    updated++;
                }
                last = row.id();
            }
        }
    }

    private String reencrypt(final String stored, final ColumnSpec spec) {
        final String plaintext;
        if (encryptor.isEncrypted(stored)) {
            plaintext = encryptor.decrypt(stored);
        } else if (spec.legacyCiphertext()) {
            plaintext = encryptor.decryptLegacy(stored);
        } else {
            plaintext = stored;
        }
        return encryptor.encrypt(plaintext);
    }

    private static ColumnSpec plain(final String table, final String column) {
        return new ColumnSpec(table, column, false);
    }

    private static ColumnSpec legacy(final String table, final String column) {
        return new ColumnSpec(table, column, true);
    }

    private record Row(UUID id, String value) { }
}
