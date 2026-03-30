package com.psyassistant.crm.clients;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Local-disk implementation used for development and simple deployments.
 */
@Component
public class LocalClientPhotoStorage implements ClientPhotoStorage {

    private final Path baseDir;

    /**
     * Creates local storage with configured base directory.
     *
     * @param storageDir directory where photo files are stored
     */
    public LocalClientPhotoStorage(
            @Value("${app.client-profile.photo.storage-dir:./data/client-photos}")
            final String storageDir) {
        this.baseDir = Path.of(storageDir).toAbsolutePath().normalize();
    }

    @Override
    public String savePhoto(final byte[] content, final String extension) {
        String fileName = Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + extension;
        try {
            Files.createDirectories(baseDir);
            Path target = baseDir.resolve(fileName);
            Files.write(target, content);
            return fileName;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store client profile photo", ex);
        }
    }

    @Override
    public byte[] loadPhoto(final String storageKey) {
        try {
            Path source = baseDir.resolve(storageKey).normalize();
            if (!source.startsWith(baseDir)) {
                throw new IllegalStateException("Invalid photo storage key");
            }
            return Files.readAllBytes(source);
        } catch (IOException ex) {
            throw new IllegalStateException("Client profile photo not found", ex);
        }
    }
}
