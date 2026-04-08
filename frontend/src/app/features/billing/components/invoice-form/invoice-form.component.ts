import { CommonModule } from '@angular/common';
import { Component, inject, OnDestroy } from '@angular/core';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormArray, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { AutoCompleteModule, AutoCompleteSelectEvent } from 'primeng/autocomplete';
import { Subject, takeUntil } from 'rxjs';
import { InvoiceService } from '../../services/invoice.service';
import { ClientService } from '../../../clients/services/client.service';
import { ClientSearchResult } from '../../../clients/models/client.model';
import { TherapistManagementService } from '../../../admin/therapists/services/therapist-management.service';
import { TherapistProfile } from '../../../admin/therapists/models/therapist.model';

@Component({
  selector: 'app-invoice-form',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, TranslocoModule, AutoCompleteModule],
  template: `
    <div class="page-container">
      <a [routerLink]="['/billing/invoices']" class="back-link">
        ← {{ 'billing.invoices.actions.backToList' | transloco }}
      </a>
      <h1 class="page-title">{{ 'billing.invoices.form.createManual' | transloco }}</h1>

      <div *ngIf="error" class="alert-error" role="alert">{{ error }}</div>

      <form [formGroup]="form" (ngSubmit)="onSubmit()" novalidate>
        <div class="card">
          <h2 class="card-title">{{ 'billing.invoices.form.clientSection' | transloco }}</h2>

          <div class="form-group">
            <label class="form-label" for="clientSearch">
              {{ 'billing.invoices.form.clientId' | transloco }} *
            </label>
            <p-autoComplete
              inputId="clientSearch"
              [ngModel]="clientQuery"
              (ngModelChange)="clientQuery = $event?.name ?? $event ?? ''"
              [ngModelOptions]="{standalone: true}"
              [suggestions]="clientSuggestions"
              (completeMethod)="searchClients($event)"
              (onSelect)="onClientSelect($event)"
              (onClear)="onClientClear()"
              field="name"
              [minLength]="2"
              [delay]="300"
              [showClear]="true"
              [showEmptyMessage]="true"
              [emptyMessage]="'billing.invoices.form.noClientResults' | transloco"
              [placeholder]="'billing.invoices.form.clientIdPlaceholder' | transloco"
              styleClass="w-full"
              [appendTo]="'body'">
              <ng-template let-client pTemplate="item">
                <div class="autocomplete-item">
                  <div class="autocomplete-item-name">{{ client.name }}</div>
                  <div class="autocomplete-item-meta">
                    <span *ngIf="client.clientCode">{{ client.clientCode }}</span>
                    <span *ngIf="client.email">{{ client.email }}</span>
                    <span *ngIf="client.phone">{{ client.phone }}</span>
                  </div>
                </div>
              </ng-template>
            </p-autoComplete>
            <div *ngIf="form.get('clientId')?.invalid && form.get('clientId')?.touched"
                 class="field-error">
              {{ 'common.validation.required' | transloco }}
            </div>
          </div>

          <div class="form-group">
            <label class="form-label" for="therapistSearch">
              {{ 'billing.invoices.form.therapistId' | transloco }}
            </label>
            <p-autoComplete
              inputId="therapistSearch"
              [ngModel]="therapistQuery"
              (ngModelChange)="therapistQuery = $event?.name ?? $event?.email ?? $event ?? ''"
              [ngModelOptions]="{standalone: true}"
              [suggestions]="therapistSuggestions"
              (completeMethod)="searchTherapists($event)"
              (onSelect)="onTherapistSelect($event)"
              (onClear)="onTherapistClear()"
              field="name"
              [minLength]="1"
              [delay]="200"
              [showClear]="true"
              [showEmptyMessage]="true"
              [emptyMessage]="'billing.invoices.form.noTherapistResults' | transloco"
              [placeholder]="'billing.invoices.form.therapistIdPlaceholder' | transloco"
              styleClass="w-full"
              [appendTo]="'body'">
              <ng-template let-therapist pTemplate="item">
                <div class="autocomplete-item">
                  <div class="autocomplete-item-name">{{ therapist.name }}</div>
                  <div class="autocomplete-item-meta">
                    <span *ngIf="therapist.email">{{ therapist.email }}</span>
                  </div>
                </div>
              </ng-template>
            </p-autoComplete>
          </div>

          <div class="form-group">
            <label class="form-label" for="notes">
              {{ 'billing.invoices.form.notes' | transloco }}
            </label>
            <textarea id="notes" class="form-textarea"
                      formControlName="notes"
                      rows="2"
                      [placeholder]="'billing.invoices.form.notesPlaceholder' | transloco">
            </textarea>
          </div>

          <div class="form-group">
            <label class="form-label" for="discount">
              {{ 'billing.invoices.form.discount' | transloco }}
            </label>
            <input id="discount" type="number" class="form-input form-input-sm"
                   formControlName="discount"
                   min="0"
                   step="0.01" />
          </div>
        </div>

        <!-- Line items -->
        <div class="card">
          <div class="card-header-row">
            <h2 class="card-title">{{ 'billing.invoices.form.lineItems' | transloco }}</h2>
            <button type="button" class="btn-secondary" (click)="addItem()">
              + {{ 'billing.invoices.actions.addLineItem' | transloco }}
            </button>
          </div>

          <div *ngIf="lineItems.length === 0" class="empty-state-small">
            {{ 'billing.invoices.form.noLineItems' | transloco }}
          </div>

          <div formArrayName="lineItems">
            <div *ngFor="let item of lineItems.controls; let i = index"
                 [formGroupName]="i" class="line-item-row">
              <div class="form-group flex-grow">
                <label class="form-label sr-only">{{ 'billing.invoices.form.description' | transloco }}</label>
                <input type="text" class="form-input"
                       formControlName="description"
                       [placeholder]="'billing.invoices.form.description' | transloco" />
              </div>
              <div class="form-group" style="flex: 0 0 100px">
                <label class="form-label sr-only">{{ 'billing.invoices.form.quantity' | transloco }}</label>
                <input type="number" class="form-input"
                       formControlName="quantity"
                       min="0.01"
                       [placeholder]="'billing.invoices.form.quantity' | transloco" />
              </div>
              <div class="form-group" style="flex: 0 0 120px">
                <label class="form-label sr-only">{{ 'billing.invoices.form.unitPrice' | transloco }}</label>
                <input type="number" class="form-input"
                       formControlName="unitPrice"
                       min="0"
                       step="0.01"
                       [placeholder]="'billing.invoices.form.unitPrice' | transloco" />
              </div>
              <button type="button" class="btn-icon-danger"
                      [attr.aria-label]="'billing.invoices.actions.removeLineItem' | transloco"
                      (click)="removeItem(i)">✕</button>
            </div>
          </div>

          <div *ngIf="lineItems.length > 0" class="totals-preview">
            <span class="total-label">{{ 'billing.invoices.detail.total' | transloco }}:</span>
            <span class="total-value">{{ computedTotal | number:'1.2-2' }}</span>
          </div>
        </div>

        <!-- Submit -->
        <div class="form-actions">
          <a [routerLink]="['/billing/invoices']" class="btn-secondary">
            {{ 'common.actions.cancel' | transloco }}
          </a>
          <button type="submit" class="btn-primary"
                  [disabled]="form.invalid || lineItems.length === 0 || submitting">
            {{ 'billing.invoices.actions.createDraft' | transloco }}
          </button>
        </div>
      </form>
    </div>
  `,
  styles: [`
    .page-container { padding: var(--spacing-lg); max-width: 800px; }
    .back-link { display: inline-block; color: var(--color-accent); text-decoration: none;
      margin-bottom: var(--spacing-md); font-size: 0.875rem; }
    .page-title { margin: 0 0 var(--spacing-lg); font-size: 1.5rem; }
    .alert-error { padding: var(--spacing-sm) var(--spacing-md); background: #fee2e2;
      color: #991b1b; border-radius: var(--radius-sm); margin-bottom: var(--spacing-md); }
    .card { background: var(--color-surface, #fff); border: 1px solid var(--color-border);
      border-radius: var(--radius-md); padding: var(--spacing-md); margin-bottom: var(--spacing-md); }
    .card-header-row { display: flex; justify-content: space-between; align-items: center;
      margin-bottom: var(--spacing-md); }
    .card-title { font-size: 1rem; font-weight: 600; margin: 0; }
    .form-group { margin-bottom: var(--spacing-sm); }
    .form-label { display: block; font-size: 0.875rem; font-weight: 500;
      margin-bottom: 4px; color: var(--color-text-secondary); }
    .sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0,0,0,0); }
    .form-input { width: 100%; padding: 8px 10px; border: 1px solid var(--color-border);
      border-radius: var(--radius-sm); font-size: 0.875rem; }
    .form-input-sm { width: 120px; }
    .form-textarea { width: 100%; padding: 8px 10px; border: 1px solid var(--color-border);
      border-radius: var(--radius-sm); font-size: 0.875rem; resize: vertical; }
    .field-error { font-size: 0.75rem; color: #dc2626; margin-top: 2px; }
    .flex-grow { flex: 1; }
    .empty-state-small { color: var(--color-text-muted); font-size: 0.875rem;
      padding: var(--spacing-sm) 0; }
    .line-item-row { display: flex; gap: var(--spacing-sm); align-items: flex-end;
      margin-bottom: var(--spacing-sm); }
    .btn-icon-danger { background: none; border: none; color: #ef4444; cursor: pointer;
      font-size: 1rem; padding: 8px 6px; border-radius: var(--radius-sm); line-height: 1; }
    .btn-icon-danger:hover { background: #fee2e2; }
    .totals-preview { display: flex; justify-content: flex-end; gap: var(--spacing-md);
      margin-top: var(--spacing-md); padding-top: var(--spacing-md);
      border-top: 1px solid var(--color-border); font-size: 0.9rem; }
    .total-label { color: var(--color-text-muted); }
    .total-value { font-weight: 700; }
    .form-actions { display: flex; justify-content: flex-end; gap: var(--spacing-sm); }
    ::ng-deep .w-full { width: 100%; }
    ::ng-deep .w-full .p-autocomplete-input { width: 100%; }
    .autocomplete-item { padding: 2px 0; }
    .autocomplete-item-name { font-weight: 500; }
    .autocomplete-item-meta { font-size: 0.75rem; color: var(--color-text-muted); display: flex; gap: 0.75rem; margin-top: 1px; }
  `]
})
export class InvoiceFormComponent implements OnDestroy {
  private fb = inject(FormBuilder);
  private invoiceService = inject(InvoiceService);
  private router = inject(Router);
  private clientService = inject(ClientService);
  private therapistService = inject(TherapistManagementService);
  private destroy$ = new Subject<void>();

