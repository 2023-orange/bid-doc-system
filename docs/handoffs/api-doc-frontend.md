# bid-doc-system 后端接口文档（前端联调用）

> **文档版本**: v2.6  
> **最后更新**: 2026-06-23  
> **适用范围**: auth + folder + document + project + workflow + audit + notify + common 模块接口  
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

- 登录后服务端通过 Sa-Token 颁发 token。当前后端返回 Sa-Token 原值，前端在后续请求中通过请求头 `Authorization: <token>` 携带。
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
- **用户模块**：详情 / 用户选择器 / 启用禁用
- **角色模块**：分配 / 撤销 / 查询
- **部门模块**：创建 / 管理树 / 编辑 / 启用禁用 / 删除空部门
- **文件夹模块**：CRUD / 删除 / 批量删除 / 移动 / 复制 / 授权管理 / 管理员管理 / 收藏 / 统计洞察 / 最近访问 / 文件夹标签
- **文档模块**：上传 / 下载 / 预览 / 版本管理 / 元数据 / 标签 / 搜索 / 统计
- **字典模块**：项目、资料、审批相关枚举字典
- **审计模块**：审计日志查询 / 详情 / 对象时间线
- **通知模块**：站内通知列表 / 未读数 / 已读处理
- **项目模块**：项目 CRUD / 工作台详情 / 成员 / 阶段状态 / 资料清单 / 清单模板 / 项目归档详情
- **工作流模块**：资料、版本、清单项审批提交 / 待办 / 审批处理 / 撤回 / 转交 / 加签 / 终止 / 流程定义维护 / 历史

🚧 **后续阶段**（暂不可用）：
- 更完整的外部分享、全文索引、预览转换等能力需以后端实际代码为准

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
| `FOLDER_FAVORITE` | 收藏 |
| `FOLDER_AUDIT_VIEW` | 查看审计日志（接口未实现） |

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
| `4222001` | 用户未具备 FOLDER_ADMIN 角色 | manager add 时被设置人无 FOLDER_ADMIN |
| `5001001` | 系统异常 | 未捕获的运行时异常 |
| `5003001` | 审计记录失败 | audit 写入失败 |
| `5003002` | 审计查询失败 | — |
| `4004001` | 审批任务不是待处理状态 | approve / reject / transfer / add-sign 目标任务非 PENDING |
| `4004002` | 审批流程配置不完整 | 提交审批时缺少可用定义或节点配置异常 |
| `4034001` | 无权处理该审批任务 | 当前用户不是任务处理人且不是超级管理员 |
| `4044001` | 审批任务不存在 | taskId 无效或已删除 |
| `4044002` | 审批实例不存在 | approval instance id 无效或已删除 |
| `4044003` | 审批流程定义不存在 | workflow definition id 无效或已删除 |
| `4044004` | 审批节点不存在 | workflow node id 无效或已删除 |
| `4005001` | 项目至少需要一个负责人 | 创建项目未传 ownerUserIds |
| `4005002` | 项目所属部门未配置事业部缩写 | 创建项目编号时部门 extensionData 缺少 abbr |
| `4005003` | 已归档项目不允许修改 | 修改已归档项目、成员、阶段、状态或清单 |
| `4005004` | 必需清单项未完成，不能归档 | PATCH 项目状态为 ARCHIVED 时存在未完成必需清单项 |
| `4035001` | 无项目操作权限 | 非项目负责人/成员且非超级管理员访问项目 |
| `4045001` | 项目不存在 | projectId 无效或已删除 |

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

- `token` 为 Sa-Token 原始 token 值；当前后端配置无 `Bearer` 前缀，前端直接放入 `Authorization` 请求头使用即可。
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

### 5.1 查询用户选择器选项

```
GET /api/v1/users/options?keyword=&deptId=&status=&size=
```

用于负责人、成员等人员选择器。返回轻量字段，前端不需要让用户输入 id。

**查询参数**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| keyword | string | 否 | 按姓名或用户名模糊搜索 |
| deptId | string\|number | 否 | 限定部门 |
| status | int | 否 | `1` 启用 / `0` 禁用；负责人选择建议传 `1` |
| size | int | 否 | 返回数量，默认 `20`，最大 `50` |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": "2058217227220389889",
      "realName": "张三",
      "username": "alice",
      "deptId": "2058217226998091777",
      "deptName": "研发部",
      "status": 1
    }
  ],
  "timestamp": 1779552230324
}
```

---

### 5.2 查询用户详情

```
GET /api/v1/users/{id}
```

**响应**：与 §4.3 的 `data` 同结构（含 `roleCodes`）。

---

### 5.3 启用 / 禁用用户

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
  "parentId": null,
  "sortOrder": 10,
  "managerUserId": null,
  "status": 1,
  "remark": "技术中心下属研发部门"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | string | 是 | 非空 |
| parentId | string\|number\|null | 否 | 一级部门传 `null`；子部门传父级部门 id |
| sortOrder | int | 否 | 同级排序，默认 `0` |
| managerUserId | string\|number | 否 | 部门负责人用户 id |
| status | int | 否 | `1` 启用 / `0` 禁用，默认 `1` |
| remark | string | 否 | — |

后端根据 `parentId` 自动计算 `level`，最多支持 5 级部门树。

**响应**：`code=0, data=null`。

---

### 7.2 查询部门管理树

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
      "parentId": null,
      "parentName": null,
      "level": 1,
      "sortOrder": 10,
      "managerUserId": null,
      "status": 1,
      "remark": "...",
      "createdAt": "2026-05-24T00:03:20.123456Z",
      "updatedAt": "2026-05-24T00:03:20.123456Z",
      "hasChildren": true,
      "memberCount": 15,
      "projectCount": 3,
      "children": [
        {
          "id": "2058217226998091778",
          "name": "技术一组",
          "parentId": "2058217226998091777",
          "parentName": "研发部",
          "level": 2,
          "sortOrder": 20,
          "managerUserId": null,
          "status": 0,
          "remark": null,
          "createdAt": "2026-05-24T00:03:20.123456Z",
          "updatedAt": "2026-05-24T00:03:20.123456Z",
          "hasChildren": false,
          "memberCount": 5,
          "projectCount": 1,
          "children": []
        }
      ]
    }
  ],
  "timestamp": 1779552230324
}
```

前端应直接使用后端返回的树结构和 `status` 字段，不要自行从缺失字段推断启用状态。

新增统计字段：

| 字段 | 类型 | 说明 |
|---|---|---|
| memberCount | int | 部门直属启用用户数；只统计 `sys_user.dept_id = department.id`、`status = 1`、`deleted = false` 的用户，不包含子部门用户 |
| projectCount | int | 部门进行中项目数；只统计 `bid_project.owner_dept_id = department.id`、`project_status = NORMAL`、`deleted = false` 的项目 |

前端提示建议：当前项目状态共有 `NORMAL`（正常）、`PAUSED`（暂停）、`ARCHIVED`（归档）、`CANCELLED`（取消）、`DELETED`（删除）五种；部门树里的 `projectCount` 只会把状态为 `NORMAL` 且未删除的项目计入“进行中项目数”，其它状态不计入。

---

### 7.3 编辑部门

```
PUT /api/v1/departments/{id}
```

**请求体**

