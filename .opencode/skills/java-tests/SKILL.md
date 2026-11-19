---
name: java-tests
description: Java test-code standards for this project's Spring Boot backend — AAA pattern, isolation, test doubles, meaningful fixtures, coverage gates. Load when writing or reviewing tests under backend/src/test/java.
---

# Java Test Code Standards

## Structure & Naming

- Follow the **AAA pattern** in every test. Visually separate the three phases with a blank line.
- Name tests to describe the scenario and expected outcome: `shouldExpectedBehavior_whenCondition()`.
- One behaviour per test. Group related tests in nested, named contexts that describe the scenario.

## Isolation & Test Doubles

- Every test must be **fully independent**: no shared mutable state, pass in any order, deterministic.
- Only replace collaborators that cross system boundaries (databases, external services, time, randomness, file system).
- Use the simplest test double that satisfies the test: stub before spy, spy before full mock.
- Only verify interactions when the interaction itself is the intended behaviour; otherwise verify outcomes.

## Test Data

- Construct test data as close to the test as possible. Long test fixtures shared across many tests make it hard to reason about what each test actually needs.
- Use the **Builder** or **Object Mother** pattern to create default valid objects, then vary only the fields relevant to the test.
- Give test data meaningful values, not arbitrary ones. `age = 17` is more informative than `age = 1` when testing a minimum-age rule.
- Avoid hardcoded identifiers that may conflict across tests (e.g., shared database IDs).

## Coverage & Quality Gates

- Coverage below **80% line and branch** for core modules should fail the build.
- Focus coverage on branching logic, edge cases, and error paths — not just the percentage threshold.
- Flaky tests must be fixed or removed immediately.
- Never use fixed `sleep` or delay for asynchronous outcomes. Use polling, callbacks, or async assertion utilities.
- Isolate slow tests (network, containers, external services) with tags or build profiles.

> Project note: unit tests run on in-memory H2 with Flyway disabled (`backend/src/test/resources/application.yml`) — no Postgres needed. Integration tests are separate (Python, `./scripts/integration-test.sh`).
