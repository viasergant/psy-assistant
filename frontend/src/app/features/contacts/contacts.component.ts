import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { AuthService } from '../../core/auth/auth.service';
import { PlatformContacts } from './contacts.model';
import { ContactsService } from './contacts.service';

/**
 * Public contacts page. Reachable by logged-in and anonymous users, so it lives
 * outside the authenticated shell.
 */
@Component({
  selector: 'app-contacts',
  standalone: true,
  imports: [RouterLink, TranslocoPipe],
  template: `
    <main class="contacts-page">
      <a class="back-link" [routerLink]="backLink()">{{ 'contacts.back' | transloco }}</a>
      <h1>{{ 'contacts.title' | transloco }}</h1>

      @if (loading()) {
        <p>{{ 'contacts.loading' | transloco }}</p>
      } @else if (failed()) {
        <p role="alert">{{ 'contacts.loadError' | transloco }}</p>
      } @else if (isEmpty()) {
        <p>{{ 'contacts.empty' | transloco }}</p>
      } @else {
        <ul class="contact-list">
          @if (contacts()?.phone; as phone) {
            <li>
              <span class="label">{{ 'contacts.phone' | transloco }}</span>
              <a [href]="'tel:' + phoneHref(phone)">{{ phone }}</a>
            </li>
          }
          @if (contacts()?.email; as email) {
            <li>
              <span class="label">{{ 'contacts.email' | transloco }}</span>
              <a [href]="'mailto:' + email">{{ email }}</a>
            </li>
          }
          @for (channel of contacts()?.telegramChannels; track channel) {
            <li>
              <span class="label">{{ 'contacts.telegram' | transloco }}</span>
              <a [href]="channel" target="_blank" rel="noopener noreferrer">{{ channel }}</a>
            </li>
          }
        </ul>
      }
    </main>
  `,
  styles: [`
    .contacts-page { max-width: 40rem; margin: 0 auto; padding: 2rem 1rem; }
    .back-link { display: inline-block; margin-bottom: 1rem; }
    .contact-list { list-style: none; padding: 0; display: grid; gap: 0.75rem; }
    .label { display: block; font-size: 0.8rem; color: #6b7280; }
  `]
})
export class ContactsComponent implements OnInit {
  private readonly service = inject(ContactsService);
  private readonly auth = inject(AuthService);

  readonly contacts = signal<PlatformContacts | null>(null);
  readonly loading = signal(true);
  readonly failed = signal(false);

  ngOnInit(): void {
    this.service.getContacts().subscribe({
      next: contacts => {
        this.contacts.set(contacts);
        this.loading.set(false);
      },
      error: () => {
        this.failed.set(true);
        this.loading.set(false);
      }
    });
  }

  isEmpty(): boolean {
    const c = this.contacts();
    return !c || (!c.phone && !c.email && c.telegramChannels.length === 0);
  }

  backLink(): string {
    return this.auth.isAuthenticated() ? '/' : '/auth/login';
  }

  phoneHref(phone: string): string {
    return phone.replace(/[^+\d]/g, '');
  }
}
