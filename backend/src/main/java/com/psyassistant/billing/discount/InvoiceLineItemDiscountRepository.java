package com.psyassistant.billing.discount;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for {@link InvoiceLineItemDiscount} — append-only snapshots. */
public interface InvoiceLineItemDiscountRepository extends JpaRepository<InvoiceLineItemDiscount, UUID> {

    List<InvoiceLineItemDiscount> findByLineItemId(UUID lineItemId);
}
