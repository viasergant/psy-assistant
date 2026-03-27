package com.psyassistant.crm.leads;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specifications for dynamic filtering of {@link Lead} entities.
 *
 * <p>Used by {@link LeadService} to build paginated, filterable queries without loading
 * all leads into memory.
 */
public final class LeadSpecification {

    private LeadSpecification() {
    }

    /**
     * Builds a {@link Specification} that filters leads by optional status, owner, and archive
     * visibility.
     *
     * @param status          when non-null, restricts results to leads with this status
     * @param ownerId         when non-null, restricts results to leads owned by this user
     * @param includeArchived when false (default), INACTIVE leads are excluded
     * @return combined specification (all non-null filters are AND-ed)
     */
    public static Specification<Lead> withFilters(
            final LeadStatus status,
            final UUID ownerId,
            final boolean includeArchived) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (ownerId != null) {
                predicates.add(cb.equal(root.get("ownerId"), ownerId));
            }

            if (!includeArchived) {
                predicates.add(cb.notEqual(root.get("status"), LeadStatus.INACTIVE));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
