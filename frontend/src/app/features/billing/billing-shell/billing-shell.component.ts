import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';

/**
 * Shell layout for the billing section.
 * Renders a secondary tab bar and delegates to the active child route.
 */
@Component({
  selector: 'app-billing-shell',
  standalone: true,
  imports: [RouterModule, TranslocoModule],
  template: `
    <nav class="billing-subnav" aria-label="Billing sections">
      <a routerLink="/billing/invoices"  routerLinkActive="active">
        {{ 'billing.invoices.title' | transloco }}
      </a>
      <a routerLink="/billing/packages"  routerLinkActive="active">
        {{ 'billing.packages.title' | transloco }}
      </a>
      <a routerLink="/billing/catalog"   routerLinkActive="active">
        {{ 'billing.catalog.title' | transloco }}
      </a>
      <a routerLink="/billing/discounts" routerLinkActive="active">
        {{ 'billing.discounts.title' | transloco }}
      </a>
      <a routerLink="/billing/dashboard" routerLinkActive="active">
        {{ 'billing.dashboard.title' | transloco }}
      </a>
    </nav>
    <router-outlet />
  `,
  styles: [`
    .billing-subnav {
      display: flex;
      gap: 0;
      border-bottom: 2px solid var(--color-border, #E2E8F0);
      margin-bottom: var(--spacing-lg, 1.5rem);
      padding: 0 var(--spacing-lg, 1.5rem);
      background: #fff;
      overflow-x: auto;
    }
    .billing-subnav a {
      padding: .65rem 1.1rem;
      font-size: .9rem;
      font-weight: 500;
      color: var(--color-text-secondary, #64748B);
      text-decoration: none;
      border-bottom: 2px solid transparent;
      margin-bottom: -2px;
      white-space: nowrap;
      transition: color .15s, border-color .15s;
    }
    .billing-subnav a:hover {
      color: var(--color-text-primary, #1E293B);
    }
    .billing-subnav a.active {
      color: var(--color-accent, #0EA5A0);
      border-bottom-color: var(--color-accent, #0EA5A0);
      font-weight: 600;
    }
  `],
})
export class BillingShellComponent {}
