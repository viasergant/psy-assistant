package com.psyassistant.notifications.telegram;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalTime;

/**
 * Update of notification preferences.
 *
 * @param timezone              IANA zone id
 * @param language              en or uk
 * @param digestEnabled         daily digest on/off
 * @param digestTime            local digest time
 * @param digestDays            weekday bit mask
 * @param skipEmptyDigest       skip empty days
 * @param reminderEnabled       reminders on/off
 * @param reminderMinutesBefore lead time in minutes
 * @param includeClientNames    show first names instead of client codes
 */
public record UpdateTelegramSettingsRequest(
        @NotBlank String timezone,
        @NotNull @Pattern(regexp = "en|uk") String language,
        boolean digestEnabled,
        @NotNull LocalTime digestTime,
        @Min(0) @Max(127) int digestDays,
        boolean skipEmptyDigest,
        boolean reminderEnabled,
        @Min(5) @Max(1440) int reminderMinutesBefore,
        boolean includeClientNames) { }
