# bid-doc-system folder 模块可落地开发文档 V4

> 适用项目：`bid-doc-system`  
> 适用阶段：folder MVP 开发落地  
> 输入依据：当前压缩包代码现状、项目内 `docs/folder` 文档、上传的 `folder-module-requirements-design-v3.md`  
> 本文定位：在 V3 需求设计文档基础上，补齐“真实代码适配、接口契约、数据库 DDL、开发顺序、测试验收、文档修改建议”。

---

## 0. 复审结论

### 0.1 总体结论

V3 文档的方向是正确的：它已经把 folder MVP 收敛到目录树、CRUD、移动复制、授权、管理员、收藏、目录检索和审计日志，并明确不把上传下载、文档内容、标签、审批、通知放入本期。

但从当前代码落地角度看，V3 仍然偏“需求设计”，还缺少以下工程化内容：

1. 与当前真实代码字段类型对齐。
2. 与当前 `PageResponse`、`UserContext`、`MyMetaObjectHandler` 的实现差异对齐。
3. 缺少数据库 DDL 与索引的完整版本。
4. 缺少接口清单、DTO/VO 字段、Service 方法签名、权限校验落点。
5. 缺少最小可执行开发顺序。
6. 缺少审计模块最小实现方案。
7. 缺少数据范围 AOP 的落地接口，而不仅是原则描述。
8. 缺少验收用例和边界测试清单。

因此建议保留 V3 作为“需求与架构设计文档”，新增本文 V4 作为“开发实施文档”。

---

## 1. 当前项目真实现状

### 1.1 代码结构现状

当前代码主包为：

```text
com.example.biddoc
├── audit       # 包位已存在，基本为空
├── auth        # 已有实际业务实现
├── common      # 已有公共基础设施
├── document    # 包位已存在，基本为空
├── folder      # 包位已存在，基本为空
├── notify      # 包位已存在，基本为空
└── workflow    # 包位已存在，基本为空
```

### 1.2 已实现能力

#### auth 模块

已实现：

1. 用户注册、登录、退出、当前用户获取。
2. 用户启用/禁用。
3. 部门创建、部门列表。
4. 用户角色分配、撤销、按角色查询用户。
5. `RoleCodeEnum`，包含：
   - `SUPER_ADMIN`
   - `FOLDER_ADMIN`
   - `DEPT_MANAGER`
   - `EMPLOYEE`
6. Sa-Token Session 中缓存当前用户和角色列表。

#### common 模块

已实现：

1. `ApiResponse<T>` 统一响应。
2. `PageResponse<T>` 分页响应，但当前结构仍是 `pagination.page/size/total/hasNext`。
3. `BusinessException`、`ErrorCode`、`GlobalExceptionHandler`。
4. `UserContext` ThreadLocal 当前用户上下文。
5. `TokenInterceptor`：登录校验、写入 UserContext、写入 MDC 用户信息。
6. `LoggingInterceptor`：生成 traceId 并写入 MDC。
7. `MyMetaObjectHandler`：自动填充 `createdAt/updatedAt/createdBy/updatedBy`。
8. `StpInterfaceImpl`：为 Sa-Token 提供角色列表。

### 1.3 对 folder 开发的关键影响

1. folder 可以直接复用 `UserContext.get()` 获取当前用户 ID、角色列表、部门 ID。
2. folder 可以直接使用 `StpUtil.hasRole(...)` 和 `@SaCheckRole(...)` 做角色兜底。
3. folder 资源级权限必须新建 `FolderPermissionService`，不能依赖 Sa-Token PermissionList，因为当前 `getPermissionList` 返回空列表。
4. 审计日志当前没有真实服务，因此 folder MVP 应先落一个最小 `audit` 模块实现，否则“操作日志查询”无法闭环。
5. 当前 `createdBy/updatedBy` 是 `String` 类型自动填充，因此 folder 表如果直接复用自动填充，实体字段应先使用 `String`；如果要统一成 `Long`，必须同步改造 `MyMetaObjectHandler` 和已有 auth 实体，成本较高，不建议在 folder MVP 中顺手大改。
6. 当前 `PageResponse` 与 V3 建议的 `pageNum/pageSize/totalPages` 不一致，应先做 common 小改造，避免 folder 接口与后续 document 不一致。

---

## 2. V3 文档修改建议

### 2.1 必须修改

| 位置 | 当前问题 | 修改建议 | 原因 |
|---|---|---|---|
| 6.1.2 Folder 字段 | `createdBy/updatedBy` 写成 bigint | 改为“代码落地阶段沿用 String；后续统一审计字段时再迁移 Long” | 当前 `MyMetaObjectHandler` 填充 String，直接用 Long 会导致填充失败或类型不一致 |
| 6.5 AuditOperationLog | 只有表建议，缺少最小服务接口 | 补充 `AuditService.record()`、`AuditQueryService.pageByBiz()` | folder 查询操作日志必须有服务承载 |
| 8.9 数据范围 | 只有原则，无类设计 | 增补 `@DataScope`、`DataScopeContext`、`DataScopeAspect`、`DataScopeRule` | 方便直接编码 |
| 13 分页接口 | 建议结构与当前 `PageResponse` 不一致 | 要么改 common，要么文档明确“先改 common 再开发 folder” | 防止接口风格割裂 |
| 14 接口设计 | 只有 Controller/DTO/VO 名称 | 增补 HTTP 方法、路径、请求体、返回体、权限点 | 后端和前端才能并行开发 |
| 15 MVC 落地 | 包结构缺少 impl、event、cache、permission 支撑 | 增补 `cache`、`event`、`support` 包 | 权限、缓存、审计需要专门落点 |
| 18.3 待确认 | 仍有 3 个点未定 | 在 V4 中直接收敛为默认决策 | 开发文档不能留核心行为空白 |

### 2.2 建议修改

1. 把“文件夹操作日志查询（通过 audit 模块承载）”拆成两个开发任务：
   - `audit` 最小能力建设。
   - `folder` 按 folder 维度查询审计日志。

2. 把“文件夹搜索”统一改名为“目录检索 / 条件筛选”。接口路径建议用 `/folders/search`，文档标题用“目录条件检索”。

3. 把“根级文件夹管理”明确为：
   - 根级文件夹只允许 `SUPER_ADMIN` 创建、重命名、删除、移动、编辑。
   - 根级文件夹不允许授权。
   - 根级文件夹不允许普通用户收藏。
   - 普通用户仅在有后代目录可见时看到根级文件夹作为路径容器。

