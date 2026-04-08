package com.psyassistant.billing.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.billing.invoice.Invoice;
import com.psyassistant.billing.invoice.InvoiceRepository;
import com.psyassistant.billing.invoice.InvoiceSource;
import com.psyassistant.billing.invoice.InvoiceStateException;
import com.psyassistant.billing.invoice.InvoiceStatus;
import com.psyassistant.billing.payment.dto.PaymentResponse;
import com.psyassistant.billing.payment.dto.RefundResponse;
import com.psyassistant.billing.payment.dto.RegisterPaymentRequest;
import com.psyassistant.billing.payment.dto.RegisterRefundRequest;
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
 * Unit tests for {@link PaymentService}.
 *
 * <p>Covers all payment Gherkin scenarios plus error paths.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RefundRepository refundRepository;

    private PaymentService service;

    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final UUID THERAPIST_ID = UUID.randomUUID();
    private static final String ACTOR = "finance.staff";

    @BeforeEach
    void setUp() {
        service = new PaymentService(invoiceRepository, paymentRepository, refundRepository);
    }

    // ---- helpers ------------------------------------------------------------

    private Invoice issuedInvoice(final BigDecimal total) {
        Invoice invoice = new Invoice(CLIENT_ID, THERAPIST_ID, InvoiceSource.MANUAL);
        ReflectionTestUtils.setField(invoice, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(invoice, "status", InvoiceStatus.ISSUED);
        ReflectionTestUtils.setField(invoice, "total", total);
        ReflectionTestUtils.setField(invoice, "subtotal", total);
        ReflectionTestUtils.setField(invoice, "paidAmount", BigDecimal.ZERO);
        return invoice;
    }

    private Invoice overdueInvoice(final BigDecimal total) {
        Invoice invoice = issuedInvoice(total);
        ReflectionTestUtils.setField(invoice, "status", InvoiceStatus.OVERDUE);
        return invoice;
    }

    private Invoice partiallyPaidInvoice(final BigDecimal total, final BigDecimal paidAmount) {
        Invoice invoice = issuedInvoice(total);
        ReflectionTestUtils.setField(invoice, "status", InvoiceStatus.PARTIALLY_PAID);
        ReflectionTestUtils.setField(invoice, "paidAmount", paidAmount);
        return invoice;
    }

    private Invoice paidInvoice(final BigDecimal total) {
        Invoice invoice = issuedInvoice(total);
        ReflectionTestUtils.setField(invoice, "status", InvoiceStatus.PAID);
        ReflectionTestUtils.setField(invoice, "paidAmount", total);
        return invoice;
    }

    private Payment stubSavedPayment(final UUID invoiceId, final BigDecimal amount) {
        Payment payment = new Payment(
                invoiceId, amount, PaymentMethod.BANK_TRANSFER,
                LocalDate.of(2026, 4, 8), "REF-001", null, ACTOR);
        ReflectionTestUtils.setField(payment, "id", UUID.randomUUID());
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        return payment;
    }

    private Refund stubSavedRefund(final UUID invoiceId, final BigDecimal amount) {
        Refund refund = new Refund(
                invoiceId, null, amount, "Duplicate charge",
                LocalDate.of(2026, 4, 8), null, ACTOR);
        ReflectionTestUtils.setField(refund, "id", UUID.randomUUID());
        when(refundRepository.save(any(Refund.class))).thenReturn(refund);
        return refund;
    }

    // ---- Scenario: Full payment ------------------------------------------

    @Test
    void registerPaymentFullAmountSetsStatusPaid() {
        Invoice invoice = issuedInvoice(new BigDecimal("200.00"));
        when(invoiceRepository.findByIdForUpdate(invoice.getId()))
                .thenReturn(Optional.of(invoice));
        stubSavedPayment(invoice.getId(), new BigDecimal("200.00"));

        RegisterPaymentRequest req = new RegisterPaymentRequest(
                new BigDecimal("200.00"), PaymentMethod.BANK_TRANSFER,
                LocalDate.of(2026, 4, 8), "REF-001", null);

        PaymentResponse response = service.registerPayment(invoice.getId(), req, ACTOR);

        assertThat(response.invoiceStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(response.invoiceOutstandingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(invoice.getPaidAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        verify(invoiceRepository).save(invoice);
    }

    // ---- Scenario: Partial payment --------------------------------------

    @Test
    void registerPaymentPartialAmountSetsStatusPartiallyPaid() {
        Invoice invoice = issuedInvoice(new BigDecimal("200.00"));
        when(invoiceRepository.findByIdForUpdate(invoice.getId()))
                .thenReturn(Optional.of(invoice));
        stubSavedPayment(invoice.getId(), new BigDecimal("80.00"));

        RegisterPaymentRequest req = new RegisterPaymentRequest(
                new BigDecimal("80.00"), PaymentMethod.CASH,
                LocalDate.of(2026, 4, 8), null, null);

        PaymentResponse response = service.registerPayment(invoice.getId(), req, ACTOR);

        assertThat(response.invoiceStatus()).isEqualTo(InvoiceStatus.PARTIALLY_PAID);
        assertThat(response.invoiceOutstandingBalance()).isEqualByComparingTo(new BigDecimal("120.00"));
        assertThat(invoice.getPaidAmount()).isEqualByComparingTo(new BigDecimal("80.00"));
    }

    // ---- Scenario: Second payment clears balance -------------------------

    @Test
    void registerPaymentSecondPaymentClearsBalanceSetsStatusPaid() {
        Invoice invoice = partiallyPaidInvoice(new BigDecimal("200.00"), new BigDecimal("80.00"));
        when(invoiceRepository.findByIdForUpdate(invoice.getId()))
                .thenReturn(Optional.of(invoice));
        stubSavedPayment(invoice.getId(), new BigDecimal("120.00"));

        RegisterPaymentRequest req = new RegisterPaymentRequest(
                new BigDecimal("120.00"), PaymentMethod.BANK_TRANSFER,
                LocalDate.of(2026, 4, 8), "REF-002", null);

        PaymentResponse response = service.registerPayment(invoice.getId(), req, ACTOR);

        assertThat(response.invoiceStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(response.invoiceOutstandingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(invoice.getPaidAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    // ---- Scenario: Overpayment rejected ----------------------------------

    @Test
    void registerPaymentOverpaymentThrowsPaymentValidationException() {
        Invoice invoice = issuedInvoice(new BigDecimal("200.00"));
        when(invoiceRepository.findByIdForUpdate(invoice.getId()))
                .thenReturn(Optional.of(invoice));

        RegisterPaymentRequest req = new RegisterPaymentRequest(
                new BigDecimal("250.00"), PaymentMethod.CARD,
                LocalDate.of(2026, 4, 8), null, null);

        assertThatThrownBy(() -> service.registerPayment(invoice.getId(), req, ACTOR))
                .isInstanceOf(PaymentValidationException.class)
                .hasMessageContaining("exceeds outstanding balance");

        verify(paymentRepository, never()).save(any());
        verify(invoiceRepository, never()).save(any());
    }

    // ---- Scenario: Payment on OVERDUE invoice works ----------------------

    @Test
    void registerPaymentOverdueInvoiceFullPaymentSetsStatusPaid() {
        Invoice invoice = overdueInvoice(new BigDecimal("200.00"));
        when(invoiceRepository.findByIdForUpdate(invoice.getId()))
                .thenReturn(Optional.of(invoice));
        stubSavedPayment(invoice.getId(), new BigDecimal("200.00"));

        RegisterPaymentRequest req = new RegisterPaymentRequest(
                new BigDecimal("200.00"), PaymentMethod.CASH,
                LocalDate.of(2026, 4, 8), null, null);

        PaymentResponse response = service.registerPayment(invoice.getId(), req, ACTOR);

        assertThat(response.invoiceStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    // ---- Scenario: Payment on CANCELLED invoice rejected ----------------

    @Test
    void registerPaymentCancelledInvoiceThrowsInvoiceStateException() {
        Invoice invoice = issuedInvoice(new BigDecimal("200.00"));
        ReflectionTestUtils.setField(invoice, "status", InvoiceStatus.CANCELLED);
        when(invoiceRepository.findByIdForUpdate(invoice.getId()))
                .thenReturn(Optional.of(invoice));

        RegisterPaymentRequest req = new RegisterPaymentRequest(
                new BigDecimal("100.00"), PaymentMethod.CASH,
                LocalDate.of(2026, 4, 8), null, null);

        assertThatThrownBy(() -> service.registerPayment(invoice.getId(), req, ACTOR))
                .isInstanceOf(InvoiceStateException.class);

        verify(paymentRepository, never()).save(any());
    }

    // ---- Scenario: Payment on DRAFT invoice rejected --------------------

    @Test
    void registerPaymentDraftInvoiceThrowsInvoiceStateException() {
        Invoice invoice = new Invoice(CLIENT_ID, THERAPIST_ID, InvoiceSource.MANUAL);
        ReflectionTestUtils.setField(invoice, "id", UUID.randomUUID());
        when(invoiceRepository.findByIdForUpdate(invoice.getId()))
                .thenReturn(Optional.of(invoice));

        RegisterPaymentRequest req = new RegisterPaymentRequest(
                new BigDecimal("100.00"), PaymentMethod.CASH,
                LocalDate.of(2026, 4, 8), null, null);

        assertThatThrownBy(() -> service.registerPayment(invoice.getId(), req, ACTOR))
                .isInstanceOf(InvoiceStateException.class);
    }

    // ---- Scenario: Invoice not found ------------------------------------

    @Test
    void registerPaymentInvoiceNotFoundThrowsEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(invoiceRepository.findByIdForUpdate(id)).thenReturn(Optional.empty());

        RegisterPaymentRequest req = new RegisterPaymentRequest(
                new BigDecimal("100.00"), PaymentMethod.CASH,
                LocalDate.of(2026, 4, 8), null, null);

        assertThatThrownBy(() -> service.registerPayment(id, req, ACTOR))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---- Scenario: Refund on PAID invoice --------------------------------

    @Test
    void registerRefundPaidInvoiceCreatesRefundAndStatusRemainsPaid() {
        Invoice invoice = paidInvoice(new BigDecimal("200.00"));
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        stubSavedRefund(invoice.getId(), new BigDecimal("50.00"));

        RegisterRefundRequest req = new RegisterRefundRequest(
                new BigDecimal("50.00"), "Duplicate charge",
                LocalDate.of(2026, 4, 8), null, null);

        RefundResponse response = service.registerRefund(invoice.getId(), req, ACTOR);

        assertThat(response.invoiceId()).isEqualTo(invoice.getId());
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        verify(refundRepository).save(any(Refund.class));
        verify(invoiceRepository, never()).save(any());
    }

    // ---- Scenario: Refund on non-PAID invoice rejected ------------------

    @Test
    void registerRefundIssuedInvoiceThrowsInvoiceStateException() {
        Invoice invoice = issuedInvoice(new BigDecimal("200.00"));
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        RegisterRefundRequest req = new RegisterRefundRequest(
                new BigDecimal("50.00"), "Wrong status",
                LocalDate.of(2026, 4, 8), null, null);

        assertThatThrownBy(() -> service.registerRefund(invoice.getId(), req, ACTOR))
                .isInstanceOf(InvoiceStateException.class);

        verify(refundRepository, never()).save(any());
    }

    // ---- getPaymentsForInvoice ------------------------------------------

    @Test
    void getPaymentsForInvoiceReturnsAllPayments() {
        Invoice invoice = paidInvoice(new BigDecimal("200.00"));
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        Payment payment = new Payment(
                invoice.getId(), new BigDecimal("200.00"), PaymentMethod.CASH,
                LocalDate.of(2026, 4, 8), null, null, ACTOR);
        ReflectionTestUtils.setField(payment, "id", UUID.randomUUID());
        when(paymentRepository.findByInvoiceId(invoice.getId())).thenReturn(List.of(payment));

        List<PaymentResponse> result = service.getPaymentsForInvoice(invoice.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).amount()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    // ---- getRefundsForInvoice -------------------------------------------

    @Test
    void getRefundsForInvoiceInvoiceNotFoundThrowsEntityNotFoundException() {
        UUID id = UUID.randomUUID();
        when(invoiceRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.getRefundsForInvoice(id))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
