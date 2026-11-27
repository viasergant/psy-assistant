package com.psyassistant.contacts;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unauthenticated endpoint with the platform contact details for the public Contacts page.
 * Permitted by {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/contacts")
@Tag(name = "Contacts", description = "Public platform contact details")
public class PublicContactsController {

    private final PlatformContactsService service;

    /**
     * Constructs the controller.
     *
     * @param service contacts service
     */
    public PublicContactsController(final PlatformContactsService service) {
        this.service = service;
    }

    /**
     * Returns the configured contacts.
     *
     * @return 200 with phone, email and Telegram links (empty values when unconfigured)
     */
    @Operation(summary = "Get platform contacts")
    @GetMapping
    public ResponseEntity<ContactsResponse> get() {
        return ResponseEntity.ok(service.get());
    }
}
