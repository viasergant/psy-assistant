package com.psyassistant.users.rest;

import com.psyassistant.common.exception.ErrorResponse;
import com.psyassistant.users.User;
import com.psyassistant.users.UserRepository;
import com.psyassistant.users.dto.UpdateLanguageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for current user preferences and settings.
 */
@RestController
@RequestMapping("/api/v1/users/me")
@Tag(name = "User Preferences", description = "Current user settings and preferences")
public class UserController {

    private final UserRepository userRepository;

    /**
     * Constructs the controller with its required dependencies.
     *
     * @param userRepository user repository
     */
    public UserController(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Updates the current user's language preference.
     *
     * @param request   language update request
     * @param principalName current user's ID as string
     * @return 204 No Content on success
     */
    @Operation(
            summary = "Update language preference",
            description = "Updates the current authenticated user's preferred language (en, uk)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Language updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid language code",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/language")
    public ResponseEntity<Void> updateLanguage(
            @Valid @RequestBody final UpdateLanguageRequest request,
            @AuthenticationPrincipal final String principalName) {

        UUID userId = UUID.fromString(principalName);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setLanguage(request.language());
        userRepository.save(user);

        return ResponseEntity.noContent().build();
    }
}