4. 把“逻辑删除”写清楚：
   - 用户语义：直接删除，不提供恢复。
   - 技术实现：数据库逻辑删除。
   - 审计日志保留。

5. 增加“开发前置改造”章节：
   - common 分页规范。
   - ErrorCode 补充 folder/audit 错误码。
   - `application-dev.yml` 从空文件变为本地配置承载文件。
   - 敏感配置从 `application.yml` 移出。

---

## 3. V4 已收敛业务决策

### 3.1 根级文件夹

1. 数据库存储真实根级文件夹，不存单一虚拟根。
2. 根级文件夹满足：
   - `parentId = 0`
   - `level = 0`
   - `ancestorIds = 当前节点 id 字符串`
3. 根级文件夹只能由 `SUPER_ADMIN` 创建。
4. 根级文件夹不允许授权。
5. 根级文件夹不允许普通用户收藏。
6. 根级文件夹敏感操作仅 `SUPER_ADMIN` 可执行：
   - 编辑
   - 重命名
   - 删除
   - 移动
   - 权限配置
7. 普通用户只有在其拥有某根级文件夹后代目录的可见权限时，才可在树中看到该根级文件夹作为路径容器。

### 3.2 权限模型

MVP 权限采用：

```text
SUPER_ADMIN 全局放行
  -> 根级保护规则
  -> FolderManager 命中
  -> 当前节点显式授权
  -> 父节点继承授权
  -> 无权限
```

其中：

1. `FOLDER_ADMIN` 只是“可被设置为目录管理员”的资格角色，不是全局目录管理员。
2. `DEPT_MANAGER` 不天然获得 folder 管理权，只参与数据范围放大。
3. MVP 不做显式拒绝，只做允许授权。
4. MVP 不接入菜单/按钮权限系统，folder 权限点先作为业务枚举。

### 3.3 folder 权限点

```text
FOLDER_VIEW
FOLDER_CREATE
FOLDER_EDIT
FOLDER_RENAME
FOLDER_DELETE
FOLDER_MOVE
FOLDER_COPY
FOLDER_GRANT
FOLDER_MANAGER_SET
FOLDER_INHERIT_SET
FOLDER_SEARCH
FOLDER_FAVORITE
FOLDER_AUDIT_VIEW
```

### 3.4 授权主体

```text
USER
ROLE
DEPT
```

其中 `ROLE` 的 `subjectId` 建议存 `roleCode`，类型使用 `varchar(64)`，不要存 `sys_role.id`。原因是当前项目核心角色判断已使用 `RoleCodeEnum`，且 Sa-Token 角色也是 roleCode。

### 3.5 删除策略

1. 用户视角：直接删除，不可恢复。
2. 技术实现：逻辑删除。
3. 删除范围：当前节点及全部后代。
4. 同步清理：grant、manager、favorite。
5. 不删除：audit_operation_log。
6. 当前 document 模块未落地，不做 document 占用校验；未来 document 接入后必须补充 `DocumentFolderGuard`。

---

## 4. 开发前置改造

### 4.1 common 分页结构统一

当前 `PageResponse` 为：

```java
private List<T> list;
private Pagination pagination;

public static class Pagination {
    private long page;
    private long size;
    private long total;
    private boolean hasNext;
}
```

建议改为：

```java
@Data
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> list;
    private long pageNum;
    private long pageSize;
    private long total;
    private long totalPages;
    private boolean hasNext;

    public static <T> PageResponse<T> of(com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> page) {
        return new PageResponse<>(
                page.getRecords(),
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getPages(),
                page.getCurrent() < page.getPages()
        );
    }
}
```

新增：

```java
package com.example.biddoc.common.result;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PageQuery {
    @Min(value = 1, message = "pageNum不能小于1")
    private long pageNum = 1;

    @Min(value = 1, message = "pageSize不能小于1")
    @Max(value = 100, message = "pageSize不能超过100")
    private long pageSize = 20;

    private String sortBy;
    private String sortOrder = "desc";
}
```

### 4.2 ErrorCode 补充

建议在 `ErrorCode` 增加 folder/audit 语义错误码：

```java
// Folder
FOLDER_NOT_FOUND(4042001, "文件夹不存在"),
FOLDER_NAME_DUPLICATED(4002001, "同级文件夹名称已存在"),
FOLDER_CYCLE_NOT_ALLOWED(4002002, "不能移动到自身或子目录下"),
FOLDER_LEVEL_EXCEEDED(4002003, "文件夹层级超过限制"),
FOLDER_ROOT_PROTECTED(4032001, "根级文件夹仅超级管理员可操作"),
FOLDER_PERMISSION_DENIED(4032002, "无文件夹操作权限"),
FOLDER_GRANT_NOT_ALLOWED_ON_ROOT(4002004, "根级文件夹不允许授权"),
FOLDER_FAVORITE_NOT_ALLOWED_ON_ROOT(4002005, "根级文件夹不允许收藏"),

// Audit
AUDIT_LOG_NOT_FOUND(4043001, "审计日志不存在")
```

### 4.3 配置文件调整

当前 `application.yml` 中含数据库密码和 JWT 密钥，且 `application-dev.yml` 为空。建议改成：

- `application.yml`：公共配置，不放敏感信息。
- `application-dev.yml`：本地开发配置。
- `application-prod.yml`：生产配置模板，敏感项用环境变量。

建议：

```yaml
spring:
  profiles:
    active: dev
```

开发配置放入：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/bid_doc_system
    username: postgres
    password: ${BIDDOC_DB_PASSWORD:postgres}
  data:
    redis:
      host: localhost
      port: 6379
      password: ${BIDDOC_REDIS_PASSWORD:}

sa-token:
  jwt-secret-key: ${BIDDOC_JWT_SECRET:bid-doc-dev-secret-change-me}
