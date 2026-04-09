import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { RouterModule } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import { PackageDefinition, PackageInstance } from '../../../billing/models/package.model';
import { PackageService } from '../../../billing/services/package.service';
import { SellPackageDialogComponent } from '../../../billing/components/sell-package-dialog/sell-package-dialog.component';

@Component({
  selector: 'app-client-packages',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslocoModule, SellPackageDialogComponent],
  template: `
    <div class="section">
      <div class="section-header">
        <h2 class="section-title">{{ 'clients.packages.title' | transloco }}</h2>
        <button type="button" class="btn-ghost btn-sm" (click)="openSell()">
          + {{ 'clients.packages.actions.sell' | transloco }}
        </button>
      </div>

      <div *ngIf="loading" class="state-msg" aria-live="polite">
        {{ 'clients.packages.loading' | transloco }}
      </div>

      <div *ngIf="loadError" class="alert-error" role="alert">{{ loadError }}</div>

      <div *ngIf="!loading && !loadError && packages.length === 0" class="empty-section">
        {{ 'clients.packages.empty' | transloco }}
      </div>

      <!-- Active packages first, then exhausted/expired -->
      <div *ngIf="!loading && packages.length > 0" class="packages-list">
        <div *ngFor="let pkg of packages" class="package-card" [class.package-card--inactive]="pkg.status !== 'ACTIVE'">
          <div class="package-card__header">
            <span class="package-name">{{ pkg.definitionName }}</span>
            <span class="badge badge-sm"
                  [class.badge-active]="pkg.status === 'ACTIVE'"
                  [class.badge-warn]="pkg.status === 'EXPIRED'"
                  [class.badge-inactive]="pkg.status === 'EXHAUSTED'">
              {{ 'clients.packages.status.' + pkg.status | transloco }}
            </span>
          </div>

          <div class="package-card__meta">
            <span class="meta-item">
              {{ 'billing.catalog.serviceTypes.' + pkg.serviceType | transloco }}
            </span>
            <span class="meta-sep">·</span>
            <span class="meta-item">
              {{ 'clients.packages.purchased' | transloco : { date: (pkg.purchasedAt | date:'mediumDate') } }}
            </span>
            <ng-container *ngIf="pkg.expiresAt">
              <span class="meta-sep">·</span>
              <span class="meta-item" [class.text-warn]="isNearExpiry(pkg)">
                {{ 'clients.packages.expires' | transloco : { date: (pkg.expiresAt | date:'mediumDate') } }}
              </span>
            </ng-container>
          </div>

          <!-- Session progress bar -->
          <div class="session-progress" [attr.aria-label]="pkg.sessionsRemaining + ' of ' + pkg.sessionsTotal + ' sessions remaining'">
            <div class="session-progress__bar-track">
              <div class="session-progress__bar-fill"
                   [style.width.%]="(pkg.sessionsRemaining / pkg.sessionsTotal) * 100"
                   [class.progress-fill--low]="(pkg.sessionsRemaining / pkg.sessionsTotal) <= 0.25"></div>
            </div>
            <div class="session-progress__label">
              <span class="sessions-remaining">
                {{ 'clients.packages.sessionsRemaining' | transloco : { remaining: pkg.sessionsRemaining, total: pkg.sessionsTotal } }}
              </span>
              <span *ngIf="pkg.invoiceId" class="invoice-link">
                <a [routerLink]="['/billing/invoices', pkg.invoiceId]" class="link-subtle">
                  {{ 'clients.packages.viewInvoice' | transloco }}
                </a>
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <app-sell-package-dialog
      *ngIf="showSellDialog"
      [clientId]="clientId"
      [definitions]="definitions"
      (sold)="onSold($event)"
      (cancel)="showSellDialog = false"
    />
  `,
  styles: [`
    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: var(--spacing-md, 1rem);
    }
    .packages-list {
      display: flex;
      flex-direction: column;
      gap: var(--spacing-sm, .75rem);
    }
    .package-card {
      border: 1px solid var(--color-border, #E2E8F0);
      border-radius: var(--radius-md, 8px);
      padding: var(--spacing-md, 1rem);
      background: #fff;
    }
    .package-card--inactive {
      background: var(--color-surface, #F8FAFC);
      opacity: .85;
    }
    .package-card__header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: .35rem;
    }
    .package-name {
      font-weight: 600;
      font-size: .95rem;
      color: var(--color-text-primary, #1E293B);
    }
    .package-card__meta {
      display: flex;
      align-items: center;
      gap: .4rem;
      font-size: .825rem;
      color: var(--color-text-secondary, #64748B);
      margin-bottom: .75rem;
    }
    .meta-sep { opacity: .5; }
    .session-progress { }
    .session-progress__bar-track {
      height: 6px;
      background: var(--color-border, #E2E8F0);
      border-radius: 99px;
      overflow: hidden;
      margin-bottom: .4rem;
    }
    .session-progress__bar-fill {
      height: 100%;
      background: var(--color-accent, #0EA5A0);
      border-radius: 99px;
      transition: width .3s ease;
    }
    .progress-fill--low {
      background: var(--color-warn, #F59E0B);
    }
    .session-progress__label {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: .825rem;
    }
    .sessions-remaining {
      font-weight: 600;
      color: var(--color-text-primary, #1E293B);
    }
    .empty-section {
      color: var(--color-text-secondary, #64748B);
      font-size: .9rem;
      padding: .5rem 0;
    }
    .text-warn { color: var(--color-warn, #F59E0B); }
    .link-subtle {
      color: var(--color-accent, #0EA5A0);
      text-decoration: none;
      font-size: .8rem;
    }
    .link-subtle:hover { text-decoration: underline; }
  `],
})
export class ClientPackagesComponent implements OnInit, OnChanges {
  @Input() clientId!: string;

  packages: PackageInstance[] = [];
  definitions: PackageDefinition[] = [];
  loading = false;
  loadError = '';
  showSellDialog = false;

  constructor(private packageService: PackageService) {}

  ngOnInit(): void {
    this.loadPackages();
    this.packageService.listDefinitions('ACTIVE').subscribe((defs) => (this.definitions = defs));
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['clientId'] && !changes['clientId'].firstChange) {
      this.loadPackages();
    }
  }

  loadPackages(): void {
    if (!this.clientId) return;
    this.loading = true;
    this.loadError = '';
    this.packageService.listClientPackages(this.clientId).subscribe({
      next: (pkgs) => {
        // Sort: ACTIVE first, then EXHAUSTED, then EXPIRED
        this.packages = pkgs.sort((a, b) => {
          const order = { ACTIVE: 0, EXHAUSTED: 1, EXPIRED: 2 };
          return order[a.status] - order[b.status];
        });
        this.loading = false;
      },
      error: () => {
        this.loadError = 'clients.packages.loadError';
        this.loading = false;
      },
    });
  }

  isNearExpiry(pkg: PackageInstance): boolean {
    if (!pkg.expiresAt || pkg.status !== 'ACTIVE') return false;
    const daysLeft = Math.floor(
      (new Date(pkg.expiresAt).getTime() - Date.now()) / (1000 * 60 * 60 * 24),
    );
    return daysLeft < 14;
  }

  openSell(): void {
    this.showSellDialog = true;
  }

  onSold(_instance: PackageInstance): void {
    this.showSellDialog = false;
    this.loadPackages();
  }
}
