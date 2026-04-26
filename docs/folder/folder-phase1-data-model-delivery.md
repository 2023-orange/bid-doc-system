# Folder 模块第一阶段交付说明

## 1. 修改文件清单

### 1.1 新增文件

#### SQL

- [V4__folder_audit_init.sql](D:/JavaProject/bid-doc-system/bid-doc-system/docs/sql/V4__folder_audit_init.sql)

#### folder 模块

- [FolderEntity.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/folder/entity/FolderEntity.java)
- [FolderGrantEntity.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/folder/entity/FolderGrantEntity.java)
- [FolderManagerEntity.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/folder/entity/FolderManagerEntity.java)
- [FolderFavoriteEntity.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/folder/entity/FolderFavoriteEntity.java)
- [FolderMapper.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/folder/mapper/FolderMapper.java)
- [FolderGrantMapper.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/folder/mapper/FolderGrantMapper.java)
- [FolderManagerMapper.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/folder/mapper/FolderManagerMapper.java)
- [FolderFavoriteMapper.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/folder/mapper/FolderFavoriteMapper.java)
- [FolderSubjectTypeEnum.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/folder/constant/FolderSubjectTypeEnum.java)
- [FolderGrantScopeEnum.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/folder/constant/FolderGrantScopeEnum.java)
- [FolderManageScopeEnum.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/folder/constant/FolderManageScopeEnum.java)
- [FolderPermissionCodeEnum.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/folder/constant/FolderPermissionCodeEnum.java)

#### audit 模块

- [AuditOperationLogEntity.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/audit/entity/AuditOperationLogEntity.java)
- [AuditOperationLogMapper.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/audit/mapper/AuditOperationLogMapper.java)
- [AuditModuleCodeEnum.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/audit/constant/AuditModuleCodeEnum.java)
- [AuditOperationTypeEnum.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/audit/constant/AuditOperationTypeEnum.java)

### 1.2 修改文件

- 本阶段未修改现有业务文件。

### 1.3 未修改但参考过的文件

- [folder-existing-code-analysis-report.md](D:/JavaProject/bid-doc-system/bid-doc-system/docs/folder/folder-existing-code-analysis-report.md)
- [folder-module-development-doc-v4.md](D:/JavaProject/bid-doc-system/bid-doc-system/docs/folder/folder-module-development-doc-v4.md)
- [SysUser.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/entity/SysUser.java)
- [SysDepartment.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/entity/SysDepartment.java)
- [SysUserRole.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/entity/SysUserRole.java)
- [MyMetaObjectHandler.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/common/config/MyMetaObjectHandler.java)
- [application.yml](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/resources/application.yml)
- [RoleCodeEnum.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/constant/RoleCodeEnum.java)

## 2. 数据表说明

### 2.1 doc_folder

用途：

- 保存文件夹树节点。

核心字段说明：

- `parent_id`：父节点 ID，根级固定为 `0`
- `name`：文件夹名称
- `ancestor_ids`：祖先链
- `level`：层级深度
- `sort_no`：排序号
- `owner_dept_id`：所属部门 ID
- `owner_user_id`：负责人或创建人
- `inherit_permission`：是否继承父级权限
- `status`：状态预留
- `remark`：备注
- `created_at/created_by/updated_at/updated_by/deleted`：审计与逻辑删除字段

关键索引说明：

- `idx_doc_folder_parent_id`
- `idx_doc_folder_owner_dept_id`
- `idx_doc_folder_owner_user_id`
- `idx_doc_folder_level`
- `idx_doc_folder_created_at`
- `uk_doc_folder_parent_name_active`

### 2.2 doc_folder_grant

用途：

- 保存文件夹显式授权。

核心字段说明：

- `folder_id`：文件夹 ID
- `subject_type`：授权主体类型，`USER/ROLE/DEPT`
- `subject_id`：授权主体标识
- `permission_code`：权限编码
- `grant_scope`：授权范围，`SELF/SELF_AND_DESCENDANTS`
- `effective_from/effective_to`：授权有效期
- `created_at/created_by/updated_at/updated_by/deleted`

关键索引说明：

- `idx_doc_folder_grant_folder_id`
- `idx_doc_folder_grant_subject`
- `idx_doc_folder_grant_effective`
- `uk_doc_folder_grant_active`

### 2.3 doc_folder_manager

用途：

- 保存文件夹管理员。

核心字段说明：

- `folder_id`：文件夹 ID
- `user_id`：管理员用户 ID
- `manage_scope`：管理范围，`SELF/SELF_AND_DESCENDANTS`
- `created_at/created_by/updated_at/updated_by/deleted`

关键索引说明：

- `idx_doc_folder_manager_folder_id`
- `idx_doc_folder_manager_user_id`
- `uk_doc_folder_manager_active`