```

### 4.4 代码卫生建议

1. `GlobalExceptionHandler` 当前导入的是 `java.net.BindException`，建议改成 `org.springframework.validation.BindException`，否则 Web 参数绑定异常捕获语义不准确。
2. 当前密码明文比较，后续至少应在 auth 模块引入 BCrypt。folder 开发不阻塞，但上线前必须处理。
3. 当前 `JwtUtil.parse()` 是占位实现，若项目已经采用 Sa-Token JWT，应删除未使用的旧工具或标记 deprecated，避免误用。
4. 当前 Maven wrapper 没有执行权限，建议设置：`chmod +x mvnw`。
5. 当前项目没有测试目录和测试用例，folder 开发应补最小单元测试/集成测试。

---

## 5. 数据库设计

### 5.1 命名规则

folder 相关表使用 `doc_` 前缀：

```text
doc_folder
doc_folder_grant
doc_folder_manager
doc_folder_favorite
```

审计表使用：

```text
audit_operation_log
```

### 5.2 PostgreSQL DDL

#### 5.2.1 doc_folder

```sql
create table if not exists doc_folder (
    id bigint primary key,
    parent_id bigint not null default 0,
    name varchar(128) not null,
    ancestor_ids varchar(1024) not null,
    level integer not null,
    sort_no integer not null default 0,
    owner_dept_id bigint null,
    owner_user_id bigint null,
    inherit_permission boolean not null default true,
    status smallint not null default 1,
    remark varchar(512),
    created_at timestamptz not null default now(),
    created_by varchar(64),
    updated_at timestamptz not null default now(),
    updated_by varchar(64),
    deleted boolean not null default false
);

comment on table doc_folder is '文档目录/文件夹表';
comment on column doc_folder.parent_id is '父文件夹ID，根级为0';
comment on column doc_folder.ancestor_ids is '祖先链，包含自身ID，逗号分隔';
comment on column doc_folder.level is '层级，根级为0';
comment on column doc_folder.owner_dept_id is '所属部门ID，根级可为空';
comment on column doc_folder.inherit_permission is '是否继承父级权限';

create index if not exists idx_doc_folder_parent_id on doc_folder(parent_id) where deleted = false;
create index if not exists idx_doc_folder_owner_dept_id on doc_folder(owner_dept_id) where deleted = false;
create index if not exists idx_doc_folder_owner_user_id on doc_folder(owner_user_id) where deleted = false;
create index if not exists idx_doc_folder_level on doc_folder(level) where deleted = false;
create index if not exists idx_doc_folder_created_at on doc_folder(created_at);
create index if not exists idx_doc_folder_ancestor_ids on doc_folder(ancestor_ids);

create unique index if not exists uk_doc_folder_parent_name_active
on doc_folder(parent_id, name)
where deleted = false;
```

#### 5.2.2 doc_folder_grant

```sql
create table if not exists doc_folder_grant (
    id bigint primary key,
    folder_id bigint not null,
    subject_type varchar(32) not null,
    subject_id varchar(64) not null,
    permission_code varchar(64) not null,
    grant_scope varchar(32) not null default 'SELF',
    effective_from timestamptz null,
    effective_to timestamptz null,
    created_at timestamptz not null default now(),
    created_by varchar(64),
    updated_at timestamptz not null default now(),
    updated_by varchar(64),
    deleted boolean not null default false,
    constraint ck_doc_folder_grant_subject_type check (subject_type in ('USER','ROLE','DEPT')),
    constraint ck_doc_folder_grant_scope check (grant_scope in ('SELF','SELF_AND_DESCENDANTS'))
);

create index if not exists idx_doc_folder_grant_folder_id on doc_folder_grant(folder_id) where deleted = false;
create index if not exists idx_doc_folder_grant_subject on doc_folder_grant(subject_type, subject_id) where deleted = false;
create index if not exists idx_doc_folder_grant_effective on doc_folder_grant(effective_from, effective_to);

create unique index if not exists uk_doc_folder_grant_active
on doc_folder_grant(folder_id, subject_type, subject_id, permission_code)
where deleted = false;
```

#### 5.2.3 doc_folder_manager

```sql
create table if not exists doc_folder_manager (
    id bigint primary key,
    folder_id bigint not null,
    user_id bigint not null,
    manage_scope varchar(32) not null default 'SELF_AND_DESCENDANTS',
    created_at timestamptz not null default now(),
    created_by varchar(64),
    updated_at timestamptz not null default now(),
    updated_by varchar(64),
    deleted boolean not null default false,
    constraint ck_doc_folder_manager_scope check (manage_scope in ('SELF','SELF_AND_DESCENDANTS'))
);

create index if not exists idx_doc_folder_manager_folder_id on doc_folder_manager(folder_id) where deleted = false;
create index if not exists idx_doc_folder_manager_user_id on doc_folder_manager(user_id) where deleted = false;

create unique index if not exists uk_doc_folder_manager_active
on doc_folder_manager(folder_id, user_id)
where deleted = false;
```

#### 5.2.4 doc_folder_favorite

```sql
create table if not exists doc_folder_favorite (
    id bigint primary key,
    folder_id bigint not null,
    user_id bigint not null,
    created_at timestamptz not null default now(),
    created_by varchar(64),
    deleted boolean not null default false
);

create index if not exists idx_doc_folder_favorite_user_id on doc_folder_favorite(user_id) where deleted = false;
create index if not exists idx_doc_folder_favorite_folder_id on doc_folder_favorite(folder_id) where deleted = false;

create unique index if not exists uk_doc_folder_favorite_active
on doc_folder_favorite(folder_id, user_id)
where deleted = false;
```

#### 5.2.5 audit_operation_log

```sql
create table if not exists audit_operation_log (
    id bigint primary key,
    module_code varchar(32) not null,
    biz_type varchar(32) not null,
    biz_id bigint not null,
    operation_type varchar(64) not null,
    operator_user_id bigint,
    operator_dept_id bigint,
    request_id varchar(64),
    operation_time timestamptz not null default now(),
    before_data jsonb,
    after_data jsonb,
    extra_data jsonb,
    created_at timestamptz not null default now(),
    created_by varchar(64),
    deleted boolean not null default false
);

create index if not exists idx_audit_operation_module_biz
on audit_operation_log(module_code, biz_type, biz_id)
where deleted = false;

create index if not exists idx_audit_operation_operator
on audit_operation_log(operator_user_id, operation_time desc)
where deleted = false;

create index if not exists idx_audit_operation_dept
on audit_operation_log(operator_dept_id, operation_time desc)
where deleted = false;

