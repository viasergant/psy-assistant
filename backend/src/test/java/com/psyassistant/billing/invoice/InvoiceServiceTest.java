package com.psyassistant.billing.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.billing.invoice.config.InvoiceProperties;
import com.psyassistant.billing.invoice.dto.AddLineItemRequest;
import com.psyassistant.billing.invoice.dto.CancelInvoiceRequest;
import com.psyassistant.billing.invoice.dto.CreateManualInvoiceRequest;
import com.psyassistant.billing.invoice.dto.InvoiceResponse;
import com.psyassistant.billing.invoice.pdf.InvoicePdfService;
import com.psyassistant.therapists.repository.TherapistPricingRuleRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link InvoiceService}.
 *
 * <p>Covers: manual invoice creation, line item management, issue, cancel, and illegal
 * state transitions.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceNumberSeqRepository seqRepository;

    @Mock
    private com.psyassistant.sessions.repository.SessionRecordRepository sessionRecordRepository;

    @Mock
    private InvoicePdfService pdfService;

    @Mock
    private TherapistPricingRuleRepository pricingRuleRepository;

    private InvoiceService service;

    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final UUID THERAPIST_ID = UUID.randomUUID();
    private static final String ACTOR = "finance.staff";

    @BeforeEach
    void setUp() {
        InvoiceProperties props = new InvoiceProperties(14, "/tmp/invoices");
        service = new InvoiceService(invoiceRepository, seqRepository, sessionRecordRepository, pdfService, props,
                pricingRuleRepository);
    }

    // ---- createManual -------------------------------------------------------

    @Test
    void createManualPersistsInvoiceWithLineItems() {
        stubSeq(2026, 1);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            ReflectionTestUtils.setField(i, "id", UUID.randomUUID());
            return i;
        });

        CreateManualInvoiceRequest req = new CreateManualInvoiceRequest(
                CLIENT_ID,
                THERAPIST_ID,
                List.of(new CreateManualInvoiceRequest.LineItemRequest(
                        "Therapy session", BigDecimal.ONE, new BigDecimal("150.00"))),
                null,
                null
        );

        InvoiceResponse resp = service.createManual(req, ACTOR);

        assertThat(resp.invoiceNumber()).isEqualTo("2026-0001");
        assertThat(resp.clientId()).isEqualTo(CLIENT_ID);
        assertThat(resp.status()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(resp.lineItems()).hasSize(1);
        assertThat(resp.lineItems().get(0).lineTotal()).isEqualByComparingTo("150.00");
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void createManualAppliesDiscount() {
        stubSeq(2026, 1);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            ReflectionTestUtils.setField(i, "id", UUID.randomUUID());
            return i;
        });

        CreateManualInvoiceRequest req = new CreateManualInvoiceRequest(
                CLIENT_ID,
                THERAPIST_ID,
                List.of(new CreateManualInvoiceRequest.LineItemRequest(
                        "Session", BigDecimal.ONE, new BigDecimal("200.00"))),
                new BigDecimal("20.00"),
                null
        );

        InvoiceResponse resp = service.createManual(req, ACTOR);

        assertThat(resp.total()).isEqualByComparingTo("180.00");
    }

    // ---- addLineItem --------------------------------------------------------

    @Test
    void addLineItemAppendsItemAndRecalculates() {
        Invoice invoice = draftInvoiceWithNumber("2026-0001");
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        AddLineItemRequest req = new AddLineItemRequest(
                "Extra", new BigDecimal("2"), new BigDecimal("50.00"));

        service.addLineItem(invoice.getId(), req, ACTOR);

        assertThat(invoice.getLineItems()).hasSize(1);
        assertThat(invoice.getSubtotal()).isEqualByComparingTo("100.00");
    }

    @Test
    void addLineItemToIssuedInvoiceThrowsStateException() {
        Invoice invoice = draftInvoiceWithNumber("2026-0002");
        invoice.addLineItem(new InvoiceLineItem("Item", BigDecimal.ONE, new BigDecimal("10"), 0));
        invoice.issue(LocalDate.now(), LocalDate.now().plusDays(14));
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        AddLineItemRequest req = new AddLineItemRequest("X", BigDecimal.ONE, BigDecimal.TEN);

        assertThatThrownBy(() -> service.addLineItem(invoice.getId(), req, ACTOR))
                .isInstanceOf(InvoiceStateException.class)
                .hasMessageContaining("locked");
    }

    // ---- issue --------------------------------------------------------------

    @Test
    void issueTransitionsDraftToIssued() {
        Invoice invoice = draftInvoiceWithNumber("2026-0003");
        invoice.addLineItem(new InvoiceLineItem("Item", BigDecimal.ONE, new BigDecimal("100"), 0));
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(pdfService.generateAndStore(invoice)).thenReturn("/tmp/invoices/2026-0003.pdf");

        service.issue(invoice.getId(), ACTOR);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(invoice.getIssuedDate()).isEqualTo(LocalDate.now());
        assertThat(invoice.getDueDate()).isEqualTo(LocalDate.now().plusDays(14));
        assertThat(invoice.getPdfPath()).isNotNull();
    }

    @Test
    void issueEmptyInvoiceThrowsStateException() {
        Invoice invoice = draftInvoiceWithNumber("2026-0004");
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.issue(invoice.getId(), ACTOR))
                .isInstanceOf(InvoiceStateException.class)
                .hasMessageContaining("line item");
    }

    @Test
    void issueAlreadyIssuedInvoiceThrowsStateException() {
        Invoice invoice = draftInvoiceWithNumber("2026-0005");
        invoice.addLineItem(new InvoiceLineItem("Item", BigDecimal.ONE, new BigDecimal("50"), 0));
        invoice.issue(LocalDate.now(), LocalDate.now().plusDays(14));
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.issue(invoice.getId(), ACTOR))
                .isInstanceOf(InvoiceStateException.class)
                .hasMessageContaining("DRAFT");
    }

    // ---- cancel -------------------------------------------------------------

    @Test
    void cancelDraftInvoiceSucceeds() {
        Invoice invoice = draftInvoiceWithNumber("2026-0006");
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        service.cancel(invoice.getId(), new CancelInvoiceRequest("Client request"), ACTOR);

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
        assertThat(invoice.getCancellationReason()).isEqualTo("Client request");
    }

    @Test
    void cancelPaidInvoiceThrowsStateException() {
        Invoice invoice = draftInvoiceWithNumber("2026-0007");
        ReflectionTestUtils.setField(invoice, "status", InvoiceStatus.PAID);
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() ->
                service.cancel(invoice.getId(), new CancelInvoiceRequest("Test"), ACTOR))
                .isInstanceOf(InvoiceStateException.class)
                .hasMessageContaining("PAID");
    }

    // ---- getById ------------------------------------------------------------

    @Test
    void getByIdThrowsEntityNotFoundForUnknownId() {
        UUID id = UUID.randomUUID();
        when(invoiceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ---- helpers ------------------------------------------------------------

    private void stubSeq(final int year, final int startSeq) {
        InvoiceNumberSeq seq = new InvoiceNumberSeq((short) year);
        seq.setLastSeq(startSeq - 1);
        when(seqRepository.findForUpdateByYear((short) year)).thenReturn(Optional.of(seq));
        when(seqRepository.save(any(InvoiceNumberSeq.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Invoice draftInvoiceWithNumber(final String number) {
        Invoice invoice = new Invoice(CLIENT_ID, THERAPIST_ID, InvoiceSource.MANUAL);
        UUID id = UUID.randomUUID();
        ReflectionTestUtils.setField(invoice, "id", id);
        invoice.setInvoiceNumber(number);
        invoice.setInvoiceYear((short) 2026);
        invoice.setInvoiceSeq(1);
        return invoice;
    }
}
