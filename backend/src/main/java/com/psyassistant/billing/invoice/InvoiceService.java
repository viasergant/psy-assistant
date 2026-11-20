package com.psyassistant.billing.invoice;

import com.psyassistant.billing.catalog.ServiceCatalogPriceHistoryRepository;
import com.psyassistant.billing.catalog.ServiceCatalogRepository;
import com.psyassistant.billing.catalog.ServiceCatalogTherapistOverrideRepository;
import com.psyassistant.billing.catalog.ServiceStatus;
import com.psyassistant.billing.invoice.config.InvoiceProperties;
import com.psyassistant.billing.invoice.dto.AddLineItemRequest;
import com.psyassistant.billing.invoice.dto.CancelInvoiceRequest;
import com.psyassistant.billing.invoice.dto.CreateInvoiceFromSessionRequest;
import com.psyassistant.billing.invoice.dto.CreateManualInvoiceRequest;
import com.psyassistant.billing.invoice.dto.CreatePackageInvoiceRequest;
import com.psyassistant.billing.invoice.dto.InvoiceListItemResponse;
import com.psyassistant.billing.invoice.dto.InvoiceResponse;
import com.psyassistant.billing.invoice.pdf.InvoicePdfService;
import com.psyassistant.billing.payment.Payment;
import com.psyassistant.billing.payment.PaymentMethod;
import com.psyassistant.billing.payment.PaymentRepository;
import com.psyassistant.billing.pkg.PrepaidPackageInstance;
import com.psyassistant.billing.pkg.PrepaidPackageInstanceRepository;
import com.psyassistant.common.tenant.TenantContext;
import com.psyassistant.sessions.domain.SessionRecord;
import com.psyassistant.sessions.domain.SessionStatus;
import com.psyassistant.sessions.event.SessionCompletedEvent;
import com.psyassistant.sessions.repository.SessionRecordRepository;
import com.psyassistant.therapists.domain.TherapistPricingRule;
import com.psyassistant.therapists.repository.TherapistPricingRuleRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core service for invoice lifecycle management.
 *
 * <p>RBAC rules:
 * <ul>
 *   <li>CREATE/ISSUE/CANCEL operations: Finance Staff and System Administrator</li>
 *   <li>READ operations: Finance Staff, System Administrator, Supervisor</li>
 * </ul>
 *
 * <p>All monetary arithmetic uses {@link BigDecimal}; no floating-point operations.
 */
@Service
public class InvoiceService {

    private static final Logger LOG = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoiceRepository;
    private final InvoiceNumberSeqRepository seqRepository;
    private final SessionRecordRepository sessionRecordRepository;
    private final InvoicePdfService pdfService;
    private final InvoiceProperties properties;
    private final TherapistPricingRuleRepository pricingRuleRepository;
    private final ServiceCatalogRepository catalogRepository;
    private final ServiceCatalogPriceHistoryRepository priceHistoryRepository;
    private final ServiceCatalogTherapistOverrideRepository overrideRepository;
    private final PrepaidPackageInstanceRepository packageInstanceRepository;
    private final PaymentRepository paymentRepository;

    public InvoiceService(
            final InvoiceRepository invoiceRepository,
            final InvoiceNumberSeqRepository seqRepository,
            final SessionRecordRepository sessionRecordRepository,
            final InvoicePdfService pdfService,
            final InvoiceProperties properties,
            final TherapistPricingRuleRepository pricingRuleRepository,
            final ServiceCatalogRepository catalogRepository,
            final ServiceCatalogPriceHistoryRepository priceHistoryRepository,
            final ServiceCatalogTherapistOverrideRepository overrideRepository,
            final PrepaidPackageInstanceRepository packageInstanceRepository,
            final PaymentRepository paymentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.seqRepository = seqRepository;
        this.sessionRecordRepository = sessionRecordRepository;
        this.pdfService = pdfService;
        this.properties = properties;
        this.pricingRuleRepository = pricingRuleRepository;
        this.catalogRepository = catalogRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.overrideRepository = overrideRepository;
        this.packageInstanceRepository = packageInstanceRepository;
        this.paymentRepository = paymentRepository;
    }

    // =========================================================================
    // Automatic invoice generation
    // =========================================================================

