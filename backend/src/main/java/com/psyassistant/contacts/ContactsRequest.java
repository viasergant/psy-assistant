package com.psyassistant.contacts;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Update payload for the platform contacts. Every field is optional; blank values clear the field.
 *
 * @param phone            phone number in free international format
 * @param email            e-mail address
 * @param telegramChannels Telegram handles ({@code @name}) or {@code https://t.me/...} links
 */
public record ContactsRequest(
        @Size(max = 32)
        @Pattern(regexp = "^$|^\\+?[0-9 ()\\-]{5,32}$", message = "must be a valid phone number")
        String phone,
        @Size(max = 254) @Email String email,
        @Size(max = ContactsRequest.MAX_CHANNELS)
        List<@Pattern(regexp = "^(@[A-Za-z0-9_]{5,32}|https://t\\.me/[A-Za-z0-9_+/-]{3,64})$",
                message = "must be @handle or an https://t.me/ link") String> telegramChannels) {

    /** Maximum number of Telegram channels. */
    public static final int MAX_CHANNELS = 10;
}
