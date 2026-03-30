package com.psyassistant.crm.clients;

/**
 * Raw photo payload used by API responses.
 *
 * @param content image bytes
 * @param mimeType image content type
 */
public record ClientPhotoData(byte[] content, String mimeType) {
}
