# AGENTS.md

## Scope

- This file is for coding agents working in this repository.
- It defines stable engineering rules, module routing rules, and safety constraints.
- It is not a project progress tracker.
- Do not use this file to infer current module completion.

Interpretation rules:
- Only treat code, config, build files, and actual repository contents as current facts.
- Do not treat design docs, SQL drafts, empty packages, or directory names alone as proof that functionality exists.
- Before changing behavior, inspect the current code in the relevant module.
- If something cannot be confirmed from the repository, say so explicitly instead of inferring it.

## Ambiguity And Unknowns Hard Rules

These rules override convenience, speed, or inferred intent.

- If the request, repository state, expected behavior, business rule, module boundary, data model, configuration, acceptance criteria, or implementation target is ambiguous, missing, contradictory, or cannot be verified, stop and ask the user a concrete clarification question before implementing or documenting it.
- Do not guess, assume, invent, or fill gaps with imagined facts, designs, APIs, files, frameworks, behaviors, requirements, commands, database schema, or module status.
- Do not silently choose between multiple plausible interpretations. State the options clearly and ask the user to decide.
- If work can continue safely without the ambiguous part, clearly separate confirmed work from blocked or uncertain work, and proceed only within the confirmed scope.
- If an assumption is unavoidable for analysis only, label it explicitly as an assumption and do not implement changes based on it without user confirmation.

## Project Type

- This repository is a modular monolith for a document management system.
- The repository currently contains a Java backend Maven project.
- The system is intended for frontend-backend separation, but the actual frontend state must always be confirmed from repository files before making claims.
- `auth` and `folder` are key business modules and should be checked first for related work.

Key paths:
- [pom.xml](pom.xml)
- [src/main/java/com/example/biddoc](src/main/java/com/example/biddoc)
- [src/main/resources](src/main/resources)

## Architecture Strategy

- Keep the system as a modular monolith by default.
- Prefer clear module boundaries inside one deployable application.
- Do not default to microservices, RPC, or distributed transactions.
- Prefer interfaces, adapters, and internal service boundaries before introducing infrastructure.
- Do not add complexity for hypothetical future scale alone.

When proposing new middleware or infrastructure, explain:
- why it is needed
- what concrete problem it solves
- what benefit it brings
- what complexity and maintenance cost it adds
- whether it is necessary now
- what lighter in-process or monolith-local alternative exists

Repository-specific guidance:
- Redis is already present as a dependency and config surface.
- Message queues, object storage, full-text search engines, and schedulers must not be treated as implemented unless confirmed by code or build files.

## Tech Stack

Confirmed from repository files:
- Language: Java 17
- Framework: Spring Boot
- Web: Spring MVC
- Validation: Jakarta Validation
- AOP: Spring AOP
- Build tool: Maven Wrapper
- Data access: MyBatis-Plus
- Database: PostgreSQL
- Cache / session-related dependency: Redis
- Authentication / authorization: Sa-Token

Agent rules:
- Treat the stack as repository-derived, not assumption-derived.
- If future files introduce new frameworks or tools, verify them before documenting them as facts.
- If frontend files are absent, do not invent frontend framework, build tool, package manager, directory structure, or commands.

Evidence:
- [pom.xml](pom.xml)
- [src/main/resources/application.yml](src/main/resources/application.yml)

## Repository Structure

Core backend paths:
- Java source: [src/main/java/com/example/biddoc](src/main/java/com/example/biddoc)
- Resources and config: [src/main/resources](src/main/resources)
- Tests: [src/test/java](src/test/java)

Build and root files:
- Maven config: [pom.xml](pom.xml)
- Maven wrapper: [mvnw](mvnw), [mvnw.cmd](mvnw.cmd)
- Agent guide: [AGENTS.md](AGENTS.md)

Reference-only or verify-before-use paths:
- Docs: [docs](docs)
- SQL references: [docs/sql](docs/sql)
- Architecture docs: [docs/architecture](docs/architecture)
- Docker-related directory: [docker](docker)
- Build output: [target](target)

