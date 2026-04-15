package com.psyassistant.notifications.template;

import com.psyassistant.notifications.NotificationEventType;
import com.psyassistant.notifications.template.dto.CreateTemplateRequest;
import com.psyassistant.notifications.template.dto.DeactivateResponse;
import com.psyassistant.notifications.template.dto.PreviewResponse;
import com.psyassistant.notifications.template.dto.TemplateResponse;
import com.psyassistant.notifications.template.dto.UpdateTemplateRequest;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Business logic for notification template management.
 *
 * <p>Invariant: at most one ACTIVE template per (event_type, channel, language).
 * Enforced at service layer via SELECT FOR UPDATE and at DB layer via partial unique index.
 */
@Service
public class NotificationTemplateService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationTemplateService.class);

    private final NotificationTemplateRepository repository;
    private final TemplateVariableService variableService;

    public NotificationTemplateService(
            final NotificationTemplateRepository repository,
            final TemplateVariableService variableService) {
        this.repository = repository;
        this.variableService = variableService;
    }

    // ---- CRUD -----------------------------------------------------------

    /**
     * Lists templates matching the given optional filters (null = no filter for that field).
     */
    @Transactional(readOnly = true)
    public List<TemplateResponse> listTemplates(
            final NotificationEventType eventType,
            final NotificationChannel channel,
            final NotificationLanguage language,
            final TemplateStatus status) {
        return repository.findByFilters(eventType, channel, language, status)
                .stream()
                .map(TemplateResponse::from)
                .toList();
    }

    /**
     * Returns a single template by id.
     *
     * @throws EntityNotFoundException when not found
     */
    @Transactional(readOnly = true)
    public TemplateResponse getTemplate(final UUID id) {
        return TemplateResponse.from(findOrThrow(id));
    }

    /**
     * Creates a new template in INACTIVE state.
     * Validates variables and flags the record if unknown tokens are present.
     */
    @Transactional
    public TemplateResponse createTemplate(final CreateTemplateRequest request) {
        final boolean unknown = variableService.hasUnknownVariables(request.subject(), request.body());
        if (unknown) {
            LOG.warn("Template creation contains unknown variables; templateEventType={} channel={} lang={}",
                    request.eventType(), request.channel(), request.language());
        }
        final NotificationTemplate template = new NotificationTemplate(
                request.eventType(),
                request.channel(),
                request.language(),
                request.subject(),
                request.body(),
                unknown
        );
        return TemplateResponse.from(repository.save(template));
    }

    /**
     * Updates the content of an INACTIVE template.
     *
     * @throws TemplateNotInactiveException when the template is currently ACTIVE
     * @throws EntityNotFoundException      when not found
     */
    @Transactional
    public TemplateResponse updateTemplate(final UUID id, final UpdateTemplateRequest request) {
        final NotificationTemplate template = findOrThrow(id);
        if (template.isActive()) {
            throw new TemplateNotInactiveException(id);
        }
        final boolean unknown = variableService.hasUnknownVariables(request.subject(), request.body());
        if (unknown) {
            LOG.warn("Template update contains unknown variables; templateId={}", id);
        }
        template.updateContent(request.subject(), request.body(), unknown);
        return TemplateResponse.from(repository.save(template));
    }

    /**
     * Activates a template, auto-deactivating the existing ACTIVE one for the same combo.
     *
     * <p>Uses SELECT FOR UPDATE to prevent concurrent race conditions.
     * Maps {@link DataIntegrityViolationException} → HTTP 409 as a safety net for the
     * DB partial unique index.
     *
     * @throws EntityNotFoundException when the template to activate is not found
     */
    @Transactional
    public TemplateResponse activateTemplate(final UUID id) {
        final NotificationTemplate toActivate = findOrThrow(id);

        // Lock and deactivate any existing active template for this combo.
        final Optional<NotificationTemplate> existing = repository.findActiveForUpdateLocked(
                toActivate.getEventType(), toActivate.getChannel(), toActivate.getLanguage());
        existing.ifPresent(prev -> {
            if (!id.equals(prev.getId())) {
                LOG.info("Deactivating previous active template id={} for eventType={} channel={} lang={}",
                        prev.getId(), prev.getEventType(), prev.getChannel(), prev.getLanguage());
                prev.deactivate();
                repository.save(prev);
            }
        });

        toActivate.activate();
        try {
            return TemplateResponse.from(repository.saveAndFlush(toActivate));
        } catch (DataIntegrityViolationException ex) {
            LOG.warn("Concurrent activation conflict for templateId={}", id);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Another template was activated concurrently for the same event/channel/language");
        }
    }

    /**
     * Deactivates a template and returns a warning if no other active template
     * exists for the same event_type/channel/language combination.
     *
     * @throws EntityNotFoundException when not found
     */
    @Transactional
    public DeactivateResponse deactivateTemplate(final UUID id) {
        final NotificationTemplate template = findOrThrow(id);
        template.deactivate();
        repository.save(template);

        final boolean noReplacement = repository
                .findByEventTypeAndChannelAndLanguageAndStatus(
                        template.getEventType(),
                        template.getChannel(),
                        template.getLanguage(),
                        TemplateStatus.ACTIVE)
                .isEmpty();

        if (noReplacement) {
            LOG.warn("No active template after deactivation; eventType={} channel={} lang={}",
                    template.getEventType(), template.getChannel(), template.getLanguage());
        }
        return new DeactivateResponse(TemplateResponse.from(template), noReplacement);
    }

    /**
     * Renders the template's subject and body with sample substitution data for preview.
     *
     * @throws EntityNotFoundException when not found
     */
    @Transactional(readOnly = true)
    public PreviewResponse previewTemplate(final UUID id) {
        final NotificationTemplate template = findOrThrow(id);
        final String renderedSubject = variableService.render(
                template.getSubject(), TemplateVariableService.SAMPLE_CONTEXT);
        final String renderedBody = variableService.render(
                template.getBody(), TemplateVariableService.SAMPLE_CONTEXT);
        return new PreviewResponse(renderedSubject, renderedBody);
    }

    /**
     * Deletes a template only if it is INACTIVE.
     *
     * @throws TemplateNotInactiveException when the template is ACTIVE
     * @throws EntityNotFoundException      when not found
     */
    @Transactional
    public void deleteTemplate(final UUID id) {
        final NotificationTemplate template = findOrThrow(id);
        if (template.isActive()) {
            throw new TemplateNotInactiveException(id);
        }
        repository.delete(template);
    }

    // ---- internal helpers -----------------------------------------------

    private NotificationTemplate findOrThrow(final UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notification template not found: " + id));
    }
}
