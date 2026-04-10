import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { ButtonModule } from 'primeng/button';

@Component({
  selector: 'app-export-controls',
  standalone: true,
  imports: [CommonModule, TranslocoPipe, ButtonModule],
  template: `
    <div class="export-controls">
      <p-button
        [label]="'reports.export.csv' | transloco"
        icon="pi pi-file"
        severity="secondary"
        size="small"
        [disabled]="disabled"
        (onClick)="exportCsv.emit()"
      />
      <p-button
        [label]="'reports.export.xlsx' | transloco"
        icon="pi pi-file-excel"
        severity="secondary"
        size="small"
        [disabled]="disabled"
        (onClick)="exportXlsx.emit()"
      />
    </div>
  `,
  styles: [`
    .export-controls {
      display: flex;
      gap: var(--spacing-sm);
    }
  `],
})
export class ExportControlsComponent {
  @Input() disabled = true;
  @Output() exportCsv = new EventEmitter<void>();
  @Output() exportXlsx = new EventEmitter<void>();
}
