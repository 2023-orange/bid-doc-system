# folder 模块 Phase 4 实施计划（delete / move / copy + Phase 3.5 修复）

> 文档版本：v5
> 适用阶段：Phase 3.5 收尾修复 + Phase 4a（delete/batchDelete）+ Phase 4b（move/copy）
> 前置文档：[folder-module-development-doc-v4.md](folder-module-development-doc-v4.md)

---

## 背景

Phase 3 已完成：folder CRUD + 树、权限引擎（FolderPermissionService）、授权管理（FolderGrantService）、管理员管理（FolderManagerService）、已有写操作接审计（create/rename/update）。编译通过（BUILD SUCCESS），但端到端验证尚未在本地执行。

当前缺口：
1. verify 脚本 M3 有错误码 bug（4002008 vs 实际 4002007）
2. jsonb 写入未经本地 PostgreSQL 验证
3. delete / batchDelete / move / copy 均未实现
4. FolderMapper 无子树查询方法
5. isManagerOf 逻辑在三处 service 中重复

---

## 当前状态

| 功能 | 状态 |
|---|---|
| folder CRUD + 树 | 已完成 |
| 权限引擎 | 已完成 |
| FolderGrantService + Controller | 已完成 |
| FolderManagerService + Controller | 已完成 |
| AuditService.record() | 已完成 |
| create/rename/update 接审计 | 已完成 |
| delete / batchDelete | 未实现 |
| move | 未实现 |
| copy | 未实现 |
| favorite | 未实现（Phase 5） |
| search | 未实现（Phase 5） |
| audit 查询接口 | 未实现（Phase 5） |

---

## 目标

本阶段分三步交付：

**Phase 3.5（收尾修复）**：修复已知 bug，本地验证 Phase 3 功能可用。

**Phase 4a（delete/batchDelete）**：实现目录删除，含子树级联清理 grant/manager/favorite，审计接入。

**Phase 4b（move/copy）**：实现目录移动和复制，含 ancestorIds/level 批量更新、防循环、审计接入。

---

## 范围内

- verify 脚本 M3 错误码修复
- FolderMapper 新增子树查询方法（findDescendantIds）
- FolderPermissionService 接口扩展（canDelete/canMove/canCopy）
- FolderService.delete(id) + batchDelete(req)
- FolderService.move(id, req)
- FolderService.copy(id, req)
- 对应 Controller 端点（DELETE /folders/{id}、DELETE /folders/batch、PATCH /folders/{id}/move、POST /folders/{id}/copy）
- 对应 DTO（FolderDeleteReqDTO、FolderMoveReqDTO、FolderCopyReqDTO）
- 对应 ErrorCode（FOLDER_CYCLE_NOT_ALLOWED、FOLDER_MOVE_TARGET_INVALID、FOLDER_COPY_TARGET_INVALID）
- verify 脚本追加 D1-D4、MV1-MV5、CP1-CP4 验证段
- isManagerOf 重复逻辑抽取到工具类（仅在 Phase 4 开始前做，不单独开 PR）

## 范围外（本阶段不做）

- favorite
- search
- audit 查询接口（AuditQueryService / FolderAuditController）
- cache（Redis 接入）
- PageResponse 重构
- BCrypt / 密码加密
- application-dev/prod 配置拆分
- JwtUtil 清理
- DataScope AOP
- document/notify/workflow 模块

---

## 设计原则

1. **非必要不改已有代码**：FolderServiceImpl 已有的 create/rename/update 逻辑不动，只新增方法。
2. **同事务审计**：delete/move/copy 的审计 record 与主业务在同一事务，失败一起回滚。
3. **逻辑删除**：delete 不物理删除，设 deleted=true；子树、grant、manager、favorite 同步逻辑删除。
4. **audit 不删除**：audit_operation_log 记录永久保留，不随 folder 删除而清理。
5. **防循环移动**：move 时禁止目标父节点是自身或自身后代。
6. **复制不复制权限**：copy 不复制 grant/manager/favorite，新节点 owner 为当前用户。
7. **批量删除父子去重**：batchDelete 传入 [A, A.child] 时只处理 A，基于 ancestorIds 判断。
8. **中文注释**：复杂权限判断、事务边界、ancestorIds 操作处必须有中文注释。

