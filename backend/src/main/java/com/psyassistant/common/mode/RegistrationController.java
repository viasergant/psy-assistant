package com.psyassistant.common.mode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints for self-service solo therapist registration with email confirmation.
 *
 * <p>Active only in the {@code saas} Spring profile. Registration creates an inactive
 * account and emails a confirmation link; no tokens are issued until the link is used.
 * All endpoints answer identically whether or not the email address is already known.
 */
@RestController
@Profile("saas")
@RequestMapping("/api/v1/register")
public class RegistrationController {

    private final RegistrationService registrationService;

    /**
     * Constructs the controller.
     *
     * @param registrationService registration use cases
     */
    public RegistrationController(final RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    /**
     * Starts registration of a new solo therapist organization.
     *
     * @param request validated registration data
     * @param http    request, used for the caller IP
     * @return 202 Accepted with an empty body
     */
    @PostMapping
    public ResponseEntity<Void> register(
            @Valid @RequestBody final RegistrationRequest request,
            final HttpServletRequest http) {
        registrationService.register(request, http.getRemoteAddr());
        return ResponseEntity.accepted().build();
    }

    /**
     * Confirms the email address and activates the account.
     *
     * @param request body carrying the token from the email link
     * @return 204 No Content
     */
    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(@Valid @RequestBody final ConfirmEmailRequest request) {
        registrationService.confirm(request.token());
        return ResponseEntity.noContent().build();
    }

    /**
     * Re-sends the confirmation email.
     *
     * @param request validated request
     * @param http    request, used for the caller IP
     * @return 202 Accepted with an empty body
     */
    @PostMapping("/resend")
    public ResponseEntity<Void> resend(
            @Valid @RequestBody final ResendConfirmationRequest request,
            final HttpServletRequest http) {
        registrationService.resendConfirmation(request, http.getRemoteAddr());
        return ResponseEntity.accepted().build();
    }
}