```json
{
  "name": "研发中心",
  "parentId": null,
  "sortOrder": 10,
  "managerUserId": null,
  "status": 1,
  "remark": "更新后的备注"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | string | 是 | 非空 |
| parentId | string\|number\|null | 否 | 传 `null` 表示移动为一级部门 |
| sortOrder | int | 否 | 同级排序，默认 `0` |
| managerUserId | string\|number | 否 | 部门负责人用户 id |
| status | int | 否 | `1` 启用 / `0` 禁用；省略则保持不变 |
| remark | string | 否 | — |

后端会阻止移动到自身或下级部门，并校验移动后整棵子树不超过 5 级。

**响应**：`code=0, data=null`。

---

### 7.4 启用 / 禁用部门

```
PATCH /api/v1/departments/{id}/status
```

**请求体**

```json
{
  "status": 0,
  "cascadeDeptIds": ["2058217226998091778"]
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| status | int | 是 | `1` 启用 / `0` 禁用 |
| cascadeDeptIds | array<string\|number> | 否 | 需要连带启用/禁用的下级部门 id；空数组表示只处理当前部门 |

禁用非叶子部门时，前端建议弹窗展示下级部门供管理员勾选；后端会校验 `cascadeDeptIds` 必须属于当前部门的下级部门。

**响应**：`code=0, data=null`。

---

### 7.5 删除空部门

```
DELETE /api/v1/departments/{id}
```

仅超级管理员可删除。删除和禁用语义不同：删除只允许无子部门、无用户、无项目、无资料、无文件夹、无审批流程引用的空部门；否则后端返回业务错误，前端应提示先调整引用或改用禁用。

**响应**：`code=0, data=null`。

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

### 8.0.1 前端对接推荐

文件夹管理页建议按“首屏聚合 + 树懒加载 + 详情按需加载”的方式接入：

1. **首屏并行请求**：`GET /folders/stats`、`GET /folders/tree/root`、`GET /folders/favorites?limit=10`、`GET /folders/recent?limit=10`。如果页面有“我管理的 / 我创建的”快捷区，再并行请求 `/managed`、`/owned`。
2. **树展开**：点击展开节点时调用 `GET /folders/children?parentId={id}`；使用返回的 `hasChildren` 控制展开图标，使用 `documentCount/totalSize/lastAccessTime` 做节点辅助信息。
3. **详情面板**：选中文件夹后调用 `GET /folders/{id}/detail`，同时请求 `GET /folders/{id}/documents` 加载右侧文档列表。`GET /folders/{id}` 已返回同一增强 DTO，老调用可继续使用。
4. **访问记录**：详情页真正打开后再调用 `POST /folders/{id}/access` 记录 `VIEW`，不要在 hover、树预加载、批量渲染时写访问日志；同一页面会话建议对同一 folderId 去重或节流。
5. **按钮权限**：优先使用详情 DTO 的 `permissions` 或 `GET /folders/{id}/permissions/me` 控制按钮。隐藏按钮只是体验优化，后端仍会做最终权限校验。
6. **移动 / 复制成功后局部更新**：`move` / `copy` 返回 `id/name/parentId/fullPath`，前端可用该结果局部刷新树节点和面包屑，不必强制刷新整棵树。
7. **标签**：标签词表继续使用 `/api/v1/tags`；文件夹标签用 `GET/PUT /folders/{id}/tags`。`PUT` 是全量替换，提交前请合并当前用户最终选择的完整 tagId 列表。
8. **统计口径**：`stats/detail/tree` 均已做权限过滤。超级管理员的总文件夹、总文档、总容量、启用文件夹是全局口径，收藏/管理/创建/最近访问仍是当前用户口径。

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

等价增强详情接口：

```
GET /api/v1/folders/{id}/detail
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
    "fullPath": "/投标资料",
    "parentName": null,
    "ancestorIds": "2058217227685957633",
    "level": 0,
    "sortNo": 1,
    "ownerDeptId": "1",
    "ownerDeptName": "经营部",
    "ownerUserId": "1",
    "ownerUserName": "管理员",
    "inheritPermission": true,
    "status": 1,
    "remark": "总根",
    "createdAt": "2026-05-24T00:03:20.123456Z",
    "createdBy": "1",
    "createdByName": "管理员",
    "updatedAt": "2026-05-24T00:03:20.123456Z",
    "updatedBy": "1",
    "updatedByName": "管理员",
    "documentCount": 3,
    "totalDocumentCount": 12,
    "folderSize": 5242880,
    "totalSize": 20971520,
    "childFolderCount": 2,
    "totalChildFolderCount": 5,
    "permissions": {
      "canView": true,
      "canCreateChild": true,
      "canRename": true,
      "canEdit": true,
      "canDelete": false,
      "canMove": true,
      "canCopy": true,
      "canGrant": true,
      "canManage": true,
      "canFavorite": true,
      "isOwner": true,
      "isManager": false,
      "isSuperAdmin": false
    },
    "viewCount": 26,
    "lastAccessTime": "2026-06-23T10:00:00Z",
    "lastAccessUserName": "张三",
    "downloadCount": 8,
    "isFavorite": true,
    "relatedProjects": [
      { "projectId": "2058217000000000001", "projectName": "机场改扩建投标", "projectNo": "XM-2026-001" }
    ],
    "tags": [
      { "id": "1", "name": "重要", "createdAt": "2026-06-01T00:00:00Z", "createdBy": "1" }
    ]
  },
  "timestamp": 1779552230324
}
```

字段口径：

| 字段 | 说明 |
|---|---|
| fullPath | 按当前用户可见祖先拼接；如果存在不可见祖先，返回形如 `/.../当前可见路径`，不会泄露无权目录名称 |
| documentCount / folderSize | 当前文件夹直属文档数量 / 当前版本容量，容量取 `doc_document.latest_size` |
| totalDocumentCount / totalSize | 当前文件夹及当前用户可见后代目录的文档数量 / 当前版本容量 |
| childFolderCount / totalChildFolderCount | 当前用户可见的直属子文件夹数 / 可见后代文件夹总数 |
| permissions.canGrant | 根级目录恒为 `false`；非根按 `SUPER_ADMIN > owner > effective manager` 判断 |
| permissions.canManage | `SUPER_ADMIN`、owner、effective manager 任一命中即为 `true` |

> `ancestorIds` 是逗号分隔的祖先链（**包含自身**），如 root="100"、子="100,200"、孙="100,200,300"。前端展示路径请优先使用 `fullPath`。

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
      "documentCount": 5,
      "totalSize": 7340032,
      "lastAccessTime": "2026-06-23T10:00:00Z",
      "createdAt": "2026-05-24T00:03:20.123456Z"
    }
  ],
  "timestamp": 1779552230324
}
```

> `hasChildren` 已基于当前用户权限计算（只算可见子节点）；不会泄露不可见的孙节点信息。`documentCount` 为当前节点直属文档数，`totalSize` 为当前节点直属文档当前版本容量。

---

### 8.6 根树

```
GET /api/v1/folders/tree/root
```

等同于 `GET /folders/children?parentId=0`。返回当前用户可见的根级目录列表。

---

### 8.6.1 查询当前用户文件夹权限【新增】

```
GET /api/v1/folders/{id}/permissions/me
```

查询当前登录用户对指定文件夹的可操作权限，适合前端控制按钮展示；后端权限仍是最终安全边界。

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "canView": true,
    "canCreateChild": true,
    "canRename": false,
    "canEdit": true,
    "canDelete": false,
    "canMove": false,
    "canCopy": true,
    "canGrant": true,
    "canManage": true,
    "canFavorite": true,
    "isOwner": false,
    "isManager": true,
    "isSuperAdmin": false
  },
  "timestamp": 1779552230324
}
```

前端按钮建议：

| 按钮 / 功能 | 建议字段 |
|---|---|
| 新建子文件夹 | `canCreateChild` |
| 重命名 | `canRename` |
| 编辑备注 / 排序 / 状态 | `canEdit` |
| 删除 | `canDelete` |
| 移动 | `canMove` |
| 复制 | `canCopy` |
| 授权管理 | `canGrant` |
| 管理员设置 | `canManage` |
| 收藏 / 取消收藏 | `canFavorite` |

---

### 8.6.2 搜索文件夹【新增】

```
GET /api/v1/folders/search
```

按关键词搜索当前用户可见的文件夹，支持限定父目录、递归搜索、仅收藏过滤和分页。

**查询参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| keyword | string | 否 | - | 搜索关键词 |
| parentId | string\|number | 否 | - | 限定父目录 |
| recursive | boolean | 否 | true | 是否递归搜索子目录 |
| favoriteOnly | boolean | 否 | false | 是否仅搜索收藏文件夹 |
| page | int | 否 | 1 | 页码 |
| size | int | 否 | 20 | 每页数量 |

**响应**：分页结构，`list` 元素为 `FolderTreeNodeRespDTO`。

---

### 8.6.3 文件夹统计【新增】

```
GET /api/v1/folders/stats
```

返回当前用户可见范围的文件夹统计。超级管理员的 `totalFolders/totalDocuments/totalSize/activeFolders` 按全局口径；`favoriteFolders/managedFolders/ownedFolders/recentAccessCount` 仍按当前用户口径。

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "totalFolders": 86,
    "totalDocuments": 420,
    "totalSize": 21474836480,
    "activeFolders": 80,
    "favoriteFolders": 10,
    "managedFolders": 8,
    "ownedFolders": 12,
    "sharedWithMeFolders": 6,
    "recentAccessCount": 9,
    "storageQuota": 107374182400,
    "storageUsageRate": 0.2000
  },
  "timestamp": 1779552230324
}
```

字段说明：

| 字段 | 说明 |
|---|---|
| totalSize | 当前版本 UI 容量，按 `doc_document.latest_size` 统计 |
| sharedWithMeFolders | 只统计显式授权带来的可见文件夹，不包含同部门默认可见 |
| recentAccessCount | 近 7 天当前用户访问过的可见文件夹去重数 |
| storageQuota | 当前固定为 100GB，后续如有租户 / 部门配额再调整 |

---

### 8.6.4 快捷访问列表【新增 / 增强】

```
GET /api/v1/folders/favorites?limit=10
GET /api/v1/folders/recent?limit=10
GET /api/v1/folders/managed?limit=10
GET /api/v1/folders/owned?limit=10
```

| 接口 | 用途 | 排序建议 |
|---|---|---|
| `/favorites` | 我的收藏；已增强为快捷访问 DTO，并保留旧字段别名 | 收藏时间倒序 |
| `/recent` | 最近访问；同一文件夹按最近一次访问去重 | 访问时间倒序 |
| `/managed` | 我管理的文件夹 | 更新时间倒序 |
| `/owned` | 我创建 / 拥有的文件夹 | 创建时间倒序 |

`limit` 默认 10，最大 50。

**响应元素**

```json
{
  "id": "2058217227685957633",
  "name": "投标资料",
  "parentId": "0",
  "fullPath": "/投标资料",
  "level": 0,
  "sortNo": 1,
  "documentCount": 3,
  "lastAccessTime": "2026-06-23T10:00:00Z",
  "favoriteId": "2058217999000000001",
  "folderId": "2058217227685957633",
  "folderName": "投标资料",
  "createdAt": "2026-06-20T10:00:00Z",
  "createdBy": "1"
}
```

> `folderId/folderName/favoriteId/createdAt/createdBy` 主要用于兼容原 `/favorites` 响应；新页面建议统一使用 `id/name/fullPath/documentCount/lastAccessTime`。

---

### 8.6.5 记录文件夹访问【新增】

```
POST /api/v1/folders/{id}/access
```

用于支撑最近访问和访问统计。前端建议只在用户真正打开详情或执行下载 / 编辑动作时记录。

**请求体**

```json
{ "accessType": "VIEW" }
```

| accessType | 权限要求 | 建议触发点 |
|---|---|---|
| `VIEW` | 可查看文件夹 | 详情页打开成功后 |
| `DOWNLOAD` | 可查看文件夹 | 从该文件夹下载文档成功前后均可，建议成功后 |
| `EDIT` | 可编辑文件夹 | 编辑文件夹信息成功后 |

**响应**：`code=0, data=null`。

---

### 8.6.6 文件夹标签【新增】

```
GET /api/v1/folders/{id}/tags
PUT /api/v1/folders/{id}/tags
```

文件夹标签复用 `/api/v1/tags` 标签词表；这里的标签表示“手动绑定到文件夹的业务标签”。

**查询响应**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    { "id": "1", "name": "重要", "createdAt": "2026-06-01T00:00:00Z", "createdBy": "1" }
  ],
  "timestamp": 1779552230324
}
```

**全量替换请求体**

```json
{ "tagIds": ["1", "2", "3"] }
```

| 操作 | 权限 |
|---|---|
| 查询 | `FOLDER_VIEW` |
| 全量替换 | `FOLDER_EDIT` |