---

## 数据模型影响

无新表。涉及字段：

- `doc_folder.deleted`：批量 update
- `doc_folder.parent_id / ancestor_ids / level`：move/copy 时批量更新
- `doc_folder_grant.deleted`：delete 时级联
- `doc_folder_manager.deleted`：delete 时级联
- `doc_folder_favorite.deleted`：delete 时级联

**FolderMapper 新增方法**（需要自定义 SQL）：

```java
// 查询某节点的全部后代 ID（不含自身）
// SQL: SELECT id FROM doc_folder WHERE deleted=false
//      AND ','||ancestor_ids||',' LIKE '%,'||#{folderId}||',%'
List<Long> findDescendantIds(@Param("folderId") Long folderId);

// 批量更新 ancestorIds 和 level（move 时使用）
int batchUpdateAncestorAndLevel(
    @Param("ids") List<Long> ids,
    @Param("oldPrefix") String oldPrefix,
    @Param("newPrefix") String newPrefix,
    @Param("levelDelta") int levelDelta
);
```

**ancestorIds 前缀替换的安全写法**（防止 id=5 误匹配 id=15）：

```sql
UPDATE doc_folder
SET ancestor_ids = TRIM(BOTH ',' FROM
    REPLACE(',' || ancestor_ids || ',', ',' || #{oldPrefix} || ',', ',' || #{newPrefix} || ',')),
    level = level + #{levelDelta}
WHERE id IN (...)
```

---

## 接口设计

| 功能 | 方法 | 路径 | 权限 |
|---|---|---|---|
| 删除目录 | DELETE | `/api/v1/folders/{id}` | FOLDER_DELETE；root 仅 SUPER_ADMIN |
| 批量删除 | DELETE | `/api/v1/folders/batch` | 每个根节点 FOLDER_DELETE |
| 移动目录 | PATCH | `/api/v1/folders/{id}/move` | FOLDER_MOVE（源）+ FOLDER_CREATE（目标父） |
| 复制目录 | POST | `/api/v1/folders/{id}/copy` | FOLDER_COPY（源）+ FOLDER_CREATE（目标父） |

---

## DTO 设计

```java
// FolderDeleteReqDTO（batchDelete 用）
@NotEmpty List<Long> folderIds

// FolderMoveReqDTO
@NotNull Long targetParentId  // 0 表示移动为根级，仅 SUPER_ADMIN

// FolderCopyReqDTO
@NotNull Long targetParentId
String targetName  // 可空，为空时自动生成"原名(复制)"
```

---

## Service 设计

### FolderService 新增方法

```java
void delete(Long id);
void batchDelete(FolderDeleteReqDTO req);
void move(Long id, FolderMoveReqDTO req);
Long copy(Long id, FolderCopyReqDTO req);  // 返回新根节点 id
```

### delete(id) 流程

1. 取 folder，不存在抛 FOLDER_NOT_FOUND
2. level==0 则要求 SUPER_ADMIN；否则 checkPermission(folder, FOLDER_DELETE)
3. 查子树 ID：`folderMapper.findDescendantIds(id)`，合并 [id] + 子树
4. 事务内批量逻辑删除：doc_folder、doc_folder_grant、doc_folder_manager、doc_folder_favorite
5. record(DELETE, bizId=id, before={name,parentId,level}, extra={subtreeCount})

### batchDelete(req) 流程

1. 批量查 folder 实体
2. 父子去重：若某节点的 ancestorIds 包含集合内其他节点的 id，则跳过该节点
3. 对每个保留节点调 deleteInternal(id, recordAudit=false)（不写单条 DELETE 审计）
4. 最后写一条 BATCH_DELETE 汇总审计（extra={folderIds, actualCount}）

