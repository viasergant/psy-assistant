package com.psyassistant.common.config;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Versioned AES-256-GCM encryption shared by all field-level JPA converters.
 *
 * <p><strong>Ciphertext format:</strong> {@code enc:<keyId>:Base64(IV || ciphertext+tag)}. The key id
 * lets several keys be active for decryption while only {@code app.encryption.active-key-id}
 * is used for new writes, so keys can be rotated without downtime.
 *
 * <p><strong>Keys:</strong> the key {@code k1} is always the legacy {@code session.notes.encryption-key}.
 * Additional keys are supplied as {@code app.encryption.keys.<id>} (Base64, 32 bytes). Un-prefixed
 * values written before key versioning are decrypted with {@code k1} via {@link #decryptLegacy}.
 */
@Component
public class FieldEncryptor {

    /** Prefix marking a value as encrypted by this class. */
    public static final String PREFIX = "enc:";

    /** Id of the key configured through {@code session.notes.encryption-key}. */
    public static final String LEGACY_KEY_ID = "k1";

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int EXPECTED_KEY_BYTES = 32;

    private final String legacyKeyBase64;
    private final String activeKeyId;
    private final Environment environment;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, SecretKey> keys = new HashMap<>();

    /**
     * Creates the encryptor.
     *
     * @param legacyKeyBase64 Base64 32-byte key, registered as {@value #LEGACY_KEY_ID}
     * @param activeKeyId     id of the key used for new writes
     * @param environment     source of the optional {@code app.encryption.keys.*} map
     */
    public FieldEncryptor(
            @Value("${session.notes.encryption-key}") final String legacyKeyBase64,
            @Value("${app.encryption.active-key-id:" + LEGACY_KEY_ID + "}") final String activeKeyId,
            final Environment environment) {
        this.legacyKeyBase64 = legacyKeyBase64;
        this.activeKeyId = activeKeyId;
        this.environment = environment;
    }

    /**
     * Loads and validates all configured keys; fails fast on a missing, malformed or unknown active key.
     *
     * @throws IllegalStateException if configuration is invalid
     */
    @PostConstruct
    public void init() {
        keys.put(LEGACY_KEY_ID, parseKey(LEGACY_KEY_ID, legacyKeyBase64));
        final Map<String, String> extra = Binder.get(environment)
                .bind("app.encryption.keys", Bindable.mapOf(String.class, String.class))
                .orElse(Map.of());
        extra.forEach((id, value) -> keys.put(id, parseKey(id, value)));
        if (!keys.containsKey(activeKeyId)) {
            throw new IllegalStateException(
                    "app.encryption.active-key-id '" + activeKeyId + "' has no configured key");
        }
    }

    /**
     * Tells whether a stored value carries the encryption prefix.
     *
     * @param value stored column value, may be {@code null}
     * @return {@code true} if the value was produced by {@link #encrypt}
     */
    public boolean isEncrypted(final String value) {
        return value != null && value.startsWith(PREFIX);
    }

    /**
     * Encrypts plaintext with the active key.
     *
     * @param plaintext value to encrypt, not {@code null}
     * @return {@code enc:<keyId>:Base64(IV||ciphertext)}
     */
    public String encrypt(final String plaintext) {
        try {
            final byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            final Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keys.get(activeKeyId), new GCMParameterSpec(GCM_TAG_BITS, iv));
            final byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return PREFIX + activeKeyId + ":" + Base64.getEncoder().encodeToString(concat(iv, ciphertext));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt field value", e);
        }
    }

    /**
     * Decrypts a value produced by {@link #encrypt}.
     *
     * @param stored value starting with {@value #PREFIX}
     * @return plaintext
     */
    public String decrypt(final String stored) {
        final int sep = stored.indexOf(':', PREFIX.length());
        if (sep < 0) {
            throw new IllegalStateException("Malformed encrypted field value");
        }
        final String keyId = stored.substring(PREFIX.length(), sep);
        final SecretKey key = keys.get(keyId);
        if (key == null) {
            throw new IllegalStateException("No encryption key configured for id '" + keyId + "'");
        }
        return decryptWith(key, stored.substring(sep + 1));
    }

    /**
     * Decrypts an un-prefixed value written before key versioning, using the legacy key.
     *
     * @param base64IvAndCipher legacy {@code Base64(IV||ciphertext)}
     * @return plaintext
     */
    public String decryptLegacy(final String base64IvAndCipher) {
        return decryptWith(keys.get(LEGACY_KEY_ID), base64IvAndCipher);
    }

    /**
     * Returns the id of the key used for new writes.
     *
     * @return active key id
     */
    public String activeKeyId() {
        return activeKeyId;
    }

    /**
     * Extracts the key id of an encrypted value, or {@code null} if it is not encrypted.
     *
     * @param stored stored column value
     * @return key id or {@code null}
     */
    public String keyIdOf(final String stored) {
        if (!isEncrypted(stored)) {
            return null;
        }
        final int sep = stored.indexOf(':', PREFIX.length());
        return sep < 0 ? null : stored.substring(PREFIX.length(), sep);
    }

    private String decryptWith(final SecretKey key, final String base64IvAndCipher) {
        try {
            final byte[] ivAndCipher = Base64.getDecoder().decode(base64IvAndCipher);
            final Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, ivAndCipher, 0, GCM_IV_LENGTH));
            final byte[] plaintext = cipher.doFinal(ivAndCipher, GCM_IV_LENGTH, ivAndCipher.length - GCM_IV_LENGTH);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (java.security.GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Failed to decrypt field value", e);
        }
    }

    private static SecretKey parseKey(final String id, final String base64) {
        if (base64 == null || base64.isBlank()) {
            throw new IllegalStateException("Encryption key '" + id + "' must be provided and non-blank");
        }
        final byte[] bytes = Base64.getDecoder().decode(base64.trim());
        if (bytes.length != EXPECTED_KEY_BYTES) {
            throw new IllegalStateException(
                    "Encryption key '" + id + "' must decode to exactly 32 bytes (256 bits); got " + bytes.length);
        }
        return new SecretKeySpec(bytes, "AES");
    }

    private static byte[] concat(final byte[] a, final byte[] b) {
        final byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
