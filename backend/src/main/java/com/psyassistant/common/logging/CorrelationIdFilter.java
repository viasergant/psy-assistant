package com.psyassistant.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter that propagates a correlation ID through the MDC for every HTTP request.
 *
 * <p>If the incoming request carries an {@code X-Request-ID} header the value is reused;
 * otherwise a new UUID is generated. The ID is stored under the MDC key {@code requestId}
 * so that every log statement emitted during the request lifecycle includes it automatically.
 *
 * <p>The same ID is echoed back to the caller via the {@code X-Request-ID} response header,
 * enabling end-to-end correlation between client logs and server logs.
 *
 * <p>MDC cleanup is performed in a {@code finally} block to guarantee removal even when
 * a downstream filter or handler throws an exception.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** HTTP request / response header name carrying the correlation ID. */
    static final String REQUEST_ID_HEADER = "X-Request-ID";

    /** MDC key under which the correlation ID is stored during request processing. */
    static final String MDC_KEY = "requestId";

    /**
     * Reads or generates a correlation ID, binds it to the MDC, and clears it after the
     * request completes.
     *
     * @param request  the incoming HTTP request
     * @param response the outgoing HTTP response
     * @param chain    the remaining filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain chain) throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
