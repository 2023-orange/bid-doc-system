# folder Phase 3 收尾 + 审计最小闭环 实施计划

## Context

`bid-doc-system` 是 Spring Boot 3.2.4 + Java 17 + MyBatis-Plus + Sa-Token + PostgreSQL 的文档管理系统。folder 模块按 [docs/folder/folder-module-development-doc-v4.md](docs/folder/folder-module-development-doc-v4.md) 的 6 个阶段推进，当前完成度：

- **Phase 0/1（基础设施 + 审计骨架）**：完成。已有 `AuditOperationLogEntity / Mapper / AuditModuleCodeEnum / AuditOperationTypeEnum`（GRANT/MANAGER/FAVORITE/MOVE/COPY/BATCH_DELETE 等 operationType 全部已枚举），但**无 Service 入口**。
- **Phase 2（folder CRUD + 树）**：完成。[FolderServiceImpl.java](src/main/java/com/example/biddoc/folder/service/impl/FolderServiceImpl.java) 已落 create/rename/update/getById/listChildren/listRootTree，含 8 层校验、同名校验、根级 SUPER_ADMIN 守卫。
- **Phase 3（权限服务）**：部分完成。[FolderPermissionServiceImpl.java](src/main/java/com/example/biddoc/folder/service/impl/FolderPermissionServiceImpl.java) 已实现 canView/canRename/canEdit/canCreateChild（含 manager 继承、grant 生效期、root 敏感操作守卫）。**未完成**：grant/manager 的 list/add/remove 业务入口、controller、DTO，以及 audit 接入。
- **Phase 4/5**：未启动（移动/复制/删除/收藏/检索/日志查询）。

本次目标：**收尾 Phase 3 + 落地审计最小闭环**，让 folder 已有及新增的写操作均产生审计记录，形成一个可独立验证的里程碑。Phase 4/5 的较大改造留待下一迭代。

**范围外**（明确不做）：move/copy/delete/batchDelete、favorite、search、audit-query、cache、PageResponse 重构、BCrypt、`application-dev/prod` 拆分、`BindException` 修复。这些在 V4 §4 已记录，将统一在 Phase 4 启动前一次性处理。

**起点说明**：以当前工作树（含已存在但未提交的 `auth remember-me`、`FolderServiceImpl` 微调、`verify-folder-api.ps1` 更新、`FolderPermissionService*` 新增）为基线。建议在 T1 之前先 `git add` + `git commit` 当前 WIP，把它作为已完成的 Phase 2/3 半成品基线。

---

## 关键设计决策（已收敛）

1. **审计同事务**：`AuditService.record()` 用 `@Transactional(REQUIRED)`，异常向外抛，主业务回滚。V4 §15.2 已收敛 MVP 用同事务，简化排查。
2. **jsonb 列写入**：`AuditOperationLogEntity` 已声明 `JacksonTypeHandler` + `autoResultMap=true`，字段类型为 `Map<String,Object>`。**潜在风险**：MyBatis-Plus 的 `JacksonTypeHandler` 默认按 VARCHAR 传值，而 PostgreSQL JDBC 默认不会把 `varchar` 隐式转 `jsonb`，导致 `column "before_data" is of type jsonb but expression is of type character varying`。处理方案：在 `application.yml` 的 JDBC URL 上追加 `?stringtype=unspecified`（最小侵入），在 T1 验证。若仍失败，再写自定义 `PGobjectJsonbTypeHandler`。
3. **ROLE 主体的 subjectId**：统一存 `RoleCodeEnum.getCode()`（如 `"FOLDER_ADMIN"`），不存 `sys_role.id`。`FolderPermissionServiceImpl.matchesGrantSubject` 已经按 roleCode 比对。
4. **manager 资格**：被设置为 manager 的用户必须当前持有 `FOLDER_ADMIN` 有效角色（V4 §8.3），否则 422。查询走 `UserRoleService.listActiveRoleCodesByUserId(userId)`（已存在）。
5. **grant/manager 的"管理权限"**：操作者满足 `SUPER_ADMIN` 或 ownerUserId==self 或 已经是该目录的 manager 才能改授权/管理员。grant 不允许在 root（level=0）目录上。
6. **唯一索引冲突**：捕 `org.springframework.dao.DuplicateKeyException` → 转 `BusinessException(FOLDER_GRANT_DUPLICATED / FOLDER_MANAGER_DUPLICATED)`，避免 500。
7. **createdBy/updatedBy 类型不动**：沿用 String，依赖 `MyMetaObjectHandler` 现状。

