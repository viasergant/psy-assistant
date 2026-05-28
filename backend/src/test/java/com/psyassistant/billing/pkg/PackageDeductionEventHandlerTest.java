package com.psyassistant.billing.pkg;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.billing.catalog.ServiceType;
import com.psyassistant.scheduling.domain.SessionType;
import com.psyassistant.sessions.event.SessionCompletedEvent;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link PackageDeductionEventHandler}.
 */
@ExtendWith(MockitoExtension.class)
class PackageDeductionEventHandlerTest {

    @Mock private PackageService packageService;
    @Mock private SessionTypeToServiceTypeMapper serviceTypeMapper;

    private PackageDeductionEventHandler handler;

    private static final UUID SESSION_ID   = UUID.randomUUID();
    private static final UUID CLIENT_ID    = UUID.randomUUID();
    private static final UUID THERAPIST_ID = UUID.randomUUID();
    private static final UUID PACKAGE_ID   = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        handler = new PackageDeductionEventHandler(packageService, serviceTypeMapper);
    }

    @Nested
    class WhenSessionTypeIsNull {

        @Test
        void shouldSkipDeduction() {
            final SessionCompletedEvent event = event(null);

            handler.handleSessionCompleted(event);

            verify(packageService, never()).tryDeductSession(any(), any(), any(), any(), any());
        }
    }

    @Nested
    class WhenSessionTypeHasNoMapping {

        @Test
        void shouldSkipDeduction() {
            when(serviceTypeMapper.resolve("UNKNOWN")).thenReturn(Optional.empty());

            handler.handleSessionCompleted(event(sessionType("UNKNOWN")));

            verify(packageService, never()).tryDeductSession(any(), any(), any(), any(), any());
        }
    }

    @Nested
    class WhenEligiblePackageExists {

        @Test
        void shouldDeductOneSession() {
            when(serviceTypeMapper.resolve("IN_PERSON"))
                    .thenReturn(Optional.of(ServiceType.INDIVIDUAL_SESSION));
            when(packageService.tryDeductSession(
                    eq(CLIENT_ID), eq(ServiceType.INDIVIDUAL_SESSION),
                    eq(SESSION_ID), eq(THERAPIST_ID), any()))
                    .thenReturn(new PackageDeductionResult.Deducted(PACKAGE_ID, 5, 4));

            handler.handleSessionCompleted(event(sessionType("IN_PERSON")));

            verify(packageService).tryDeductSession(
                    eq(CLIENT_ID), eq(ServiceType.INDIVIDUAL_SESSION),
                    eq(SESSION_ID), eq(THERAPIST_ID), any());
        }

        @Test
        void shouldHandlePackageExhaustion() {
            when(serviceTypeMapper.resolve("GROUP"))
                    .thenReturn(Optional.of(ServiceType.GROUP_SESSION));
            when(packageService.tryDeductSession(
                    eq(CLIENT_ID), eq(ServiceType.GROUP_SESSION),
                    eq(SESSION_ID), eq(THERAPIST_ID), any()))
                    .thenReturn(new PackageDeductionResult.PackageExhausted(PACKAGE_ID));

            handler.handleSessionCompleted(event(sessionType("GROUP")));

            verify(packageService).tryDeductSession(any(), any(), any(), any(), any());
        }
    }

    @Nested
    class WhenNoEligiblePackageExists {

        @Test
        void shouldCompleteWithoutError() {
            when(serviceTypeMapper.resolve("FOLLOW_UP"))
                    .thenReturn(Optional.of(ServiceType.FOLLOW_UP));
            when(packageService.tryDeductSession(any(), any(), any(), any(), any()))
                    .thenReturn(new PackageDeductionResult.NoEligiblePackage());

            handler.handleSessionCompleted(event(sessionType("FOLLOW_UP")));

            verify(packageService).tryDeductSession(any(), any(), any(), any(), any());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static SessionCompletedEvent event(final SessionType sessionType) {
        return new SessionCompletedEvent(SESSION_ID, CLIENT_ID, THERAPIST_ID,
                LocalDate.now(), sessionType, null, null);
    }

    private static SessionType sessionType(final String code) {
        return new SessionType(code, code, null);
    }
}
