# Project Guidelines

## Scope

- This repository is a monorepo with two main applications:
	- backend: Spring Boot 3.4.x, Java 21, Maven, PostgreSQL, Flyway
	- frontend: Angular 20 standalone architecture, PrimeNG, Transloco
- Use this root guidance for cross-repo tasks. When a task is app-specific, follow the app README first.

## Build And Test

- Backend setup and local run:
	- Do not use Docker
	- Start DB: cd backend && docker compose up -d
	- Run app (local profile): cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
	- Validate build, tests, and style checks: cd backend && ./mvnw verify
- Frontend setup and local run:
	- Install deps: cd frontend && npm install
	- Start dev server: cd frontend && npm start
	- Lint: cd frontend && npm run lint
	- Unit tests: cd frontend && npm test

## Architecture

- Backend package boundaries are feature-oriented under src/main/java/com/psyassistant:
	- common: shared concerns (audit, config, exceptions, security)
	- auth, users, crm, billing, scheduling, sessions, notifications, reporting, integration
- Frontend structure under src/app:
	- core: auth and i18n foundation
	- layout/shell: app chrome and navigation
	- features: lazy-loaded feature areas (leads, clients, schedule, sessions, billing, reports, admin, auth)

## Conventions

- Backend entities should follow the shared audit model from common/audit/BaseEntity.
- Database schema changes must be done through Flyway migration files in src/main/resources/db/migration using V<N>__description.sql naming.
- Keep API contracts explicit with request/response DTOs and consistent error handling through common exception patterns.
- Frontend uses standalone Angular patterns (no NgModules); route and provider changes should align with src/app/app.routes.ts and src/app/app.config.ts.
- **Always use i18n for UI text**: Never hardcode user-facing strings in templates or components. All labels, messages, buttons, headings, placeholders, and error text must use Transloco (e.g., `{{ 't.someKey' | transloco }}`). Add new keys to frontend/src/assets/i18n/*.json following the structure in docs/i18n-key-structure.md. Use frontend/scripts/validate-i18n-*.js to validate i18n compliance before committing.
- Frontend i18n strings belong in frontend/src/assets/i18n/*.json and are loaded via Transloco.
- Do not log personal or sensitive client information. Log opaque identifiers only.

## Pitfalls

- Do not commit secrets. Use environment variables or backend/src/main/resources/application-secrets.properties (git-ignored).
- Outside local development, JWT_SECRET must be provided explicitly.
- Keep Logback config in logback-spring.xml so profile-based logging works correctly.
- JPA DDL auto-create is not the workflow here; Flyway is the source of truth for schema evolution.

## Documentation

- Product and domain scope: [requirements.md](requirements.md)
- Backend runtime and environment details: [backend/README.md](backend/README.md)
- Frontend scripts and structure: [frontend/README.md](frontend/README.md)
- Observability and logging policy: [docs/adr/001-observability.md](docs/adr/001-observability.md)
- Feature specifications: [docs/specs/](docs/specs/)

## Jira

- Jira project name: PSY-ASSISTANT
- Ticket key prefix: PA-