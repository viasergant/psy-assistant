package com.psyassistant.billing.invoice.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.psyassistant.billing.invoice.Invoice;
import com.psyassistant.billing.invoice.config.InvoiceProperties;
import com.psyassistant.crm.clients.ClientRepository;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Generates PDF invoices from a Thymeleaf HTML template using openhtmltopdf-pdfbox.
 *
 * <p>Uses a standalone {@link ClassLoaderTemplateResolver} rather than relying on
 * Spring Boot web MVC auto-configuration, ensuring this service works correctly
 * in any application context.
 *
 * <p>Target: PDF generation ≤5 s for ≤50 line items.
 */
@Service
public class InvoicePdfService {

    private static final Logger LOG = LoggerFactory.getLogger(InvoicePdfService.class);
    private static final String TEMPLATE_NAME = "invoice-pdf";

    private final InvoiceProperties properties;
    private final SpringTemplateEngine templateEngine;
    private final ClientRepository clientRepository;

    public InvoicePdfService(final InvoiceProperties properties, final ClientRepository clientRepository) {
        this.properties = properties;
        this.clientRepository = clientRepository;
        this.templateEngine = buildTemplateEngine();
    }

    /**
     * Renders the invoice as a PDF file and stores it in the configured storage path.
     *
     * @param invoice the invoice to render
     * @return the absolute file path where the PDF was stored
     */
    public String generateAndStore(final Invoice invoice) {
        String html = renderHtml(invoice);
        Path storagePath = Paths.get(properties.pdfStoragePath());

        try {
            Files.createDirectories(storagePath);
        } catch (IOException e) {
            throw new PdfGenerationException("Cannot create PDF storage directory: " + storagePath, e);
        }

        String fileName = "invoice-" + invoice.getInvoiceNumber().replace("/", "-") + ".pdf";
        Path outputPath = storagePath.resolve(fileName);

        try (OutputStream out = new FileOutputStream(outputPath.toFile())) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, outputPath.getParent().toUri().toString());
            builder.toStream(out);
            builder.run();
            LOG.info("PDF generated for invoice {} at {}", invoice.getInvoiceNumber(), outputPath);
            return outputPath.toAbsolutePath().toString();
        } catch (Exception e) {
            throw new PdfGenerationException("Failed to generate PDF for invoice " + invoice.getInvoiceNumber(), e);
        }
    }

    /**
     * Reads a stored PDF file and returns its raw bytes.
     *
     * @param pdfPath the absolute path to the PDF file
     * @return PDF bytes
     */
    public byte[] readStored(final String pdfPath) {
        try {
            return Files.readAllBytes(Path.of(pdfPath));
        } catch (IOException e) {
            throw new PdfGenerationException("Cannot read PDF at path: " + pdfPath, e);
        }
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    private String renderHtml(final Invoice invoice) {
        Context ctx = new Context();
        ctx.setVariables(buildModel(invoice));
        return templateEngine.process(TEMPLATE_NAME, ctx);
    }

    private Map<String, Object> buildModel(final Invoice invoice) {
        Map<String, Object> model = new HashMap<>();
        model.put("invoice", invoice);
        UUID clientId = invoice.getClientId();
        String clientName = clientId != null
                ? clientRepository.findById(clientId).map(c -> c.getFullName()).orElse(clientId.toString())
                : "";
        model.put("clientName", clientName);
        return model;
    }

    private static SpringTemplateEngine buildTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(org.thymeleaf.templatemode.TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    /**
     * Unchecked exception thrown when PDF generation or retrieval fails.
     */
    public static class PdfGenerationException extends RuntimeException {
        public PdfGenerationException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
