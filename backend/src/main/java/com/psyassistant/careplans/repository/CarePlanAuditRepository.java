package com.psyassistant.careplans.repository;

import com.psyassistant.careplans.domain.CarePlanAudit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Append-only repository for {@link CarePlanAudit} entries. No update or delete operations. */
@Repository
public interface CarePlanAuditRepository extends JpaRepository<CarePlanAudit, UUID> {

    Page<CarePlanAudit> findByCarePlanIdOrderByActionTimestampDesc(UUID carePlanId, Pageable pageable);

    List<CarePlanAudit> findByCarePlanIdOrderByActionTimestampDesc(UUID carePlanId);
}
