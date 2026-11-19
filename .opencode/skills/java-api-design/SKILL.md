---
name: java-api-design
description: REST/API design standards for this project's Spring Boot backend — resource/URL design, HTTP methods, status codes, RFC 9457 errors, versioning, pagination, idempotency. Load when implementing or reviewing controllers/endpoints under backend/.
---

# API Design Standards

## Resource & URL Design

- Model resources as **nouns, not verbs**: `POST /orders`, `GET /orders/{id}`, not `POST /createOrder`
- Use **plural nouns** for collection endpoints
- Limit nesting to two levels; child resources must be genuinely subordinate
- Use **kebab-case** for multi-word path segments: `/order-items`
- Query parameters for filtering, sorting, pagination only — never for state-changing intent
- Resource IDs should be **opaque** (UUIDs, not sequential integers)

> Project note: all endpoints are versioned under `/api/v1/` (see `AGENTS.md`).

---

## HTTP Methods

- **GET must never mutate state.**
- **PUT implies full replacement.** Use PATCH for partial updates.
- **DELETE must be idempotent.** Deleting an already-deleted resource should return success (or 404).
- Use **POST for operations that do not map cleanly to CRUD**: name the sub-resource after the outcome, not the action.

---

## Status Codes

Return the most semantically precise code:

- Never return 200 with error payload
- Never return 500 for client mistakes (return 4xx)
- Never expose internal details in 5xx responses
- Return 404 rather than 403 when resource existence is sensitive

---

## Request Design

- Accept only required fields; never allow setting server-managed state
- Require `Content-Type` header for all requests with a body
- Require `X-Request-ID` header or generate and echo it back
- Use ISO 8601 for all dates: `2024-03-15T10:30:00Z`
- Use lowercase `camelCase` for all JSON field names

---

## Response Design

- Return minimum necessary information; never expose internal IDs or details
- Be consistent: same field names across all endpoints
- Return wrapped collections, never bare arrays: `{ "items": [...], "total": 142 }`
- Include `Location` header on every 201 response
- Echo `X-Request-ID` in response headers

---

## Error Response (RFC 9457)

Every error must follow this structure:

```json
{
  "type": "https://errors.example.com/validation-failed",
  "title": "Validation Failed",
  "status": 422,
  "detail": "Description specific to this occurrence",
  "instance": "/v1/orders",
  "timestamp": "2024-03-15T10:30:00Z",
  "correlationId": "request-id-here",
  "errors": [{ "field": "name", "code": "REQUIRED", "message": "..." }]
}
```

- One structure for all errors; consumers never parse differently by status code
- `type` and error codes are permanent contracts — never rename within a version
- `title` is static; `detail` is dynamic and specific to this error
- Never expose stack traces, SQL errors, or internal addresses
- For 5xx: expose only base fields; log full details server-side
- Omit `rejectedValue` for sensitive fields (credentials, tokens, PII)

---

## Versioning

- Version from the first release: `/v1/`, `/v2/`
- Never make breaking changes within a version

**Breaking (require new version):** rename/remove fields or endpoints, change types/formats, add required fields, remove enum values

**Non-breaking (safe within version):** add optional response fields, add optional request fields with defaults, add new endpoints

- Maintain at least one previous version in parallel
- Communicate deprecation via `Deprecation` and `Sunset` headers

---

## Pagination

- Paginate all collection endpoints with unbounded results
- Use cursor-based pagination for large/frequently-changing datasets; offset-based for small/stable
- Apply default and maximum page size; reject oversized requests with 400

---

## Idempotency

- Support `Idempotency-Key` header on mutation endpoints with real-world side effects (payments, orders, notifications)
- Deduplicate within the validity window
- Reject reused keys with different payloads (422)

---

## Security

- Enforce auth at the API boundary, never in business logic
- Strip internal details from responses (database IDs, stack traces, service names)
- Apply rate limiting to all endpoints; return 429 with `Retry-After`
- Use encrypted transport only (HTTPS/TLS)

---

## Documentation

- Maintain OpenAPI contract generated from implementation; never manually written
- Document all fields, error codes, with realistic examples
- Publish changelog with each version
