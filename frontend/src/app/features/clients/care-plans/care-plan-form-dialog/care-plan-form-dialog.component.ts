import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { TranslocoModule } from '@jsverse/transloco';
import { CarePlanService } from '../services/care-plan.service';

interface GoalFormValue {
  description: string;
  priority: number;
  targetDate: string;
  interventions: { interventionType: string; description: string; frequency: string }[];
  milestones: { description: string; targetDate: string }[];
}


@Component({
  selector: 'app-care-plan-form-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslocoModule],
  template: `
    <div class="dialog-overlay" (click)="onOverlayClick($event)">
      <div class="dialog" role="dialog" [attr.aria-label]="'carePlans.form.title' | transloco">
        <div class="dialog-header">
          <h2>{{ 'carePlans.form.title' | transloco }}</h2>
          <button type="button" class="dialog-close" (click)="cancelled.emit()" [attr.aria-label]="'common.close' | transloco">✕</button>
        </div>

        <div class="dialog-content">
          <form [formGroup]="form" (ngSubmit)="submit()">

            <div class="field">
              <label>{{ 'carePlans.form.planTitle' | transloco }} <span class="required">*</span></label>
              <input type="text" formControlName="title" [placeholder]="'carePlans.form.planTitlePlaceholder' | transloco" />
              <span class="error-msg" *ngIf="getControl('title').invalid && getControl('title').touched">
                {{ 'carePlans.form.titleRequired' | transloco }}
              </span>
            </div>

            <div class="field">
              <label>{{ 'carePlans.form.description' | transloco }}</label>
              <textarea formControlName="description" rows="3" [placeholder]="'carePlans.form.descriptionPlaceholder' | transloco"></textarea>
            </div>

            <!-- Goals Section -->
            <div class="goals-section">
              <div class="goals-header">
                <h3>{{ 'carePlans.form.goals' | transloco }} <span class="required">*</span></h3>
                <button type="button" class="btn-ghost btn-sm" (click)="addGoal()">
                  + {{ 'carePlans.form.addGoal' | transloco }}
                </button>
              </div>

              <div *ngIf="goalsArray.length === 0" class="empty-goals">
                {{ 'carePlans.form.noGoals' | transloco }}
              </div>

              <div *ngFor="let goalCtrl of goalsArray.controls; let gi = index"
                   [formGroup]="asGroup(goalCtrl)"
                   class="goal-block">
                <div class="goal-block-header">
                  <span>{{ 'carePlans.form.goal' | transloco }} {{ gi + 1 }}</span>
                  <button type="button" class="btn-ghost btn-sm btn-danger" (click)="removeGoal(gi)">
                    {{ 'common.remove' | transloco }}
                  </button>
                </div>

                <div class="field">
                  <label>{{ 'carePlans.form.goalDescription' | transloco }} <span class="required">*</span></label>
                  <textarea formControlName="description" rows="2"
                            [placeholder]="'carePlans.form.goalDescriptionPlaceholder' | transloco">
                  </textarea>
                </div>

                <div class="field-row">
                  <div class="field">
                    <label>{{ 'carePlans.form.priority' | transloco }}</label>
                    <input type="number" formControlName="priority" min="1" max="999" />
                  </div>
                  <div class="field">
                    <label>{{ 'carePlans.form.targetDate' | transloco }}</label>
                    <input type="date" formControlName="targetDate" />
                  </div>
                </div>

                <!-- Interventions in goal -->
                <div class="sub-block">
                  <div class="sub-block-header">
                    <span>{{ 'carePlans.form.interventions' | transloco }}</span>
                    <button type="button" class="btn-ghost btn-sm" (click)="addIntervention(gi)">
                      + {{ 'carePlans.form.addIntervention' | transloco }}
                    </button>
                  </div>
                  <div *ngFor="let invCtrl of getInterventions(gi).controls; let ii = index"
                       [formGroup]="asGroup(invCtrl)"
                       class="inline-item">
                    <select formControlName="interventionType">
                      <option value="">{{ 'carePlans.form.selectType' | transloco }}</option>
                      <option *ngFor="let t of interventionTypes" [value]="t">{{ t }}</option>
                    </select>
                    <input type="text" formControlName="description"
                           [placeholder]="'carePlans.form.interventionDesc' | transloco" />
                    <input type="text" formControlName="frequency"
                           [placeholder]="'carePlans.form.frequency' | transloco" />
                    <button type="button" class="btn-ghost btn-sm btn-danger" (click)="removeIntervention(gi, ii)">✕</button>
                  </div>
                </div>

                <!-- Milestones in goal -->
                <div class="sub-block">
                  <div class="sub-block-header">
                    <span>{{ 'carePlans.form.milestones' | transloco }}</span>
                    <button type="button" class="btn-ghost btn-sm" (click)="addMilestone(gi)">
                      + {{ 'carePlans.form.addMilestone' | transloco }}
                    </button>
                  </div>
                  <div *ngFor="let msCtrl of getMilestones(gi).controls; let mi = index"
                       [formGroup]="asGroup(msCtrl)"
                       class="inline-item">
                    <input type="text" formControlName="description"
                           [placeholder]="'carePlans.form.milestoneDesc' | transloco" style="flex:1" />
                    <input type="date" formControlName="targetDate" />
                    <button type="button" class="btn-ghost btn-sm btn-danger" (click)="removeMilestone(gi, mi)">✕</button>
                  </div>
                </div>
              </div>
            </div>

            <div *ngIf="submitError" class="alert-error" role="alert">{{ submitError }}</div>

            <div class="dialog-actions">
              <button type="button" class="btn-secondary" (click)="cancelled.emit()">
                {{ 'common.cancel' | transloco }}
              </button>
              <button type="submit" class="btn-primary"
                      [disabled]="form.invalid || saving || goalsArray.length === 0">
                {{ saving ? ('common.saving' | transloco) : ('carePlans.form.create' | transloco) }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .goals-section { margin-top: var(--spacing-lg); }
    .goals-header, .sub-block-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--spacing-sm); }
    .goals-header h3, .sub-block-header span { font-size: 0.95rem; font-weight: 600; margin: 0; }
    .goal-block { border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: var(--spacing-md); margin-bottom: var(--spacing-sm); }
    .goal-block-header { display: flex; justify-content: space-between; align-items: center; font-weight: 600; margin-bottom: var(--spacing-sm); }
    .sub-block { margin-top: var(--spacing-md); padding-top: var(--spacing-sm); border-top: 1px dashed var(--color-border); }
    .inline-item { display: flex; gap: var(--spacing-sm); align-items: center; margin-bottom: 6px; }
    .inline-item input, .inline-item select { flex: 1; padding: 4px 8px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); font-size: 0.875rem; }
    .btn-sm { padding: 4px 10px; font-size: 0.8rem; }
    .btn-danger { color: #dc2626; }
    .empty-goals { text-align: center; padding: var(--spacing-md); color: var(--color-text-muted); font-size: 0.875rem; }
    .field-row { display: grid; grid-template-columns: 1fr 1fr; gap: var(--spacing-md); }
  `]
})
export class CarePlanFormDialogComponent implements OnInit {
  @Input({ required: true }) clientId!: string;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  form!: FormGroup;
  saving = false;
  submitError: string | null = null;
  interventionTypes: string[] = [];

