package com.psyassistant.reporting.reports.export;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Writes report rows to a CSV stream.
 *
 * <p>Uses {@link PrintWriter} directly to avoid buffering entire result set in memory.
 * Values containing commas, quotes, or newlines are properly escaped per RFC 4180.
 */
public class CsvExportWriter {

    /**
     * Writes the header row and all data rows to the provided writer.
     *
     * @param writer  the output writer (caller is responsible for closing)
     * @param headers column header labels
     * @param rows    data rows; each Object is formatted by {@link #formatCell}
     * @throws IOException if writing fails
     */
    public void write(final PrintWriter writer, final List<String> headers, final List<Object[]> rows)
            throws IOException {
        writer.println(String.join(",", headers.stream().map(this::escapeCsv).toList()));
        for (final Object[] row : rows) {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < row.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(escapeCsv(formatCell(row[i])));
            }
            writer.println(sb);
        }
        writer.flush();
    }

    private String escapeCsv(final String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    private String formatCell(final Object value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }
}
