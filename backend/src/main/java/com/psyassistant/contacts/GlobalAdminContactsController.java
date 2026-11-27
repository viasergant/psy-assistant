package com.psyassistant.contacts;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Global administrator endpoints for editing the platform contact details. */
@RestController
@RequestMapping("/api/v1/global-admin/contacts")
@PreAuthorize("hasAuthority('GLOBAL_ADMIN')")
@Tag(name = "Global Admin — Contacts", description = "Edit the public platform contact details")
public class GlobalAdminContactsController {

    private final PlatformContactsService service;

    /**
     * Constructs the controller.
     *
     * @param service contacts service
     */
    public GlobalAdminContactsController(final PlatformContactsService service) {
        this.service = service;
    }

    /**
     * Returns the configured contacts for the admin form.
     *
     * @return 200 with the contacts
     */
    @Operation(summary = "Get platform contacts")
    @GetMapping
    public ResponseEntity<ContactsResponse> get() {
        return ResponseEntity.ok(service.get());
    }

    /**
     * Replaces the platform contacts.
     *
     * @param request new values; blank fields are cleared
     * @return 200 with the stored contacts, or 400 on validation errors
     */
    @Operation(summary = "Update platform contacts")
    @PutMapping
    public ResponseEntity<ContactsResponse> update(@Valid @RequestBody final ContactsRequest request) {
        return ResponseEntity.ok(service.update(request));
    }
}
