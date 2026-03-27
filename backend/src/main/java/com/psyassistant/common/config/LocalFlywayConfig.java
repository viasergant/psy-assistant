package com.psyassistant.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Local-profile Flyway configuration.
 *
 * <p>Wipes the database schema and re-runs all migrations on every startup so
 * the local environment always starts from a known, consistent state —
 * including the seed admin user inserted by V3.
 *
 * <p>This bean is only active when the {@code local} Spring profile is set and
 * must never be used in production.
 */
@Configuration
@Profile("local")
public class LocalFlywayConfig {

    private static final Logger LOG = LoggerFactory.getLogger(LocalFlywayConfig.class);

    /**
     * Migration strategy for local development: clean then migrate.
     *
     * <p>Guarantees a fresh schema on every startup, eliminating state drift
     * caused by previous failed migrations or manual DB changes.
     *
     * @return a {@link FlywayMigrationStrategy} that calls {@code clean()} before {@code migrate()}
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            LOG.warn("Local profile: wiping Flyway-managed schema before migration (dev only)");
            flyway.clean();
            flyway.migrate();
        };
    }
}

