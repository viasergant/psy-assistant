package com.psyassistant.notifications.telegram;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

class TelegramMessageFormatterTest {

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");

    private final TelegramMessageFormatter formatter = new TelegramMessageFormatter(source());

    private static ResourceBundleMessageSource source() {
        final ResourceBundleMessageSource s = new ResourceBundleMessageSource();
        s.setBasename("messages");
        s.setDefaultEncoding("UTF-8");
        s.setFallbackToSystemLocale(false);
        return s;
    }

    @Test
    void digestListsTimesInRecipientZoneAndLink() {
        final var start = ZonedDateTime.parse("2026-11-26T08:00:00Z"); // 10:00 in Kyiv
        final String text = formatter.digest(LocalDate.of(2026, 11, 26),
                List.of(new TelegramMessageFormatter.Line(start, 50, "Individual", "C-1042")),
                KYIV, Locale.ENGLISH, "https://app/schedule");

        assertThat(text).contains("1 session(s)").contains("10:00-10:50 | Individual | C-1042")
                .contains("https://app/schedule");
    }

    @Test
    void emptyDigestSaysNoSessions() {
        assertThat(formatter.digest(LocalDate.of(2026, 11, 26), List.of(), KYIV, Locale.ENGLISH, "u"))
                .contains("No sessions");
    }

    @Test
    void reminderShowsLeadMinutesAndLocalTime() {
        final var line = new TelegramMessageFormatter.Line(
                ZonedDateTime.parse("2026-11-26T08:00:00Z"), 50, "Individual", "C-1042");

        assertThat(formatter.reminder(line, KYIV, Locale.ENGLISH, 60))
                .isEqualTo("Session in 60 min: 10:00 | Individual | C-1042");
    }

    @Test
    void ukrainianTextIsLocalized() {
        assertThat(formatter.text("telegram.test", Locale.forLanguageTag("uk"))).contains("Тестове");
    }
}
