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
