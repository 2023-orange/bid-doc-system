# Phase 3 Project Checklist Plan

## Goal

Add checklist templates and project checklist item tracking under the `project` module.

## Main APIs

- `POST /api/v1/checklist-templates`
- `PUT /api/v1/checklist-templates/{id}`
- `GET /api/v1/checklist-templates/{id}`
- `GET /api/v1/checklist-templates`
- `DELETE /api/v1/checklist-templates/{id}`
- `POST /api/v1/checklist-templates/{id}/items`
- `POST /api/v1/projects/{projectId}/checklist/generate`
- `GET /api/v1/projects/{projectId}/checklist`
- `PATCH /api/v1/projects/{projectId}/checklist/items/{itemId}/owner`
- `POST /api/v1/projects/{projectId}/checklist/items/{itemId}/documents`
- `DELETE /api/v1/projects/{projectId}/checklist/items/{itemId}/documents/{documentId}`

## Main Data

- `bid_checklist_template`
- `bid_checklist_template_item`
- `bid_project_checklist_item`
- `bid_project_checklist_document`

## Rules

- Templates are maintained by super admin.
- Project owners generate checklist copies from templates.
- Template changes do not affect generated project checklist items.
- Binding a document checks project permission and document visibility.
- Checklist item status is recalculated after binding, unbinding, approval, and project archive.

## Verification

- Service tests cover template copy, independent project snapshots, binding permission, and status calculation.
- `mvnw.cmd -DskipTests package` succeeds.
