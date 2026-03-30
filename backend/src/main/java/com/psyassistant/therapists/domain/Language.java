package com.psyassistant.therapists.domain;

import com.psyassistant.common.audit.SimpleBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Reference entity for languages (e.g., "English", "Spanish", etc.).
 */
@Entity
@Table(name = "language")
public class Language extends SimpleBaseEntity {

    /** Full language name (e.g., "English"). */
    @Column(name = "name", nullable = false, unique = true, length = 128)
    private String name;

    /** ISO 639-1 language code (e.g., "en", "es"). */
    @Column(name = "language_code", nullable = false, unique = true, length = 10)
    private String languageCode;

    // Constructors
    public Language() { }

    public Language(String name, String languageCode) {
        this.name = name;
        this.languageCode = languageCode;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }
}
