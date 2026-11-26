package com.psyassistant.notifications.telegram;

import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Supplies infrastructure beans for the Telegram subsystem.
 */
@Configuration
public class TelegramConfig {

    /**
     * System UTC clock, injectable so jobs are testable with a fixed clock.
     *
     * @return the clock
     */
    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock clock() {
        return Clock.systemUTC();
    }
}
