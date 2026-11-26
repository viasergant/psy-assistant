package com.psyassistant.notifications.telegram;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Self-service Telegram settings. All operations act on the authenticated user only; no user id is
 * accepted from the request.
 */
@RestController
@RequestMapping("/api/v1/telegram")
public class TelegramSettingsController {

    private final TelegramSettingsService service;
    private final TelegramLinkService linkService;
    private final TelegramProperties props;

    /**
     * Creates the controller.
     *
     * @param service     settings service
     * @param linkService link service
     * @param props       configuration
     */
    public TelegramSettingsController(final TelegramSettingsService service, final TelegramLinkService linkService,
                                      final TelegramProperties props) {
        this.service = service;
        this.linkService = linkService;
        this.props = props;
    }

    /**
     * Returns the caller's settings.
     *
     * @param principal authenticated user
     * @return settings
     */
    @GetMapping("/settings")
    public TelegramSettingsDto get(final Principal principal) {
        return service.get(userId(principal));
    }

    /**
     * Updates the caller's settings.
     *
     * @param principal authenticated user
     * @param request   new values
     * @return updated settings
     */
    @PutMapping("/settings")
    public TelegramSettingsDto update(final Principal principal,
                                      @Valid @RequestBody final UpdateTelegramSettingsRequest request) {
        return service.update(userId(principal), request);
    }

    /**
     * Creates a one-time deep link for connecting a Telegram chat.
     *
     * @param principal authenticated user
     * @return the link
     */
    @PostMapping("/link")
    public Map<String, String> link(final Principal principal) {
        requireOperational();
        return Map.of("url", linkService.createDeepLink(userId(principal)));
    }

    /**
     * Disconnects Telegram.
     *
     * @param principal authenticated user
     * @return 204
     */
    @DeleteMapping("/link")
    public ResponseEntity<Void> unlink(final Principal principal) {
        linkService.unlink(userId(principal));
        return ResponseEntity.noContent().build();
    }

    /**
     * Sends a test message to the caller's chat.
     *
     * @param principal authenticated user
     * @return 202
     */
    @PostMapping("/test")
    public ResponseEntity<Void> test(final Principal principal) {
        requireOperational();
        service.sendTest(userId(principal));
        return ResponseEntity.accepted().build();
    }

    private void requireOperational() {
        if (!props.isOperational()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Telegram is not configured");
        }
    }

    private static UUID userId(final Principal principal) {
        return UUID.fromString(principal.getName());
    }
}