> `PUT` 会先删除旧绑定再插入新绑定；前端提交的是最终完整列表，不是增量 patch。传空数组表示清空文件夹标签。

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

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "successCount": 2,
    "failedCount": 0,
    "failedItems": []
  },
  "timestamp": 1779552230324
}
```

仍保持事务原子性：失败会走统一异常响应，不返回半成功列表；成功时 `failedItems=[]`。审计 op = `BATCH_DELETE`（即使 folderIds 只含 1 个 id 也是此 op）。

---

### 8.9 移动

```
PATCH /api/v1/folders/{id}/move
```

需源 `FOLDER_MOVE` + 目标父可编辑（`canEdit=true`）。创建子文件夹接口仍使用 `canCreateChild`。

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

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "2058217227685957633",
    "name": "投标资料",
    "parentId": "2058217319365054465",
    "fullPath": "/项目资料/投标资料"
  },
  "timestamp": 1779552230324
}
```

---

### 8.10 复制

```
POST /api/v1/folders/{id}/copy
```

需源 `FOLDER_COPY` + 目标父可编辑（`canEdit=true`）。创建子文件夹接口仍使用 `canCreateChild`。

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
| targetName | string | 否 | 新根节点名；为空则默认为 `原名称 - 副本`；重名自动追加序号，如 `原名称 - 副本 (2)` |

**复制语义**：

- 复制**整个子树**（含所有后代）。
- 只复制文件夹结构，**不复制** 文档、grant、manager、favorite。
- 新节点 `ownerUserId = 当前用户`；`ownerDeptId` 继承目标父，目标父无则用当前用户部门兜底。
- 子树相对排序（sortNo）保留；新根节点 sortNo 取目标父下 max+1。

**校验**：

- 源存在 / 目标父存在 / 权限（同 §8.9）
- 复制后最大层级 ≤ 8（`target.level + 1 + 源子树高度`），否则 `4002002`

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": "2058217348263809026",
    "name": "投标资料 - 副本",
    "parentId": "2058217319365054465",
    "fullPath": "/项目资料/投标资料 - 副本"
  },
  "timestamp": 1779552230324
}
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
| file | File | 是 | 文件对象 |
| name | string | 否 | 文档名称，为空则使用原始文件名 |
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
| size | int | 否 | 20 | 每页数量 |
| keyword | string | 否 | - | 按文档名称模糊搜索 |
| sortBy | string | 否 | createdAt | 新版排序字段：createdAt / updatedAt / name / size |
| sortOrder | string | 否 | desc | 新版排序方向：asc / desc |
| sort | string | 否 | createdAt | 旧版排序字段，仍兼容；当 `sortBy` 存在时以 `sortBy` 为准 |
| order | string | 否 | desc | 旧版排序方向，仍兼容；当 `sortOrder` 存在时以 `sortOrder` 为准 |

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
        "size": 1048576,
        "latestMime": "application/pdf",
        "mimeType": "application/pdf",
        "status": "APPROVED",
        "ownerUserId": "1",
        "uploadedBy": "1",
        "uploadedByName": "张三",
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

前端建议优先读取别名字段：

| 旧字段 | 新增别名 | 用途 |
|---|---|---|
| latestSize | size | 文件大小展示 |
| latestMime | mimeType | 文件类型图标 / 预览判断 |
| documentStatus | status | 列表状态展示 |
| ownerUserId | uploadedBy | 上传人 id |
| - | uploadedByName | 上传人显示名 |

---

### 11.4 删除文档

```
DELETE /api/v1/documents/{id}
```

软删除文档及其所有版本。需要对文档所在文件夹有 `FOLDER_DELETE` 权限。

**响应**：`code=0, data=null`

**典型错误**：4032001（无权限） / 4042001（文档不存在）

---

### 11.5 上传新版本

```
POST /api/v1/documents/{id}/versions
```

为已有文档上传新版本。需要对文档所在文件夹有 `FOLDER_EDIT` 权限。

**请求类型**: `multipart/form-data`

**表单字段**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| file | File | 是 | 文件对象 |
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

### 11.8.1 预览文档（当前版本）【新增】

```
GET /api/v1/documents/{id}/preview
```

预览文档当前版本，响应为内联文件流。当前代码复用下载链路并设置 `Content-Disposition: inline`，适合前端 iframe / 新窗口 / blob URL 预览。

**响应**: 文件流（PDF、图片、文本等实际 MIME 类型）

**响应头**:
- `Content-Type`: 文件的 MIME 类型
- `Content-Length`: 文件大小（字节）
- `Content-Disposition`: `inline; filename*=UTF-8''文件名`
- `Cache-Control`: `no-store`

---

### 11.8.2 预览指定版本【新增】

```
GET /api/v1/documents/{id}/versions/{versionNo}/preview
```

预览文档指定版本，响应同 §11.8.1。

---

### 11.8.3 更新资料元数据【新增】

```
PUT /api/v1/documents/{id}/metadata
```

补全或更新资料业务元数据，不生成新的文件版本。

**请求体**

```json
{
  "documentName": "投标文件.pdf",
  "businessCategory": "TECHNICAL",
  "tenderStructureCategory": "技术标",
  "sensitiveLevel": "INTERNAL",
  "ownerDeptId": "2058217226998091777",
  "sourceType": "UPLOAD",
  "hasExpireDate": true,
  "effectiveDate": "2026-06-01T00:00:00Z",
  "expireDate": "2026-12-31T23:59:59Z",
  "remark": "补全资料属性"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| documentName | string | 否 | 资料名称 |
| businessCategory | string | 否 | 业务分类，建议通过字典或后端约定取值 |
| tenderStructureCategory | string | 否 | 标书结构分类 |
| sensitiveLevel | string | 否 | 敏感等级，见 `/api/v1/dicts/sensitiveLevel` |
| ownerDeptId | string\|number | 否 | 归属部门 id |
| sourceType | string | 否 | 来源类型 |
| hasExpireDate | boolean | 否 | 是否存在有效期 |
| effectiveDate | datetime | 否 | 生效时间 |
| expireDate | datetime | 否 | 失效时间 |
| remark | string | 否 | 备注 |

**响应**：`code=0, data=null`。

---

### 11.8.4 提交资料审批状态【新增】

```
POST /api/v1/documents/{id}/submit-approval
```

将资料状态标记为审批中。当前代码说明具体审批实例由 workflow 接口处理；需要创建审批实例时使用 §17 的审批提交接口。

**响应**：`code=0, data=null`。

---

### 11.8.5 作废资料【新增】

```
POST /api/v1/documents/{id}/void?reason={reason}
```

将资料标记为已作废。

**查询参数**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| reason | string | 否 | 作废原因 |

**响应**：`code=0, data=null`。

---

### 11.8.6 恢复资料【新增】

```
POST /api/v1/documents/{id}/restore
```

将资料恢复到待提交状态。

**响应**：`code=0, data=null`。

---

### 11.8.7 查询可绑定资料【新增】

```
GET /api/v1/documents/bindable
```

查询已审批通过且当前用户可见的资料，供项目清单绑定资料时选择。

**查询参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| keyword | string | 否 | - | 搜索关键词 |
| page | int | 否 | 1 | 页码 |
| size | int | 否 | 20 | 每页数量 |

**响应**：分页结构同 §11.9 的 `PageResponse<DocumentListItemRespDTO>`。

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
| mimeType | string | 否 | - | MIME 类型过滤【新增】 |
| ownerUserId | string | 否 | - | 归属用户过滤【新增】 |
| documentNo | string | 否 | - | 资料编号过滤【新增】 |
| documentStatus | string | 否 | - | 资料状态过滤，见 `/api/v1/dicts/documentStatus`【新增】 |
| businessCategory | string | 否 | - | 业务分类过滤【新增】 |
| sensitiveLevel | string | 否 | - | 敏感等级过滤【新增】 |
| ownerDeptId | string | 否 | - | 归属部门过滤【新增】 |
| expiredOnly | boolean | 否 | false | 仅查询已过期资料【新增】 |
| createdFrom | datetime | 否 | - | 创建时间起点【新增】 |
| createdTo | datetime | 否 | - | 创建时间终点【新增】 |
| favoriteFolderOnly | boolean | 否 | false | 仅查询收藏文件夹内资料【新增】 |
| tagIds | array<string\|number> | 否 | - | 标签 id 过滤；GET 请求可重复传参或按前端 HTTP 客户端约定序列化【新增】 |
| sortBy | string | 否 | createdAt | 排序字段：createdAt / name / size |
| sortOrder | string | 否 | desc | 排序方向：asc / desc |
| page | int | 否 | 1 | 页码 |
| size | int | 否 | 20 | 每页数量 |

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
| limit | int | 否 | 10 | 返回记录数量 |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "keyword": "投标",
      "searchCount": 5,
      "lastSearchTime": "2026-05-31T10:30:00Z"
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
| limit | int | 否 | 10 | 返回记录数量 |
| days | int | 否 | 7 | 统计最近 N 天 |

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
    "totalSize": 5368709120,
    "totalVersions": 3200
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
| limit | int | 否 | 10 | 返回记录数量 |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "mimeType": "application/pdf",
      "count": 450,
      "totalSize": 2147483648
    },
    {
      "mimeType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      "count": 320,
      "totalSize": 1073741824
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
| limit | int | 否 | 10 | 返回记录数量 |
| days | int | 否 | 30 | 统计最近 N 天 |

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "documentId": "2058217319302139906",
      "documentName": "投标文件.pdf",
      "downloadCount": 85
    }
  ],
  "timestamp": 1779552230324
}
```

---

## 12. 字典接口 `/api/v1/dicts`【新增】

### 12.1 查询全部字典【新增】

```
GET /api/v1/dicts
```

