---
name: java-production
description: Java production-code standards for this project's Spring Boot backend — SOLID, naming, layering, error handling, null safety, logging. Load when implementing or reviewing code under backend/src/main/java.
---

# Java Production Code Standards

## Design Principles

- Apply **SOLID** rigorously. Favour **composition over inheritance**. Program to **interfaces**, not concrete implementations.
- Prefer **immutable objects** by default. Allow mutation only when there is a justified reason.
- Avoid anemic domain models: encapsulate behaviour alongside data rather than placing all logic in separate service or utility classes.

## Naming & Readability

- Choose names that reveal intent. Encode behaviour in method names (`calculateTax`, `findActiveOrders`) rather than data shapes (`getData`, `getList`).
- Limit method length to what fits on screen without scrolling. Extract sub-routines with descriptive names when logic grows.

## Architecture & Layering

- Enforce a strict, unidirectional dependency graph between layers. Inner layers must have zero knowledge of outer layers.
- Keep domain and business logic free of framework and infrastructure concerns so it can be tested without a running container.
- Make layer boundaries explicit through dedicated translation objects. Do not let data structures or persistence entities leak across layers unmodified.
- Separate commands (mutations that change state) from queries (reads that return data) — avoid methods that do both.

## Error Handling

- Define domain-specific exception types that convey the business meaning of a failure, not just the technical one.
- Include sufficient context in every exception message for an engineer to diagnose the problem without a debugger.
- Distinguish between recoverable errors (callers can handle) and programming errors (indicate a bug); treat them differently.

## Null Safety & Defensive Coding

- Express nullability explicitly through API design, return-type semantics, or documented contracts — not through silent convention.
- Validate all inputs at system boundaries (controllers, message consumers, public APIs). Fail fast with a clear message.

## Logging

- Write log messages for the on-call engineer at 3 AM: include what happened, the relevant identifiers, and what it implies.
- Never log credentials, tokens, secrets, or personally identifiable information (this project logs opaque identifiers only — see `docs/adr/001-observability.md`).
- Attach a correlation or trace identifier to every log statement involved in processing a single request or transaction.
