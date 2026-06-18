# 审批流配置化一期交接文档

## 当前状态

本次公司本机已实现审批流配置化一期的后端代码，但因为本机 Java 环境不是项目要求的 Java 17，且当前环境不方便连接完整数据库，所以没有运行 Maven 测试和打包验证。

当前代码应作为 WIP 状态保留，回到可测试电脑后再做完整验证、修补编译问题和业务漏洞。

本交接文档的对比基线是远程仓库 `https://github.com/2023-orange/bid-doc-system.git` 的 `origin/ai/folder-module-ai-work`。截至本次补充时，本地分支 `codex/document-use-archive-loop` 的 HEAD 与该远程分支同为：

```text
397ea24 feat: add dict api and document lifecycle fields
```

因此本文描述的是当前工作区未提交内容相对 `origin/ai/folder-module-ai-work` 的代码与 SQL 变更。`src/main/resources/application-dev.yml` 存在本地配置改动，按交接规则不纳入本功能提交，也不作为需要同步到远端的变更。

## 如何对比远程基线

在当前仓库执行：

```powershell
git fetch origin
git diff --stat origin/ai/folder-module-ai-work -- . ':!src/main/resources/application-dev.yml'
git diff --name-status origin/ai/folder-module-ai-work -- . ':!src/main/resources/application-dev.yml'
```

如需看完整代码差异：

```powershell
git diff origin/ai/folder-module-ai-work -- . ':!src/main/resources/application-dev.yml'
```

本次补充时确认，已跟踪文件的主要差异集中在：
- `audit`：补充审批相关审计模块和操作类型。
- `common`：补充审批流程定义、审批节点、流程配置错误等错误码。
- `document`：补充审批撤回后的资料和版本状态回写方法。
- `workflow`：补充流程定义配置、流程匹配、节点流转、动作日志、候选人快照、撤回、移交、加签、终止、历史扩展等后端能力。
- `src/test/java/com/example/biddoc/workflow/service/impl/ApprovalServiceImplTest.java`：补充审批服务测试中的构造和依赖适配。

当前未跟踪但应随功能提交的新增文件包括：
- `src/main/java/com/example/biddoc/workflow/controller/ApprovalDefinitionController.java`
- `src/main/java/com/example/biddoc/workflow/dto/req/ApprovalAddSignReqDTO.java`
- `src/main/java/com/example/biddoc/workflow/dto/req/ApprovalConditionSaveReqDTO.java`
- `src/main/java/com/example/biddoc/workflow/dto/req/ApprovalDefinitionSaveReqDTO.java`
- `src/main/java/com/example/biddoc/workflow/dto/req/ApprovalNodeSaveReqDTO.java`
- `src/main/java/com/example/biddoc/workflow/dto/req/ApprovalTerminateReqDTO.java`
- `src/main/java/com/example/biddoc/workflow/dto/req/ApprovalTransferReqDTO.java`
- `src/main/java/com/example/biddoc/workflow/dto/resp/ApprovalConditionRespDTO.java`
- `src/main/java/com/example/biddoc/workflow/dto/resp/ApprovalDefinitionRespDTO.java`
- `src/main/java/com/example/biddoc/workflow/dto/resp/ApprovalNodeRespDTO.java`
- `src/main/java/com/example/biddoc/workflow/entity/ApprovalActionLogEntity.java`
- `src/main/java/com/example/biddoc/workflow/entity/ApprovalConditionEntity.java`
- `src/main/java/com/example/biddoc/workflow/entity/ApprovalDefinitionEntity.java`
- `src/main/java/com/example/biddoc/workflow/entity/ApprovalNodeEntity.java`
- `src/main/java/com/example/biddoc/workflow/entity/ApprovalTaskCandidateEntity.java`
- `src/main/java/com/example/biddoc/workflow/mapper/ApprovalActionLogMapper.java`
- `src/main/java/com/example/biddoc/workflow/mapper/ApprovalConditionMapper.java`
- `src/main/java/com/example/biddoc/workflow/mapper/ApprovalDefinitionMapper.java`
- `src/main/java/com/example/biddoc/workflow/mapper/ApprovalNodeMapper.java`
- `src/main/java/com/example/biddoc/workflow/mapper/ApprovalTaskCandidateMapper.java`
- `src/main/java/com/example/biddoc/workflow/service/ApprovalDefinitionService.java`
- `src/main/java/com/example/biddoc/workflow/service/ApprovalFlowEngineService.java`
- `src/main/java/com/example/biddoc/workflow/service/impl/ApprovalDefinitionServiceImpl.java`
- `src/main/java/com/example/biddoc/workflow/service/impl/ApprovalFlowEngineServiceImpl.java`
- `src/main/resources/db/migration/V109__workflow_definition_init.sql`
- `src/main/resources/db/migration/V110__document_use_archive_init.sql`
- `src/main/java/com/example/biddoc/document/entity/DocumentUseGrantEntity.java`
- `src/main/java/com/example/biddoc/document/mapper/DocumentUseGrantMapper.java`
- `src/main/java/com/example/biddoc/project/entity/ProjectArchiveRecordEntity.java`
- `src/main/java/com/example/biddoc/project/entity/ProjectArchiveChecklistSnapshotEntity.java`
- `src/main/java/com/example/biddoc/project/entity/ProjectArchiveDocumentSnapshotEntity.java`
- `src/main/java/com/example/biddoc/project/mapper/ProjectArchiveRecordMapper.java`
- `src/main/java/com/example/biddoc/project/mapper/ProjectArchiveChecklistSnapshotMapper.java`
- `src/main/java/com/example/biddoc/project/mapper/ProjectArchiveDocumentSnapshotMapper.java`
- `docs/handoffs/2026-06-18-workflow-config-phase1-handoff.md`

