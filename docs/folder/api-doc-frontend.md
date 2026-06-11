# bid-doc-system 后端接口文档（前端联调用）

> **文档版本**: v2.2
> **最后更新**: 2026-06-11
> **适用范围**: auth + folder + document 模块完整接口
> **接口前缀**: 除 `/actuator` 等基础设施外，业务接口统一前缀 `/api/v1`
> **本文档以实际代码为准**，所有列出的接口均已实现并可用

---

## 1. 通用约定

### 1.1 统一响应包装

所有业务接口的响应体形如：

```json
{
  "code": 0,
  "message": "success",
  "data": { /* 接口返回数据，可能为 null */ },
  "timestamp": 1779552230324
}
```

- `code = 0` 表示成功；非 0 一律视为错误，详见 §3 错误码。
- `message` 成功时为 `"success"`，失败时为中文错误描述。
- `data` 类型随接口而变；某些接口无返回数据时为 `null`。
- `timestamp` 服务端时间戳（毫秒）。

### 1.2 鉴权

- 登录后服务端通过 Sa-Token 颁发 token。当前配置 `sa-token.token-prefix` 为空，前端在后续请求中通过请求头 `Authorization: <token>` 携带登录响应里的 `data.token` 原值。
- token 失效场景：默认 30 天物理超时 + 30 分钟无操作超时；登录时如携带 `rememberMe=true`，活跃超时延长为 5 天（详见 §5.2）。
- 未登录或 token 无效会返回 `code = 4011001` 或被 Sa-Token 框架拦截抛出 401（具体形式由框架决定）。

### 1.3 数据类型约定

- **Long 主键**：JSON 序列化为**字符串**（避免 JS 精度丢失），例：`"id": "2058217319302139906"`。请求体中前端可传字符串或数字，后端自动转换。
- **OffsetDateTime**：ISO-8601 带时区，例：`"2026-05-24T00:03:20.123456Z"`。
- **LocalDateTime**（如有）：`yyyy-MM-dd HH:mm:ss` 格式。
- **布尔**：`true` / `false`。

### 1.4 字符集 & 编码

- 请求 / 响应一律 `application/json; charset=UTF-8`。

### 1.5 已实现模块

✅ **已实现并可用**：
- **认证模块**：登录 / 注册 / 退出 / 当前用户
- **用户模块**：详情 / 启用禁用
- **角色模块**：分配 / 撤销 / 查询
- **部门模块**：创建 / 列表
- **文件夹模块**：CRUD / 删除 / 批量删除 / 移动 / 复制 / 授权管理 / 管理员管理
- **文件夹收藏**：收藏 / 取消收藏 / 我的收藏
- **审计模块**：审计日志分页查询（SUPER_ADMIN）
- **文档标签**：标签管理 / 文档绑定标签 / 按标签搜索
- **通知模块**：站内通知 / 未读数 / 标记已读
- **工作流模块**：文档轻量审批
- **文档模块**：上传 / 下载 / 预览 / 版本管理 / 搜索 / 统计 / 筛选增强

🚧 **后续阶段**（暂不可用）：
- 外链分享
- OCR / Office 转 PDF
- Elasticsearch 全文搜索

---

## 2. 常量参考

### 2.1 角色码（RoleCodeEnum）

| code | 含义 |
|---|---|
| `SUPER_ADMIN` | 超级管理员；全局放行 |
| `FOLDER_ADMIN` | 文件夹管理员（资格角色，本身不带全局权限） |
| `DEPT_MANAGER` | 部门经理 |
| `EMPLOYEE` | 普通员工（注册时默认分配） |

### 2.2 文件夹权限码（FolderPermissionCodeEnum）

授权时 `permissionCodes` 字段取值范围：

| code | 含义 |
|---|---|
| `FOLDER_VIEW` | 查看文件夹 |
| `FOLDER_CREATE` | 创建子文件夹 |
| `FOLDER_EDIT` | 编辑（remark / status / inheritPermission / sortNo） |
| `FOLDER_RENAME` | 重命名 |
| `FOLDER_DELETE` | 删除 |
| `FOLDER_MOVE` | 移动 |
| `FOLDER_COPY` | 复制 |
| `FOLDER_GRANT` | 授权管理 |
| `FOLDER_MANAGER_SET` | 管理员设置 |
| `FOLDER_FAVORITE` | 收藏（当前收藏接口按 FOLDER_VIEW 校验） |
| `FOLDER_AUDIT_VIEW` | 查看审计日志（当前审计查询 v1 仅 SUPER_ADMIN） |

### 2.3 授权主体类型（subjectType）

| code | subjectId 字段含义 |
|---|---|
| `USER` | 用户 id（字符串形式的 Long） |
| `ROLE` | 角色码（取自 RoleCodeEnum） |
| `DEPT` | 部门 id（字符串形式的 Long） |

### 2.4 授权 / 管理范围（grantScope / manageScope）

| code | 含义 |
|---|---|
| `SELF` | 仅当前节点 |
| `SELF_AND_DESCENDANTS` | 当前节点及全部后代 |

---

## 3. 错误码

| code | 语义 | 典型触发 |
|---|---|---|
| `0` | 成功 | — |
| `4001001` | 参数校验失败 | Jakarta Validation 报错；后端业务前置参数非法 |
| `4001002` | 资源冲突 | — |
| `4001003` | 业务规则非法 | — |
| `4002001` | 同级文件夹名称已存在 | create / rename / move / copy 同父下重名 |
| `4002002` | 文件夹层级超出限制 | 最大 8 层（root=0），create / move / copy 越限 |
| `4002003` | 当前文件夹存在子节点 | 暂未使用（cascade 删除走级联，不报此码） |
| `4002004` | 授权已存在 | grant add 唯一索引冲突 |
| `4002005` | 根级文件夹不允许授权 | grant add 目标为 root |
| `4002006` | 授权主体非法 | subjectType / ROLE 的 subjectId 非法 |
| `4002007` | 该用户已是管理员 | manager add 唯一索引冲突 |
| `4002008` | 不能移动到自身或子目录下 | move 自环 / 后代循环 |
| `4002009` | 目标父目录无效 | move targetParentId=0 / target=当前父（noop） |
| `4002101` | 上传文件不能为空 | 文档上传 / 上传新版本未传文件或空文件 |
| `4002102` | 文件大小超出限制 | 文件大小超过 50MB |
| `4002103` | 文档名称非法 | 文档名为空、超过 255 字符，或包含 `/`、`\`、空字符 |
| `4002104` | 同文件夹下文档名称已存在 | 上传新文档时同目录重名 |
| `4002105` | 根级文件夹不允许直接上传文档 | folderId=0 或根级目录上传 |
| `4002106` | 当前文件类型不支持预览 | 预览 Office / 压缩包等浏览器不能直接预览的类型 |
| `4002201` | 标签名称已存在 | 创建标签重名 |
| `4004001` | 审批任务不是待处理状态 | 重复审批或处理已完成任务 |
| `4011001` | 认证失败 | 用户名/密码不匹配；token 异常 |
| `4011002` | 账号不可用 | status=0 |
| `4011003` | 登录受限 | — |
| `4031001` | 权限不足 | 通用权限拒绝 |
| `4031002` | 角色权限不匹配 | `@SaCheckRole` 拦截 |
| `4032001` | 无文件夹操作权限 | folder permission 校验失败 |
| `4041001` | 资源不存在 | — |
| `4042001` | 文件夹不存在 | folder id 找不到或已逻辑删除 |
| `4042002` | 父文件夹不存在 | parentId / targetParentId 非法 |
| `4042003` | 授权记录不存在 | grant delete 目标 id 不存在或跨 folder |
| `4042004` | 管理员记录不存在 | manager delete 目标 id 不存在或跨 folder |
| `4042101` | 文档不存在 | document id 找不到或已逻辑删除 |
| `4042102` | 文档版本不存在 | 指定 versionNo 不存在 |
| `4042201` | 标签不存在 | 标签删除 / 文档绑定标签时 id 不存在 |
| `4043001` | 通知不存在 | 标记非本人通知或不存在通知 |
| `4044001` | 审批任务不存在 | 审批任务 id 不存在 |
| `4044002` | 审批实例不存在 | 审批实例 id 不存在 |
| `4222001` | 用户未具备 FOLDER_ADMIN 角色 | manager add 时被设置人无 FOLDER_ADMIN |
| `4032101` | 文档操作权限不足 | 非文档所有者、文件夹管理员或超级管理员执行删除 / 上传新版本 |
| `4034001` | 无权处理该审批任务 | 非审批人处理审批任务 |
| `5001001` | 系统异常 | 未捕获的运行时异常 |
| `5003001` | 审计记录失败 | audit 写入失败 |
| `5003002` | 审计查询失败 | — |
| `5005001` | 文件存储写入失败 | 上传文件写入存储失败 |
| `5005002` | 存储对象不存在 | 下载时存储对象缺失 |
| `5005003` | 文件存储读取失败 | 下载时读取存储失败 |

---

## 4. 认证接口 `/api/v1/auth`

### 4.1 用户注册

```
POST /api/v1/auth/register
```

**请求体**

```json
{
  "username": "alice",
  "password": "12345678",
  "realName": "张三",
  "email": "alice@example.com",
  "mobile": "13900000001",
  "deptId": "2058217226998091777",
  "jobLevel": 3
}
```

| 字段 | 类型 | 必填 | 校验 |
|---|---|---|---|
| username | string | 是 | 4–20 字符 |
| password | string | 是 | ≥ 8 字符 |
| realName | string | 是 | 非空 |
| email | string | 是 | 邮箱格式 |
| mobile | string | 是 | 非空 |
| deptId | string\|number | 是 | 已存在的部门 id |
| jobLevel | int | 是 | 非空 |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": { "userId": "2058217227220389889", "status": "PENDING" },
  "timestamp": 1779552230324
}
```

