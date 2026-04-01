import { Component, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { AutoComplete, AutoCompleteModule } from 'primeng/autocomplete';
import { Subject, debounceTime, distinctUntilChanged, switchMap, takeUntil } from 'rxjs';
import { ClientSearchResult } from '../../features/clients/models/client.model';
import { ClientService } from '../../features/clients/services/client.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

/**
 * Global search bar for quickly finding clients by name, email, phone, code, or tags.
 * Debounced autocomplete with 300ms delay as per PA-24 acceptance criteria.
 */
@Component({
  selector: 'app-global-search',
  standalone: true,
  imports: [CommonModule, FormsModule, AutoCompleteModule, TranslocoPipe],
  template: `
    <p-autoComplete
      [(ngModel)]="selectedClient"
      [suggestions]="suggestions"
      (completeMethod)="onSearch($event)"
      (onSelect)="onSelect($event)"
      [placeholder]="'search.client.placeholder' | transloco"
      field="name"
      [minLength]="2"
      [delay]="300"
      [showEmptyMessage]="true"
      [emptyMessage]="'search.client.noResults' | transloco"
      styleClass="global-search"
      [appendTo]="'body'">
      <ng-template let-client pTemplate="item">
        <div class="search-result-item">
          <div class="client-name">{{ client.name }}</div>
          <div class="client-meta">
            <span *ngIf="client.clientCode" class="client-code">{{ client.clientCode }}</span>
            <span *ngIf="client.email" class="client-email">{{ client.email }}</span>
            <span *ngIf="client.phone" class="client-phone">{{ client.phone }}</span>
          </div>
          <div *ngIf="client.tags?.length > 0" class="client-tags">
            <span *ngFor="let tag of client.tags" class="tag">{{ tag }}</span>
          </div>
        </div>
      </ng-template>
    </p-autoComplete>
  `,
  styles: [`
    :host {
      display: block;
      min-width: 300px;
    }

    ::ng-deep .global-search {
      width: 100%;

      .p-autocomplete-input {
        width: 100%;
      }
    }

    .search-result-item {
      padding: 0.5rem 0;
    }

    .client-name {
      font-weight: 600;
      margin-bottom: 0.25rem;
    }

    .client-meta {
      font-size: 0.875rem;
      color: var(--text-color-secondary);
      display: flex;
      gap: 1rem;
    }

    .client-tags {
      margin-top: 0.25rem;
      display: flex;
      gap: 0.25rem;
      flex-wrap: wrap;
    }

    .tag {
      font-size: 0.75rem;
      background: var(--surface-200);
      padding: 0.125rem 0.5rem;
      border-radius: 0.25rem;
    }
  `]
})
export class GlobalSearchComponent implements OnDestroy {
  selectedClient: ClientSearchResult | null = null;
  suggestions: ClientSearchResult[] = [];
  private destroy$ = new Subject<void>();

  constructor(
    private clientService: ClientService,
    private router: Router
  ) {}

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSearch(event: { query: string }): void {
    const query = event.query?.trim();
    if (!query || query.length < 2) {
      this.suggestions = [];
      return;
    }

    this.clientService.searchClients(query, 10)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (results) => {
          this.suggestions = results;
        },
        error: () => {
          this.suggestions = [];
        }
      });
  }

  onSelect(event: ClientSearchResult): void {
    if (event && event.id) {
      this.router.navigate(['/clients', event.id]);
      // Clear selection after navigation
      setTimeout(() => {
        this.selectedClient = null;
      }, 0);
    }
  }
}