---

## 实施任务（按顺序执行）

### T1. 补 ErrorCode + 验证 jsonb 写入

文件：[src/main/java/com/example/biddoc/common/exception/ErrorCode.java](src/main/java/com/example/biddoc/common/exception/ErrorCode.java)、[src/main/resources/application.yml](src/main/resources/application.yml)

- 在 ErrorCode 追加 7 项（避免与现有 code 冲突；现有 folder 段 4002001-4002003 已占）：
  - `FOLDER_GRANT_NOT_FOUND(4042003, "授权记录不存在")`
  - `FOLDER_GRANT_DUPLICATED(4002004, "授权已存在")`
  - `FOLDER_GRANT_ON_ROOT_FORBIDDEN(4002005, "根级文件夹不允许授权")`
  - `FOLDER_GRANT_SUBJECT_INVALID(4002006, "授权主体非法")`
  - `FOLDER_MANAGER_NOT_FOUND(4042004, "管理员记录不存在")`
  - `FOLDER_MANAGER_DUPLICATED(4002007, "该用户已是管理员")`
  - `FOLDER_MANAGER_REQUIRES_ROLE(4222001, "用户未具备 FOLDER_ADMIN 角色")`
- 在 JDBC URL 末追加 `?stringtype=unspecified`，使 jsonb 列接受 String 参数。
- 写一个最小 ApplicationRunner 或单测，向 `audit_operation_log` 插入一行含 `Map.of("name","x")` 的 beforeData，确认 jsonb 写入成功。若失败，则编写 `common/handler/PGobjectJsonbTypeHandler` 并在 audit 实体三个 jsonb 字段上替换 `typeHandler`。

验证：手工执行 `select before_data->>'name' from audit_operation_log;` 返回 `x`。

### T2. AuditService 落地

文件（新建）：
- [src/main/java/com/example/biddoc/audit/dto/AuditRecordCommand.java](src/main/java/com/example/biddoc/audit/dto/AuditRecordCommand.java)
- [src/main/java/com/example/biddoc/audit/service/AuditService.java](src/main/java/com/example/biddoc/audit/service/AuditService.java)
- [src/main/java/com/example/biddoc/audit/service/impl/AuditServiceImpl.java](src/main/java/com/example/biddoc/audit/service/impl/AuditServiceImpl.java)

要点：
- `AuditRecordCommand` 字段：`moduleCode`(String)、`bizType`(String)、`bizId`(Long)、`operationType`(String)、`beforeData`(Map<String,Object>)、`afterData`(Map<String,Object>)、`extraData`(Map<String,Object>)。提供 `@Builder`。
- `record()`：`@Transactional(propagation=REQUIRED, rollbackFor=Exception.class)`。从 `UserContext.get()` 取 `operatorUserId`/`operatorDeptId`；从 `MDC.get("traceId")` 取 `requestId`；`operationTime = OffsetDateTime.now()`；设置 `moduleCode = FOLDER`（来自 command）；构造 `AuditOperationLogEntity` 后调 `auditOperationLogMapper.insert(entity)`。
- 入参校验：moduleCode/operationType/bizType/bizId 必填，缺失抛 `BusinessException(AUDIT_RECORD_FAILED)`。
- bizType 用字符串字面值 `"FOLDER"`（与 moduleCode 同），bizId 统一为 `folderId`；`grantId/managerId` 放入 extraData。

