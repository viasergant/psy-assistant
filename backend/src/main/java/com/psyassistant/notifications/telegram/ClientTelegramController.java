package com.psyassistant.notifications.telegram;

import com.psyassistant.crm.clients.ClientProfileService;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lets a therapist invite a client to the Telegram bot and see/revoke the link.
 *
 * <p>Every call first loads the client through {@link ClientProfileService#getClientProfile}, which is
 * org-filtered and enforces the assigned-therapist rule, so a client id from another organization (or
 * an unassigned client) yields 404/403 and never reaches the link service.
 */
@RestController
@RequestMapping("/api/v1/clients/{clientId}/telegram")
public class ClientTelegramController {

    private static final String ACCESS = "hasAuthority('MANAGE_CLIENTS') or hasAuthority('READ_CLIENTS_ALL') "
            + "or hasAuthority('READ_ASSIGNED_CLIENTS')";
    private static final Set<String> LANGUAGES = Set.of("en", "uk");

    private final ClientProfileService clientProfileService;
    private final TelegramLinkService linkService;
    private final TelegramProperties props;

    /**
     * Creates the controller.
     *
     * @param clientProfileService access-checked client loader
     * @param linkService          link service
     * @param props                configuration
     */
    public ClientTelegramController(final ClientProfileService clientProfileService,
                                    final TelegramLinkService linkService, final TelegramProperties props) {
        this.clientProfileService = clientProfileService;
        this.linkService = linkService;
        this.props = props;
    }

    /**
     * Returns whether the client has a linked Telegram chat.
     *
     * @param clientId client id
     * @return {@code {available, linked}}
     */
    @GetMapping
    @PreAuthorize(ACCESS)
    public Map<String, Boolean> status(@PathVariable final UUID clientId) {
        clientProfileService.getClientProfile(clientId);
        return Map.of("available", props.isOperational(), "linked", linkService.isClientLinked(clientId));
    }

    /**
     * Creates a one-time invite link for the client.
     *
     * @param clientId client id
     * @param language language of the bot's replies to the client (en or uk)
     * @param timezone IANA zone in which session times are shown to the client
     * @return the link
     */
    @PostMapping("/link")
    @PreAuthorize(ACCESS)
    public Map<String, String> link(@PathVariable final UUID clientId,
                                    @RequestParam(defaultValue = "en") final String language,
                                    @RequestParam(defaultValue = "UTC") final String timezone) {
        if (!props.isOperational()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Telegram is not configured");
        }
        if (!LANGUAGES.contains(language)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported language");
        }
        if (!ZoneId.getAvailableZoneIds().contains(timezone)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown timezone");
        }
        clientProfileService.getClientProfile(clientId);
        return Map.of("url", linkService.createClientDeepLink(clientId, language, timezone));
    }

    /**
     * Disconnects the client's Telegram chat.
     *
     * @param clientId client id
     * @return 204
     */
    @DeleteMapping
    @PreAuthorize(ACCESS)
    public ResponseEntity<Void> unlink(@PathVariable final UUID clientId) {
        clientProfileService.getClientProfile(clientId);
        linkService.unlinkClient(clientId);
        return ResponseEntity.noContent().build();
    }
}
