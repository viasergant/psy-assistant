package com.psyassistant.reporting.caseload;

import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link SupervisorTeamMember} persistence operations.
 */
@Repository
public interface SupervisorTeamRepository
        extends JpaRepository<SupervisorTeamMember, SupervisorTeamMember.SupervisorTeamMemberId> {

    /**
     * Returns the set of therapist profile IDs that belong to the given supervisor's team.
     *
     * @param supervisorId the supervisor user UUID
     * @return set of therapist profile UUIDs in the supervisor's team
     */
    @Query("SELECT m.therapistProfileId FROM SupervisorTeamMember m WHERE m.supervisorId = :supervisorId")
    Set<UUID> findTherapistIdsBySupervisorId(@Param("supervisorId") UUID supervisorId);
}
