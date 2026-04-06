import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { TherapistManagementService } from '../../services/therapist-management.service';
import { TherapistProfile, EMPLOYMENT_STATUS_LABELS } from '../../models/therapist.model';

/**
 * Therapist profile detail view.
 *
 * Displays comprehensive read-only information about a therapist including:
 * - Basic information (name, email, phone)
 * - Employment status
 * - Biography
 * - Specializations
 * - Languages
 */
@Component({
  selector: 'app-therapist-detail',
  standalone: true,
  imports: [CommonModule, TranslocoPipe],
  styleUrl: './therapist-detail.component.scss',
  template: `
    <div class="page">
      <div *ngIf="loading" class="state-msg" aria-live="polite">Loading profile…</div>
      <div *ngIf="!loading && loadError" class="alert-error" role="alert">{{ loadError }}</div>

      <div *ngIf="!loading && therapist" class="profile-container">
        <!-- Header -->
        <header class="profile-header">
          <div class="header-content">
            <h1>{{ therapist.name }}</h1>
            <span class="badge" [class.badge-active]="therapist.active" [class.badge-inactive]="!therapist.active">
              {{ therapist.active ? 'Active' : 'Inactive' }}
            </span>
          </div>
          <div class="header-actions">
            <button class="btn-secondary" (click)="goBack()">← Back to List</button>
            <button class="btn-primary" (click)="editTherapist()">Edit Profile</button>
          </div>
        </header>

        <!-- Basic Information -->
        <section class="profile-section">
          <h2>Contact Information</h2>
          <div class="info-grid">
            <div class="info-item">
              <label>{{ 'admin.therapists.detail.fields.email' | transloco }}</label>
              <p>{{ therapist.email }}</p>
            </div>
            <div class="info-item">
              <label>{{ 'admin.therapists.detail.fields.phone' | transloco }}</label>
              <p>{{ therapist.phone || '—' }}</p>
            </div>
            <div class="info-item">
              <label>Employment Status</label>
              <p>
                <span class="badge" [class.badge-active]="therapist.employmentStatus === 'ACTIVE'">
                  {{ statusLabels[therapist.employmentStatus] }}
                </span>
              </p>
            </div>
          </div>
        </section>

        <!-- Biography -->
        <section class="profile-section" *ngIf="therapist.bio">
          <h2>{{ 'admin.therapists.detail.sections.biography' | transloco }}</h2>
          <p class="bio-text">{{ therapist.bio }}</p>
        </section>

        <!-- Specializations -->
        <section class="profile-section">
          <h2>{{ 'admin.therapists.detail.sections.specializations' | transloco }}</h2>
          <div class="chips-container" *ngIf="therapist.specializations.length > 0">
            <span *ngFor="let spec of therapist.specializations" class="chip">
              {{ spec.name }}
            </span>
          </div>
          <p *ngIf="therapist.specializations.length === 0" class="text-muted">No specializations listed</p>
        </section>

        <!-- Languages -->
        <section class="profile-section">
          <h2>{{ 'admin.therapists.detail.sections.languages' | transloco }}</h2>
          <div class="chips-container" *ngIf="therapist.languages.length > 0">
            <span *ngFor="let lang of therapist.languages" class="chip">
              {{ lang.name }}
            </span>
          </div>
          <p *ngIf="therapist.languages.length === 0" class="text-muted">No languages listed</p>
        </section>

        <!-- Metadata -->
        <section class="profile-section metadata">
          <h2>{{ 'admin.therapists.detail.sections.metadata' | transloco }}</h2>
          <div class="info-grid">
            <div class="info-item">
              <label>{{ 'admin.therapists.detail.fields.created' | transloco }}</label>
              <p>{{ therapist.createdAt | date:'dd MMM yyyy, HH:mm' }}</p>
            </div>
            <div class="info-item">
              <label>Created By</label>
              <p>{{ therapist.createdBy || '—' }}</p>
            </div>
            <div class="info-item" *ngIf="therapist.lastModifiedAt">
              <label>Last Updated</label>
              <p>{{ therapist.lastModifiedAt | date:'dd MMM yyyy, HH:mm' }}</p>
            </div>
            <div class="info-item" *ngIf="therapist.lastModifiedBy">
              <label>Last Modified By</label>
              <p>{{ therapist.lastModifiedBy }}</p>
            </div>
            <div class="info-item">
              <label>{{ 'admin.therapists.detail.fields.version' | transloco }}</label>
              <p>{{ therapist.version }}</p>
            </div>
          </div>
        </section>
      </div>
    </div>
  `,
})

export class TherapistDetailComponent implements OnInit {
  therapist: TherapistProfile | null = null;
  loading = false;
  loadError = '';
  statusLabels = EMPLOYMENT_STATUS_LABELS;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private therapistService: TherapistManagementService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadTherapist(id);
    } else {
      this.loadError = 'No therapist ID provided';
    }
  }

  private loadTherapist(id: string): void {
    this.loading = true;
    this.loadError = '';
    this.therapistService.getTherapist(id).subscribe({
      next: (therapist) => {
        this.therapist = therapist;
        this.loading = false;
      },
      error: (err) => {
        this.loadError = err?.error?.message || 'Failed to load therapist profile';
        this.loading = false;
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/admin/therapists']);
  }

  editTherapist(): void {
    if (this.therapist) {
      // Navigate to list with edit mode, or stay here and open edit dialog
      this.router.navigate(['/admin/therapists'], { 
        state: { editTherapistId: this.therapist.id }
      });
    }
  }
}
