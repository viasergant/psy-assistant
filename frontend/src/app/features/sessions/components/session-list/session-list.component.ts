import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { MessageService } from 'primeng/api';
import { AutoComplete } from 'primeng/autocomplete';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { MultiSelect } from 'primeng/multiselect';
import { Paginator } from 'primeng/paginator';
import { Select } from 'primeng/select';
import { Skeleton } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Subject, debounceTime, distinctUntilChanged, finalize, takeUntil } from 'rxjs';
import { SessionRecord, SessionFilters, SessionStatus } from '../../models/session.model';
import { SessionService } from '../../services/session.service';
import { CancelSessionDialogComponent } from '../cancel-session-dialog/cancel-session-dialog.component';
import { CompleteSessionDialogComponent } from '../complete-session-dialog/complete-session-dialog.component';

interface StatusOption {
  label: string;
  value: SessionStatus;
}

interface DateRangeOption {
  label: string;
  value: string;
}

/**
 * Session list component with filtering, status management, and action workflows
 */
@Component({
  selector: 'app-session-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TranslocoPipe,
    TableModule,
    Button,
    DatePicker,
    Select,
    MultiSelect,
    AutoComplete,
    Tag,
    Skeleton,
    Paginator,
    CompleteSessionDialogComponent,
    CancelSessionDialogComponent,
  ],
  templateUrl: './session-list.component.html',
  styleUrls: ['./session-list.component.scss'],
})
export class SessionListComponent implements OnInit, OnDestroy {
  sessions: SessionRecord[] = [];
  filteredSessions: SessionRecord[] = [];
  loading = false;
  error: string | null = null;

  filterForm: FormGroup;
  dateRangeOptions: DateRangeOption[] = [];
  statusOptions: StatusOption[] = [];

  // Dialog state
  showCompleteDialog = false;
  showCancelDialog = false;
  selectedSession: SessionRecord | null = null;

  // Pagination
  first = 0;
  rows = 10;

  SessionStatus = SessionStatus;

  private destroy$ = new Subject<void>();

  constructor(
    private sessionService: SessionService,
    private fb: FormBuilder,
    private transloco: TranslocoService,
    private messageService: MessageService
  ) {
    this.filterForm = this.fb.group({
      dateRange: ['today'],
      customStartDate: [null],
      customEndDate: [null],
      clientSearch: [''],
      statusFilter: [[]],
    });
  }

  ngOnInit(): void {
    this.initializeFilterOptions();
    this.setupFilterListeners();
    this.loadSessions();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private initializeFilterOptions(): void {
    this.dateRangeOptions = [
      {
        label: this.transloco.translate('sessions.list.today'),
        value: 'today',
      },
      {
        label: this.transloco.translate('sessions.list.thisWeek'),
        value: 'thisWeek',
      },
      {
        label: this.transloco.translate('sessions.list.last7Days'),
        value: 'last7Days',
      },
      {
        label: this.transloco.translate('sessions.list.last30Days'),
        value: 'last30Days',
      },
      {
        label: this.transloco.translate('sessions.list.customRange'),
        value: 'custom',
      },
    ];

    this.statusOptions = [
      {
        label: this.transloco.translate('sessions.status.pending'),
        value: SessionStatus.PENDING,
      },
      {
        label: this.transloco.translate('sessions.status.inProgress'),
        value: SessionStatus.IN_PROGRESS,
      },
      {
        label: this.transloco.translate('sessions.status.completed'),
        value: SessionStatus.COMPLETED,
      },
      {
        label: this.transloco.translate('sessions.status.cancelled'),
        value: SessionStatus.CANCELLED,
      },
    ];
  }

  private setupFilterListeners(): void {
    this.filterForm.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => {
        this.loadSessions();
      });
  }

