package com.psyassistant.reporting.caseload;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Composite-key entity that maps a supervisor to a therapist profile.
 *
 * <p>Backed by the {@code supervisor_team_member} table created in V40 migration.
 */
@Entity
@Table(name = "supervisor_team_member")
@IdClass(SupervisorTeamMember.SupervisorTeamMemberId.class)
public class SupervisorTeamMember {

    @Id
    @Column(name = "supervisor_id", nullable = false)
    private UUID supervisorId;

    @Id
    @Column(name = "therapist_profile_id", nullable = false)
    private UUID therapistProfileId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "assigned_by", length = 255)
    private String assignedBy;

    /** Required by JPA. */
    protected SupervisorTeamMember() {
    }

    /**
     * Constructs a new team membership.
     *
     * @param supervisorId       UUID of the supervisor user
     * @param therapistProfileId UUID of the therapist_profile record
     * @param assignedBy         name of the principal that assigned the membership
     */
    public SupervisorTeamMember(
            final UUID supervisorId,
            final UUID therapistProfileId,
            final String assignedBy) {
        this.supervisorId = supervisorId;
        this.therapistProfileId = therapistProfileId;
        this.assignedAt = Instant.now();
        this.assignedBy = assignedBy;
    }

    /**
     * Returns the supervisor user ID.
     *
     * @return supervisor UUID
     */
    public UUID getSupervisorId() {
        return supervisorId;
    }

    /**
     * Returns the therapist profile ID.
     *
     * @return therapist profile UUID
     */
    public UUID getTherapistProfileId() {
        return therapistProfileId;
    }

    /**
     * Returns when this membership was assigned.
     *
     * @return assignment instant
     */
    public Instant getAssignedAt() {
        return assignedAt;
    }

    /**
     * Composite primary key for {@link SupervisorTeamMember}.
     */
    public static class SupervisorTeamMemberId implements Serializable {

        private static final long serialVersionUID = 1L;

        private UUID supervisorId;
        private UUID therapistProfileId;

        /** Required by JPA. */
        public SupervisorTeamMemberId() {
        }

        /**
         * Constructs the composite key.
         *
         * @param supervisorId       supervisor user UUID
         * @param therapistProfileId therapist profile UUID
         */
        public SupervisorTeamMemberId(final UUID supervisorId, final UUID therapistProfileId) {
            this.supervisorId = supervisorId;
            this.therapistProfileId = therapistProfileId;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SupervisorTeamMemberId other)) {
                return false;
            }
            return java.util.Objects.equals(supervisorId, other.supervisorId)
                    && java.util.Objects.equals(therapistProfileId, other.therapistProfileId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(supervisorId, therapistProfileId);
        }
    }
}
