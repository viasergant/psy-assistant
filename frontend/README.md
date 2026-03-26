# PSY Assistant CRM — Frontend

Angular 20 frontend scaffold for the PSY Assistant CRM system. Standalone-component architecture with lazy-loaded feature routes, PrimeNG UI, JWT HTTP interceptor, Transloco i18n, and auth route guard.

## Local Setup

### Prerequisites

- Node.js LTS (see `.nvmrc` — version 22 recommended)
- npm 10+

```bash
# Install the correct Node version via nvm
nvm use

# Install dependencies
npm install

# Start the development server
npm start
# or
ng serve
```

Application is accessible at http://localhost:4200.

### Production Build

```bash
ng build --configuration production
```

Build artifacts are placed in `dist/frontend/browser/`.

### Linting

```bash
npm run lint
# or
ng lint
```

### Unit Tests

```bash
npm test
# or
ng test
```

## Architecture

### Key Technologies

| Concern | Choice |
|---------|--------|
| Framework | Angular 20 (standalone components, no NgModules) |
| UI Library | PrimeNG 20 with Aura preset (token-based theming) |
| i18n | Transloco (`@jsverse/transloco`) |
| HTTP auth | Functional JWT interceptor (localStorage) |
| Router | `withComponentInputBinding()` enabled |

### Folder Structure

```
src/
  app/
    app.config.ts           -- bootstrap providers (order matters)
    app.routes.ts           -- root lazy routes
    core/
      auth/
        auth.service.ts     -- token helpers
        auth.guard.ts       -- redirect to /auth/login when unauthenticated
        jwt.interceptor.ts  -- adds Authorization: Bearer header
      i18n/
        transloco-loader.ts -- HTTP loader for translation files
    layout/
      shell/
        shell.component.*   -- top toolbar + sidebar nav + router-outlet
    features/
      leads/leads.routes.ts
      clients/clients.routes.ts
      schedule/schedule.routes.ts
      sessions/sessions.routes.ts
      billing/billing.routes.ts
      reports/reports.routes.ts
      admin/admin.routes.ts
      auth/auth.routes.ts   -- no authGuard here; contains /auth/login
  assets/
    i18n/
      en.json               -- default English translations
  styles.scss               -- global styles; primeicons import only
```

### Route Guard

`authGuard` (`CanActivateFn`) is applied to all 7 feature routes (`/leads`, `/clients`, `/schedule`, `/sessions`, `/billing`, `/reports`, `/admin`). It is **not** applied to `/auth` or the root redirect. This prevents redirect loops.

```
No token → navigate to /auth/login
Token present → allow activation
```

### JWT Interceptor

A functional `HttpInterceptorFn` registered via `withInterceptors([jwtInterceptor])` inside `provideHttpClient()`. Reads `localStorage.getItem('access_token')` on every request. If a token is present it clones the request and sets `Authorization: Bearer <token>`. If no token is found the request is forwarded unmodified.

**Security note:** localStorage is accessible to JavaScript and is therefore vulnerable to XSS. This is an accepted trade-off at scaffold stage. Migration to HttpOnly cookies is planned in a future auth story.

### i18n Translation File Structure (`assets/i18n/en.json`)

```json
{
  "app.title": "PSY Assistant CRM",
  "nav.leads": "Leads",
  "nav.clients": "Clients",
  "nav.schedule": "Schedule",
  "nav.sessions": "Sessions",
  "nav.billing": "Billing",
  "nav.reports": "Reports",
  "nav.admin": "Admin"
}
```

Keys follow the `<namespace>.<key>` dot-notation convention. New translations must be added to `en.json` and any additional locale files under `src/assets/i18n/`.

### PrimeNG Theming

PrimeNG v20 uses token-based theming exclusively. Legacy CSS imports from `primeng/resources/themes/` are removed and must not be used.

Theme is configured in `app.config.ts`:
```typescript
providePrimeNG({ theme: { preset: Aura } })
```

Only `primeicons/primeicons.css` is imported in `styles.scss`.

## Out of Scope (this story)

- Actual login/logout UI
- Domain-specific components
- State management (NgRx/Signals)
- Full E2E test suite