注册后用户默认 `status=0`（禁用），需 SUPER_ADMIN 调用 §5.2 启用接口后方可登录。

---

### 4.2 用户登录

```
POST /api/v1/auth/login
```

**请求体**

```json
{
  "username": "alice",
  "password": "12345678",
  "rememberMe": false
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| username | string | 是 | — |
| password | string | 是 | — |
| rememberMe | boolean | 否 | `true` 则活跃超时延长到 5 天；省略或 `false` 走默认 30 分钟无操作超时 |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "xxxxxx-xxxxxxxx-...",
    "expireIn": 2592000,
    "user": {
      "id": "2058217227220389889",
      "username": "alice",
      "realName": "张三",
      "email": "alice@example.com",
      "mobile": "13900000001",
      "deptId": "2058217226998091777",
      "jobLevel": 3,
      "status": 1,
      "roleCodes": ["EMPLOYEE"]
    },
    "roleCodes": ["EMPLOYEE"]
  },
  "timestamp": 1779552230324
}
```

- `token` 为 Sa-Token 原始 token 值；当前后端配置无 `Bearer` 前缀，前端直接把该值放入 `Authorization` 请求头即可。
- `expireIn` 单位秒。

**典型错误**

- `4011001` 用户名或密码错误
- `4011002` 账号未启用

---

### 4.3 当前登录用户

```
GET /api/v1/auth/me
```

**请求头**

```
Authorization: <token>
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "2058217227220389889",
    "username": "alice",
    "realName": "张三",
    "email": "alice@example.com",
    "mobile": "13900000001",
    "deptId": "2058217226998091777",
    "jobLevel": 3,
    "status": 1,
    "roleCodes": ["EMPLOYEE"]
  },
  "timestamp": 1779552230324
}
```

---

### 4.4 退出登录

```
POST /api/v1/auth/logout
```

**请求头**：需 `Authorization`。
**响应**：`code=0, data=null`。

---

## 5. 用户接口 `/api/v1/users`

### 5.1 查询用户详情

```
GET /api/v1/users/{id}
```

**响应**：与 §4.3 的 `data` 同结构（含 `roleCodes`）。

---

### 5.2 启用 / 禁用用户

```
PATCH /api/v1/users/{id}/status
```

**请求体**

```json
{ "status": 1 }
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| status | int | 是 | `1` 启用 / `0` 禁用 |

**响应**：`code=0, data=null`。

---

## 6. 角色接口 `/api/v1/roles`

> 除 `GET /user/{userId}` 外，其余接口需要 `SUPER_ADMIN` 角色。

### 6.1 为用户分配角色

```
POST /api/v1/roles/assign
```

**请求体**

```json
{
  "userId": "2058217227220389889",
  "roleCode": "FOLDER_ADMIN",
  "isPrimary": false,
  "effectiveStartTime": null,
  "effectiveEndTime": null
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userId | string\|number | 是 | — |
| roleCode | string | 是 | 取自 §2.1 |
| isPrimary | boolean | 否 | 是否设为主角色 |
| effectiveStartTime | datetime | 否 | 为空立即生效 |
| effectiveEndTime | datetime | 否 | 为空永久有效 |

---

### 6.2 撤销用户角色

```
DELETE /api/v1/roles/revoke?userId={userId}&roleCode={roleCode}
```

**响应**：`code=0, data=null`。

---

### 6.3 查询用户的角色列表

```
GET /api/v1/roles/user/{userId}
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": "...",
      "userId": "2058217227220389889",
      "roleCode": "FOLDER_ADMIN",
      "roleName": "文件夹管理员",
      "isPrimary": false,
      "status": 1,
      "effectiveStartTime": null,
      "effectiveEndTime": null,
      "sourceType": 1,
      "createdAt": "2026-05-24T00:03:20.123456Z"
    }
  ],
  "timestamp": 1779552230324
}
```

---

### 6.4 查询某角色下的用户 id 列表（仅 SUPER_ADMIN）

```
GET /api/v1/roles/code/{roleCode}/users
```

**响应**

```json
{ "code": 0, "data": ["2058217227220389889", "2058217227287498754"], "message": "success", "timestamp": ... }
```

---

## 7. 部门接口 `/api/v1/departments`

### 7.1 创建部门

```
POST /api/v1/departments
```

**请求体**

```json
{
  "name": "研发部",
  "parentId": 1,
  "level": 2,
  "remark": "技术中心下属研发部门"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | string | 是 | 非空 |
| parentId | string\|number | 是 | 顶级部门传 `1`（默认 root） |
| level | int | 是 | 仅允许 `1` 或 `2` |
| remark | string | 否 | — |

**响应**：`code=0, data=null`。

---

### 7.2 查询部门列表（平铺）

```
GET /api/v1/departments
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": "2058217226998091777",
      "name": "研发部",
      "parentId": "1",
      "level": 2,
      "managerUserId": null,
      "status": 1,
      "remark": "...",
      "createdAt": "2026-05-24T00:03:20.123456Z",
      "createdBy": "1",
      "updatedAt": "2026-05-24T00:03:20.123456Z",
      "updatedBy": "1",
      "extensionData": null
    }
  ],
  "timestamp": 1779552230324
}
```

> 当前为平铺列表，未构造树形结构。前端如需树，自行按 `parentId` 组装。

---

## 8. 文件夹接口 `/api/v1/folders`

### 8.0 权限模型速览

判定顺序（从高到低）：

```
SUPER_ADMIN 全局放行
  → 根级保护（写操作仅 SUPER_ADMIN）
  → FolderManager 命中（SELF / SELF_AND_DESCENDANTS）
  → owner（owner_user_id 等于当前用户）
  → 同部门可查看（仅 view）
  → 显式 grant 命中（含父继承）
  → 拒绝（4032001）
```

- **根级目录**（level=0）：创建、编辑、重命名、删除均仅限 SUPER_ADMIN；不允许授权（grant add 报 4002005）；不允许设管理员；普通用户在树中仅作为"路径容器"可见（前提是其下有可见后代）。
- **同名唯一**：同一 `parentId` 下名称唯一（不区分大小写按字符串匹配）。
- **最大层级**：8（root=0，最深可至 level=8）。

---

### 8.1 创建文件夹

```
POST /api/v1/folders
```

**请求体**

```json
{ "parentId": 0, "name": "投标资料", "remark": "总根" }
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| parentId | string\|number | 否 | `0` 或省略 = 创建根级（仅 SUPER_ADMIN）；其他值需对父有 FOLDER_CREATE |
| name | string | 是 | 非空，trim 后做同级唯一校验 |
| remark | string | 否 | — |

