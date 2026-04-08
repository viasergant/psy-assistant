package com.psyassistant.billing.invoice;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repository for {@link Invoice} entities. */
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Page<Invoice> findByClientId(UUID clientId, Pageable pageable);

    Page<Invoice> findByTherapistId(UUID therapistId, Pageable pageable);

    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    @Query("""
           SELECT i FROM Invoice i
           WHERE (:clientId IS NULL OR i.clientId = :clientId)
             AND (:therapistId IS NULL OR i.therapistId = :therapistId)
             AND (:status IS NULL OR i.status = :status)
           """)
    Page<Invoice> findFiltered(
            @Param("clientId") UUID clientId,
            @Param("therapistId") UUID therapistId,
            @Param("status") InvoiceStatus status,
            Pageable pageable);

    /**
     * Bulk-update ISSUED invoices past their due date to OVERDUE.
     *
     * @return number of rows updated
     */
    @Modifying
    @Query("""
           UPDATE Invoice i
           SET i.status = com.psyassistant.billing.invoice.InvoiceStatus.OVERDUE
           WHERE i.status = com.psyassistant.billing.invoice.InvoiceStatus.ISSUED
             AND i.dueDate < CURRENT_DATE
           """)
    int bulkMarkOverdue();

    /** Returns all ISSUED invoices with a past due date (for logging / notification). */
    @Query("""
           SELECT i FROM Invoice i
           WHERE i.status = com.psyassistant.billing.invoice.InvoiceStatus.ISSUED
             AND i.dueDate < CURRENT_DATE
           """)
    List<Invoice> findOverdueIssuedInvoices();

    /** Returns {@code true} if an invoice already exists for the given session. */
    boolean existsBySessionId(UUID sessionId);
}
