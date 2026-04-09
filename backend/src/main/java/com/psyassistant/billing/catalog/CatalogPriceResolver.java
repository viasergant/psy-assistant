package com.psyassistant.billing.catalog;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the effective price for a given service, therapist, and session date.
 *
 * <p>Used by InvoiceService when creating invoices. Does NOT filter by service status
 * so that historical invoices for inactive services still resolve correctly.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>Therapist-specific override for this service (if present)</li>
 *   <li>Default price history record effective on the session date</li>
 * </ol>
 */
@Component
public class CatalogPriceResolver {

    private final ServiceCatalogTherapistOverrideRepository overrideRepository;
    private final ServiceCatalogPriceHistoryRepository priceHistoryRepository;

    public CatalogPriceResolver(
            final ServiceCatalogTherapistOverrideRepository overrideRepository,
            final ServiceCatalogPriceHistoryRepository priceHistoryRepository) {
        this.overrideRepository = overrideRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    /**
     * Resolves the billable price for the given service, therapist, and session date.
     *
     * @param serviceId   the service catalog entry ID
     * @param therapistId the therapist performing the service
     * @param sessionDate the date of the session
     * @return the applicable price; never null
     * @throws EntityNotFoundException if no price record can be found
     */
    @Transactional(readOnly = true)
    public BigDecimal resolvePrice(final UUID serviceId, final UUID therapistId,
                                   final LocalDate sessionDate) {
        // 1. Check therapist override
        return overrideRepository
                .findByServiceCatalogIdAndTherapistId(serviceId, therapistId)
                .map(ServiceCatalogTherapistOverride::getPrice)
                .orElseGet(() -> resolveDefaultPrice(serviceId, sessionDate));
    }

    private BigDecimal resolveDefaultPrice(final UUID serviceId, final LocalDate date) {
        List<ServiceCatalogPriceHistory> records =
                priceHistoryRepository.findEffectivePrice(serviceId, date);
        if (records.isEmpty()) {
            throw new EntityNotFoundException(
                    "No price record found for service " + serviceId + " on date " + date);
        }
        return records.get(0).getPrice();
    }
}