create index if not exists idx_audit_operation_time
on audit_operation_log(operation_time desc)
where deleted = false;
```

---

## 6. Java 包结构

### 6.1 folder 包结构

建议：

```text
com.example.biddoc.folder
├── controller
│   ├── FolderController.java
│   ├── FolderGrantController.java
│   ├── FolderManagerController.java
│   ├── FolderFavoriteController.java
│   └── FolderAuditController.java
├── service
│   ├── FolderService.java
│   ├── FolderGrantService.java
│   ├── FolderManagerService.java
│   ├── FolderFavoriteService.java
│   ├── FolderPermissionService.java
│   └── FolderCacheService.java
├── service.impl
├── mapper
│   ├── DocFolderMapper.java
│   ├── DocFolderGrantMapper.java
│   ├── DocFolderManagerMapper.java
│   └── DocFolderFavoriteMapper.java
├── entity
│   ├── DocFolder.java
│   ├── DocFolderGrant.java
│   ├── DocFolderManager.java
│   └── DocFolderFavorite.java
├── dto
│   ├── FolderCreateRequest.java
│   ├── FolderUpdateRequest.java
│   ├── FolderRenameRequest.java
│   ├── FolderMoveRequest.java
│   ├── FolderCopyRequest.java
│   ├── FolderDeleteRequest.java
│   ├── FolderGrantSaveRequest.java
│   ├── FolderManagerSaveRequest.java
│   └── FolderSearchRequest.java
├── vo
│   ├── FolderTreeNodeVO.java
│   ├── FolderDetailVO.java
│   ├── FolderGrantVO.java
│   ├── FolderManagerVO.java
│   ├── FolderFavoriteVO.java
│   ├── FolderPermissionVO.java
│   └── AuditLogVO.java
├── enums
│   ├── FolderPermissionCode.java
│   ├── FolderGrantSubjectType.java
│   ├── FolderGrantScope.java
│   └── FolderOperationType.java
├── converter
│   └── FolderConverter.java
└── support
    ├── FolderTreeSupport.java
    └── FolderNameSupport.java
```

> 注意：当前 auth 包使用 `convertor` 拼法，document 空包使用 `converter`。建议从 folder 开始统一使用 `converter`，并后续逐步把 auth 的 `convertor` 迁移为 `converter`，或反过来统一为 `convertor`。关键是全项目保持一种拼法。

### 6.2 audit 包结构

```text
com.example.biddoc.audit
├── controller
│   └── AuditOperationLogController.java  # 可后置，folder MVP 可先不暴露通用接口
├── service
│   ├── AuditService.java
│   └── AuditQueryService.java
├── service.impl
├── mapper
│   └── AuditOperationLogMapper.java
├── entity
│   └── AuditOperationLog.java
├── dto
│   ├── AuditRecordCommand.java
│   └── AuditLogQuery.java
└── vo
    └── AuditOperationLogVO.java
```

### 6.3 common 数据范围包

```text
com.example.biddoc.common.datascope
├── DataScope.java
├── DataScopeAspect.java
├── DataScopeContext.java
├── DataScopeRule.java
└── DataScopeType.java
```

---

## 7. Entity 设计

### 7.1 DocFolder

关键点：`createdBy/updatedBy` 先使用 `String` 以兼容现有 `MyMetaObjectHandler`。

```java
@Data
@TableName("doc_folder")
public class DocFolder {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long parentId;
    private String name;
    private String ancestorIds;
    private Integer level;
    private Integer sortNo;
    private Long ownerDeptId;
    private Long ownerUserId;
    private Boolean inheritPermission;
    private Integer status;
    private String remark;
    private Boolean deleted;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;
}
```

### 7.2 DocFolderGrant

```java
@Data
@TableName("doc_folder_grant")
public class DocFolderGrant {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long folderId;
    private String subjectType;
    private String subjectId;
    private String permissionCode;
    private String grantScope;
    private OffsetDateTime effectiveFrom;
    private OffsetDateTime effectiveTo;
    private Boolean deleted;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;
}
```

### 7.3 DocFolderManager

```java
@Data
@TableName("doc_folder_manager")
public class DocFolderManager {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long folderId;
    private Long userId;
    private String manageScope;
    private Boolean deleted;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;
}
```

### 7.4 DocFolderFavorite

```java
@Data
@TableName("doc_folder_favorite")
public class DocFolderFavorite {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long folderId;
    private Long userId;
    private Boolean deleted;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private String createdBy;
}
```

---

## 8. 接口设计

统一前缀：

```text
/api/v1/folders
```

### 8.1 FolderController

| 功能 | 方法 | 路径 | 权限 |
|---|---|---|---|
| 创建文件夹 | POST | `/api/v1/folders` | 父节点 `FOLDER_CREATE`；根级仅 SUPER_ADMIN |
| 编辑文件夹 | PUT | `/api/v1/folders/{id}` | `FOLDER_EDIT`；根级仅 SUPER_ADMIN |
| 重命名 | PATCH | `/api/v1/folders/{id}/name` | `FOLDER_RENAME`；根级仅 SUPER_ADMIN |
| 删除 | DELETE | `/api/v1/folders/{id}` | `FOLDER_DELETE`；根级仅 SUPER_ADMIN |
| 批量删除 | DELETE | `/api/v1/folders/batch` | 每个根节点 `FOLDER_DELETE` |
| 移动 | PATCH | `/api/v1/folders/{id}/move` | 源 `FOLDER_MOVE` + 目标父 `FOLDER_CREATE` |
| 复制 | POST | `/api/v1/folders/{id}/copy` | 源 `FOLDER_COPY` + 目标父 `FOLDER_CREATE` |
| 根树 | GET | `/api/v1/folders/tree/root` | 登录后按可见范围过滤 |
| 子节点 | GET | `/api/v1/folders/{parentId}/children` | 父节点可见 |
| 详情 | GET | `/api/v1/folders/{id}` | `FOLDER_VIEW` |
| 条件检索 | POST | `/api/v1/folders/search` | `FOLDER_SEARCH` |

### 8.2 FolderGrantController

| 功能 | 方法 | 路径 | 权限 |
|---|---|---|---|
| 查询授权 | GET | `/api/v1/folders/{folderId}/grants` | `FOLDER_GRANT` 或 FolderManager |
| 新增授权 | POST | `/api/v1/folders/{folderId}/grants` | `FOLDER_GRANT`；根级禁止 |
| 删除授权 | DELETE | `/api/v1/folders/{folderId}/grants/{grantId}` | `FOLDER_GRANT`；根级禁止 |

### 8.3 FolderManagerController

| 功能 | 方法 | 路径 | 权限 |
|---|---|---|---|
| 查询管理员 | GET | `/api/v1/folders/{folderId}/managers` | `FOLDER_MANAGER_SET` 或 FolderManager |
| 添加管理员 | POST | `/api/v1/folders/{folderId}/managers` | `SUPER_ADMIN` 或 `FOLDER_MANAGER_SET`；被添加人必须有 FOLDER_ADMIN 角色 |
| 删除管理员 | DELETE | `/api/v1/folders/{folderId}/managers/{managerId}` | `SUPER_ADMIN` 或 `FOLDER_MANAGER_SET` |

### 8.4 FolderFavoriteController

| 功能 | 方法 | 路径 | 权限 |
|---|---|---|---|
| 收藏 | POST | `/api/v1/folders/{folderId}/favorite` | `FOLDER_FAVORITE`；根级禁止 |
| 取消收藏 | DELETE | `/api/v1/folders/{folderId}/favorite` | 登录用户本人 |
| 我的收藏 | GET | `/api/v1/folders/favorites` | 登录用户本人 |

### 8.5 FolderAuditController

| 功能 | 方法 | 路径 | 权限 |
|---|---|---|---|
| 查询目录审计日志 | POST | `/api/v1/folders/{folderId}/audit-logs` | `FOLDER_AUDIT_VIEW` |

---

## 9. DTO / VO 设计

### 9.1 FolderCreateRequest

```java
@Data
public class FolderCreateRequest {
    @NotBlank(message = "文件夹名称不能为空")
    @Size(max = 128, message = "文件夹名称不能超过128个字符")
    private String name;

