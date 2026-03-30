package com.psyassistant.common.config;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
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
     * Migration strategy for local development: drop + recreate the public schema, then migrate.
     *
     * <p>Using {@code DROP SCHEMA … CASCADE} instead of {@code flyway.clean()} avoids a known
     * Flyway/PostgreSQL issue where the clean step queries the catalog for table names but then
     * fails with "table does not exist" when the catalog contains stale entries from a previously
     * interrupted migration.
     *
     * @param dataSource the application datasource, used to execute the schema reset directly
     * @return a {@link FlywayMigrationStrategy} that resets the schema before migrating
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(DataSource dataSource) {
        return flyway -> {
            LOG.warn("Local profile: resetting public schema before migration (dev only)");
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DROP SCHEMA public CASCADE");
                stmt.execute("CREATE SCHEMA public");
                stmt.execute("GRANT ALL ON SCHEMA public TO PUBLIC");
                LOG.info("Public schema dropped and recreated successfully");
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to reset public schema for local dev", ex);
            }
            flyway.migrate();
        };
    }
}

