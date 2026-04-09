import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { TranslocoModule } from '@jsverse/transloco';
import { CreateDiscountRuleRequest, DiscountRule } from '../../models/discount.model';
import { ServiceCatalogItem } from '../../models/service-catalog.model';
import { DiscountService } from '../../services/discount.service';
import { ServiceCatalogService } from '../../services/service-catalog.service';
import { DiscountRuleFormComponent } from '../discount-rule-form/discount-rule-form.component';

@Component({
  selector: 'app-discount-rule-list',
  standalone: true,
  imports: [CommonModule, TranslocoModule, DiscountRuleFormComponent],
  template: `
    <div class="page-container">

      <div class="page-header">
        <h1 class="page-title">{{ 'billing.discounts.title' | transloco }}</h1>
        <button type="button" class="btn-primary" (click)="openCreate()">
          + {{ 'billing.discounts.actions.create' | transloco }}
        </button>
      </div>

      <div *ngIf="loading" class="state-msg" aria-live="polite">
        {{ 'billing.discounts.list.loading' | transloco }}
      </div>

      <div *ngIf="loadError" class="alert-error" role="alert">{{ loadError }}</div>

      <div *ngIf="!loading && !loadError && rules.length === 0" class="empty-state">
        <p>{{ 'billing.discounts.list.empty' | transloco }}</p>
        <button type="button" class="btn-primary" (click)="openCreate()">
          + {{ 'billing.discounts.actions.create' | transloco }}
        </button>
      </div>

      <div *ngIf="!loading && rules.length > 0" class="table-wrapper">
        <table class="data-table table-hoverable" role="table"
               [attr.aria-label]="'billing.discounts.title' | transloco">
          <thead>
            <tr>
              <th scope="col">{{ 'billing.discounts.fields.name' | transloco }}</th>
              <th scope="col">{{ 'billing.discounts.fields.type' | transloco }}</th>
              <th scope="col" class="col-right">{{ 'billing.discounts.fields.value' | transloco }}</th>
              <th scope="col">{{ 'billing.discounts.fields.scope' | transloco }}</th>
              <th scope="col">{{ 'billing.discounts.fields.linkedTo' | transloco }}</th>
              <th scope="col">{{ 'billing.discounts.fields.status' | transloco }}</th>
              <th scope="col">{{ 'billing.catalog.fields.actions' | transloco }}</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let rule of rules">
              <td class="fw-medium">{{ rule.name }}</td>
              <td>{{ 'billing.discounts.types.' + rule.type | transloco }}</td>
              <td class="col-right">
                <ng-container *ngIf="rule.type === 'PERCENTAGE'">{{ rule.value }}%</ng-container>
                <ng-container *ngIf="rule.type === 'FIXED_AMOUNT'">{{ rule.value | number:'1.2-2' }}</ng-container>
              </td>
              <td>{{ 'billing.discounts.scopes.' + rule.scope | transloco }}</td>
              <td>
                <span *ngIf="rule.scope === 'CLIENT' && rule.clientId" class="muted">
                  {{ 'billing.discounts.fields.clientScopeLabel' | transloco }}
                </span>
                <span *ngIf="rule.scope === 'SERVICE' && rule.serviceCatalogId" class="muted">
                  {{ getServiceName(rule.serviceCatalogId) }}
                </span>
              </td>
              <td>
                <span class="badge"
                      [class.badge-active]="rule.active"
                      [class.badge-inactive]="!rule.active">
                  {{ (rule.active ? 'billing.discounts.status.active' : 'billing.discounts.status.inactive') | transloco }}
                </span>
              </td>
              <td class="action-cell">
                <button *ngIf="rule.active" type="button" class="btn-link btn-sm text-danger"
                        (click)="deactivate(rule)">
                  {{ 'billing.discounts.actions.deactivate' | transloco }}
                </button>
                <button *ngIf="!rule.active" type="button" class="btn-link btn-sm"
                        (click)="activate(rule)">
                  {{ 'billing.discounts.actions.activate' | transloco }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <app-discount-rule-form
        *ngIf="showForm"
        [services]="services"
        (saved)="onSaved($event)"
        (cancel)="showForm = false"
      />
    </div>
  `,
})
export class DiscountRuleListComponent implements OnInit {
  rules: DiscountRule[] = [];
  services: ServiceCatalogItem[] = [];
  loading = false;
  loadError = '';
  showForm = false;

  constructor(
    private discountService: DiscountService,
    private catalogService: ServiceCatalogService,
  ) {}

  ngOnInit(): void {
    this.loadRules();
    this.catalogService.list('ACTIVE').subscribe((svcs) => (this.services = svcs));
  }

  loadRules(): void {
    this.loading = true;
    this.loadError = '';
    this.discountService.list().subscribe({
      next: (rules) => {
        this.rules = rules;
        this.loading = false;
      },
      error: () => {
        this.loadError = 'billing.discounts.list.loadError';
        this.loading = false;
      },
    });
  }

  openCreate(): void {
    this.showForm = true;
  }

  onSaved(req: CreateDiscountRuleRequest): void {
    this.discountService.create(req).subscribe({
      next: (rule) => {
        this.rules = [...this.rules, rule];
        this.showForm = false;
      },
    });
  }

  deactivate(rule: DiscountRule): void {
    this.discountService.toggleActive(rule.id, false).subscribe((updated) => this.replaceRule(updated));
  }

  activate(rule: DiscountRule): void {
    this.discountService.toggleActive(rule.id, true).subscribe((updated) => this.replaceRule(updated));
  }

  getServiceName(serviceCatalogId: string): string {
    return this.services.find((s) => s.id === serviceCatalogId)?.name ?? serviceCatalogId;
  }

  private replaceRule(updated: DiscountRule): void {
    this.rules = this.rules.map((r) => (r.id === updated.id ? updated : r));
  }
}
