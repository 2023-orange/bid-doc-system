# Phase 2 Project Module Plan

## Goal

Add bid project basics: project creation, generated project number, members, stages, status, permission-filtered list/detail, audit, and notifications.

## Main APIs

- `POST /api/v1/projects`
- `PUT /api/v1/projects/{id}`
- `GET /api/v1/projects/{id}`
- `GET /api/v1/projects`
- `POST /api/v1/projects/{id}/members`
- `DELETE /api/v1/projects/{id}/members/{userId}`
- `PATCH /api/v1/projects/{id}/stage`
- `PATCH /api/v1/projects/{id}/status`

## Main Data

- `bid_project`
- `bid_project_member`
- `bid_project_no_sequence`

## Rules

- Project number is `DEPTABBR-YYYYMMDD-001`.
- Project requires at least one owner.
- Super admin can manage all projects.
- Project owners can manage their own projects.
- Ordinary users can only view projects they participate in.
- Create, update, member changes, stage changes, and status changes write audit records.
- Added members receive notifications.

## Verification

- Service tests cover number generation, owner-required validation, permission filtering, and stage changes.
- `mvnw.cmd -DskipTests package` succeeds.
