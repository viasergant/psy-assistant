import { CommonModule } from '@angular/common';
import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
} from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe } from '@jsverse/transloco';
import { DatePicker } from 'primeng/datepicker';
import { Select } from 'primeng/select';
import { Subject, takeUntil } from 'rxjs';
import { ReportFilter, SessionTypeOption, TherapistOption } from '../../models/report.models';

export interface FilterConfig {
  showTherapist: boolean;
  showSessionType: boolean;
  showLeadSource: boolean;
}

@Component({
  selector: 'app-report-filter',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoPipe, DatePicker, Select],
  templateUrl: './report-filter.component.html',
  styleUrls: ['./report-filter.component.scss'],
})
export class ReportFilterComponent implements OnInit, OnDestroy {
  readonly leadSourceOptions = [
    { label: 'Website', value: 'WEBSITE' },
    { label: 'Referral', value: 'REFERRAL' },
    { label: 'Social Media', value: 'SOCIAL_MEDIA' },
    { label: 'Phone', value: 'PHONE' },
    { label: 'Walk-in', value: 'WALK_IN' },
    { label: 'Other', value: 'OTHER' },
  ];

  @Input() config: FilterConfig = {
    showTherapist: false,
    showSessionType: false,
    showLeadSource: false,
  };
  @Input() therapists: TherapistOption[] = [];
  @Input() sessionTypes: SessionTypeOption[] = [];

  @Output() filterChanged = new EventEmitter<ReportFilter | null>();

  form: FormGroup;
  private destroy$ = new Subject<void>();

  constructor(private fb: FormBuilder) {
    this.form = this.fb.group({
      dateRange: [null, Validators.required],
      therapistId: [null],
      sessionTypeId: [null],
      leadSource: [null],
    });
  }

  ngOnInit(): void {
    this.form.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.emitFilter();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onApply(): void {
    this.emitFilter();
  }

  onReset(): void {
    this.form.reset();
    this.filterChanged.emit(null);
  }

  private emitFilter(): void {
    const v = this.form.value;
    const range: Date[] | null = v.dateRange;

    if (!range || range.length < 2 || !range[0] || !range[1]) {
      return;
    }

    const filter: ReportFilter = {
      dateFrom: this.formatDate(range[0]),
      dateTo: this.formatDate(range[1]),
    };

    if (v.therapistId) filter.therapistId = v.therapistId;
    if (v.sessionTypeId) filter.sessionTypeId = v.sessionTypeId;
    if (v.leadSource) filter.leadSource = v.leadSource;

    this.filterChanged.emit(filter);
  }

  private formatDate(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
}