Agent rules:
- Do not treat `target/` as source of truth.
- Do not infer business capability from documentation alone.
- Do not infer module completeness from the existence of a package or directory alone.

## General Engineering Rules

- Do not perform unrelated refactors.
- Do not format unrelated files.
- Keep changes small, local, and explainable.
- Do not casually modify production-facing configuration.
- Do not casually change existing SQL semantics or migration-like scripts.
- Do not commit or output secrets, tokens, passwords, private keys, or sensitive connection values.
- Do not log passwords, tokens, secrets, file contents, or sensitive payloads.
- Before adding a dependency, explain the reason, value, cost, and simpler alternatives.
- If a repository rule is not consistent yet, state that directly instead of pretending it is settled.
- Never promote an unverified inference into a claimed current fact.
- If behavior matters, inspect the implementation before changing it.

## Code Comment Rules

- When writing or modifying code, add necessary comments for the main logic and important code paths.
- Comments must be written in Chinese.
- Important code that should be commented includes, but is not limited to:
    - core business logic
    - complex conditional branches
    - permission or authorization checks
    - transaction boundaries and multi-step data consistency logic
    - non-obvious data conversion or mapping logic
    - complex queries, algorithms, or performance-sensitive code
    - code that prevents invalid, unsafe, or destructive behavior
- Comments should explain why the code exists and what key rule it protects, not merely repeat what the code literally does.
- Do not add noisy comments for obvious single-line code, simple getters/setters, imports, annotations, or trivial assignments.
- If existing comments are inaccurate, outdated, or misleading, update them instead of adding duplicate comments.


## Backend Engineering Rules

Common repository conventions should be confirmed from current code before modification. This repository commonly uses:
- `controller`
- `service`
- `service.impl`
- `mapper`
- `entity`
- `dto.req`
- `dto.resp`
- `convertor`
- `constant`
- `common`

Controller rules:
- Own HTTP endpoints, request parsing, validation, and response shaping.
- Do not put complex persistence or cross-module orchestration in controllers.

Service rules:
- Own business logic, multi-step coordination, and transactional boundaries.
- If a module already uses service interfaces plus `service.impl`, follow the existing style unless there is a clear repo-wide reason to change it.

Mapper rules:
- `mapper` is the repository's persistence naming convention.
- Prefer MyBatis-Plus style when extending current persistence logic.
- Introduce XML SQL only when genuinely needed for complex queries.

Entity / DTO / VO rules:
- `Entity` is the persistence model.
- `ReqDTO` is the request contract.
- `RespDTO` is the response contract.
- Do not invent `VO` as a repository convention unless actual code establishes it or the change introduces it consistently.

Convertor rules:
- If a module already uses `convertor`, keep mapping logic there instead of scattering conversions across controllers and services.

Response and exception rules:
- Prefer the repository's unified response pattern.
- Prefer central exception handling over controller-local ad hoc error wrapping.
- Reuse existing business exception and error code patterns where present.

Validation rules:
- Prefer request validation close to request DTOs.
- Reuse the repository's existing validation style before introducing another one.

Transaction rules:
- Put multi-write consistency logic in the service layer.
- Do not spread transaction-sensitive logic across controllers.

Auth integration rules:
- Reuse the repository's current auth chain and current-user context mechanism.
- If current-user context is needed, prefer repository-provided context or session mechanisms over manual header parsing.

Logging rules:
- Reuse existing request tracing and MDC-style patterns if present.
- Never add sensitive data to logs.


Reference paths:
- [src/main/java/com/example/biddoc/auth](src/main/java/com/example/biddoc/auth)
- [src/main/java/com/example/biddoc/common](src/main/java/com/example/biddoc/common)

## Commands

Backend run:
```bash
./mvnw spring-boot:run
```

Backend run on Windows:
```bash
mvnw.cmd spring-boot:run
```

