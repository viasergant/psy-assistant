package com.psyassistant.billing.invoice;

import com.psyassistant.billing.invoice.dto.DashboardSummary;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
     * Acquires a pessimistic write lock on an Invoice row for payment registration.
     * Must be called inside a {@code @Transactional} method.
     *
     * @param id the invoice UUID
     * @return locked invoice, or empty if not found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invoice i WHERE i.id = :id")
    Optional<Invoice> findByIdForUpdate(@Param("id") UUID id);

    /**
     * Bulk-update ISSUED and PARTIALLY_PAID invoices past their due date to OVERDUE.
     *
     * @return number of rows updated
     */
    @Modifying
    @Query("""
           UPDATE Invoice i
           SET i.status = com.psyassistant.billing.invoice.InvoiceStatus.OVERDUE
           WHERE i.status IN (
               com.psyassistant.billing.invoice.InvoiceStatus.ISSUED,
               com.psyassistant.billing.invoice.InvoiceStatus.PARTIALLY_PAID)
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

    /**
     * Aggregates finance dashboard summary data: outstanding amounts, overdue amounts,
     * collected-this-month, and aging buckets.
     *
     * @return projection containing all dashboard aggregates
     */
    @Query(value = """
            SELECT
                COALESCE(SUM(CASE WHEN status IN ('ISSUED','OVERDUE','PARTIALLY_PAID')
                    THEN (total - paid_amount) END), 0) AS totalOutstanding,
                COALESCE(SUM(CASE WHEN status = 'OVERDUE'
                    THEN (total - paid_amount) END), 0) AS totalOverdue,
                COALESCE((SELECT SUM(amount) FROM payments
                    WHERE created_at >= date_trunc('month', now())), 0) AS collectedThisMonth,
                COALESCE(SUM(CASE WHEN status IN ('ISSUED','OVERDUE','PARTIALLY_PAID')
                    AND (CURRENT_DATE - due_date) BETWEEN 0 AND 30
                    THEN (total - paid_amount) END), 0) AS aging030,
                COALESCE(SUM(CASE WHEN status IN ('ISSUED','OVERDUE','PARTIALLY_PAID')
                    AND (CURRENT_DATE - due_date) BETWEEN 31 AND 60
                    THEN (total - paid_amount) END), 0) AS aging3160,
                COALESCE(SUM(CASE WHEN status IN ('ISSUED','OVERDUE','PARTIALLY_PAID')
                    AND (CURRENT_DATE - due_date) > 60
                    THEN (total - paid_amount) END), 0) AS aging60plus
            FROM invoices
            WHERE status != 'CANCELLED'
            """, nativeQuery = true)
    DashboardSummary getDashboardSummary();
}
