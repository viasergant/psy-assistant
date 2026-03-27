package com.psyassistant.users;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link User} entities.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their email address.
     *
     * @param email the email to look up
     * @return an {@link Optional} containing the user if found
     */
    Optional<User> findByEmail(String email);
}