    /**
     * Automatically creates a DRAFT invoice when a session is completed.
     *
     * <p>Looks up the therapist's most recent pricing rule whose service type name
     * matches the session type name. Falls back to {@link BigDecimal#ZERO} when no
     * matching rule exists so that a DRAFT is still created for manual review.
     *
     * <p>Idempotent: skips creation when an invoice for the session already exists.
     *
     * @param event the session-completed event
     */
    @EventListener
    @Transactional
    public void handleSessionCompleted(final SessionCompletedEvent event) {
        if (invoiceRepository.existsBySessionId(event.sessionId())) {
            LOG.debug("Invoice already exists for session {}, skipping auto-generation", event.sessionId());
            return;
        }

        UUID sessionTypeId = event.sessionType() != null ? event.sessionType().getId() : null;
        String sessionTypeName = event.sessionType() != null ? event.sessionType().getName() : null;

        // Resolve line-item description: prefer the catalog item name when available
        String lineItemDesc;
        BigDecimal unitPrice;
        if (event.serviceCatalogItemId() != null) {
            var catalogItem = catalogRepository.findById(event.serviceCatalogItemId());
            if (catalogItem.isPresent()) {
                lineItemDesc = catalogItem.get().getName() + " — " + event.sessionDate();
                unitPrice = resolveUnitPriceByCatalogItem(
                        event.therapistId(), event.serviceCatalogItemId(), event.sessionId());
            } else {
                LOG.warn("Service catalog item {} not found for session {} — falling back to session-type resolution",
                        event.serviceCatalogItemId(), event.sessionId());
                lineItemDesc = sessionTypeName != null
                        ? sessionTypeName + " — " + event.sessionDate()
                        : "Session — " + event.sessionDate();
                unitPrice = resolveUnitPrice(event.therapistId(), sessionTypeId, event.sessionId());
            }
        } else {
            lineItemDesc = sessionTypeName != null
                    ? sessionTypeName + " — " + event.sessionDate()
                    : "Session — " + event.sessionDate();
            unitPrice = resolveUnitPrice(event.therapistId(), sessionTypeId, event.sessionId());
        }

        Invoice invoice = new Invoice(event.clientId(), event.therapistId(), InvoiceSource.SESSION);
        invoice.setOrgId(TenantContext.get());
        invoice.setSessionId(event.sessionId());
        invoice.addLineItem(new InvoiceLineItem(lineItemDesc, BigDecimal.ONE, unitPrice, 0));

        assignInvoiceNumber(invoice);
        Invoice saved = invoiceRepository.save(invoice);

        if (event.depositAmount() != null && event.depositAmount().compareTo(BigDecimal.ZERO) > 0) {
            saved.applyDeposit(event.depositAmount());
            paymentRepository.save(new Payment(
                    saved.getId(),
                    event.depositAmount(),
                    PaymentMethod.CASH,
                    event.sessionDate(),
                    null,
                    "Advance payment received at booking",
                    "system"
            ));
            invoiceRepository.save(saved);
            LOG.debug("Registered deposit payment {} against invoice {} for session {}",
                    event.depositAmount(), saved.getInvoiceNumber(), event.sessionId());
        }

        LOG.info("Auto-generated invoice {} for session {}", saved.getInvoiceNumber(), event.sessionId());
    }

    /**
     * Resolves the unit price using a specific service catalog item.
     *
     * <p>Price resolution priority:
     * <ol>
     *   <li>Therapist-specific override for this catalog item</li>
     *   <li>Catalog item base price (open price history record)</li>
     *   <li>Zero (WARN logged)</li>
     * </ol>
     */
    private BigDecimal resolveUnitPriceByCatalogItem(final UUID therapistId,
                                                      final UUID catalogItemId,
                                                      final UUID sessionId) {
        var override = overrideRepository.findByServiceCatalogIdAndTherapistId(catalogItemId, therapistId);
        if (override.isPresent()) {
            return override.get().getPrice();
        }
        var openPrice = priceHistoryRepository.findOpenRecordReadOnly(catalogItemId);
        if (openPrice.isPresent()) {
            return openPrice.get().getPrice();
        }
        LOG.warn("No price found for catalogItemId={} therapistId={} sessionId={} — using zero price",
                catalogItemId, therapistId, sessionId);
        return BigDecimal.ZERO;
    }

