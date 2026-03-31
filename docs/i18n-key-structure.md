# i18n Key Naming Convention

## Overview

This document defines the hierarchical key naming convention for internationalization (i18n) in the PSY-ASSISTANT frontend application. All UI-visible strings must be sourced from translation files (`en.json`, `uk.json`) using this structure.

## Key Structure Pattern

```
{domain}.{feature}.{element}[.{variant}]
```

### Components

- **domain**: Top-level feature area (e.g., `auth`, `clients`, `schedule`)
- **feature**: Specific sub-feature or screen (e.g., `login`, `profile`, `calendar`)
- **element**: UI element type (e.g., `label`, `button`, `error`, `placeholder`)
- **variant** (optional): Specific variant or state (e.g., `email`, `phone`, `required`)

## Domain Organization

### `common`
Shared strings used across multiple features:
- `common.actions.*` - Button labels (save, cancel, delete, etc.)
- `common.validation.*` - Form validation messages
- `common.status.*` - Status indicators (loading, success, error)
- `common.aria.*` - ARIA labels for accessibility
- `common.confirmation.*` - Confirmation dialog messages

### `auth`
Authentication and authorization:
- `auth.login.*` - Login screen
- `auth.passwordReset.*` - Password reset flow
- `auth.firstTimeSetup.*` - First-time password change
- `auth.errors.*` - Authentication error messages

### `nav`
Navigation and application shell:
- `nav.*` - Top-level navigation items
- `nav.sidebar.*` - Sidebar navigation
- `nav.breadcrumbs.*` - Breadcrumb navigation

### `clients`
Client management:
- `clients.list.*` - Client list view
- `clients.profile.*` - Client profile details
- `clients.form.*` - Client form fields
- `clients.appointments.*` -Client appointment history
- `clients.notes.*` - Client notes

### `leads`
Lead management:
- `leads.list.*` - Lead list view
- `leads.create.*` - Create lead dialog
- `leads.edit.*` - Edit lead dialog
- `leads.convert.*` - Convert lead to client
- `leads.filters.*` - Filter options

### `schedule`
Schedule and calendar management:
- `schedule.calendar.*` - Calendar view
- `schedule.recurring.*` - Weekly working hours
- `schedule.override.*` - Specific date overrides
- `schedule.leave.*` - Leave period management
- `schedule.leaveType.*` - Leave type labels
- `schedule.leaveStatus.*` - Leave status labels
- `schedule.validation.*` - Schedule validation messages

### `sessions`
Session management:
- `sessions.list.*` - Session list view
- `sessions.notes.*` - Session notes
- `sessions.billing.*` - Session billing information
- `sessions.outcomes.*` - Session outcomes

### `therapists`
Therapist management:
- `therapists.list.*` - Therapist list
- `therapists.wizard.*` - Profile wizard
- `therapists.profile.*` - Therapist profile
- `therapists.credentials.*` - Professional credentials
- `therapists.specializations.*` - Specialization areas
- `therapists.types.*` - Therapist types

### `admin`
Administrative functions:
- `admin.users.*` - User management
- `admin.therapists.*` - Therapist administration
- `admin.leave.*` - Leave request approval
- `admin.tabs.*` - Admin section tabs

### `billing`
Billing and invoicing:
- `billing.invoices.*` - Invoice management
- `billing.payments.*` - Payment processing
- `billing.reports.*` - Billing reports

### `reports`
Reporting and analytics:
- `reports.filters.*` - Report filters
- `reports.exports.*` - Export options
- `reports.charts.*` - Chart labels

### `errors`
Error messages:
- `errors.http.*` - HTTP error codes (404, 500, etc.)
- `errors.validation.*` - Validation errors
- `errors.business.*` - Business logic errors

## Examples

### Good Key Names

```json
{
  "auth.login.username": "Username",
  "schedule.calendar.weekRange": "{{start}} - {{end}}",
  "clients.profile.form.phoneNumber": "Phone Number",
  "common.actions.save": "Save",
  "common.validation.required": "This field is required",
  "errors.http.404.message": "Page not found"
}
```

### Bad Key Names (Avoid)

```json
{
  "username": "Username",  // Too generic, no domain
  "auth_login_username": "Username",  // Use dots, not underscores
  "authLoginUsernameLabel": "Username",  // Too verbose, camelCase instead of dot notation
  "schedule.calendar.week.range": "{{start}} - {{end}}"  // Over-nested
}
```

## Interpolation

Use `{{variableName}}` for dynamic values:

```json
{
  "schedule.calendar.weekRange": "{{start}} - {{end}}",
  "clients.profile.welcomeMessage": "Welcome, {{name}}!",
  "sessions.notes.lastUpdated": "Last updated {{count}} days ago"
}
```

## Usage in Components

### HTML Templates

```html
<!-- Static translation -->
<label>{{ 'auth.login.username' | transloco }}</label>

<!-- With interpolation -->
<p>{{ 'schedule.calendar.weekRange' | transloco: {start: startDate, end: endDate} }}</p>

<!-- Structural directive -->
<ng-container *transloco="let t">
  <h1>{{ t('clients.profile.title') }}</h1>
  <p>{{ t('clients.profile.subtitle') }}</p>
</ng-container>
```

### TypeScript

```typescript
import { inject } from '@angular/core';
import { TranslocoService } from '@ngneat/transloco';

export class MyComponent {
  private transloco = inject(TranslocoService);

  showToast() {
    this.messageService.add({
      severity: 'success',
      summary: this.transloco.translate('common.status.success'),
      detail: this.transloco.translate('clients.profile.saveSuccess')
    });
  }
}
```

### Reactive Translations (Dropdowns)

For values that must update on language switch:

```typescript
// ❌ WRONG (static)
therapyTypes = [
  { label: this.transloco.translate('therapists.types.psychologist'), value: 'PSYCHOLOGIST' }
];

// ✅ CORRECT (reactive)
therapyTypes$ = this.transloco.selectTranslate('therapists.types', {}, 'therapists').pipe(
  map(t => [
    { label: t.psychologist, value: 'PSYCHOLOGIST' },
    { label: t.psychiatrist, value: 'PSYCHIATRIST' }
  ])
);
```

## Validation

All translation files are validated in CI using the following checks:

1. **JSON validity**: Files must be valid JSON
2. **Key parity**: All keys in `en.json` must exist in `uk.json` and vice versa
3. **Hard-coded strings**: No hard-coded display strings allowed in templates or TypeScript
4. **String length**: Ukrainian strings should not exceed 40% length variance from English

## Maintenance

When adding new features:

1. Add all UI strings to both `en.json` and `uk.json` simultaneously
2. Follow the hierarchical key naming convention
3. Run `npm run validate:i18n:parity` to ensure key consistency
4. Run `npm run validate:i18n:hardcoded` to catch any hard-coded strings
5. Request native speaker review for Ukrainian translations

## References

- Transloco documentation: https://ngneat.github.io/transloco/
- WCAG 2.1 AA guidelines for text alternatives
- Project contributing guide: [CONTRIBUTING.md](../CONTRIBUTING.md)
