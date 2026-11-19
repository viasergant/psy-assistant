# AGENTS.md — psy-assistant

Monorepo for a psychological assistance CRM: `backend/` (Spring Boot 3.4 / Java 21 / Maven) + `frontend/` (Angular 20 standalone, PrimeNG, Transloco). Full conventions live in `docs/features/AGENTS.md`; product scope in `requirements.md`. BMAD process artifacts (`_bmad/`, `_bmad-output/`, `context/`, `plan/`) are not source code.

## Deployment mode (read first)

- The backend **fails fast at startup** unless a `solo` or `clinic` Spring profile is active (`DeploymentModeValidator`). Always run with e.g. `-Dspring.profiles.active=solo,local`.
- `backend/run.sh` = solo+local. `backend/debug.sh` is stale (no mode profile) — add the mode profile before using it.
- Solo: first boot creates one user from `SOLO_USERNAME`/`SOLO_PASSWORD` env vars. Clinic: one-time `POST /api/v1/setup/admin`; admin/caseload controllers are `@Profile("clinic")` and return 404 in solo mode.
- Frontend fetches `GET /api/v1/meta` on boot and gates routes/UI by mode (`modeGuard`, `*appIfMode`).

## Backend commands (run from `backend/`)

- Local DB: `docker compose up -d` (Postgres 16 on localhost:5432).
- Run app: `./mvnw spring-boot:run -Dspring-boot.run.profiles=solo,local`
- ⚠️ The default `DB_URL` in **all** profiles points to `192.168.10.146` (home server), not localhost — the README's "defaults match Docker Compose" is stale. When using docker compose, override: `DB_URL=jdbc:postgresql://localhost:5432/psyassistant`.
- Build + unit tests + Checkstyle: `./mvnw verify`; single test: `./mvnw test -Dtest=ClassName`
- Unit tests run on in-memory H2 with Flyway disabled (`src/test/resources/application.yml`) — no Postgres needed.
- The `local` profile seeds `admin@psyassistant.local` / `Admin1234!` plus a dev therapist via repeatable scripts in `db/local-seed/`.
- Swagger UI (local profile only): http://localhost:8080/swagger-ui.html · health: `/actuator/health`
- Schema changes = new Flyway migration `V<N>__description.sql` in `db/migration`. Never edit an applied migration; JPA `ddl-auto` is validate/none — Flyway owns the schema.
- Checkstyle runs in the `validate` phase and fails on errors: line length ≤ 120, no star imports, import order.
- Outside the local profile, `JWT_SECRET` and `SESSION_NOTES_ENCRYPTION_KEY` (base64-encoded 32 bytes) are required. Secrets go in `application-secrets.properties` (git-ignored) or env vars — never commit them.

## Frontend commands (run from `frontend/`)

- Node 22 (`nvm use`), then `npm install`, `npm start` → http://localhost:4200. The dev server proxies `/api` to localhost:8080 (`proxy.conf.json`) — the backend must be running for API calls.
- `npm run lint` · `npm test` (Karma; requires a local Chrome) · production build: `ng build --configuration production` → `dist/frontend/browser/`.
- i18n: never hardcode user-facing strings — use Transloco keys in `src/assets/i18n/*.json` and run `npm run validate:i18n` (4 checks) before committing.
- UI: reuse shared classes and design tokens from `frontend/src/styles/` (see its README) instead of component-specific CSS. PrimeNG v20 is token-themed only (Aura preset); legacy theme CSS imports are forbidden.

## Integration tests (Python, run from repo root)

- `./scripts/integration-test.sh` — requires a **running backend** and a configured `.env` in `backend/tests/integration/` (copy from `.env.test`; admin credentials = the local seed above).
- Single test: `./scripts/integration-test.sh -k "test_name"`; single file: pass the filename. `external`-marked tests are skipped by default; `-n auto` for parallelism.
- When changing backend logic, add/modify the corresponding integration tests and update `docs/test-scenarios.md`.

## Conventions

- Every change gets an entry under `[Unreleased]` in the root `CHANGELOG.md`.
- Jira: project PSY-ASSISTANT, tickets prefixed `PA-*`; feature branches are named `feature/PA-<n>-slug`. GitHub and Jira MCP servers are configured (`.mcp.json`, git-ignored).
- Never log client PII — opaque identifiers only (`docs/adr/001-observability.md`).
- Deploy: `./scripts/deploy.sh` (`--skip-tests`, `--frontend-only`, `--backend-only`); server provisioning and Caddy/systemd operations are documented in `backend/README.md`.

## Key references

- `docs/features/AGENTS.md` — full project guidelines (conventions, pitfalls)
- `backend/README.md`, `frontend/README.md` — app-specific setup and deployment
- `docs/specs/` — feature specifications · `docs/i18n-key-structure.md` — translation key layout