    @NotNull(message = "父文件夹ID不能为空")
    private Long parentId;

    private Integer sortNo;
    private Long ownerDeptId;
    private String remark;
}
```

规则：

1. `parentId = 0` 表示创建根级文件夹，仅 `SUPER_ADMIN` 可操作。
2. 普通目录 `ownerDeptId` 为空时默认继承父目录；父目录无 ownerDeptId 时回退当前用户 deptId。
3. 根级目录允许 `ownerDeptId` 为空。

### 9.2 FolderUpdateRequest

```java
@Data
public class FolderUpdateRequest {
    private Integer sortNo;

    @Size(max = 512, message = "备注不能超过512个字符")
    private String remark;

    private Boolean inheritPermission;
}
```

### 9.3 FolderRenameRequest

```java
@Data
public class FolderRenameRequest {
    @NotBlank(message = "文件夹名称不能为空")
    @Size(max = 128, message = "文件夹名称不能超过128个字符")
    private String name;
}
```

### 9.4 FolderMoveRequest

```java
@Data
public class FolderMoveRequest {
    @NotNull(message = "目标父文件夹ID不能为空")
    private Long targetParentId;
}
```

### 9.5 FolderCopyRequest

```java
@Data
public class FolderCopyRequest {
    @NotNull(message = "目标父文件夹ID不能为空")
    private Long targetParentId;

    private String targetName;
}
```

### 9.6 FolderDeleteRequest

```java
@Data
public class FolderDeleteRequest {
    @NotEmpty(message = "文件夹ID不能为空")
    private List<Long> folderIds;
}
```

### 9.7 FolderGrantSaveRequest

```java
@Data
public class FolderGrantSaveRequest {
    @NotBlank
    private String subjectType; // USER / ROLE / DEPT

    @NotBlank
    private String subjectId;

    @NotEmpty
    private List<String> permissionCodes;

    @NotBlank
    private String grantScope; // SELF / SELF_AND_DESCENDANTS

    private OffsetDateTime effectiveFrom;
    private OffsetDateTime effectiveTo;
}
```

### 9.8 FolderManagerSaveRequest

```java
@Data
public class FolderManagerSaveRequest {
    @NotNull
    private Long userId;