### 2.4 doc_folder_favorite

用途：

- 保存用户收藏文件夹关系。

核心字段说明：

- `folder_id`：文件夹 ID
- `user_id`：收藏用户 ID
- `created_at/created_by/deleted`

关键索引说明：

- `idx_doc_folder_favorite_user_id`
- `idx_doc_folder_favorite_folder_id`
- `uk_doc_folder_favorite_active`

### 2.5 audit_operation_log

用途：

- 保存通用业务审计日志，不做成 folder 私有日志表。

核心字段说明：

- `module_code`：模块编码
- `biz_type`：业务类型
- `biz_id`：业务主键
- `operation_type`：操作类型
- `operator_user_id`：操作人
- `operator_dept_id`：操作人部门
- `request_id`：链路追踪 ID
- `operation_time`：业务操作时间
- `before_data/after_data/extra_data`：JSONB 快照数据
- `created_at/created_by/deleted`

关键索引说明：

- `idx_audit_operation_module_code`
- `idx_audit_operation_biz`
- `idx_audit_operation_type`
- `idx_audit_operation_operator_user`
- `idx_audit_operation_operator_dept`
- `idx_audit_operation_time`

## 3. 与现有逻辑的关系

### 3.1 是否修改 auth

- 否。
- 本阶段没有修改 `auth` 模块任何已有逻辑。

### 3.2 是否修改 common

- 否。
- 现有 `OffsetDateTime + String createdBy/updatedBy + Boolean deleted + MyMetaObjectHandler` 已满足本阶段数据模型需要，因此没有对 `common` 做新增或改造。

### 3.3 是否影响现有接口

- 否。
- 本阶段未实现 Controller、Service 业务逻辑，也未改动登录、登出、用户、角色、部门接口。

### 3.4 是否引入新依赖

- 否。
- JSONB 字段复用了项目已有的 `JacksonTypeHandler`，没有新增依赖。

## 4. 编译 / 验证结果

### 4.1 实际执行的命令

```powershell
mvn -q -DskipTests compile
```

### 4.2 是否成功

- 成功。

### 4.3 补充说明

- 本次验证的是 Java 侧编译，不包含数据库建表执行。
- SQL 文件已生成，但未在当前会话中连接 PostgreSQL 执行。

### 4.4 建议你本地执行的命令

```powershell
mvn -q -DskipTests compile
```

```powershell
psql -U postgres -d bid_doc_system -f docs/sql/V4__folder_audit_init.sql
```

## 5. 风险说明

### 5.1 字段类型风险

- `createdBy/updatedBy` 继续沿用 `String`，这是为了兼容现有 `MyMetaObjectHandler`。如果后续想统一改成 `Long`，必须同时调整 `common` 和现有 `auth` 实体，不能只改 `folder/audit`。
- `audit_operation_log.created_by` 沿用 `String`，而 `operator_user_id` 保持 `Long`，这是“审计字段风格”和“业务操作人字段”并存的结果。

### 5.2 jsonb 映射风险

- `audit_operation_log` 的 `before_data/after_data/extra_data` 采用 `Map<String, Object> + JacksonTypeHandler`。
- 这与现有项目 `SysUser.extensionData`、`SysDepartment.extensionData` 风格一致，但复杂嵌套对象在不同查询方式下仍需实际联调验证。

### 5.3 Mapper 扫描风险

- 本次新增 Mapper 全部显式加了 `@Mapper`，与现有项目风格一致。
- 当前项目没有额外的 `@MapperScan` 需求，因此不需要新增扫描配置。

### 5.4 逻辑删除风险

- 项目 `application.yml` 已配置全局 `deleted` 逻辑删除字段。
- 但现有业务代码普遍是手动维护 `deleted=false/true`，并没有完全依赖统一逻辑删除语义。
- 因此后续 `folder` Service 实现时，需要统一约束查询条件与删除动作的写法，避免风格不一致。

### 5.5 subject_id 风险

- `doc_folder_grant.subject_id` 采用 `varchar(64)`，用于兼容：
- `USER`：字符串化用户 ID
- `DEPT`：字符串化部门 ID
- `ROLE`：直接存 `roleCode`
- 这样能与当前 `RoleCodeEnum` 和 V4 文档保持一致，但后续 Service 层需要明确转换规则，避免把数字 ID 和 roleCode 混用出错。

## 6. 下一阶段建议

- 下一阶段只做 `folder` 基础 CRUD 与树结构查询。
- 优先实现：创建文件夹、重命名、详情、列表/children 查询、同级重名校验、层级和祖先链维护。
- 暂时不要并行做授权、管理员、收藏、审计查询接口，先把 `doc_folder` 主表读写闭环做稳定。
