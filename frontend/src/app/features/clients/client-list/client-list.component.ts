import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import {
  TherapistManagementService,
} from '../../admin/therapists/services/therapist-management.service';
import { TherapistProfile } from '../../admin/therapists/models/therapist.model';
import { ClientListItem } from '../models/client.model';
import { ClientService } from '../services/client.service';

@Component({
  selector: 'app-client-list',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslocoPipe],
  template: `
    <div class="cl-page">

      <!-- Page header -->
      <header class="cl-header">
        <div class="cl-header-left">
          <h1 class="cl-title">{{ 'clients.title' | transloco }}</h1>
          <span class="cl-count" *ngIf="!loading && !loadError">{{ totalElements }}</span>
        </div>
      </header>

      <!-- Filter bar -->
      <div class="cl-filters" role="group" [attr.aria-label]="'clients.list.ariaFilters' | transloco">

        <!-- Text search -->
        <div class="cl-search-wrap">
          <span class="cl-search-icon">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/>
            </svg>
          </span>
          <input
            class="cl-search"
            type="search"
            [(ngModel)]="searchQuery"
            (ngModelChange)="onSearchChange($event)"
            [placeholder]="'clients.list.searchPlaceholder' | transloco"
            aria-label="Search clients"
          />
        </div>

        <!-- Therapist filter -->
        <div class="cl-filter-group">
          <label class="cl-filter-label" for="therapistFilter">
            {{ 'clients.list.therapistFilterLabel' | transloco }}
          </label>
          <select
            id="therapistFilter"
            class="cl-select"
            [(ngModel)]="selectedTherapistId"
            (change)="applyFilters()">
            <option value="">{{ 'clients.list.therapistAll' | transloco }}</option>
            <option *ngFor="let t of therapists" [value]="t.id">{{ t.name }}</option>
          </select>
        </div>

        <!-- Tag filter -->
        <div class="cl-filter-group">
          <label class="cl-filter-label">
            {{ 'clients.list.tagFilterLabel' | transloco }}
          </label>
          <div class="cl-tag-filter">
            <div class="cl-tag-chips" *ngIf="selectedTags.length > 0">
              <span
                class="cl-tag-chip"
                *ngFor="let tag of selectedTags"
                (click)="removeTag(tag)">
                {{ tag }} <span class="cl-tag-remove">×</span>
              </span>
            </div>
            <select
              class="cl-select cl-select-tag"
              (change)="addTag($event)"
              [disabled]="availableTags.length === 0">
              <option value="">{{ 'clients.list.tagFilterPlaceholder' | transloco }}</option>
              <option
                *ngFor="let tag of availableTags"
                [value]="tag"
                [disabled]="selectedTags.includes(tag)">
                {{ tag }}
              </option>
            </select>
          </div>
        </div>

        <!-- Sort controls -->
        <div class="cl-filter-group cl-sort-group">
          <label class="cl-filter-label">Sort</label>
          <div class="cl-sort-row">
            <select class="cl-select" [(ngModel)]="sortField" (change)="applyFilters()">
              <option value="fullName">Name</option>
              <option value="createdAt">Created</option>
            </select>
            <button
              class="cl-sort-dir"
              type="button"
              (click)="toggleSortDir()"
              [attr.aria-label]="sortDir === 'asc' ? 'Sort ascending' : 'Sort descending'">
              <svg *ngIf="sortDir === 'asc'" width="14" height="14" viewBox="0 0 24 24"
                   fill="none" stroke="currentColor" stroke-width="2.5">
                <path d="M12 19V5M5 12l7-7 7 7"/>
              </svg>
              <svg *ngIf="sortDir === 'desc'" width="14" height="14" viewBox="0 0 24 24"
                   fill="none" stroke="currentColor" stroke-width="2.5">
                <path d="M12 5v14M5 12l7 7 7-7"/>
              </svg>
            </button>
          </div>
        </div>

      </div>

      <!-- State messages -->
      <div *ngIf="loading" class="cl-state" aria-live="polite">
        <span class="cl-spinner"></span>
        {{ 'clients.list.loading' | transloco }}
      </div>
      <div *ngIf="!loading && loadError" class="cl-alert" role="alert">
        {{ loadError }}
      </div>
      <div *ngIf="!loading && !loadError && clients.length === 0" class="cl-state">
        {{ 'clients.list.noClients' | transloco }}
      </div>

      <!-- Table -->
      <div class="cl-table-wrap" *ngIf="!loading && !loadError && clients.length > 0">
        <table [attr.aria-label]="'clients.list.ariaList' | transloco">
          <thead>
            <tr>
              <th scope="col" class="col-name">
                <button class="cl-sort-btn" (click)="sortByField('fullName')" type="button"
                        [class.active]="sortField === 'fullName'"
                        [attr.aria-sort]="ariaSort('fullName')">
                  {{ 'clients.list.tableHeaders.name' | transloco }}
                  <span class="cl-sort-indicator" *ngIf="sortField === 'fullName'">
                    {{ sortDir === 'asc' ? '↑' : '↓' }}
                  </span>
                </button>
              </th>
              <th scope="col" class="col-contact">{{ 'clients.list.tableHeaders.contact' | transloco }}</th>
              <th scope="col" class="col-city">{{ 'clients.list.tableHeaders.city' | transloco }}</th>
              <th scope="col" class="col-therapist">{{ 'clients.list.tableHeaders.therapist' | transloco }}</th>
              <th scope="col" class="col-tags">{{ 'clients.list.tableHeaders.tags' | transloco }}</th>
              <th scope="col" class="col-created">
                <button class="cl-sort-btn" (click)="sortByField('createdAt')" type="button"
                        [class.active]="sortField === 'createdAt'"
                        [attr.aria-sort]="ariaSort('createdAt')">
                  {{ 'clients.list.tableHeaders.created' | transloco }}
                  <span class="cl-sort-indicator" *ngIf="sortField === 'createdAt'">
                    {{ sortDir === 'asc' ? '↑' : '↓' }}
                  </span>
                </button>
              </th>
              <th scope="col" class="col-actions">
                <span class="sr-only">{{ 'clients.list.tableHeaders.actions' | transloco }}</span>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr
              *ngFor="let client of clients"
              class="cl-row"
              (click)="openClient(client)"
              [attr.aria-label]="'Open ' + client.fullName"
              tabindex="0"
              (keydown.enter)="openClient(client)"
              (keydown.space)="openClient(client)">
              <!-- Name + Code -->
              <td class="col-name">
                <div class="cl-name-cell">
                  <span class="cl-avatar" [style.background]="avatarColor(client.fullName)">
                    {{ avatarInitial(client.fullName) }}
                  </span>
                  <div class="cl-name-info">
                    <span class="cl-name">{{ client.fullName }}</span>
                    <span class="cl-code" *ngIf="client.clientCode">{{ client.clientCode }}</span>
                  </div>
                </div>
              </td>
              <!-- Contact -->
              <td class="col-contact">
                <div class="cl-contact-stack">
                  <span class="cl-contact-line" *ngIf="client.email">
                    <svg width="11" height="11" viewBox="0 0 24 24" fill="none"
                         stroke="currentColor" stroke-width="2">
                      <rect x="2" y="4" width="20" height="16" rx="2"/>
                      <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/>
                    </svg>
                    {{ client.email }}
                  </span>
                  <span class="cl-contact-line" *ngIf="client.phone">
                    <svg width="11" height="11" viewBox="0 0 24 24" fill="none"
                         stroke="currentColor" stroke-width="2">
                      <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.69 12a19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 3.6 1.19h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L7.91 8.76a16 16 0 0 0 6 6l.94-.94a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 21.73 16z"/>
                    </svg>
                    {{ client.phone }}
                  </span>
                  <span class="cl-empty" *ngIf="!client.email && !client.phone">—</span>
                </div>
              </td>
              <!-- City -->
              <td class="col-city">{{ client.city || '—' }}</td>
              <!-- Therapist -->
              <td class="col-therapist">
                <span *ngIf="client.assignedTherapistId">
                  {{ therapistName(client.assignedTherapistId) }}
                </span>
                <span class="cl-empty" *ngIf="!client.assignedTherapistId">—</span>
              </td>
              <!-- Tags -->
              <td class="col-tags">
                <span class="cl-tags-wrap" *ngIf="client.tags.length > 0">
                  <span class="cl-tag" *ngFor="let tag of client.tags.slice(0, 3)">{{ tag }}</span>
                  <span class="cl-tag cl-tag-more" *ngIf="client.tags.length > 3">
                    +{{ client.tags.length - 3 }}
                  </span>
                </span>
                <span class="cl-empty" *ngIf="client.tags.length === 0">—</span>
              </td>
              <!-- Created -->
              <td class="col-created">{{ client.createdAt | date:'dd MMM yyyy' }}</td>
              <!-- Open action -->
              <td class="col-actions">
                <svg class="cl-row-arrow" width="16" height="16" viewBox="0 0 24 24"
                     fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M9 18l6-6-6-6"/>
                </svg>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div class="cl-pagination" *ngIf="!loading && totalPages > 1"
           [attr.aria-label]="'clients.list.ariaPagination' | transloco">
        <button
          class="cl-page-btn"
          (click)="goToPage(currentPage - 1)"
          [disabled]="currentPage === 0"
          aria-label="Previous page">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
               stroke="currentColor" stroke-width="2.5">
            <path d="M15 18l-6-6 6-6"/>
          </svg>
        </button>
        <div class="cl-page-numbers">
          <button
            *ngFor="let p of pageNumbers()"
            class="cl-page-num"
            [class.active]="p === currentPage"
            [disabled]="p < 0"
            (click)="p >= 0 && goToPage(p)">
            {{ p < 0 ? '…' : p + 1 }}
          </button>
        </div>
        <button
          class="cl-page-btn"
          (click)="goToPage(currentPage + 1)"
          [disabled]="currentPage >= totalPages - 1"
          aria-label="Next page">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
               stroke="currentColor" stroke-width="2.5">
            <path d="M9 18l6-6-6-6"/>
          </svg>
        </button>
      </div>

    </div>
  `,
  styles: [`
    /* ─── Layout ────────────────────────────────────────────────────── */
    .cl-page {
      padding: var(--spacing-xl, 2rem);
      max-width: 1280px;
      margin: 0 auto;
      font-family: 'DM Sans', 'Outfit', system-ui, sans-serif;
      color: var(--color-text-primary, #111827);
    }

    /* ─── Header ─────────────────────────────────────────────────────── */
    .cl-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: var(--spacing-xl, 2rem);
    }
    .cl-header-left {
      display: flex;
      align-items: baseline;
      gap: 0.75rem;
    }
    .cl-title {
      margin: 0;
      font-size: 1.625rem;
      font-weight: 700;
      letter-spacing: -0.03em;
      color: var(--color-text-primary, #0F172A);
    }
    .cl-count {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 1.75rem;
      height: 1.375rem;
      padding: 0 .5rem;
      background: #E0F2FE;
      color: #0369A1;
      font-size: .75rem;
      font-weight: 600;
      border-radius: 999px;
    }

    /* ─── Filter bar ─────────────────────────────────────────────────── */
    .cl-filters {
      display: flex;
      gap: 1rem;
      margin-bottom: 1.5rem;
      align-items: flex-end;
      flex-wrap: wrap;
    }
    .cl-filter-group {
      display: flex;
      flex-direction: column;
      gap: .3rem;
    }
    .cl-filter-label {
      font-size: .75rem;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: .05em;
      color: var(--color-text-secondary, #6B7280);
    }
    .cl-search-wrap {
      position: relative;
      flex: 1;
      min-width: 220px;
    }
    .cl-search-icon {
      position: absolute;
      left: .75rem;
      top: 50%;
      transform: translateY(-50%);
      color: var(--color-text-secondary, #9CA3AF);
      pointer-events: none;
      display: flex;
      align-items: center;
    }
    .cl-search {
      width: 100%;
      padding: .575rem .875rem .575rem 2.375rem;
      border: 1.5px solid var(--color-border, #E5E7EB);
      border-radius: var(--radius-md, 8px);
      font-size: .9375rem;
      font-family: inherit;
      color: inherit;
      background: #fff;
      outline: none;
      transition: border-color .15s ease, box-shadow .15s ease;
      box-sizing: border-box;
    }
    .cl-search:focus {
      border-color: #0EA5A0;
      box-shadow: 0 0 0 3px rgba(14,165,160,.12);
    }
    .cl-select {
      appearance: none;
      padding: .575rem 2rem .575rem .875rem;
      border: 1.5px solid var(--color-border, #E5E7EB);
      border-radius: var(--radius-md, 8px);
      font-size: .875rem;
      font-family: inherit;
      color: var(--color-text-primary, #111827);
      background: #fff url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%236B7280' stroke-width='2.5'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E") no-repeat right .65rem center;
      outline: none;
      cursor: pointer;
      transition: border-color .15s ease;
    }
    .cl-select:focus { border-color: #0EA5A0; }
    .cl-select:disabled { opacity: .45; cursor: default; }
    .cl-select-tag { padding-right: 2rem; max-width: 200px; }

    /* Tag filter */
    .cl-tag-filter { display: flex; flex-direction: column; gap: .35rem; }
    .cl-tag-chips { display: flex; flex-wrap: wrap; gap: .3rem; }
    .cl-tag-chip {
      display: inline-flex;
      align-items: center;
      gap: .25rem;
      padding: .2rem .55rem;
      background: #F0FDFA;
      border: 1px solid #99F6E4;
      color: #0D9488;
      border-radius: 999px;
      font-size: .75rem;
      font-weight: 500;
      cursor: pointer;
      transition: background .12s ease;
    }
    .cl-tag-chip:hover { background: #CCFBF1; }
    .cl-tag-remove { font-size: .875rem; margin-left: .1rem; opacity: .7; }

    /* Sort row */
    .cl-sort-row { display: flex; align-items: center; gap: .4rem; }
    .cl-sort-dir {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 2rem;
      height: 2rem;
      border: 1.5px solid var(--color-border, #E5E7EB);
      border-radius: var(--radius-md, 8px);
      background: #fff;
      color: #374151;
      cursor: pointer;
      transition: background .12s ease, border-color .12s ease;
    }
    .cl-sort-dir:hover { background: #F9FAFB; border-color: #9CA3AF; }

    /* ─── State messages ─────────────────────────────────────────────── */
    .cl-state {
      display: flex;
      align-items: center;
      gap: .75rem;
      padding: 3rem;
      text-align: center;
      justify-content: center;
      color: var(--color-text-secondary, #6B7280);
      font-size: .9375rem;
    }
    .cl-alert {
      padding: .875rem 1.125rem;
      background: #FEF2F2;
      border: 1px solid #FECACA;
      border-radius: var(--radius-md, 8px);
      color: #DC2626;
      font-size: .875rem;
      margin-bottom: 1rem;
    }
    .cl-spinner {
      display: inline-block;
      width: 1rem;
      height: 1rem;
      border: 2px solid #E5E7EB;
      border-top-color: #0EA5A0;
      border-radius: 50%;
      animation: cl-spin .7s linear infinite;
    }
    @keyframes cl-spin { to { transform: rotate(360deg); } }

    /* ─── Table ──────────────────────────────────────────────────────── */
    .cl-table-wrap {
      overflow-x: auto;
      border: 1px solid var(--color-border, #E5E7EB);
      border-radius: var(--radius-lg, 12px);
      box-shadow: 0 1px 3px rgba(0,0,0,.06);
    }
    table {
      width: 100%;
      border-collapse: collapse;
      font-size: .9rem;
    }
    thead {
      background: #F8FAFC;
      border-bottom: 1.5px solid var(--color-border, #E5E7EB);
    }
    th {
      padding: .7rem 1rem;
      text-align: left;
      font-size: .75rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: .04em;
      color: var(--color-text-secondary, #6B7280);
      white-space: nowrap;
    }
    .cl-sort-btn {
      background: none;
      border: none;
      cursor: pointer;
      font-weight: 700;
      font-size: inherit;
      text-transform: uppercase;
      letter-spacing: inherit;
      padding: 0;
      color: var(--color-text-secondary, #6B7280);
      display: flex;
      align-items: center;
      gap: .25rem;
      transition: color .12s ease;
    }
    .cl-sort-btn:hover, .cl-sort-btn.active { color: #0D9488; }
    .cl-sort-indicator { font-size: .8rem; }
    td {
      padding: .875rem 1rem;
      border-bottom: 1px solid #F1F5F9;
      vertical-align: middle;
    }
    .cl-row {
      cursor: pointer;
      transition: background .1s ease;
      outline: none;
    }
    .cl-row:hover { background: #F8FAFC; }
    .cl-row:focus-visible { outline: 2px solid #0EA5A0; outline-offset: -2px; }
    tbody tr:last-child td { border-bottom: none; }

    /* Name cell */
    .cl-name-cell {
      display: flex;
      align-items: center;
      gap: .75rem;
    }
    .cl-avatar {
      flex-shrink: 0;
      width: 2rem;
      height: 2rem;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: .8125rem;
      font-weight: 700;
      color: #fff;
      text-transform: uppercase;
      letter-spacing: .03em;
    }
    .cl-name-info {
      display: flex;
      flex-direction: column;
      gap: .1rem;
    }
    .cl-name {
      font-weight: 600;
      color: var(--color-text-primary, #0F172A);
      font-size: .9375rem;
    }
    .cl-code {
      font-size: .75rem;
      color: var(--color-text-secondary, #9CA3AF);
      font-family: 'JetBrains Mono', 'Fira Code', monospace;
    }

    /* Contact */
    .cl-contact-stack {
      display: flex;
      flex-direction: column;
      gap: .2rem;
    }
    .cl-contact-line {
      display: flex;
      align-items: center;
      gap: .3rem;
      font-size: .8375rem;
      color: var(--color-text-secondary, #4B5563);
    }
    .cl-contact-line svg { flex-shrink: 0; opacity: .7; }
    .cl-empty { color: #CBD5E1; font-size: .875rem; }

    /* Tags */
    .cl-tags-wrap {
      display: flex;
      flex-wrap: wrap;
      gap: .25rem;
    }
    .cl-tag {
      display: inline-block;
      padding: .175rem .525rem;
      background: #F0F9FF;
      border: 1px solid #BAE6FD;
      color: #0369A1;
      border-radius: 999px;
      font-size: .73rem;
      font-weight: 500;
      white-space: nowrap;
    }
    .cl-tag-more {
      background: #F1F5F9;
      border-color: #CBD5E1;
      color: #64748B;
    }

    /* Row arrow */
    .cl-row-arrow {
      color: #CBD5E1;
      transition: color .1s ease, transform .1s ease;
    }
    .cl-row:hover .cl-row-arrow {
      color: #0EA5A0;
      transform: translateX(2px);
    }

    /* Column widths */
    .col-name { min-width: 200px; }
    .col-contact { min-width: 170px; }
    .col-city { min-width: 100px; }
    .col-therapist { min-width: 130px; }
    .col-tags { min-width: 140px; }
    .col-created { white-space: nowrap; min-width: 110px; }
    .col-actions { width: 40px; text-align: center; }

    /* ─── Pagination ─────────────────────────────────────────────────── */
    .cl-pagination {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: .4rem;
      margin-top: 1.5rem;
    }
    .cl-page-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 2.125rem;
      height: 2.125rem;
      border: 1.5px solid var(--color-border, #E5E7EB);
      border-radius: var(--radius-md, 8px);
      background: #fff;
      color: #374151;
      cursor: pointer;
      transition: background .12s ease, border-color .12s ease;
    }
    .cl-page-btn:hover:not(:disabled) { background: #F9FAFB; border-color: #9CA3AF; }
    .cl-page-btn:disabled { opacity: .3; cursor: not-allowed; }
    .cl-page-numbers {
      display: flex;
      align-items: center;
      gap: .25rem;
    }
    .cl-page-num {
      display: flex;
      align-items: center;
      justify-content: center;
      min-width: 2.125rem;
      height: 2.125rem;
      border: 1.5px solid transparent;
      border-radius: var(--radius-md, 8px);
      background: none;
      font-size: .875rem;
      font-family: inherit;
      color: #374151;
      cursor: pointer;
      transition: background .12s ease, color .12s ease, border-color .12s ease;
    }
    .cl-page-num:hover:not([disabled]):not(.active) {
      background: #F9FAFB;
      border-color: #E5E7EB;
    }
    .cl-page-num.active {
      background: #0EA5A0;
      color: #fff;
      border-color: #0EA5A0;
      font-weight: 700;
      cursor: default;
    }
    .cl-page-num[disabled] {
      cursor: default;
      pointer-events: none;
      color: #9CA3AF;
    }

    .sr-only {
      position: absolute; width: 1px; height: 1px;
      padding: 0; margin: -1px; overflow: hidden;
      clip: rect(0,0,0,0); white-space: nowrap; border: 0;
    }
  `]
})
export class ClientListComponent implements OnInit, OnDestroy {