### move(id, req) 流程

1. 取 source；取 target（targetParentId==0 则为根级，仅 SUPER_ADMIN）
2. source root 要求 SUPER_ADMIN；否则 checkPermission(source, FOLDER_MOVE)
3. target 非 root 时 checkPermission(target, FOLDER_CREATE)
4. target.id == source.id 抛 FOLDER_MOVE_TARGET_INVALID
5. target.ancestorIds 包含 source.id 抛 FOLDER_CYCLE_NOT_ALLOWED（防循环）
6. ensureNameUnique(targetParentId, source.name, source.id)
7. 计算 newLevel；查子树最大 level；若 newLevel + (maxOldLevel - source.level) > 8 抛 FOLDER_LEVEL_EXCEEDED
8. 事务内：
   a. 更新 source 自身 parentId/level/ancestorIds
   b. 查子树 IDs；batchUpdateAncestorAndLevel（替换 ancestorIds 前缀，更新 level）
9. record(MOVE, before={parentId,ancestorIds,level}旧, after=新, extra={subtreeCount})

### copy(id, req) 流程

1. 取 source；取 target；checkPermission(source, FOLDER_COPY)；checkPermission(target, FOLDER_CREATE)
2. 计算复制后最大 depth；超过 8 抛 FOLDER_LEVEL_EXCEEDED
3. resolveTargetName：req.targetName 为空则用 source.name；同名时追加"(复制)"、"(复制1)"…（上限 100）
4. BFS 复制：LinkedHashMap<Long oldId, Long newId>；按 level 升序遍历源子树
5. 每个新节点：id=IdWorker.getId()、parentId=map.get(oldParentId) 或 targetParentId、level/ancestorIds 重新计算、ownerUserId=currentUser、ownerDeptId 继承目标父或 currentUser.deptId、inheritPermission=true
6. 不复制 grant/manager/favorite
7. record(COPY, bizId=newRootId, extra={sourceFolderId, targetParentId, copiedCount})

---

## 权限设计

### FolderPermissionService 接口扩展

新增以下方法（不改现有方法）：

```java
boolean canDelete(FolderEntity folder);
void checkDelete(FolderEntity folder);
boolean canMove(FolderEntity folder);
void checkMove(FolderEntity folder);
boolean canCopy(FolderEntity folder);
void checkCopy(FolderEntity folder);
```

实现规则（与 V4 §12.3 一致）：
- SUPER_ADMIN 全局放行
- root（level==0）的 delete/move：仅 SUPER_ADMIN
- 非 root：FolderManager 命中 > 显式授权命中 > 无权限

### isManagerOf 重复逻辑处理

在 Phase 4 开始前，将 `FolderGrantServiceImpl` 和 `FolderManagerServiceImpl` 中的 `isManagerOf` + `resolveSelfAndAncestors` 私有方法提取为包级私有工具类 `FolderManagerChecker`（非 Spring Bean，纯静态），供三处 service 调用。

**注意**：这是维护性改动，不改变任何业务行为，改完必须重新编译验证。同时将字符串字面量 `"SELF_AND_DESCENDANTS"` 替换为 `FolderManageScopeEnum.SELF_AND_DESCENDANTS.getCode()`。

---

## 审计设计

| 操作 | operationType | bizId | before | after | extra |
|---|---|---|---|---|---|
| delete | DELETE | folder id | {name,parentId,level} | null | {subtreeCount} |
| batchDelete | BATCH_DELETE | 0 | null | null | {folderIds,actualCount} |
| move | MOVE | folder id | {parentId,ancestorIds,level}旧 | 新 | {subtreeCount} |
| copy | COPY | new root id | null | {parentId,name,level} | {sourceFolderId,targetParentId,copiedCount} |

所有 OffsetDateTime 字段在放入 Map 前调用 `.toString()` 转字符串。

