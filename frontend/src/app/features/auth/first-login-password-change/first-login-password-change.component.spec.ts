import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FirstLoginPasswordChangeComponent } from './first-login-password-change.component';

describe('FirstLoginPasswordChangeComponent', () => {
  let component: FirstLoginPasswordChangeComponent;
  let fixture: ComponentFixture<FirstLoginPasswordChangeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FirstLoginPasswordChangeComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FirstLoginPasswordChangeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
