package com.psyassistant.billing.invoice.rest;

import com.psyassistant.billing.invoice.InvoiceService;
import com.psyassistant.billing.invoice.InvoiceStatus;
import com.psyassistant.billing.invoice.dto.AddLineItemRequest;
import com.psyassistant.billing.invoice.dto.CancelInvoiceRequest;
import com.psyassistant.billing.invoice.dto.CreateInvoiceFromSessionRequest;
import com.psyassistant.billing.invoice.dto.CreateManualInvoiceRequest;
import com.psyassistant.billing.invoice.dto.CreatePackageInvoiceRequest;
import com.psyassistant.billing.invoice.dto.InvoiceListItemResponse;
import com.psyassistant.billing.invoice.dto.InvoiceResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for invoice management.
 *
 * <p>URL structure:
 * <ul>
 *   <li>{@code GET /api/v1/invoices} — paginated list with optional filters</li>
 *   <li>{@code POST /api/v1/invoices/from-session} — create from completed session</li>
 *   <li>{@code POST /api/v1/invoices/manual} — create with manual line items</li>
 *   <li>{@code POST /api/v1/invoices/from-package} — create from prepaid package (stub)</li>
 *   <li>{@code GET /api/v1/invoices/{id}} — detail with line items</li>
 *   <li>{@code POST /api/v1/invoices/{id}/line-items} — add line item (draft only)</li>
 *   <li>{@code DELETE /api/v1/invoices/{id}/line-items/{itemId}} — remove line item (draft only)</li>
 *   <li>{@code POST /api/v1/invoices/{id}/issue} — issue (lock) invoice</li>
 *   <li>{@code POST /api/v1/invoices/{id}/cancel} — cancel invoice</li>
 *   <li>{@code GET /api/v1/invoices/{id}/pdf} — download PDF</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private static final Logger LOG = LoggerFactory.getLogger(InvoiceController.class);

    private final InvoiceService invoiceService;

    public InvoiceController(final InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    // =========================================================================
    // List / detail
    // =========================================================================

    @GetMapping
    @PreAuthorize("hasAuthority('READ_INVOICES')")
    public ResponseEntity<Page<InvoiceListItemResponse>> list(
            @RequestParam(required = false) final UUID clientId,
            @RequestParam(required = false) final UUID therapistId,
            @RequestParam(required = false) final InvoiceStatus status,
            @PageableDefault(size = 20, sort = "createdAt") final Pageable pageable) {

        return ResponseEntity.ok(invoiceService.list(clientId, therapistId, status, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_INVOICES')")
    public ResponseEntity<InvoiceResponse> getById(@PathVariable final UUID id) {
        return ResponseEntity.ok(invoiceService.getById(id));
    }

    // =========================================================================
    // Create
    // =========================================================================

    @PostMapping("/from-session")
    @PreAuthorize("hasAuthority('CREATE_INVOICES')")
    public ResponseEntity<InvoiceResponse> createFromSession(
            @Valid @RequestBody final CreateInvoiceFromSessionRequest request,
            final Authentication auth) {

        LOG.info("POST /api/v1/invoices/from-session session={} actor={}", request.sessionId(), actorName(auth));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.createFromSession(request, actorName(auth)));
    }

    @PostMapping("/manual")
    @PreAuthorize("hasAuthority('CREATE_INVOICES')")
    public ResponseEntity<InvoiceResponse> createManual(
            @Valid @RequestBody final CreateManualInvoiceRequest request,
            final Authentication auth) {

        LOG.info("POST /api/v1/invoices/manual client={} actor={}", request.clientId(), actorName(auth));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.createManual(request, actorName(auth)));
    }

    @PostMapping("/from-package")
    @PreAuthorize("hasAuthority('CREATE_INVOICES')")
    public ResponseEntity<InvoiceResponse> createFromPackage(
            @Valid @RequestBody final CreatePackageInvoiceRequest request,
            final Authentication auth) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.createFromPackage(request, actorName(auth)));
    }

    // =========================================================================
    // Line items
    // =========================================================================

    @PostMapping("/{id}/line-items")
    @PreAuthorize("hasAuthority('CREATE_INVOICES')")
    public ResponseEntity<InvoiceResponse> addLineItem(
            @PathVariable final UUID id,
            @Valid @RequestBody final AddLineItemRequest request,
            final Authentication auth) {

        return ResponseEntity.ok(invoiceService.addLineItem(id, request, actorName(auth)));
    }

    @DeleteMapping("/{id}/line-items/{itemId}")
    @PreAuthorize("hasAuthority('CREATE_INVOICES')")
    public ResponseEntity<Void> removeLineItem(
            @PathVariable final UUID id,
            @PathVariable final UUID itemId,
            final Authentication auth) {

        invoiceService.removeLineItem(id, itemId, actorName(auth));
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasAuthority('ISSUE_INVOICES')")
    public ResponseEntity<InvoiceResponse> issue(
            @PathVariable final UUID id,
            final Authentication auth) {

        LOG.info("POST /api/v1/invoices/{}/issue actor={}", id, actorName(auth));
        return ResponseEntity.ok(invoiceService.issue(id, actorName(auth)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('CANCEL_INVOICES')")
    public ResponseEntity<InvoiceResponse> cancel(
            @PathVariable final UUID id,
            @Valid @RequestBody final CancelInvoiceRequest request,
            final Authentication auth) {

        LOG.info("POST /api/v1/invoices/{}/cancel reason={} actor={}", id, request.reason(), actorName(auth));
        return ResponseEntity.ok(invoiceService.cancel(id, request, actorName(auth)));
    }

    // =========================================================================
    // PDF download
    // =========================================================================

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('READ_INVOICES')")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable final UUID id) {
        byte[] pdfBytes = invoiceService.getPdfBytes(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("invoice-" + id + ".pdf").build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String actorName(final Authentication auth) {
        return auth != null ? auth.getName() : "system";
    }
}
