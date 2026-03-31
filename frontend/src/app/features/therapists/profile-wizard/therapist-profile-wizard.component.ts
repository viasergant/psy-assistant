import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { StepperModule } from 'primeng/stepper';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { Textarea } from 'primeng/textarea';
import { MultiSelectModule } from 'primeng/multiselect';
import { Select } from 'primeng/select';
import { InputNumberModule } from 'primeng/inputnumber';
import { CheckboxModule } from 'primeng/checkbox';
import { MessageService } from 'primeng/api';
import { ToastModule } from 'primeng/toast';
import { IdNamePair } from '../../admin/therapists/models/therapist.model';
import { TranslocoPipe } from '@jsverse/transloco';

/**
 * Multi-step wizard for therapists to complete their profile after first login.
 * 
 * Steps:
 * 1. Personal Information (name, phone, bio)
 * 2. Credentials & Qualifications (licenses, education, years experience)
 * 3. Specializations & Languages (multi-select)
 * 4. Availability & Pricing (optional)
 */
@Component({
  selector: 'app-therapist-profile-wizard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    StepperModule,
    ButtonModule,
    InputTextModule,
    Textarea,
    MultiSelectModule,
    Select,
    InputNumberModule,
    CheckboxModule,
    ToastModule,
    TranslocoPipe
  ],
  providers: [MessageService],
  templateUrl: './therapist-profile-wizard.component.html',
  styleUrl: './therapist-profile-wizard.component.scss'
})
export class TherapistProfileWizardComponent implements OnInit {
  currentStep = 1;  // PrimeNG v20 Stepper uses 1-based indexing
  loading = false;

  // Forms for each step
  personalInfoForm!: FormGroup;
  credentialsForm!: FormGroup;
  specializationsForm!: FormGroup;
  pricingForm!: FormGroup;

  // Dropdown/multi-select options
  specializations: IdNamePair[] = [];
  languages: IdNamePair[] = [];
  
  professionalTitleOptions = [
    { label: 'Psychologist', value: 'PSYCHOLOGIST' },
    { label: 'Psychiatrist', value: 'PSYCHIATRIST' },
    { label: 'Therapist', value: 'THERAPIST' },
    { label: 'Counselor', value: 'COUNSELOR' },
    { label: 'Social Worker', value: 'SOCIAL_WORKER' },
    { label: 'Other', value: 'OTHER' }
  ];