**响应**

```json
{ "code": 0, "data": { "id": "2058217227685957633" }, "message": "success", "timestamp": ... }
```

**典型错误**：4032001 / 4002001 / 4002002 / 4042002

---

### 8.2 查询详情

```
GET /api/v1/folders/{id}
```

需 `FOLDER_VIEW`（owner / 同部门 / manager / grant 任一命中）。

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "2058217227685957633",
    "parentId": "0",
    "name": "投标资料",
    "ancestorIds": "2058217227685957633",
    "level": 0,
    "sortNo": 1,
    "ownerDeptId": "1",
    "ownerUserId": "1",
    "inheritPermission": true,
    "status": 1,
    "remark": "总根",
    "createdAt": "2026-05-24T00:03:20.123456Z",
    "createdBy": "1",
    "updatedAt": "2026-05-24T00:03:20.123456Z",
    "updatedBy": "1"
  },
  "timestamp": 1779552230324
}
```

> `ancestorIds` 是逗号分隔的祖先链（**包含自身**），如 root="100"、子="100,200"、孙="100,200,300"。

---

### 8.3 重命名

```
PATCH /api/v1/folders/{id}/name
```

需 `FOLDER_RENAME`；根级仅 SUPER_ADMIN。

**请求体**

```json
{ "name": "投标资料-2026" }
```

**响应**：`code=0, data=null`。

**典型错误**：4032001 / 4002001 / 4042001

---

### 8.4 编辑（remark / status / inheritPermission / sortNo）

```
PUT /api/v1/folders/{id}
```

需 `FOLDER_EDIT`；根级仅 SUPER_ADMIN。

**请求体**

```json
{
  "remark": "更新备注",
  "status": 1,
  "inheritPermission": false,
  "sortNo": 5
}
```

| 字段 | 类型 | 必填 | 校验 |
|---|---|---|---|
| remark | string | 否 | — |
| status | int | 是 | 仅允许 `0` 或 `1` |
| inheritPermission | boolean | 是 | — |
| sortNo | int | 是 | ≥ 1 |

**响应**：`code=0, data=null`。

> 注意：本接口**不支持**修改 `name` / `parentId` / `ancestorIds` / `level`。重命名走 §8.3；移动走 §8.9。

---

### 8.5 子节点列表（懒加载）

```
GET /api/v1/folders/children?parentId={parentId}
```

- `parentId` 省略 / 0 = 列根级（与 §8.6 等价）。
- 非根 `parentId`：当前用户必须对该父目录有 view 权限，否则报 4032001。
- 返回列表已根据当前用户权限过滤（不可见的子目录会被剔除）。

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": "2058217228453515266",
      "parentId": "2058217227685957633",
      "name": "技术标",
      "level": 1,
      "sortNo": 1,
      "hasChildren": true,
      "createdAt": "2026-05-24T00:03:20.123456Z"
    }
  ],
  "timestamp": 1779552230324
}
```

> `hasChildren` 已基于当前用户权限计算（只算可见子节点）；不会泄露不可见的孙节点信息。

---

### 8.6 根树

```
GET /api/v1/folders/tree/root
```

等同于 `GET /folders/children?parentId=0`。返回当前用户可见的根级目录列表。

---

### 8.7 删除（单个）

```
DELETE /api/v1/folders/{id}
```

需 `FOLDER_DELETE`；根级仅 SUPER_ADMIN。**级联软删** 自身及全部后代 + 相关的 grant / manager / favorite。

**响应**：`code=0, data=null`。审计 op = `DELETE`。

**典型错误**：4032001 / 4042001

---

### 8.8 批量删除

```
DELETE /api/v1/folders/batch
```

**请求体**

