package com.psyassistant.common.config;

import com.psyassistant.common.i18n.CustomCookieLocaleResolver;
import com.psyassistant.users.UserRepository;
import java.nio.charset.StandardCharsets;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;

/**
 * i18n configuration for locale resolution and message source.
 *
 * <p>Configures:
 * - LocaleResolver with cookie-based locale detection and database preference for authenticated users
 * - MessageSource for backend validation/error messages in multiple languages
 */
@Configuration
public class I18nConfig {

    /**
     * Configures locale resolver with cookie and database support.
     * Priority: Database (authenticated users) → Cookie → Accept-Language header → Default (en).
     *
     * @param userRepository user repository for database locale lookup
     * @return CustomCookieLocaleResolver instance
     */
    @Bean
    public LocaleResolver localeResolver(final UserRepository userRepository) {
        return new CustomCookieLocaleResolver(userRepository);
    }

    /**
     * Configures MessageSource for loading localized messages.
     * Message files: src/main/resources/messages_en.properties, messages_uk.properties.
     *
     * @return MessageSource with UTF-8 encoding
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = 
            new ReloadableResourceBundleMessageSource();
        
        messageSource.setBasename("classpath:messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setCacheSeconds(3600); // Cache for 1 hour in production
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setUseCodeAsDefaultMessage(true); // Return key if translation missing
        
        return messageSource;
    }
}