    /**
     * Resolves the unit price for auto-invoice generation.
     *
     * <p>Price resolution priority:
     * <ol>
     *   <li>Therapist-specific override in the service catalog for this session type</li>
     *   <li>Service catalog base price for this session type</li>
     *   <li>TherapistPricingRule rate for this session type (legacy)</li>
     *   <li>Zero (WARN logged)</li>
     * </ol>
     */
    private BigDecimal resolveUnitPrice(final UUID therapistId, final UUID sessionTypeId,
                                        final UUID sessionId) {
        if (therapistId == null) {
            return BigDecimal.ZERO;
        }
        if (sessionTypeId == null) {
            LOG.warn("No session type for session {} therapistId={} — using zero price",
                    sessionId, therapistId);
            return BigDecimal.ZERO;
        }

        // 1. Service catalog: therapist override → catalog base price
        var catalogEntry = catalogRepository.findFirstBySessionTypeIdAndStatus(
                sessionTypeId, ServiceStatus.ACTIVE);
        if (catalogEntry.isPresent()) {
            var override = overrideRepository.findByServiceCatalogIdAndTherapistId(
                    catalogEntry.get().getId(), therapistId);
            if (override.isPresent()) {
                return override.get().getPrice();
            }
            var openPrice = priceHistoryRepository.findOpenRecordReadOnly(catalogEntry.get().getId());
            if (openPrice.isPresent()) {
                return openPrice.get().getPrice();
            }
        }

        // 2. TherapistPricingRule (legacy fallback)
        List<TherapistPricingRule> rules =
                pricingRuleRepository.findByTherapistProfileIdAndSessionTypeIdOrderByEffectiveFromDesc(
                        therapistId, sessionTypeId);
        if (!rules.isEmpty()) {
            return rules.get(0).getRate();
        }

        LOG.warn("No price found for sessionTypeId={} therapistId={} sessionId={} — using zero price",
                sessionTypeId, therapistId, sessionId);
        return BigDecimal.ZERO;
    }

    // =========================================================================
    // Create
    // =========================================================================

