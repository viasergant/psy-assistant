package com.psyassistant.crm.clients;

/**
 * Stores and retrieves profile photos from a backing storage.
 */
public interface ClientPhotoStorage {

    /**
     * Saves profile photo bytes and returns an opaque storage key.
     */
    String savePhoto(byte[] content, String extension);

    /**
     * Loads photo bytes by storage key.
     */
    byte[] loadPhoto(String storageKey);
}
