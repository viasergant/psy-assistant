import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { MultiSelect } from 'primeng/multiselect';
import { DatePicker } from 'primeng/datepicker';
import { Button } from 'primeng/button';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { Subject, debounceTime, distinctUntilChanged, finalize, takeUntil } from 'rxjs';
import { CaseloadRow } from '../models/caseload.model';
import { CaseloadService } from '../services/caseload.service';
import { CaseloadDrilldownComponent } from '../caseload-drilldown/caseload-drilldown.component';

interface SpecializationOption {
  label: string;
  value: string;
}

@Component({
  selector: 'app-caseload-overview',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslocoPipe,
    MultiSelect,
    DatePicker,
    Button,
    TableModule,
    CaseloadDrilldownComponent,
  ],
  templateUrl: './caseload-overview.component.html',
  styleUrls: ['./caseload-overview.component.scss'],
})
export class CaseloadOverviewComponent implements OnInit, OnDestroy {
  rows: CaseloadRow[] = [];
  totalRecords = 0;
  loading = false;
  pageSize = 20;
  first = 0;

  // Drilldown state
  drilldownVisible = false;
  selectedTherapistId: string | null = null;
  selectedTherapistName: string | null = null;

  filterForm: FormGroup;
  specializationOptions: SpecializationOption[] = [];

  private currentPage = 0;
  private destroy$ = new Subject<void>();

  constructor(
    private caseloadService: CaseloadService,
    private fb: FormBuilder,
  ) {
    this.filterForm = this.fb.group({
      specializations: [[]],
      snapshotDate: [null],
    });
  }

  ngOnInit(): void {
    this.filterForm.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
      takeUntil(this.destroy$),
    ).subscribe(() => {
      this.first = 0;
      this.currentPage = 0;
      this.loadPage();
    });

    this.loadPage();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    this.first = event.first ?? 0;
    this.pageSize = event.rows ?? this.pageSize;
    this.currentPage = Math.floor(this.first / this.pageSize);
    this.loadPage();
  }

  openDrilldown(row: CaseloadRow): void {
    this.selectedTherapistId = row.therapistProfileId;
    this.selectedTherapistName = row.therapistName;
    this.drilldownVisible = true;
  }

  formatUtilization(rate: number | null): string {
    if (rate === null) return '—';
    return (rate * 100).toFixed(1) + '%';
  }

  private loadPage(): void {
    const form = this.filterForm.value;
    const snapshotDate: Date | null = form.snapshotDate;

    this.loading = true;
    this.caseloadService.listCaseload({
      specializations: form.specializations?.length ? form.specializations : undefined,
      snapshotDate: snapshotDate ? this.formatDate(snapshotDate) : undefined,
      page: this.currentPage,
      size: this.pageSize,
    }).pipe(
      finalize(() => (this.loading = false)),
      takeUntil(this.destroy$),
    ).subscribe({
      next: (res) => {
        this.rows = res.content;
        this.totalRecords = res.totalElements;
      },
    });
  }

  private formatDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
}
