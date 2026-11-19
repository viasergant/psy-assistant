package com.psyassistant.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * An independent practice on a shared deployment.
 *
 * <p>Every user, client, and clinical record belongs to exactly one organization.
 * Row-level isolation is enforced at query time via a Hibernate {@code orgFilter}
 * that restricts all queries to the calling user's organization.
 *
 * <p>This entity intentionally does not extend {@code BaseEntity} or
 * {@code SimpleBaseEntity} — organizations can be created by unauthenticated
 * principals during self-service registration, so Spring Data Auditing fields
 * are not applicable.
 */
@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrganizationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrganizationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Required by JPA. */
    protected Organization() {
    }

    /**
     * Creates a new organization with the given attributes.
     *
     * @param name   human-readable display name
     * @param slug   URL-safe unique identifier (lowercase, hyphens)
     * @param type   SOLO or CLINIC
     * @param status initial lifecycle status
     */
    public Organization(final String name, final String slug,
                        final OrganizationType type, final OrganizationStatus status) {
        this.name = name;
        this.slug = slug;
        this.type = type;
        this.status = status;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public OrganizationType getType() {
        return type;
    }

    public OrganizationStatus getStatus() {
        return status;
    }

    public void setStatus(final OrganizationStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
