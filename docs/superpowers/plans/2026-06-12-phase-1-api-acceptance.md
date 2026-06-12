# Phase 1 API Acceptance Plan

## Goal

Validate the existing auth, folder, document, tag, audit, notification, and lightweight approval APIs against local PostgreSQL.

## Tasks

- Confirm `application-dev.yml` keeps Flyway disabled and does not contain real secrets.
- Provide local SQL setup instructions in `docs/local-db-setup.md`.
- Extend `scripts/verify-next-phase-api.ps1` with parameterized checks for login, current user, folders, documents, audit, notifications, and approvals.
- Fix only blocking runtime issues discovered while validating existing capabilities.

## Verification

- Run `mvnw.cmd -DskipTests package`.
- Run the smoke script when local credentials and IDs are available.
- Record any skipped manual checks in the final report.
