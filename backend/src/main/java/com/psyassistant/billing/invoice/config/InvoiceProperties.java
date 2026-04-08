package com.psyassistant.billing.invoice.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Invoice configuration loaded from {@code app.invoices.*}.
 *
 * <p>Example {@code application.yml}:
 * <pre>
 * app:
 *   invoices:
 *     payment-terms-days: 14
 *     pdf-storage-path: /var/psy/invoices/pdf
 * </pre>
 */
@ConfigurationProperties(prefix = "app.invoices")
@Validated
public record InvoiceProperties(
    @Min(1) int paymentTermsDays,
    @NotBlank String pdfStoragePath
) { }