验证：单元测试或临时 controller 调用一次 `record()` 后 `select * from audit_operation_log` 应有数据，operatorUserId 等字段不为 null。

### T3. FolderGrant DTO 设计

文件（新建）：
- [src/main/java/com/example/biddoc/folder/dto/req/FolderGrantSaveReqDTO.java](src/main/java/com/example/biddoc/folder/dto/req/FolderGrantSaveReqDTO.java)
- [src/main/java/com/example/biddoc/folder/dto/resp/FolderGrantRespDTO.java](src/main/java/com/example/biddoc/folder/dto/resp/FolderGrantRespDTO.java)

要点：
- Save 字段：`subjectType`(@NotBlank, 取值 USER/ROLE/DEPT)、`subjectId`(@NotBlank, 最长 64)、`permissionCodes`(@NotEmpty `List<String>`)、`grantScope`(@NotBlank, SELF/SELF_AND_DESCENDANTS)、`effectiveFrom`(可空 OffsetDateTime)、`effectiveTo`(可空)。
- Resp 字段：`grantId`、`folderId`、`subjectType`、`subjectId`、`permissionCode`、`grantScope`、`effectiveFrom`、`effectiveTo`、`createdAt`、`createdBy`。
- 复用现有 4 个枚举（`FolderSubjectTypeEnum / FolderPermissionCodeEnum / FolderGrantScopeEnum`）做服务侧二次校验，不直接绑枚举（保持 V3 DTO 风格）。

### T4. FolderGrantService + Impl

文件（新建）：
- [src/main/java/com/example/biddoc/folder/service/FolderGrantService.java](src/main/java/com/example/biddoc/folder/service/FolderGrantService.java)
- [src/main/java/com/example/biddoc/folder/service/impl/FolderGrantServiceImpl.java](src/main/java/com/example/biddoc/folder/service/impl/FolderGrantServiceImpl.java)

方法：
- `List<FolderGrantRespDTO> list(Long folderId)`：先校验 folder 存在且可见（复用 `FolderPermissionService.checkView`），返回该 folder 的直接授权（`folder_id = folderId AND deleted = false`），按 createdAt 倒序。
- `void add(Long folderId, FolderGrantSaveReqDTO req)`：
  1. 取 folder（不存在 → `FOLDER_NOT_FOUND`）。
  2. `level == 0` → `FOLDER_GRANT_ON_ROOT_FORBIDDEN`。
  3. 操作者管理资格：`isSuperAdmin || owner == self || isManagerOf(folder)`，否则 `FOLDER_PERMISSION_DENIED`。**复用** `FolderPermissionServiceImpl` 中 `hasManagerScope` 的同等逻辑：在 service 注入 `FolderManagerMapper` 直接判断（避环），不要互相依赖 `FolderGrantService` ↔ `FolderPermissionService`。
  4. 枚举校验：`subjectType ∈ {USER,ROLE,DEPT}`、`grantScope ∈ {SELF, SELF_AND_DESCENDANTS}`、每个 `permissionCode ∈ FolderPermissionCodeEnum`，违反 → `FOLDER_GRANT_SUBJECT_INVALID` 或 `PARAM_INVALID`。
  5. `subjectType=ROLE` 时再校验 `RoleCodeEnum.isValid(subjectId)`。
  6. 拆 permissionCodes 多行插入；捕 `DuplicateKeyException` → `FOLDER_GRANT_DUPLICATED`。
  7. 事务内调 `auditService.record(GRANT_ADD, bizId=folderId, after=新 grant 快照, extra={grantId,permissionCodes,subjectType,subjectId})`。
- `void remove(Long folderId, Long grantId)`：
  1. 查 grantId，存在且 `grant.folderId == folderId` 否则 `FOLDER_GRANT_NOT_FOUND`。
  2. 操作者管理资格同 add。
  3. 逻辑删除（`updateById` set deleted=true）。
  4. record `GRANT_REMOVE`，before=删除前快照。

