package com.psyassistant.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.auth.service.ActiveUserJwtValidator;
import com.psyassistant.auth.service.UserActivityTracker;
import com.psyassistant.users.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

/** Unit tests for {@link ActiveUserJwtValidator}. */
class ActiveUserJwtValidatorTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserActivityTracker activityTracker = mock(UserActivityTracker.class);
    private final ActiveUserJwtValidator validator = new ActiveUserJwtValidator(userRepository, activityTracker);

    private static Jwt jwtFor(final String subject) {
        return Jwt.withTokenValue("t").header("alg", "HS256").subject(subject)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    }

    @Test
    void acceptsTokenOfActiveUser() {
        UUID id = UUID.randomUUID();
        when(userRepository.findActiveById(id)).thenReturn(Optional.of(true));

        assertThat(validator.validate(jwtFor(id.toString())).hasErrors()).isFalse();
        verify(activityTracker).recordActivity(id);
    }

    @Test
    void rejectsTokenOfSuspendedUser() {
        UUID id = UUID.randomUUID();
        when(userRepository.findActiveById(id)).thenReturn(Optional.of(false));

        assertThat(validator.validate(jwtFor(id.toString())).hasErrors()).isTrue();
        verify(activityTracker, never()).recordActivity(id);
    }

    @Test
    void rejectsTokenOfDeletedUser() {
        UUID id = UUID.randomUUID();
        when(userRepository.findActiveById(id)).thenReturn(Optional.empty());

        assertThat(validator.validate(jwtFor(id.toString())).hasErrors()).isTrue();
    }

    @Test
    void rejectsTokenWithNonUuidSubject() {
        assertThat(validator.validate(jwtFor("not-a-uuid")).hasErrors()).isTrue();
    }
}
