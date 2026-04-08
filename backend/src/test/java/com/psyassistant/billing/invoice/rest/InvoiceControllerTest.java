package com.psyassistant.billing.invoice.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psyassistant.billing.invoice.InvoiceService;
import com.psyassistant.billing.invoice.InvoiceSource;
import com.psyassistant.billing.invoice.InvoiceStateException;
import com.psyassistant.billing.invoice.InvoiceStatus;
import com.psyassistant.billing.invoice.dto.CancelInvoiceRequest;
import com.psyassistant.billing.invoice.dto.CreateManualInvoiceRequest;
import com.psyassistant.billing.invoice.dto.InvoiceListItemResponse;
import com.psyassistant.billing.invoice.dto.InvoiceResponse;
import com.psyassistant.common.config.SecurityConfig;
import com.psyassistant.common.exception.GlobalExceptionHandler;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link InvoiceController}.
 *
 * <p>Verifies HTTP status codes, JSON shape, security (RBAC), and error mappings.
 */
@WebMvcTest(controllers = InvoiceController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InvoiceService invoiceService;

    @MockitoBean
    private com.psyassistant.common.audit.AuditLogService auditLogService;

    private static final String BASE = "/api/v1/invoices";
    private static final UUID INVOICE_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();

    // ---- GET list -----------------------------------------------------------

    @Test
    void listReturns200ForReadInvoicesAuthority() throws Exception {
        when(invoiceService.list(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleListItem())));

        mockMvc.perform(get(BASE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("READ_INVOICES"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].invoiceNumber").value("2026-0001"));
    }

    @Test
    void listReturns403WithoutRequiredAuthority() throws Exception {
        mockMvc.perform(get(BASE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("UNRELATED"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listReturns401WithoutJwt() throws Exception {
        mockMvc.perform(get(BASE))
                .andExpect(status().isUnauthorized());
    }

    // ---- GET by id ----------------------------------------------------------

    @Test
    void getByIdReturns200ForReadInvoicesAuthority() throws Exception {
        when(invoiceService.getById(INVOICE_ID)).thenReturn(sampleResponse());

        mockMvc.perform(get(BASE + "/" + INVOICE_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("READ_INVOICES"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(INVOICE_ID.toString()));
    }

    @Test
    void getByIdReturns404WhenNotFound() throws Exception {
        when(invoiceService.getById(INVOICE_ID))
                .thenThrow(new EntityNotFoundException("Invoice not found: " + INVOICE_ID));

        mockMvc.perform(get(BASE + "/" + INVOICE_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("READ_INVOICES"))))
                .andExpect(status().isNotFound());
    }

    // ---- POST /manual -------------------------------------------------------

    @Test
    void createManualReturns201ForCreateInvoicesAuthority() throws Exception {
        when(invoiceService.createManual(any(), any())).thenReturn(sampleResponse());

        CreateManualInvoiceRequest req = new CreateManualInvoiceRequest(
                CLIENT_ID,
                null,
                List.of(new CreateManualInvoiceRequest.LineItemRequest(
                        "Session", BigDecimal.ONE, new BigDecimal("100.00"))),
                null,
                null
        );

        mockMvc.perform(post(BASE + "/manual")
                        .with(jwt().authorities(new SimpleGrantedAuthority("CREATE_INVOICES")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void createManualReturns403WithoutCreateInvoicesAuthority() throws Exception {
        CreateManualInvoiceRequest req = new CreateManualInvoiceRequest(
                CLIENT_ID,
                null,
                List.of(new CreateManualInvoiceRequest.LineItemRequest(
                        "Session", BigDecimal.ONE, new BigDecimal("100.00"))),
                null,
                null
        );

        mockMvc.perform(post(BASE + "/manual")
                        .with(jwt().authorities(new SimpleGrantedAuthority("READ_INVOICES")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createManualReturns400ForMissingLineItems() throws Exception {
        CreateManualInvoiceRequest req = new CreateManualInvoiceRequest(
                CLIENT_ID, null, Collections.emptyList(), null, null);

        mockMvc.perform(post(BASE + "/manual")
                        .with(jwt().authorities(new SimpleGrantedAuthority("CREATE_INVOICES")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ---- POST /{id}/issue ---------------------------------------------------

    @Test
    void issueReturns200ForIssueInvoicesAuthority() throws Exception {
        InvoiceResponse issued = sampleResponseWithStatus(InvoiceStatus.ISSUED);
        when(invoiceService.issue(eq(INVOICE_ID), any())).thenReturn(issued);

        mockMvc.perform(post(BASE + "/" + INVOICE_ID + "/issue")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ISSUE_INVOICES"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"));
    }

    @Test
    void issueReturns422WhenAlreadyIssued() throws Exception {
        when(invoiceService.issue(eq(INVOICE_ID), any()))
                .thenThrow(new InvoiceStateException("Invoice can only be issued from DRAFT state"));

        mockMvc.perform(post(BASE + "/" + INVOICE_ID + "/issue")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ISSUE_INVOICES"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVOICE_STATE_ERROR"));
    }

    // ---- POST /{id}/cancel --------------------------------------------------

    @Test
    void cancelReturns200ForCancelInvoicesAuthority() throws Exception {
        InvoiceResponse cancelled = sampleResponseWithStatus(InvoiceStatus.CANCELLED);
        when(invoiceService.cancel(eq(INVOICE_ID), any(), any())).thenReturn(cancelled);

        mockMvc.perform(post(BASE + "/" + INVOICE_ID + "/cancel")
                        .with(jwt().authorities(new SimpleGrantedAuthority("CANCEL_INVOICES")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CancelInvoiceRequest("Client request"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // ---- DELETE /{id}/line-items/{itemId} -----------------------------------

    @Test
    void removeLineItemReturns204ForCreateInvoicesAuthority() throws Exception {
        UUID itemId = UUID.randomUUID();
        doNothing().when(invoiceService).removeLineItem(eq(INVOICE_ID), eq(itemId), any());

        mockMvc.perform(delete(BASE + "/" + INVOICE_ID + "/line-items/" + itemId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("CREATE_INVOICES"))))
                .andExpect(status().isNoContent());
    }

    // ---- GET /{id}/pdf ------------------------------------------------------

    @Test
    void downloadPdfReturns200WithApplicationPdfContentType() throws Exception {
        byte[] pdfBytes = new byte[]{0x25, 0x50, 0x44, 0x46};
        when(invoiceService.getPdfBytes(INVOICE_ID)).thenReturn(pdfBytes);

        mockMvc.perform(get(BASE + "/" + INVOICE_ID + "/pdf")
                        .with(jwt().authorities(new SimpleGrantedAuthority("READ_INVOICES"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("invoice-" + INVOICE_ID)));
    }

    // ---- helpers ------------------------------------------------------------

    private InvoiceListItemResponse sampleListItem() {
        return new InvoiceListItemResponse(
                INVOICE_ID,
                "2026-0001",
                CLIENT_ID,
                null,
                InvoiceSource.MANUAL,
                InvoiceStatus.DRAFT,
                null,
                null,
                new BigDecimal("100.00"),
                Instant.now()
        );
    }

    private InvoiceResponse sampleResponse() {
        return sampleResponseWithStatus(InvoiceStatus.DRAFT);
    }

    private InvoiceResponse sampleResponseWithStatus(final InvoiceStatus status) {
        return new InvoiceResponse(
                INVOICE_ID,
                "2026-0001",
                CLIENT_ID,
                null,
                InvoiceSource.MANUAL,
                null,
                null,
                status,
                null,
                null,
                null,
                null,
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("100.00"),
                null,
                null,
                Collections.emptyList(),
                Instant.now(),
                Instant.now()
        );
    }
}
