package com.psyassistant.notifications.template;

import com.psyassistant.notifications.NotificationEventType;
import com.psyassistant.notifications.template.dto.CreateTemplateRequest;
import com.psyassistant.notifications.template.dto.DeactivateResponse;
import com.psyassistant.notifications.template.dto.PreviewResponse;
import com.psyassistant.notifications.template.dto.TemplateResponse;
import com.psyassistant.notifications.template.dto.UpdateTemplateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for notification template management.
 *
 * <p>All endpoints require the {@code MANAGE_NOTIFICATION_TEMPLATES} authority
 * (held exclusively by the SYSTEM_ADMINISTRATOR role).
 *
 * <p>Supported variable tokens (use in subject/body):
 * {@code {{client_name}}}, {@code {{therapist_name}}},
 * {@code {{appointment_datetime}}}, {@code {{appointment_date}}},
 * {@code {{appointment_time}}}, {@code {{location}}}, {@code {{organization_name}}}
 */
@RestController
@RequestMapping("/api/admin/notification-templates")
@PreAuthorize("hasAuthority('MANAGE_NOTIFICATION_TEMPLATES')")
@Tag(name = "Admin — Notification Templates",
        description = "Create, edit, preview, and activate/deactivate notification message templates")
public class NotificationTemplateController {

    private final NotificationTemplateService service;

    public NotificationTemplateController(final NotificationTemplateService service) {
        this.service = service;
    }

    @Operation(summary = "List templates",
            description = "Returns all templates, optionally filtered by eventType, channel, language, or status")
    @GetMapping
    public ResponseEntity<List<TemplateResponse>> list(
            @RequestParam(required = false) final NotificationEventType eventType,
            @RequestParam(required = false) final NotificationChannel channel,
            @RequestParam(required = false) final NotificationLanguage language,
            @RequestParam(required = false) final TemplateStatus status) {
        return ResponseEntity.ok(service.listTemplates(eventType, channel, language, status));
    }

    @Operation(summary = "Get template by id")
    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> getById(@PathVariable final UUID id) {
        return ResponseEntity.ok(service.getTemplate(id));
    }

    @Operation(summary = "Create template",
            description = "Saves a new template in INACTIVE state. Returns 201. "
                    + "A warning flag (hasUnknownVariables) is set if unrecognized tokens are detected.")
    @PostMapping
    public ResponseEntity<TemplateResponse> create(
            @Valid @RequestBody final CreateTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createTemplate(request));
    }

    @Operation(summary = "Update template",
            description = "Updates subject and body. Only allowed on INACTIVE templates.")
    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponse> update(
            @PathVariable final UUID id,
            @Valid @RequestBody final UpdateTemplateRequest request) {
        return ResponseEntity.ok(service.updateTemplate(id, request));
    }

    @Operation(summary = "Activate template",
            description = "Activates this template and auto-deactivates any previous ACTIVE one "
                    + "for the same event_type/channel/language. Returns 409 on concurrent conflict.")
    @PostMapping("/{id}/activate")
    public ResponseEntity<TemplateResponse> activate(@PathVariable final UUID id) {
        return ResponseEntity.ok(service.activateTemplate(id));
    }

    @Operation(summary = "Deactivate template",
            description = "Deactivates this template. Response includes noActiveReplacement=true "
                    + "if no other active template exists for the same event_type/channel/language.")
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<DeactivateResponse> deactivate(@PathVariable final UUID id) {
        return ResponseEntity.ok(service.deactivateTemplate(id));
    }

    @Operation(summary = "Preview template",
            description = "Renders the template subject and body with sample substitution data "
                    + "(Іван Петренко / 2026-04-01 10:00 / Office 3). No persistence.")
    @PostMapping("/{id}/preview")
    public ResponseEntity<PreviewResponse> preview(@PathVariable final UUID id) {
        return ResponseEntity.ok(service.previewTemplate(id));
    }

    @Operation(summary = "Delete template",
            description = "Permanently deletes the template. Only allowed on INACTIVE templates.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable final UUID id) {
        service.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
}