整体 `@Transactional(rollbackFor=Exception.class)`。

### T5. FolderGrantController

文件（新建）：[src/main/java/com/example/biddoc/folder/controller/FolderGrantController.java](src/main/java/com/example/biddoc/folder/controller/FolderGrantController.java)

- `GET /api/v1/folders/{folderId}/grants` → `ApiResponse<List<FolderGrantRespDTO>>`
- `POST /api/v1/folders/{folderId}/grants` → `ApiResponse<Void>`
- `DELETE /api/v1/folders/{folderId}/grants/{grantId}` → `ApiResponse<Void>`

注解 `@Valid`、`@RequestBody`、`@PathVariable`、`@RequiredArgsConstructor`，风格对齐现有 [FolderController.java](src/main/java/com/example/biddoc/folder/controller/FolderController.java)。

### T6. FolderManager DTO + Service + Impl

文件（新建）：
- [src/main/java/com/example/biddoc/folder/dto/req/FolderManagerSaveReqDTO.java](src/main/java/com/example/biddoc/folder/dto/req/FolderManagerSaveReqDTO.java)：`userId`(@NotNull)、`manageScope`(@NotBlank, SELF/SELF_AND_DESCENDANTS)
- [src/main/java/com/example/biddoc/folder/dto/resp/FolderManagerRespDTO.java](src/main/java/com/example/biddoc/folder/dto/resp/FolderManagerRespDTO.java)
- [src/main/java/com/example/biddoc/folder/service/FolderManagerService.java](src/main/java/com/example/biddoc/folder/service/FolderManagerService.java)
- [src/main/java/com/example/biddoc/folder/service/impl/FolderManagerServiceImpl.java](src/main/java/com/example/biddoc/folder/service/impl/FolderManagerServiceImpl.java)

方法：
- `list(folderId)`：校验 canView 后返回该 folder 的所有 manager。
- `add(folderId, req)`：
  1. folder 存在；
  2. 操作者管理资格（同 grant.add）；
  3. 调 `UserRoleService.listActiveRoleCodesByUserId(req.userId)`，必须含 `RoleCodeEnum.FOLDER_ADMIN.getCode()`，否则 `FOLDER_MANAGER_REQUIRES_ROLE`；
  4. manageScope 校验；
  5. 插入；捕 `DuplicateKeyException` → `FOLDER_MANAGER_DUPLICATED`；
  6. record `MANAGER_ADD`。
- `remove(folderId, managerId)`：存在性 + folderId 一致校验 → 逻辑删除 → record `MANAGER_REMOVE`。

### T7. FolderManagerController

文件（新建）：[src/main/java/com/example/biddoc/folder/controller/FolderManagerController.java](src/main/java/com/example/biddoc/folder/controller/FolderManagerController.java)

三接口对齐 V4 §8.3：
- `GET /api/v1/folders/{folderId}/managers`
- `POST /api/v1/folders/{folderId}/managers`
- `DELETE /api/v1/folders/{folderId}/managers/{managerId}`

### T8. 既有 folder CRUD 接审计 + verify 脚本

文件：
- 修改 [src/main/java/com/example/biddoc/folder/service/impl/FolderServiceImpl.java](src/main/java/com/example/biddoc/folder/service/impl/FolderServiceImpl.java)：注入 `AuditService`，在 `create/rename/update` 末尾各加一次 `auditService.record(...)`：
  - create：`operationType=CREATE, before=null, after={parentId,name,level,ownerDeptId}`
  - rename：`operationType=RENAME, before={name:old}, after={name:new}`
  - update：`operationType=UPDATE, before={remark,status,inheritPermission,sortNo}(旧), after=(新)`
- 修改 [scripts/verify-folder-api.ps1](scripts/verify-folder-api.ps1)：追加用例段（命名 G1–G7、M1–M4、A1–A3，见下方"验证"）。

## 审计落点汇总

