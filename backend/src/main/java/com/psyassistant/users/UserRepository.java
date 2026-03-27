package com.psyassistant.users;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Spring Data repository for {@link User} entities.
 */
public interface UserRepository extends JpaRepository<User, UUID>,
        JpaSpecificationExecutor<User> {

    /**
     * Finds a user by their email address.
     *
     * @param email the email to look up
     * @return an {@link Optional} containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Returns true if a user with the given email exists.
     *
     * @param email email to check
     * @return true if an account with this email already exists
     */
    boolean existsByEmail(String email);
}
