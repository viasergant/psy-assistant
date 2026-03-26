package com.psyassistant.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configures Springdoc / OpenAPI metadata.
 *
 * <p>This bean is active only in the {@code local} profile so that the Swagger UI
 * is never exposed in staging or production environments.
 */
@Configuration
@Profile("local")
public class OpenApiConfig {

    /**
     * Provides application-level OpenAPI metadata.
     *
     * @return a customised {@link OpenAPI} instance
     */
    @Bean
    public OpenAPI psyAssistantOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Psy-Assistant API")
                        .description("REST API for the Psychological Assistance CRM")
                        .version("0.0.1-SNAPSHOT"));
    }
}
