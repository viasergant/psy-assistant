import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { TranslocoModule } from '@jsverse/transloco';
import {
  ClientDetail,
  UpdateClientProfilePayload,
  UpdateClientTagsPayload,
} from '../models/client.model';
import { ClientService } from '../services/client.service';
import { ClientTimelineComponent } from '../components/timeline/client-timeline.component';
import { AuthService } from '../../../core/auth/auth.service';
import { getCurrentTherapistProfileId, getCurrentUserRole } from '../../schedule/guards/schedule.guard';
import { AppointmentBookingDialogComponent } from '../../schedule/components/appointment-booking-dialog/appointment-booking-dialog.component';
import { TherapistManagementService } from '../../admin/therapists/services/therapist-management.service';
import { TherapistProfile } from '../../admin/therapists/models/therapist.model';
import { CarePlanListComponent } from '../care-plans/care-plan-list/care-plan-list.component';

/**
 * Client profile page for PA-23 slice-one read and update flow.
 */
@Component({
  selector: 'app-client-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule, FormsModule, TranslocoModule, ClientTimelineComponent, AppointmentBookingDialogComponent, CarePlanListComponent],
  template: `
    <div class="page">
      <a class="back-link" routerLink="/leads">&larr; Back to leads</a>

      <div *ngIf="loading" class="state-msg" aria-live="polite">Loading client…</div>

      <div *ngIf="loadError" class="alert-error" role="alert">{{ loadError }}</div>

      <div *ngIf="client && !loading">
        <header class="page-header">
          <div>
            <h1>{{ client.fullName }}</h1>
            <p class="sub">{{ client.clientCode || client.id }}</p>
          </div>
          <span class="badge-client">{{ 'clients.detail.badge' | transloco }}</span>
        </header>

        <div class="toolbar">
          <div *ngIf="!editing" class="edit-actions">
            <ng-container *ngIf="needsTherapistPicker">
              <select class="therapist-select"
                      [(ngModel)]="selectedAdminTherapistId"
                      [attr.aria-label]="'clients.detail.selectTherapist' | transloco">
                <option value="">{{ 'clients.detail.selectTherapistPlaceholder' | transloco }}</option>
                <option *ngFor="let t of availableTherapists" [value]="t.id">{{ t.name }}</option>
              </select>
            </ng-container>
            <button *ngIf="canBook"
                    type="button"
                    class="btn-ghost"
                    (click)="openBookingDialog()">
              {{ 'clients.detail.bookAppointment' | transloco }}
            </button>
            <button *ngIf="client.canEditProfile"
                    type="button"
                    class="btn-primary"
                    (click)="startEdit()">
              {{ 'clients.detail.editProfile' | transloco }}
            </button>
          </div>

          <div *ngIf="editing" class="edit-actions">
            <button type="button" class="btn-ghost" (click)="cancelEdit()">{{ 'clients.detail.cancel' | transloco }}</button>
            <button type="button" class="btn-primary" [disabled]="saving || profileForm.invalid"
                    (click)="save()">
              {{ saving ? ('clients.detail.saving' | transloco) : ('clients.detail.saveChanges' | transloco) }}
            </button>
          </div>
        </div>

        <div *ngIf="saveError" class="alert-error" role="alert">{{ saveError }}</div>

        <form [formGroup]="profileForm" class="profile-form">
          <section class="section">
            <h2 class="section-title">{{ 'clients.detail.sections.basicInfo' | transloco }}</h2>

            <div class="grid two">
              <label class="field">
                <span>Full name</span>
                <input type="text" formControlName="fullName" [readonly]="!editing" />
              </label>

              <label class="field">
                <span>{{ 'clients.detail.fields.preferredName' | transloco }}</span>
                <input type="text" formControlName="preferredName" [readonly]="!editing" />
              </label>

              <label class="field">
                <span>{{ 'clients.detail.fields.dateOfBirth' | transloco }}</span>
                <input type="date" formControlName="dateOfBirth" [readonly]="!editing" />
              </label>

              <label class="field">
                <span>{{ 'clients.detail.fields.pronouns' | transloco }}</span>
                <input type="text" formControlName="pronouns" [readonly]="!editing" />
              </label>

              <label class="field">
                <span>{{ 'clients.detail.fields.gender' | transloco }}</span>
                <input type="text" formControlName="sexOrGender" [readonly]="!editing" />
              </label>
            </div>
          </section>

          <section class="section">
            <h2 class="section-title">{{ 'clients.detail.sections.contactDetails' | transloco }}</h2>

            <div class="grid two">
              <label class="field">
                <span>{{ 'clients.detail.fields.email' | transloco }}</span>
                <input type="email" formControlName="email" [readonly]="!editing" />
              </label>

              <label class="field">
                <span>{{ 'clients.detail.fields.phone' | transloco }}</span>
                <input type="text" formControlName="phone" [readonly]="!editing" />
              </label>

              <label class="field">
                <span>{{ 'clients.detail.fields.secondaryPhone' | transloco }}</span>
                <input type="text" formControlName="secondaryPhone" [readonly]="!editing" />
              </label>

              <label class="field">
                <span>{{ 'clients.detail.fields.addressLine1' | transloco }}</span>
                <input type="text" formControlName="addressLine1" [readonly]="!editing" />
              </label>

              <label class="field">
                <span>{{ 'clients.detail.fields.addressLine2' | transloco }}</span>
                <input type="text" formControlName="addressLine2" [readonly]="!editing" />
              </label>

              <label class="field">
                <span>{{ 'clients.detail.fields.city' | transloco }}</span>
                <input type="text" formControlName="city" [readonly]="!editing" />
              </label>

              <label class="field">
                <span>{{ 'clients.detail.fields.region' | transloco }}</span>
                <input type="text" formControlName="region" [readonly]="!editing" />
              </label>

              <label class="field">
                <span>{{ 'clients.detail.fields.postalCode' | transloco }}</span>
                <input type="text" formControlName="postalCode" [readonly]="!editing" />
              </label>

              <label class="field">
                <span>{{ 'clients.detail.fields.country' | transloco }}</span>
                <input type="text" formControlName="country" [readonly]="!editing" />
              </label>
            </div>
          </section>

          <section class="section">
            <h2 class="section-title">{{ 'clients.detail.sections.referral' | transloco }}</h2>

            <div class="grid two">
              <label class="field">
                <span>{{ 'clients.detail.fields.referralSource' | transloco }}</span>
                <input type="text" formControlName="referralSource" [readonly]="!editing" />
              </label>

              <label class="field">
                <span>{{ 'clients.detail.fields.referralContact' | transloco }}</span>
                <input type="text" formControlName="referralContactName" [readonly]="!editing" />
              </label>
            </div>

            <label class="field">
              <span>{{ 'clients.detail.fields.referralNotes' | transloco }}</span>
              <textarea rows="3" formControlName="referralNotes" [readonly]="!editing"></textarea>
            </label>
          </section>

          <section class="section">
            <h2 class="section-title">{{ 'clients.detail.sections.emergencyContact' | transloco }}</h2>

            <div class="grid two">
              <label class="field">
                <span>{{ 'clients.detail.fields.emergencyName' | transloco }}</span>
                <input type="text" formControlName="emergencyContactName" [readonly]="!editing" />
              </label>

              <label class="field">
                <span>{{ 'clients.detail.fields.emergencyRelationship' | transloco }}</span>
                <input type="text" formControlName="emergencyContactRelationship" [readonly]="!editing" />
              </label>

              <label class="field">
                <span>{{ 'clients.detail.fields.emergencyPhone' | transloco }}</span>
                <input type="text" formControlName="emergencyContactPhone" [readonly]="!editing" />
              </label>

              <label class="field">
                <span>{{ 'clients.detail.fields.emergencyEmail' | transloco }}</span>
                <input type="email" formControlName="emergencyContactEmail" [readonly]="!editing" />
              </label>
            </div>
          </section>

          <section class="section">
            <h2 class="section-title">{{ 'clients.detail.sections.commPrefs' | transloco }}</h2>

            <div class="grid two">
              <label class="field">
                <span>{{ 'clients.detail.fields.preferredMethod' | transloco }}</span>
                <input type="text" formControlName="preferredCommunicationMethod"
                       [readonly]="!editing" />
              </label>

              <label class="toggle">
                <input type="checkbox" formControlName="allowPhone" [disabled]="!editing" />
                <span>{{ 'clients.detail.fields.allowPhone' | transloco }}</span>
              </label>

              <label class="toggle">
                <input type="checkbox" formControlName="allowSms" [disabled]="!editing" />
                <span>{{ 'clients.detail.fields.allowSms' | transloco }}</span>
              </label>

              <label class="toggle">
                <input type="checkbox" formControlName="allowEmail" [disabled]="!editing" />
                <span>{{ 'clients.detail.fields.allowEmail' | transloco }}</span>
              </label>

              <label class="toggle">
                <input type="checkbox" formControlName="allowVoicemail" [disabled]="!editing" />
                <span>{{ 'clients.detail.fields.allowVoicemail' | transloco }}</span>
              </label>
            </div>
          </section>

          <section class="section">
            <h2 class="section-title">{{ 'clients.detail.sections.tags' | transloco }}</h2>
            <div class="tags-wrap">
              <span *ngFor="let tag of client.tags" class="tag-pill">
                {{ tag }}
                <button *ngIf="client.canEditTags"
                        type="button"
                        class="tag-remove"
                        (click)="removeTag(tag)">
                  ×
                </button>
              </span>
            </div>

            <div *ngIf="client.canEditTags" class="tag-editor">
              <input
                type="text"
                [value]="tagInput"
                (input)="tagInput = $any($event.target).value"
                (keydown.enter)="addTag($event)"
                [placeholder]="'clients.detail.tags.addTagPlaceholder' | transloco"
              />
              <button type="button" class="btn-primary" [disabled]="tagsSaving" (click)="saveTags()">
                {{ tagsSaving ? ('clients.detail.tags.savingTags' | transloco) : ('clients.detail.tags.saveTags' | transloco) }}
              </button>
            </div>

            <div *ngIf="client.canEditTags && filteredTagHints.length" class="tag-hints">
              <span class="hint-label">{{ 'clients.detail.tags.suggestions' | transloco }}</span>
              <button
                *ngFor="let hint of filteredTagHints"
                type="button"
                class="hint-chip"
                (click)="applyTagHint(hint)">
                {{ hint }}
              </button>
            </div>

            <div *ngIf="tagsError" class="alert-error" role="alert">{{ tagsError }}</div>
            <p *ngIf="!client.tags.length" class="muted">No tags yet.</p>
          </section>

          <section class="section">
            <h2 class="section-title">{{ 'clients.detail.sections.photo' | transloco }}</h2>
            <div class="photo-block">
              <img *ngIf="photoSrc"
                   class="profile-photo"
                [src]="photoSrc"
                   [alt]="client.fullName + ' photo'" />
              <div *ngIf="!client.photoUrl || !photoSrc" class="photo-placeholder">No photo uploaded</div>
            </div>

            <div *ngIf="client.canUploadPhoto" class="photo-controls">
              <input
                #photoInput
                type="file"
                accept="image/jpeg,image/png,image/webp"
                (change)="onPhotoSelected($event)"
              />
              <button
                type="button"
                class="btn-primary"
                [disabled]="photoUploading"
                (click)="photoInput.click()">
                {{ photoUploading ? 'Uploading...' : 'Upload photo' }}
              </button>
            </div>

            <div *ngIf="photoError" class="alert-error" role="alert">{{ photoError }}</div>
          </section>

          <section class="section">
            <h2 class="section-title">{{ 'clients.detail.sections.notes' | transloco }}</h2>
            <label class="field">
              <span>{{ 'clients.detail.fields.notes' | transloco }}</span>
              <textarea rows="4" formControlName="notes" [readonly]="!editing"></textarea>
            </label>
          </section>
        </form>

        <div class="section" *ngIf="client.sourceLeadId">
          <h2 class="section-title">Referral Link</h2>
          <a [routerLink]="['/leads']" class="link">View source lead history</a>
        </div>

        <div class="section" *ngIf="client.notes">
          <h2 class="section-title">Pre-conversion history</h2>
          <div class="notes-block">{{ client.notes }}</div>
        </div>

        <div class="section">
          <app-client-timeline [clientId]="client.id" />
        </div>

        <div class="section">
          <app-care-plan-list [clientId]="client.id" />
        </div>
      </div>
    </div>

    <app-appointment-booking-dialog
      *ngIf="showBookingDialog && effectiveTherapistId"
      [therapistProfileId]="effectiveTherapistId!"
      [clients]="bookingClientList"
      (submitted)="onBookingSubmitted()"
      (cancelled)="onBookingCancelled()"
    />
  `,
  styles: [`
    .page { padding: 2rem; max-width: 980px; margin: 0 auto; }
    .sub { margin: .35rem 0 0; color: #64748B; font-size: .85rem; }
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
    .profile-form { display: block; }
    .grid.two {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: .85rem 1rem;
    }
    .field { display: flex; flex-direction: column; gap: .35rem; margin-bottom: .75rem; }
    .field span { font-size: .85rem; color: #64748B; }
    .field input, .field textarea {
      border: 1px solid #CBD5E1; border-radius: 8px; padding: .55rem .7rem;
      font: inherit; background: #fff;
    }
    .field input[readonly], .field textarea[readonly] {
      background: #F8FAFC;
    }
    .toggle { display: inline-flex; align-items: center; gap: .5rem; color: #1E293B; }
    .section-title {
      font-size: 1rem; font-weight: 600; color: #374151;
      border-bottom: 1.5px solid #E2E8F0; padding-bottom: .5rem; margin-bottom: 1rem;
    }
    .toolbar {
      display: flex; justify-content: flex-end; margin-bottom: 1rem;
    }
    .edit-actions { display: inline-flex; gap: .5rem; align-items: center; }
    .therapist-select {
      border: 1px solid #CBD5E1; border-radius: 8px; padding: .45rem .65rem;
      font: inherit; background: #fff; color: #1E293B; cursor: pointer;
    }
    .btn-primary, .btn-ghost {
      border-radius: 8px; padding: .5rem .85rem; border: 1px solid transparent;
      font-weight: 600; cursor: pointer;
    }
    .btn-primary { background: #0EA5A0; color: #fff; }
    .btn-primary:disabled { opacity: .7; cursor: default; }
    .btn-ghost { background: #fff; border-color: #CBD5E1; color: #334155; }
    .link { color: #0EA5A0; text-decoration: underline; }
    .muted { color: #64748B; margin: 0; }
    .tags-wrap { display: flex; gap: .5rem; flex-wrap: wrap; margin-bottom: .75rem; }
    .tag-pill {
      display: inline-flex; align-items: center; gap: .4rem;
      background: #ECFEFF; color: #155E75; border: 1px solid #A5F3FC;
      border-radius: 999px; padding: .22rem .6rem; font-size: .8rem;
    }
    .tag-remove {
      border: 0; background: transparent; color: #155E75; cursor: pointer;
      font-size: 1rem; line-height: 1;
    }
    .tag-editor { display: flex; gap: .5rem; align-items: center; margin-bottom: .75rem; }
    .tag-editor input {
      border: 1px solid #CBD5E1; border-radius: 8px; padding: .5rem .65rem;
      font: inherit; min-width: 280px;
    }
    .tag-hints {
      display: flex; align-items: center; flex-wrap: wrap; gap: .45rem;
      margin-bottom: .75rem;
    }
    .hint-label { color: #64748B; font-size: .8rem; }
    .hint-chip {
      border: 1px solid #BFDBFE; background: #EFF6FF; color: #1D4ED8;
      border-radius: 999px; padding: .2rem .55rem; font-size: .76rem;
      cursor: pointer;
    }
    .hint-chip:hover { background: #DBEAFE; }
    .photo-block { margin-bottom: .75rem; }
    .profile-photo {
      width: 120px; height: 120px; object-fit: cover;
      border-radius: 16px; border: 1px solid #E2E8F0;
    }
    .photo-placeholder {
      width: 120px; height: 120px; display: grid; place-items: center;
      border: 1px dashed #CBD5E1; border-radius: 16px; color: #64748B;
      font-size: .8rem; text-align: center; padding: .4rem;
    }
    .photo-controls { display: flex; align-items: center; gap: .6rem; }
    .photo-controls input[type="file"] { display: none; }
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
export class ClientDetailComponent implements OnInit, OnDestroy {
  private static readonly PHOTO_MAX_BYTES = 5 * 1024 * 1024;
  private static readonly TAG_HINT_STORAGE_KEY = 'clients.recent-tags';
  private static readonly DEFAULT_TAG_HINTS = [
    'priority',
    'follow-up',
    'anxiety',
    'depression',
    'family',
    'adolescent',
    'trauma',
    'stress',
  ];

  client: ClientDetail | null = null;
  loading = false;
  saving = false;
  editing = false;
  tagInput = '';
  tagsSaving = false;
  photoUploading = false;
  photoSrc: string | null = null;
  recentTagHints: string[] = [];
  tagsError: string | null = null;
  photoError: string | null = null;
  loadError: string | null = null;
  saveError: string | null = null;
  showBookingDialog = false;
  therapistProfileId: string | null = null;
  availableTherapists: TherapistProfile[] = [];
  selectedAdminTherapistId = '';
  private _userRole: string | null = null;
  private photoObjectUrl: string | null = null;

  profileForm: FormGroup;

  constructor(
    private route: ActivatedRoute,
    private clientService: ClientService,
    private fb: FormBuilder,
    private authService: AuthService,
    private therapistManagementService: TherapistManagementService
  ) {
    this.profileForm = this.fb.group({
      fullName: ['', [Validators.required]],
      preferredName: [''],
      dateOfBirth: [''],
      sexOrGender: [''],
      pronouns: [''],
      ownerId: [''],
      assignedTherapistId: [''],
      notes: [''],
      email: [''],
      phone: [''],
      secondaryPhone: [''],
      addressLine1: [''],
      addressLine2: [''],
      city: [''],
      region: [''],
      postalCode: [''],
      country: [''],
      referralSource: [''],
      referralContactName: [''],
      referralNotes: [''],
      preferredCommunicationMethod: [''],
      allowPhone: [false],
      allowSms: [false],
      allowEmail: [false],
      allowVoicemail: [false],
      emergencyContactName: [''],
      emergencyContactRelationship: [''],
      emergencyContactPhone: [''],
      emergencyContactEmail: ['']
    });
  }

  ngOnInit(): void {
    this.therapistProfileId = getCurrentTherapistProfileId(this.authService);
    this._userRole = getCurrentUserRole(this.authService);
    this.recentTagHints = this.loadRecentTagHints();

    if (this.needsTherapistPicker) {
      this.therapistManagementService.getTherapists(0, 100, undefined, true).subscribe({
        next: (page) => { this.availableTherapists = page.content; },
        error: () => { /* non-critical, picker stays empty */ }
      });
    }

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
        this.patchForm(c);
        this.refreshPhotoSource(c);
      },
      error: () => {
        this.loading = false;
        this.loadError = 'Failed to load client. Please try again.';
      }
    });
  }

  ngOnDestroy(): void {
    this.releasePhotoObjectUrl();
  }

  startEdit(): void {
    this.editing = true;
    this.saveError = null;
  }

  cancelEdit(): void {
    this.editing = false;
    this.saveError = null;
    if (this.client) {
      this.patchForm(this.client);
    }
  }

  get needsTherapistPicker(): boolean {
    const role = this._userRole;
    return !this.therapistProfileId &&
      (role === 'SYSTEM_ADMINISTRATOR' || role === 'RECEPTION_ADMIN_STAFF');
  }

  get effectiveTherapistId(): string | null {
    if (this.therapistProfileId) return this.therapistProfileId;
    if (this.needsTherapistPicker) return this.selectedAdminTherapistId || null;
    return this.client?.assignedTherapistId || null;
  }

  get canBook(): boolean {
    return !!this.effectiveTherapistId;
  }

  get bookingClientList(): Array<{id: string; name: string}> {
    if (!this.client) return [];
    return [{ id: this.client.id, name: this.client.fullName }];
  }

  openBookingDialog(): void {
    this.showBookingDialog = true;
  }

  onBookingSubmitted(): void {
    this.showBookingDialog = false;
  }

  onBookingCancelled(): void {
    this.showBookingDialog = false;
  }

  save(): void {
    if (!this.client || this.profileForm.invalid || this.saving) {
      return;
    }

    this.saving = true;
    this.saveError = null;
    const payload = this.toPayload(this.client.version);

    this.clientService.updateClient(this.client.id, payload).subscribe({
      next: (updated) => {
        this.saving = false;
        this.editing = false;
        this.client = updated;
        this.patchForm(updated);
      },
      error: (err) => {
        this.saving = false;
        if (err?.status === 409) {
          this.saveError = 'This profile was updated elsewhere. Reload and try again.';
        } else {
          this.saveError = 'Failed to save profile changes. Please try again.';
        }
      }
    });
  }

  addTag(event?: Event): void {
    if (event) {
      event.preventDefault();
    }
    if (!this.client || !this.client.canEditTags) {
      return;
    }

    const next = this.tagInput.trim();
    if (!next) {
      return;
    }

    if (!this.hasTag(next)) {
      this.client = { ...this.client, tags: [...this.client.tags, next] };
    }
    this.tagInput = '';
    this.tagsError = null;
  }

  applyTagHint(hint: string): void {
    this.tagInput = hint;
    this.addTag();
  }

  removeTag(tag: string): void {
    if (!this.client || !this.client.canEditTags) {
      return;
    }

    this.client = { ...this.client, tags: this.client.tags.filter((value) => value !== tag) };
    this.tagsError = null;
  }

  saveTags(): void {
    if (!this.client || this.tagsSaving || !this.client.canEditTags) {
      return;
    }

    this.tagsSaving = true;
    this.tagsError = null;
    const payload: UpdateClientTagsPayload = {
      version: this.client.version,
      tags: this.client.tags,
    };

    this.clientService.updateTags(this.client.id, payload).subscribe({
      next: (updated) => {
        this.tagsSaving = false;
        this.client = updated;
        this.persistRecentTagHints(updated.tags);
      },
      error: (err) => {
        this.tagsSaving = false;
        this.tagsError = err?.status === 409
          ? 'Tags were updated elsewhere. Refresh and try again.'
          : 'Failed to save tags. Please try again.';
      },
    });
  }

  get filteredTagHints(): string[] {
    if (!this.client) {
      return [];
    }

    const query = this.tagInput.trim().toLowerCase();
    const existing = new Set(this.client.tags.map((tag) => tag.toLowerCase()));
    const pool = [
      ...ClientDetailComponent.DEFAULT_TAG_HINTS,
      ...this.recentTagHints,
      ...this.client.tags,
    ];

    const unique: string[] = [];
    const seen = new Set<string>();
    for (const candidate of pool) {
      const value = candidate.trim();
      const key = value.toLowerCase();
      if (!value || seen.has(key) || existing.has(key)) {
        continue;
      }
      if (query && !key.includes(query)) {
        continue;
      }
      seen.add(key);
      unique.push(value);
      if (unique.length >= 8) {
        break;
      }
    }
    return unique;
  }

  onPhotoSelected(event: Event): void {
    if (!this.client || this.photoUploading || !this.client.canUploadPhoto) {
      return;
    }

    const target = event.target as HTMLInputElement;
    const file = target.files?.[0];
    if (!file) {
      return;
    }

    if (file.size > ClientDetailComponent.PHOTO_MAX_BYTES) {
      this.photoError = 'Photo must be 5 MB or smaller.';
      target.value = '';
      return;
    }
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
      this.photoError = 'Only JPEG, PNG, or WEBP images are allowed.';
      target.value = '';
      return;
    }

    this.photoUploading = true;
    this.photoError = null;

    this.clientService.uploadPhoto(this.client.id, this.client.version, file).subscribe({
      next: (updated) => {
        this.photoUploading = false;
        this.client = updated;
        this.refreshPhotoSource(updated);
        target.value = '';
      },
      error: () => {
        this.photoUploading = false;
        this.photoError = 'Failed to upload photo. Please try again.';
        target.value = '';
      },
    });
  }

  private patchForm(c: ClientDetail): void {
    this.profileForm.patchValue({
      fullName: c.fullName || '',
      preferredName: c.preferredName || '',
      dateOfBirth: c.dateOfBirth || '',
      sexOrGender: c.sexOrGender || '',
      pronouns: c.pronouns || '',
      ownerId: c.ownerId || '',
      assignedTherapistId: c.assignedTherapistId || '',
      notes: c.notes || '',
      email: c.email || '',
      phone: c.phone || '',
      secondaryPhone: c.secondaryPhone || '',
      addressLine1: c.addressLine1 || '',
      addressLine2: c.addressLine2 || '',
      city: c.city || '',
      region: c.region || '',
      postalCode: c.postalCode || '',
      country: c.country || '',
      referralSource: c.referralSource || '',
      referralContactName: c.referralContactName || '',
      referralNotes: c.referralNotes || '',
      preferredCommunicationMethod: c.preferredCommunicationMethod || '',
      allowPhone: c.allowPhone ?? false,
      allowSms: c.allowSms ?? false,
      allowEmail: c.allowEmail ?? false,
      allowVoicemail: c.allowVoicemail ?? false,
      emergencyContactName: c.emergencyContactName || '',
      emergencyContactRelationship: c.emergencyContactRelationship || '',
      emergencyContactPhone: c.emergencyContactPhone || '',
      emergencyContactEmail: c.emergencyContactEmail || ''
    }, { emitEvent: false });
  }

  private refreshPhotoSource(client: ClientDetail): void {
    this.releasePhotoObjectUrl();
    this.photoError = null;

    if (!client.photoUrl) {
      this.photoSrc = null;
      return;
    }

    this.clientService.getPhoto(client.id).subscribe({
      next: (blob) => {
        this.photoObjectUrl = URL.createObjectURL(blob);
        this.photoSrc = this.photoObjectUrl;
      },
      error: () => {
        this.photoSrc = null;
        this.photoError = 'Failed to load photo. Please refresh and try again.';
      },
    });
  }

  private releasePhotoObjectUrl(): void {
    if (this.photoObjectUrl) {
      URL.revokeObjectURL(this.photoObjectUrl);
      this.photoObjectUrl = null;
    }
  }

  private toPayload(version: number): UpdateClientProfilePayload {
    const raw = this.profileForm.getRawValue();
    const nil = (v: string | null | undefined): string | null => {
      const trimmed = (v ?? '').trim();
      return trimmed.length ? trimmed : null;
    };

    return {
      version,
      fullName: (raw.fullName || '').trim(),
      preferredName: nil(raw.preferredName),
      dateOfBirth: nil(raw.dateOfBirth),
      sexOrGender: nil(raw.sexOrGender),
      pronouns: nil(raw.pronouns),
      ownerId: nil(raw.ownerId),
      assignedTherapistId: nil(raw.assignedTherapistId),
      notes: nil(raw.notes),
      email: nil(raw.email),
      phone: nil(raw.phone),
      secondaryPhone: nil(raw.secondaryPhone),
      addressLine1: nil(raw.addressLine1),
      addressLine2: nil(raw.addressLine2),
      city: nil(raw.city),
      region: nil(raw.region),
      postalCode: nil(raw.postalCode),
      country: nil(raw.country),
      referralSource: nil(raw.referralSource),
      referralContactName: nil(raw.referralContactName),
      referralNotes: nil(raw.referralNotes),
      preferredCommunicationMethod: nil(raw.preferredCommunicationMethod),
      allowPhone: !!raw.allowPhone,
      allowSms: !!raw.allowSms,
      allowEmail: !!raw.allowEmail,
      allowVoicemail: !!raw.allowVoicemail,
      emergencyContactName: nil(raw.emergencyContactName),
      emergencyContactRelationship: nil(raw.emergencyContactRelationship),
      emergencyContactPhone: nil(raw.emergencyContactPhone),
      emergencyContactEmail: nil(raw.emergencyContactEmail)
    };
  }

  private hasTag(candidate: string): boolean {
    if (!this.client) {
      return false;
    }
    const normalized = candidate.trim().toLowerCase();
    return this.client.tags.some((tag) => tag.trim().toLowerCase() === normalized);
  }

  private loadRecentTagHints(): string[] {
    try {
      const raw = localStorage.getItem(ClientDetailComponent.TAG_HINT_STORAGE_KEY);
      if (!raw) {
        return [];
      }
      const parsed = JSON.parse(raw);
      if (!Array.isArray(parsed)) {
        return [];
      }
      return parsed.filter((value): value is string => typeof value === 'string').slice(0, 50);
    } catch {
      return [];
    }
  }

  private persistRecentTagHints(tags: string[]): void {
    const merged = [...tags, ...this.recentTagHints];
    const unique: string[] = [];
    const seen = new Set<string>();

    for (const tag of merged) {
      const value = tag.trim();
      const key = value.toLowerCase();
      if (!value || seen.has(key)) {
        continue;
      }
      seen.add(key);
      unique.push(value);
      if (unique.length >= 50) {
        break;
      }
    }

    this.recentTagHints = unique;
    try {
      localStorage.setItem(ClientDetailComponent.TAG_HINT_STORAGE_KEY, JSON.stringify(unique));
    } catch {
      // Ignore storage failures and keep in-memory hints only.
    }
  }
}
