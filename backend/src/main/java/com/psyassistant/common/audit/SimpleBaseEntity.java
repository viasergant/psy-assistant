package com.psyassistant.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Lightweight base class for reference/lookup entities.
 *
 * <p>Provides common fields: surrogate UUID primary key, creation timestamp,
 * and last-modification timestamp. Does not track the creator identity,
 * making it suitable for reference data tables that don't require full audit trails.
 *
 * <p>For entities that need creator tracking, use {@link BaseEntity} instead.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class SimpleBaseEntity {

    /** Surrogate primary key, generated as a random UUID on insert. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    protected UUID id;

    /** Timestamp set once at creation; never updated afterwards. */
    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    protected Instant createdAt;

    /** Timestamp updated on every change. */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    protected Instant updatedAt;

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
}
