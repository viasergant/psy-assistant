import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';

/**
 * Layout for global admin settings — accessible only to the configured
 * global admin account (GLOBAL_ADMIN JWT authority).
 */
@Component({
  selector: 'app-global-admin-layout',
  standalone: true,
  imports: [RouterModule, TranslocoModule],
  template: `
    <div class="ga-container">
      <div class="ga-header">
        <h1>{{ 'globalAdmin.title' | transloco }}</h1>
      </div>

      <nav class="ga-tabs" role="tablist">
        <a
          routerLink="/global-admin/risk-flag-types"
          routerLinkActive="active"
          class="tab"
          role="tab"
          [attr.aria-selected]="rla1.isActive"
          #rla1="routerLinkActive"
        >
          {{ 'globalAdmin.tabs.riskFlagTypes' | transloco }}
        </a>
        <a
          routerLink="/global-admin/users"
          routerLinkActive="active"
          class="tab"
          role="tab"
          [attr.aria-selected]="rla2.isActive"
          #rla2="routerLinkActive"
        >
          {{ 'globalAdmin.tabs.users' | transloco }}
        </a>
      </nav>

      <div class="ga-content">
        <router-outlet></router-outlet>
      </div>
    </div>
  `,
  styles: [`
    .ga-container {
      display: flex;
      flex-direction: column;
      height: 100%;
      background: #f9fafb;
    }

    .ga-header {
      background: white;
      border-bottom: 1px solid #e5e7eb;
      padding: 1.5rem 2rem;

      h1 {
        margin: 0;
        font-size: 1.875rem;
        font-weight: 700;
        color: #111827;
      }
    }

    .ga-tabs {
      display: flex;
      gap: 0.25rem;
      background: white;
      border-bottom: 2px solid #e5e7eb;
      padding: 0 2rem;

      .tab {
        padding: 1rem 1.5rem;
        color: #6b7280;
        text-decoration: none;
        font-weight: 500;
        border-bottom: 3px solid transparent;
        margin-bottom: -2px;
        transition: all 0.2s;

        &:hover {
          color: #374151;
          background: #f9fafb;
        }

        &.active {
          color: #3b82f6;
          border-bottom-color: #3b82f6;
          font-weight: 600;
        }
      }
    }

    .ga-content {
      flex: 1;
      overflow: auto;
    }
  `]
})
export class GlobalAdminLayoutComponent {}
