package com.psyassistant.notifications.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link NotificationTemplateService}.
 */
@ExtendWith(MockitoExtension.class)
class NotificationTemplateServiceTest {

    @Mock
    private NotificationTemplateRepository repository;

    private NotificationTemplateService service;

    @BeforeEach
    void setUp() {
        service = new NotificationTemplateService(repository, new TemplateVariableService());
    }

    // ---- helpers ----------------------------------------------------------

    private NotificationTemplate makeTemplate(final TemplateStatus status) {
        final NotificationTemplate t = new NotificationTemplate(
                NotificationEventType.APPOINTMENT_REMINDER,
                NotificationChannel.EMAIL,
                NotificationLanguage.EN,
                "Hello {{client_name}}",
                "<p>Your appointment is on {{appointment_date}}</p>",
                false
        );
        if (status == TemplateStatus.ACTIVE) {
            t.activate();
        }
        return t;
    }

    // ---- listTemplates ----------------------------------------------------

    @Test
    @DisplayName("listTemplates returns mapped DTOs")
    void listTemplatesReturnsMappedDtos() {
        final NotificationTemplate t = makeTemplate(TemplateStatus.INACTIVE);
        when(repository.findByFilters(null, null, null, null)).thenReturn(List.of(t));

        final List<TemplateResponse> result = service.listTemplates(null, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(TemplateStatus.INACTIVE);
    }

    // ---- getTemplate ------------------------------------------------------

    @Test
    @DisplayName("getTemplate throws EntityNotFoundException when not found")
    void getTemplateNotFoundThrows() {
        final UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTemplate(id))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---- createTemplate ---------------------------------------------------

    @Nested
    @DisplayName("createTemplate")
    class Create {

        @Test
        @DisplayName("saves template as INACTIVE with no unknown variables")
        void createSavedAsInactive() {
            final CreateTemplateRequest req = new CreateTemplateRequest(
                    NotificationEventType.WELCOME,
                    NotificationChannel.EMAIL,
                    NotificationLanguage.EN,
                    "Welcome {{client_name}}",
                    "<p>Hello {{client_name}}</p>"
            );
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            final TemplateResponse result = service.createTemplate(req);

            assertThat(result.status()).isEqualTo(TemplateStatus.INACTIVE);
            assertThat(result.hasUnknownVariables()).isFalse();
        }

        @Test
        @DisplayName("flags hasUnknownVariables when body contains unknown token")
        void createUnknownVariableFlagged() {
            final CreateTemplateRequest req = new CreateTemplateRequest(
                    NotificationEventType.WELCOME,
                    NotificationChannel.EMAIL,
                    NotificationLanguage.EN,
                    "Subject",
                    "<p>Hello {{unknown_token}}</p>"
            );
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            final TemplateResponse result = service.createTemplate(req);

            assertThat(result.hasUnknownVariables()).isTrue();
        }
    }

    // ---- updateTemplate ---------------------------------------------------

    @Test
    @DisplayName("updateTemplate throws TemplateNotInactiveException when template is ACTIVE")
    void updateTemplateActiveThrows() {
        final UUID id = UUID.randomUUID();
        final NotificationTemplate active = makeTemplate(TemplateStatus.ACTIVE);
        when(repository.findById(id)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service.updateTemplate(id,
                new UpdateTemplateRequest("Subject", "Body")))
                .isInstanceOf(TemplateNotInactiveException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("updateTemplate re-validates variables and saves")
    void updateTemplateInactiveSavesAndRevalidates() {
        final UUID id = UUID.randomUUID();
        final NotificationTemplate template = makeTemplate(TemplateStatus.INACTIVE);
        when(repository.findById(id)).thenReturn(Optional.of(template));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        final TemplateResponse result = service.updateTemplate(id,
                new UpdateTemplateRequest("New Subject", "New {{client_name}} body"));

        assertThat(result.hasUnknownVariables()).isFalse();
        verify(repository).save(template);
    }

    // ---- activateTemplate -------------------------------------------------

    @Test
    @DisplayName("activateTemplate deactivates existing active and activates new one")
    void activateTemplateDeactivatesPrevious() {
        final NotificationTemplate existing = makeTemplate(TemplateStatus.ACTIVE);
        final NotificationTemplate toActivate = makeTemplate(TemplateStatus.INACTIVE);
        final UUID toActivateId = UUID.randomUUID();

        when(repository.findById(toActivateId)).thenReturn(Optional.of(toActivate));
        when(repository.findActiveForUpdateLocked(
                toActivate.getEventType(), toActivate.getChannel(), toActivate.getLanguage()))
                .thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        service.activateTemplate(toActivateId);

        assertThat(existing.getStatus()).isEqualTo(TemplateStatus.INACTIVE);
        assertThat(toActivate.getStatus()).isEqualTo(TemplateStatus.ACTIVE);
    }

    @Test
    @DisplayName("activateTemplate with no prior active just activates")
    void activateTemplateNoPriorActivates() {
        final NotificationTemplate toActivate = makeTemplate(TemplateStatus.INACTIVE);
        final UUID id = UUID.randomUUID();

        when(repository.findById(id)).thenReturn(Optional.of(toActivate));
        when(repository.findActiveForUpdateLocked(any(), any(), any())).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        service.activateTemplate(id);

        assertThat(toActivate.getStatus()).isEqualTo(TemplateStatus.ACTIVE);
    }

    // ---- deactivateTemplate -----------------------------------------------

    @Test
    @DisplayName("deactivateTemplate sets INACTIVE and warns when no replacement active")
    void deactivateTemplateNoReplacementWarnsFlagSet() {
        final UUID id = UUID.randomUUID();
        final NotificationTemplate active = makeTemplate(TemplateStatus.ACTIVE);

        when(repository.findById(id)).thenReturn(Optional.of(active));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.findByEventTypeAndChannelAndLanguageAndStatus(
                active.getEventType(), active.getChannel(), active.getLanguage(),
                TemplateStatus.ACTIVE)).thenReturn(Optional.empty());

        final DeactivateResponse response = service.deactivateTemplate(id);

        assertThat(response.noActiveReplacement()).isTrue();
        assertThat(response.template().status()).isEqualTo(TemplateStatus.INACTIVE);
    }

    @Test
    @DisplayName("deactivateTemplate noActiveReplacement=false when another active exists")
    void deactivateTemplateReplacementExistsFlagFalse() {
        final UUID id = UUID.randomUUID();
        final NotificationTemplate active = makeTemplate(TemplateStatus.ACTIVE);
        final NotificationTemplate other = makeTemplate(TemplateStatus.ACTIVE);

        when(repository.findById(id)).thenReturn(Optional.of(active));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.findByEventTypeAndChannelAndLanguageAndStatus(
                active.getEventType(), active.getChannel(), active.getLanguage(),
                TemplateStatus.ACTIVE)).thenReturn(Optional.of(other));

        final DeactivateResponse response = service.deactivateTemplate(id);

        assertThat(response.noActiveReplacement()).isFalse();
    }

    // ---- deleteTemplate ---------------------------------------------------

    @Test
    @DisplayName("deleteTemplate throws TemplateNotInactiveException when ACTIVE")
    void deleteTemplateActiveThrows() {
        final UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(makeTemplate(TemplateStatus.ACTIVE)));

        assertThatThrownBy(() -> service.deleteTemplate(id))
                .isInstanceOf(TemplateNotInactiveException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteTemplate deletes INACTIVE template")
    void deleteTemplateInactiveDeletes() {
        final UUID id = UUID.randomUUID();
        final NotificationTemplate template = makeTemplate(TemplateStatus.INACTIVE);
        when(repository.findById(id)).thenReturn(Optional.of(template));

        service.deleteTemplate(id);

        verify(repository).delete(template);
    }

    // ---- previewTemplate --------------------------------------------------

    @Test
    @DisplayName("previewTemplate substitutes sample values")
    void previewTemplateSubstitutesSampleValues() {
        final UUID id = UUID.randomUUID();
        final NotificationTemplate template = new NotificationTemplate(
                NotificationEventType.APPOINTMENT_REMINDER,
                NotificationChannel.EMAIL,
                NotificationLanguage.EN,
                "Hello {{client_name}}",
                "<p>Your appointment: {{appointment_date}} at {{location}}</p>",
                false
        );
        when(repository.findById(id)).thenReturn(Optional.of(template));

        final PreviewResponse preview = service.previewTemplate(id);

        assertThat(preview.renderedSubject()).isEqualTo("Hello Іван Петренко");
        assertThat(preview.renderedBody()).contains("2026-04-01").contains("Office 3");
    }
}