## 已完成的主要内容

### 1. 审批流定义配置

新增流程定义、节点、条件分支、候选人快照、动作日志等后端结构。

新增或改造的核心能力：
- 流程定义创建、修改、查询、启用、停用
- 流程节点创建、修改、删除
- 审批节点、条件节点、结束节点的轻量流转模型
- 处理人规则预留和实现：指定用户、指定角色、项目负责人、文件夹 owner
- 启用后的流程定义禁止直接修改，以降低在途审批被配置变化影响的风险

### 2. 审批发起和流转改造

已把原来的固定审批人逻辑改造成：
- 优先按场景匹配启用流程定义
- 找不到流程定义时回退到原轻量审批逻辑
- 提交审批后生成流程实例和首节点任务
- 审批通过后推进到下一节点或结束
- 审批驳回、终止、最终通过时复用业务状态回写

一期覆盖场景：
- `DOCUMENT_APPROVAL`
- `DOCUMENT_VERSION_APPROVAL`
- `CHECKLIST_ITEM_APPROVAL`

### 3. 新增审批动作

新增接口和服务方法：
- 撤回：`POST /api/v1/approvals/{id}/withdraw`
- 移交：`POST /api/v1/approvals/tasks/{taskId}/transfer`
- 加签：`POST /api/v1/approvals/tasks/{taskId}/add-sign`
- 终止：`POST /api/v1/approvals/{id}/terminate`

### 4. 审批历史扩展

审批历史 DTO 已扩展流程、节点、动作、处理意见、处理人、处理时间等字段。

## 数据库变更

新增迁移文件：

```text
src/main/resources/db/migration/V109__workflow_definition_init.sql
src/main/resources/db/migration/V110__document_use_archive_init.sql
```

目前这两个 SQL 还没有同步到 `docs/sql` 文件夹。当前 `docs/sql` 下只确认存在：

```text
docs/sql/V4__folder_audit_init.sql
```

开发环境 `application-dev.yml` 中 Flyway 自动迁移是否启用需要以本机配置为准。本仓库规则里已说明 dev profile 当前不能直接当作“启动即自动迁移”的已验证行为；为了明确可控，建议先手动在开发库按顺序执行：

```text
src/main/resources/db/migration/V109__workflow_definition_init.sql
src/main/resources/db/migration/V110__document_use_archive_init.sql
```

执行顺序不要反过来。`V109` 扩展审批基础表并新增审批流配置表，`V110` 新增资料使用授权和项目归档快照表；当前代码中新增实体和 Mapper 会依赖这些表存在。

### 新增数据表

`V109__workflow_definition_init.sql` 新增：
- `wf_approval_definition`
- `wf_approval_node`
- `wf_approval_condition`
- `wf_approval_action_log`
- `wf_approval_task_candidate`

