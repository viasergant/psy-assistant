package com.psyassistant.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson configuration for proper serialization/deserialization.
 *
 * <p>Ensures LocalTime, LocalDate, and other JSR-310 types are serialized
 * as strings (ISO-8601 format) instead of arrays.
 */
@Configuration
public class JacksonConfig {

    /**
     * Configure ObjectMapper with JavaTimeModule for proper date/time handling.
     *
     * @return configured ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // Register JavaTimeModule for JSR-310 support
        mapper.registerModule(new JavaTimeModule());
        
        // Disable writing dates as timestamps (arrays)
        // This makes LocalTime serialize as "09:00:00" instead of [9, 0, 0]
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        return mapper;
    }
}
