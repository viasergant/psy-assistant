import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';

/**
 * Admin area layout with tabbed navigation
 */
@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslocoModule],
  template: `
    <div class="admin-container">
      <div class="admin-header">
        <h1>{{ 'admin.title' | transloco }}</h1>
      </div>

      <nav class="admin-tabs" role="tablist">
        <a
          routerLink="/admin/users"
          routerLinkActive="active"
          class="tab"
          role="tab"
          [attr.aria-selected]="rla1.isActive"
          #rla1="routerLinkActive"
        >
          {{ 'admin.tabs.users' | transloco }}
        </a>
        <a
          routerLink="/admin/therapists"
          routerLinkActive="active"
          class="tab"
          role="tab"
          [attr.aria-selected]="rla2.isActive"
          #rla2="routerLinkActive"
        >
          {{ 'admin.tabs.therapists' | transloco }}
        </a>
        <a
          routerLink="/admin/leave-requests"
          routerLinkActive="active"
          class="tab"
          role="tab"
          [attr.aria-selected]="rla3.isActive"
          #rla3="routerLinkActive"
        >
          {{ 'admin.tabs.leaveRequests' | transloco }}
        </a>
      </nav>

      <div class="admin-content">
        <router-outlet></router-outlet>
      </div>
    </div>
  `,
  styles: [`
    .admin-container {
      display: flex;
      flex-direction: column;
      height: 100%;
      background: #f9fafb;
    }

    .admin-header {
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

    .admin-tabs {
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
        position: relative;

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

    .admin-content {
      flex: 1;
      overflow: auto;
      padding: 0;
    }
  `]
})
export class AdminLayoutComponent {}