`V110__document_use_archive_init.sql` 新增：
- `doc_document_use_grant`
- `bid_project_archive_record`
- `bid_project_archive_checklist_snapshot`
- `bid_project_archive_document_snapshot`

### 扩展旧表字段

`wf_approval_instance` 新增：
- `definition_id`
- `definition_version`
- `current_node_id`
- `current_node_code`

`wf_approval_task` 新增：
- `definition_id`
- `node_id`
- `node_code`
- `transferred_from_task_id`
- `add_sign`

`V110__document_use_archive_init.sql` 本次只新增表和索引，未确认存在对旧表的 `ALTER TABLE` 修改。

### SQL 执行前检查

执行 SQL 前建议在数据库里确认以下旧表已经存在，因为 `V109` 会扩展它们：

```sql
select to_regclass('public.wf_approval_instance');
select to_regclass('public.wf_approval_task');
```

如果返回 `null`，说明更早的工作流初始化 SQL 尚未执行，需先补齐既有迁移顺序，不能只执行 `V109`。

执行 `V109` 后可用以下 SQL 粗查表和字段：

```sql
select to_regclass('public.wf_approval_definition');
select to_regclass('public.wf_approval_node');
select to_regclass('public.wf_approval_condition');
select to_regclass('public.wf_approval_action_log');
select to_regclass('public.wf_approval_task_candidate');

select column_name
from information_schema.columns
where table_schema = 'public'
  and table_name = 'wf_approval_instance'
  and column_name in ('definition_id', 'definition_version', 'current_node_id', 'current_node_code')
order by column_name;

select column_name
from information_schema.columns
where table_schema = 'public'
  and table_name = 'wf_approval_task'
  and column_name in ('definition_id', 'node_id', 'node_code', 'transferred_from_task_id', 'add_sign')
order by column_name;
```

执行 `V110` 后可用以下 SQL 粗查表：

```sql
select to_regclass('public.doc_document_use_grant');
select to_regclass('public.bid_project_archive_record');
select to_regclass('public.bid_project_archive_checklist_snapshot');
select to_regclass('public.bid_project_archive_document_snapshot');
```

## 当前未验证事项

公司本机没有做以下验证：
- 没有运行 `.\mvnw.cmd test`
- 没有运行 `.\mvnw.cmd -DskipTests package`
- 没有连接 PostgreSQL 执行 `V109__workflow_definition_init.sql`
- 没有跑 smoke 脚本
- 没有验证接口真实请求链路

已做过的轻量检查：
- 确认新增 SQL 文件存在于 `src/main/resources/db/migration`
- 确认当前 git 工作区包含审批流相关代码改动
- 确认 `origin` 指向 `https://github.com/2023-orange/bid-doc-system.git`
- 确认当前工作区还包含 `V110__document_use_archive_init.sql` 及对应资料授权、项目归档快照实体和 Mapper

## 保留代码建议

推荐方式一：提交一个本地 WIP commit。

注意不要把 `src/main/resources/application-dev.yml` 一起提交。这个文件包含本地数据库连接配置改动，不属于本功能交接内容。

可在公司本机执行：

```powershell
git add src/main/java/com/example/biddoc/audit/constant/AuditModuleCodeEnum.java
git add src/main/java/com/example/biddoc/audit/constant/AuditOperationTypeEnum.java
git add src/main/java/com/example/biddoc/common/exception/ErrorCode.java
git add src/main/java/com/example/biddoc/document/service/DocumentService.java
git add src/main/java/com/example/biddoc/document/service/impl/DocumentServiceImpl.java
git add src/main/java/com/example/biddoc/workflow
git add src/main/resources/db/migration/V109__workflow_definition_init.sql
git add src/main/resources/db/migration/V110__document_use_archive_init.sql
git add src/main/java/com/example/biddoc/document/entity/DocumentUseGrantEntity.java
git add src/main/java/com/example/biddoc/document/mapper/DocumentUseGrantMapper.java
git add src/main/java/com/example/biddoc/project/entity/ProjectArchiveRecordEntity.java
git add src/main/java/com/example/biddoc/project/entity/ProjectArchiveChecklistSnapshotEntity.java
git add src/main/java/com/example/biddoc/project/entity/ProjectArchiveDocumentSnapshotEntity.java
git add src/main/java/com/example/biddoc/project/mapper/ProjectArchiveRecordMapper.java
git add src/main/java/com/example/biddoc/project/mapper/ProjectArchiveChecklistSnapshotMapper.java
git add src/main/java/com/example/biddoc/project/mapper/ProjectArchiveDocumentSnapshotMapper.java
git add src/test/java/com/example/biddoc/workflow/service/impl/ApprovalServiceImplTest.java
git add docs/handoffs/2026-06-18-workflow-config-phase1-handoff.md
git commit -m "wip: implement workflow configuration phase one"
```

