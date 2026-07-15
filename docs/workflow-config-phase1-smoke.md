# 审批流配置化一期本地 Smoke 验证

本文档用于在本地验证 `codex/workflow-config-phase1-handoff` 分支的配置化审批链路。所有敏感值只通过当前终端临时环境变量注入，不写入 `application-dev.yml` 或其他配置文件。

## 启动前置

在 PowerShell 当前窗口设置环境变量：

```powershell
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "<本地 PostgreSQL 密码>"
$env:REDIS_PASSWORD = "<本地 Redis 密码>"
```

如果本机 Redis 无密码，可以不设置 `REDIS_PASSWORD` 或设置为空字符串；如果 Redis 开启了密码但未设置 `REDIS_PASSWORD`，登录链路可能出现 Redis `NOAUTH` 错误。不要把 Redis 密码写入仓库配置。

确认本地库 `bid_doc_system` 已包含 V109/V110 结构和基础测试数据。Flyway dev profile 当前不自动迁移，服务启动不能替代数据库结构验证。

可先执行只读数据库验证脚本。该脚本只查询 catalog 和 smoke 记录数量，不执行 DDL/DML：

```powershell
.\scripts\verify-workflow-config-db.ps1
```

启动服务：

```powershell
.\mvnw.cmd spring-boot:run
```

Smoke 客户端输出只允许打印步骤名、HTTP 状态、业务 ID 和简短错误摘要，不打印 token、密码、完整请求头或敏感 payload。

脚本可从环境变量读取测试账号，密码也可以在提示时交互式输入，避免出现在命令历史中：

```powershell
$env:SMOKE_ADMIN_USERNAME = "<超级管理员用户名>"
$env:SMOKE_SUBMITTER_USERNAME = "<发起人用户名>"
$env:SMOKE_APPROVER_USERNAME = "<审批人用户名>"

.\scripts\workflow-config-phase1-smoke.ps1 `
  -ApproverUserId <审批人用户ID> `
  -DocumentId <可提交审批的资料ID>
```

完整 Phase1 验证会在同一个 smoke 流程定义下为不同动作各使用一份资料，避免同一实例终态后无法重复操作，也避免 `wf_approval_definition` 对相同 `scenario/bizModule/bizType/dept/businessCategory/version` 的唯一约束被重复流程定义触发：

```powershell
$env:SMOKE_ADMIN_USERNAME = "<超级管理员用户名>"
$env:SMOKE_SUBMITTER_USERNAME = "<发起人用户名>"
$env:SMOKE_APPROVER_USERNAME = "<审批人用户名>"
$env:SMOKE_TRANSFER_USERNAME = "<转交接收人用户名>"
$env:SMOKE_ADDSIGN_USERNAME = "<加签人用户名>"

.\scripts\workflow-config-phase1-smoke.ps1 `
  -RunFullPhase1 `
  -ApproverUserId <审批人用户ID> `
  -TransferUserId <转交接收人用户ID> `
  -AddSignUserId <加签人用户ID> `
  -DocumentId <通过链路资料ID> `
  -RejectDocumentId <驳回链路资料ID> `
  -WithdrawDocumentId <撤回链路资料ID> `
  -TransferDocumentId <转交链路资料ID> `
  -AddSignDocumentId <加签链路资料ID> `
  -TerminateDocumentId <终止链路资料ID> `
  -Prefix "codex-smoke-$(Get-Date -Format 'yyyyMMddHHmmss')"
```

如需全自动执行，可临时设置 `SMOKE_ADMIN_PASSWORD`、`SMOKE_SUBMITTER_PASSWORD`、`SMOKE_APPROVER_PASSWORD`，但不要写入文件或提交到仓库。
完整模式还可临时设置 `SMOKE_TRANSFER_PASSWORD`、`SMOKE_ADDSIGN_PASSWORD`。

## 数据命名

本轮写入的测试数据统一使用 `codex-smoke-*` 前缀，便于识别和清理。例如：

- 流程定义名：`codex-smoke-document-approval`
- 节点编码：`codex-smoke-approve-1`
- 文档名：`codex-smoke-document.txt`
- 审批意见：`codex-smoke-submit`、`codex-smoke-approve`

## 主链路

1. 超级管理员登录，保存 token 到内存变量，禁止输出 token。
2. 创建流程定义：
   - `scenario=DOCUMENT_APPROVAL`
   - `bizModule=DOCUMENT`
   - `bizType=DOCUMENT`
   - `name=codex-smoke-document-approval`
   - `businessCategory=<Prefix>`，确保 smoke 只匹配本轮资料，且同一轮只创建一个流程定义。
3. 新增审批节点：
   - `nodeCode=codex-smoke-approve-1`
   - `nodeType=APPROVAL`
   - `approveMode=ANY`
   - `assigneeType=USER`
   - `assigneeValue=<启用审批人用户ID>`
4. 启用流程定义。
5. 准备资料：
   - 优先使用已有可查看资料。
   - 如需新增，文件夹和资料名称均使用 `codex-smoke-*` 前缀。
6. 发起资料审批，记录 `instanceId`。
7. 审批人查询待办，按 `instanceId` 找到 `taskId`。
8. 审批通过，确认接口返回统一响应结构。
9. 查询审批历史，确认动作日志包含提交和通过记录。
10. 超级管理员停用 `codex-smoke-document-approval` 流程定义。

## 分支链路

以下分支建议各自新建审批实例，避免同一实例终态后无法重复操作：

- 驳回：提交审批后由当前处理人调用驳回，确认实例终态为 `REJECTED`，业务对象回写为未通过。
- 撤回：提交审批后由发起人调用撤回，确认待办关闭、实例终态为 `WITHDRAWN`。
- 移交：当前处理人移交给另一个启用用户，确认旧任务为 `TRANSFERRED`，新任务处理人为目标用户。
- 加签：当前处理人加签给另一个启用用户，确认生成 `addSign=true` 的待办；加签待办未全部处理前，不推进下一节点。
- 终止：超级管理员终止待处理实例，确认实例终态为 `TERMINATED`，业务对象回到可重新提交状态。

`-RunFullPhase1` 会自动执行以上分支链路：先跑通过链路和历史查询，再依次跑驳回、撤回、移交、加签、终止，最后停用 smoke 流程定义。

## 只读回查建议

Smoke 完成后可用只读 SQL 检查状态，不在脚本中打印敏感信息：

```sql
select id, name, enabled
from wf_approval_definition
where name like 'codex-smoke-%'
order by created_at desc;

select id, definition_id, document_id, status, current_node_code
from wf_approval_instance
where submit_comment like 'codex-smoke-%'
order by created_at desc;

select id, instance_id, approver_user_id, status, node_code, transferred_from_task_id, add_sign
from wf_approval_task
where instance_id in (
    select id from wf_approval_instance where submit_comment like 'codex-smoke-%'
)
order by created_at desc;

select task_id, candidate_type, candidate_value, candidate_user_id, resolved
from wf_approval_task_candidate
where instance_id in (
    select id from wf_approval_instance where submit_comment like 'codex-smoke-%'
)
order by created_at desc;
```

如需清理数据，先停用 smoke 流程定义；清理 SQL 必须人工确认具体 ID 后执行，不在本文档提供自动删除命令。
