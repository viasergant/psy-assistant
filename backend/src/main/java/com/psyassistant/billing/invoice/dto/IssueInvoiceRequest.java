package com.psyassistant.billing.invoice.dto;

import java.time.LocalDate;

/** Request body for issuing a draft invoice. */
public record IssueInvoiceRequest(
    LocalDate dueDate
) { }