返回项目、资料、审批相关枚举字典。当前代码包含：`projectStatus`、`projectStage`、`projectMemberRole`、`checklistItemStatus`、`documentStatus`、`sensitiveLevel`、`approvalStatus`、`approvalBizType`。

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "documentStatus": [
      {
        "type": "documentStatus",
        "code": "APPROVED",
        "name": "Approved",
        "description": "Approved document can be bound to checklist items",
        "sort": 4
      }
    ]
  },
  "timestamp": 1779552230324
}
```

### 12.2 按类型查询字典【新增】

```
GET /api/v1/dicts/{type}
```

**响应**：`data` 为 `DictItemRespDTO[]`；未知 `type` 返回空数组。

---

## 13. 文件夹收藏 `/api/v1/folders`【增强】

### 13.1 查询我的收藏文件夹【增强】

```
GET /api/v1/folders/favorites?limit=10
```

响应为 `FolderShortcutRespDTO[]`，详见 §8.6.4。接口保留 `favoriteId/folderId/folderName/createdAt/createdBy` 旧字段别名，旧页面可继续读取；新页面建议统一读取 `id/name/fullPath/documentCount/lastAccessTime`。

### 13.2 收藏文件夹【新增】

```
POST /api/v1/folders/{folderId}/favorite
```

**响应**：`code=0, data=null`。

### 13.3 取消收藏文件夹【新增】

```
DELETE /api/v1/folders/{folderId}/favorite
```

**响应**：`code=0, data=null`。

---

## 14. 标签接口 `/api/v1`【新增】

> 标签词表 `GET/POST/DELETE /api/v1/tags` 同时服务文档标签和文件夹标签。文件夹绑定接口见 §8.6.6，不需要单独维护第二套词表。

### 14.1 查询标签列表【新增】

```
GET /api/v1/tags
```

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": [
    { "id": "2058217319302139906", "name": "投标", "createdAt": "2026-06-19T10:00:00Z", "createdBy": "1" }
  ],
  "timestamp": 1779552230324
}
```

### 14.2 创建标签【新增】

```
POST /api/v1/tags
```

**请求体**

```json
{ "name": "投标" }
```

**响应**：`data` 为创建后的 `TagRespDTO`。

### 14.3 删除标签【新增】

```
DELETE /api/v1/tags/{id}
```

**响应**：`code=0, data=null`。

### 14.4 绑定文档标签【新增】

```
PUT /api/v1/documents/{id}/tags
```

**请求体**

```json
{ "tagIds": ["2058217319302139906", "2058217319302139907"] }
```

**响应**：`code=0, data=null`。

### 14.5 查询文档标签【新增】

```
GET /api/v1/documents/{id}/tags
```

**响应**：`data` 为 `TagRespDTO[]`。

---

## 15. 审计接口 `/api/v1/audit`【增强】

审计模块当前定位为“用户业务操作轨迹”，重点展示谁在什么时间对哪个业务对象做了什么操作。当前覆盖文件预览、下载、上传、新版本、授权、项目清单绑定、审批流转、文件夹权限变化等场景；不作为登录失败、注册失败、普通请求日志等安全风控流水使用。

本节为总接口文档内的审计接口契约摘要；更完整的前端展示建议见 `docs/handoffs/audit-frontend-contract.md`。

所有审计查询接口均需要 `SUPER_ADMIN` 角色。

### 15.1 查询审计日志【增强】

```
GET /api/v1/audit/logs
```

**查询参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| moduleCode | string | 否 | - | 模块码，如 `DOCUMENT` / `WORKFLOW` / `PROJECT` / `FOLDER` |
| bizType | string | 否 | - | 主业务对象类型，如 `DOCUMENT` / `FOLDER` / `CHECKLIST_ITEM` |
| bizId | string | 否 | - | 主业务对象 id |
| operationType | string | 否 | - | 操作类型，如 `DOWNLOAD` / `PREVIEW` / `APPROVAL_APPROVE` |
| operatorUserId | string | 否 | - | 操作用户 id |
| operatorDeptId | string | 否 | - | 操作部门 id |
| relatedBizType | string | 否 | - | 关联上下文类型，如 `APPROVAL_INSTANCE` / `PROJECT` |
| relatedBizId | string | 否 | - | 关联上下文 id |
| keyword | string | 否 | - | 关键词，后端匹配 `objectName`、`actionSummary`、`requestId` |
| startTime | datetime | 否 | - | 操作时间起点 |
| endTime | datetime | 否 | - | 操作时间终点 |
| page | int | 否 | 1 | 页码 |
| size | int | 否 | 20 | 每页数量 |

**响应**

