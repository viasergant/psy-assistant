package com.psyassistant.billing.discount;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repository for {@link DiscountRule}. */
public interface DiscountRuleRepository extends JpaRepository<DiscountRule, UUID> {

    List<DiscountRule> findAllByOrderByCreatedAtDesc();

    /** Returns active rules applicable to the given client (CLIENT scope). */
    @Query("""
           SELECT r FROM DiscountRule r
           WHERE r.active = TRUE
             AND r.scope = 'CLIENT'
             AND r.clientId = :clientId
           """)
    List<DiscountRule> findActiveClientRules(@Param("clientId") UUID clientId);

    /** Returns active rules applicable to the given service catalog entry (SERVICE scope). */
    @Query("""
           SELECT r FROM DiscountRule r
           WHERE r.active = TRUE
             AND r.scope = 'SERVICE'
             AND r.serviceCatalogId = :serviceCatalogId
           """)
    List<DiscountRule> findActiveServiceRules(@Param("serviceCatalogId") UUID serviceCatalogId);
}