  sessionDurationOptions = [
    { label: '30 minutes', value: 30 },
    { label: '45 minutes', value: 45 },
    { label: '50 minutes', value: 50 },
    { label: '60 minutes', value: 60 },
    { label: '90 minutes', value: 90 }
  ];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private http: HttpClient,
    private messageService: MessageService
  ) {
    this.initializeForms();
  }

  ngOnInit(): void {
    this.loadDropdownOptions();
  }

  /**
   * Initialize all form groups.
   */
  private initializeForms(): void {
    this.personalInfoForm = this.fb.group({
      fullName: ['', [Validators.required, Validators.maxLength(100)]],
      phone: ['', [Validators.pattern(/^\+?[1-9]\d{1,14}$/)]],
      contactPhone: ['', [Validators.pattern(/^\+?[1-9]\d{1,14}$/)]],
      bio: ['', [Validators.required, Validators.maxLength(500)]]
    });

    this.credentialsForm = this.fb.group({
      professionalTitle: ['', Validators.required],
      licenseNumbers: [''],
      credentials: [''],
      yearsOfExperience: [null, [Validators.min(0), Validators.max(70)]],
      education: ['']
    });

    this.specializationsForm = this.fb.group({
      specializationIds: [[], [Validators.required, Validators.minLength(1)]],
      languageIds: [[], [Validators.required, Validators.minLength(1)]]
    });

    this.pricingForm = this.fb.group({
      sessionDuration: [50],
      baseRate: [null, [Validators.min(0)]],
      acceptsInsurance: [false]
    });
  }

  /**
   * Load specializations and languages from backend.
   */
  private loadDropdownOptions(): void {
    forkJoin({
      specializations: this.http.get<IdNamePair[]>('/api/v1/specializations'),
      languages: this.http.get<IdNamePair[]>('/api/v1/languages')
    }).subscribe({
      next: (data) => {
        this.specializations = data.specializations;
        this.languages = data.languages;
      },
      error: (error) => {
        console.error('Failed to load dropdown options', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Error',
          detail: 'Failed to load form options. Please refresh the page.'
        });
      }
    });
  }

  /**
   * Validate and proceed to next step.
   */
  nextStep(): void {
    const currentForm = this.getCurrentStepForm();
    if (currentForm && !currentForm.valid) {
      currentForm.markAllAsTouched();
      this.messageService.add({
        severity: 'warn',
        summary: 'Validation Error',
        detail: 'Please fill in all required fields correctly.'
      });
      return;
    }

    if (this.currentStep < 3) {
      this.currentStep++;
    }
  }

  /**
   * Navigate to previous step.
   */
  previousStep(): void {
    if (this.currentStep > 0) {
      this.currentStep--;
    }
  }

  /**
   * Get the form for the current step.
   */
  private getCurrentStepForm(): FormGroup | null {
    switch (this.currentStep) {
      case 0: return this.personalInfoForm;
      case 1: return this.credentialsForm;
      case 2: return this.specializationsForm;
      case 3: return this.pricingForm;
      default: return null;
    }
  }

  /**
   * Save progress and complete later.
   */
  completeLater(): void {
    this.loading = true;
    
    // Collect all form data
    const profileData = {
      ...this.personalInfoForm.value,
      ...this.credentialsForm.value,
      ...this.specializationsForm.value,
      ...this.pricingForm.value
    };

    // Save draft to backend
    this.http.patch('/api/v1/therapists/profile/me', profileData).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Progress Saved',
          detail: 'Your profile has been saved. You can complete it later.'
        });
        setTimeout(() => this.router.navigate(['/']), 1500);
      },
      error: (error) => {
        console.error('Failed to save profile', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Save Failed',
          detail: 'Could not save your progress. Please try again.'
        });
        this.loading = false;
      }
    });
  }

  /**
   * Submit final profile data and mark as complete.
   */
  submitProfile(): void {
    // Validate all forms
    if (!this.personalInfoForm.valid || !this.credentialsForm.valid || !this.specializationsForm.valid) {
      this.messageService.add({
        severity: 'error',
        summary: 'Incomplete Profile',
        detail: 'Please complete all required fields in previous steps.'
      });
      return;
    }

    this.loading = true;

    // Collect all form data
    const profileData = {
      ...this.personalInfoForm.value,
      ...this.credentialsForm.value,
      ...this.specializationsForm.value,
      ...this.pricingForm.value
    };

    // Update profile and mark as complete
    this.http.patch('/api/v1/therapists/profile/me', profileData).subscribe({
      next: () => {
        // Mark profile as complete
        this.http.put('/api/v1/therapists/profile/mark-complete', {}).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: 'Profile Complete!',
              detail: 'Your profile has been completed successfully.'
            });
            setTimeout(() => this.router.navigate(['/']), 1500);
          },
          error: (error) => {
            console.error('Failed to mark profile complete', error);
            this.messageService.add({
              severity: 'error',
              summary: 'Completion Failed',
              detail: error.error?.message || 'Could not complete your profile. Please ensure all required fields are filled.'
            });
            this.loading = false;
          }
        });
      },
      error: (error) => {
        console.error('Failed to update profile', error);
        this.messageService.add({
          severity: 'error',
          summary: 'Update Failed',
          detail: 'Could not save your profile. Please try again.'
        });
        this.loading = false;
      }
    });
  }

  /**
   * Check if a form field has errors and has been touched.
   */
  hasError(form: FormGroup, fieldName: string): boolean {
    const field = form.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }

  /**
   * Get error message for a field.
   */
  getErrorMessage(form: FormGroup, fieldName: string): string {
    const field = form.get(fieldName);
    if (!field || !field.errors) return '';

    if (field.errors['required']) return 'This field is required';
    if (field.errors['maxLength']) return `Maximum ${field.errors['maxLength'].requiredLength} characters`;
    if (field.errors['minLength']) return `At least ${field.errors['minLength'].requiredLength} item(s) required`;
    if (field.errors['pattern']) return 'Invalid format';
    if (field.errors['min']) return `Minimum value is ${field.errors['min'].min}`;
    if (field.errors['max']) return `Maximum value is ${field.errors['max'].max}`;

    return 'Invalid value';
  }
}