`data` 为分页对象，`list` 元素为审计记录：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": "206800000000000001",
        "operationTime": "2026-06-20T15:34:16+08:00",
        "moduleCode": "DOCUMENT",
        "bizType": "DOCUMENT",
        "bizId": "300400000000000001",
        "operationType": "DOWNLOAD",
        "operatorUserId": "300200000000000001",
        "operatorName": "张三",
        "operatorDeptId": "100100000000000001",
        "objectName": "投标文件.pdf",
        "actionSummary": "张三 下载了《投标文件.pdf》 v3",
        "relatedBizType": "APPROVAL_INSTANCE",
        "relatedBizId": "206800000000000001",
        "clientIp": "10.0.0.8",
        "userAgent": "Mozilla/5.0 ...",
        "requestId": "33eabaa..."
      }
    ],
    "page": 1,
    "size": 20,
    "total": 1,
    "totalPages": 1,
    "hasNext": false
  },
  "timestamp": 1782017656000
}
```

**核心响应字段**

| 字段 | 类型 | 说明 |
|---|---|---|
| id | string | 审计记录 id |
| moduleCode | string | 模块码 |
| bizType | string | 主业务对象类型 |
| bizId | string | 主业务对象 id |
| operationType | string | 操作类型 |
| operatorUserId | string | 操作用户 id |
| operatorName | string | 操作人显示名 |
| operatorDeptId | string | 操作部门 id |
| requestId | string | 请求 id，用于排查链路 |
| objectName | string | 业务对象名称，如文件名、项目名、文件夹名 |
| actionSummary | string | 一句话业务摘要，列表页建议优先展示 |
| relatedBizType | string | 关联上下文类型 |
| relatedBizId | string | 关联上下文 id |
| clientIp | string | 客户端 IP，主要用于预览/下载等追溯 |
| userAgent | string | User-Agent，主要用于预览/下载等追溯 |
| operationTime | datetime | 操作时间 |
| beforeData | object | 变更前 JSON，列表页可不展示 |
| afterData | object | 变更后 JSON，列表页可不展示 |
| extraData | object | 扩展 JSON，列表页可不展示 |
| createdAt | datetime | 创建时间 |
| createdBy | string | 创建人 |

### 15.2 查询审计详情【新增】

```
GET /api/v1/audit/logs/{id}
```

返回单条完整审计记录，字段同 §15.1 的审计记录对象，包含 `beforeData`、`afterData`、`extraData`。

前端详情抽屉建议分为基础信息、变更前、变更后、扩展数据四块。`beforeData`、`afterData`、`extraData` 是 JSON 对象，不要直接拼接到文本或表格单元格里，否则会显示为 `[object Object]`；建议用格式化 JSON 查看器、键值表，或 `JSON.stringify(value, null, 2)` 展示。

### 15.3 查询对象时间线【新增】

```
GET /api/v1/audit/timeline?bizType=DOCUMENT&bizId={documentId}
GET /api/v1/audit/timeline?relatedBizType=APPROVAL_INSTANCE&relatedBizId={instanceId}
```

**查询参数**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| bizType | string | 二选一 | 与 `bizId` 配合，查看某个主业务对象轨迹，如文件轨迹 |
| bizId | string | 二选一 | 主业务对象 id |
| relatedBizType | string | 二选一 | 与 `relatedBizId` 配合，查看某个关联上下文轨迹，如审批实例流转 |
| relatedBizId | string | 二选一 | 关联上下文 id |

**响应**

`data` 为审计记录数组，按 `operationTime asc` 排序，字段同 §15.1 的审计记录对象。

**推荐调用场景**

- 文件详情页“操作轨迹”：`GET /api/v1/audit/timeline?bizType=DOCUMENT&bizId={documentId}`
- 审批详情页“流转轨迹”：`GET /api/v1/audit/timeline?relatedBizType=APPROVAL_INSTANCE&relatedBizId={instanceId}`

### 15.4 操作类型映射【增强】

| code | 含义 |
|---|---|
| `CREATE` | 创建 |
| `UPDATE` | 更新 |
| `RENAME` | 重命名 |
| `DELETE` | 删除 |
| `BATCH_DELETE` | 批量删除 |
| `MOVE` | 移动 |
| `COPY` | 复制 |
| `GRANT_ADD` | 新增授权 |
| `GRANT_REMOVE` | 移除授权 |
| `MANAGER_ADD` | 新增管理员 |
| `MANAGER_REMOVE` | 移除管理员 |
| `FAVORITE` | 收藏 |
| `UNFAVORITE` | 取消收藏 |
| `UPLOAD` | 上传文档 |
| `DOWNLOAD` | 下载文档 |
| `PREVIEW` | 预览文档 |
| `NEW_VERSION` | 上传新版本 |
| `APPROVAL_SUBMIT` | 提交审批 |
| `APPROVAL_APPROVE` | 审批通过 |
| `APPROVAL_REJECT` | 审批驳回 |
| `APPROVAL_WITHDRAW` | 撤回审批 |
| `APPROVAL_TRANSFER` | 转交审批 |
| `APPROVAL_ADD_SIGN` | 加签审批 |
| `APPROVAL_TERMINATE` | 终止审批 |
| `WORKFLOW_DEFINITION_CREATE` | 创建流程定义 |
| `WORKFLOW_DEFINITION_UPDATE` | 更新流程定义 |
| `WORKFLOW_DEFINITION_ENABLE` | 启用流程定义 |
| `WORKFLOW_DEFINITION_DISABLE` | 停用流程定义 |

---

## 16. 通知接口 `/api/v1/notifications`【新增】

### 16.1 查询我的通知【新增】

```
GET /api/v1/notifications
```

**查询参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| page | int | 否 | 1 | 页码 |
| size | int | 否 | 20 | 每页数量 |
| unreadOnly | boolean | 否 | false | 是否仅查询未读 |

**响应字段**：`id`、`type`、`title`、`content`、`bizType`、`bizId`、`readFlag`、`readAt`、`createdAt`。

### 16.2 查询未读数【新增】

```
GET /api/v1/notifications/unread-count
```

**响应**

```json
{ "code": 0, "message": "success", "data": { "count": 3 }, "timestamp": 1779552230324 }
```

### 16.3 标记单条已读【新增】

```
PATCH /api/v1/notifications/{id}/read
```

**响应**：`code=0, data=null`。

### 16.4 全部标记已读【新增】

```
PATCH /api/v1/notifications/read-all
```

**响应**：`code=0, data=null`。

---

## 17. 项目接口 `/api/v1/projects`【新增】

### 17.1 创建项目【新增】

```
POST /api/v1/projects
```

**请求体**

```json
{
  "projectName": "某项目投标",
  "tenderUnit": "招标单位",
  "ownerDeptId": "2058217226998091777",
  "projectType": "PUBLIC_BID",
  "bidDeadline": "2026-07-01T10:00:00Z",
  "folderId": "2058217319302139906",
  "remark": "重点项目，需提前锁定商务资料",
  "ownerUserIds": ["1"],
  "materialOwnerUserIds": ["2"],
  "memberUserIds": ["3"]
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| projectName | string | 是 | 项目名称 |
| tenderUnit | string | 否 | 招标单位 |
| ownerDeptId | string\|number | 是 | 归属部门 |
| projectType | string | 否 | 项目类型 |
| bidDeadline | datetime | 否 | 投标截止时间 |
| folderId | string\|number | 否 | 关联文件夹 |
| remark | string | 否 | 项目备注 |
| ownerUserIds | array | 否 | 项目负责人 |
| materialOwnerUserIds | array | 否 | 资料负责人 |
| memberUserIds | array | 否 | 项目成员 |

**响应**：`data={ "id": "项目id" }`。

### 17.2 更新项目【新增】

```
PUT /api/v1/projects/{id}
```

请求体字段：`projectName`、`tenderUnit`、`projectType`、`bidDeadline`、`folderId`、`remark`，均为可选。

### 17.3 查询项目详情【新增】

```
GET /api/v1/projects/{id}
```

**响应字段**：`id`、`projectNo`、`projectName`、`tenderUnit`、`ownerDeptId`、`projectType`、`projectStage`、`projectStatus`、`bidDeadline`、`folderId`、`remark`、`createdAt`、`updatedAt`。

### 17.4 查询项目列表【新增】

```
GET /api/v1/projects
```

**查询参数**：`keyword`、`projectNo`、`ownerDeptId`、`projectType`、`projectStage`、`projectStatus`、`ownerUserId`、`deadlineFrom`、`deadlineTo`、`page`、`size`。

### 17.5 添加项目成员【新增】

```
POST /api/v1/projects/{id}/members
```

**请求体**

```json
{ "userIds": ["2058217227220389889"], "memberRole": "MEMBER" }
```

### 17.6 移除项目成员【新增】

```
DELETE /api/v1/projects/{id}/members/{userId}
```

### 17.7 修改项目阶段【新增】

```
PATCH /api/v1/projects/{id}/stage
```

**请求体**：`{ "projectStage": "PREPARE" }`

### 17.8 修改项目状态【新增】

```
PATCH /api/v1/projects/{id}/status
```

**请求体**：`{ "projectStatus": "NORMAL" }`

可用状态见字典 `PROJECT_STATUS` 或后端枚举：`NORMAL`、`PAUSED`、`ARCHIVED`、`CANCELLED`、`DELETED`。

**归档触发**：当 `projectStatus=ARCHIVED` 时，后端会在同一事务内执行项目归档：
- 校验项目可编辑，已归档项目再次修改会返回 `4005003`。
- 校验所有必需清单项均为 `COMPLETE`，否则返回 `4005004`。
- 校验绑定资料和固化版本均未作废、未删除、未过期，且资料状态和版本审批状态均为 `APPROVED`。
- 校验项目下不存在审批中的资料、版本或清单项。
- 生成归档记录、清单快照、资料版本快照，并将项目阶段同步为 `ARCHIVED`。
- 归档成功后，后续资料新版本不会影响本次归档快照。

**响应**：`code=0, data=null`。

### 17.9 查询项目归档详情【新增】

```
GET /api/v1/projects/{id}/archive
```

查询项目最近一次归档记录及固化快照。当前用户需具备项目查看权限；项目未归档或无归档记录时返回 `4041001`（message 为“项目归档记录不存在”）。

**响应示例**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "record": {
      "id": "300800000000000001",
      "projectId": "300500000000000001",
      "archiveNo": "ARCH-BD202606190001",
      "archiveStatus": "ARCHIVED",
      "archivedAt": "2026-06-19T15:30:00Z",
      "archivedBy": "300200000000000001",
      "archiveReason": "项目归档",
      "checklistTotal": 12,
      "checklistComplete": 12,
      "documentTotal": 18,
      "snapshotHash": "9f2f1c...",
      "remark": null
    },
    "checklistSnapshots": [
      {
        "id": "300810000000000001",
        "archiveRecordId": "300800000000000001",
        "projectId": "300500000000000001",
        "checklistItemId": "300620000000000001",
        "templateItemId": "300610000000000001",
        "itemName": "商务响应文件",
        "requiredFlag": true,
        "itemStatus": "ARCHIVED",
        "boundDocumentCount": 2,
        "snapshotJson": "{\"originalStatus\":\"COMPLETE\",\"businessCategory\":\"商务标\"}"
      }
    ],
    "documentSnapshots": [
      {
        "id": "300820000000000001",
        "archiveRecordId": "300800000000000001",
        "projectId": "300500000000000001",
        "checklistItemId": "300620000000000001",
        "documentId": "300400000000000001",
        "versionNo": 2,
        "documentName": "医院智能化改造项目-商务响应文件.docx",
        "documentStatus": "APPROVED",
        "versionStatus": "APPROVED",
        "expireAt": null,
        "storageType": "SYSTEM",
        "fileSize": 245760,
        "snapshotJson": "{\"documentNo\":\"DOC-20260619-001\",\"currentVersionNo\":2}"
      }
    ]
  },
  "timestamp": 1779552230324
}
```

**字段说明**

| 字段 | 说明 |
|---|---|
| record | 归档主记录，包含归档编号、归档时间、归档人、清单/资料统计和快照 hash |
| checklistSnapshots | 归档时固化的项目清单状态；`snapshotJson` 保留原状态、业务分类、负责人、数量约束等扩展信息 |
| documentSnapshots | 归档时固化的资料版本；`versionNo` 是归档绑定版本，后续资料新版本不会改变该值 |

### 17.10 查询项目工作台详情【新增】

```
GET /api/v1/projects/{id}/workbench
```

用于项目详情页首屏。项目工作台只做项目上下文聚合，不复制资料模块和审批模块的处理能力；资料上传、资料详情、审批处理仍跳转到对应模块接口。

**权限**

- `SUPER_ADMIN` 可查看全部项目。
- 项目成员可查看自己参与的项目。
- 清单责任人即使不是项目成员，也可查看被分配责任清单所在项目，但不因此获得成员维护或项目管理权限。
- 已归档项目返回 `summary.readOnly=true`，前端应隐藏编辑、成员维护、生成清单、绑定资料等写操作。

**响应主要结构**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "summary": {
      "projectId": "3005000000000000001",
      "projectNo": "TB-20260620-001",
      "projectName": "东海医院智能化改造项目",
      "projectStage": "COLLECTING",
      "projectStatus": "NORMAL",
      "bidDeadline": "2026-07-01T10:00:00+08:00",
      "checklistCompletionRate": 33,
      "riskCount": 3,
      "readOnly": false,
      "primaryActions": ["VIEW_PROJECT_FILES", "VIEW_RELATED_APPROVALS", "UPDATE_PROJECT"]
    },
    "basicInfo": {
      "id": "3005000000000000001",
      "projectNo": "TB-20260620-001",
      "projectName": "东海医院智能化改造项目",
      "tenderUnit": "东海市第一人民医院",
      "projectType": "SMART_BUILDING",
      "projectStage": "COLLECTING",
      "projectStatus": "NORMAL",
      "bidDeadline": "2026-07-01T10:00:00+08:00",
      "remark": "重点医疗行业投标项目，商务标和技术标需同步推进。"
    },
    "organization": {
      "ownerDeptId": "3001000000000000021",
      "ownerDeptName": "投标管理部",
      "businessUnit": "TB",
      "owners": [{ "userId": "3002000000000000002", "userName": "张晨" }],
      "materialOwners": [{ "userId": "3002000000000000003", "userName": "李若溪" }],
      "participantCount": 4
    },
    "checklistStats": {
      "total": 4,
      "complete": 1,
      "pendingCollect": 1,
      "pendingReview": 1,
      "needSupplement": 1,
      "archived": 0,
      "overdue": 1,
      "boundDocumentCount": 2
    },
    "riskSummaries": [
      { "code": "OVERDUE_CHECKLIST", "name": "清单逾期", "level": "HIGH", "count": 1 },
      { "code": "NEED_SUPPLEMENT", "name": "需补充资料", "level": "MEDIUM", "count": 1 },
      { "code": "PENDING_APPROVAL", "name": "待处理审批", "level": "MEDIUM", "count": 1 }
    ],
    "memberResponsibilities": [],
    "checklist": [],
    "projectFiles": [],
    "approvalSummaries": [],
    "archiveRecords": [],
    "operationLogs": [],
    "tabs": [
      { "code": "OVERVIEW", "label": "概览" },
      { "code": "PROJECT_BASIC_INFO", "label": "项目基本信息" },
      { "code": "MEMBER_RESPONSIBILITIES", "label": "成员职责" },
      { "code": "CHECKLIST", "label": "资料清单" },
      { "code": "PROJECT_FILES", "label": "项目文件" },
      { "code": "RELATED_APPROVALS", "label": "关联审批" },
      { "code": "ARCHIVE_RECORDS", "label": "归档记录" },
      { "code": "OPERATION_LOGS", "label": "操作日志" }
    ]
  },
  "timestamp": 1779552230324
}
```

前端文案使用“项目基本信息”“关联审批”，不要在项目详情中使用“流程状态”来描述审批聚合区。

