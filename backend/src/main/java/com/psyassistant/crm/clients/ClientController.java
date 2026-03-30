package com.psyassistant.crm.clients;

import com.psyassistant.common.exception.ErrorResponse;
import com.psyassistant.crm.clients.dto.ClientDetailDto;
import com.psyassistant.crm.clients.dto.UpdateClientProfileRequest;
import com.psyassistant.crm.clients.dto.UpdateClientTagsRequest;
import com.psyassistant.users.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST endpoints for reading client records.
 *
 * <p>Clients are created exclusively via the lead-to-client conversion workflow.
 * Direct create/update endpoints will be added in a later iteration.
 */
@RestController
@RequestMapping("/api/v1/clients")
@Tag(name = "CRM — Client Management",
        description = "Read access to client profiles created from converted leads")
public class ClientController {

    private final ClientProfileService clientProfileService;

    /**
     * Constructs the controller.
     *
     * @param clientProfileService client profile service
     */
    public ClientController(final ClientProfileService clientProfileService) {
        this.clientProfileService = clientProfileService;
    }

    /**
     * Returns the full detail view of a single client.
     *
     * @param id the client's UUID
     * @return 200 with a {@link ClientDetailDto}, or 404 if not found
     */
    @Operation(summary = "Get client",
            description = "Returns the full detail view of a single client")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Client detail",
            content = @Content(schema = @Schema(implementation = ClientDetailDto.class))),
        @ApiResponse(responseCode = "404", description = "Client not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_CLIENTS') "
            + "or hasAuthority('READ_CLIENTS_ALL') "
            + "or hasAuthority('READ_ASSIGNED_CLIENTS')")
    public ResponseEntity<ClientDetailDto> getClient(@PathVariable final UUID id) {
        return ResponseEntity.ok(clientProfileService.getClientProfile(id));
    }

    /**
     * Fully replaces PA-23 slice-one profile fields.
     *
     * @param id      client UUID
     * @param request validated profile update request
     * @param auth    current authentication principal
     * @return 200 with updated profile details
     */
    @Operation(summary = "Update client",
            description = "Full replacement of PA-23 slice-one profile fields")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Client updated",
            content = @Content(schema = @Schema(implementation = ClientDetailDto.class))),
        @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Client not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Update conflict",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_CLIENTS')")
    public ResponseEntity<ClientDetailDto> updateClient(
            @PathVariable final UUID id,
            @Valid @RequestBody final UpdateClientProfileRequest request,
            final Authentication auth) {
        UUID actorId = UserManagementService.currentPrincipalId();
        String actorName = auth != null ? auth.getName() : null;
        return ResponseEntity.ok(clientProfileService.updateClientProfile(id, request, actorId, actorName));
    }

    /**
     * Replaces all client tags in one request.
     */
    @PatchMapping("/{id}/tags")
    @PreAuthorize("hasAuthority('MANAGE_CLIENTS')")
    public ResponseEntity<ClientDetailDto> updateClientTags(
            @PathVariable final UUID id,
            @Valid @RequestBody final UpdateClientTagsRequest request,
            final Authentication auth) {
        UUID actorId = UserManagementService.currentPrincipalId();
        String actorName = auth != null ? auth.getName() : null;
        return ResponseEntity.ok(clientProfileService.updateClientTags(id, request, actorId, actorName));
    }

    /**
     * Uploads or replaces a profile photo for one client.
     */
    @PostMapping(path = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('MANAGE_CLIENTS')")
    public ResponseEntity<ClientDetailDto> uploadClientPhoto(
            @PathVariable final UUID id,
            @RequestParam("version") final Long version,
            @RequestParam("file") final MultipartFile file,
            final Authentication auth) {
        UUID actorId = UserManagementService.currentPrincipalId();
        String actorName = auth != null ? auth.getName() : null;
        return ResponseEntity.ok(
                clientProfileService.uploadClientPhoto(id, version, file, actorId, actorName)
        );
    }

    /**
     * Streams the client profile photo bytes.
     */
    @GetMapping("/{id}/photo")
    @PreAuthorize("hasAuthority('MANAGE_CLIENTS') "
            + "or hasAuthority('READ_CLIENTS_ALL') "
            + "or hasAuthority('READ_ASSIGNED_CLIENTS')")
    public ResponseEntity<byte[]> getClientPhoto(@PathVariable final UUID id) {
        ClientPhotoData photo = clientProfileService.getClientPhoto(id);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(photo.mimeType());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Stored photo has invalid mime type", ex);
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(photo.content());
    }
}
