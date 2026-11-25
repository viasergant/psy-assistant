# ADR 002 — Encryption of data at rest and in transit

**Status:** Accepted
**Date:** 2026-11-25

## Context

psy-assistant stores special-category data (mental-health records). Applicable baseline: Ukrainian Law No. 2297-VI on personal data protection and medical secrecy (Art. 40 of the Fundamentals of Healthcare Legislation); GDPR-level controls are a design target for the future (draft law 8153), not a current obligation. Neither Ukrainian law nor the Ombudsman's Model Procedure prescribes a specific cipher; the duty is "appropriate protection" plus access control. A lawyer should confirm duties specific to psychologists vs. clinics and cross-border rules for the chosen cloud region.

## Decision

Three layers.

1. **Infrastructure (operator checklist)**
   - Managed Postgres / volume encryption at rest, including snapshots and backups, with provider-managed keys (KMS).
   - TLS to Postgres: `DB_SSLMODE=verify-full` plus `DB_SSLROOTCERT` for the provider CA (default `require`).
   - Application secrets (`JWT_SECRET`, encryption keys) in the provider secret manager, not next to the app.
2. **Field-level encryption of clinical free text** — JPA converters backed by `FieldEncryptor` (AES-256-GCM, fresh 12-byte IV, 128-bit tag).
   - `EncryptedTextConverter`: columns that used to be plaintext (reads pass un-prefixed values through until backfilled).
   - `EncryptedStringConverter`: columns already encrypted before versioning (session notes, email log); un-prefixed values are legacy ciphertext decrypted with `k1`.
3. **Key versioning and rotation** — stored format `enc:<keyId>:Base64(IV||ciphertext)`.

### Deliberately NOT encrypted
- `full_name`, `email`, `phone`, DOB, address: client search uses `ILIKE` and a `tsvector`; protected by layer 1 and access control. Blind indexes were rejected because they only support exact match.
- Billing notes, templates, therapist bio: not clinical. Revisit if billing notes may contain health details.
- Not implemented: GCM associated data (table.column). A JPA `AttributeConverter` has no column context; it would need Hibernate `UserType`s.

## Runbooks

**Enable on an existing database:** deploy, set `app.encryption.backfill.enabled=true` (env `APP_ENCRYPTION_BACKFILL_ENABLED=true`) for one start, check logs (counts per column, never values), unset it. Take a DB snapshot first.

**Rotate a key:** generate `openssl rand -base64 32`; add `APP_ENCRYPTION_KEYS_K2=<key>` and `APP_ENCRYPTION_ACTIVE_KEY_ID=k2`; restart; run the backfill once (re-encrypts everything to `k2`); keep `k1` configured until `SELECT count(*) ... WHERE col NOT LIKE 'enc:k2:%'` is zero for all columns in `EncryptionBackfillRunner.COLUMNS`, then it may be removed from config. `k1` is `SESSION_NOTES_ENCRYPTION_KEY` and must stay set while the property is required by the app.

**Loss of a key makes the data it protects unrecoverable** — back keys up separately from the database.

## Consequences
- Encrypted fields cannot be searched or sorted in SQL; add such features via application-side filtering or a new blind index.
- Ciphertext is ~35% longer than plaintext (all affected columns are `TEXT`).
- Audit tables copying old/new values are encrypted too.
- Follow-ups: KMS envelope encryption; encrypting more fields if the data classification changes.
