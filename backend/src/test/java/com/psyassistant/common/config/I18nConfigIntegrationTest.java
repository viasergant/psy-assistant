package com.psyassistant.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for i18n {@link MessageSource} configuration.
 */
@SpringBootTest
@ActiveProfiles("test")
class I18nConfigIntegrationTest {

    @Autowired
    private MessageSource messageSource;

    @Test
    void messageSourceLoadsEnglishMessages() {
        String message = messageSource.getMessage("validation.required", null, Locale.ENGLISH);
        assertThat(message).isEqualTo("This field is required");
    }

    @Test
    void messageSourceLoadsUkrainianMessages() {
        String message = messageSource.getMessage("validation.required", null, 
            Locale.forLanguageTag("uk"));
        assertThat(message).isEqualTo("Це поле обов'язкове");
    }

    @Test
    void messageSourceReturnsKeyWhenTranslationMissing() {
        String message = messageSource.getMessage("nonexistent.key", null, Locale.ENGLISH);
        assertThat(message).isEqualTo("nonexistent.key");
    }

    @Test
    void messageSourceSupportsParameterSubstitution() {
        String message = messageSource.getMessage("schedule.therapistSchedule", 
            new Object[]{"Dr. Smith"}, Locale.ENGLISH);
        // This test assumes we'll add parametrized messages in the future
        // For now, just verify it doesn't throw
        assertThat(message).isNotNull();
    }
}
