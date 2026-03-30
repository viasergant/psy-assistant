package com.psyassistant.therapists.domain;

import com.psyassistant.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Reference entity for service types used in pricing rules
 * (e.g., "Individual Session", "Group Session", "Intake Assessment", etc.).
 */
@Entity
@Table(name = "service_type")
public class ServiceType extends BaseEntity {

    /** Name of the service type (e.g., "Individual Session"). */
    @Column(name = "name", nullable = false, unique = true, length = 128)
    private String name;

    /** Optional detailed description of the service type. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Constructors
    public ServiceType() { }

    public ServiceType(String name, String description) {
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
