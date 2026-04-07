package com.psyassistant.careplans.repository;

import com.psyassistant.careplans.domain.CarePlan;
import com.psyassistant.careplans.domain.CarePlanStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Persistence operations for {@link CarePlan}. */
@Repository
public interface CarePlanRepository extends JpaRepository<CarePlan, UUID> {

    List<CarePlan> findByClientIdOrderByCreatedAtDesc(UUID clientId);

    List<CarePlan> findByClientIdAndStatusOrderByCreatedAtDesc(UUID clientId, CarePlanStatus status);

    List<CarePlan> findByTherapistIdOrderByCreatedAtDesc(UUID therapistId);

    @Query("SELECT COUNT(c) FROM CarePlan c WHERE c.clientId = :clientId AND c.status = 'ACTIVE'")
    long countActiveByClientId(@Param("clientId") UUID clientId);
}