---

## 18. 清单模板接口 `/api/v1/checklist-templates`【新增】

### 18.1 创建模板【新增】

```
POST /api/v1/checklist-templates
```

**请求体**：`{ "templateName": "默认模板", "projectType": "PUBLIC_BID", "enabled": true }`

### 18.2 更新模板【新增】

```
PUT /api/v1/checklist-templates/{id}
```

### 18.3 查询模板详情【新增】

```
GET /api/v1/checklist-templates/{id}
```

### 18.4 查询模板列表【新增】

```
GET /api/v1/checklist-templates?projectType={projectType}&page=1&size=20
```

### 18.5 删除模板【新增】

```
DELETE /api/v1/checklist-templates/{id}
```

### 18.6 新增模板清单项【新增】

```
POST /api/v1/checklist-templates/{id}/items
```

**请求体字段**：`itemName`、`description`、`required`、`businessCategory`、`tenderStructureCategory`、`suggestedSensitiveLevel`、`allowedSource`、`allowedFileTypes`、`minCount`、`maxCount`、`sortOrder`。

### 18.7 更新模板清单项【新增】

```
PUT /api/v1/checklist-templates/{id}/items/{itemId}
```

### 18.8 删除模板清单项【新增】

```
DELETE /api/v1/checklist-templates/{id}/items/{itemId}
```

## 19. 项目资料清单接口 `/api/v1/projects/{projectId}/checklist`【新增】

### 19.1 根据模板生成清单【新增】

```
POST /api/v1/projects/{projectId}/checklist/generate?templateId={templateId}
```

### 19.2 查询项目清单【新增】

```
GET /api/v1/projects/{projectId}/checklist
```

**响应字段**：`id`、`projectId`、`templateItemId`、`itemName`、`description`、`required`、`businessCategory`、`tenderStructureCategory`、`sensitiveLevel`、`allowedSource`、`allowedFileTypes`、`minCount`、`maxCount`、`deadline`、`ownerUserId`、`status`、`sortOrder`、`createdAt`、`updatedAt`。

### 19.3 更新清单项负责人【新增】

```
PATCH /api/v1/projects/{projectId}/checklist/items/{itemId}/owner
```

**请求体**：`{ "ownerUserId": "2058217227220389889", "deadline": "2026-07-01T10:00:00Z" }`

### 19.4 绑定资料到清单项【新增】

```
POST /api/v1/projects/{projectId}/checklist/items/{itemId}/documents
```

**请求体**：`{ "documentId": "2058217319302139906", "versionNo": 1 }`

### 19.5 解绑清单项资料【新增】

```
DELETE /api/v1/projects/{projectId}/checklist/items/{itemId}/documents/{documentId}
```

---

## 20. 审批接口 `/api/v1`【新增】

### 20.1 提交资料审批【新增】

```
POST /api/v1/documents/{id}/approval/submit
```

**请求体**：`{ "comment": "请审批" }`，可省略。

**响应**：`data={ "instanceId": "审批实例id" }`。

### 20.2 提交资料版本审批【新增】

```
POST /api/v1/documents/{id}/versions/{versionNo}/approval/submit
```

### 20.3 提交清单项审批【新增】

```
POST /api/v1/projects/{projectId}/checklist/items/{itemId}/approval/submit
```

### 20.4 查询我的审批任务【新增】

```
GET /api/v1/approvals/tasks?status={status}
```

**查询参数**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| status | string | 否 | 按任务状态过滤，如 `PENDING`、`APPROVED`、`REJECTED`、`TRANSFERRED`、`WITHDRAWN`、`CANCELLED` |

**权限语义**：普通用户只返回分配给自己的审批任务；`SUPER_ADMIN` 返回所有审批任务，用于全局审批监管。

**响应字段**：`taskId`、`instanceId`、`documentId`、`definitionName`、`nodeName`、`actionType`、`actionComment`、`actionTime`、`handlerUserId`、`businessTitle`、`bizModule`、`bizId`、`documentName`、`documentNo`、`projectName`、`checklistItemName`、`submitterName`、`approverUserId`、`approverName`、`handlerName`、`instanceStatus`、`instanceStatusName`、`status`、`comment`、`canApprove`、`canReject`、`canTransfer`、`canAddSign`、`canWithdraw`、`canTerminate`、`createdAt`、`handledAt`。

**字段补充说明**

| 字段 | 说明 |
|---|---|
| businessTitle | 后端聚合的业务标题；资料审批通常为资料名，版本审批带版本号，清单项审批通常为“项目名 / 清单项名” |
| documentName / documentNo | 审批关联资料的名称和资料编号；清单项审批无直接资料时可能为空 |
| projectName / checklistItemName | 清单项审批时返回项目名和清单项名 |
| submitterName / approverName / handlerName | 发起人、当前处理人、实际处理人显示名；优先 `realName`，为空时回退 `username` |
| instanceStatus / instanceStatusName | 审批实例状态及中文名 |
| canApprove / canReject / canTransfer / canAddSign | 当前登录用户是否可对该任务执行对应动作；超级管理员可处理所有待处理任务 |
| canWithdraw | 当前登录用户是否可撤回审批实例；待处理实例下发起人或超级管理员为 true |
| canTerminate | 当前登录用户是否可终止审批实例；待处理实例下仅超级管理员为 true |

### 20.5 审批通过【新增】

```
POST /api/v1/approvals/{id}/approve
```

**请求体**：`{ "comment": "同意" }`，可省略。

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "taskStatus": "APPROVED",
    "instanceStatus": "PENDING",
    "completed": false,
    "nextTaskId": "300900000000000021",
    "nextNodeName": "法务审查"
  },
  "timestamp": 1781892000000
}
```

- `completed=false` 表示当前节点已通过，但整个流程仍有后续节点。
- `completed=true` 表示审批实例已进入终态，`nextTaskId` 和 `nextNodeName` 为 `null`。

### 20.6 审批拒绝【新增】

```
POST /api/v1/approvals/{id}/reject
```

**请求体**：`{ "comment": "请补充材料" }`，可省略。

**响应**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "taskStatus": "REJECTED",
    "instanceStatus": "REJECTED",
    "completed": true,
    "nextTaskId": null,
    "nextNodeName": null
  },
  "timestamp": 1781892000000
}
```

### 20.7 撤回审批【新增】

```
POST /api/v1/approvals/{id}/withdraw
```

路径参数 `{id}` 为审批实例 id（`instanceId`）。仅审批发起人或 `SUPER_ADMIN` 可撤回待处理实例；撤回会关闭所有待处理任务，并回滚资料/版本/清单项审批中状态。

**请求体**：`{ "comment": "资料需重新调整" }`，可省略。

**响应**：`code=0, data=null`。

### 20.8 转交审批任务【新增】

```
POST /api/v1/approvals/tasks/{taskId}/transfer
```

仅当前任务处理人或 `SUPER_ADMIN` 可转交待处理任务。转交会关闭原任务，生成一个新的待处理任务给目标用户。

**请求体**