    @NotBlank
    private String manageScope; // SELF / SELF_AND_DESCENDANTS
}
```

### 9.9 FolderSearchRequest

```java
@Data
public class FolderSearchRequest extends PageQuery {
    private String name;
    private Long parentId;
    private Long rootId;
    private Integer level;
    private Long ownerDeptId;
    private Long ownerUserId;
    private OffsetDateTime createdFrom;
    private OffsetDateTime createdTo;
    private Boolean favoriteOnly;
    private Boolean onlyAccessible = true;
}
```

### 9.10 FolderTreeNodeVO

```java
@Data
public class FolderTreeNodeVO {
    private Long id;
    private Long parentId;
    private String name;
    private Integer level;
    private Integer sortNo;
    private Boolean hasChildren;
    private Boolean favorite;
    private List<String> permissionCodes;
    private List<FolderTreeNodeVO> children;
}
```

### 9.11 FolderDetailVO

```java
@Data
public class FolderDetailVO {
    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private String ancestorIds;
    private Integer level;
    private Integer sortNo;
    private Long ownerDeptId;
    private Long ownerUserId;
    private Boolean inheritPermission;
    private Integer status;
    private String remark;
    private Boolean favorite;
    private List<String> permissionCodes;
    private Integer grantCount;
    private Integer managerCount;
    private OffsetDateTime createdAt;
    private String createdBy;
    private OffsetDateTime updatedAt;
    private String updatedBy;
}
```

---

## 10. Service 设计

### 10.1 FolderService

```java
public interface FolderService {
    Long create(FolderCreateRequest req);
    void update(Long id, FolderUpdateRequest req);
    void rename(Long id, FolderRenameRequest req);
    void delete(Long id);
    void batchDelete(FolderDeleteRequest req);
    void move(Long id, FolderMoveRequest req);
    Long copy(Long id, FolderCopyRequest req);
    List<FolderTreeNodeVO> loadRootTree();
    List<FolderTreeNodeVO> loadChildren(Long parentId);
    FolderDetailVO detail(Long id);
    PageResponse<FolderDetailVO> search(FolderSearchRequest req);
}
```

### 10.2 FolderPermissionService

```java
public interface FolderPermissionService {
    boolean hasPermission(Long userId, Long folderId, FolderPermissionCode permissionCode);
    void checkPermission(Long folderId, FolderPermissionCode permissionCode);
    void checkRootSensitiveOperation(DocFolder folder);
    List<String> listPermissionCodes(Long userId, Long folderId);
    boolean isManager(Long userId, Long folderId);
}
```

### 10.3 FolderGrantService

```java
public interface FolderGrantService {
    List<FolderGrantVO> list(Long folderId);
    void add(Long folderId, FolderGrantSaveRequest req);
    void remove(Long folderId, Long grantId);
}
```

### 10.4 FolderManagerService

```java
public interface FolderManagerService {
    List<FolderManagerVO> list(Long folderId);
    void add(Long folderId, FolderManagerSaveRequest req);
    void remove(Long folderId, Long managerId);
}
```

### 10.5 FolderFavoriteService

```java
public interface FolderFavoriteService {
    void favorite(Long folderId);
    void unfavorite(Long folderId);
    PageResponse<FolderFavoriteVO> listMine(PageQuery query);
    boolean isFavorite(Long userId, Long folderId);
}
```

### 10.6 AuditService

```java
public interface AuditService {
    void record(AuditRecordCommand command);
}
```

`AuditRecordCommand`：

```java
@Data
@Builder
public class AuditRecordCommand {
    private String moduleCode;
    private String bizType;
    private Long bizId;
    private String operationType;
    private Object beforeData;
    private Object afterData;
    private Object extraData;
}
```

---

## 11. 核心业务流程

### 11.1 创建文件夹

流程：

1. 获取当前用户。
2. trim 并校验名称。
3. 如果 `parentId = 0`：
   - 校验 `SUPER_ADMIN`。
   - `level = 0`。
   - `ownerDeptId` 可为空。
4. 如果 `parentId != 0`：
   - 查询父目录，必须存在且未删除。
   - 校验父目录 `FOLDER_CREATE`。
   - `level = parent.level + 1`，不得超过 8。
   - `ancestorIds` 先插入后回填，或使用自定义 ID 生成后一次性写入。
   - `ownerDeptId` 默认继承父目录，父目录为空则使用当前用户部门。
5. 校验同级名称唯一。
6. 插入 `doc_folder`。
7. 如果根级：更新 `ancestorIds = id`。
8. 删除父节点 children 缓存。
9. 写审计日志 `CREATE`。

### 11.2 重命名

流程：

1. 查询目录。
2. 根级目录校验 `SUPER_ADMIN`。
3. 普通目录校验 `FOLDER_RENAME`。
4. trim 名称并校验。
5. 校验同级名称唯一。
6. 更新 name。
7. 删除父节点 children 缓存和当前节点详情相关缓存。
8. 写审计日志 `RENAME`，记录 before/after name。

### 11.3 移动

流程：

1. 查询源节点。
2. 查询目标父节点；`targetParentId = 0` 表示移动为根级，仅 `SUPER_ADMIN` 可操作，不建议 MVP 对普通目录开放移动到根级。
3. 根级源节点仅 `SUPER_ADMIN` 可移动。
4. 校验源节点 `FOLDER_MOVE`。
5. 校验目标父节点 `FOLDER_CREATE`。
6. 校验目标父不是自己。
7. 校验目标父不在源节点子树内。
8. 校验目标父下无同名目录。
9. 校验移动后最大层级不超过 8。
10. 事务内更新源节点和全部后代的 `ancestorIds/level`。
11. 删除原父、新父、源节点相关缓存。
12. 删除源子树相关权限缓存。
13. 写审计日志 `MOVE`。

### 11.4 复制

流程：

1. 查询源节点及子树。
2. 校验源节点 `FOLDER_COPY`。
3. 校验目标父节点 `FOLDER_CREATE`。
4. 校验复制后最大层级不超过 8。
5. 目标名称为空时自动生成 `原名称(复制)`，冲突时追加序号。
6. 事务内按层级顺序复制节点，建立 oldId -> newId 映射。
7. 不复制 grant/manager/favorite。
8. 新资源 owner：
   - `ownerDeptId` 继承目标父目录。
   - 目标父目录 owner 为空则使用当前用户部门。
   - `ownerUserId` 使用当前用户。
9. 删除目标父 children 缓存。
10. 写审计日志 `COPY`，extraData 记录 sourceFolderId、targetParentId、oldNewIdMap 可选。

### 11.5 删除 / 批量删除

流程：

1. 查询待删除节点。
2. 对传入 ID 做父子去重：如果某节点的祖先也在待删除集合中，则该节点无需单独处理。
3. 对每个保留节点：
   - 根级目录仅 `SUPER_ADMIN` 可删除。
   - 普通目录校验 `FOLDER_DELETE`。
4. 查询全部待删子树 ID。
5. 事务内逻辑删除：
   - `doc_folder`
   - `doc_folder_grant`
   - `doc_folder_manager`
   - `doc_folder_favorite`
6. 不删除 audit。
7. 删除相关 children/permission/favorite 缓存。
8. 写审计日志 `DELETE` 或 `BATCH_DELETE`。

### 11.6 授权

流程：

1. 查询目录。
2. 如果是根级目录，拒绝授权。
3. 校验当前用户 `FOLDER_GRANT` 或命中可管理范围。
4. 校验 `subjectType`、`subjectId`、`permissionCodes`、`grantScope`。
5. 如果 subjectType=ROLE，校验 roleCode 合法。
6. 如果 subjectType=USER，校验用户存在。
7. 如果 subjectType=DEPT，校验部门存在。
8. 插入授权记录；重复授权可返回资源冲突，也可幂等成功。建议 MVP 采用幂等成功。
9. 删除权限缓存。
10. 写审计日志 `GRANT_ADD`。

### 11.7 管理员设置

流程：

1. 查询目录。
2. 根级目录不建议设置 FolderManager；根级由 `SUPER_ADMIN` 管理。
3. 校验当前用户 `FOLDER_MANAGER_SET` 或 `SUPER_ADMIN`。
4. 校验被设置用户存在。
5. 校验被设置用户具备 `FOLDER_ADMIN` 角色。
6. 插入 `doc_folder_manager`。
7. 删除权限缓存。
8. 写审计日志 `MANAGER_ADD`。

### 11.8 收藏

流程：

1. 查询目录。
2. 根级目录禁止收藏。
3. 校验当前用户具备 `FOLDER_FAVORITE` 或至少 `FOLDER_VIEW`。建议采用 `FOLDER_VIEW` 即可收藏。
4. 若已收藏，幂等成功。
5. 插入 favorite。
6. 删除用户收藏缓存。
7. 写审计日志 `FAVORITE`。

---

## 12. 权限计算落地

### 12.1 超级管理员

```java
if (currentUser.isSuperAdmin()) {
    return true;
}
```

### 12.2 根级保护

对以下操作，如果目标是根级目录且当前用户不是 `SUPER_ADMIN`，直接拒绝：

```text
FOLDER_EDIT
FOLDER_RENAME
FOLDER_DELETE
FOLDER_MOVE
FOLDER_GRANT
FOLDER_MANAGER_SET
FOLDER_INHERIT_SET
```

### 12.3 FolderManager 命中规则

`manageScope = SELF`：仅 folderId 等于目标 folderId。  
`manageScope = SELF_AND_DESCENDANTS`：目标 folder 的 `ancestorIds` 包含 manager.folderId。

FolderManager 默认拥有：

```text
FOLDER_VIEW
FOLDER_CREATE
FOLDER_EDIT
FOLDER_RENAME
FOLDER_DELETE
FOLDER_MOVE
FOLDER_COPY
FOLDER_GRANT
FOLDER_MANAGER_SET
FOLDER_INHERIT_SET
FOLDER_SEARCH
FOLDER_FAVORITE
FOLDER_AUDIT_VIEW
```

但根级保护优先级高于 FolderManager。

### 12.4 显式授权命中规则

授权命中需要同时满足：

1. 授权未删除。
2. 授权未过期。
3. `permissionCode` 匹配。
4. subject 命中当前用户：
   - USER：`subjectId == userId`
   - ROLE：`subjectId in currentUser.roleCodes`
   - DEPT：`subjectId == currentUser.deptId`，MVP 暂不递归部门树，后续再扩展下级部门。
5. scope 命中：
   - SELF：授权 folderId 等于目标 folderId。
   - SELF_AND_DESCENDANTS：目标 folder 的 `ancestorIds` 包含授权 folderId。

### 12.5 权限继承

当目标节点 `inheritPermission = true` 时，沿父链向上查找授权；当遇到 `inheritPermission = false` 的节点时停止继续向上继承。

MVP 实现建议：

1. 查询当前节点到根的 ancestorIds。
2. 从当前节点向父级回溯。
3. 如果当前节点有显式授权，命中即返回 true。
4. 如果某节点 `inheritPermission = false` 且该节点不是目标节点，则停止继续回溯。
5. 根级目录不做资源授权，因此普通用户不会从根级继承授权。

---

## 13. 数据范围 AOP 落地

### 13.1 目标

数据范围不是替代资源权限，而是用于列表类查询时减少数据泄露和数据扫描范围。

### 13.2 注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {
    DataScopeType value() default DataScopeType.FOLDER;
}
```

