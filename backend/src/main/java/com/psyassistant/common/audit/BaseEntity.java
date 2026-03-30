package com.psyassistant.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedBy;

/**
 * Base class for all fully audited JPA entities.
 *
 * <p>Extends {@link SimpleBaseEntity} to inherit UUID primary key, creation timestamp,
 * and last-modification timestamp, then adds creator identity tracking.
 * Subclasses inherit all audit fields automatically when JPA auditing is enabled via
 * {@link AuditingConfig}.
 *
 * <p>For reference/lookup entities that don't need creator tracking,
 * use {@link SimpleBaseEntity} directly.
 */
@MappedSuperclass
public abstract class BaseEntity extends SimpleBaseEntity {

    /** Principal name that created the record; never updated afterwards. */
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    protected String createdBy;

    /**
     * Returns the name of the principal that created this entity.
     *
     * @return creator name, or {@code null} when no security context was present at creation time
     */
    public String getCreatedBy() {
        return createdBy;
    }
}
