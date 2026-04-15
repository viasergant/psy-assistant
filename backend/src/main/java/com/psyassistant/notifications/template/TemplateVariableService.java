package com.psyassistant.notifications.template;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Validates and substitutes template variables of the form {@code {{variable_name}}}.
 *
 * <p>Supported tokens:
 * <ul>
 *   <li>{@code {{client_name}}}</li>
 *   <li>{@code {{therapist_name}}}</li>
 *   <li>{@code {{appointment_datetime}}}</li>
 *   <li>{@code {{appointment_date}}}</li>
 *   <li>{@code {{appointment_time}}}</li>
 *   <li>{@code {{location}}}</li>
 *   <li>{@code {{organization_name}}}</li>
 * </ul>
 *
 * <p>Any token not in the list above is considered unknown and flagged during validation.
 * At render time, known tokens are replaced; unknown tokens are removed to avoid leaking
 * raw template syntax in sent messages.
 */
@Service
public class TemplateVariableService {

    /** Regex that matches any {{token}} placeholder. */
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\{\\{([a-z_]+)}}");

    /** Sample data used for preview rendering. */
    public static final Map<String, String> SAMPLE_CONTEXT = Map.of(
            "client_name",            "Іван Петренко",
            "therapist_name",         "Dr. Test",
            "appointment_datetime",   "2026-04-01 10:00",
            "appointment_date",       "2026-04-01",
            "appointment_time",       "10:00",
            "location",               "Office 3",
            "organization_name",      "PSY Assistant"
    );

    private static final java.util.Set<String> KNOWN_VARIABLES =
            java.util.Set.copyOf(SAMPLE_CONTEXT.keySet());

    /**
     * Scans {@code text} for any {@code {{token}}} patterns and returns the list of
     * token names that are not in the set of supported variables.
     *
     * @param text subject or body string to validate
     * @return list of unrecognized token names (empty if all tokens are known)
     */
    public List<String> validateVariables(final String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        final List<String> unknown = new ArrayList<>();
        final Matcher m = TOKEN_PATTERN.matcher(text);
        while (m.find()) {
            final String token = m.group(1);
            if (!KNOWN_VARIABLES.contains(token)) {
                unknown.add(token);
            }
        }
        return unknown;
    }

    /**
     * Returns {@code true} if the combined subject + body contain any unknown variable tokens.
     *
     * @param subject template subject (may be null)
     * @param body    template body
     * @return true when unknown tokens are present
     */
    public boolean hasUnknownVariables(final String subject, final String body) {
        return !validateVariables(subject).isEmpty() || !validateVariables(body).isEmpty();
    }

    /**
     * Replaces all known tokens in {@code text} with values from {@code context}.
     * Unknown tokens are removed (replaced with an empty string) so that no raw
     * syntax escapes into sent messages.
     *
     * @param text    text with {@code {{token}}} placeholders
     * @param context map of token name → replacement value
     * @return rendered text with all tokens substituted
     */
    public String render(final String text, final Map<String, String> context) {
        if (text == null) {
            return null;
        }
        final StringBuffer sb = new StringBuffer();
        final Matcher m = TOKEN_PATTERN.matcher(text);
        while (m.find()) {
            final String token = m.group(1);
            final String replacement = context.getOrDefault(token, "");
            // Escape replacement to avoid treating $ in addresses as back-references.
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
