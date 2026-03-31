package com.psyassistant.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Enables Spring Retry support for {@code @Retryable} annotations.
 *
 * <p>Used primarily for handling optimistic locking failures in appointment booking.
 * When concurrent modifications occur, JPA throws {@link jakarta.persistence.OptimisticLockException}
 * and the operation is retried automatically.
 *
 * <p>See {@link com.psyassistant.scheduling.service.AppointmentService#createAppointment}
 * for retry configuration details.
 */
@Configuration
@EnableRetry
public class RetryConfig {
    // No custom beans needed - using @Retryable annotation-based configuration
}