Backend build:
```bash
./mvnw clean package
```

Backend build on Windows:
```bash
mvnw.cmd clean package
```

Backend test:
```bash
./mvnw test
```

Backend test on Windows:
```bash
mvnw.cmd test
```

Command rules:
- These commands are supported by the Maven project structure and wrapper files.
- Do not treat the existence of `mvn test` as proof of meaningful test coverage.
- Do not invent frontend, lint, format, or migration commands unless actual repository files support them.

## Definition Of Done

For any completed task, report:
- what changed
- why it changed
- impacted scope
- files modified
- whether tests were run
- exact test command(s) used, if any
- test result(s)
- what was not validated
- potential risks or follow-up items

Do not:
- claim unverified behavior is complete
- claim tests passed if they were not run
- hide missing verification
- describe a recommendation as if it were implemented

If the task was analysis-only:
- say no code was modified
- say no file was written if none was written
- say no tests were run if none were run

## Module Boundaries

### auth

`auth` is the default module for changes related to:
- registration
- login
- logout
- current-user lookup
- login state
- users
- roles
- departments
- user-role relationships
- auth-related interceptors and current-user context integration

Before changing `auth`:
- inspect the current code in [src/main/java/com/example/biddoc/auth](src/main/java/com/example/biddoc/auth)
- inspect shared auth wiring in [src/main/java/com/example/biddoc/common](src/main/java/com/example/biddoc/common)
- do not freeze old observations from this file as if auth behavior never changes

### folder

`folder` is the default module for changes related to:
- folder hierarchy
- parent-child relationships
- ancestor chain handling
- folder ordering
- ownership
- grants
- managers
- favorites
- folder-specific permission semantics

Folder is an active module.

Before changing `folder`:
- inspect the current code in [src/main/java/com/example/biddoc/folder](src/main/java/com/example/biddoc/folder)
- do not judge module completion from directory names alone
- do not judge module completion from empty packages alone
- do not judge module completion from design docs or SQL drafts alone

When implementing or modifying folder behavior, preserve:
- hierarchy integrity
- parent-child correctness
- ancestor chain correctness
- level or depth consistency if present
- sort or order consistency
- ownership semantics
- permission inheritance semantics
- soft delete semantics
- prevention of circular references
- prevention of illegal self or descendant moves

Folder mutation rules:
- Tree mutation logic must consider descendants, not only the current node.
- Rename and create logic must consider sibling-level uniqueness if enforced by code or schema.
- Delete logic must explicitly account for descendants and related records.
- Move logic must explicitly account for ancestry rewrite and invalid target detection.
- Permission-related changes must consider inheritance and explicit grants together.

## Multi-module Routing Rules

Current-routing guidance:
- login, register, token or session, current user, user, role, department changes: start in `auth`
- folder hierarchy, rename, move, delete, ownership, grants, manager or favorite behavior: start in `folder`
- request auth chain, current-user context, interceptors, shared exceptions, shared response patterns: start in `common` and then trace into the owning business module

Future-routing guidance only:
- document metadata, lifecycle, versions: `document`
- file upload, download, and storage adapters: `storage`
- unified access control and permission evaluation: `permission`
- permission-filtered indexing and querying: `search`
- preview, thumbnails, OCR, format conversion: `preview`
- audit records and operation logging flows: `audit`
- share links and external access control: `share`

Rule:
- Future-routing guidance is not evidence that the module already exists.
- Do not create all future modules preemptively.

## Future Module Boundary Suggestions

- The top-level package structure under `src/main/java/com/example/biddoc` is already established.
- Prefer extending existing top-level packages before introducing new top-level business packages.
- Do not create new top-level modules only because a concept appears in architecture discussion.
- Before adding a new top-level package, explain why the capability cannot belong cleanly to an existing package.

