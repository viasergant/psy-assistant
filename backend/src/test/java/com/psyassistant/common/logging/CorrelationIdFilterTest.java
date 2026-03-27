package com.psyassistant.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * Unit tests for {@link CorrelationIdFilter}.
 *
 * <p>These tests run without a Spring context — the filter is instantiated directly.
 */
class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    /** Clears MDC before and after every test to avoid cross-test contamination. */
    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    /**
     * When the incoming request already carries {@code X-Request-ID}, the same value must be
     * stored in MDC during filter execution, echoed in the response header, and removed
     * afterwards.
     */
    @Test
    void providedRequestIdIsStoredInMdcDuringChainAndClearedAfter() throws Exception {
        final String providedId = "test-correlation-id-123";

        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final AtomicReference<String> capturedMdcValue = new AtomicReference<>();

        when(request.getHeader(CorrelationIdFilter.REQUEST_ID_HEADER)).thenReturn(providedId);
        doNothing().when(response).setHeader(anyString(), anyString());

        final FilterChain chain = (req, res) ->
                capturedMdcValue.set(MDC.get(CorrelationIdFilter.MDC_KEY));

        filter.doFilterInternal(request, response, chain);

        assertThat(capturedMdcValue.get())
                .as("MDC must contain the provided X-Request-ID during filter execution")
                .isEqualTo(providedId);

        verify(response).setHeader(CorrelationIdFilter.REQUEST_ID_HEADER, providedId);

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY))
                .as("MDC requestId must be cleared after the filter completes")
                .isNull();
    }

    /**
     * When the incoming request has no {@code X-Request-ID} header, a UUID must be generated,
     * stored in MDC during execution, written to the response header, and removed afterwards.
     */
    @Test
    void missingRequestIdGeneratesUuidInMdcAndResponseHeader() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final AtomicReference<String> capturedMdcValue = new AtomicReference<>();

        when(request.getHeader(CorrelationIdFilter.REQUEST_ID_HEADER)).thenReturn(null);
        doNothing().when(response).setHeader(anyString(), anyString());

        final FilterChain chain = (req, res) ->
                capturedMdcValue.set(MDC.get(CorrelationIdFilter.MDC_KEY));

        filter.doFilterInternal(request, response, chain);

        assertThat(capturedMdcValue.get())
                .as("A UUID must be generated and placed in MDC when no header is present")
                .isNotNull()
                .isNotBlank()
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

        verify(response).setHeader(eq(CorrelationIdFilter.REQUEST_ID_HEADER), anyString());

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY))
                .as("MDC requestId must be cleared after the filter completes")
                .isNull();
    }

    /**
     * MDC must be cleared even when the downstream filter chain throws an exception.
     */
    @Test
    void mdcIsClearedEvenWhenChainThrows() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        when(request.getHeader(CorrelationIdFilter.REQUEST_ID_HEADER)).thenReturn("some-id");
        doNothing().when(response).setHeader(anyString(), anyString());

        final FilterChain chain = (req, res) -> {
            throw new RuntimeException("downstream failure");
        };

        try {
            filter.doFilterInternal(request, response, chain);
        } catch (RuntimeException ignored) {
            // expected
        }

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY))
                .as("MDC must be cleared even after an exception in the filter chain")
                .isNull();
    }

    /**
     * A blank (whitespace-only) {@code X-Request-ID} header must be treated the same as absent —
     * a new UUID must be generated.
     */
    @Test
    void blankRequestIdHeaderGeneratesNewUuid() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);
        final AtomicReference<String> capturedMdcValue = new AtomicReference<>();

        when(request.getHeader(CorrelationIdFilter.REQUEST_ID_HEADER)).thenReturn("   ");
        doNothing().when(response).setHeader(anyString(), anyString());

        final FilterChain chain = (req, res) ->
                capturedMdcValue.set(MDC.get(CorrelationIdFilter.MDC_KEY));

        filter.doFilterInternal(request, response, chain);

        assertThat(capturedMdcValue.get())
                .as("A blank X-Request-ID must be replaced with a generated UUID")
                .isNotBlank()
                .isNotEqualTo("   ");

        verify(response, never()).setHeader(
                CorrelationIdFilter.REQUEST_ID_HEADER, "   ");
    }
}