    /**
     * Creates a draft invoice from a completed session.
     *
     * <p>Requires the session to be in COMPLETED status and a unit price
     * to be provided (either from the service catalog override or the request).
     *
     * @param request   the creation request
     * @param actorName the name of the creating user (for audit)
     * @return the created invoice
     */
    @Transactional
    @PreAuthorize("hasAuthority('CREATE_INVOICES')")
    public InvoiceResponse createFromSession(
            final CreateInvoiceFromSessionRequest request,
            final String actorName) {

        SessionRecord session = sessionRecordRepository.findById(request.sessionId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Session not found: " + request.sessionId()));

        if (session.getStatus() != SessionStatus.COMPLETED) {
            throw new InvoiceStateException(
                    "Invoice can only be generated from a COMPLETED session; "
                    + "current status: " + session.getStatus());
        }

        if (request.unitPriceOverride() == null || request.unitPriceOverride().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvoiceStateException("unitPriceOverride is required and must be >= 0 for session invoices");
        }

        Invoice invoice = new Invoice(session.getClientId(), session.getTherapistId(), InvoiceSource.SESSION);
        invoice.setOrgId(TenantContext.get());
        invoice.setSessionId(session.getId());
        invoice.setNotes(request.notes());

        String sessionTypeName = session.getSessionType() != null
                ? session.getSessionType().getName()
                : "Session";

        InvoiceLineItem lineItem = new InvoiceLineItem(
                sessionTypeName + " — " + session.getSessionDate(),
                BigDecimal.ONE,
                request.unitPriceOverride(),
                0
        );
        invoice.addLineItem(lineItem);

        assignInvoiceNumber(invoice);

        Invoice saved = invoiceRepository.save(invoice);
        LOG.info("Invoice {} created from session {} by {}", saved.getInvoiceNumber(), request.sessionId(), actorName);
        return InvoiceResponse.from(saved);
    }

    /**
     * Creates a manual invoice with the provided line items.
     *
     * @param request   the creation request
     * @param actorName the name of the creating user
     * @return the created invoice
     */
    @Transactional
    @PreAuthorize("hasAuthority('CREATE_INVOICES')")
    public InvoiceResponse createManual(
            final CreateManualInvoiceRequest request,
            final String actorName) {

        Invoice invoice = new Invoice(request.clientId(), request.therapistId(), InvoiceSource.MANUAL);
        invoice.setOrgId(TenantContext.get());
        invoice.setNotes(request.notes());
        if (request.discount() != null) {
            invoice.setDiscount(request.discount());
        }

        for (int i = 0; i < request.lineItems().size(); i++) {
            CreateManualInvoiceRequest.LineItemRequest li = request.lineItems().get(i);
            invoice.addLineItem(new InvoiceLineItem(li.description(), li.quantity(), li.unitPrice(), i));
        }

        assignInvoiceNumber(invoice);

        Invoice saved = invoiceRepository.save(invoice);
        LOG.info("Manual invoice {} created for client {} by {}",
                saved.getInvoiceNumber(), request.clientId(), actorName);
        return InvoiceResponse.from(saved);
    }

    /**
     * Creates a DRAFT invoice for a prepaid package purchase.
     *
     * <p>Links the invoice back to the package instance by setting
     * {@code PrepaidPackageInstance.invoiceId} after the invoice is saved.
     *
     * @param request   the request containing the package instance UUID
     * @param actorName the authenticated actor performing the action
     * @return the created invoice
     */
    @Transactional
    @PreAuthorize("hasAuthority('CREATE_INVOICES')")
    public InvoiceResponse createFromPackage(
            final CreatePackageInvoiceRequest request,
            final String actorName) {

        PrepaidPackageInstance instance = packageInstanceRepository.findById(request.prepaidPackageId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Package instance not found: " + request.prepaidPackageId()));

        Invoice invoice = new Invoice(instance.getClientId(), null, InvoiceSource.PACKAGE);
        invoice.setOrgId(TenantContext.get());
        invoice.setPrepaidPackageId(instance.getId());

        String lineItemDesc = instance.getDefinition().getName()
                + " — " + instance.getDefinition().getSessionQty() + " sessions";
        invoice.addLineItem(new InvoiceLineItem(
                lineItemDesc,
                BigDecimal.ONE,
                instance.getDefinition().getPrice(),
                0));

        assignInvoiceNumber(invoice);
        Invoice saved = invoiceRepository.save(invoice);

        instance.setInvoiceId(saved.getId());
        packageInstanceRepository.save(instance);

        LOG.info("Invoice {} created from package instance {} by {}",
                saved.getInvoiceNumber(), instance.getId(), actorName);
        return InvoiceResponse.from(saved);
    }

    // =========================================================================
    // Line items
    // =========================================================================

    /**
     * Appends a line item to a draft invoice.
     *
     * @param invoiceId the invoice UUID
     * @param request   the new line item
     * @param actorName actor for logging
     * @return updated invoice
     */
    @Transactional
    @PreAuthorize("hasAuthority('CREATE_INVOICES')")
    public InvoiceResponse addLineItem(
            final UUID invoiceId,
            final AddLineItemRequest request,
            final String actorName) {

        Invoice invoice = loadById(invoiceId);
        int nextOrder = invoice.getLineItems().size();
        invoice.addLineItem(
                new InvoiceLineItem(request.description(), request.quantity(), request.unitPrice(), nextOrder));
        LOG.info("Line item added to invoice {} by {}", invoice.getInvoiceNumber(), actorName);
        return InvoiceResponse.from(invoice);
    }

    /**
     * Removes a line item from a draft invoice.
     *
     * @param invoiceId the invoice UUID
     * @param itemId    the line item UUID to remove
     * @param actorName actor for logging
     */
    @Transactional
    @PreAuthorize("hasAuthority('CREATE_INVOICES')")
    public void removeLineItem(
            final UUID invoiceId,
            final UUID itemId,
            final String actorName) {

        Invoice invoice = loadById(invoiceId);
        invoice.removeLineItem(itemId);
        LOG.info("Line item {} removed from invoice {} by {}", itemId, invoice.getInvoiceNumber(), actorName);
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Issues a draft invoice: locks it for editing, sets issued and due dates, generates PDF.
     *
     * @param invoiceId the invoice UUID
     * @param actorName actor for logging
     * @return updated invoice
     */
    @Transactional
    @PreAuthorize("hasAuthority('ISSUE_INVOICES')")
    public InvoiceResponse issue(final UUID invoiceId, final String actorName) {
        Invoice invoice = loadById(invoiceId);

        LocalDate issuedDate = LocalDate.now();
        LocalDate dueDate = issuedDate.plusDays(properties.paymentTermsDays());
        invoice.issue(issuedDate, dueDate);

        // Generate and store PDF
        String pdfPath = pdfService.generateAndStore(invoice);
        invoice.setPdfPath(pdfPath);

        LOG.info("Invoice {} issued by {}, due {}", invoice.getInvoiceNumber(), actorName, dueDate);
        return InvoiceResponse.from(invoice);
    }

    /**
     * Cancels a Draft or Issued invoice with a mandatory reason.
     *
     * @param invoiceId the invoice UUID
     * @param request   cancellation reason
     * @param actorName actor for logging
     * @return updated invoice
     */
    @Transactional
    @PreAuthorize("hasAuthority('CANCEL_INVOICES')")
    public InvoiceResponse cancel(
            final UUID invoiceId,
            final CancelInvoiceRequest request,
            final String actorName) {

        Invoice invoice = loadById(invoiceId);
        invoice.cancel(request.reason());
        LOG.info("Invoice {} cancelled by {} — {}", invoice.getInvoiceNumber(), actorName, request.reason());
        return InvoiceResponse.from(invoice);
    }

    // =========================================================================
    // Queries
    // =========================================================================

    /**
     * Returns a single invoice with full line item details.
     *
     * @param invoiceId the invoice UUID
     * @return invoice detail response
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('READ_INVOICES')")
    public InvoiceResponse getById(final UUID invoiceId) {
        return InvoiceResponse.from(loadById(invoiceId));
    }

    /**
     * Returns a paginated invoice list with optional filters.
     *
     * @param clientId    optional client filter
     * @param therapistId optional therapist filter
     * @param status      optional status filter
     * @param pageable    pagination
     * @return page of invoice summaries
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('READ_INVOICES')")
    public Page<InvoiceListItemResponse> list(
            final UUID clientId,
            final UUID therapistId,
            final InvoiceStatus status,
            final Pageable pageable) {

        return invoiceRepository.findFiltered(clientId, therapistId, status, pageable)
                .map(InvoiceListItemResponse::from);
    }

    /**
     * Returns the raw PDF bytes for a stored invoice PDF.
     *
     * @param invoiceId the invoice UUID
     * @return PDF bytes
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('READ_INVOICES')")
    public byte[] getPdfBytes(final UUID invoiceId) {
        Invoice invoice = loadById(invoiceId);
        if (invoice.getPdfPath() == null) {
            throw new EntityNotFoundException("PDF not yet generated for invoice: " + invoiceId);
        }
        return pdfService.readStored(invoice.getPdfPath());
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    private Invoice loadById(final UUID invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + invoiceId));
    }

    /**
     * Assigns a race-safe sequential invoice number using SELECT ... FOR UPDATE on
     * the {@code invoice_number_seq} row for the current year.
     *
     * <p>Format: {@code YYYY-NNNN} (e.g., {@code 2026-0001}).
     * Gaps are permitted after cancellation (per business decision).
     */
    private void assignInvoiceNumber(final Invoice invoice) {
        short year = (short) LocalDate.now().getYear();
        InvoiceNumberSeq seq = seqRepository.findForUpdateByYear(year)
                .orElseGet(() -> {
                    InvoiceNumberSeq newSeq = new InvoiceNumberSeq(year);
                    return seqRepository.save(newSeq);
                });

        int nextSeq = seq.getLastSeq() + 1;
        seq.setLastSeq(nextSeq);
        seqRepository.save(seq);

        String invoiceNumber = String.format("%d-%04d", year, nextSeq);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setInvoiceYear(year);
        invoice.setInvoiceSeq(nextSeq);
    }
}
