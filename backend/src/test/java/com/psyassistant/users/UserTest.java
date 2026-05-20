package com.psyassistant.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link User}.
 *
 * <p>Covers: constructors, role collection methods (getRoles, setRoles, addRole,
 * removeRole, hasRole), deprecated bridge methods, invariant enforcement, and
 * updatedAt timestamp refresh.
 */
class UserTest {

    // ---- constructor: multi-role (fullName variant) ----------------------

    @Test
    void constructorWithSetAndFullNamePopulatesRoles() {
        User user = new User("a@example.com", "hash", "Alice",
                Set.of(UserRole.THERAPIST), true);

        assertThat(user.getRoles()).containsExactly(UserRole.THERAPIST);
        assertThat(user.getEmail()).isEqualTo("a@example.com");
        assertThat(user.getFullName()).isEqualTo("Alice");
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void constructorCanonalizesLegacyAdminRoleToSystemAdministrator() {
        User user = new User("a@example.com", "hash", "Alice",
                Set.of(UserRole.ADMIN, UserRole.THERAPIST), true);

        assertThat(user.getRoles()).contains(UserRole.SYSTEM_ADMINISTRATOR, UserRole.THERAPIST);
        assertThat(user.getRoles()).doesNotContain(UserRole.ADMIN);
    }

    @Test
    void constructorThrowsIllegalArgumentForEmptyRolesSet() {
        assertThatThrownBy(() ->
                new User("a@example.com", "hash", "Alice", Set.of(), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("roles must not be empty");
    }

    @Test
    void constructorThrowsIllegalArgumentForNullRoles() {
        assertThatThrownBy(() ->
                new User("a@example.com", "hash", "Alice", (Set<UserRole>) null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("roles must not be empty");
    }

    // ---- constructor: multi-role (no fullName variant) ------------------

    @Test
    void constructorWithSetAndNoFullNamePopulatesRolesAndSetsFullNameNull() {
        User user = new User("a@example.com", "hash", Set.of(UserRole.FINANCE), true);

        assertThat(user.getRoles()).containsExactly(UserRole.FINANCE);
        assertThat(user.getFullName()).isNull();
    }

    // ---- constructor: deprecated single-role ----------------------------

    @Test
    void deprecatedSingleRoleConstructorSetsOneRole() {
        @SuppressWarnings("deprecation")
        User user = new User("a@example.com", "hash", UserRole.SUPERVISOR, true);

        assertThat(user.getRoles()).containsExactly(UserRole.SUPERVISOR);
    }

    @Test
    void deprecatedSingleRoleConstructorCanonalizesLegacyAdmin() {
        @SuppressWarnings("deprecation")
        User user = new User("a@example.com", "hash", UserRole.ADMIN, true);

        assertThat(user.getRoles()).containsExactly(UserRole.SYSTEM_ADMINISTRATOR);
    }

    @Test
    void deprecatedFullNameConstructorSetsOneRoleAndFullName() {
        @SuppressWarnings("deprecation")
        User user = new User("a@example.com", "hash", "Alice", UserRole.THERAPIST, true);

        assertThat(user.getRoles()).containsExactly(UserRole.THERAPIST);
        assertThat(user.getFullName()).isEqualTo("Alice");
    }

    // ---- getRoles (unmodifiable) ----------------------------------------

    @Test
    void getRolesReturnsUnmodifiableView() {
        User user = new User("a@example.com", "hash", Set.of(UserRole.THERAPIST), true);

        assertThatThrownBy(() -> user.getRoles().add(UserRole.SUPERVISOR))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // ---- setRoles -------------------------------------------------------

    @Test
    void setRolesReplacesExistingRoles() {
        User user = new User("a@example.com", "hash", Set.of(UserRole.THERAPIST), true);

        user.setRoles(Set.of(UserRole.SUPERVISOR, UserRole.FINANCE));

        assertThat(user.getRoles()).containsExactlyInAnyOrder(UserRole.SUPERVISOR, UserRole.FINANCE);
    }

    @Test
    void setRolesThrowsIllegalArgumentForEmptySet() {
        User user = new User("a@example.com", "hash", Set.of(UserRole.THERAPIST), true);

        assertThatThrownBy(() -> user.setRoles(Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("roles must not be empty");
    }

    @Test
    void setRolesThrowsIllegalArgumentForNull() {
        User user = new User("a@example.com", "hash", Set.of(UserRole.THERAPIST), true);

        assertThatThrownBy(() -> user.setRoles(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("roles must not be empty");
    }

    @Test
    void setRolesUpdatesUpdatedAt() {
        User user = new User("a@example.com", "hash", Set.of(UserRole.THERAPIST), true);
        Instant before = user.getUpdatedAt();

        user.setRoles(Set.of(UserRole.FINANCE));

        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(before);
    }

    // ---- addRole --------------------------------------------------------

    @Test
    void addRoleAddsNewRoleToSet() {
        User user = new User("a@example.com", "hash", Set.of(UserRole.THERAPIST), true);

        user.addRole(UserRole.SUPERVISOR);

        assertThat(user.getRoles()).containsExactlyInAnyOrder(UserRole.THERAPIST, UserRole.SUPERVISOR);
    }

    @Test
    void addRoleIsIdempotentForExistingRole() {
        User user = new User("a@example.com", "hash", Set.of(UserRole.THERAPIST), true);

        user.addRole(UserRole.THERAPIST);

        assertThat(user.getRoles()).containsExactly(UserRole.THERAPIST);
    }

    @Test
    void addRoleUpdatesUpdatedAt() {
        User user = new User("a@example.com", "hash", Set.of(UserRole.THERAPIST), true);
        Instant before = user.getUpdatedAt();

        user.addRole(UserRole.SUPERVISOR);

        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(before);
    }

    @Test
    void addRoleCanonicalizesLegacyAdminAlias() {
        User user = new User("a@example.com", "hash", Set.of(UserRole.THERAPIST), true);

        user.addRole(UserRole.ADMIN);

        assertThat(user.getRoles()).containsExactlyInAnyOrder(UserRole.THERAPIST, UserRole.SYSTEM_ADMINISTRATOR);
        assertThat(user.getRoles()).doesNotContain(UserRole.ADMIN);
    }

    // ---- removeRole -----------------------------------------------------

    @Test
    void removeRoleRemovesFromSetWhenMultipleRolesExist() {
        User user = new User("a@example.com", "hash",
                new LinkedHashSet<>(Set.of(UserRole.THERAPIST, UserRole.SUPERVISOR)), true);

        user.removeRole(UserRole.SUPERVISOR);

        assertThat(user.getRoles()).containsExactly(UserRole.THERAPIST);
    }

    @Test
    void removeRoleThrowsIllegalStateWhenRemovingLastRole() {
        User user = new User("a@example.com", "hash", Set.of(UserRole.THERAPIST), true);

        assertThatThrownBy(() -> user.removeRole(UserRole.THERAPIST))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("user must have at least one role");
    }

    @Test
    void removeRoleDoesNothingForAbsentRole() {
        User user = new User("a@example.com", "hash", Set.of(UserRole.THERAPIST), true);

        user.removeRole(UserRole.FINANCE);

        assertThat(user.getRoles()).containsExactly(UserRole.THERAPIST);
    }

    @Test
    void removeRoleUpdatesUpdatedAt() {
        User user = new User("a@example.com", "hash",
                new LinkedHashSet<>(Set.of(UserRole.THERAPIST, UserRole.SUPERVISOR)), true);
        Instant before = user.getUpdatedAt();

        user.removeRole(UserRole.SUPERVISOR);

        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(before);
    }

    @Test
    void removeRoleCanonicalizesLegacyAdminAliasBeforeRemoving() {
        User user = new User("a@example.com", "hash",
                new LinkedHashSet<>(Set.of(UserRole.SYSTEM_ADMINISTRATOR, UserRole.THERAPIST)), true);

        user.removeRole(UserRole.ADMIN);

        assertThat(user.getRoles()).containsExactly(UserRole.THERAPIST);
        assertThat(user.getRoles()).doesNotContain(UserRole.SYSTEM_ADMINISTRATOR);
    }

    @Test
    void removeRoleCanonicalizesLegacyAdminAliasForLastRoleGuard() {
        User user = new User("a@example.com", "hash", Set.of(UserRole.SYSTEM_ADMINISTRATOR), true);

        assertThatThrownBy(() -> user.removeRole(UserRole.ADMIN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("user must have at least one role");
    }

    // ---- hasRole --------------------------------------------------------

    @Test
    void hasRoleReturnsTrueForAssignedRole() {
        User user = new User("a@example.com", "hash", Set.of(UserRole.THERAPIST), true);

        assertThat(user.hasRole(UserRole.THERAPIST)).isTrue();
    }

    @Test
    void hasRoleReturnsFalseForUnassignedRole() {
        User user = new User("a@example.com", "hash", Set.of(UserRole.THERAPIST), true);

        assertThat(user.hasRole(UserRole.SUPERVISOR)).isFalse();
    }

    // ---- deprecated getRole / setRole -----------------------------------

    @Test
    void deprecatedGetRoleReturnsFirstRole() {
        User user = new User("a@example.com", "hash", Set.of(UserRole.THERAPIST), true);

        @SuppressWarnings("deprecation")
        UserRole role = user.getRole();

        assertThat(role).isEqualTo(UserRole.THERAPIST);
    }

    @Test
    void deprecatedGetRoleThrowsIllegalStateWhenRolesSetIsEmpty() {
        User user = createUserWithEmptyRoles();

        //noinspection deprecation
        assertThatThrownBy(user::getRole)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("user has no roles");
    }

    @Test
    void deprecatedSetRoleReplacesAllRolesWithSingleRole() {
        User user = new User("a@example.com", "hash",
                new LinkedHashSet<>(Set.of(UserRole.THERAPIST, UserRole.SUPERVISOR)), true);

        @SuppressWarnings("deprecation")
        Void unused = null;
        user.setRole(UserRole.FINANCE);

        assertThat(user.getRoles()).containsExactly(UserRole.FINANCE);
    }

    @Test
    void deprecatedSetRoleUpdatesUpdatedAt() {
        User user = new User("a@example.com", "hash", Set.of(UserRole.THERAPIST), true);
        Instant before = user.getUpdatedAt();

        @SuppressWarnings("deprecation")
        Void unused = null;
        user.setRole(UserRole.FINANCE);

        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(before);
    }

    // ---- multi-role combined checks ------------------------------------

    @Test
    void userConstructedWithTwoRolesHasBothRoles() {
        User user = new User("a@example.com", "hash",
                new LinkedHashSet<>(Set.of(UserRole.THERAPIST, UserRole.SUPERVISOR)), true);

        assertThat(user.getRoles()).containsExactlyInAnyOrder(UserRole.THERAPIST, UserRole.SUPERVISOR);
        assertThat(user.hasRole(UserRole.THERAPIST)).isTrue();
        assertThat(user.hasRole(UserRole.SUPERVISOR)).isTrue();
        assertThat(user.hasRole(UserRole.FINANCE)).isFalse();
    }

    // ---- private helpers -----------------------------------------------

    /**
     * Creates a User via the JPA-protected no-arg constructor so that the
     * roles set is empty — used to test the deprecated {@code getRole()} null guard.
     */
    private User createUserWithEmptyRoles() {
        try {
            java.lang.reflect.Constructor<User> ctor =
                    User.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