```json
{ "folderIds": ["2058217312796774401", "2058217319302139908"] }
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| folderIds | array<string\|number> | 是 | 非空数组 |

**语义**：
- **父子去重**：若集合内某 id 的祖先也在集合中，则该 id 被祖先覆盖，不再单独处理（但仍会作为后代被级联删除）。
- **任一 id 不存在则整体回滚**（事务原子），返回 `4042001`。
- 每个保留根节点逐个做权限校验，任一拒绝即整体失败 `4032001`。

**响应**：`code=0, data=null`。审计 op = `BATCH_DELETE`（即使 folderIds 只含 1 个 id 也是此 op）。

---

### 8.9 移动

```
PATCH /api/v1/folders/{id}/move
```

需源 `FOLDER_MOVE` + 目标父 `FOLDER_CREATE`。

**请求体**

```json
{ "targetParentId": "2058217319365054465" }
```

| 字段 | 类型 | 必填 | 约束 |
|---|---|---|---|
| targetParentId | string\|number | 是 | 必须 > 0；MVP 不允许移动到根级 |

**校验链**（按顺序）：

1. 源存在 → 否则 `4042001`
2. `targetParentId != 0` → 否则 `4002009`
3. 目标父存在 → 否则 `4042002`
4. 目标父 ≠ 源 → 否则 `4002008`
5. 目标父 ≠ 源当前父（避免 noop） → 否则 `4002009`
6. 目标父不在源子树中（防循环） → 否则 `4002008`
7. 源不是根级 + 权限校验 → 否则 `4032001`
8. 目标父下无同名 → 否则 `4002001`
9. 源子树最大层级 + delta ≤ 8 → 否则 `4002002`

事务内完成源节点 + 全部后代的 `ancestor_ids` 前缀重写和 `level` 偏移。审计 op = `MOVE`，`extraData.affectedFolderIds` 含源+所有后代 id 列表。

**响应**：`code=0, data=null`。

---

### 8.10 复制

```
POST /api/v1/folders/{id}/copy
```

需源 `FOLDER_COPY` + 目标父 `FOLDER_CREATE`。

**请求体**

```json
{
  "targetParentId": "2058217319365054465",
  "targetName": "投标资料-副本"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| targetParentId | string\|number | 是 | 必须 > 0 |
| targetName | string | 否 | 新根节点名；为空则沿用源名，遇同名报 `4002001`，前端可二次请求带新 name |

**复制语义**：

- 复制**整个子树**（含所有后代）。
- **不复制** grant / manager / favorite。
- 新节点 `ownerUserId = 当前用户`；`ownerDeptId` 继承目标父，目标父无则用当前用户部门兜底。
- 子树相对排序（sortNo）保留；新根节点 sortNo 取目标父下 max+1。

**校验**：

- 源存在 / 目标父存在 / 权限 / 目标父下无同名（同 §8.9）
- 复制后最大层级 ≤ 8（`target.level + 1 + 源子树高度`），否则 `4002002`

**响应**

```json
{ "code": 0, "data": { "id": "2058217348263809026" }, "message": "success", "timestamp": ... }
```

`data.id` 是新根节点 id。审计 op = `COPY`，`afterData.rootNewId` = 此 id，`afterData.copiedCount` = 复制的节点总数。

---

## 9. 文件夹授权 `/api/v1/folders/{folderId}/grants`

> **根级目录**（level=0）不允许设置授权：grant add 返回 `4002005`。
> 增删授权需要当前用户对该目录有"管理资格"：SUPER_ADMIN / owner / 该目录有效 manager 之一。

### 9.1 查询授权列表

```
GET /api/v1/folders/{folderId}/grants
```

需对该目录有 view 权限。返回未删除的 grant 列表，按 createdAt 倒序。

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "grantId": "2058215576308776961",
      "folderId": "2058215512156897281",
      "subjectType": "USER",
      "subjectId": "2058215510848274433",
      "permissionCode": "FOLDER_VIEW",
      "grantScope": "SELF",
      "effectiveFrom": null,
      "effectiveTo": null,
      "createdAt": "2026-05-24T00:03:20.123456Z",
      "createdBy": "1"
    }
  ],
  "timestamp": 1779552230324
}
```

---

### 9.2 新增授权

```
POST /api/v1/folders/{folderId}/grants
```

**请求体**

```json
{
  "subjectType": "USER",
  "subjectId": "2058215510848274433",
  "permissionCodes": ["FOLDER_VIEW", "FOLDER_EDIT"],
  "grantScope": "SELF",
  "effectiveFrom": null,
  "effectiveTo": null
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| subjectType | string | 是 | `USER` / `ROLE` / `DEPT` |
| subjectId | string | 是 | 长度 ≤ 64；ROLE 传 roleCode，其它传 id |
| permissionCodes | array<string> | 是 | 取自 §2.2 |
| grantScope | string | 是 | `SELF` / `SELF_AND_DESCENDANTS` |
| effectiveFrom | datetime | 否 | 为空立即生效 |
| effectiveTo | datetime | 否 | 为空永久有效 |

**语义**：一次请求会为 `permissionCodes` 中**每一项**分别插入一条 grant 记录（即一次调用可建多条）。重复授权（同 folder / subject / permissionCode）返回 `4002004`。

**响应**：`code=0, data=null`。

**典型错误**：4002005（根级禁止） / 4032001（无管理资格） / 4002004（重复） / 4002006（subjectType 非法 / ROLE 的 roleCode 非法）

---

### 9.3 删除授权

```
DELETE /api/v1/folders/{folderId}/grants/{grantId}
```

**响应**：`code=0, data=null`。

**典型错误**：`4042003`（grantId 不存在或不属于该 folderId） / `4032001`（无管理资格）

---

## 10. 文件夹管理员 `/api/v1/folders/{folderId}/managers`

> **根级目录**不允许设置管理员（接口会以 `4032001` "根级不允许设置管理员"拒绝）。
> 增删 manager 需要当前用户对该目录有管理资格（SUPER_ADMIN / owner / manager 之一）。
> **被设置为管理员的用户必须持有 `FOLDER_ADMIN` 角色**（仅资格角色，无业务自动权限），否则报 `4222001`。

### 10.1 查询管理员列表

```
GET /api/v1/folders/{folderId}/managers
```

需对该目录有 view 权限。返回未删除的 manager 列表，按 createdAt 倒序。

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "managerId": "2058215576891785218",
      "folderId": "2058215512156897281",
      "userId": "2058215513901727746",
      "manageScope": "SELF_AND_DESCENDANTS",
      "createdAt": "2026-05-24T00:03:20.123456Z",
      "createdBy": "1"
    }
  ],
  "timestamp": 1779552230324
}
```

---

### 10.2 新增管理员

```
POST /api/v1/folders/{folderId}/managers
```

**请求体**

```json
{
  "userId": "2058215513901727746",
  "manageScope": "SELF_AND_DESCENDANTS"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userId | string\|number | 是 | 被设置为 manager 的用户，必须持有 `FOLDER_ADMIN` 角色 |
| manageScope | string | 是 | `SELF` / `SELF_AND_DESCENDANTS` |

**响应**：`code=0, data=null`。

**典型错误**：4222001（无 FOLDER_ADMIN 角色） / 4002007（重复） / 4032001（无管理资格 / 根级）

---

### 10.3 删除管理员

```
DELETE /api/v1/folders/{folderId}/managers/{managerId}
```

**响应**：`code=0, data=null`。

**典型错误**：`4042004`（managerId 不存在或不属于该 folderId） / `4032001`

---

## 10A. 文件夹收藏 `/api/v1/folders`

> 收藏接口用于前端“我的收藏 / 常用目录”。v1 不单独使用 `FOLDER_FAVORITE` 权限点，收藏前只校验当前用户对目标文件夹具备 `FOLDER_VIEW`。

### 10A.1 我的收藏

```
GET /api/v1/folders/favorites
```

返回当前用户收藏且仍可查看的文件夹列表，按收藏时间倒序。若收藏夹中的目录已删除或当前用户已失去查看权限，结果中会被过滤。

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "favoriteId": "2058215576891785218",
      "folderId": "2058215512156897281",
      "parentId": "2058215512005902336",
      "folderName": "技术标",
      "level": 2,
      "sortNo": 1,
      "createdAt": "2026-06-11T10:30:00Z",
      "createdBy": "7"
    }
  ],
  "timestamp": 1779552230324
}
```

### 10A.2 收藏文件夹

```
POST /api/v1/folders/{folderId}/favorite
```

需要对目标文件夹有 `FOLDER_VIEW` 权限。重复收藏保持幂等，仍返回成功。真实新增收藏时写审计 `FAVORITE`。

**响应**：`code=0, data=null`。

**典型错误**：`4042001`（文件夹不存在） / `4032001`（无查看权限）

### 10A.3 取消收藏

```
DELETE /api/v1/folders/{folderId}/favorite
```

需要对目标文件夹有 `FOLDER_VIEW` 权限。重复取消保持幂等，仍返回成功。真实取消收藏时写审计 `UNFAVORITE`。

**响应**：`code=0, data=null`。

**典型错误**：`4042001`（文件夹不存在） / `4032001`（无查看权限）

---

## 10B. 审计日志 `/api/v1/audit`

> v1 仅 `SUPER_ADMIN` 可查询全量审计日志。普通用户访问会返回 `4031002`。

### 10B.1 查询审计日志

```
GET /api/v1/audit/logs
```

**查询参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| moduleCode | string | 否 | - | 模块码，如 `FOLDER` / `DOCUMENT` |
| bizType | string | 否 | - | 业务类型，如 `FOLDER` / `DOCUMENT` |
| bizId | string | 否 | - | 业务对象 ID |
| operationType | string | 否 | - | 操作类型，如 `DOWNLOAD` / `PREVIEW` / `FAVORITE` |
| operatorUserId | string | 否 | - | 操作人用户 ID |
| operatorDeptId | string | 否 | - | 操作人部门 ID |
| startTime | datetime | 否 | - | 操作时间起点 |
| endTime | datetime | 否 | - | 操作时间终点 |
| page | int | 否 | 1 | 页码 |
| size | int | 否 | 20 | 每页数量，最大 100 |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": "2058215576891785218",
        "moduleCode": "DOCUMENT",
        "bizType": "DOCUMENT",
        "bizId": "2058215512156897281",
        "operationType": "PREVIEW",
        "operatorUserId": "7",
        "operatorDeptId": "100",
        "requestId": "trace-id",
        "operationTime": "2026-06-11T10:30:00Z",
        "beforeData": null,
        "afterData": { "versionNo": 1, "size": 102400 },
        "extraData": { "ipAddress": "127.0.0.1" },
        "createdAt": "2026-06-11T10:30:00Z",
        "createdBy": "7"
      }
    ],
    "page": 1,
    "size": 20,
    "total": 1,
    "totalPages": 1,
    "hasNext": false
  },
  "timestamp": 1779552230324
}
```

---

## 10C. 文件夹前端支撑接口 `/api/v1/folders`

### 10C.1 当前用户对文件夹的权限

```
GET /api/v1/folders/{id}/permissions/me
```

返回当前用户对指定文件夹的可操作能力。前端可用它控制按钮显示，但真实安全边界仍以后端操作接口校验为准。

**响应 data**

```json
{
  "canView": true,
  "canCreateChild": true,
  "canRename": false,
  "canEdit": false,
  "canDelete": false,
  "canMove": false,
  "canCopy": true,
  "canFavorite": true,
  "isOwner": false,
  "isManager": false,
  "isSuperAdmin": false
}
```

### 10C.2 目录检索

```
GET /api/v1/folders/search
```

**查询参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| keyword | string | 否 | - | 文件夹名称关键词 |
| parentId | string | 否 | 0 | 限定父目录 |
| recursive | boolean | 否 | true | 是否递归子树 |
| favoriteOnly | boolean | 否 | false | 仅返回我的收藏目录 |
| page | int | 否 | 1 | 页码 |
| size | int | 否 | 20 | 每页数量，最大 100 |

返回 `PageResponse<FolderTreeNodeRespDTO>`。

---

## 10D. 标签 `/api/v1`

### 10D.1 标签列表

```
GET /api/v1/tags
```

普通登录用户可读取标签列表。

### 10D.2 创建标签