### 13.3 Context

```java
@Data
public class DataScopeRule {
    private boolean superAdmin;
    private Long userId;
    private Long deptId;
    private List<String> roleCodes;
    private Set<Long> accessibleDeptIds;
}
```

```java
public class DataScopeContext {
    private static final ThreadLocal<DataScopeRule> HOLDER = new ThreadLocal<>();
    public static void set(DataScopeRule rule) { HOLDER.set(rule); }
    public static DataScopeRule get() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }
}
```

### 13.4 Aspect

AOP 只负责生成规则，不直接拼 SQL。

```java
@Around("@annotation(dataScope)")
public Object around(ProceedingJoinPoint pjp, DataScope dataScope) throws Throwable {
    UserContext.UserInfo user = UserContext.get();
    DataScopeRule rule = buildRule(user, dataScope.value());
    try {
        DataScopeContext.set(rule);
        return pjp.proceed();
    } finally {
        DataScopeContext.clear();
    }
}
```

### 13.5 Mapper 查询使用方式

Service 构造 query 时读取：

```java
DataScopeRule scope = DataScopeContext.get();
if (scope != null && !scope.isSuperAdmin()) {
    wrapper.and(w -> w
        .eq(DocFolder::getOwnerUserId, scope.getUserId())
        .or()
        .in(DocFolder::getOwnerDeptId, scope.getAccessibleDeptIds())
        .or()
        .in(DocFolder::getId, accessibleFolderIdsFromGrant)
    );
}
```

MVP 可先用“owner + grant 可访问目录 ID”实现，部门树递归可以后续增强。

---

## 14. 缓存设计

### 14.1 缓存服务

统一封装：

```java
public interface FolderCacheService {
    List<FolderTreeNodeVO> getChildren(Long parentId);
    void setChildren(Long parentId, List<FolderTreeNodeVO> children);
    List<String> getPermissions(Long userId, Long folderId);
    void setPermissions(Long userId, Long folderId, List<String> permissionCodes);
    void evictChildren(Long parentId);
    void evictPermission(Long userId, Long folderId);
    void evictFolderRelated(Long folderId);
    void evictUserFavorite(Long userId);
}
```

### 14.2 Key

```text
biddoc:{profile}:folder:children:{parentId}
biddoc:{profile}:folder:perm:{userId}:{folderId}
biddoc:{profile}:folder:favorite:{userId}
```

### 14.3 TTL

| 缓存 | TTL |
|---|---|
| children | 10 分钟 |
| permission | 5 分钟 |
| favorite | 5 分钟 |

### 14.4 MVP 可选策略

如果希望首版开发更稳，可以先只实现缓存失效接口为空实现，不实际接 Redis。等 folder 主功能跑通后再启用缓存。这样可以保证业务正确性优先。

---

## 15. 审计日志落地

### 15.1 必记事件

```text
CREATE
UPDATE
RENAME
DELETE
BATCH_DELETE
MOVE
COPY
GRANT_ADD
GRANT_REMOVE
MANAGER_ADD
MANAGER_REMOVE
FAVORITE
UNFAVORITE
INHERIT_SWITCH
```

### 15.2 AuditService.record 行为

1. 从 `UserContext` 获取 operatorUserId/operatorDeptId。
2. 从 MDC 获取 `traceId`。
3. 设置 `moduleCode = FOLDER`。
4. `beforeData/afterData/extraData` 序列化为 jsonb。
5. 失败时只记录系统日志，不应阻断主业务事务；但如果你希望强审计一致性，可让审计与业务同事务。MVP 建议同事务，方便排查。

### 15.3 folder 日志查询

查询条件：

```java
@Data
public class FolderAuditLogQuery extends PageQuery {
    private List<String> operationTypes;
    private Long operatorUserId;
    private Long operatorDeptId;
    private OffsetDateTime operationFrom;
    private OffsetDateTime operationTo;
}
```

---

## 16. 开发顺序建议

### 阶段 0：准备

