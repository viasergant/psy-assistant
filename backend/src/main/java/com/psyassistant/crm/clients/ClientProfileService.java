package com.psyassistant.crm.clients;

import com.psyassistant.crm.clients.audit.ClientProfileAuditRecorder;
import com.psyassistant.crm.clients.dto.ClientDetailDto;
import com.psyassistant.crm.clients.dto.ClientSummaryDto;
import com.psyassistant.crm.clients.dto.UpdateClientProfileRequest;
import com.psyassistant.crm.clients.dto.UpdateClientTagsRequest;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service layer for client profile read and update operations.
 */
@Service
public class ClientProfileService {

        private static final Set<String> ALLOWED_PHOTO_MIME_TYPES = Set.of(
                        "image/jpeg",
                        "image/png",
                        "image/webp"
        );

    private final ClientRepository clientRepository;
        private final ClientTagRepository clientTagRepository;
    private final ClientProfileAuditRecorder auditRecorder;
        private final ClientPhotoStorage photoStorage;
        private final long photoMaxBytes;

    /**
     * Constructs the service.
     */
    public ClientProfileService(final ClientRepository clientRepository,
                                final ClientTagRepository clientTagRepository,
                                final ClientProfileAuditRecorder auditRecorder,
                                final ClientPhotoStorage photoStorage,
                                @Value("${app.client-profile.photo.max-bytes:5242880}")
                                final long photoMaxBytes) {
        this.clientRepository = clientRepository;
        this.clientTagRepository = clientTagRepository;
        this.auditRecorder = auditRecorder;
        this.photoStorage = photoStorage;
        this.photoMaxBytes = photoMaxBytes;
    }

