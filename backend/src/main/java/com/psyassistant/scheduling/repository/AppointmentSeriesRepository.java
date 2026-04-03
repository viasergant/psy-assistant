package com.psyassistant.scheduling.repository;

import com.psyassistant.scheduling.domain.AppointmentSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link AppointmentSeries} entity.
 *
 * <p>Uses {@code Long} as the ID type because the series table uses a BIGSERIAL PK
 * (not UUID) to keep FK joins compact across potentially large appointment tables.
 */
@Repository
public interface AppointmentSeriesRepository extends JpaRepository<AppointmentSeries, Long> {
}