```
POST /api/v1/tags
```

请求体：

```json
{ "name": "投标" }
```

仅 `SUPER_ADMIN` 或 `FOLDER_ADMIN` 可维护标签。

### 10D.3 删除标签

```
DELETE /api/v1/tags/{id}
```

仅 `SUPER_ADMIN` 或 `FOLDER_ADMIN` 可删除标签；删除后同步移除文档标签关系。

### 10D.4 绑定文档标签

```
PUT /api/v1/documents/{id}/tags
```

请求体：

```json
{ "tagIds": ["2058215576891785218"] }
```

绑定采用全量替换语义。

### 10D.5 查询文档标签

```
GET /api/v1/documents/{id}/tags
```

返回该文档当前绑定的标签列表。

---

## 10E. 通知 `/api/v1/notifications`

### 10E.1 通知列表

```
GET /api/v1/notifications?page=1&size=20&unreadOnly=false
```

返回当前用户站内通知分页列表。

### 10E.2 未读数

```
GET /api/v1/notifications/unread-count
```

响应：

```json
{ "code": 0, "data": { "count": 3 }, "message": "success", "timestamp": 1779552230324 }
```

### 10E.3 标记已读

```
PATCH /api/v1/notifications/{id}/read
PATCH /api/v1/notifications/read-all
```

---

## 10F. 文档审批 `/api/v1`

### 10F.1 提交审批

```
POST /api/v1/documents/{id}/approval/submit
```

请求体：

```json
{ "comment": "请审批" }
```

提交人必须能查看文档。v1 审批人选择文件夹 owner；审批提交会产生通知和审计。

### 10F.2 我的审批任务

```
GET /api/v1/approvals/tasks?status=PENDING
```

### 10F.3 审批通过 / 驳回

```
POST /api/v1/approvals/{id}/approve
POST /api/v1/approvals/{id}/reject
```

请求体：

```json
{ "comment": "同意" }
```

### 10F.4 文档审批历史

```
GET /api/v1/documents/{id}/approval/history
```

---

## 11. 文档接口 `/api/v1`

### 11.1 上传新文档

```
POST /api/v1/folders/{folderId}/documents
```

在指定文件夹下上传新文档（首版本）。需要对目标文件夹有 `FOLDER_CREATE` 权限。

**请求类型**: `multipart/form-data`

**表单字段**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| file | File | 是 | 文件对象；后端限制 0 < size <= 50MB |
| name | string | 否 | 文档名称，为空则使用原始文件名；不能为空，最长 255 字符，不能包含 `/`、`\`、空字符 |
| remark | string | 否 | 备注 |
| changeLog | string | 否 | 版本变更说明 |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "documentId": "2058217319302139906",
    "versionNo": 1,
    "name": "投标文件.pdf",
    "size": 1048576,
    "mimeType": "application/pdf"
  },
  "timestamp": 1779552230324
}
```

**典型错误**：4032001（无权限） / 4042001（文件夹不存在）

---

### 11.2 获取文档详情

```
GET /api/v1/documents/{id}
```

获取文档详情，包含当前版本信息。需要对文档所在文件夹有 `FOLDER_VIEW` 权限。

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "2058217319302139906",
    "folderId": "2058217227685957633",
    "name": "投标文件.pdf",
    "currentVersionNo": 3,
    "currentVersion": {
      "versionNo": 3,
      "size": 1048576,
      "mimeType": "application/pdf",
      "originalFilename": "投标文件_v3.pdf",
      "uploadedByUserId": "1",
      "uploadedAt": "2026-05-31T10:30:00Z",
      "changeLog": "修正了第三章节的错误"
    },
    "ownerUserId": "1",
    "ownerDeptId": "2058217226998091777",
    "remark": "重要投标文件",
    "createdAt": "2026-05-24T00:03:20Z",
    "updatedAt": "2026-05-31T10:30:00Z"
  },
  "timestamp": 1779552230324
}
```

---

### 11.3 获取文件夹下的文档列表

```
GET /api/v1/folders/{folderId}/documents
```

获取指定文件夹下的文档列表（分页）。需要对文件夹有 `FOLDER_VIEW` 权限。

**查询参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| page | int | 否 | 1 | 页码（从 1 开始） |
| size | int | 否 | 20 | 每页数量；后端允许范围 1-100，非法值回退为 20 |
| sort | string | 否 | createdAt | 排序字段：createdAt / name / size |
| order | string | 否 | desc | 排序方向：asc / desc |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": "2058217319302139906",
        "name": "投标文件.pdf",
        "currentVersionNo": 3,
        "latestSize": 1048576,
        "latestMime": "application/pdf",
        "ownerUserId": "1",
        "createdAt": "2026-05-24T00:03:20Z"
      }
    ],
    "page": 1,
    "size": 20,
    "total": 45,
    "totalPages": 3,
    "hasNext": true
  },
  "timestamp": 1779552230324
}
```

---

### 11.4 删除文档

```
DELETE /api/v1/documents/{id}
```

软删除文档及其所有版本。当前代码要求先通过文档所在文件夹的编辑权限校验，并且当前用户必须是文档所有者、该文件夹管理员或 `SUPER_ADMIN`。

**响应**：`code=0, data=null`

**典型错误**：4032001（无文件夹操作权限） / 4032101（非文档所有者、文件夹管理员或超级管理员） / 4042101（文档不存在）

---

### 11.5 上传新版本

```
POST /api/v1/documents/{id}/versions
```

为已有文档上传新版本。当前代码要求先通过文档所在文件夹的编辑权限校验，并且当前用户必须是文档所有者、该文件夹管理员或 `SUPER_ADMIN`。

**请求类型**: `multipart/form-data`

**表单字段**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| file | File | 是 | 文件对象；后端限制 0 < size <= 50MB |
| changeLog | string | 否 | 版本变更说明 |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "versionNo": 4,
    "size": 1048576,
    "mimeType": "application/pdf",
    "originalFilename": "投标文件_v4.pdf",
    "uploadedByUserId": "1",
    "uploadedAt": "2026-05-31T11:00:00Z",
    "changeLog": "更新了预算部分"
  },
  "timestamp": 1779552230324
}
```

---

### 11.6 获取版本列表

```
GET /api/v1/documents/{id}/versions
```

获取文档的所有版本列表（按版本号降序）。需要对文档所在文件夹有 `FOLDER_VIEW` 权限。

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "versionNo": 3,
      "size": 1048576,
      "mimeType": "application/pdf",
      "originalFilename": "投标文件_v3.pdf",
      "uploadedByUserId": "1",
      "uploadedAt": "2026-05-31T10:30:00Z",
      "changeLog": "修正了第三章节的错误"
    },
    {
      "versionNo": 2,
      "size": 1024000,
      "mimeType": "application/pdf",
      "originalFilename": "投标文件_v2.pdf",
      "uploadedByUserId": "1",
      "uploadedAt": "2026-05-30T15:20:00Z",
      "changeLog": "添加了附件"
    }
  ],
  "timestamp": 1779552230324
}
```

---

### 11.7 下载文档（当前版本）

```
GET /api/v1/documents/{id}/download
```

下载文档的当前版本。需要对文档所在文件夹有 `FOLDER_VIEW` 权限。

**响应**: 文件流（`application/octet-stream` 或实际 MIME 类型）

**响应头**:
- `Content-Type`: 文件的 MIME 类型
- `Content-Length`: 文件大小（字节）
- `Content-Disposition`: `attachment; filename*=UTF-8''文件名`
- `Cache-Control`: `no-store`

---

### 11.8 下载指定版本

```
GET /api/v1/documents/{id}/versions/{versionNo}/download
```

下载文档的指定版本。需要对文档所在文件夹有 `FOLDER_VIEW` 权限。

**路径参数**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | string | 文档 ID |
| versionNo | int | 版本号 |

**响应**: 文件流（同 §11.7）

---

### 11.8.1 预览文档（当前版本）

```
GET /api/v1/documents/{id}/preview
```

预览文档的当前版本。需要对文档所在文件夹有 `FOLDER_VIEW` 权限。

**支持类型**

- `application/pdf`
- `image/*`
- `text/*`