    /**
     * Returns one client profile with capability flags based on current authorities.
     */
    @Transactional(readOnly = true)
    public ClientDetailDto getClientProfile(final UUID id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + id));

        boolean canManageClients = hasAuthority("MANAGE_CLIENTS");
        boolean canReadAll = hasAuthority("READ_CLIENTS_ALL");
        boolean canReadAssigned = hasAuthority("READ_ASSIGNED_CLIENTS");

        if (!canManageClients && !canReadAll) {
            enforceAssignedTherapistRead(client, canReadAssigned);
        }

        return ClientDetailDto.from(
                client,
                getSortedTags(client.getId()),
                buildPhotoUrl(client),
                canManageClients,
                canManageClients,
                canManageClients
        );
    }

    /**
     * Returns a list of all clients as lightweight summary DTOs for dropdown/selection UIs.
     * Sorted alphabetically by display name.
     */
    @Transactional(readOnly = true)
    public List<ClientSummaryDto> getAllClients() {
        boolean canManageClients = hasAuthority("MANAGE_CLIENTS");
        boolean canReadAll = hasAuthority("READ_CLIENTS_ALL");
        boolean canReadAssigned = hasAuthority("READ_ASSIGNED_CLIENTS");

        if (!canManageClients && !canReadAll && !canReadAssigned) {
            throw new AccessDeniedException("Access denied");
        }

        return clientRepository.findAll().stream()
                .map(ClientSummaryDto::from)
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .toList();
    }

    /**
     * Performs a full replacement update of PA-23 slice-one fields.
     */
    @Transactional
    public ClientDetailDto updateClientProfile(final UUID id,
                                               final UpdateClientProfileRequest request,
                                               final UUID actorId,
                                               final String actorName) {
        if (!hasAuthority("MANAGE_CLIENTS")) {
            throw new AccessDeniedException("Access denied");
        }

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + id));

        if (!Objects.equals(client.getVersion(), request.version())) {
            throw new ObjectOptimisticLockingFailureException(Client.class, id);
        }

        List<ClientProfileAuditRecorder.FieldChange> changes = new ArrayList<>();

        applyIfChanged(changes, "fullName", client.getFullName(), request.fullName(),
                client::setFullName);
        applyIfChanged(changes, "preferredName", client.getPreferredName(), request.preferredName(),
                client::setPreferredName);
        applyIfChanged(changes, "dateOfBirth", client.getDateOfBirth(), request.dateOfBirth(),
                client::setDateOfBirth);
        applyIfChanged(changes, "sexOrGender", client.getSexOrGender(), request.sexOrGender(),
                client::setSexOrGender);
        applyIfChanged(changes, "pronouns", client.getPronouns(), request.pronouns(),
                client::setPronouns);
        applyIfChanged(changes, "ownerId", client.getOwnerId(), request.ownerId(),
                client::setOwnerId);
        applyIfChanged(changes, "assignedTherapistId", client.getAssignedTherapistId(),
                request.assignedTherapistId(), client::setAssignedTherapistId);
        applyIfChanged(changes, "notes", client.getNotes(), request.notes(), client::setNotes);
        applyIfChanged(changes, "email", client.getEmail(), request.email(), client::setEmail);
        applyIfChanged(changes, "phone", client.getPhone(), request.phone(), client::setPhone);
        applyIfChanged(changes, "secondaryPhone", client.getSecondaryPhone(),
                request.secondaryPhone(), client::setSecondaryPhone);
        applyIfChanged(changes, "addressLine1", client.getAddressLine1(), request.addressLine1(),
                client::setAddressLine1);
        applyIfChanged(changes, "addressLine2", client.getAddressLine2(), request.addressLine2(),
                client::setAddressLine2);
        applyIfChanged(changes, "city", client.getCity(), request.city(), client::setCity);
        applyIfChanged(changes, "region", client.getRegion(), request.region(), client::setRegion);
        applyIfChanged(changes, "postalCode", client.getPostalCode(), request.postalCode(),
                client::setPostalCode);
        applyIfChanged(changes, "country", client.getCountry(), request.country(),
                client::setCountry);
        applyIfChanged(changes, "referralSource", client.getReferralSource(),
                request.referralSource(), client::setReferralSource);
        applyIfChanged(changes, "referralContactName", client.getReferralContactName(),
                request.referralContactName(), client::setReferralContactName);
        applyIfChanged(changes, "referralNotes", client.getReferralNotes(), request.referralNotes(),
                client::setReferralNotes);
        applyIfChanged(changes, "preferredCommunicationMethod",
                client.getPreferredCommunicationMethod(), request.preferredCommunicationMethod(),
                client::setPreferredCommunicationMethod);
        applyIfChanged(changes, "allowPhone", client.getAllowPhone(), request.allowPhone(),
                client::setAllowPhone);
        applyIfChanged(changes, "allowSms", client.getAllowSms(), request.allowSms(),
                client::setAllowSms);
        applyIfChanged(changes, "allowEmail", client.getAllowEmail(), request.allowEmail(),
                client::setAllowEmail);
        applyIfChanged(changes, "allowVoicemail", client.getAllowVoicemail(),
                request.allowVoicemail(), client::setAllowVoicemail);
        applyIfChanged(changes, "emergencyContactName", client.getEmergencyContactName(),
                request.emergencyContactName(), client::setEmergencyContactName);
        applyIfChanged(changes, "emergencyContactRelationship",
                client.getEmergencyContactRelationship(), request.emergencyContactRelationship(),
                client::setEmergencyContactRelationship);
        applyIfChanged(changes, "emergencyContactPhone", client.getEmergencyContactPhone(),
                request.emergencyContactPhone(), client::setEmergencyContactPhone);
        applyIfChanged(changes, "emergencyContactEmail", client.getEmergencyContactEmail(),
                request.emergencyContactEmail(), client::setEmergencyContactEmail);

        Client saved = clientRepository.save(client);
        auditRecorder.recordProfileUpdate(saved.getId(), actorId, actorName, changes);

        return ClientDetailDto.from(
                saved,
                getSortedTags(saved.getId()),
                buildPhotoUrl(saved),
                true,
                true,
                true
        );
    }

    /**
     * Replaces all tags for one client.
     */
    @Transactional
    public ClientDetailDto updateClientTags(final UUID id,
                                            final UpdateClientTagsRequest request,
                                            final UUID actorId,
                                            final String actorName) {
        if (!hasAuthority("MANAGE_CLIENTS")) {
            throw new AccessDeniedException("Access denied");
        }

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + id));

        if (!Objects.equals(client.getVersion(), request.version())) {
            throw new ObjectOptimisticLockingFailureException(Client.class, id);
        }

        List<String> previousTags = getSortedTags(client.getId());
        List<String> normalized = normalizeTags(request.tags());

        clientTagRepository.deleteAllByClientId(client.getId());
        clientTagRepository.flush();
        if (!normalized.isEmpty()) {
            List<ClientTag> tagEntities = normalized.stream()
                    .map(tag -> new ClientTag(client, tag))
                    .toList();
            clientTagRepository.saveAll(tagEntities);
        }

        Client saved = clientRepository.save(client);
        if (!Objects.equals(previousTags, normalized)) {
            auditRecorder.recordProfileUpdate(saved.getId(), actorId, actorName, List.of(
                    new ClientProfileAuditRecorder.FieldChange("tags", previousTags, normalized)
            ));
        }

        return ClientDetailDto.from(saved, normalized, buildPhotoUrl(saved), true, true, true);
    }

    /**
     * Uploads and replaces a profile photo for one client.
     */
    @Transactional
    public ClientDetailDto uploadClientPhoto(final UUID id,
                                             final Long version,
                                             final MultipartFile file,
                                             final UUID actorId,
                                             final String actorName) {
        if (!hasAuthority("MANAGE_CLIENTS")) {
            throw new AccessDeniedException("Access denied");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo file is required");
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_PHOTO_MIME_TYPES.contains(mimeType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported photo type. Allowed: image/jpeg, image/png, image/webp");
        }
        if (file.getSize() > photoMaxBytes) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Photo size exceeds configured limit");
        }

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + id));

        if (!Objects.equals(client.getVersion(), version)) {
            throw new ObjectOptimisticLockingFailureException(Client.class, id);
        }

        String extension = switch (mimeType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };

        byte[] content;
        try {
            content = file.getBytes();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Failed to read photo payload", ex);
        }

        String previousStorageKey = client.getPhotoStorageKey();
        String storageKey = photoStorage.savePhoto(content, extension);
        client.setPhotoStorageKey(storageKey);
        client.setPhotoMimeType(mimeType);
        client.setPhotoUpdatedAt(OffsetDateTime.now());
        Client saved = clientRepository.save(client);

        auditRecorder.recordProfileUpdate(saved.getId(), actorId, actorName, List.of(
                new ClientProfileAuditRecorder.FieldChange(
                        "photoStorageKey",
                        previousStorageKey,
                        storageKey
                )
        ));

        return ClientDetailDto.from(
                saved,
                getSortedTags(saved.getId()),
                buildPhotoUrl(saved),
                true,
                true,
                true
        );
    }

    /**
     * Reads profile photo bytes for one client with assignment-aware authorization.
     */
    @Transactional(readOnly = true)
    public ClientPhotoData getClientPhoto(final UUID id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + id));

        boolean canManageClients = hasAuthority("MANAGE_CLIENTS");
        boolean canReadAll = hasAuthority("READ_CLIENTS_ALL");
        boolean canReadAssigned = hasAuthority("READ_ASSIGNED_CLIENTS");

        if (!canManageClients && !canReadAll) {
            enforceAssignedTherapistRead(client, canReadAssigned);
        }

        if (client.getPhotoStorageKey() == null || client.getPhotoMimeType() == null) {
            throw new EntityNotFoundException("Client photo not found");
        }

        byte[] content = photoStorage.loadPhoto(client.getPhotoStorageKey());
        return new ClientPhotoData(content, client.getPhotoMimeType());
    }

    private <T> void applyIfChanged(final List<ClientProfileAuditRecorder.FieldChange> changes,
                                    final String fieldName,
                                    final T current,
                                    final T next,
                                    final Consumer<T> setter) {
        if (!Objects.equals(current, next)) {
            setter.accept(next);
            changes.add(new ClientProfileAuditRecorder.FieldChange(fieldName, current, next));
        }
    }

    private void enforceAssignedTherapistRead(final Client client, final boolean canReadAssigned) {
        if (!canReadAssigned) {
            throw new AccessDeniedException("Access denied");
        }

        UUID actorId = currentPrincipalId();
        if (actorId == null || !Objects.equals(actorId, client.getAssignedTherapistId())) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private boolean hasAuthority(final String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    private UUID currentPrincipalId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private List<String> getSortedTags(final UUID clientId) {
        return clientTagRepository.findAllByClientId(clientId).stream()
                .map(ClientTag::getTag)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private String buildPhotoUrl(final Client client) {
        if (client.getPhotoStorageKey() == null) {
            return null;
        }
        return "/api/v1/clients/" + client.getId() + "/photo";
    }

    private List<String> normalizeTags(final List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> unique = tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return unique.stream().limit(20).toList();
    }
}
