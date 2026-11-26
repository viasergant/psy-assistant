package com.psyassistant.notifications.telegram;

import java.time.Instant;

/**
 * Settings as exposed to the owner. The chat id is never exposed.
 *
 * @param available             whether Telegram is configured on this server
 * @param linked                whether a chat is linked
 * @param linkedAt              link time
 * @param timezone              IANA zone
 * @param language              en or uk
 * @param digestEnabled         daily digest on/off
 * @param digestTime            local time, HH:mm
 * @param digestDays            weekday bit mask (bit 0 = Monday)
 * @param skipEmptyDigest       skip the digest on days without sessions
 * @param reminderEnabled       reminders on/off
 * @param reminderMinutesBefore lead time in minutes
 * @param includeClientNames    show client first names instead of codes
 */
public record TelegramSettingsDto(
        boolean available, boolean linked, Instant linkedAt, String timezone, String language,
        boolean digestEnabled, String digestTime, int digestDays, boolean skipEmptyDigest,
        boolean reminderEnabled, int reminderMinutesBefore, boolean includeClientNames) { }
