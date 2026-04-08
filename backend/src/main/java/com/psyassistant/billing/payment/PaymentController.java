package com.psyassistant.billing.payment;

import com.psyassistant.billing.payment.dto.PaymentResponse;
import com.psyassistant.billing.payment.dto.RefundResponse;
import com.psyassistant.billing.payment.dto.RegisterPaymentRequest;
import com.psyassistant.billing.payment.dto.RegisterRefundRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for payment and refund operations on invoices.
 *
 * <p>URL structure:
 * <ul>
 *   <li>{@code POST /api/v1/invoices/{id}/payments} — register payment</li>
 *   <li>{@code GET /api/v1/invoices/{id}/payments} — list payments</li>
 *   <li>{@code POST /api/v1/invoices/{id}/refunds} — register refund</li>
 *   <li>{@code GET /api/v1/invoices/{id}/refunds} — list refunds</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/invoices/{id}")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(final PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Registers a payment against an invoice.
     *
     * @param id      invoice UUID from path
     * @param request payment details
     * @param auth    authenticated principal
     * @return 201 with payment response
     */
    @PostMapping("/payments")
    @PreAuthorize("hasAuthority('MANAGE_PAYMENTS')")
    public ResponseEntity<PaymentResponse> registerPayment(
            @PathVariable final UUID id,
            @Valid @RequestBody final RegisterPaymentRequest request,
            final Authentication auth) {
        String actor = auth.getName();
        PaymentResponse response = paymentService.registerPayment(id, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lists all payments recorded for an invoice.
     *
     * @param id invoice UUID from path
     * @return 200 with list of payment responses
     */
    @GetMapping("/payments")
    @PreAuthorize("hasAuthority('READ_INVOICES')")
    public ResponseEntity<List<PaymentResponse>> listPayments(@PathVariable final UUID id) {
        return ResponseEntity.ok(paymentService.getPaymentsForInvoice(id));
    }

    /**
     * Registers a refund against a paid invoice.
     *
     * @param id      invoice UUID from path
     * @param request refund details
     * @param auth    authenticated principal
     * @return 201 with refund response
     */
    @PostMapping("/refunds")
    @PreAuthorize("hasAuthority('MANAGE_PAYMENTS')")
    public ResponseEntity<RefundResponse> registerRefund(
            @PathVariable final UUID id,
            @Valid @RequestBody final RegisterRefundRequest request,
            final Authentication auth) {
        String actor = auth.getName();
        RefundResponse response = paymentService.registerRefund(id, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lists all refunds recorded for an invoice.
     *
     * @param id invoice UUID from path
     * @return 200 with list of refund responses
     */
    @GetMapping("/refunds")
    @PreAuthorize("hasAuthority('READ_INVOICES')")
    public ResponseEntity<List<RefundResponse>> listRefunds(@PathVariable final UUID id) {
        return ResponseEntity.ok(paymentService.getRefundsForInvoice(id));
    }
}