  clients: ClientListItem[] = [];
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  readonly pageSize = 20;

  sortField = 'fullName';
  sortDir = 'asc';

  searchQuery = '';
  selectedTherapistId = '';
  selectedTags: string[] = [];
  availableTags: string[] = [];
  therapists: TherapistProfile[] = [];

  loading = false;
  loadError: string | null = null;

  private readonly searchSubject = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  /** Palette for avatar colours — deterministic per name. */
  private readonly AVATAR_COLORS = [
    '#0D9488', '#0369A1', '#6D28D9', '#B45309',
    '#047857', '#BE185D', '#1D4ED8', '#7C3AED',
  ];

  constructor(
    private readonly clientService: ClientService,
    private readonly therapistService: TherapistManagementService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    // Debounce text search
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$),
    ).subscribe(() => {
      this.currentPage = 0;
      this.loadClients();
    });

    this.loadClients();
    this.loadTags();
    this.loadTherapists();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  onSearchChange(value: string): void {
    this.searchSubject.next(value);
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.loadClients();
  }

  toggleSortDir(): void {
    this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    this.currentPage = 0;
    this.loadClients();
  }

  sortByField(field: string): void {
    if (this.sortField === field) {
      this.toggleSortDir();
    } else {
      this.sortField = field;
      this.sortDir = 'asc';
      this.currentPage = 0;
      this.loadClients();
    }
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.loadClients();
  }

  addTag(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const tag = select.value;
    if (tag && !this.selectedTags.includes(tag)) {
      this.selectedTags = [...this.selectedTags, tag];
      this.applyFilters();
    }
    select.value = '';
  }

  removeTag(tag: string): void {
    this.selectedTags = this.selectedTags.filter(t => t !== tag);
    this.applyFilters();
  }

  openClient(client: ClientListItem): void {
    this.router.navigate(['/clients', client.id]);
  }

  ariaSort(field: string): string {
    if (this.sortField !== field) return 'none';
    return this.sortDir === 'asc' ? 'ascending' : 'descending';
  }

  avatarInitial(name: string): string {
    return name ? name.trim().charAt(0).toUpperCase() : '?';
  }

  avatarColor(name: string): string {
    if (!name) return this.AVATAR_COLORS[0];
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
      hash = (hash << 5) - hash + name.charCodeAt(i);
      hash |= 0;
    }
    return this.AVATAR_COLORS[Math.abs(hash) % this.AVATAR_COLORS.length];
  }

  therapistName(id: string): string {
    return this.therapists.find(t => t.id === id)?.name ?? id.substring(0, 8) + '…';
  }

  /** Build a compact page-number list: first, last, current ±1, with ellipsis gaps. */
  pageNumbers(): number[] {
    if (this.totalPages <= 7) {
      return Array.from({ length: this.totalPages }, (_, i) => i);
    }
    const p = this.currentPage;
    const last = this.totalPages - 1;
    const pages = new Set<number>([0, last, p]);
    if (p > 0) pages.add(p - 1);
    if (p < last) pages.add(p + 1);
    const sorted = [...pages].sort((a, b) => a - b);
    const result: number[] = [];
    for (let i = 0; i < sorted.length; i++) {
      if (i > 0 && sorted[i] - sorted[i - 1] > 1) result.push(-1); // ellipsis sentinel
      result.push(sorted[i]);
    }
    return result;
  }

  private loadClients(): void {
    this.loading = true;
    this.loadError = null;
    const therapistId = this.selectedTherapistId || undefined;
    const q = this.searchQuery.trim() || undefined;
    this.clientService.listClients(
      this.currentPage,
      this.pageSize,
      this.sortField,
      this.sortDir,
      q,
      this.selectedTags.length > 0 ? this.selectedTags : undefined,
      therapistId,
    ).subscribe({
      next: page => {
        this.clients = page.content;
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
        this.loading = false;
      },
      error: () => {
        this.loadError = 'clients.list.error';
        this.loading = false;
      },
    });
  }

  private loadTags(): void {
    this.clientService.getAllTags().subscribe({
      next: tags => { this.availableTags = tags; },
      error: () => { /* non-critical */ },
    });
  }

  private loadTherapists(): void {
    this.therapistService.getTherapists(0, 100, undefined, true).subscribe({
      next: page => { this.therapists = page.content; },
      error: () => { /* non-critical */ },
    });
  }
}
