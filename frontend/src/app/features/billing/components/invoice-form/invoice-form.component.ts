import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormArray, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { InvoiceService } from '../../services/invoice.service';

@Component({
  selector: 'app-invoice-form',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, TranslocoModule],
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
            <label class="form-label" for="clientId">
              {{ 'billing.invoices.form.clientId' | transloco }} *
            </label>
            <input id="clientId" type="text" class="form-input"
                   formControlName="clientId"
                   [placeholder]="'billing.invoices.form.clientIdPlaceholder' | transloco" />
            <div *ngIf="form.get('clientId')?.invalid && form.get('clientId')?.touched"
                 class="field-error">
              {{ 'common.validation.required' | transloco }}
            </div>
          </div>

          <div class="form-group">
            <label class="form-label" for="therapistId">
              {{ 'billing.invoices.form.therapistId' | transloco }}
            </label>
            <input id="therapistId" type="text" class="form-input"
                   formControlName="therapistId"
                   [placeholder]="'billing.invoices.form.therapistIdPlaceholder' | transloco" />
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
  `]
})
export class InvoiceFormComponent {
  form = this.fb.group({
    clientId: ['', Validators.required],
    therapistId: [''],
    notes: [''],
    discount: [0],
    lineItems: this.fb.array([])
  });

  submitting = false;
  error: string | null = null;

  constructor(
    private fb: FormBuilder,
    private invoiceService: InvoiceService,
    private router: Router
  ) {}

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
