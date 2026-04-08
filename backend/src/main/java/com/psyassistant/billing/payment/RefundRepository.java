package com.psyassistant.billing.payment;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link Refund} entities. */
public interface RefundRepository extends JpaRepository<Refund, UUID> {

    List<Refund> findByInvoiceId(UUID invoiceId);
}
