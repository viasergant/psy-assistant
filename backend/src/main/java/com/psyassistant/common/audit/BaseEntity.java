package com.psyassistant.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Base class for all audited JPA entities.
 *
 * <p>Provides common fields: surrogate UUID primary key, creation timestamp,
 * last-modification timestamp, and the identity of the creator. Subclasses
 * inherit these fields automatically when JPA auditing is enabled via
 * {@link AuditingConfig}.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /** Surrogate primary key, generated as a random UUID on insert. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Timestamp set once at creation; never updated afterwards. */
    @CreatedDate
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    /** Timestamp updated on every change. */
    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    /** Principal name that created the record; never updated afterwards. */
    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    /**
     * Returns the entity's surrogate primary key.
     *
     * @return UUID, or {@code null} before the entity is first persisted
     */
    public UUID getId() {
        return id;
    }

    /**
     * Returns the instant at which the entity was first persisted.
     *
     * @return creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the instant at which the entity was last modified.
     *
     * @return last-modification timestamp
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Returns the name of the principal that created this entity.
     *
     * @return creator name, or {@code null} when no security context was present at creation time
     */
    public String getCreatedBy() {
        return createdBy;
    }
}
