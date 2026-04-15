package com.psyassistant.notifications.template;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TemplateVariableService}.
 */
class TemplateVariableServiceTest {

    private final TemplateVariableService service = new TemplateVariableService();

    // ---- validateVariables ------------------------------------------------

    @Test
    @DisplayName("validateVariables returns empty list when all tokens are known")
    void validateVariablesKnownTokensEmpty() {
        final List<String> result = service.validateVariables(
                "Dear {{client_name}}, your appointment is on {{appointment_date}} at {{location}}.");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("validateVariables returns unknown token names")
    void validateVariablesUnknownTokensReturned() {
        final List<String> result = service.validateVariables(
                "Click {{cancel_link}} or contact {{unknown_field}}.");
        assertThat(result).containsExactlyInAnyOrder("cancel_link", "unknown_field");
    }

    @Test
    @DisplayName("validateVariables returns empty list on null text")
    void validateVariablesNullEmpty() {
        assertThat(service.validateVariables(null)).isEmpty();
    }

    @Test
    @DisplayName("validateVariables returns empty list on blank text")
    void validateVariablesBlankEmpty() {
        assertThat(service.validateVariables("   ")).isEmpty();
    }

    @Test
    @DisplayName("validateVariables handles text with no tokens")
    void validateVariablesNoTokensEmpty() {
        assertThat(service.validateVariables("Hello there, no placeholders here.")).isEmpty();
    }

    // ---- hasUnknownVariables ----------------------------------------------

    @Test
    @DisplayName("hasUnknownVariables returns true when body contains unknown token")
    void hasUnknownVariablesBodyUnknownTrue() {
        assertThat(service.hasUnknownVariables(null, "{{cancel_link}} click here")).isTrue();
    }

    @Test
    @DisplayName("hasUnknownVariables returns true when subject contains unknown token")
    void hasUnknownVariablesSubjectUnknownTrue() {
        assertThat(service.hasUnknownVariables("Your {{bad_var}} appt", "Body")).isTrue();
    }

    @Test
    @DisplayName("hasUnknownVariables returns false when all tokens are known")
    void hasUnknownVariablesAllKnownFalse() {
        assertThat(service.hasUnknownVariables(
                "Hello {{client_name}}", "Appt on {{appointment_date}}")).isFalse();
    }

    // ---- render -----------------------------------------------------------

    @Test
    @DisplayName("render replaces all known tokens")
    void renderReplacesKnownTokens() {
        final String result = service.render(
                "Dear {{client_name}}, your appointment is at {{location}}.",
                Map.of("client_name", "Іван", "location", "Room 5"));
        assertThat(result).isEqualTo("Dear Іван, your appointment is at Room 5.");
    }

    @Test
    @DisplayName("render removes unknown tokens (replaces with empty string)")
    void renderRemovesUnknownTokens() {
        final String result = service.render(
                "Click {{cancel_link}} for details.",
                Map.of());
        assertThat(result).isEqualTo("Click  for details.");
    }

    @Test
    @DisplayName("render returns null when input is null")
    void renderNullReturnsNull() {
        assertThat(service.render(null, Map.of())).isNull();
    }

    @Test
    @DisplayName("render handles $ in replacement value without back-reference issues")
    void renderDollarSignInValueHandledSafely() {
        final String result = service.render(
                "Cost: {{client_name}}",
                Map.of("client_name", "$100"));
        assertThat(result).isEqualTo("Cost: $100");
    }

    @Test
    @DisplayName("render with SAMPLE_CONTEXT substitutes all standard tokens")
    void renderSampleContextAllSubstituted() {
        final String body = "{{client_name}} | {{therapist_name}} | {{appointment_datetime}} "
                + "| {{appointment_date}} | {{appointment_time}} | {{location}} | {{organization_name}}";
        final String result = service.render(body, TemplateVariableService.SAMPLE_CONTEXT);
        assertThat(result).doesNotContain("{{");
        assertThat(result).doesNotContain("}}");
    }
}