**响应**: 文件流。

**响应头**:
- `Content-Type`: 文件的 MIME 类型
- `Content-Length`: 文件大小（字节）
- `Content-Disposition`: `inline; filename*=UTF-8''文件名`
- `Cache-Control`: `no-store`

**典型错误**：`4002106`（当前文件类型不支持预览） / `4042101` / `4042102` / `4032001`

---

### 11.8.2 预览指定版本

```
GET /api/v1/documents/{id}/versions/{versionNo}/preview
```

预览文档的指定版本。需要对文档所在文件夹有 `FOLDER_VIEW` 权限。支持类型和响应头同 §11.8.1。

**路径参数**

| 参数 | 类型 | 说明 |
|---|---|---|
| id | string | 文档 ID |
| versionNo | int | 版本号 |

**典型错误**：`4002106`（当前文件类型不支持预览） / `4042101` / `4042102` / `4032001`

---

### 11.9 搜索文档

```
GET /api/v1/documents/search
```

按文档名称模糊搜索，支持文件夹范围过滤、权限过滤、分页。只返回当前用户有权限查看的文档。

**查询参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| keyword | string | 否 | - | 搜索关键词（模糊匹配文档名称） |
| folderId | string | 否 | - | 限定搜索范围的文件夹 ID |
| recursive | boolean | 否 | true | 是否递归搜索子文件夹 |
| mimeType | string | 否 | - | 按 MIME 类型过滤 |
| ownerUserId | string | 否 | - | 按文档所有者过滤 |
| createdFrom | datetime | 否 | - | 创建时间起点 |
| createdTo | datetime | 否 | - | 创建时间终点 |
| favoriteFolderOnly | boolean | 否 | false | 仅搜索当前用户收藏文件夹下的文档 |
| tagIds | array<string> | 否 | - | 按标签过滤；支持重复 query 参数 |
| sortBy | string | 否 | createdAt | 排序字段：createdAt / updatedAt / name / size |
| sortOrder | string | 否 | desc | 排序方向：asc / desc |
| page | int | 否 | 1 | 页码 |
| size | int | 否 | 20 | 每页数量；后端允许范围 1-100，非法值回退为 20 |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": "2058217319302139906",
        "name": "投标文件.pdf",
        "currentVersionNo": 3,
        "latestSize": 1048576,
        "latestMime": "application/pdf",
        "ownerUserId": "1",
        "createdAt": "2026-05-24T00:03:20Z"
      }
    ],
    "page": 1,
    "size": 20,
    "total": 8,
    "totalPages": 1,
    "hasNext": false
  },
  "timestamp": 1779552230324
}
```

---

### 11.10 获取搜索历史

```
GET /api/v1/documents/search/history
```

获取当前用户的搜索历史记录。

**查询参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| limit | int | 否 | 10 | 返回记录数量；后端允许范围 1-50，非法值回退为 10 |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": "2058217319302139906",
      "keyword": "投标",
      "folderId": "2058217319365054465",
      "resultCount": 8,
      "searchTime": "2026-05-31T10:30:00Z"
    }
  ],
  "timestamp": 1779552230324
}
```

---

### 11.11 清除搜索历史

```
DELETE /api/v1/documents/search/history
```

清除当前用户的所有搜索历史记录。

**响应**：`code=0, data=null`

---

### 11.12 获取热门搜索

```
GET /api/v1/documents/search/hot
```

获取热门搜索关键词排行（全局统计）。

**查询参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| limit | int | 否 | 10 | 返回记录数量；后端允许范围 1-50，非法值回退为 10 |
| days | int | 否 | 7 | 统计最近 N 天；后端允许范围 1-30，非法值回退为 7 |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "keyword": "投标",
      "searchCount": 128
    },
    {
      "keyword": "合同",
      "searchCount": 95
    }
  ],
  "timestamp": 1779552230324
}
```

---

### 11.13 获取存储空间统计

```
GET /api/v1/documents/stats/storage
```

获取文档总数、存储空间等统计信息（当前用户可见范围）。

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "totalDocuments": 1250,
    "totalVersions": 3200,
    "totalSize": 5368709120,
    "totalSizeReadable": "5.00 GB",
    "myDocuments": 12,
    "mySize": 104857600,
    "mySizeReadable": "100.00 MB"
  },
  "timestamp": 1779552230324
}
```

---

### 11.14 获取文档类型分布

```
GET /api/v1/documents/stats/types
```

按 MIME 类型统计文档数量和大小（当前用户可见范围）。

**查询参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| limit | int | 否 | 10 | 返回记录数量；后端允许范围 1-50，非法值回退为 10 |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "mimeType": "application/pdf",
      "count": 450,
      "totalSize": 2147483648,
      "totalSizeReadable": "2.00 GB"
    },
    {
      "mimeType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      "count": 320,
      "totalSize": 1073741824,
      "totalSizeReadable": "1.00 GB"
    }
  ],
  "timestamp": 1779552230324
}
```

---

### 11.15 获取热门文档排行

```
GET /api/v1/documents/stats/popular
```

按下载次数统计热门文档（当前用户可见范围）。

**查询参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| limit | int | 否 | 10 | 返回记录数量；后端允许范围 1-50，非法值回退为 10 |
| days | int | 否 | 30 | 统计最近 N 天；后端允许范围 1-90，非法值回退为 30 |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": "2058217319302139906",
      "name": "投标文件.pdf",
      "currentVersionNo": 3,
      "size": "1048576",
      "mimeType": "application/pdf",
      "ownerUserId": "1",
      "downloadCount": 85,
      "createdAt": "2026-05-24T00:03:20Z"
    }
  ],
  "timestamp": 1779552230324
}
```

---

## 12. 前端联调建议

### 12.1 ID 处理

- 后端把 Long 序列化为 String。前端定义类型时建议 `string` 形式存储；如需做 `===` 比较请保持字符串。
- 提交请求时 `string` 或 `number` 都可，但**强烈推荐 `string`** 以避免 JS 大整数精度问题。

### 12.2 鉴权请求头封装

```ts
// axios 示例
axios.interceptors.request.use(cfg => {
  const token = localStorage.getItem('token');
  if (token) cfg.headers.Authorization = token; // 当前后端 token-prefix 为空，直接使用登录响应里的 data.token
  return cfg;
});
```

注意：当前登录响应里的 `data.token` 不带 `Bearer ` 前缀；如果后续重新启用 `sa-token.token-prefix=Bearer`，这里需要同步调整。

### 12.3 错误处理

```ts
axios.interceptors.response.use(
  resp => {
    if (resp.data.code !== 0) {
      // 全局错误提示
      message.error(resp.data.message);
      return Promise.reject(resp.data);
    }
    return resp.data.data;  // 拆包后只返回 data
  },
  error => {
    // 处理网络错误或 HTTP 状态码错误
    if (error.response?.status === 401) {
      // token 失效，跳转登录
      localStorage.removeItem('token');
      router.push('/login');
    }
    return Promise.reject(error);
  }
);
```

### 12.4 树形数据组装

`/folders/tree/root` 与 `/folders/children` 都是**懒加载**单层返回，前端按需在用户展开节点时再请求 `children`。`hasChildren` 字段已是权限过滤后的真实值。

### 12.5 文件夹移动 / 复制的 UI 提示

- **移动**：禁止把节点拖到自身或其后代下；前端可基于 `ancestorIds` 字段预判（含自身 id 即非法），减少 404/4002008 报错。
- **复制**：在目标父下重名时，可弹框让用户输入新名（即 `targetName` 字段），无需重新拉源节点。

### 12.6 批量删除的提示

- batchDelete 是**事务原子**：任一 id 失败整体回滚。前端不会拿到"部分成功"语义。
- 建议在前端按用户选择构造 `folderIds`，然后由后端做父子去重，前端无需重复实现。

### 12.7 文件上传处理

**使用 FormData 上传文件**：

