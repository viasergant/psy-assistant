package com.psyassistant.reporting.reports.export;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * Writes report rows to an XLSX stream using {@link SXSSFWorkbook} with a 100-row
 * in-memory window to avoid OOM errors for large exports (up to 50,000 rows).
 */
public class XlsxExportWriter {

    private static final int STREAMING_WINDOW = 100;

    /**
     * Writes the header row and all data rows to the provided output stream.
     *
     * @param out     the output stream (caller is responsible for closing)
     * @param headers column header labels
     * @param rows    data rows
     * @throws IOException if writing fails
     */
    public void write(final OutputStream out, final List<String> headers, final List<Object[]> rows)
            throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(STREAMING_WINDOW)) {
            workbook.setCompressTempFiles(true);
            final Sheet sheet = workbook.createSheet("Report");
            final CellStyle dateStyle = createDateStyle(workbook);

            // Header row
            final Row header = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                header.createCell(i).setCellValue(headers.get(i));
            }

            // Data rows
            int rowNum = 1;
            for (final Object[] rowData : rows) {
                final Row row = sheet.createRow(rowNum++);
                for (int col = 0; col < rowData.length; col++) {
                    writeCell(row.createCell(col), rowData[col], dateStyle);
                }
            }

            workbook.write(out);
            workbook.dispose();
        }
    }

    private CellStyle createDateStyle(final SXSSFWorkbook workbook) {
        final CreationHelper helper = workbook.getCreationHelper();
        final CellStyle style = workbook.createCellStyle();
        style.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd"));
        return style;
    }

    private void writeCell(final Cell cell, final Object value, final CellStyle dateStyle) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof LocalDate ld) {
            cell.setCellValue(ld);
            cell.setCellStyle(dateStyle);
        } else if (value instanceof BigDecimal bd) {
            cell.setCellValue(bd.doubleValue());
        } else if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
    }
}