---

## 错误码设计

在 `ErrorCode.java` 追加（避免与现有冲突）：

```java
FOLDER_CYCLE_NOT_ALLOWED(4002010, "不能移动到自身或子目录下"),
FOLDER_MOVE_TARGET_INVALID(4002011, "目标父目录非法"),
FOLDER_COPY_TARGET_INVALID(4002012, "复制目标父目录非法"),
```

---

## 验证方案

### Phase 3.5 验证（本地执行）

1. 修复 verify 脚本 M3 错误码后，本地执行：
   ```
   ./mvnw spring-boot:run
   .\scripts\verify-folder-api.ps1
   ```
2. 执行 create folder 后查询 audit_operation_log：
   ```sql
   SELECT module_code, operation_type, before_data, after_data
   FROM audit_operation_log ORDER BY created_at DESC LIMIT 5;
   ```
   确认 jsonb 字段可读（`before_data->>'name'` 有值）。

### Phase 4 验证（verify 脚本追加段）

**D1-D4（delete/batchDelete）**
- D1：SUPER_ADMIN 删除根目录 → 200，子树全部 deleted=true，grant/manager/favorite 同步删除
- D2：普通用户无权限删除 → 4032001
- D3：batchDelete 传入父子节点 → 父子去重，只处理父节点
- D4：删除后 audit_operation_log 仍保留

**MV1-MV5（move）**
- MV1：移动到自身 → FOLDER_MOVE_TARGET_INVALID (4002011)
- MV2：移动到子目录 → FOLDER_CYCLE_NOT_ALLOWED (4002010)
- MV3：移动后超过 8 级 → FOLDER_LEVEL_EXCEEDED (4002002)
- MV4：目标父下有同名目录 → FOLDER_NAME_DUPLICATED (4002001)
- MV5：移动成功后 ancestorIds/level 正确更新（含子树）

**CP1-CP4（copy）**
- CP1：复制三层树结构完整（新节点 id 不同，结构相同）
- CP2：新树无源 grant
- CP3：新树无源 favorite
- CP4：复制后超过 8 级 → FOLDER_LEVEL_EXCEEDED

---

## 实施任务拆分

### T0：Phase 3.5 修复（必须先做）

**目标**：修复 verify 脚本 M3 错误码 bug

**涉及文件**：`scripts/verify-folder-api.ps1`

**具体动作**：将 M3 段的 `ExpectedCode 4002008` 改为 `ExpectedCode 4002007`

**验证方式**：本地跑 verify 脚本，M3 通过

**是否允许改动已有代码**：是（仅改脚本，不改业务代码）

---

### T1：isManagerOf 重复逻辑抽取

**目标**：消除 FolderGrantServiceImpl / FolderManagerServiceImpl 中的重复 manager 判断逻辑

**涉及文件**：
- `folder/service/impl/FolderGrantServiceImpl.java`（删除私有方法，改为调用工具类）
- `folder/service/impl/FolderManagerServiceImpl.java`（同上）
- 新建 `folder/support/FolderManagerChecker.java`（静态工具类）

**具体动作**：
1. 新建 `FolderManagerChecker`，将 `isManagerOf` + `resolveSelfAndAncestors` 提取为静态方法，接受 `FolderManagerMapper` 作为参数
2. 同时将字符串字面量 `"SELF_AND_DESCENDANTS"` 替换为 `FolderManageScopeEnum.SELF_AND_DESCENDANTS.getCode()`
3. 两个 ServiceImpl 删除私有方法，改为调用 `FolderManagerChecker.isManagerOf(...)`

**验证方式**：`./mvnw clean package -DskipTests` 编译通过

**是否允许改动已有代码**：是（仅重构，不改业务行为）

---

### T2：ErrorCode 补充 + FolderPermissionService 接口扩展

**目标**：为 Phase 4 准备错误码和权限接口

