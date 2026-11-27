package com.psyassistant.contacts;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads and updates the platform-wide contact details. */
@Service
public class PlatformContactsService {

    private static final String SEPARATOR = "\n";
    private static final String TELEGRAM_BASE = "https://t.me/";

    private final PlatformContactsRepository repository;

    /**
     * Constructs the service.
     *
     * @param repository contacts repository
     */
    public PlatformContactsService(final PlatformContactsRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns the current contacts, or empty values when nothing is configured yet.
     *
     * @return contacts, never null
     */
    @Transactional(readOnly = true)
    public ContactsResponse get() {
        return repository.findById(PlatformContacts.SINGLETON_ID)
                .map(PlatformContactsService::toResponse)
                .orElseGet(() -> new ContactsResponse(null, null, List.of()));
    }

    /**
     * Replaces the contacts. Telegram handles are normalised to {@code https://t.me/} links.
     *
     * @param request validated update payload
     * @return the stored contacts
     */
    @Transactional
    public ContactsResponse update(final ContactsRequest request) {
        String channels = normaliseChannels(request.telegramChannels());
        PlatformContacts contacts = repository.findById(PlatformContacts.SINGLETON_ID)
                .orElseGet(() -> new PlatformContacts(null, null, null));
        contacts.update(blankToNull(request.phone()), blankToNull(request.email()), channels);
        return toResponse(repository.save(contacts));
    }

    private static String normaliseChannels(final List<String> raw) {
        if (raw == null) {
            return null;
        }
        List<String> links = raw.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.startsWith("@") ? TELEGRAM_BASE + s.substring(1) : s)
                .distinct()
                .toList();
        return links.isEmpty() ? null : String.join(SEPARATOR, links);
    }

    private static String blankToNull(final String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ContactsResponse toResponse(final PlatformContacts contacts) {
        List<String> channels = contacts.getTelegramChannels() == null
                ? List.of()
                : Arrays.asList(contacts.getTelegramChannels().split(SEPARATOR));
        return new ContactsResponse(contacts.getPhone(), contacts.getEmail(), channels);
    }
}
