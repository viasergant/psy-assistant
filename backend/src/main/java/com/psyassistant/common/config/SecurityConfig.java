package com.psyassistant.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psyassistant.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security configuration for the stateless JWT-based API.
 *
 * <p>Uses Spring OAuth2 Resource Server to validate HS256-signed JWTs.
 * BCrypt (strength 12) is used for password hashing. The refresh token
 * endpoints and Actuator health check are permitted without a JWT.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final int BCRYPT_STRENGTH = 12;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.issuer}")
    private String issuer;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * Configures the primary security filter chain with JWT resource server support.
     *
     * @param http          the {@link HttpSecurity} builder provided by Spring Security
     * @param jwtDecoder    the JWT decoder bean
     * @param objectMapper  used to write JSON error responses
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if the configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(
            final HttpSecurity http,
            final JwtDecoder jwtDecoder,
            final ObjectMapper objectMapper) throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").authenticated()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder)
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
                .authenticationEntryPoint((request, response, ex) ->
                    writeError(response, objectMapper, HttpStatus.UNAUTHORIZED,
                            "TOKEN_EXPIRED", request.getRequestURI()))
                .accessDeniedHandler((request, response, ex) ->
                    writeError(response, objectMapper, HttpStatus.FORBIDDEN,
                            "FORBIDDEN", request.getRequestURI()))
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    writeError(response, objectMapper, HttpStatus.UNAUTHORIZED,
                            "TOKEN_EXPIRED", request.getRequestURI()))
            );

        return http.build();
    }

    /**
     * Builds a {@link JwtDecoder} that validates HS256-signed tokens.
     *
     * @return configured JWT decoder
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec key = new SecretKeySpec(keyBytes, HMAC_SHA256);
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * Configures the JWT to authorities converter.
     *
     * <p>Reads the {@code roles} claim as a list; values already carry the
     * {@code ROLE_} prefix so no additional prefix mapping is needed.
     *
     * @return configured {@link JwtAuthenticationConverter}
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("");  // prefix already in claim value

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    /**
     * BCrypt password encoder at strength 12.
     *
     * @return the password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    /**
     * Configures CORS to allow the Angular dev server (and production origin).
     *
     * @return the CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-ID"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void writeError(
            final HttpServletResponse response,
            final ObjectMapper objectMapper,
            final HttpStatus status,
            final String code,
            final String path) {
        try {
            response.setStatus(status.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            ErrorResponse body = new ErrorResponse(
                    Instant.now(), status.value(), status.getReasonPhrase(), code, path);
            response.getWriter().write(objectMapper.writeValueAsString(body));
        } catch (Exception ignored) {
            // Last-resort: if serialization fails the status code is already set
        }
    }
}
