package com.psyassistant.billing.payment;

import com.psyassistant.billing.invoice.Invoice;
import com.psyassistant.billing.invoice.InvoiceRepository;
import com.psyassistant.billing.invoice.InvoiceStatus;
import com.psyassistant.billing.payment.dto.PaymentResponse;
import com.psyassistant.billing.payment.dto.RefundResponse;
import com.psyassistant.billing.payment.dto.RegisterPaymentRequest;
import com.psyassistant.billing.payment.dto.RegisterRefundRequest;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core service for payment and refund lifecycle management.
 *
 * <p>Uses pessimistic locking ({@code SELECT ... FOR UPDATE}) on the invoice row
 * during payment registration to prevent concurrent overpayments.
 */
@Service
@Transactional
public class PaymentService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;

    public PaymentService(
            final InvoiceRepository invoiceRepository,
            final PaymentRepository paymentRepository,
            final RefundRepository refundRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
    }

    /**
     * Records a payment against an invoice.
     *
     * <p>Acquires a pessimistic write lock on the invoice row, validates the state
     * machine and amount, then atomically persists the payment and updates
     * {@code paid_amount} and {@code status} on the invoice.
     *
     * @param invoiceId UUID of the invoice to pay
     * @param request   payment details
     * @param actor     name of the principal recording the payment (from JWT)
     * @return response DTO for the created payment record
     * @throws EntityNotFoundException    if the invoice is not found
     * @throws com.psyassistant.billing.invoice.InvoiceStateException
     *                                    if the invoice cannot accept payments
     * @throws PaymentValidationException if the amount exceeds the outstanding balance
     */
    public PaymentResponse registerPayment(
            final UUID invoiceId,
            final RegisterPaymentRequest request,
            final String actor) {

        Invoice invoice = invoiceRepository.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + invoiceId));

        invoice.canAcceptPayment();
        invoice.applyPayment(request.amount());

        Payment payment = new Payment(
                invoiceId,
                request.amount(),
                request.paymentMethod(),
                request.paymentDate(),
                request.reference(),
                request.notes(),
                actor);

        Payment saved = paymentRepository.save(payment);
        invoiceRepository.save(invoice);

        LOG.info("Payment registered: invoiceId={}, paymentId={}, amount={}, method={}",
                invoiceId, saved.getId(), request.amount(), request.paymentMethod());

        return PaymentResponse.from(saved, invoice.getOutstandingBalance(), invoice.getStatus());
    }

    /**
     * Records a refund against a PAID invoice.
     *
     * <p>The invoice remains in PAID status after a refund is registered.
     *
     * @param invoiceId UUID of the invoice to refund against
     * @param request   refund details
     * @param actor     name of the principal recording the refund
     * @return response DTO for the created refund record
     * @throws EntityNotFoundException if the invoice is not found
     * @throws com.psyassistant.billing.invoice.InvoiceStateException
     *                                 if the invoice is not in PAID status
     */
    public RefundResponse registerRefund(
            final UUID invoiceId,
            final RegisterRefundRequest request,
            final String actor) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + invoiceId));

        if (invoice.getStatus() != InvoiceStatus.PAID) {
            throw new com.psyassistant.billing.invoice.InvoiceStateException(
                    "Refunds can only be issued on PAID invoices, current status: " + invoice.getStatus());
        }

        Refund refund = new Refund(
                invoiceId,
                request.paymentId(),
                request.amount(),
                request.reason(),
                request.refundDate(),
                request.reference(),
                actor);

        Refund saved = refundRepository.save(refund);

        LOG.info("Refund registered: invoiceId={}, refundId={}, amount={}",
                invoiceId, saved.getId(), request.amount());

        return RefundResponse.from(saved);
    }

    /**
     * Returns all payments recorded for a given invoice.
     *
     * @param invoiceId UUID of the invoice
     * @return list of payment response DTOs (may be empty)
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForInvoice(final UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + invoiceId));

        return paymentRepository.findByInvoiceId(invoiceId).stream()
                .map(p -> PaymentResponse.from(p, invoice.getOutstandingBalance(), invoice.getStatus()))
                .toList();
    }

    /**
     * Returns all refunds recorded for a given invoice.
     *
     * @param invoiceId UUID of the invoice
     * @return list of refund response DTOs (may be empty)
     */
    @Transactional(readOnly = true)
    public List<RefundResponse> getRefundsForInvoice(final UUID invoiceId) {
        if (!invoiceRepository.existsById(invoiceId)) {
            throw new EntityNotFoundException("Invoice not found: " + invoiceId);
        }
        return refundRepository.findByInvoiceId(invoiceId).stream()
                .map(RefundResponse::from)
                .toList();
    }
}
