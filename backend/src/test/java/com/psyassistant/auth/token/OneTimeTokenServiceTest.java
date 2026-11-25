package com.psyassistant.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OneTimeTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-25T10:00:00Z");

    private OneTimeTokenRepository repository;
    private OneTimeTokenService service;

    @BeforeEach
    void setUp() {
        repository = mock(OneTimeTokenRepository.class);
        service = new OneTimeTokenService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void issueStoresOnlyHashAndReplacesPreviousTokens() {
        UUID userId = UUID.randomUUID();

        String raw = service.issue(userId, TokenPurpose.EMAIL_CONFIRMATION, Duration.ofHours(24));

        assertThat(raw).hasSize(64).matches("[0-9a-f]{64}");
        verify(repository).deleteAllByUserIdAndPurpose(userId, TokenPurpose.EMAIL_CONFIRMATION);
        ArgumentCaptor<OneTimeToken> saved = ArgumentCaptor.forClass(OneTimeToken.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getUserId()).isEqualTo(userId);
        assertThat(saved.getValue().isExpired(NOW.plus(Duration.ofHours(23)))).isFalse();
        assertThat(saved.getValue().isExpired(NOW.plus(Duration.ofHours(24)))).isTrue();
    }

    @Test
    void consumeValidTokenReturnsUserAndMarksConsumed() {
        UUID userId = UUID.randomUUID();
        OneTimeToken token = captureIssued(userId, Duration.ofHours(1));
        String raw = lastRaw;
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));

        UUID result = service.consume(raw, TokenPurpose.EMAIL_CONFIRMATION);

        assertThat(result).isEqualTo(userId);
        assertThat(token.isConsumed()).isTrue();
    }

    @Test
    void consumeReusedTokenIsRejected() {
        OneTimeToken token = captureIssued(UUID.randomUUID(), Duration.ofHours(1));
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));
        service.consume(lastRaw, TokenPurpose.EMAIL_CONFIRMATION);

        assertThatThrownBy(() -> service.consume(lastRaw, TokenPurpose.EMAIL_CONFIRMATION))
                .isInstanceOf(InvalidTokenException.class)
                .extracting(e -> ((InvalidTokenException) e).getReason())
                .isEqualTo(InvalidTokenException.Reason.INVALID);
    }

    @Test
    void consumeExpiredTokenIsRejectedAsExpired() {
        OneTimeToken token = captureIssued(UUID.randomUUID(), Duration.ZERO);
        when(repository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.consume(lastRaw, TokenPurpose.EMAIL_CONFIRMATION))
                .isInstanceOf(InvalidTokenException.class)
                .extracting(e -> ((InvalidTokenException) e).getReason())
                .isEqualTo(InvalidTokenException.Reason.EXPIRED);
    }

    @Test
    void consumeUnknownTokenIsRejected() {
        when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consume("nope", TokenPurpose.EMAIL_CONFIRMATION))
                .isInstanceOf(InvalidTokenException.class);
    }

    private String lastRaw;

    private OneTimeToken captureIssued(final UUID userId, final Duration ttl) {
        lastRaw = service.issue(userId, TokenPurpose.EMAIL_CONFIRMATION, ttl);
        ArgumentCaptor<OneTimeToken> saved = ArgumentCaptor.forClass(OneTimeToken.class);
        verify(repository).save(saved.capture());
        return saved.getValue();
    }
}
