package com.psyassistant.billing.invoice;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Provides SELECT ... FOR UPDATE on the {@code invoice_number_seq} row
 * used for race-safe invoice number generation.
 */
public interface InvoiceNumberSeqRepository extends JpaRepository<InvoiceNumberSeq, Short> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InvoiceNumberSeq s WHERE s.invoiceYear = :year")
    Optional<InvoiceNumberSeq> findForUpdateByYear(@Param("year") short year);

    @Modifying
    @Query("UPDATE InvoiceNumberSeq s SET s.lastSeq = :seq WHERE s.invoiceYear = :year")
    void updateSeq(@Param("year") short year, @Param("seq") int seq);
}
