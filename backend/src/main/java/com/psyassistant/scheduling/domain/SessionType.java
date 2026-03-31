package com.psyassistant.scheduling.domain;

import com.psyassistant.common.audit.SimpleBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Lookup table for appointment session types.
 *
 * <p>Defines the different types of sessions that can be scheduled
 * (e.g., in-person, online, intake, follow-up, group therapy).
 *
 * <p>Extends {@link SimpleBaseEntity} to inherit UUID primary key and timestamp auditing.
 */
@Entity
@Table(name = "session_type")
public class SessionType extends SimpleBaseEntity {

    /** Unique code for programmatic reference (e.g., "IN_PERSON", "ONLINE"). */
    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    /** Human-readable display name for UI (e.g., "In-Person Session"). */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** Optional description explaining the session type. */
    @Column(name = "description", length = 500)
    private String description;

    /** Soft delete flag - inactive types are hidden from UI but preserved for historical records. */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Default constructor for JPA.
     */
    protected SessionType() {
    }

    /**
     * Creates a new session type.
     *
     * @param code unique identifier code
     * @param name display name
     * @param description optional explanation
     */
    public SessionType(final String code, final String name, final String description) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.isActive = true;
    }

    // Getters and setters

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(final Boolean isActive) {
        this.isActive = isActive;
    }
}
