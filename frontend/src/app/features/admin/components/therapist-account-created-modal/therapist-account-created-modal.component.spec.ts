import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TherapistAccountCreatedModalComponent } from './therapist-account-created-modal.component';

describe('TherapistAccountCreatedModalComponent', () => {
  let component: TherapistAccountCreatedModalComponent;
  let fixture: ComponentFixture<TherapistAccountCreatedModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TherapistAccountCreatedModalComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TherapistAccountCreatedModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
