package com.psyassistant.billing.invoice;

import com.psyassistant.billing.invoice.config.InvoiceProperties;
import com.psyassistant.billing.invoice.dto.AddLineItemRequest;
import com.psyassistant.billing.invoice.dto.CancelInvoiceRequest;
import com.psyassistant.billing.invoice.dto.CreateInvoiceFromSessionRequest;
import com.psyassistant.billing.invoice.dto.CreateManualInvoiceRequest;
import com.psyassistant.billing.invoice.dto.InvoiceListItemResponse;
import com.psyassistant.billing.invoice.dto.InvoiceResponse;
import com.psyassistant.billing.invoice.pdf.InvoicePdfService;
import com.psyassistant.sessions.domain.SessionRecord;
import com.psyassistant.sessions.domain.SessionStatus;
import com.psyassistant.sessions.repository.SessionRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public InvoiceService(
            final InvoiceRepository invoiceRepository,
            final InvoiceNumberSeqRepository seqRepository,
            final SessionRecordRepository sessionRecordRepository,
            final InvoicePdfService pdfService,
            final InvoiceProperties properties) {
        this.invoiceRepository = invoiceRepository;
        this.seqRepository = seqRepository;
        this.sessionRecordRepository = sessionRecordRepository;
        this.pdfService = pdfService;
        this.properties = properties;
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
     * Stub: package invoice creation is deferred until PA-49 is implemented.
     *
     * @throws UnsupportedOperationException always
     */
    @Transactional
    @PreAuthorize("hasAuthority('CREATE_INVOICES')")
    public InvoiceResponse createFromPackage(
            final com.psyassistant.billing.invoice.dto.CreatePackageInvoiceRequest request,
            final String actorName) {
        throw new UnsupportedOperationException(
                "Package invoice generation is pending PA-49 (Prepaid Packages) integration.");
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
