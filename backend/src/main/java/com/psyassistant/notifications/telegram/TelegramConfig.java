package com.psyassistant.notifications.telegram;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Supplies infrastructure beans for the Telegram subsystem.
 */
@Configuration
public class TelegramConfig {

    /** Qualifier of the API client that acts as the separate client-facing bot. */
    public static final String CLIENT_BOT = "clientBot";

    /**
     * API client of the therapist bot (the default {@link TelegramClient}).
     *
     * @param props configuration
     * @return the client
     */
    @Bean
    @Primary
    public TelegramClient therapistBotClient(final TelegramProperties props) {
        return new TelegramBotApiClient(props, props::getBotToken);
    }

    /**
     * API client of the client bot, which has its own token.
     *
     * @param props configuration
     * @return the client
     */
    @Bean
    @Qualifier(CLIENT_BOT)
    public TelegramClient clientBotClient(final TelegramProperties props) {
        return new TelegramBotApiClient(props, () -> props.getClientBot().getBotToken());
    }

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
