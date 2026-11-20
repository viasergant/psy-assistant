package com.psyassistant.billing.discount;

import com.psyassistant.billing.discount.dto.CreateDiscountRuleRequest;
import com.psyassistant.billing.discount.dto.DiscountRuleResponse;
import com.psyassistant.common.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD service for discount rules. */
@Service
public class DiscountRuleService {

    private static final Logger LOG = LoggerFactory.getLogger(DiscountRuleService.class);

    private final DiscountRuleRepository repository;

    public DiscountRuleService(final DiscountRuleRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('MANAGE_DISCOUNTS')")
    public List<DiscountRuleResponse> list(final UUID clientId, final UUID serviceCatalogId) {
        List<DiscountRule> rules;
        if (clientId != null) {
            rules = repository.findActiveClientRules(clientId);
        } else if (serviceCatalogId != null) {
            rules = repository.findActiveServiceRules(serviceCatalogId);
        } else {
            rules = repository.findAllByOrderByCreatedAtDesc();
        }
        return rules.stream().map(DiscountRuleResponse::from).toList();
    }

    @Transactional
    @PreAuthorize("hasAuthority('MANAGE_DISCOUNTS')")
    public DiscountRuleResponse create(final CreateDiscountRuleRequest request, final String actorName) {
        DiscountRule rule = new DiscountRule(
                request.name(),
                request.type(),
                request.value(),
                request.scope(),
                request.clientId(),
                request.serviceCatalogId()
        );
        rule.setOrgId(TenantContext.get());
        rule.setUpdatedBy(actorName);

        DiscountRule saved = repository.save(rule);
        LOG.info("Discount rule '{}' created by {}", saved.getName(), actorName);
        return DiscountRuleResponse.from(saved);
    }

    @Transactional
    @PreAuthorize("hasAuthority('MANAGE_DISCOUNTS')")
    public DiscountRuleResponse toggleActive(final UUID id, final boolean active, final String actorName) {
        DiscountRule rule = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Discount rule not found: " + id));
        rule.setActive(active);
        rule.setUpdatedBy(actorName);
        LOG.info("Discount rule {} set active={} by {}", id, active, actorName);
        return DiscountRuleResponse.from(repository.save(rule));
    }
}
