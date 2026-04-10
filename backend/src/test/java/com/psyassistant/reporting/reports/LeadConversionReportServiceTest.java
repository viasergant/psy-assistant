package com.psyassistant.reporting.reports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import com.psyassistant.reporting.reports.dto.LeadConversionRow;
import com.psyassistant.reporting.reports.dto.PagedReportResponse;
import com.psyassistant.reporting.reports.dto.ReportFilter;
import com.psyassistant.reporting.reports.impl.LeadConversionReportService;
import com.psyassistant.reporting.reports.repository.LeadConversionReportRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link LeadConversionReportService}.
 */
@ExtendWith(MockitoExtension.class)
class LeadConversionReportServiceTest {

    @Mock
    private LeadConversionReportRepository repository;

    private LeadConversionReportService service;

    @BeforeEach
    void setUp() {
        service = new LeadConversionReportService(repository);
    }

    @Test
    @DisplayName("returns empty paged response when no data")
    void returnsEmptyWhenNoData() {
        when(repository.count(any(), any(), isNull())).thenReturn(0L);

        final ReportFilter filter = new ReportFilter(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                null, null, null, 0, 25);

        final PagedReportResponse<LeadConversionRow> result = service.getPage(filter);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    @DisplayName("maps rows correctly from repository")
    void mapsRowsFromRepository() {
        final LocalDate from = LocalDate.of(2025, 1, 1);
        final LocalDate to = LocalDate.of(2025, 12, 31);

        final List<Object[]> repoRows = List.of(
                new Object[]{"WEBSITE", 10L, 3L, new BigDecimal("30.00")},
                new Object[]{"REFERRAL", 5L, 4L, new BigDecimal("80.00")}
        );

        when(repository.count(eq(from), eq(to), isNull())).thenReturn(2L);
        when(repository.findPage(eq(from), eq(to), isNull(), anyInt(), anyInt()))
                .thenReturn(repoRows);

        final ReportFilter filter = new ReportFilter(from, to, null, null, null, 0, 25);
        final PagedReportResponse<LeadConversionRow> result = service.getPage(filter);

        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).leadSource()).isEqualTo("WEBSITE");
        assertThat(result.content().get(0).totalLeads()).isEqualTo(10L);
        assertThat(result.content().get(0).conversionRate()).isEqualByComparingTo("30.00");
        assertThat(result.totalElements()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getAll returns flat list for export")
    void getAllReturnsFlatList() {
        final LocalDate from = LocalDate.of(2025, 1, 1);
        final LocalDate to = LocalDate.of(2025, 12, 31);

        final List<Object[]> repoRows = List.<Object[]>of(
                new Object[]{"WEBSITE", 10L, 3L, new BigDecimal("30.00")}
        );
        doReturn(repoRows).when(repository).findAll(from, to, "WEBSITE");

        final ReportFilter filter = new ReportFilter(from, to, null, null, "WEBSITE", 0, 25);
        final List<LeadConversionRow> result = service.getAll(filter);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).leadSource()).isEqualTo("WEBSITE");
    }
}
