package com.psyassistant.common.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRepository;
import com.psyassistant.users.UserRole;
import jakarta.servlet.http.Cookie;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for {@link CustomCookieLocaleResolver}.
 */
@ExtendWith(MockitoExtension.class)
class CustomCookieLocaleResolverTest {

    @Mock
    private UserRepository userRepository;

    private CustomCookieLocaleResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CustomCookieLocaleResolver(userRepository);
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolveLocaleReturnsEnglishWhenNoPreferencesSet() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        Locale locale = resolver.resolveLocale(request);

        assertThat(locale).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void resolveLocaleReturnsLocaleFromCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("pa_locale", "uk"));

        Locale locale = resolver.resolveLocale(request);

        assertThat(locale.getLanguage()).isEqualTo("uk");
    }

    @Test
    void resolveLocaleReturnsLocaleFromAcceptLanguageHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "uk,en;q=0.9");

        Locale locale = resolver.resolveLocale(request);

        assertThat(locale.getLanguage()).isEqualTo("uk");
    }

    @Test
    void resolveLocaleReturnsEnglishWhenAcceptLanguageIsUnsupported() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "fr,de;q=0.9");

        Locale locale = resolver.resolveLocale(request);

        assertThat(locale).isEqualTo(Locale.ENGLISH);
    }

    @Test
    void resolveLocaleReturnsUserDatabasePreferenceWhenAuthenticated() {
        UUID userId = UUID.randomUUID();
        User user = new User("test@example.com", "hash", UserRole.THERAPIST, true);
        user.setLanguage("uk");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Set authentication context - use 3-parameter constructor for authenticated token
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(userId.toString(), null, java.util.Collections.emptyList())
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("pa_locale", "en")); // Cookie should be overridden by DB

        Locale locale = resolver.resolveLocale(request);

        assertThat(locale.getLanguage()).isEqualTo("uk");
    }

    @Test
    void resolveLocaleFallsBackToCookieWhenUserNotFoundInDatabase() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(userId.toString(), null, java.util.Collections.emptyList())
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("pa_locale", "uk"));

        Locale locale = resolver.resolveLocale(request);

        assertThat(locale.getLanguage()).isEqualTo("uk");
    }

    @Test
    void resolveLocalePrioritizesDatabaseOverCookieAndHeader() {
        UUID userId = UUID.randomUUID();
        User user = new User("test@example.com", "hash", UserRole.THERAPIST, true);
        user.setLanguage("uk");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(userId.toString(), null, java.util.Collections.emptyList())
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("pa_locale", "en"));
        request.addHeader("Accept-Language", "en");

        Locale locale = resolver.resolveLocale(request);

        assertThat(locale.getLanguage()).isEqualTo("uk");
    }
}
