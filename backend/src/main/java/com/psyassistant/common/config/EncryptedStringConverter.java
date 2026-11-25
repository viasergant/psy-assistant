package com.psyassistant.common.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * JPA {@link AttributeConverter} that transparently encrypts and decrypts {@code TEXT} column
 * values with AES-256-GCM via {@link FieldEncryptor}.
 *
 * <p>Use this converter on columns that were <em>already encrypted</em> before key versioning
 * (session notes, email delivery log). Un-prefixed stored values are treated as legacy
 * {@code Base64(IV||ciphertext)} and decrypted with the legacy key; new writes use the
 * versioned {@code enc:<keyId>:...} format. For columns that held plaintext before encryption
 * was introduced use {@link EncryptedTextConverter}.
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final FieldEncryptor encryptor;

    /**
     * Creates the converter.
     *
     * @param encryptor shared versioned encryptor
     */
    public EncryptedStringConverter(final FieldEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    @Override
    public String convertToDatabaseColumn(final String attribute) {
        return attribute == null ? null : encryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(final String dbData) {
        if (dbData == null) {
            return null;
        }
        return encryptor.isEncrypted(dbData) ? encryptor.decrypt(dbData) : encryptor.decryptLegacy(dbData);
    }
}
