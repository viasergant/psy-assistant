import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { DialogModule } from 'primeng/dialog';
import { Subject, finalize, takeUntil } from 'rxjs';
import { TherapistClientRow } from '../models/caseload.model';
import { CaseloadService } from '../services/caseload.service';

@Component({
  selector: 'app-caseload-drilldown',
  standalone: true,
  imports: [CommonModule, TranslocoPipe, DialogModule, TableModule],
  templateUrl: './caseload-drilldown.component.html',
  styleUrls: ['./caseload-drilldown.component.scss'],
})
export class CaseloadDrilldownComponent implements OnChanges, OnDestroy {
  @Input() therapistProfileId: string | null = null;
  @Input() therapistName: string | null = null;
  @Input() visible = false;
  @Output() visibleChange = new EventEmitter<boolean>();

  clients: TherapistClientRow[] = [];
  totalRecords = 0;
  loading = false;
  rows = 10;

  private destroy$ = new Subject<void>();

  constructor(private caseloadService: CaseloadService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['visible'] && this.visible && this.therapistProfileId) {
      this.loadPage(0);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    const page = Math.floor((event.first ?? 0) / (event.rows ?? this.rows));
    this.loadPage(page, event.rows ?? this.rows);
  }

  private loadPage(page: number, size: number = this.rows): void {
    if (!this.therapistProfileId) return;
    this.loading = true;
    this.caseloadService.getTherapistClients(this.therapistProfileId, page, size)
      .pipe(
        finalize(() => (this.loading = false)),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (res) => {
          this.clients = res.content;
          this.totalRecords = res.totalElements;
        },
      });
  }

  close(): void {
    this.visibleChange.emit(false);
  }
}
