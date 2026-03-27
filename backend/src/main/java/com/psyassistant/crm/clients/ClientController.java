package com.psyassistant.crm.clients;

import com.psyassistant.common.exception.ErrorResponse;
import com.psyassistant.crm.clients.dto.ClientDetailDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    private final ClientRepository clientRepository;

    /**
     * Constructs the controller.
     *
     * @param clientRepository client data access
     */
    public ClientController(final ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
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
    @PreAuthorize("hasAuthority('MANAGE_LEADS') or hasAuthority('READ_LEADS')")
    public ResponseEntity<ClientDetailDto> getClient(@PathVariable final UUID id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + id));
        return ResponseEntity.ok(ClientDetailDto.from(client));
    }
}
