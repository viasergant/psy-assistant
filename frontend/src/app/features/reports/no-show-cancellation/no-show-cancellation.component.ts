import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { Subject, finalize, takeUntil } from 'rxjs';
import { NoShowCancellationRow, ReportFilter, SessionTypeOption, TherapistOption } from '../models/report.models';
import { ReportService } from '../services/report.service';
import { ExportControlsComponent } from '../shared/export-controls/export-controls.component';
import { ReportEmptyStateComponent } from '../shared/report-empty-state/report-empty-state.component';
import { FilterConfig, ReportFilterComponent } from '../shared/report-filter/report-filter.component';

@Component({
  selector: 'app-no-show-cancellation',
  standalone: true,
  imports: [
    CommonModule,
    TranslocoPipe,
    TableModule,
    ReportFilterComponent,
    ExportControlsComponent,
    ReportEmptyStateComponent,
  ],
  templateUrl: './no-show-cancellation.component.html',
  styleUrls: ['./no-show-cancellation.component.scss'],
})
export class NoShowCancellationComponent implements OnInit, OnDestroy {
  rows: NoShowCancellationRow[] = [];
  totalRecords = 0;
  loading = false;
  pageSize = 25;
  first = 0;
  activeFilter: ReportFilter | null = null;
  therapists: TherapistOption[] = [];
  sessionTypes: SessionTypeOption[] = [];

  readonly filterConfig: FilterConfig = {
    showTherapist: true,
    showSessionType: true,
    showLeadSource: false,
  };

  private readonly reportService = inject(ReportService);
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.reportService.getTherapists().pipe(takeUntil(this.destroy$)).subscribe({
      next: (res) => { this.therapists = res.content ?? []; },
    });
    this.reportService.getSessionTypes().pipe(takeUntil(this.destroy$)).subscribe({
      next: (res) => { this.sessionTypes = res; },
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onFilterChanged(filter: ReportFilter | null): void {
    this.activeFilter = filter;
    if (filter) {
      this.first = 0;
      this.loadPage(0);
    } else {
      this.rows = [];
      this.totalRecords = 0;
    }
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    if (!this.activeFilter) return;
    this.first = event.first ?? 0;
    this.pageSize = event.rows ?? this.pageSize;
    this.loadPage(Math.floor(this.first / this.pageSize));
  }

  onExportCsv(): void {
    if (this.activeFilter) this.reportService.exportReport('no-show-cancellation', this.activeFilter, 'csv');
  }

  onExportXlsx(): void {
    if (this.activeFilter) this.reportService.exportReport('no-show-cancellation', this.activeFilter, 'xlsx');
  }

  private loadPage(page: number): void {
    if (!this.activeFilter) return;
    this.loading = true;
    this.reportService
      .getNoShowCancellation({ ...this.activeFilter, page, size: this.pageSize })
      .pipe(finalize(() => (this.loading = false)), takeUntil(this.destroy$))
      .subscribe({
        next: (res) => {
          this.rows = res.content;
          this.totalRecords = res.totalElements;
        },
      });
  }
}
