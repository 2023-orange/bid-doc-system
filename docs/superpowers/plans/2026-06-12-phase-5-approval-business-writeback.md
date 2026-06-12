# Phase 5 Approval Business Writeback Plan

## Goal

Enhance the existing lightweight workflow module so approvals update document and checklist business state.

## Main APIs

- `POST /api/v1/documents/{id}/approval/submit`
- `POST /api/v1/documents/{id}/versions/{versionNo}/approval/submit`
- `POST /api/v1/projects/{projectId}/checklist/items/{itemId}/approval/submit`
- `GET /api/v1/approvals/tasks`
- `POST /api/v1/approvals/{id}/approve`
- `POST /api/v1/approvals/{id}/reject`
- `GET /api/v1/documents/{id}/approval/history`
- `GET /api/v1/projects/{projectId}/approval/history`

## Rules

- Document approval submit moves document to `APPROVING`.
- Approve moves document to `APPROVED`.
- Reject moves document to `REJECTED`.
- Checklist approval recalculates checklist item state.
- Only the task approver or super admin can handle an approval task.
- Approval actions write audit records and send notifications.

## Verification

- Service tests cover document approval status transitions, non-approver rejection, checklist recalculation, audit, and notifications.
- `mvnw.cmd -DskipTests package` succeeds.