**涉及文件**：
- `common/exception/ErrorCode.java`
- `folder/service/FolderPermissionService.java`
- `folder/service/impl/FolderPermissionServiceImpl.java`

**具体动作**：
1. ErrorCode 追加 FOLDER_CYCLE_NOT_ALLOWED / FOLDER_MOVE_TARGET_INVALID / FOLDER_COPY_TARGET_INVALID
2. FolderPermissionService 接口新增 canDelete/checkDelete/canMove/checkMove/canCopy/checkCopy
3. FolderPermissionServiceImpl 实现这 6 个方法（规则见权限设计章节）

**验证方式**：编译通过

**是否允许改动已有代码**：是（接口扩展，不改现有方法）

---

### T3：FolderMapper 新增子树查询方法

**目标**：为 delete/move/copy 提供子树 ID 查询能力

**涉及文件**：
- `folder/mapper/FolderMapper.java`
- 新建 `src/main/resources/mapper/FolderMapper.xml`（如需复杂 SQL）

**具体动作**：
1. FolderMapper 新增 `findDescendantIds(@Param("folderId") Long folderId)`
2. FolderMapper 新增 `batchUpdateAncestorAndLevel(...)` 用于 move 的批量更新
3. 使用 XML 或 `@Select/@Update` 注解实现，SQL 见数据模型章节

**验证方式**：编译通过；本地可通过 IDEA DB console 手工验证 SQL 正确性

**是否允许改动已有代码**：是（新增方法，不改现有方法）

---

### T4：FolderDeleteReqDTO + FolderService.delete/batchDelete

**目标**：实现目录删除和批量删除

**涉及文件**：
- 新建 `folder/dto/req/FolderDeleteReqDTO.java`
- 修改 `folder/service/FolderService.java`（新增方法）
- 修改 `folder/service/impl/FolderServiceImpl.java`（实现方法）

**具体动作**：按 Service 设计章节的 delete/batchDelete 流程实现

**验证方式**：编译通过；本地跑 D1-D4 验证段

**是否允许改动已有代码**：是（新增方法，不改现有方法）

---

### T5：FolderMoveReqDTO + FolderService.move

**目标**：实现目录移动

**涉及文件**：
- 新建 `folder/dto/req/FolderMoveReqDTO.java`
- 修改 `folder/service/FolderService.java`
- 修改 `folder/service/impl/FolderServiceImpl.java`

**具体动作**：按 Service 设计章节的 move 流程实现，重点：防循环检测、ancestorIds 批量更新

**验证方式**：编译通过；本地跑 MV1-MV5 验证段

**是否允许改动已有代码**：是（新增方法）

---

### T6：FolderCopyReqDTO + FolderService.copy

**目标**：实现目录复制

**涉及文件**：
- 新建 `folder/dto/req/FolderCopyReqDTO.java`
- 修改 `folder/service/FolderService.java`
- 修改 `folder/service/impl/FolderServiceImpl.java`

**具体动作**：按 Service 设计章节的 copy 流程实现，重点：BFS 复制、oldId 到 newId 映射、不复制 grant/manager/favorite

**验证方式**：编译通过；本地跑 CP1-CP4 验证段

**是否允许改动已有代码**：是（新增方法）

---

### T7：Controller 端点 + verify 脚本

**目标**：暴露 delete/batchDelete/move/copy 接口，更新验证脚本

**涉及文件**：
- 修改 `folder/controller/FolderController.java`（新增 4 个端点）
- 修改 `scripts/verify-folder-api.ps1`（追加 D1-D4、MV1-MV5、CP1-CP4）

**具体动作**：
- `DELETE /api/v1/folders/{id}`
- `DELETE /api/v1/folders/batch`（body=FolderDeleteReqDTO）
- `PATCH /api/v1/folders/{id}/move`
- `POST /api/v1/folders/{id}/copy`

**验证方式**：编译通过；本地全量跑 verify 脚本

