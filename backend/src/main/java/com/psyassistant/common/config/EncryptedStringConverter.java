package com.psyassistant.common.config;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JPA {@link AttributeConverter} that transparently encrypts and decrypts {@code TEXT}
 * column values using AES-256-GCM.
 *
 * <p><strong>Encryption scheme:</strong> For each plaintext write the converter generates a
 * fresh 12-byte IV, encrypts with AES/GCM/NoPadding (128-bit auth tag), and stores the
 * result as {@code Base64(IV || ciphertext)}. On read it splits off the IV and decrypts.
 *
 * <p><strong>Key supply:</strong> The raw key must be a Base64-encoded 32-byte value
 * (256 bits) supplied via {@code session.notes.encryption-key}.  The converter fails fast at
 * startup if the key is absent or of the wrong length.
 *
 * <p>Apply with {@code @Convert(converter = EncryptedStringConverter.class)} on any
 * {@code String} entity field that should be encrypted at rest.
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int EXPECTED_KEY_BYTES = 32;

    private final String encryptionKeyBase64;
    private SecretKey secretKey;

    /**
     * Constructs the converter with the supplied Base64-encoded encryption key.
     *
     * @param encryptionKeyBase64 Base64-encoded 32-byte AES-256 key
     */
    public EncryptedStringConverter(
            @Value("${session.notes.encryption-key}") final String encryptionKeyBase64) {
        this.encryptionKeyBase64 = encryptionKeyBase64;
    }

    /**
     * Decodes and validates the encryption key at application startup.
     *
     * @throws IllegalStateException if the key is missing or not exactly 32 bytes
     */
    @PostConstruct
    public void init() {
        if (encryptionKeyBase64 == null || encryptionKeyBase64.isBlank()) {
            throw new IllegalStateException(
                    "session.notes.encryption-key must be provided and non-blank");
        }
        final byte[] keyBytes = Base64.getDecoder().decode(encryptionKeyBase64.trim());
        if (keyBytes.length != EXPECTED_KEY_BYTES) {
            throw new IllegalStateException(
                    "session.notes.encryption-key must decode to exactly 32 bytes (256 bits); "
                    + "got " + keyBytes.length);
        }
        secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Encrypts the plaintext attribute value before writing to the database.
     *
     * @param attribute plaintext string, may be {@code null}
     * @return {@code null} if attribute is {@code null}, otherwise {@code Base64(IV||ciphertext)}
     */
    @Override
    public String convertToDatabaseColumn(final String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            final byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            final Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            final byte[] ciphertext = cipher.doFinal(attribute.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            final byte[] ivAndCipher = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, ivAndCipher, 0, iv.length);
            System.arraycopy(ciphertext, 0, ivAndCipher, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(ivAndCipher);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt session note content", e);
        }
    }

    /**
     * Decrypts the database column value when reading an entity.
     *
     * @param dbData {@code Base64(IV||ciphertext)} string from the database, may be {@code null}
     * @return plaintext string, or {@code null} if dbData is {@code null}
     */
    @Override
    public String convertToEntityAttribute(final String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            final byte[] ivAndCipher = Base64.getDecoder().decode(dbData);

            final byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(ivAndCipher, 0, iv, 0, GCM_IV_LENGTH);

            final byte[] ciphertext = new byte[ivAndCipher.length - GCM_IV_LENGTH];
            System.arraycopy(ivAndCipher, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

            final Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            final byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt session note content", e);
        }
    }
}
