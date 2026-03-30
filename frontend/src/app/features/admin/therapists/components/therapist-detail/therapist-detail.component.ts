import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
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
 * - License information
 * - Pricing rules
 */
@Component({
  selector: 'app-therapist-detail',
  standalone: true,
  imports: [CommonModule],
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
              <label>Email</label>
              <p>{{ therapist.email }}</p>
            </div>
            <div class="info-item">
              <label>Phone</label>
              <p>{{ therapist.phone || '—' }}</p>
            </div>
            <div class="info-item">
              <label>Contact Phone</label>
              <p>{{ therapist.contactPhone || '—' }}</p>
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
          <h2>Biography</h2>
          <p class="bio-text">{{ therapist.bio }}</p>
        </section>

        <!-- Specializations -->
        <section class="profile-section">
          <h2>Specializations</h2>
          <div class="chips-container" *ngIf="therapist.specializations.length > 0">
            <span *ngFor="let spec of therapist.specializations" class="chip">
              {{ spec.name }}
            </span>
          </div>
          <p *ngIf="therapist.specializations.length === 0" class="text-muted">No specializations listed</p>
        </section>

        <!-- Languages -->
        <section class="profile-section">
          <h2>Languages</h2>
          <div class="chips-container" *ngIf="therapist.languages.length > 0">
            <span *ngFor="let lang of therapist.languages" class="chip">
              {{ lang.name }}
            </span>
          </div>
          <p *ngIf="therapist.languages.length === 0" class="text-muted">No languages listed</p>
        </section>

        <!-- Licenses -->
        <section class="profile-section" *ngIf="therapist.licenses && therapist.licenses.length > 0">
          <h2>Licenses & Credentials</h2>
          <div class="table-wrapper">
            <table aria-label="License information">
              <thead>
                <tr>
                  <th>Type</th>
                  <th>Number</th>
                  <th>Issuing Body</th>
                  <th>Issue Date</th>
                  <th>Expiry Date</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let license of therapist.licenses">
                  <td>{{ license.licenseType }}</td>
                  <td>{{ license.licenseNumber }}</td>
                  <td>{{ license.issuingBody }}</td>
                  <td>{{ license.issueDate | date:'dd MMM yyyy' }}</td>
                  <td>{{ license.expiryDate ? (license.expiryDate | date:'dd MMM yyyy') : '—' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- Pricing Rules -->
        <section class="profile-section" *ngIf="therapist.pricingRules && therapist.pricingRules.length > 0">
          <h2>Pricing Rules</h2>
          <div class="table-wrapper">
            <table aria-label="Pricing information">
              <thead>
                <tr>
                  <th>Session Type</th>
                  <th>Base Rate</th>
                  <th>Currency</th>
                  <th>Effective From</th>
                  <th>Effective To</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let rule of therapist.pricingRules">
                  <td>{{ rule.sessionType }}</td>
                  <td>{{ rule.baseRate }}</td>
                  <td>{{ rule.currency }}</td>
                  <td>{{ rule.effectiveFrom | date:'dd MMM yyyy' }}</td>
                  <td>{{ rule.effectiveTo ? (rule.effectiveTo | date:'dd MMM yyyy') : 'Ongoing' }}</td>
                  <td>
                    <span class="badge" [class.badge-active]="rule.active" [class.badge-inactive]="!rule.active">
                      {{ rule.active ? 'Active' : 'Inactive' }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- Metadata -->
        <section class="profile-section metadata">
          <h2>Metadata</h2>
          <div class="info-grid">
            <div class="info-item">
              <label>Created</label>
              <p>{{ therapist.createdAt | date:'dd MMM yyyy, HH:mm' }}</p>
            </div>
            <div class="info-item">
              <label>Last Updated</label>
              <p>{{ therapist.updatedAt | date:'dd MMM yyyy, HH:mm' }}</p>
            </div>
            <div class="info-item">
              <label>Version</label>
              <p>{{ therapist.version }}</p>
            </div>
          </div>
        </section>
      </div>
    </div>
  `,
  styles: [`
    .page { padding: 2rem; max-width: 1200px; margin: 0 auto; }
    .state-msg { color: #64748B; padding: 2rem 0; text-align: center; }
    .alert-error {
      padding: .75rem 1rem; background: #FEF2F2;
      border: 1px solid #FECACA; border-radius: 8px;
      color: #DC2626; margin-bottom: 1rem; font-size: .875rem;
    }
    
    .profile-container { display: flex; flex-direction: column; gap: 2rem; }
    
    .profile-header {
      display: flex; justify-content: space-between; align-items: flex-start;
      padding-bottom: 1.5rem; border-bottom: 2px solid #E2E8F0;
    }
    .header-content { display: flex; align-items: center; gap: 1rem; }
    .header-content h1 { margin: 0; font-size: 1.875rem; }
    .header-actions { display: flex; gap: 0.75rem; }
    
    .btn-primary, .btn-secondary {
      padding: .5rem 1.25rem; border: none; border-radius: 8px; 
      cursor: pointer; font-size: .9375rem; font-weight: 600;
      transition: all 0.15s ease;
    }
    .btn-primary {
      background: #0EA5A0; color: #fff;
    }
    .btn-primary:hover { background: #0C9490; box-shadow: 0 4px 12px rgba(14,165,160,.28); }
    .btn-primary:active { background: #0A8480; }
    
    .btn-secondary {
      background: #F1F5F9; color: #334155;
    }
    .btn-secondary:hover { background: #E2E8F0; }
    .btn-secondary:active { background: #CBD5E1; }
    
    .profile-section {
      background: #fff; padding: 1.5rem; border-radius: 12px;
      border: 1px solid #E2E8F0;
    }
    .profile-section h2 {
      margin: 0 0 1rem 0; font-size: 1.125rem; font-weight: 600;
      color: #1E293B;
    }
    
    .info-grid {
      display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 1.5rem;
    }
    .info-item label {
      display: block; font-size: .8125rem; font-weight: 600;
      color: #64748B; margin-bottom: .25rem;
    }
    .info-item p { margin: 0; color: #1E293B; }
    
    .bio-text {
      margin: 0; line-height: 1.6; color: #334155;
      white-space: pre-wrap;
    }
    
    .chips-container {
      display: flex; flex-wrap: wrap; gap: 0.5rem;
    }
    .chip {
      display: inline-block; padding: .375rem .875rem;
      background: #F1F5F9; color: #334155;
      border-radius: 16px; font-size: .875rem; font-weight: 500;
    }
    
    .badge {
      display: inline-block; padding: .25rem .75rem;
      border-radius: 12px; font-size: .8125rem; font-weight: 600;
    }
    .badge-active { background: #D1FAE5; color: #065F46; }
    .badge-inactive { background: #FEE2E2; color: #991B1B; }
    
    .text-muted { color: #94A3B8; margin: 0; }
    
    .table-wrapper { overflow-x: auto; }
    table {
      width: 100%; border-collapse: collapse; font-size: .9rem;
    }
    th {
      text-align: left; padding: .75rem; background: #F7FAFC;
      border-bottom: 2px solid #E2E8F0; font-weight: 600;
      color: #475569;
    }
    td {
      padding: .75rem; border-bottom: 1px solid #E2E8F0;
    }
    tr:last-child td { border-bottom: none; }
    
    .metadata { background: #FAFBFC; }
  `]
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
