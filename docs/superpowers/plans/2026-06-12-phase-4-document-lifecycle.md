# Phase 4 Document Lifecycle Plan

## Goal

Extend document records into formal bid materials with document number, metadata, status, sensitivity, owner department, validity dates, and searchable lifecycle filters.

## Main APIs

- `PUT /api/v1/documents/{id}/metadata`
- `GET /api/v1/documents/{id}/metadata`
- `POST /api/v1/documents/{id}/submit-approval`
- `POST /api/v1/documents/{id}/void`
- `POST /api/v1/documents/{id}/restore`
- `GET /api/v1/documents/bindable`

## Main Data

- Add lifecycle columns to `doc_document`.
- Add minimal business category and sensitive level tables only if no existing dictionary can be reused.

## Rules

- Upload creates a document in `INCOMPLETE` state.
- Completed metadata moves the document to `READY_SUBMIT`.
- Approval moves document through `APPROVING`, `APPROVED`, and `REJECTED`.
- Metadata changes do not create file versions.
- Replacing file content still creates a file version.
- Voided or deleted documents cannot be bound to new checklist items.
- Expired documents cannot satisfy checklist completion.

## Verification

- Service tests cover upload default state, metadata completion, version preservation, void restriction, and filters.
- `mvnw.cmd -DskipTests package` succeeds.
