package com.psyassistant.careplans.domain;

import com.psyassistant.common.audit.BaseEntity;
import com.psyassistant.common.config.EncryptedTextConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.Filter;

/**
 * Clinical care plan belonging to one client + therapist pair.
 *
 * <p>Status lifecycle: ACTIVE → CLOSED or ACTIVE → ARCHIVED.
 * Multiple active plans per client are permitted up to the configured maximum.
 */
@Filter(name = "orgFilter", condition = "org_id = :orgId")
@Entity
@Table(name = "care_plan")
public class CarePlan extends BaseEntity {

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Column(name = "therapist_id", nullable = false, updatable = false)
    private UUID therapistId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Convert(converter = EncryptedTextConverter.class)
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CarePlanStatus status = CarePlanStatus.ACTIVE;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by_user_id")
    private UUID closedByUserId;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @OneToMany(mappedBy = "carePlan", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    @OrderBy("priority ASC, created_at ASC")
    private List<CarePlanGoal> goals = new ArrayList<>();

    // ---- constructors ----

    protected CarePlan() { }

    public CarePlan(final UUID clientId, final UUID therapistId,
                    final String title, final String description) {
        this.clientId = clientId;
        this.therapistId = therapistId;
        this.title = title;
        this.description = description;
    }

    // ---- business methods ----

    public void close(final UUID actorId) {
        this.status = CarePlanStatus.CLOSED;
        this.closedAt = Instant.now();
        this.closedByUserId = actorId;
    }

    public void archive() {
        this.status = CarePlanStatus.ARCHIVED;
        this.archivedAt = Instant.now();
    }

    /** Returns true when the plan is still mutatable. */
    public boolean isActive() {
        return CarePlanStatus.ACTIVE == this.status;
    }

    // ---- getters / setters ----

    public UUID getClientId() {
        return clientId;
    }

    public UUID getTherapistId() {
        return therapistId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public CarePlanStatus getStatus() {
        return status;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public UUID getClosedByUserId() {
        return closedByUserId;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public List<CarePlanGoal> getGoals() {
        return goals;
    }

    public UUID getOrgId() {
        return orgId;
    }

    public void setOrgId(final UUID orgId) {
        this.orgId = orgId;
    }
}
