import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';
import { HasPermissionDirective } from './has-permission.directive';
import { AuthService } from '../auth.service';

function makeFakeJwt(role: string): string {
  const body = btoa(JSON.stringify({ sub: '1', role, exp: 9999999999 })).replace(/=/g, '');
  return `header.${body}.sig`;
}

@Component({
  standalone: true,
  imports: [HasPermissionDirective],
  template: `
    <button *appHasPermission="'VIEW_BILLING_ACTIONS'" id="billing-btn">Billing</button>
    <button *appHasPermission="'VIEW_SESSION_NOTES'" id="notes-btn">Notes</button>
    <button *appHasPermission="'BOOK_APPOINTMENT'" id="book-btn">Book</button>
  `
})
class TestHostComponent {}

describe('HasPermissionDirective', () => {
  let authService: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: [provideHttpClient(), provideRouter([])]
    });
    authService = TestBed.inject(AuthService);
  });

  it('renders elements when user has the required permission', () => {
    authService.setToken(makeFakeJwt('FINANCE'));
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();

    const billingBtn = fixture.debugElement.query(By.css('#billing-btn'));
    expect(billingBtn).toBeTruthy();
  });

  it('removes elements from DOM when user lacks permission', () => {
    authService.setToken(makeFakeJwt('FINANCE'));
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();

    // FINANCE cannot VIEW_SESSION_NOTES
    const notesBtn = fixture.debugElement.query(By.css('#notes-btn'));
    expect(notesBtn).toBeNull();
  });

  it('renders BOOK_APPOINTMENT for RECEPTION_ADMIN_STAFF', () => {
    authService.setToken(makeFakeJwt('RECEPTION_ADMIN_STAFF'));
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();

    const bookBtn = fixture.debugElement.query(By.css('#book-btn'));
    expect(bookBtn).toBeTruthy();
  });

  it('hides all permission elements when no token', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();

    const billingBtn = fixture.debugElement.query(By.css('#billing-btn'));
    const notesBtn   = fixture.debugElement.query(By.css('#notes-btn'));
    const bookBtn    = fixture.debugElement.query(By.css('#book-btn'));

    expect(billingBtn).toBeNull();
    expect(notesBtn).toBeNull();
    expect(bookBtn).toBeNull();
  });

  it('ADMIN sees all elements', () => {
    authService.setToken(makeFakeJwt('ADMIN'));
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();

    const billingBtn = fixture.debugElement.query(By.css('#billing-btn'));
    const notesBtn   = fixture.debugElement.query(By.css('#notes-btn'));
    const bookBtn    = fixture.debugElement.query(By.css('#book-btn'));

    expect(billingBtn).toBeTruthy();
    expect(notesBtn).toBeTruthy();
    expect(bookBtn).toBeTruthy();
  });
});