```ts
// 上传新文档
const uploadDocument = async (folderId: string, file: File, options?: {
  name?: string;
  remark?: string;
  changeLog?: string;
}) => {
  const formData = new FormData();
  formData.append('file', file);
  if (options?.name) formData.append('name', options.name);
  if (options?.remark) formData.append('remark', options.remark);
  if (options?.changeLog) formData.append('changeLog', options.changeLog);

  return axios.post(`/api/v1/folders/${folderId}/documents`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
};

// 上传新版本
const uploadVersion = async (documentId: string, file: File, changeLog?: string) => {
  const formData = new FormData();
  formData.append('file', file);
  if (changeLog) formData.append('changeLog', changeLog);

  return axios.post(`/api/v1/documents/${documentId}/versions`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
};
```

**上传进度监控**：

```ts
axios.post(url, formData, {
  onUploadProgress: (progressEvent) => {
    const percentCompleted = Math.round(
      (progressEvent.loaded * 100) / progressEvent.total
    );
    console.log(`上传进度: ${percentCompleted}%`);
  }
});
```

### 12.8 文件下载处理

**下载文件并保存**：

```ts
const downloadDocument = async (documentId: string, filename?: string) => {
  const response = await axios.get(`/api/v1/documents/${documentId}/download`, {
    responseType: 'blob' // 重要：指定响应类型为 blob
  });

  // 从响应头获取文件名（如果后端提供）
  const contentDisposition = response.headers['content-disposition'];
  const filenameMatch = contentDisposition?.match(/filename\*=UTF-8''(.+)/);
  const downloadFilename = filenameMatch
    ? decodeURIComponent(filenameMatch[1])
    : filename || 'download';

  // 创建下载链接
  const url = window.URL.createObjectURL(new Blob([response.data]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', downloadFilename);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
};

// 下载指定版本
const downloadVersion = async (documentId: string, versionNo: number) => {
  const response = await axios.get(
    `/api/v1/documents/${documentId}/versions/${versionNo}/download`,
    { responseType: 'blob' }
  );
  // 处理同上
};
```

### 12.9 分页数据处理

**统一的分页响应结构**：

```ts
interface PageResponse<T> {
  list: T[];
  page: number;      // 当前页码（从 1 开始）
  size: number;      // 每页数量
  total: number;     // 总记录数
  totalPages: number; // 总页数
  hasNext: boolean;  // 是否有下一页
}

// 使用示例
const fetchDocuments = async (folderId: string, page: number = 1, size: number = 20) => {
  const response = await axios.get<PageResponse<DocumentListItem>>(
    `/api/v1/folders/${folderId}/documents`,
    { params: { page, size, sort: 'createdAt', order: 'desc' } }
  );
  return response.data;
};
```

### 12.10 文件大小格式化

```ts
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
};

// 使用示例
formatFileSize(1048576); // "1 MB"
```

### 12.11 MIME 类型图标映射

```ts
const getMimeTypeIcon = (mimeType: string): string => {
  const iconMap: Record<string, string> = {
    'application/pdf': 'file-pdf',
    'application/msword': 'file-word',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document': 'file-word',
    'application/vnd.ms-excel': 'file-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': 'file-excel',
    'application/vnd.ms-powerpoint': 'file-ppt',
    'application/vnd.openxmlformats-officedocument.presentationml.presentation': 'file-ppt',
    'image/jpeg': 'file-image',
    'image/png': 'file-image',
    'image/gif': 'file-image',
    'text/plain': 'file-text',
    'application/zip': 'file-zip',
    'application/x-rar-compressed': 'file-zip',
  };
  return iconMap[mimeType] || 'file';
};
```

### 12.12 搜索防抖

```ts
import { debounce } from 'lodash-es';

// 搜索防抖（避免频繁请求）
const debouncedSearch = debounce(async (keyword: string) => {
  if (!keyword.trim()) return;

  const result = await axios.get('/api/v1/documents/search', {
    params: { keyword, page: 1, size: 20 }
  });
  // 处理搜索结果
}, 300); // 300ms 防抖

// 使用
<input onChange={(e) => debouncedSearch(e.target.value)} />
```

### 12.13 权限控制

**基于角色的 UI 控制**：

```ts
// 从登录响应或 /auth/me 获取用户角色
const userRoles = ['EMPLOYEE', 'FOLDER_ADMIN'];

// 权限判断函数
const hasRole = (role: string) => userRoles.includes(role);
const isSuperAdmin = () => hasRole('SUPER_ADMIN');
const isFolderAdmin = () => hasRole('FOLDER_ADMIN');

// UI 条件渲染
{isSuperAdmin() && <Button>创建根级文件夹</Button>}
{isFolderAdmin() && <Button>设置管理员</Button>}
```

**基于文件夹权限的 UI 控制**：

```ts
// 根据文件夹详情判断当前用户权限
// 方法1: 通过 owner 判断
const isOwner = folder.ownerUserId === currentUserId;

// 方法2: 尝试操作，后端会返回权限错误
// 前端可以先乐观展示按钮，操作失败时提示用户

// 方法3: 如果后端提供权限列表（未来扩展）
const hasPermission = (permission: string) => {
  return folder.permissions?.includes(permission);
};
```

---

## 13. 前端开发注意事项

### 13.1 安全注意事项

1. **永远不要在前端存储敏感信息**
   - 不要在 localStorage/sessionStorage 中存储密码
   - Token 存储在 localStorage 时注意 XSS 风险
   - 考虑使用 httpOnly cookie（需后端配合）

2. **文件上传安全**
   - 前端做文件类型校验（白名单）
   - 限制文件大小（建议 ≤ 100MB）
   - 显示上传进度，允许取消上传

3. **XSS 防护**
   - 使用框架的自动转义（React/Vue 默认转义）
   - 不要使用 `dangerouslySetInnerHTML` / `v-html` 渲染用户输入
   - 文件名、备注等用户输入内容需转义显示

### 13.2 性能优化

1. **列表虚拟滚动**
   - 文档列表、文件夹树使用虚拟滚动（如 react-window / vue-virtual-scroller）
   - 大量数据时避免一次性渲染

2. **图片/文件预览优化**
   - 使用缩略图而非原图
   - 懒加载图片（Intersection Observer）
   - 考虑使用 CDN 加速

3. **搜索优化**
   - 使用防抖（300ms）减少请求
   - 缓存搜索结果
   - 显示搜索历史和热门搜索

4. **分页加载**
   - 使用分页而非无限滚动（大数据集）
   - 记住用户的分页位置

### 13.3 用户体验

1. **加载状态**
   - 所有异步操作显示 loading 状态
   - 文件上传显示进度条
   - 大文件下载显示进度

2. **错误提示**
   - 友好的错误提示（不要直接显示错误码）
   - 网络错误时提供重试按钮
   - 表单校验错误高亮显示

3. **操作确认**
   - 删除操作需要二次确认
   - 批量删除显示影响范围
   - 移动/复制操作显示目标位置

4. **空状态**
   - 空文件夹显示引导创建
   - 搜索无结果显示建议
   - 网络错误显示重试按钮

### 13.4 兼容性

1. **浏览器兼容**
   - 支持 Chrome/Edge/Firefox/Safari 最新两个版本
   - 文件下载在不同浏览器测试
   - FormData 上传在移动端测试

2. **文件名处理**
   - 支持中文文件名
   - 处理特殊字符（`/`, `\`, `:`, `*`, `?`, `"`, `<`, `>`, `|`）
   - 文件名长度限制（建议 ≤ 255 字符）

### 13.5 测试建议

1. **功能测试**
   - 测试所有 CRUD 操作
   - 测试权限边界（无权限时的表现）
   - 测试文件上传/下载（各种文件类型和大小）

2. **边界测试**
   - 空文件夹
   - 单个文件
   - 大量文件（1000+）
   - 大文件（100MB+）
   - 特殊文件名

3. **错误场景**
   - 网络断开
   - Token 过期
   - 并发操作冲突
   - 文件上传失败

### 13.6 开发工具推荐

1. **API 调试**
   - 使用 Knife4j 在线文档：`http://localhost:8080/doc.html`
   - 使用 Postman/Apifox 测试接口
   - 浏览器 DevTools Network 面板

2. **类型定义**
   - 使用 TypeScript 定义接口类型
   - 可以从 Swagger JSON 自动生成类型（swagger-typescript-api）