**是否允许改动已有代码**：是（新增端点，不改现有端点）

---

## 只提建议、不在本阶段直接改

以下项**仅记录为建议**，本阶段编码 Agent 不得擅自动手：

1. `JwtUtil` 是占位 stub 且无调用，建议标记 `@Deprecated` 或删除——延后到独立技术债 PR。
2. application.yml 含明文数据库密码与 Redis 密码，建议拆分到 `application-dev.yml` 并用环境变量——延后到独立配置 PR。
3. `MyMetaObjectHandler` 中 createdBy/updatedBy 类型为 String，与 V3 设计 Long 不一致，建议统一迁移——延后到全局审计字段迁移 PR。
4. SQL 文件 `docs/sql/V4__folder_audit_init.sql` 无 Flyway/Liquibase 集成——延后到引入数据库迁移工具 PR。

---

## 风险与兜底方案

| 风险 | 概率 | 影响 | 兜底 |
|---|---|---|---|
| jsonb 写入失败（stringtype=unspecified 不够） | 中 | 所有写操作回滚 | 实现 PGobjectJsonbTypeHandler 替换三处 typeHandler |
| ancestorIds 前缀替换误匹配（id=5 匹配 id=15） | 中 | 子树 level 错误 | 使用带逗号边界的 REPLACE 写法（见数据模型章节） |
| batchDelete 部分失败 | 低 | 数据不一致 | 整体事务，任一失败全部回滚 |
| copy 同名后缀死循环 | 低 | 接口超时 | 上限 100 次后抛 FOLDER_NAME_DUPLICATED |
| move 后子树 level 超 8 | 中 | 数据违规 | 移动前预检，超限抛 FOLDER_LEVEL_EXCEEDED |

---

## 完成标准

- verify 脚本 M3 通过（4002007）
- 本地 audit_operation_log 可写入并读取 jsonb 字段
- `./mvnw clean package -DskipTests` BUILD SUCCESS
- 本地跑 verify-folder-api.ps1 全绿（含 D1-D4、MV1-MV5、CP1-CP4）
- delete 后 grant/manager/favorite 同步逻辑删除（DB 验证）
- move 后子树 ancestorIds/level 正确（DB 验证）
- copy 后新树无源 grant/favorite（DB 验证）
- audit_operation_log 有对应 DELETE/MOVE/COPY 记录

---

## 不做什么（明确排除，下一轮编码 Agent 不得扩大范围）

- favorite / search / audit 查询接口
- cache（Redis 接入）
- PageResponse 重构
- BCrypt / 密码加密
- application-dev/prod 配置拆分
- JwtUtil 清理
- DataScope AOP
- document/notify/workflow 模块
- 任何已有接口路径的修改
- 任何已有业务行为的变更

---

## 下一轮编码 Agent 提示词草稿

```text
你现在要作为我的项目编码 Agent，基于实施计划文档完成 folder 模块 Phase 3.5 修复 +
Phase 4（delete/move/copy）。

请先完整阅读并遵守以下文件：
1. 项目根目录的 AGENTS.md
2. docs/folder/implementation-plan.md（本计划文档）
3. docs/folder/folder-module-development-doc-v4.md
4. 现有 folder、audit、auth、common 相关代码

最高优先级规则：
1. AGENTS.md 的要求优先级最高
2. 其次遵守本计划文档
3. 非必要不改已有代码
4. 不要做计划文档明确列为"范围外"或"不做什么"的内容
5. 没有验证，不要宣称"已完成"

请按 T0 → T7 的顺序实施。每完成一个任务做一次编译验证。最后输出：
- 修改了哪些文件
- 新增了哪些文件
- 完成了哪些任务
- 哪些验证已执行/未执行（说明原因）
- 残余风险和建议优化项

本地端到端验证（./mvnw spring-boot:run + verify-folder-api.ps1）需要本地
PostgreSQL + Redis，若环境不可达，请如实报告并把诊断信息给我。
```