1. 修正 Maven wrapper 执行权限。
2. 移动敏感配置到 `application-dev.yml`。
3. 修改 `PageResponse` 和新增 `PageQuery`。
4. 补充 ErrorCode。
5. 修正 `GlobalExceptionHandler` 的 BindException 导入。

验收：项目能本地编译启动，auth 原接口不破坏。

### 阶段 1：audit 最小闭环

1. 建 `audit_operation_log` 表。
2. 建 `AuditOperationLog` entity/mapper。
3. 建 `AuditService.record()`。
4. 建 `AuditQueryService.pageByBiz()`。

验收：能通过单元测试或接口调用写入并查询日志。

### 阶段 2：folder 基础 CRUD + 树

1. 建 folder 四张表。
2. 建 entity/mapper/dto/vo/converter。
3. 实现创建、编辑、重命名、详情。
4. 实现 rootTree/children 懒加载。
5. 实现同级唯一和最大层级校验。

验收：根级、二级、三级目录可创建和查询；同级重名失败；超过 8 级失败。

### 阶段 3：权限服务

1. 实现 `FolderPermissionService`。
2. 实现 `FolderGrantService`。
3. 实现 `FolderManagerService`。
4. 接入 Controller 权限校验。

验收：普通用户无授权不可见；显式授权可见；FolderManager 可管理被授予目录；根级保护生效。

### 阶段 4：移动、复制、删除

1. 实现子树查询。
2. 实现移动及 ancestorIds/level 批量更新。
3. 实现复制及 oldId/newId 映射。
4. 实现删除和批量删除去重。

验收：防循环、层级限制、名称冲突、授权不复制、收藏不复制全部通过。

### 阶段 5：收藏、检索、日志查询

1. 实现 favorite/unfavorite/listMine。
2. 实现 search。
3. 实现 folder 维度 audit logs 查询。
4. 加缓存或空缓存服务。

验收：我的收藏分页正确；检索只返回可访问目录；日志按 folder/operator/dept/time 过滤正确。

### 阶段 6：测试与文档

1. 补充接口文档注解。
2. 输出 Apifox/Knife4j 接口分组。
3. 补充数据库初始化 SQL。
4. 补充测试用例文档。

---

## 17. 验收测试清单

### 17.1 基础树测试

| 用例 | 预期 |
|---|---|
| SUPER_ADMIN 创建根级目录 | 成功 |
| 普通用户创建根级目录 | 失败 |
| 创建普通子目录 | 有父级 create 权限时成功 |
| 同一 parent 下同名 | 失败 |
| 不同 parent 下同名 | 成功 |
| 创建超过 8 级 | 失败 |

### 17.2 权限测试

| 用例 | 预期 |
|---|---|
| SUPER_ADMIN 查看所有目录 | 成功 |
| FOLDER_ADMIN 未被设置为 manager | 不自动拥有目录权限 |
| 被设置为 FolderManager | 可管理 scope 范围内目录 |
| DEPT_MANAGER 未授权 | 不自动拥有管理权限 |
| USER 显式 VIEW 授权 | 可查看 |
| ROLE 授权 | 拥有角色用户可访问 |
| DEPT 授权 | 同部门用户可访问 |
| inheritPermission=false | 不继续向上继承 |
| 授权过期 | 不生效 |

### 17.3 移动复制删除测试

| 用例 | 预期 |
|---|---|
| 移动到自身 | 失败 |
| 移动到子目录 | 失败 |
| 移动后超过 8 级 | 失败 |
| 移动到有同名目录的父级 | 失败 |
| 复制目录树 | 结构复制成功 |
| 复制不复制授权 | 新目录无源授权 |
| 复制不复制收藏 | 新目录无源收藏 |
| 批量删除父子同时传入 | 自动去重 |
| 删除目录后查详情 | 不存在 |
| 删除目录后审计日志 | 仍保留 |

### 17.4 收藏和检索测试

| 用例 | 预期 |
|---|---|
| 收藏普通目录 | 成功 |
| 重复收藏 | 幂等成功 |
| 取消未收藏目录 | 幂等成功 |
| 收藏根级目录 | 失败 |
| 我的收藏 | 只返回当前用户收藏且可访问目录 |
| 名称模糊检索 | 返回匹配结果 |
| favoriteOnly=true | 只返回收藏目录 |
| onlyAccessible=true | 不返回无权限目录 |

### 17.5 审计测试

| 操作 | 预期日志 |
|---|---|
| 创建 | CREATE |
| 编辑 | UPDATE |
| 重命名 | RENAME |
| 删除 | DELETE/BATCH_DELETE |
| 移动 | MOVE，含旧父/新父 |
| 复制 | COPY，含 sourceFolderId |
| 授权新增 | GRANT_ADD |
| 授权删除 | GRANT_REMOVE |
| 管理员新增 | MANAGER_ADD |
| 管理员删除 | MANAGER_REMOVE |
| 收藏 | FAVORITE |
| 取消收藏 | UNFAVORITE |

---

## 18. 风险与处理策略

### 18.1 最大风险：权限计算复杂

处理：先不做显式拒绝、部门树递归、文档级联权限，只做允许授权和 folder 自身权限。

### 18.2 子树移动/复制容易污染 ancestorIds

处理：移动和复制必须单独写事务测试，至少覆盖三层树。

### 18.3 审计数据与业务事务一致性

处理：MVP 同事务写审计，后续性能压力出现后再改异步事件。

### 18.4 PageResponse 改造影响 auth

处理：当前 auth 没有分页接口，改造影响较小。若未来已有前端依赖旧结构，则先新增 V2 PageResponse 或保留兼容字段。

### 18.5 createdBy 类型历史包袱

处理：folder 首版沿用 String；后续全局审计字段统一时再整体迁移为 Long。

---

## 19. 本文建议落库到项目的位置

建议将本文保存为：

```text
docs/folder/folder-module-development-doc-v4.md
```

同时保留：

```text
docs/folder/folder-module-requirements-design-v3.md
```

文档分工：

- V3：需求与架构设计基线。
- V4：开发实施与验收基线。

---

## 20. 下一步最小执行清单

1. 新增 `docs/folder/folder-module-development-doc-v4.md`。
2. 新增 SQL：`docs/sql/V4__folder_audit_init.sql`。
3. 先改 common：`PageQuery`、`PageResponse`、`ErrorCode`。
4. 先落 audit 最小模块。
5. 再落 folder CRUD/tree。
6. 最后落授权、移动复制删除、收藏检索审计查询。

