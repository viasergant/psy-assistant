package com.psyassistant.billing.catalog;

import com.psyassistant.billing.catalog.dto.CreateServiceRequest;
import com.psyassistant.billing.catalog.dto.PriceHistoryResponse;
import com.psyassistant.billing.catalog.dto.ServiceCatalogListItem;
import com.psyassistant.billing.catalog.dto.ServiceCatalogResponse;
import com.psyassistant.billing.catalog.dto.TherapistOverrideResponse;
import com.psyassistant.billing.catalog.dto.UpdateDefaultPriceRequest;
import com.psyassistant.billing.catalog.dto.UpdateServiceRequest;
import com.psyassistant.billing.catalog.dto.UpdateServiceStatusRequest;
import com.psyassistant.billing.catalog.dto.UpsertTherapistOverrideRequest;
import com.psyassistant.scheduling.domain.SessionType;
import com.psyassistant.scheduling.repository.SessionTypeRepository;
import com.psyassistant.users.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core service for the service catalog feature.
 *
 * <p>RBAC rules:
 * <ul>
 *   <li>Write operations (create/update/deactivate/price/overrides): Finance, SysAdmin</li>
 *   <li>Read operations: Finance, SysAdmin, Supervisor</li>
 * </ul>
 */
@Service
public class ServiceCatalogService {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceCatalogService.class);

    private final ServiceCatalogRepository catalogRepository;
    private final ServiceCatalogPriceHistoryRepository priceHistoryRepository;
    private final ServiceCatalogTherapistOverrideRepository overrideRepository;
    private final UserRepository userRepository;
    private final SessionTypeRepository sessionTypeRepository;

    public ServiceCatalogService(
            final ServiceCatalogRepository catalogRepository,
            final ServiceCatalogPriceHistoryRepository priceHistoryRepository,
            final ServiceCatalogTherapistOverrideRepository overrideRepository,
            final UserRepository userRepository,
            final SessionTypeRepository sessionTypeRepository) {
        this.catalogRepository = catalogRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.overrideRepository = overrideRepository;
        this.userRepository = userRepository;
        this.sessionTypeRepository = sessionTypeRepository;
    }

    // =========================================================================
    // Read operations
    // =========================================================================

    @PreAuthorize("hasAuthority('READ_SERVICE_CATALOG')")
    @Transactional(readOnly = true)
    public List<ServiceCatalogListItem> listServices(final ServiceStatus statusFilter) {
        List<ServiceCatalog> services = statusFilter != null
                ? catalogRepository.findByStatus(statusFilter)
                : catalogRepository.findAll();
        return services.stream()
                .map(s -> ServiceCatalogListItem.from(s, resolveCurrentPrice(s.getId())))
                .toList();
    }

    @PreAuthorize("hasAuthority('READ_SERVICE_CATALOG')")
    @Transactional(readOnly = true)
    public ServiceCatalogResponse getService(final UUID id) {
        ServiceCatalog service = loadService(id);
        return ServiceCatalogResponse.from(service, resolveCurrentPrice(id));
    }

    @PreAuthorize("hasAuthority('READ_SERVICE_CATALOG')")
    @Transactional(readOnly = true)
    public List<PriceHistoryResponse> getPriceHistory(final UUID serviceId) {
        loadService(serviceId); // validate existence
        return priceHistoryRepository
                .findByServiceCatalogIdOrderByEffectiveFromDesc(serviceId)
                .stream()
                .map(PriceHistoryResponse::from)
                .toList();
    }

    @PreAuthorize("hasAuthority('READ_SERVICE_CATALOG')")
    @Transactional(readOnly = true)
    public List<TherapistOverrideResponse> getTherapistOverrides(final UUID serviceId) {
        loadService(serviceId); // validate existence
        return overrideRepository.findByServiceCatalogId(serviceId)
                .stream()
                .map(o -> TherapistOverrideResponse.from(o, resolveTherapistName(o.getTherapistId())))
                .toList();
    }

    // =========================================================================
    // Write operations
    // =========================================================================

    @PreAuthorize("hasAuthority('MANAGE_SERVICE_CATALOG')")
    @Transactional
    public ServiceCatalogResponse createService(final CreateServiceRequest request) {
        if (catalogRepository.existsByNameAndCategory(request.name(), request.category())) {
            throw new DuplicateServiceException(request.name(), request.category());
        }

        ServiceCatalog service = new ServiceCatalog(
                request.name(),
                request.category(),
                resolveSessionType(request.sessionTypeId()),
                request.durationMin()
        );
        catalogRepository.save(service);

        String changedBy = currentPrincipalName();
        ServiceCatalogPriceHistory priceRecord = new ServiceCatalogPriceHistory(
                service,
                request.defaultPrice(),
                request.effectiveFrom(),
                changedBy
        );
        priceHistoryRepository.save(priceRecord);

        LOG.info("Service catalog entry created: id={}, name={}, by={}", service.getId(), service.getName(), changedBy);
        return ServiceCatalogResponse.from(service, request.defaultPrice());
    }

    @PreAuthorize("hasAuthority('MANAGE_SERVICE_CATALOG')")
    @Transactional
    public ServiceCatalogResponse updateService(final UUID id, final UpdateServiceRequest request) {
        ServiceCatalog service = loadService(id);

        if (catalogRepository.existsByNameAndCategoryAndIdNot(request.name(), request.category(), id)) {
            throw new DuplicateServiceException(request.name(), request.category());
        }

        service.setName(request.name());
        service.setCategory(request.category());
        service.setSessionType(resolveSessionType(request.sessionTypeId()));
        service.setDurationMin(request.durationMin());

        LOG.info("Service catalog entry updated: id={}", id);
        return ServiceCatalogResponse.from(service, resolveCurrentPrice(id));
    }

    @PreAuthorize("hasAuthority('MANAGE_SERVICE_CATALOG')")
    @Transactional
    public ServiceCatalogResponse updateStatus(final UUID id, final UpdateServiceStatusRequest request) {
        ServiceCatalog service = loadService(id);
        service.setStatus(request.status());
        LOG.info("Service catalog status changed: id={}, status={}", id, request.status());
        return ServiceCatalogResponse.from(service, resolveCurrentPrice(id));
    }

    /**
     * Updates the default price for a service.
     *
     * <p>Acquires a pessimistic write lock on the open price record to prevent
     * concurrent Finance users from creating duplicate open records.
     * In a single transaction: closes the old open record and creates a new one.
     */
    @PreAuthorize("hasAuthority('MANAGE_SERVICE_CATALOG')")
    @Transactional
    public ServiceCatalogResponse updateDefaultPrice(final UUID id,
                                                      final UpdateDefaultPriceRequest request) {
        ServiceCatalog service = loadService(id);
        String changedBy = currentPrincipalName();

        // Close the existing open record (if any) with a pessimistic write lock
        priceHistoryRepository.findOpenRecordByServiceId(id).ifPresent(open -> {
            open.setEffectiveTo(request.effectiveFrom().minusDays(1));
            priceHistoryRepository.save(open);
        });

        // Create new open record
        ServiceCatalogPriceHistory newRecord = new ServiceCatalogPriceHistory(
                service,
                request.price(),
                request.effectiveFrom(),
                changedBy
        );
        priceHistoryRepository.save(newRecord);

        LOG.info("Default price updated for service id={}, effective={}, by={}",
                id, request.effectiveFrom(), changedBy);
        return ServiceCatalogResponse.from(service, request.price());
    }

    @PreAuthorize("hasAuthority('MANAGE_SERVICE_CATALOG')")
    @Transactional
    public TherapistOverrideResponse upsertTherapistOverride(final UUID serviceId,
                                                              final UUID therapistId,
                                                              final UpsertTherapistOverrideRequest request) {
        loadService(serviceId); // validate service exists
        validateUserExists(therapistId);

        ServiceCatalogTherapistOverride override =
                overrideRepository.findByServiceCatalogIdAndTherapistId(serviceId, therapistId)
                        .orElseGet(() -> {
                            ServiceCatalog svc = loadService(serviceId);
                            return new ServiceCatalogTherapistOverride(svc, therapistId, request.price());
                        });
        override.setPrice(request.price());
        overrideRepository.save(override);

        LOG.info("Therapist override upserted: serviceId={}, therapistId={}", serviceId, therapistId);
        return TherapistOverrideResponse.from(override, resolveTherapistName(therapistId));
    }

    @PreAuthorize("hasAuthority('MANAGE_SERVICE_CATALOG')")
    @Transactional
    public void deleteTherapistOverride(final UUID serviceId, final UUID therapistId) {
        ServiceCatalogTherapistOverride override =
                overrideRepository.findByServiceCatalogIdAndTherapistId(serviceId, therapistId)
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Override not found for service " + serviceId + " and therapist " + therapistId));
        overrideRepository.delete(override);
        LOG.info("Therapist override removed: serviceId={}, therapistId={}", serviceId, therapistId);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private ServiceCatalog loadService(final UUID id) {
        return catalogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service not found: " + id));
    }

    private BigDecimal resolveCurrentPrice(final UUID serviceId) {
        return priceHistoryRepository.findOpenRecordReadOnly(serviceId)
                .map(ServiceCatalogPriceHistory::getPrice)
                .orElse(BigDecimal.ZERO);
    }

    private String resolveTherapistName(final UUID therapistId) {
        return userRepository.findById(therapistId)
                .map(u -> u.getFullName() != null ? u.getFullName() : u.getEmail())
                .orElse(therapistId.toString());
    }

    private void validateUserExists(final UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found: " + userId);
        }
    }

    private String currentPrincipalName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    private SessionType resolveSessionType(final UUID sessionTypeId) {
        SessionType st = sessionTypeRepository.findById(sessionTypeId)
                .orElseThrow(() -> new EntityNotFoundException("Session type not found: " + sessionTypeId));
        if (!Boolean.TRUE.equals(st.getIsActive())) {
            throw new EntityNotFoundException("Session type is inactive: " + sessionTypeId);
        }
        return st;
    }
}
