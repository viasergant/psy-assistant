import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TherapistProfileWizardComponent } from './therapist-profile-wizard.component';

describe('TherapistProfileWizardComponent', () => {
  let component: TherapistProfileWizardComponent;
  let fixture: ComponentFixture<TherapistProfileWizardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TherapistProfileWizardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TherapistProfileWizardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
