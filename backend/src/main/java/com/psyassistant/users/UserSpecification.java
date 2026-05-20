package com.psyassistant.users;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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
     * <p>The role filter uses an EXISTS subquery against the {@code user_roles} collection
     * so that users with the specified role among their many roles are correctly matched
     * without producing duplicate rows or corrupting JPA count queries.
     *
     * @param role   when non-null, restricts results to users who have this role among their roles
     * @param active when non-null, restricts results to users matching this active flag
     * @return combined specification (all non-null filters are AND-ed)
     */
    public static Specification<User> withFilters(final UserRole role, final Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (role != null) {
                Subquery<Integer> sub = query.subquery(Integer.class);
                Root<User> subRoot = sub.correlate(root);
                Join<?, ?> roleJoin = subRoot.join("roles", JoinType.INNER);
                sub.select(cb.literal(1));
                sub.where(cb.equal(roleJoin, role));
                predicates.add(cb.exists(sub));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