Current top-level package anchors:
- `auth`: user, role, department, login state, identity-related access rules
- `folder`: folder tree, ownership, grants, manager or favorite relations, folder-level access semantics
- `document`: future document metadata, document lifecycle, version relationships, document-folder associations
- `audit`: future operation audit, access audit, download audit, permission-change audit
- `notify`: future notification delivery, message sending, notification templates, event-driven reminders
- `workflow`: future approval flow, process state, task routing, workflow handlers
- `common`: shared infrastructure, shared config, interceptors, exceptions, result wrappers, utilities

Future capability placement guidance:
- `storage`: prefer landing as `document`-related or shared infrastructure capability first; only create a standalone top-level package if storage logic becomes independently complex
- `permission`: prefer implementing as shared domain or service logic across `auth`, `folder`, and future `document`; do not create a top-level `permission` package unless the permission system becomes a distinct subsystem
- `search`: prefer placing document or folder search under `document` or related business modules first; only promote it to a standalone top-level package when indexing, query, and sync logic become independently complex
- `preview`: prefer placing preview, thumbnail, or format-conversion capability under `document` first; only split it out if it grows into an independent subsystem
- `share`: prefer placing external sharing capability under `document` or `folder`, depending on the shared object; only create a standalone top-level package if share logic becomes system-wide and independently complex

Rules for adding new package boundaries:
- Reuse existing top-level packages whenever the responsibility is still clear.
- New shared infrastructure should prefer `common` only if it is truly cross-module and not business-owned.
- Do not move business logic into `common` just to avoid choosing an owning module.
- If a capability belongs to one primary business domain, keep it under that domain first.
- Only introduce a new top-level package when:
- the capability has clear independent ownership
- it serves multiple business modules
- keeping it inside an existing module would make boundaries worse, not better

Middleware and infrastructure expansion rules:
- If proposing Redis expansion, message queues, object storage, full-text search, or task scheduling, explain:
- the concrete problem
- the expected benefit
- the added complexity and maintenance cost
- whether it is necessary now
- the lighter alternative within the current monolith

## Security And Business Constraints

- Backend authorization is the security boundary for file and document access.
- Hidden frontend controls are not a security boundary.
- If no frontend project exists in the repository, do not invent frontend stack, directories, or commands.
- Search results must be permission-filtered before response once search exists.
- Do not expose real storage paths, storage keys, or bucket-internal paths.
- Prefer streaming for large uploads and downloads.
- Avoid destructive overwrite of historical document versions when versioning exists.
- Treat delete, permission change, share, download, and preview access as operations that may require audit.
- Do not trust client-provided user, role, or department identifiers without backend validation.
- For folder operations, prevent privilege escalation through move, delete, grant, or ownership changes.

## Development Workflow Rules

For coding-agent tasks, use a structured workflow instead of jumping directly into edits.

- For project familiarization, inspect the repository first and summarize confirmed facts before proposing changes.
- For new feature design, brainstorm the design and trade-offs before implementation.
- For implementation planning, create a small, verifiable plan before editing code.
- For bug fixing, use systematic debugging: symptoms, hypotheses, evidence, minimal fix, verification.
- For code changes, plan first, then implement the smallest safe change.
- For code review, classify findings by severity.
- Before finishing a task, report changed files, tests run, unverified items, and remaining risks.

Do not claim a task is complete or fixed without verification evidence.

### File Modification Safety Rules

- Before creating, writing, or modifying a file, check whether the target file already exists.
- If a file already exists, do not clear it, replace it wholesale, or overwrite its original content.
- Do not create another file with the same name to work around an existing file.
- For existing files, prefer minimal in-place edits that preserve unrelated content.
- Before editing an existing file, read the current content and understand what must be preserved.
- If the task requires replacing a whole file, explicitly state that the existing file will be replaced and wait for user confirmation.
- When generating config, rules, docs, or code files, merge with existing content instead of overwriting unless the user explicitly asks for a full replacement.
- After modifying files, report which files were changed and confirm that no unrelated files were modified.