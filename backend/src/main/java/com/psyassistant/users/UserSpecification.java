package com.psyassistant.users;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
        return withFilters(role, active, null);
    }

    /**
     * Builds a {@link Specification} filtering by optional role, active status and a free-text search.
     *
     * <p>The search term is matched case-insensitively as a substring of the email or full name.
     * LIKE wildcards in the term are escaped so they match literally.
     *
     * @param role   when non-null, restricts results to users having this role
     * @param active when non-null, restricts results to users matching this active flag
     * @param search when non-blank, restricts results to users whose email or full name contains it
     * @return combined specification (all non-null filters are AND-ed)
     */
    public static Specification<User> withFilters(
            final UserRole role, final Boolean active, final String search) {
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
            if (search != null && !search.isBlank()) {
                String escaped = search.trim().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                String pattern = "%" + escaped + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("email")), pattern, '\\'),
                        cb.like(cb.lower(root.get("fullName")), pattern, '\\')));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
