package com.psyassistant.notifications.telegram;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/**
 * Renders Telegram message texts. Output is plain text and contains only time, duration-derived end
 * time, session type and a client label (the opaque client code, or a first name when the user opted in).
 * Notes, contacts, risk flags and diagnoses never reach this class.
 */
@Component
public class TelegramMessageFormatter {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final MessageSource messages;

    /**
     * Creates the formatter.
     *
     * @param messages i18n source
     */
    public TelegramMessageFormatter(final MessageSource messages) {
        this.messages = messages;
    }

    /**
     * One appointment as it is shown to the therapist.
     *
     * @param start         start time
     * @param minutes       duration in minutes
     * @param sessionType   session type name
     * @param clientLabel   client code or first name
     */
    public record Line(ZonedDateTime start, int minutes, String sessionType, String clientLabel) { }

    /**
     * Daily digest.
     *
     * @param date     local date
     * @param lines    appointments of that day
     * @param zone     recipient zone
     * @param locale   recipient locale
     * @param appUrl   link to the calendar
     * @return message text
     */
    public String digest(final LocalDate date, final List<Line> lines, final ZoneId zone,
                         final Locale locale, final String appUrl) {
        final String day = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale));
        if (lines.isEmpty()) {
            return msg("telegram.digest.empty", locale, day);
        }
        final String body = lines.stream()
                .map(l -> line(l, zone, locale))
                .collect(Collectors.joining("\n"));
        return msg("telegram.digest.title", locale, day, lines.size()) + "\n\n" + body
                + "\n\n" + msg("telegram.open", locale, appUrl);
    }

    /**
     * Reminder for one session.
     *
     * @param line    the session
     * @param zone    recipient zone
     * @param locale  recipient locale
     * @param minutes minutes until the start
     * @return message text
     */
    public String reminder(final Line line, final ZoneId zone, final Locale locale, final long minutes) {
        return msg("telegram.reminder", locale, minutes,
                line.start().withZoneSameInstant(zone).format(TIME), line.sessionType(), line.clientLabel());
    }

    /**
     * Formats a session start for a client, for example "Thursday, 26 November 2026, 10:00".
     *
     * @param start  session start
     * @param zone   client zone
     * @param locale client locale
     * @return localized date and time
     */
    public String clientWhen(final ZonedDateTime start, final ZoneId zone, final Locale locale) {
        final ZonedDateTime local = start.withZoneSameInstant(zone);
        return local.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
                + ", " + local.format(TIME);
    }

    /**
     * Reminder sent to a client about an upcoming session. Contains only the time and session type.
     *
     * @param start       session start
     * @param sessionType session type name, may be blank
     * @param zone        client zone
     * @param locale      client locale
     * @return message text
     */
    public String clientReminder(final ZonedDateTime start, final String sessionType, final ZoneId zone,
                                 final Locale locale) {
        final String text = msg("telegram.client.reminder", locale, clientWhen(start, zone, locale));
        return sessionType == null || sessionType.isBlank() ? text : text + "\n" + sessionType;
    }

    /**
     * Looks up a bot reply.
     *
     * @param key    message key
     * @param locale recipient locale
     * @param args   message arguments
     * @return localized text
     */
    public String text(final String key, final Locale locale, final Object... args) {
        return msg(key, locale, args);
    }

    private String line(final Line l, final ZoneId zone, final Locale locale) {
        final ZonedDateTime start = l.start().withZoneSameInstant(zone);
        final ZonedDateTime end = start.plus(Duration.ofMinutes(l.minutes()));
        return msg("telegram.digest.line", locale, start.format(TIME), end.format(TIME),
                l.sessionType(), l.clientLabel());
    }

    private String msg(final String key, final Locale locale, final Object... args) {
        return messages.getMessage(key, args, locale);
    }
}
