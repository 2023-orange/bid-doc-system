# Local Database Setup

## Purpose

The dev profile keeps Flyway automatic migration disabled while the schema is still changing quickly. Apply SQL manually to local PostgreSQL database `bid_doc_system` when testing backend APIs.

## Recommended Order

Run SQL files in this order for a fresh local database:

1. `src/main/resources/db/migration/V1__sys_base.sql`
2. `src/main/resources/db/migration/V2__user_role_enhancement.sql`
3. `src/main/resources/db/migration/V3__placeholder.sql`
4. `src/main/resources/db/migration/V4__folder_audit_init.sql`
5. `src/main/resources/db/migration/V5__auth_user_role_nullable.sql`
6. `src/main/resources/db/migration/V100__document_init.sql`
7. `src/main/resources/db/migration/V101__search_history_init.sql`
8. `src/main/resources/db/migration/V102__download_log_init.sql`
9. `src/main/resources/db/migration/V103__tags_notify_workflow_init.sql`
10. `src/main/resources/db/migration/V104__project_init.sql`
11. `src/main/resources/db/migration/V105__project_checklist_init.sql`
12. `src/main/resources/db/migration/V106__document_lifecycle_init.sql`
13. `src/main/resources/db/migration/V107__workflow_business_writeback.sql`

## V103-V107 Dependency Notes

- `V103__tags_notify_workflow_init.sql` creates `doc_tag`, `doc_document_tag`, `sys_notification`, `wf_approval_instance`, and `wf_approval_task`.
- `V104__project_init.sql` creates project base tables and can run after the existing auth/folder/document base schema exists.
- `V105__project_checklist_init.sql` creates checklist template and project checklist tables. It depends on project semantics from `V104`, but does not alter `V104` tables.
- `V106__document_lifecycle_init.sql` alters `doc_document`, so it must run after `V100__document_init.sql`.
- `V107__workflow_business_writeback.sql` alters `wf_approval_instance` and `wf_approval_task`, so it must run after `V103`.

For the current dev strategy, apply `V103` before `V107`. If a local database already has `V104-V106` but missed `V103`, run `V103` first and then rerun `V107`.

Project number generation requires the owning department to have `extension_data.abbr`, for example:

```sql
update sys_department
set extension_data = coalesce(extension_data, '{}'::jsonb) || '{"abbr":"ZHGL"}'::jsonb
where id = 3001000000000000001;
```

## Smoke Script Data

`scripts/verify-next-phase-api.ps1 -RunEndToEnd` writes test data through HTTP APIs:

- `bid_project`
- `bid_project_member`
- `bid_checklist_template`
- `bid_checklist_template_item`
- `bid_project_checklist_item`
- `bid_project_checklist_document`
- `doc_document`
- `doc_document_version`
- `wf_approval_instance`
- `wf_approval_task`
- `sys_notification`
- `audit_operation_log`
- local storage files under the configured storage root

Use `-TestDataPrefix` to make generated names searchable, for example:

```powershell
.\scripts\verify-next-phase-api.ps1 `
  -BaseUrl "http://localhost:8080" `
  -AdminUsername "admin" -AdminPassword "12345678" `
  -UserUsername "wang_engineer" -UserPassword "12345678" `
  -OwnerUsername "zhang_manager" -OwnerPassword "12345678" `
  -OutsiderUsername "chen_quality" -OutsiderPassword "12345678" `
  -FolderId "3003000000000000004" `
  -EndToEndFolderId "3003000000000000005" `
  -DocumentId "3004000000000000001" `
  -RunEndToEnd `
  -TestDataPrefix "E2E-CLOSE" `
  -CleanupEndToEndData
```

When `-CleanupEndToEndData` is supplied, the script uses `psql` and the current `PGPASSWORD` environment variable to soft-delete the generated project, checklist, documents, approval instances, approval tasks, and notifications. Audit rows and physical local-storage files are intentionally not removed by the script, so the audit trail remains inspectable.

## Notes

- Do not enable Flyway in dev until migration ordering is reviewed.
- Do not commit local database passwords.
- Seed data should be kept in `scripts/seed-realistic-dev-data.sql` or a separate script with clear comments.
