package com.psyassistant.therapists.domain;

import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Reference entity for therapist specializations (e.g., "Depression", "Anxiety", etc.).
 */
@Entity
@Table(name = "specialization")
public class Specialization extends BaseEntity {

    /** Name of the specialization (e.g., "Cognitive Behavioral Therapy"). */
    @Column(name = "name", nullable = false, unique = true, length = 128)
    private String name;

    /** Optional detailed description of the specialization. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Constructors
    public Specialization() {}

    public Specialization(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
