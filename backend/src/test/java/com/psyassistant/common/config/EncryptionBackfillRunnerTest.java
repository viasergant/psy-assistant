package com.psyassistant.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import com.psyassistant.common.config.EncryptionBackfillRunner.ColumnSpec;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.env.MockEnvironment;

class EncryptionBackfillRunnerTest {

    private static final String KEY_1 = Base64.getEncoder().encodeToString(new byte[32]);
    private static final ColumnSpec PLAIN = new ColumnSpec("t_plain", "body", false);
    private static final ColumnSpec LEGACY = new ColumnSpec("t_legacy", "body", true);

    private JdbcTemplate jdbc;
    private FieldEncryptor encryptor;
    private EncryptionBackfillRunner runner;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE t_plain (id UUID PRIMARY KEY, body TEXT)");
        jdbc.execute("CREATE TABLE t_legacy (id UUID PRIMARY KEY, body TEXT)");
        encryptor = new FieldEncryptor(KEY_1, "k1", new MockEnvironment());
        encryptor.init();
        runner = new EncryptionBackfillRunner(jdbc, encryptor);
    }

    private UUID insert(final String table, final String body) {
        final UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO " + table + " (id, body) VALUES (?, ?)", id, body);
        return id;
    }

    private String body(final String table, final UUID id) {
        return jdbc.queryForObject("SELECT body FROM " + table + " WHERE id = ?", String.class, id);
    }

    @Test
    void encryptsPlaintextAndSkipsNulls() {
        final UUID a = insert("t_plain", "secret note");
        final UUID n = insert("t_plain", null);

        assertThat(runner.backfill(PLAIN)).isEqualTo(1);

        assertThat(body("t_plain", a)).startsWith("enc:k1:").doesNotContain("secret");
        assertThat(encryptor.decrypt(body("t_plain", a))).isEqualTo("secret note");
        assertThat(body("t_plain", n)).isNull();
    }

    @Test
    void isIdempotent() {
        final UUID a = insert("t_plain", "x");
        runner.backfill(PLAIN);
        final String first = body("t_plain", a);

        assertThat(runner.backfill(PLAIN)).isZero();
        assertThat(body("t_plain", a)).isEqualTo(first);
    }

    @Test
    void upgradesLegacyCiphertext() {
        final String legacyValue = encryptor.encrypt("legacy text").substring("enc:k1:".length());
        final UUID id = insert("t_legacy", legacyValue);

        assertThat(runner.backfill(LEGACY)).isEqualTo(1);

        assertThat(body("t_legacy", id)).startsWith("enc:k1:");
        assertThat(encryptor.decrypt(body("t_legacy", id))).isEqualTo("legacy text");
    }

    @Test
    void rotatesRowsToTheNewActiveKeyAcrossBatches() {
        final UUID[] ids = new UUID[1200];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = insert("t_plain", encryptor.encrypt("v" + i));
        }
        final String k2 = Base64.getEncoder().encodeToString(new byte[] {
            1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1});
        final FieldEncryptor rotated = new FieldEncryptor(KEY_1, "k2",
                new MockEnvironment().withProperty("app.encryption.keys.k2", k2));
        rotated.init();

        assertThat(new EncryptionBackfillRunner(jdbc, rotated).backfill(PLAIN)).isEqualTo(1200);

        assertThat(body("t_plain", ids[1199])).startsWith("enc:k2:");
        assertThat(rotated.decrypt(body("t_plain", ids[7]))).isEqualTo("v7");
    }
}
