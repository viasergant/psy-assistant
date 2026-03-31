package com.psyassistant.common.i18n;

import com.psyassistant.users.User;
import com.psyassistant.users.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

/**
 * Custom locale resolver that determines the user's locale with priority:
 * 1. Authenticated user's database preference (users.language)
 * 2. Cookie (pa_locale)
 * 3. Accept-Language header
 * 4. Default (en)
 */
public class CustomCookieLocaleResolver extends CookieLocaleResolver {

    private static final String COOKIE_NAME = "pa_locale";
    private static final List<String> SUPPORTED_LOCALES = List.of("en", "uk");
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private final UserRepository userRepository;

    public CustomCookieLocaleResolver(final UserRepository userRepository) {
        super(COOKIE_NAME);
        this.userRepository = userRepository;
        setDefaultLocale(DEFAULT_LOCALE);
        setCookieMaxAge(31536000); // 1 year
        setCookieSameSite("Lax");
        // Secure flag will be set by Spring Boot based on server.servlet.session.cookie.secure
    }

    @Override
    @NonNull
    public Locale resolveLocale(@NonNull final HttpServletRequest request) {
        // 1. Check authenticated user's database preference
        Locale dbLocale = resolveAuthenticatedUserLocale();
        if (dbLocale != null && isSupportedLocale(dbLocale)) {
            return dbLocale;
        }

        // 2. Check cookie
        Locale cookieLocale = resolveCookieLocale(request);
        if (cookieLocale != null && isSupportedLocale(cookieLocale)) {
            return cookieLocale;
        }

        // 3. Check Accept-Language header
        Locale headerLocale = resolveHeaderLocale(request);
        if (headerLocale != null && isSupportedLocale(headerLocale)) {
            return headerLocale;
        }

        // 4. Default
        return DEFAULT_LOCALE;
    }

    /**
     * Resolves locale from authenticated user's database preference.
     */
    @Nullable
    private Locale resolveAuthenticatedUserLocale() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }

        try {
            UUID userId = UUID.fromString(auth.getName());
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                String language = userOpt.get().getLanguage();
                return parseLocale(language);
            }
        } catch (Exception e) {
            // Invalid UUID or database error - fall through to other resolution methods
            return null;
        }

        return null;
    }

    /**
     * Resolves locale from cookie.
     */
    @Nullable
    private Locale resolveCookieLocale(final HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(cookie -> parseLocale(cookie.getValue()))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * Resolves locale from Accept-Language header.
     */
    @Nullable
    private Locale resolveHeaderLocale(final HttpServletRequest request) {
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return null;
        }

        // Parse first locale from Accept-Language header (e.g., "uk,en;q=0.9" → "uk")
        String[] languages = acceptLanguage.split(",");
        if (languages.length == 0) {
            return null;
        }

        String primaryLang = languages[0].split(";")[0].trim().split("-")[0];
        return parseLocale(primaryLang);
    }

    /**
     * Parses a locale string (e.g., "en", "uk") into a Locale object.
     */
    @Nullable
    private Locale parseLocale(final String localeString) {
        if (localeString == null || localeString.isBlank()) {
            return null;
        }
        try {
            return Locale.forLanguageTag(localeString);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Checks if a locale is supported.
     */
    private boolean isSupportedLocale(final Locale locale) {
        return SUPPORTED_LOCALES.contains(locale.getLanguage());
    }

    @Override
    public void setLocale(@NonNull final HttpServletRequest request,
                          @Nullable final HttpServletResponse response,
                          @Nullable final Locale locale) {
        if (locale != null && isSupportedLocale(locale)) {
            super.setLocale(request, response, locale);
        }
    }
}