```json
{
  "targetUserId": "300200000000000006",
  "comment": "请法务协助处理"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| targetUserId | string\|number | 是 | 目标用户必须存在、启用，且不能是当前处理人 |
| comment | string | 否 | 转交说明 |

**响应**：`code=0, data=null`。

### 20.9 加签审批任务【新增】

```
POST /api/v1/approvals/tasks/{taskId}/add-sign
```

仅当前任务处理人或 `SUPER_ADMIN` 可对待处理任务加签。加签会在同一审批实例、同一节点下新增一个待处理任务；同节点加签任务全部处理完成前，流程不会进入下一节点。

**请求体**

```json
{
  "assigneeUserId": "300200000000000005",
  "comment": "请补充质量合规意见"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| assigneeUserId | string\|number | 是 | 加签人必须存在、启用，且不能是当前处理人 |
| comment | string | 否 | 加签说明 |

**响应**：`code=0, data=null`。

### 20.10 终止审批【新增】

```
POST /api/v1/approvals/{id}/terminate
```

路径参数 `{id}` 为审批实例 id（`instanceId`）。仅 `SUPER_ADMIN` 可终止待处理审批实例；终止会关闭所有待处理任务，并按终止语义回滚业务审批中状态。

**请求体**

```json
{ "reason": "项目已取消，终止相关审批" }
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| reason | string | 是 | 终止原因，不能为空 |

**响应**：`code=0, data=null`。

### 20.11 查询资料审批历史【新增】

```
GET /api/v1/documents/{id}/approval/history
```

### 20.12 查询项目审批历史【新增】

```
GET /api/v1/projects/{projectId}/approval/history
```

**审批历史响应字段**：`instanceId`、`documentId`、`versionNo`、`definitionName`、`nodeName`、`actionType`、`actionComment`、`actionTime`、`handlerUserId`、`bizModule`、`bizId`、`bizType`、`submitterUserId`、`instanceStatus`、`submitComment`、`submittedAt`、`completedAt`、`taskId`、`approverUserId`、`taskStatus`、`taskComment`、`handledAt`。

---

## 21. 审批流程定义接口 `/api/v1/workflow/definitions`【新增】

### 21.1 创建流程定义【新增】

```
POST /api/v1/workflow/definitions
```

**请求体**

```json
{
  "name": "技术方案审批流程",
  "scenario": "DOCUMENT_APPROVAL",
  "bizModule": "DOCUMENT",
  "bizType": "DOCUMENT",
  "deptId": "300100000000000001",
  "businessCategory": "技术标"
}
```

**响应**：`data={ "id": "流程定义id" }`。

### 21.2 更新流程定义【新增】

```
PUT /api/v1/workflow/definitions/{id}
```

请求体字段同创建接口。更新定义会生成新版本语义，具体版本号由后端维护。

### 21.3 查询流程定义详情【新增】

```
GET /api/v1/workflow/definitions/{id}
```

**响应字段**：`id`、`name`、`scenario`、`bizModule`、`bizType`、`deptId`、`businessCategory`、`version`、`enabled`、`nodes`、`createdAt`、`updatedAt`。

`nodes` 元素字段：`id`、`definitionId`、`nodeCode`、`nodeName`、`nodeType`、`approveMode`、`assigneeType`、`assigneeValue`、`sortOrder`、`nextNodeCode`、`rejectToNodeCode`、`conditions`、`createdAt`、`updatedAt`。

`conditions` 元素字段：`id`、`definitionId`、`nodeId`、`conditionCode`、`fieldName`、`operator`、`compareValue`、`targetNodeCode`、`sortOrder`、`createdAt`、`updatedAt`。

### 21.4 查询流程定义列表【新增】

```
GET /api/v1/workflow/definitions?scenario={scenario}&bizType={bizType}&enabled={enabled}&page=1&size=20
```

分页响应结构同 §22.9；`list` 元素为流程定义详情的简化或完整结构（以响应为准）。

### 21.5 启用 / 停用流程定义【新增】

```
POST /api/v1/workflow/definitions/{id}/enable
POST /api/v1/workflow/definitions/{id}/disable
```

**响应**：`code=0, data=null`。

### 21.6 新增流程节点【新增】

```
POST /api/v1/workflow/definitions/{id}/nodes
```

**请求体**

```json
{
  "nodeCode": "LEGAL_REVIEW",
  "nodeName": "法务审查",
  "nodeType": "APPROVAL",
  "approveMode": "ANY",
  "assigneeType": "USER",
  "assigneeValue": "300200000000000005",
  "sortOrder": 20,
  "nextNodeCode": "QUALITY_REVIEW",
  "rejectToNodeCode": null,
  "conditions": [
    {
      "conditionCode": "AMOUNT_GE_100W",
      "fieldName": "amount",
      "operator": ">=",
      "compareValue": "1000000",
      "targetNodeCode": "MANAGER_REVIEW",
      "sortOrder": 1
    }
  ]
}
```

**响应**：`data={ "id": "节点id" }`。

### 21.7 更新流程节点【新增】

```
PUT /api/v1/workflow/definitions/{id}/nodes/{nodeId}
```

请求体字段同新增节点接口。

### 21.8 删除流程节点【新增】

```
DELETE /api/v1/workflow/definitions/{id}/nodes/{nodeId}
```

**响应**：`code=0, data=null`。

---

## 22. 前端联调建议

### 22.1 ID 处理

- 后端把 Long 序列化为 String。前端定义类型时建议 `string` 形式存储；如需做 `===` 比较请保持字符串。
- 提交请求时 `string` 或 `number` 都可，但**强烈推荐 `string`** 以避免 JS 大整数精度问题。

### 22.2 鉴权请求头封装

```ts
// axios 示例
axios.interceptors.request.use(cfg => {
  const token = localStorage.getItem('token');
  if (token) cfg.headers.Authorization = token; // 当前 token 不包含 Bearer 前缀
  return cfg;
});
```

注意：登录响应里的 `data.token` 当前不带 `Bearer ` 前缀；如果后端重新启用 token-prefix，需要同步调整。

### 22.3 错误处理

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

### 22.4 树形数据组装

`/folders/tree/root` 与 `/folders/children` 都是**懒加载**单层返回，前端按需在用户展开节点时再请求 `children`。`hasChildren` 字段已是权限过滤后的真实值。

### 22.5 文件夹移动 / 复制的 UI 提示

- **移动**：禁止把节点拖到自身或其后代下；前端可基于 `ancestorIds` 字段预判（含自身 id 即非法），减少 404/4002008 报错。
- **复制**：在目标父下重名时，可弹框让用户输入新名（即 `targetName` 字段），无需重新拉源节点。

### 22.6 批量删除的提示

- batchDelete 是**事务原子**：任一 id 失败整体回滚。前端不会拿到"部分成功"语义。
- 建议在前端按用户选择构造 `folderIds`，然后由后端做父子去重，前端无需重复实现。

### 22.7 文件上传处理

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

### 22.8 文件下载处理

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

### 22.9 分页数据处理

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
  const response = await axios.get<ApiResponse<PageResponse<DocumentListItem>>>(
    `/api/v1/folders/${folderId}/documents`,
    { params: { page, size, sortBy: 'createdAt', sortOrder: 'desc' } }
  );
  return response.data.data;
};
```

### 22.10 文件大小格式化

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

### 22.11 MIME 类型图标映射

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

### 22.12 搜索防抖

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

### 22.13 权限控制

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
type FolderPermissions = {
  canView: boolean;
  canCreateChild: boolean;
  canRename: boolean;
  canEdit: boolean;
  canDelete: boolean;
  canMove: boolean;
  canCopy: boolean;
  canGrant: boolean;
  canManage: boolean;
  canFavorite: boolean;
};

const p = folder.permissions as FolderPermissions;

const actions = {
  createChild: p.canCreateChild,
  rename: p.canRename,
  edit: p.canEdit,
  delete: p.canDelete,
  move: p.canMove,
  copy: p.canCopy,
  grant: p.canGrant,
  manage: p.canManage,
  favorite: p.canFavorite,
};
```

### 22.14 文件夹管理页推荐请求编排

```ts
const fetchFolderHome = async () => {
  const [stats, roots, favorites, recent] = await Promise.all([
    axios.get<ApiResponse<FolderStats>>('/api/v1/folders/stats'),
    axios.get<ApiResponse<FolderTreeNode[]>>('/api/v1/folders/tree/root'),
    axios.get<ApiResponse<FolderShortcut[]>>('/api/v1/folders/favorites', { params: { limit: 10 } }),
    axios.get<ApiResponse<FolderShortcut[]>>('/api/v1/folders/recent', { params: { limit: 10 } }),
  ]);

  return {
    stats: stats.data.data,
    roots: roots.data.data,
    favorites: favorites.data.data,
    recent: recent.data.data,
  };
};

const openFolder = async (folderId: string) => {
  const [detail, documents] = await Promise.all([
    axios.get<ApiResponse<FolderDetail>>(`/api/v1/folders/${folderId}/detail`),
    axios.get<ApiResponse<PageResponse<DocumentListItem>>>(`/api/v1/folders/${folderId}/documents`, {
      params: { page: 1, size: 20, sortBy: 'createdAt', sortOrder: 'desc' },
    }),
  ]);

  // 只在用户真正打开详情时记录 VIEW，避免树预加载污染“最近访问”。
  axios.post(`/api/v1/folders/${folderId}/access`, { accessType: 'VIEW' }).catch(() => {});

  return {
    detail: detail.data.data,
    documents: documents.data.data,
  };
};
```

---

## 23. 前端开发注意事项

### 23.1 安全注意事项

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

### 23.2 性能优化

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

### 23.3 用户体验

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

### 23.4 兼容性

1. **浏览器兼容**
   - 支持 Chrome/Edge/Firefox/Safari 最新两个版本
   - 文件下载在不同浏览器测试
   - FormData 上传在移动端测试

2. **文件名处理**
   - 支持中文文件名
   - 处理特殊字符（`/`, `\`, `:`, `*`, `?`, `"`, `<`, `>`, `|`）
   - 文件名长度限制（建议 ≤ 255 字符）

### 23.5 测试建议

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

### 23.6 开发工具推荐

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

## 24. 常见问题 FAQ

### Q1: Token 过期后如何处理？

**A**: 后端返回 `code=4011001` 或 HTTP 401 时，清除本地 token 并跳转到登录页。可以在 axios 拦截器中统一处理。

### Q2: 如何判断用户是否有某个文件夹的权限？

**A**: 文件夹接口已返回当前用户权限，建议：
- 打开详情时使用 `GET /api/v1/folders/{id}/detail` 返回的 `permissions` 对象。
- 只需要权限时可单独调用 `GET /api/v1/folders/{id}/permissions/me`。
- 按 `canCreateChild/canRename/canEdit/canDelete/canMove/canCopy/canGrant/canManage/canFavorite` 控制按钮显示。
- 前端隐藏按钮只是体验优化，后端仍是最终安全边界；操作失败时继续根据错误码提示用户。

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

## 25. 变更日志

| 日期 | 版本 | 范围 | 备注 |
|---|---|---|---|
| 2026-06-23 | v2.6 | 增量更新 | 【增强】文件夹管理接口补充统计、增强详情、最近/收藏/管理/拥有快捷访问、访问记录、文件夹标签、移动/复制/批量删除响应和文档列表字段别名；新增前端文件夹页对接建议 |
| 2026-06-21 | v2.5 | 增量更新 | 【增强】审计日志补充业务轨迹字段、关键词/关联业务筛选、详情接口、对象时间线接口和前端展示建议 |
| 2026-06-20 | v2.4 | 增量更新 | 【新增】项目工作台详情接口；项目基础信息新增 remark 字段；普通用户项目可见范围补充清单责任人视角 |
| 2026-06-20 | v2.3 | 增量更新 | 【增强】审批任务列表补充业务标题、资料/项目/清单项名称、发起人/处理人显示名、实例状态和前端动作权限字段；审批通过/拒绝接口改为返回任务状态、实例状态、完成标记和下一任务/节点信息 |
| 2026-06-19 | v2.2 | 增量更新 | 【新增】补充项目归档详情接口、项目状态归档触发语义、归档前校验和归档只读错误码；补充审批撤回、转交、加签、终止接口；补充审批流程定义维护接口；明确超级管理员可查看全部审批任务 |
| 2026-06-19 | v2.1 | 增量更新 | 【新增】根据当前后端代码补充字典、文件夹收藏、标签、审计、通知、项目、清单模板、项目资料清单、审批流接口；补充文档预览、元数据、状态流转、可绑定资料查询和搜索新增过滤参数 |
| 2026-05-31 | v2.0 | 全量更新 | 补充 Document 模块完整接口（上传/下载/版本管理/搜索/统计）；新增前端开发注意事项、常见问题 FAQ；优化代码示例和最佳实践 |
| 2026-05-24 | v1.0 | 首版 | 覆盖 auth/user/role/dept/folder 全量接口 |

---

## 附录：完整接口清单

### 认证模块 (4)
- `POST /api/v1/auth/register` - 用户注册
- `POST /api/v1/auth/login` - 用户登录
- `GET /api/v1/auth/me` - 获取当前用户
- `POST /api/v1/auth/logout` - 退出登录

### 用户模块 (3)
- `GET /api/v1/users/options` - 查询用户选择器选项
- `GET /api/v1/users/{id}` - 查询用户详情
- `PATCH /api/v1/users/{id}/status` - 启用/禁用用户

### 角色模块 (4)
- `POST /api/v1/roles/assign` - 分配角色
- `DELETE /api/v1/roles/revoke` - 撤销角色
- `GET /api/v1/roles/user/{userId}` - 查询用户角色列表
- `GET /api/v1/roles/code/{roleCode}/users` - 查询角色下的用户

### 部门模块 (5)
- `POST /api/v1/departments` - 创建部门
- `GET /api/v1/departments` - 查询部门管理树
- `PUT /api/v1/departments/{id}` - 编辑部门
- `PATCH /api/v1/departments/{id}/status` - 启用/禁用部门
- `DELETE /api/v1/departments/{id}` - 删除空部门

### 文件夹模块 (20)
- `POST /api/v1/folders` - 创建文件夹
- `GET /api/v1/folders/{id}` - 查询增强详情
- `GET /api/v1/folders/{id}/detail` - 查询增强详情
- `GET /api/v1/folders/stats` - 查询文件夹统计
- `PATCH /api/v1/folders/{id}/name` - 重命名
- `PUT /api/v1/folders/{id}` - 编辑
- `GET /api/v1/folders/children` - 子节点列表
- `GET /api/v1/folders/tree/root` - 根树
- `GET /api/v1/folders/{id}/permissions/me` - 【新增】查询当前用户文件夹权限
- `GET /api/v1/folders/search` - 【新增】搜索文件夹
- `GET /api/v1/folders/recent` - 查询最近访问文件夹
- `GET /api/v1/folders/managed` - 查询我管理的文件夹
- `GET /api/v1/folders/owned` - 查询我拥有的文件夹
- `DELETE /api/v1/folders/{id}` - 删除
- `DELETE /api/v1/folders/batch` - 批量删除
- `PATCH /api/v1/folders/{id}/move` - 移动
- `POST /api/v1/folders/{id}/copy` - 复制
- `POST /api/v1/folders/{id}/access` - 记录文件夹访问
- `GET /api/v1/folders/{id}/tags` - 查询文件夹标签
- `PUT /api/v1/folders/{id}/tags` - 全量替换文件夹标签

### 文件夹授权模块 (3)
- `GET /api/v1/folders/{folderId}/grants` - 查询授权列表
- `POST /api/v1/folders/{folderId}/grants` - 新增授权
- `DELETE /api/v1/folders/{folderId}/grants/{grantId}` - 删除授权

### 文件夹管理员模块 (3)
- `GET /api/v1/folders/{folderId}/managers` - 查询管理员列表
- `POST /api/v1/folders/{folderId}/managers` - 新增管理员
- `DELETE /api/v1/folders/{folderId}/managers/{managerId}` - 删除管理员

### 文档模块 (22)
- `POST /api/v1/folders/{folderId}/documents` - 上传新文档
- `GET /api/v1/documents/{id}` - 获取文档详情
- `PUT /api/v1/documents/{id}/metadata` - 【新增】更新资料元数据
- `POST /api/v1/documents/{id}/submit-approval` - 【新增】提交资料审批状态
- `POST /api/v1/documents/{id}/void` - 【新增】作废资料
- `POST /api/v1/documents/{id}/restore` - 【新增】恢复资料
- `GET /api/v1/folders/{folderId}/documents` - 获取文档列表
- `DELETE /api/v1/documents/{id}` - 删除文档
- `POST /api/v1/documents/{id}/versions` - 上传新版本
- `GET /api/v1/documents/{id}/versions` - 获取版本列表
- `GET /api/v1/documents/{id}/download` - 下载文档
- `GET /api/v1/documents/{id}/versions/{versionNo}/download` - 下载指定版本
- `GET /api/v1/documents/{id}/preview` - 【新增】预览文档
- `GET /api/v1/documents/{id}/versions/{versionNo}/preview` - 【新增】预览指定版本
- `GET /api/v1/documents/search` - 搜索文档
- `GET /api/v1/documents/bindable` - 【新增】查询可绑定资料
- `GET /api/v1/documents/search/history` - 获取搜索历史
- `DELETE /api/v1/documents/search/history` - 清除搜索历史
- `GET /api/v1/documents/search/hot` - 获取热门搜索
- `GET /api/v1/documents/stats/storage` - 获取存储统计
- `GET /api/v1/documents/stats/types` - 获取文档类型分布
- `GET /api/v1/documents/stats/popular` - 获取热门文档排行

### 字典模块 (2)【新增】
- `GET /api/v1/dicts` - 查询全部字典
- `GET /api/v1/dicts/{type}` - 按类型查询字典

### 文件夹收藏模块 (3)【增强】
- `GET /api/v1/folders/favorites` - 查询我的收藏文件夹快捷访问列表
- `POST /api/v1/folders/{folderId}/favorite` - 收藏文件夹
- `DELETE /api/v1/folders/{folderId}/favorite` - 取消收藏文件夹

### 标签模块 (5)【新增】
- `GET /api/v1/tags` - 查询标签列表
- `POST /api/v1/tags` - 创建标签
- `DELETE /api/v1/tags/{id}` - 删除标签
- `PUT /api/v1/documents/{id}/tags` - 绑定文档标签
- `GET /api/v1/documents/{id}/tags` - 查询文档标签

### 审计模块 (3)【增强】
- `GET /api/v1/audit/logs` - 查询审计日志
- `GET /api/v1/audit/logs/{id}` - 查询审计详情
- `GET /api/v1/audit/timeline` - 查询业务对象或审批实例时间线

### 通知模块 (4)【新增】
- `GET /api/v1/notifications` - 查询我的通知
- `GET /api/v1/notifications/unread-count` - 查询未读数
- `PATCH /api/v1/notifications/{id}/read` - 标记单条已读
- `PATCH /api/v1/notifications/read-all` - 全部标记已读

### 项目模块 (10)【新增】
- `POST /api/v1/projects` - 创建项目
- `PUT /api/v1/projects/{id}` - 更新项目
- `GET /api/v1/projects/{id}` - 查询项目详情
- `GET /api/v1/projects/{id}/workbench` - 查询项目工作台详情
- `GET /api/v1/projects` - 查询项目列表
- `POST /api/v1/projects/{id}/members` - 添加项目成员
- `DELETE /api/v1/projects/{id}/members/{userId}` - 移除项目成员
- `PATCH /api/v1/projects/{id}/stage` - 修改项目阶段
- `PATCH /api/v1/projects/{id}/status` - 修改项目状态
- `GET /api/v1/projects/{id}/archive` - 查询项目归档详情

### 清单模板模块 (8)【新增】
- `POST /api/v1/checklist-templates` - 创建模板
- `PUT /api/v1/checklist-templates/{id}` - 更新模板
- `GET /api/v1/checklist-templates/{id}` - 查询模板详情
- `GET /api/v1/checklist-templates` - 查询模板列表
- `DELETE /api/v1/checklist-templates/{id}` - 删除模板
- `POST /api/v1/checklist-templates/{id}/items` - 新增模板清单项
- `PUT /api/v1/checklist-templates/{id}/items/{itemId}` - 更新模板清单项
- `DELETE /api/v1/checklist-templates/{id}/items/{itemId}` - 删除模板清单项

### 项目资料清单模块 (5)【新增】
- `POST /api/v1/projects/{projectId}/checklist/generate` - 根据模板生成清单
- `GET /api/v1/projects/{projectId}/checklist` - 查询项目清单
- `PATCH /api/v1/projects/{projectId}/checklist/items/{itemId}/owner` - 更新清单项负责人
- `POST /api/v1/projects/{projectId}/checklist/items/{itemId}/documents` - 绑定资料到清单项
- `DELETE /api/v1/projects/{projectId}/checklist/items/{itemId}/documents/{documentId}` - 解绑清单项资料

### 审批模块 (12)【新增】
- `POST /api/v1/documents/{id}/approval/submit` - 提交资料审批
- `POST /api/v1/documents/{id}/versions/{versionNo}/approval/submit` - 提交资料版本审批
- `POST /api/v1/projects/{projectId}/checklist/items/{itemId}/approval/submit` - 提交清单项审批
- `GET /api/v1/approvals/tasks` - 查询我的审批任务
- `POST /api/v1/approvals/{id}/approve` - 审批通过
- `POST /api/v1/approvals/{id}/reject` - 审批拒绝
- `POST /api/v1/approvals/{id}/withdraw` - 撤回审批
- `POST /api/v1/approvals/tasks/{taskId}/transfer` - 转交审批任务
- `POST /api/v1/approvals/tasks/{taskId}/add-sign` - 加签审批任务
- `POST /api/v1/approvals/{id}/terminate` - 终止审批
- `GET /api/v1/documents/{id}/approval/history` - 查询资料审批历史
- `GET /api/v1/projects/{projectId}/approval/history` - 查询项目审批历史

### 审批流程定义模块 (9)【新增】
- `POST /api/v1/workflow/definitions` - 创建流程定义
- `PUT /api/v1/workflow/definitions/{id}` - 更新流程定义
- `GET /api/v1/workflow/definitions/{id}` - 查询流程定义详情
- `GET /api/v1/workflow/definitions` - 查询流程定义列表
- `POST /api/v1/workflow/definitions/{id}/enable` - 启用流程定义
- `POST /api/v1/workflow/definitions/{id}/disable` - 停用流程定义
- `POST /api/v1/workflow/definitions/{id}/nodes` - 新增流程节点
- `PUT /api/v1/workflow/definitions/{id}/nodes/{nodeId}` - 更新流程节点
- `DELETE /api/v1/workflow/definitions/{id}/nodes/{nodeId}` - 删除流程节点

**总计**: 120 个接口

---

**文档维护**: 本文档由后端团队维护，如有疑问请联系后端开发人员。  
**在线文档**: 启动项目后访问 `http://localhost:8080/doc.html` 查看 Knife4j 在线文档。
