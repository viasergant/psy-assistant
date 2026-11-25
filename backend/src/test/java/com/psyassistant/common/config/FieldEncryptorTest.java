package com.psyassistant.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class FieldEncryptorTest {

    private static final String KEY_1 = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String KEY_2 = Base64.getEncoder().encodeToString(filled((byte) 7));

    private static byte[] filled(final byte value) {
        final byte[] bytes = new byte[32];
        java.util.Arrays.fill(bytes, value);
        return bytes;
    }

    private static FieldEncryptor encryptor(final String activeId, final MockEnvironment env) {
        final FieldEncryptor fe = new FieldEncryptor(KEY_1, activeId, env);
        fe.init();
        return fe;
    }

    @Test
    void roundTripsUnicodeWithVersionedPrefix() {
        final FieldEncryptor fe = encryptor("k1", new MockEnvironment());

        final String stored = fe.encrypt("Тривога, panic ✓");

        assertThat(stored).startsWith("enc:k1:").doesNotContain("Тривога");
        assertThat(fe.decrypt(stored)).isEqualTo("Тривога, panic ✓");
    }

    @Test
    void usesFreshIvPerWrite() {
        final FieldEncryptor fe = encryptor("k1", new MockEnvironment());

        assertThat(fe.encrypt("same")).isNotEqualTo(fe.encrypt("same"));
    }

    @Test
    void decryptsOldKeyAfterRotation() {
        final FieldEncryptor before = encryptor("k1", new MockEnvironment());
        final String oldValue = before.encrypt("secret");

        final MockEnvironment env = new MockEnvironment().withProperty("app.encryption.keys.k2", KEY_2);
        final FieldEncryptor after = encryptor("k2", env);

        assertThat(after.decrypt(oldValue)).isEqualTo("secret");
        assertThat(after.encrypt("new")).startsWith("enc:k2:");
        assertThat(after.keyIdOf(oldValue)).isEqualTo("k1");
    }

    @Test
    void decryptsLegacyUnprefixedValue() throws Exception {
        final byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        final Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(new byte[32], "AES"), new GCMParameterSpec(128, iv));
        final byte[] ct = c.doFinal("legacy".getBytes(StandardCharsets.UTF_8));
        final byte[] all = new byte[iv.length + ct.length];
        System.arraycopy(iv, 0, all, 0, iv.length);
        System.arraycopy(ct, 0, all, iv.length, ct.length);
        final String legacy = Base64.getEncoder().encodeToString(all);

        final FieldEncryptor fe = encryptor("k1", new MockEnvironment());

        assertThat(new EncryptedStringConverter(fe).convertToEntityAttribute(legacy)).isEqualTo("legacy");
    }

    @Test
    void failsOnUnknownKeyIdAndTamperedData() {
        final FieldEncryptor fe = encryptor("k1", new MockEnvironment());
        final String stored = fe.encrypt("x");

        assertThatThrownBy(() -> fe.decrypt(stored.replace("enc:k1:", "enc:zz:")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("zz");
        assertThatThrownBy(() -> fe.decrypt(stored.substring(0, stored.length() - 4) + "AAAA"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void failsFastOnBadConfiguration() {
        final MockEnvironment env = new MockEnvironment();
        assertThatThrownBy(() -> encryptor("missing", env)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new FieldEncryptor("", "k1", env).init())
                .isInstanceOf(IllegalStateException.class);
        final String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> new FieldEncryptor(shortKey, "k1", env).init())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("32 bytes");
    }

    @Test
    void textConverterPassesPlaintextThroughAndEncryptsWrites() {
        final EncryptedTextConverter conv = new EncryptedTextConverter(encryptor("k1", new MockEnvironment()));

        assertThat(conv.convertToEntityAttribute("old plaintext")).isEqualTo("old plaintext");
        assertThat(conv.convertToDatabaseColumn(null)).isNull();
        assertThat(conv.convertToEntityAttribute(null)).isNull();
        final String stored = conv.convertToDatabaseColumn("note");
        assertThat(stored).startsWith("enc:");
        assertThat(conv.convertToEntityAttribute(stored)).isEqualTo("note");
    }
}
