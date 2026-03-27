import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { ClientDetail } from '../models/client.model';
import { ClientService } from '../services/client.service';

/**
 * Minimal client detail page displayed after lead-to-client conversion.
 *
 * Shows the client's name, a link back to the originating lead, and notes.
 * A fuller implementation with contact methods and session history is
 * planned for a subsequent iteration.
 */
@Component({
  selector: 'app-client-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="page">
      <a class="back-link" routerLink="/leads">&larr; Back to leads</a>

      <div *ngIf="loading" class="state-msg" aria-live="polite">Loading client…</div>

      <div *ngIf="loadError" class="alert-error" role="alert">{{ loadError }}</div>

      <div *ngIf="client && !loading">
        <header class="page-header">
          <h1>{{ client.fullName }}</h1>
          <span class="badge-client">Client</span>
        </header>

        <div class="section">
          <h2 class="section-title">Details</h2>

          <dl class="detail-list">
            <dt>Client ID</dt>
            <dd>{{ client.id }}</dd>

            <ng-container *ngIf="client.sourceLeadId">
              <dt>Converted from lead</dt>
              <dd>
                <a [routerLink]="['/leads']" class="link">View lead history</a>
              </dd>
            </ng-container>

            <dt>Created</dt>
            <dd>{{ client.createdAt | date:'dd MMM yyyy HH:mm' }}</dd>
          </dl>
        </div>

        <div class="section" *ngIf="client.notes">
          <h2 class="section-title">Pre-conversion history</h2>
          <div class="notes-block">{{ client.notes }}</div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page { padding: 2rem; max-width: 800px; margin: 0 auto; }
    .back-link {
      display: inline-block; margin-bottom: 1.5rem;
      color: #0EA5A0; text-decoration: none; font-size: .9375rem;
    }
    .back-link:hover { text-decoration: underline; }
    .page-header {
      display: flex; align-items: center; gap: 1rem; margin-bottom: 1.5rem;
    }
    h1 { margin: 0; font-size: 1.5rem; }
    .badge-client {
      padding: .2rem .7rem; background: #F0FDF4;
      border-radius: 999px; font-size: .8rem; font-weight: 600; color: #166534;
    }
    .section { margin-bottom: 2rem; }
    .section-title {
      font-size: 1rem; font-weight: 600; color: #374151;
      border-bottom: 1.5px solid #E2E8F0; padding-bottom: .5rem; margin-bottom: 1rem;
    }
    .detail-list {
      display: grid; grid-template-columns: 160px 1fr; row-gap: .6rem;
      column-gap: 1rem; font-size: .9375rem;
    }
    dt { font-weight: 500; color: #6B7280; }
    dd { margin: 0; color: #0F172A; }
    .link { color: #0EA5A0; text-decoration: underline; }
    .notes-block {
      background: #F9FAFB; border: 1px solid #E2E8F0; border-radius: 8px;
      padding: 1rem; font-size: .9375rem; white-space: pre-wrap; color: #374151;
    }
    .state-msg { color: #64748B; padding: 2rem 0; text-align: center; }
    .alert-error {
      padding: .75rem 1rem; background: #FEF2F2;
      border: 1px solid #FECACA; border-radius: 8px;
      color: #DC2626; margin-bottom: 1rem; font-size: .875rem;
    }
  `]
})
export class ClientDetailComponent implements OnInit {
  client: ClientDetail | null = null;
  loading = false;
  loadError: string | null = null;

  constructor(
    private route: ActivatedRoute,
    private clientService: ClientService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.loadError = 'No client ID provided.';
      return;
    }

    this.loading = true;
    this.clientService.getClient(id).subscribe({
      next: (c) => {
        this.loading = false;
        this.client = c;
      },
      error: () => {
        this.loading = false;
        this.loadError = 'Failed to load client. Please try again.';
      }
    });
  }
}