  private loadSessions(): void {
    this.loading = true;
    this.error = null;

    const filters = this.buildFilters();

    this.sessionService
      .getSessions(filters)
      .pipe(
        finalize(() => (this.loading = false)),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (sessions) => {
          this.sessions = sessions;
          this.filteredSessions = this.applyClientFilter(sessions);
        },
        error: () => {
          this.error = this.transloco.translate('sessions.messages.loadError');
          this.sessions = [];
          this.filteredSessions = [];
        },
      });
  }

  private buildFilters(): SessionFilters {
    const formValue = this.filterForm.value;
    const filters: SessionFilters = {};

    const dateRange = this.getDateRange(formValue.dateRange);
    if (dateRange) {
      filters.startDate = dateRange.start;
      filters.endDate = dateRange.end;
    }

    if (formValue.statusFilter && formValue.statusFilter.length > 0) {
      filters.status = formValue.statusFilter;
    }

    return filters;
  }

  private getDateRange(rangeType: string): { start: string; end: string } | null {
    const today = new Date();
    let start: Date;
    let end: Date = today;

    switch (rangeType) {
      case 'today':
        start = today;
        break;
      case 'thisWeek':
        start = new Date(today);
        start.setDate(today.getDate() - today.getDay());
        break;
      case 'last7Days':
        start = new Date(today);
        start.setDate(today.getDate() - 7);
        break;
      case 'last30Days':
        start = new Date(today);
        start.setDate(today.getDate() - 30);
        break;
      case 'custom':
        const customStart = this.filterForm.get('customStartDate')?.value;
        const customEnd = this.filterForm.get('customEndDate')?.value;
        if (!customStart || !customEnd) return null;
        return {
          start: this.formatDate(customStart),
          end: this.formatDate(customEnd),
        };
      default:
        return null;
    }

    return {
      start: this.formatDate(start),
      end: this.formatDate(end),
    };
  }

  private applyClientFilter(sessions: SessionRecord[]): SessionRecord[] {
    const searchTerm = this.filterForm.get('clientSearch')?.value?.toLowerCase() || '';
    if (!searchTerm) return sessions;

    return sessions.filter((session) =>
      session.clientName.toLowerCase().includes(searchTerm)
    );
  }

  private formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }

  clearFilters(): void {
    this.filterForm.patchValue({
      dateRange: 'today',
      customStartDate: null,
      customEndDate: null,
      clientSearch: '',
      statusFilter: [],
    });
  }

  getStatusSeverity(status: SessionStatus): string {
    switch (status) {
      case SessionStatus.PENDING:
        return 'secondary';
      case SessionStatus.IN_PROGRESS:
        return 'success';
      case SessionStatus.COMPLETED:
        return 'info';
      case SessionStatus.CANCELLED:
        return 'danger';
      default:
        return 'secondary';
    }
  }

  canStartSession(session: SessionRecord): boolean {
    return session.status === SessionStatus.PENDING;
  }

  canCompleteSession(session: SessionRecord): boolean {
    return session.status === SessionStatus.IN_PROGRESS;
  }

  canCancelSession(session: SessionRecord): boolean {
    return (
      session.status === SessionStatus.PENDING ||
      session.status === SessionStatus.IN_PROGRESS
    );
  }

  startSession(session: SessionRecord): void {
    this.sessionService
      .startSession(session.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: this.transloco.translate('sessions.messages.sessionStarted'),
          });
          this.loadSessions();
        },
        error: () => {
          this.messageService.add({
            severity: 'error',
            summary: this.transloco.translate('sessions.messages.startError'),
          });
        },
      });
  }

  openCompleteDialog(session: SessionRecord): void {
    this.selectedSession = session;
    this.showCompleteDialog = true;
  }

  openCancelDialog(session: SessionRecord): void {
    this.selectedSession = session;
    this.showCancelDialog = true;
  }

  onSessionCompleted(): void {
    this.showCompleteDialog = false;
    this.selectedSession = null;
    this.loadSessions();
  }

  onSessionCancelled(): void {
    this.showCancelDialog = false;
    this.selectedSession = null;
    this.loadSessions();
  }

  onDialogClose(): void {
    this.showCompleteDialog = false;
    this.showCancelDialog = false;
    this.selectedSession = null;
  }

  get isCustomDateRange(): boolean {
    return this.filterForm.get('dateRange')?.value === 'custom';
  }
}