  constructor(
    private fb: FormBuilder,
    private carePlanService: CarePlanService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(255)]],
      description: [''],
      goals: this.fb.array([])
    });

    this.carePlanService.getInterventionTypes().subscribe({
      next: types => { this.interventionTypes = types; }
    });
  }

  get goalsArray(): FormArray {
    return this.form.get('goals') as FormArray;
  }

  getInterventions(goalIndex: number): FormArray {
    return this.goalsArray.at(goalIndex).get('interventions') as FormArray;
  }

  getMilestones(goalIndex: number): FormArray {
    return this.goalsArray.at(goalIndex).get('milestones') as FormArray;
  }

  asGroup(ctrl: AbstractControl): FormGroup {
    return ctrl as FormGroup;
  }

  getControl(name: string): AbstractControl {
    return this.form.get(name)!;
  }

  addGoal(): void {
    this.goalsArray.push(this.fb.group({
      description: ['', Validators.required],
      priority: [1],
      targetDate: [''],
      interventions: this.fb.array([]),
      milestones: this.fb.array([])
    }));
  }

  removeGoal(index: number): void {
    this.goalsArray.removeAt(index);
  }

  addIntervention(goalIndex: number): void {
    this.getInterventions(goalIndex).push(this.fb.group({
      interventionType: ['', Validators.required],
      description: ['', Validators.required],
      frequency: ['']
    }));
  }

  removeIntervention(goalIndex: number, invIndex: number): void {
    this.getInterventions(goalIndex).removeAt(invIndex);
  }

  addMilestone(goalIndex: number): void {
    this.getMilestones(goalIndex).push(this.fb.group({
      description: ['', Validators.required],
      targetDate: ['']
    }));
  }

  removeMilestone(goalIndex: number, msIndex: number): void {
    this.getMilestones(goalIndex).removeAt(msIndex);
  }

  onOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('dialog-overlay')) {
      this.cancelled.emit();
    }
  }

  submit(): void {
    if (this.form.invalid || this.goalsArray.length === 0) return;
    this.saving = true;
    this.submitError = null;

    const value = this.form.value as { title: string; description: string; goals: GoalFormValue[] };
    const request = {
      title: value.title,
      description: value.description || undefined,
      goals: value.goals.map((g: GoalFormValue) => ({
        description: g.description,
        priority: g.priority || undefined,
        targetDate: g.targetDate || undefined,
        interventions: g.interventions
          .filter((i) => i.interventionType && i.description)
          .map((i) => ({
            interventionType: i.interventionType,
            description: i.description,
            frequency: i.frequency || undefined
          })),
        milestones: g.milestones
          .filter((m) => m.description)
          .map((m) => ({
            description: m.description,
            targetDate: m.targetDate || undefined
          }))
      }))
    };

    this.carePlanService.create(this.clientId, request).subscribe({
      next: () => {
        this.saving = false;
        this.saved.emit();
      },
      error: () => {
        this.saving = false;
        this.submitError = 'carePlans.form.saveError';
      }
    });
  }
}
