package com.psyassistant.common.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the stateless JWT-based API.
 *
 * <p>This is a skeleton configuration that disables CSRF (not needed for stateless APIs),
 * sets session management to stateless, and permits the Actuator health endpoint without
 * authentication. All other endpoints require authentication.
 *
 * <p>TODO PA-62: JWT authentication filter (JwtAuthenticationFilter) to be added in the
 * JWT auth story.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configures the primary security filter chain.
     *
     * @param http the {@link HttpSecurity} builder provided by Spring Security
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if the configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
            )
            .authorizeHttpRequests(auth -> auth
                // Sensitive Actuator endpoints are blocked for all callers unconditionally
                .requestMatchers("/actuator/env", "/actuator/beans").denyAll()
                // Health check is public (e.g., load balancer probes)
                .requestMatchers("/actuator/health").permitAll()
                // Remaining Actuator endpoints require authentication
                .requestMatchers("/actuator/**").authenticated()
                .anyRequest().authenticated()
            );
        // TODO PA-62: JwtAuthenticationFilter to be added in JWT auth story
        return http.build();
    }
}