  form = this.fb.group({
    clientId: ['', Validators.required],
    therapistId: [''],
    notes: [''],
    discount: [0],
    lineItems: this.fb.array([])
  });

  submitting = false;
  error: string | null = null;

  clientQuery = '';
  clientSuggestions: ClientSearchResult[] = [];

  therapistQuery = '';
  therapistSuggestions: TherapistProfile[] = [];
  private allTherapists: TherapistProfile[] = [];

  constructor() {
    this.therapistService.getTherapists(0, 200, undefined, true)
      .pipe(takeUntil(this.destroy$))
      .subscribe({ next: (page) => { this.allTherapists = page.content; }, error: () => {} });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get lineItems() {
    return this.form.get('lineItems') as FormArray;
  }

  get computedTotal(): number {
    let subtotal = 0;
    this.lineItems.controls.forEach(ctrl => {
      const qty = Number(ctrl.get('quantity')?.value) || 0;
      const price = Number(ctrl.get('unitPrice')?.value) || 0;
      subtotal += qty * price;
    });
    const discount = Number(this.form.get('discount')?.value) || 0;
    return subtotal - discount;
  }

  searchClients(event: { query: string }): void {
    const query = event.query?.trim();
    if (!query || query.length < 2) { this.clientSuggestions = []; return; }
    this.clientService.searchClients(query, 10)
      .pipe(takeUntil(this.destroy$))
      .subscribe({ next: (r) => { this.clientSuggestions = r; }, error: () => { this.clientSuggestions = []; } });
  }

  onClientSelect(event: AutoCompleteSelectEvent): void {
    const client = event.value as ClientSearchResult;
    this.form.get('clientId')!.setValue(client.id);
    this.form.get('clientId')!.markAsTouched();
  }

  onClientClear(): void {
    this.form.get('clientId')!.setValue('');
    this.form.get('clientId')!.markAsTouched();
  }

  searchTherapists(event: { query: string }): void {
    const query = event.query?.trim().toLowerCase();
    if (!query) { this.therapistSuggestions = this.allTherapists.slice(0, 10); return; }
    this.therapistSuggestions = this.allTherapists
      .filter(t => t.name.toLowerCase().includes(query) || t.email.toLowerCase().includes(query))
      .slice(0, 10);
  }

  onTherapistSelect(event: AutoCompleteSelectEvent): void {
    const therapist = event.value as TherapistProfile;
    this.form.get('therapistId')!.setValue(therapist.id);
  }

  onTherapistClear(): void {
    this.form.get('therapistId')!.setValue('');
  }

  addItem(): void {
    this.lineItems.push(this.fb.group({
      description: ['', Validators.required],
      quantity: [1, [Validators.required, Validators.min(0.01)]],
      unitPrice: [0, [Validators.required, Validators.min(0)]]
    }));
  }

  removeItem(index: number): void {
    this.lineItems.removeAt(index);
  }

  onSubmit(): void {
    if (this.form.invalid || this.lineItems.length === 0) { return; }
    this.submitting = true;
    this.error = null;

    const val = this.form.value;
    this.invoiceService.createManual({
      clientId: val.clientId!,
      therapistId: val.therapistId || undefined,
      notes: val.notes || undefined,
      discount: val.discount || undefined,
      lineItems: this.lineItems.controls.map(ctrl => ({
        description: ctrl.get('description')?.value,
        quantity: Number(ctrl.get('quantity')?.value),
        unitPrice: Number(ctrl.get('unitPrice')?.value)
      }))
    }).subscribe({
      next: (invoice) => {
        this.router.navigate(['/billing/invoices', invoice.id]);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to create invoice';
        this.submitting = false;
      }
    });
  }
}
