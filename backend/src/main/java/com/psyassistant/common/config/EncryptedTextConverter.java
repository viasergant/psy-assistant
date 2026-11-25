package com.psyassistant.common.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * JPA {@link AttributeConverter} for columns that stored <em>plaintext</em> before encryption
 * was introduced.
 *
 * <p>Writes always produce {@code enc:<keyId>:...}. Reads decrypt prefixed values and pass any
 * other value through unchanged, so rows not yet processed by the backfill
 * ({@code EncryptionBackfillRunner}) stay readable.
 */
@Converter
@Component
public class EncryptedTextConverter implements AttributeConverter<String, String> {

    private final FieldEncryptor encryptor;

    /**
     * Creates the converter.
     *
     * @param encryptor shared versioned encryptor
     */
    public EncryptedTextConverter(final FieldEncryptor encryptor) {
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
        return encryptor.isEncrypted(dbData) ? encryptor.decrypt(dbData) : dbData;
    }
}
