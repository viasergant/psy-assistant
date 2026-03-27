package com.psyassistant.users;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specifications for dynamic filtering of {@link User} entities.
 *
 * <p>Used by {@code UserManagementService} to build paginated, filterable queries without
 * loading all users into memory.
 */
public final class UserSpecification {

    private UserSpecification() {
    }

    /**
     * Builds a {@link Specification} that filters users by optional role and/or active status.
     *
     * @param role   when non-null, restricts results to users with this role
     * @param active when non-null, restricts results to users matching this active flag
     * @return combined specification (all non-null filters are AND-ed)
     */
    public static Specification<User> withFilters(final UserRole role, final Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
