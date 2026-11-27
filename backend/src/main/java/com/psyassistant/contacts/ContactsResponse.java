package com.psyassistant.contacts;

import java.util.List;

/**
 * Contact details returned by both the public and the global admin endpoints.
 *
 * @param phone            phone number, or null when not configured
 * @param email            e-mail address, or null when not configured
 * @param telegramChannels normalised {@code https://t.me/...} links, never null
 */
public record ContactsResponse(String phone, String email, List<String> telegramChannels) {
}