3. **Mock 数据**
   - 开发初期可以使用 Mock.js 或 MSW
   - 后期直接对接真实后端

---

## 14. 常见问题 FAQ

### Q1: Token 过期后如何处理？

**A**: 后端返回 `code=4011001` 或 HTTP 401 时，清除本地 token 并跳转到登录页。可以在 axios 拦截器中统一处理。

### Q2: 如何判断用户是否有某个文件夹的权限？

**A**: 前端无法提前知道权限，建议：
- 乐观展示操作按钮
- 操作失败时根据错误码提示用户
- 或者根据 owner/同部门/角色做简单判断

### Q3: 文件上传失败如何处理？

**A**:
- 检查文件大小是否超限
- 检查网络连接
- 提供重试按钮
- 显示具体错误信息

### Q4: 如何实现文件夹树的懒加载？

**A**:
```ts
// 1. 初始加载根节点
const rootFolders = await axios.get('/api/v1/folders/tree/root');

// 2. 用户点击展开时加载子节点
const loadChildren = async (parentId: string) => {
  const children = await axios.get('/api/v1/folders/children', {
    params: { parentId }
  });
  return children;
};

// 3. 根据 hasChildren 判断是否显示展开图标
```

### Q5: 搜索结果如何高亮关键词？

**A**:
```ts
const highlightKeyword = (text: string, keyword: string) => {
  if (!keyword) return text;
  const regex = new RegExp(`(${keyword})`, 'gi');
  return text.replace(regex, '<mark>$1</mark>');
};

// 使用时注意 XSS 防护，建议用组件库的高亮功能
```

### Q6: 如何处理大文件上传？

**A**:
- 前端限制文件大小（如 100MB）
- 显示上传进度
- 支持取消上传（axios CancelToken）
- 考虑分片上传（需后端支持）

### Q7: 分页组件如何与接口对接？

**A**:
```ts
const [pagination, setPagination] = useState({
  current: 1,
  pageSize: 20,
  total: 0
});

const fetchData = async (page: number, size: number) => {
  const response = await axios.get('/api/v1/folders/123/documents', {
    params: { page, size }
  });
  setPagination({
    current: response.page,
    pageSize: response.size,
    total: response.total
  });
  return response.list;
};
```

---

## 15. 变更日志

| 日期 | 版本 | 范围 | 备注 |
|---|---|---|---|
| 2026-06-10 | v2.1 | 文档校准 | 按当前 Controller/DTO/ErrorCode 修正 token 携带方式、Document 错误码、搜索历史与统计响应字段、文档删除/版本权限说明 |
| 2026-05-31 | v2.0 | 全量更新 | 补充 Document 模块完整接口（上传/下载/版本管理/搜索/统计）；新增前端开发注意事项、常见问题 FAQ；优化代码示例和最佳实践 |
| 2026-05-24 | v1.0 | 首版 | 覆盖 auth/user/role/dept/folder 全量接口 |

---

## 附录：完整接口清单

### 认证模块 (4)
- `POST /api/v1/auth/register` - 用户注册
- `POST /api/v1/auth/login` - 用户登录
- `GET /api/v1/auth/me` - 获取当前用户
- `POST /api/v1/auth/logout` - 退出登录

### 用户模块 (2)
- `GET /api/v1/users/{id}` - 查询用户详情
- `PATCH /api/v1/users/{id}/status` - 启用/禁用用户

### 角色模块 (4)
- `POST /api/v1/roles/assign` - 分配角色
- `DELETE /api/v1/roles/revoke` - 撤销角色
- `GET /api/v1/roles/user/{userId}` - 查询用户角色列表
- `GET /api/v1/roles/code/{roleCode}/users` - 查询角色下的用户

### 部门模块 (2)
- `POST /api/v1/departments` - 创建部门
- `GET /api/v1/departments` - 查询部门列表

### 文件夹模块 (10)
- `POST /api/v1/folders` - 创建文件夹
- `GET /api/v1/folders/{id}` - 查询详情
- `PATCH /api/v1/folders/{id}/name` - 重命名
- `PUT /api/v1/folders/{id}` - 编辑
- `GET /api/v1/folders/children` - 子节点列表
- `GET /api/v1/folders/tree/root` - 根树
- `DELETE /api/v1/folders/{id}` - 删除
- `DELETE /api/v1/folders/batch` - 批量删除
- `PATCH /api/v1/folders/{id}/move` - 移动
- `POST /api/v1/folders/{id}/copy` - 复制

### 文件夹授权模块 (3)
- `GET /api/v1/folders/{folderId}/grants` - 查询授权列表
- `POST /api/v1/folders/{folderId}/grants` - 新增授权
- `DELETE /api/v1/folders/{folderId}/grants/{grantId}` - 删除授权

### 文件夹管理员模块 (3)
- `GET /api/v1/folders/{folderId}/managers` - 查询管理员列表
- `POST /api/v1/folders/{folderId}/managers` - 新增管理员
- `DELETE /api/v1/folders/{folderId}/managers/{managerId}` - 删除管理员

### 文件夹收藏模块 (3)
- `GET /api/v1/folders/favorites` - 我的收藏
- `POST /api/v1/folders/{folderId}/favorite` - 收藏文件夹
- `DELETE /api/v1/folders/{folderId}/favorite` - 取消收藏

### 文件夹前端支撑模块 (2)
- `GET /api/v1/folders/{id}/permissions/me` - 查询当前用户文件夹权限
- `GET /api/v1/folders/search` - 目录检索

### 审计模块 (1)
- `GET /api/v1/audit/logs` - 查询审计日志

### 标签模块 (5)
- `GET /api/v1/tags` - 查询标签列表
- `POST /api/v1/tags` - 创建标签
- `DELETE /api/v1/tags/{id}` - 删除标签
- `PUT /api/v1/documents/{id}/tags` - 绑定文档标签
- `GET /api/v1/documents/{id}/tags` - 查询文档标签

### 通知模块 (4)
- `GET /api/v1/notifications` - 查询我的通知
- `GET /api/v1/notifications/unread-count` - 查询未读数
- `PATCH /api/v1/notifications/{id}/read` - 标记单条已读
- `PATCH /api/v1/notifications/read-all` - 全部标记已读

### 审批模块 (5)
- `POST /api/v1/documents/{id}/approval/submit` - 提交文档审批
- `GET /api/v1/approvals/tasks` - 查询我的审批任务
- `POST /api/v1/approvals/{id}/approve` - 审批通过
- `POST /api/v1/approvals/{id}/reject` - 审批驳回
- `GET /api/v1/documents/{id}/approval/history` - 查询文档审批历史

### 文档模块 (17)
- `POST /api/v1/folders/{folderId}/documents` - 上传新文档
- `GET /api/v1/documents/{id}` - 获取文档详情
- `GET /api/v1/folders/{folderId}/documents` - 获取文档列表
- `DELETE /api/v1/documents/{id}` - 删除文档
- `POST /api/v1/documents/{id}/versions` - 上传新版本
- `GET /api/v1/documents/{id}/versions` - 获取版本列表
- `GET /api/v1/documents/{id}/download` - 下载文档
- `GET /api/v1/documents/{id}/versions/{versionNo}/download` - 下载指定版本
- `GET /api/v1/documents/{id}/preview` - 预览文档
- `GET /api/v1/documents/{id}/versions/{versionNo}/preview` - 预览指定版本
- `GET /api/v1/documents/search` - 搜索文档
- `GET /api/v1/documents/search/history` - 获取搜索历史
- `DELETE /api/v1/documents/search/history` - 清除搜索历史
- `GET /api/v1/documents/search/hot` - 获取热门搜索
- `GET /api/v1/documents/stats/storage` - 获取存储统计
- `GET /api/v1/documents/stats/types` - 获取文档类型分布
- `GET /api/v1/documents/stats/popular` - 获取热门文档排行

**总计**: 65 个接口

---

**文档维护**: 本文档由后端团队维护，如有疑问请联系后端开发人员。
**在线文档**: 启动项目后访问 `http://localhost:8080/doc.html` 查看 Knife4j 在线文档。