如果需要像本次一样推送到远程新分支，建议先确定一个明确分支名，再执行：

```powershell
git switch -c codex/workflow-config-phase1-handoff
git push -u origin codex/workflow-config-phase1-handoff
```

分支名可按实际用途调整。关键点是先切到新分支，再提交和推送，避免把 WIP 直接推到 `ai/folder-module-ai-work`。

推荐方式二：导出补丁文件，带回自己电脑应用。

```powershell
git diff -- . ':!src/main/resources/application-dev.yml' > workflow-config-phase1.patch
git ls-files --others --exclude-standard | Select-String -NotMatch 'src/main/resources/application-dev.yml' | ForEach-Object { $_.Line } > workflow-config-phase1-untracked-files.txt
```

如果使用补丁方式，注意普通 `git diff` 不会自动包含未跟踪的新文件内容。更稳妥的方式仍然是本地 WIP commit，然后通过私有分支、U 盘或压缩工作区带走。

## 回家后验证顺序

### 1. 检查 Java 环境

```powershell
java -version
```

期望 Java 17。

### 2. 确认工作区状态

```powershell
git status --short
```

确认审批流相关文件存在，且 `application-dev.yml` 的本地密码配置按自己电脑环境处理。

### 3. 先做编译级验证

```powershell
.\mvnw.cmd -DskipTests package
```

如果失败，优先修复编译错误、构造函数注入错误、Mapper 扫描错误和实体字段映射错误。

### 4. 再跑单元测试

```powershell
.\mvnw.cmd test
```

如果测试失败，优先处理审批服务相关测试。

### 5. 执行数据库 SQL

开发环境 Flyway 自动迁移目前未作为已验证行为。可以手动执行：

```text
src/main/resources/db/migration/V109__workflow_definition_init.sql
src/main/resources/db/migration/V110__document_use_archive_init.sql
```

执行前建议备份开发库，或只在本地开发库执行。

### 6. 启动服务后做接口 smoke

```powershell
.\mvnw.cmd spring-boot:run
```

再根据实际登录 token 和已有数据验证：
- 创建流程定义
- 新增审批节点
- 启用流程定义
- 提交资料审批
- 审批通过
- 审批驳回
- 撤回审批
- 移交任务
- 加签任务
- 超级管理员终止审批
- 查询审批历史

## 建议让回家电脑上的 Codex 继续处理的提示词

可以直接给 Codex：

```text
请你串行接手当前仓库的审批流配置化一期 WIP。先阅读 docs/handoffs/2026-06-18-workflow-config-phase1-handoff.md，然后检查 git status。不要提交 application-dev.yml。请先用 Java 17 运行 .\mvnw.cmd -DskipTests package，再运行 .\mvnw.cmd test。遇到编译或测试失败时，按最小改动修复。修复后检查 V109__workflow_definition_init.sql 是否需要同步到 docs/sql，并给我一份最终变更总结、数据库变更说明和未验证风险。
```

## 重点风险

- 尚未经过 Java 17 编译验证。
- 尚未经过数据库真实迁移验证。
- 尚未经过接口 smoke 验证。
- `V110__document_use_archive_init.sql` 当前只确认新增数据层表、实体和 Mapper；是否已有完整业务接口或服务流程不能仅凭这些文件判断。
- 角色处理人当前应重点检查是否过滤禁用用户、删除用户。
- 流程定义快照目前是部分快照，因此启用流程禁止直接改配置；后续如果需要支持版本化发布，应新增完整快照或复制版本机制。
- `application-dev.yml` 是本地配置，不应作为审批流功能提交。