| 调用点 | operationType | bizId | before | after | extra |
|---|---|---|---|---|---|
| FolderServiceImpl.create | CREATE | new folder id | null | {parentId,name,level} | - |
| FolderServiceImpl.rename | RENAME | folder id | {name:old} | {name:new} | - |
| FolderServiceImpl.update | UPDATE | folder id | 旧字段 | 新字段 | - |
| FolderGrantServiceImpl.add（每行 permissionCode 一次） | GRANT_ADD | folder id | null | grant 行 | {grantId} |
| FolderGrantServiceImpl.remove | GRANT_REMOVE | folder id | grant 行 | null | {grantId} |
| FolderManagerServiceImpl.add | MANAGER_ADD | folder id | null | {userId,manageScope} | {managerId} |
| FolderManagerServiceImpl.remove | MANAGER_REMOVE | folder id | {userId,manageScope} | null | {managerId} |

## 关键风险与处理

1. **jsonb 写入失败**：T1 通过 `stringtype=unspecified` 验证；失败兜底自定义 PGobject TypeHandler。
2. **循环依赖**：`FolderGrantServiceImpl / FolderManagerServiceImpl` 不互相注入；判断 isManager 直连 `FolderManagerMapper`，与 `FolderPermissionService` 解耦。
3. **MDC traceId 缺失**：当前未确认是否设置；audit 字段 `requestId` 允许 null，不阻断；不在本期补 TraceFilter。
4. **manager 资格随时间过期**：本期只在 add 时校验"当前持有 FOLDER_ADMIN"，不做定时巡检。
5. **JacksonTypeHandler 与 OffsetDateTime**：确认 pom 已含 `jackson-datatype-jsr310`；若 audit 字段里包含 OffsetDateTime 会失败 → 落库前先把时间字段转 String 再放入 Map。

## 验证

### 端到端验收脚本（追加到 scripts/verify-folder-api.ps1）

**Grant 段**
- G1：非根、owner POST grants（USER、FOLDER_VIEW、SELF）→ 200
- G2：root（level=0）POST grants → 403 `FOLDER_GRANT_ON_ROOT_FORBIDDEN`
- G3：非 manager/非 owner/非 SUPER_ADMIN POST → 403 `FOLDER_PERMISSION_DENIED`
- G4：相同 subject+permission 二次 add → 409 `FOLDER_GRANT_DUPLICATED`
- G5：GET grants 返回新建项，字段完整
- G6：DELETE 不存在的 grantId → 404 `FOLDER_GRANT_NOT_FOUND`
- G7：DELETE 跨 folder（grantId 属另一目录）→ 404

**Manager 段**
- M1：add 目标无 FOLDER_ADMIN 角色 → 422 `FOLDER_MANAGER_REQUIRES_ROLE`
- M2：相同 userId 重复 add → 409 `FOLDER_MANAGER_DUPLICATED`
- M3：manageScope=SELF_AND_DESCENDANTS 成功；之后该 manager 可对子目录 create / rename
- M4：DELETE manager → 200，再次访问该目录权限丢失

**Audit 段**
- A1：每次 create/rename/update/grant/manager 后查询 `audit_operation_log` 行数 +1
- A2：rename 后 `before_data->>'name' != after_data->>'name'`
- A3：grant_add 行 `extra_data->>'grantId'` 与新 grant 实际 id 一致

### 单元/集成测试（可选）

如新建 `src/test/java/com/example/biddoc/audit/AuditServiceImplTest.java`，覆盖：record 成功写入、UserContext 缺失时仍能写入（operator 字段允许 null）、jsonb 字段反向读取一致。

### 启动校验

```
./mvnw clean package
./mvnw spring-boot:run
.\scripts\verify-folder-api.ps1
```

全绿即视为本期完成。

## 后续阶段（不在本期）

按 V4 §16，Phase 3 完成后下一步进入 **Phase 4：移动/复制/删除**（含 ancestorIds 批量更新、防循环、批量去重、grant/manager/favorite 级联清理），届时再启动新计划。
