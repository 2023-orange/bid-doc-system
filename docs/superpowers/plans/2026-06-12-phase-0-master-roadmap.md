# Bid Doc System Phase 0 Master Roadmap

> For agentic workers: execute this plan in phases and obey `AGENTS.md`. Do not infer completion from docs or package names; inspect code before each edit.

## Goal

Close the first-phase business loop: local API acceptance, bid project management, project checklist, document lifecycle, and approval writeback.

## Execution Order

1. Phase 1: validate and stabilize existing backend capabilities.
2. Phase 2: add the bid project module.
3. Phase 3: add checklist templates and project checklist items.
4. Phase 4: extend document metadata and lifecycle state.
5. Phase 5: connect approvals to document/checklist state writeback.

## Guardrails

- Keep the modular monolith.
- Do not introduce MQ, ES, OCR, Office conversion, external share links, or a BPM engine.
- Keep dev Flyway disabled; provide SQL for manual database updates.
- Add Chinese comments for important business rules, permission checks, and transaction consistency.
- Update API docs and smoke scripts when APIs change.
- Prefer service-layer transactions for multi-table writes.

## Done Criteria

- `mvnw.cmd -DskipTests package` succeeds.
- Core service tests for project, checklist, document lifecycle, and workflow writeback pass.
- SQL files exist for every new table or schema change.
- Smoke script documents how to test the core flow locally.
- Final report lists unvalidated items and remaining risks.
