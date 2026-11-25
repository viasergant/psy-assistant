# Psy-Assistant Backend

Spring Boot 3.x / Java 21 backend for the Psychological Assistance CRM.

## Technology stack

- Java 21
- Spring Boot 3.x
- Spring Security (stateless JWT skeleton)
- Spring Data JPA + Hibernate
- PostgreSQL 16
- Springdoc OpenAPI 2.x (Swagger UI)
- Maven

## Project structure

```
backend/
├── src/main/java/com/psyassistant/
│   ├── PsyAssistantApplication.java
│   ├── common/
│   │   ├── audit/          – BaseEntity, AuditingConfig
│   │   ├── exception/      – GlobalExceptionHandler, ErrorResponse
│   │   └── config/         – SecurityConfig, OpenApiConfig
│   ├── users/
│   ├── clients/
│   ├── scheduling/
│   ├── sessions/
│   ├── billing/
│   ├── notifications/
│   ├── reporting/
│   └── integration/
├── src/main/resources/
│   ├── application.yml
│   ├── application-local.yml
│   └── application-prod.yml
├── checkstyle/checkstyle.xml
├── docker-compose.yml
└── pom.xml
```

## Running locally

1. Start PostgreSQL: `docker compose up -d`
2. Run the app: `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
3. Health check: `curl http://localhost:8080/actuator/health`
4. API docs: http://localhost:8080/swagger-ui.html

## Environment variables

| Variable      | Default                                  | Description              |
|---------------|------------------------------------------|--------------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5432/psyassistant` | JDBC connection URL |
| `DB_USERNAME` | `psyassistant`                           | Database username        |
| `DB_PASSWORD` | `psyassistant`                           | Database password        |
| `JWT_SECRET`  | local profile only: built-in dev fallback | JWT signing secret (required outside local dev unless overridden in config) |
| `SESSION_NOTES_ENCRYPTION_KEY` | local profile only: built-in dev fallback | Base64 32-byte AES-256-GCM key (key id `k1`) for field-level encryption of clinical text. Generate with: `openssl rand -base64 32`. See [ADR 002](../docs/adr/002-encryption-at-rest.md) for rotation |
| `DB_SSLMODE` | `require` (prod) | Postgres TLS mode; use `verify-full` with `DB_SSLROOTCERT` for a cloud provider |
| `APP_ENCRYPTION_ACTIVE_KEY_ID` / `APP_ENCRYPTION_KEYS_<ID>` | `k1` | Key rotation: add key `<id>` and make it active, then run the backfill |

For local development, the defaults provided by `application.yml` and
`application-local.yml` match the Docker Compose service configuration, and the
`local` profile also supplies a dev-only JWT secret fallback so no extra setup is
required.

Sensitive credentials must never be committed. Place them in
`src/main/resources/application-secrets.properties` (git-ignored) or supply them as
environment variables.

## Build

```bash
./mvnw verify
```

This runs compilation, tests, and Checkstyle validation.

## Profiles

| Profile | Purpose                                      |
|---------|----------------------------------------------|
| (none)  | Base config; datasource from env vars        |
| `local` | PostgreSQL via Docker Compose, Swagger UI enabled, Flyway-managed DDL |
| `prod`  | Swagger UI disabled, tighter logging         |

---

## Production (remote server) setup

This section describes how to provision a fresh Linux server and deploy the application for the first time.

### Prerequisites

**Local machine:**
- `ssh`, `rsync`, `scp`
- Java 21 + Maven wrapper (`backend/mvnw`)
- Node.js 22 + npm

**Remote server:**
- Ubuntu 22.04 / Debian 12 (or RHEL-compatible) with `sudo` access
- SSH key-based auth for the deploy user
- PostgreSQL already running and accessible (see `DB_URL` below)

### Step 1 — Provision the server (one-time)

```bash
# Default deploy user is "deploy"; override with env vars if needed
DEPLOY_USER=deploy DEPLOY_SSH_KEY=~/.ssh/id_ed25519_psy ./scripts/setup-server.sh
```

This script (idempotent, safe to re-run) installs:
- Java 21 (Eclipse Temurin)
- Caddy web server with SPA routing and API reverse-proxy config
- A systemd service unit (`psy-assistant`)
- Required directories and a sudoers rule for the deploy user

### Step 2 — Fill in secrets

After provisioning, SSH into the server and edit the env file that was created:

```bash
ssh deploy@192.168.10.128
sudo nano /opt/psy-assistant/psy-assistant.env
```

Set every `CHANGE_ME` value:

| Variable | Description | How to generate |
|---|---|---|
| `DB_PASSWORD` | PostgreSQL password | — |
| `JWT_SECRET` | JWT signing secret (min 32 chars) | `openssl rand -base64 48` |
| `SESSION_NOTES_ENCRYPTION_KEY` | AES-256-GCM key for session-note encryption | `openssl rand -base64 32` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated list of frontend origins (no trailing slash) | e.g. `https://psy-assistant.tail1cbdbb.ts.net` |

> **Important:** `CORS_ALLOWED_ORIGINS` must include every hostname the frontend is served from.
> If the value is wrong or missing, `/api/v1/auth/login` will return **403 Forbidden**.

Example `/opt/psy-assistant/psy-assistant.env`:

```dotenv
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://192.168.10.146:5432/psyassistant_prod
DB_USERNAME=psyassistant
DB_PASSWORD=<strong-password>
JWT_SECRET=<base64-48-bytes>
SESSION_NOTES_ENCRYPTION_KEY=<base64-32-bytes>
CORS_ALLOWED_ORIGINS=https://psy-assistant.tail1cbdbb.ts.net
```

After editing, restart the service:

```bash
sudo systemctl restart psy-assistant
sudo systemctl status psy-assistant
```

### Step 3 — Deploy

Run from the project root on your local machine:

```bash
# Full deploy (builds frontend + backend, uploads, restarts)
./scripts/deploy.sh

# Skip tests for a faster iteration
./scripts/deploy.sh --skip-tests

# Deploy only the frontend (e.g. after a UI-only change)
./scripts/deploy.sh --frontend-only

# Deploy only the backend JAR
./scripts/deploy.sh --backend-only
```

Override the deploy user or SSH key without editing the script:

```bash
DEPLOY_USER=ubuntu DEPLOY_SSH_KEY=~/.ssh/id_ed25519 ./scripts/deploy.sh --skip-tests
```

### Ongoing operations

```bash
# Tail application logs
ssh deploy@192.168.10.128 'tail -f /var/log/psy-assistant/app.log'

# Check service status
ssh deploy@192.168.10.128 'sudo systemctl status psy-assistant'

# Restart service manually
ssh deploy@192.168.10.128 'sudo systemctl restart psy-assistant'

# View Caddy logs
ssh deploy@192.168.10.128 'sudo journalctl -u caddy -f'
```

### Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| Page refresh returns **404** | Caddy missing SPA fallback config | Re-run `setup-server.sh` or manually upload `scripts/Caddyfile` |
| Login / refresh returns **403** | `CORS_ALLOWED_ORIGINS` doesn't include the frontend origin | Edit `/opt/psy-assistant/psy-assistant.env`, restart service |
| Backend health check fails | Service not started or wrong DB credentials | Check `app.log`, verify `psy-assistant.env` values |
| `rsync: Permission denied` | Deploy user lacks write access to `/var/www/html` | Re-run `setup-server.sh` to restore sudoers and directory permissions |
