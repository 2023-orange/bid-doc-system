# folder 模块详细需求与设计文档（优化版 V3）

> 版本：V3  
> 状态：可进入接口文档设计前评审  
> 适用阶段：MVP 开发阶段  
> 说明：本版在原文基础上，已结合当前项目真实结构、你补充的约束以及 MVP 边界进行收敛与优化。

---

## 1. 文档目标与适用范围

### 1.1 文档目标
本文面向当前 `bid-doc-system` 单体 Spring Boot 项目，为 `folder` 主模块输出一份可直接用于后续接口文档编写、数据库设计和开发排期的详细需求与设计文档。

本文重点解决以下问题：

1. 在当前只有 `auth` 模块已有业务实现、其他模块仍为空壳的前提下，如何让 `folder` 模块独立落地。
2. 如何复用已有 `auth`、`common`、Sa-Token、MyBatis-Plus、PostgreSQL、Redis 等基础能力。
3. 如何将原文档中对 `file / log / search / upload / tag` 等未来模块的依赖，收敛为当前项目可执行的 MVP 方案。
4. 如何为后续 `document` 模块落地预留清晰边界，但不阻塞当前 `folder` 模块开发。

### 1.2 适用范围
本文适用于以下角色：

- 产品经理：评审 folder 模块的业务范围、规则边界和迭代拆分。
- Java 后端工程师：依据本文完成表设计、接口设计和代码实现。
- 前端工程师：依据本文完成树结构、授权、收藏、日志查询等页面交互设计。
- 测试工程师：依据本文编写功能、权限、异常流和边界测试用例。
- 架构负责人：依据本文确认与 `auth`、`common`、`document`、`audit` 等模块的边界关系。

### 1.3 本期范围（MVP）
本期 `folder` 模块纳入实现范围的能力如下：

1. 根级文件夹管理
2. 新建文件夹
3. 编辑文件夹
4. 重命名
5. 删除 / 批量删除
6. 移动
7. 复制
8. 查看文件夹树
9. 查看文件夹详情
10. 文件夹授权
11. 文件夹管理员设置
12. 子文件夹权限继承控制
13. 文件夹收藏 / 取消收藏 / 我的收藏
14. 文件夹搜索 / 条件筛选（仅 folder 自身维度）
15. 文件夹操作日志查询（通过 `audit` 模块承载）

### 1.4 明确不在本期范围内
以下能力不属于本期 MVP，不应在接口、数据表和实现中隐式带入：

1. 文件内容上传、下载、在线创建、在线编辑
2. 文件列表导出
3. 标签体系与标签批量导入
4. 自动规则生成文件夹
5. 第三方导入文件夹
6. 关注 / 订阅通知
7. 审批流程落地
8. 多文件夹引用
9. 文件扩展属性
10. 回收站 / 恢复机制

### 1.5 与其他模块的边界收敛原则
由于当前项目中除 `auth` 外其他业务模块均未真正落地，因此原文档中提到的未来模块依赖必须按以下方式收敛：

| 原文提法 | 优化后建议 | 说明 |
|---|---|---|
| `file` 模块 | 统一替换为 `document` 模块 | 当前包结构已有 `document`，建议未来文件/文档能力统一沉淀在 `document` 模块，避免后续命名割裂 |
| `log` 模块 | 统一替换为 `audit` 模块 | 当前已有 `audit` 包位，且你已明确业务操作日志独立，故建议使用 `audit` 作为正式日志模块名 |
| `search` 模块 | 本期不单独建模块，先内聚在 `folder` 查询能力中 | MVP 阶段无需独立搜索模块，folder 自身的条件检索先由数据库查询实现 |
| `upload` 模块 | 不单独建模块，后续作为 `document` 子域能力 | 文件上传下载本质属于 `document` 模块能力，不建议提前拆出独立模块 |
| `tag` 模块 | 本期彻底移出依赖 | 既然标签不在本期，就不应继续在主文档里作为依赖模块出现 |
| `workflow` 模块 | 保留远期扩展点，不纳入本期设计依赖 | 当前包存在但为空，可在后续审批能力落地时接入 |
| `notify` 模块 | 保留远期扩展点，不纳入本期设计依赖 | 收藏、授权、审批等通知提醒可未来再接 |

---

## 2. 现有系统现状分析

### 2.1 项目结构现状
当前项目为单体 Spring Boot 工程，主包下存在如下业务目录：

- `audit`
- `auth`
- `common`
- `document`
- `folder`
- `notify`
- `workflow`

当前已确认：

1. 只有 `auth` 模块已有实际业务实现。
2. `common` 承载公共基础设施。
3. `audit`、`document`、`folder`、`notify`、`workflow` 当前属于已预留但尚未落地的模块。
4. 因此，`folder` 设计必须避免依赖“尚不存在的成熟业务模块”。

### 2.2 当前已可复用能力
可直接复用的现有能力主要来自 `auth` 与 `common`：

#### 2.2.1 auth 模块
- 用户、角色、部门基础模型
- 登录、登出、当前用户获取
- 用户角色分配
- `RoleCodeEnum`
- Sa-Token 登录态维护
- `UserContext` 当前用户上下文

#### 2.2.2 common 模块
- `ApiResponse<T>` 统一响应体
- `BusinessException` / `ErrorCode` / `GlobalExceptionHandler`
- 登录拦截与 traceId 透传
- `MyMetaObjectHandler` 审计字段自动填充
- MyBatis-Plus 基础能力
- 逻辑删除统一配置
- slf4j + MDC 日志链路能力

### 2.3 当前尚不存在但被原文错误前置依赖的能力
原文中对以下能力的描述过早、过重，需调整为“预留边界”而非“当前依赖”：

1. `document/file` 模块的文件清单、上传下载、在线创建、外链等能力
2. `tag` 模块的标签体系能力
3. `search` 独立模块的全文检索能力
4. `upload` 独立模块的上传子系统
5. `log` 独立模块的统一审计实现

### 2.4 已确认技术约束
结合你补充的信息，以下事项已从“待确认”变为“已确认”：

1. 数据范围控制方案：采用 **注解 + AOP**
2. 菜单/按钮权限编码体系：**MVP 阶段暂不引入**
3. 业务操作日志：**独立模块，建议统一归入 `audit`**
4. 部署方式：**当前仅本地运行**
5. `document` 模块将在 `folder` 模块完成后落地
6. 删除策略：**业务语义上为直接删除，不做回收站**
7. 根模型：**业务上允许多个根级文件夹，不设置可授权的单一真实根节点**
8. 文件归属：**未来 document 必须强绑定唯一主归属 folder**

### 2.5 当前文档需要修正的核心问题
相较原稿，当前最需要修正的地方有四个：

1. **模块命名与真实项目结构不一致**
   - 应使用 `document` 替代 `file`
   - 应使用 `audit` 替代 `log`

2. **MVP 边界不够收敛**
   - 原稿仍保留了若干跨模块能力的描述，例如文件导出、搜索模块、上传模块等
   - 这些能力若继续保留，会让后续接口设计和开发排期失真

3. **日志模型不应继续做成 folder 私有最终方案**
   - 你已确认业务日志独立，因此 folder 只应定义日志事件，不应最终固化为 `folder_operation_log` 私有终态
   - 更合理的做法是由 `audit` 模块提供统一的审计记录表和查询服务

4. **分页、缓存、数据范围仍需落成统一规范**
   - 如果不现在定下来，后续 `folder` 和 `document` 两个模块很容易各写一套

---

## 3. 模块定位与系统协同关系

### 3.1 folder 模块定位
`folder` 模块是当前系统的主模块，承担两类核心职责：

1. **目录组织中心**
   - 管理由多个根级文件夹组成的目录结构（前端可按树形方式展示）
   - 为后续文档归档提供容器
   - 提供目录层级的搜索、筛选、收藏等基础能力

2. **资源权限挂载中心**
   - 授权对象来自 `auth`
   - 权限继承与覆盖发生在 folder 层
   - 后续 `document` 模块将以 folder 作为默认权限边界之一

### 3.2 MVP 阶段的模块协作关系
本期建议采用如下协作模型：

- `auth`
  - 提供用户、角色、部门、登录态与角色信息
- `common`
  - 提供响应体、异常、审计字段、拦截器、日志链路
- `folder`
  - 提供树结构、资源授权、管理员、收藏、目录查询
- `audit`
  - 提供业务操作日志的落表与查询
- `document`
  - 未来接入，负责“文档”对象，不阻塞当前 folder

### 3.3 本期不建议作为真实依赖的模块
以下模块在当前版本中只保留扩展点，不纳入 folder 的当前依赖图：

- `notify`
- `workflow`

---

## 4. 业务目标与设计原则

### 4.1 业务目标
1. 以最小可行方案快速落地 folder 主模块
2. 确保 folder 的表结构、接口边界和权限模型能够支撑后续 `document` 模块接入
3. 不为“未来可能有”的复杂能力做提前过度设计
4. 优先保证目录树、授权、审计、收藏的稳定落地

### 4.2 设计原则
#### 4.2.1 最小闭环原则
本期只实现 folder 自身可以闭环的能力，不依赖 document/tag/upload/search/workflow/notify。

#### 4.2.2 统一命名原则
为避免后续重构，命名从现在开始统一：

- 业务“文件/文档”统一叫 `document`
- 审计日志统一叫 `audit`
- folder 表、DTO、VO、接口都采用 `folder` 域命名

#### 4.2.3 兼容现有项目风格原则
- Java17 + Spring Boot 3
- MyBatis-Plus
- PostgreSQL
- Redis
- Sa-Token
- MVC 分层
- `ApiResponse` / `PageResponse`
- 逻辑删除
- 审计字段统一自动填充

#### 4.2.4 风险前置原则
对以下高风险点必须在设计阶段明确：

- 子树移动
- 子树复制
- 同级重名
- 权限继承
- 数据范围
- 缓存一致性
- 审计追踪

---

## 5. 迭代范围拆分建议

### 5.1 R1：folder MVP（建议立即实现）
建议将当前可真正落地的能力收敛到以下范围：

1. 根级文件夹管理
2. 文件夹树查询
3. 文件夹 CRUD
4. 移动 / 复制
5. 文件夹授权
6. 文件夹管理员
7. 权限继承开关
8. 收藏
9. 文件夹条件检索
10. 业务审计日志查询

### 5.2 R2：document 接入后再开放
以下能力依赖 `document` 模块，不建议放进本期 folder 接口清单：

1. 文件清单查询
2. 文件清单导出
3. 文件上传/下载
4. 文件归档到 folder
5. 删除 folder 前校验是否存在 document
6. 文件级联权限

### 5.3 R3：后续扩展能力
1. 通知提醒
2. 审批流
3. 自动建目录
4. 标签体系
5. 高级搜索

---

## 6. 核心业务对象建模（优化版）

### 6.1 Folder
#### 6.1.1 含义
表示系统中的目录节点，业务上允许存在多个根级文件夹；前端展示时可视为一棵目录树。

#### 6.1.2 建议字段
| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| parentId | bigint | 父节点 ID；根级文件夹固定为 `0` |
| name | varchar(128) | 文件夹名称 |
| ancestorIds | varchar(1024) | 祖先链，建议格式如 `1,1001,1002` |
| level | int | 层级深度，根级文件夹为 0 |
| sortNo | int | 排序号 |
| ownerDeptId | bigint | 所属部门 ID；根级文件夹可为空，普通目录建议必填 |
| ownerUserId | bigint | 负责人 / 创建人；复制目录时建议写入当前操作人 |
| inheritPermission | boolean | 是否继承父级权限 |
| status | smallint | 可选，预留启用状态 |
| remark | varchar(512) | 备注 |
| createdAt | timestamp | 创建时间 |
| createdBy | bigint | 创建人 |
| updatedAt | timestamp | 更新时间 |
| updatedBy | bigint | 更新人 |
| deleted | boolean | 逻辑删除 |

#### 6.1.3 为什么推荐 `ancestorIds`
相较原稿中的“纯邻接表 + 可选 path”，这里更建议采用 **`parentId + ancestorIds + level` 的轻量混合树模型**：

1. 不需要引入 PostgreSQL `ltree`
2. 比纯 `parentId` 更方便做：
   - 子树查询
   - 防循环校验
   - 移动后的批量更新
   - 路径展示
3. 更适合 MyBatis-Plus + PostgreSQL 的当前技术栈
4. 实现复杂度显著低于闭包表

#### 6.1.4 owner 字段建议
- 普通目录：`ownerDeptId` 建议必填，用于数据范围和责任归属
- 根级文件夹：`ownerDeptId` 可为空，默认不参与普通数据范围裁剪
- 复制目录：将复制后的子树视为新资源，建议统一按“目标父目录归属 + 当前操作人”重置 owner 字段，而不是保留源目录 owner

### 6.2 FolderGrant
#### 6.2.1 含义
记录某个授权主体对某个 folder 的显式授权。

#### 6.2.2 建议字段
| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| folderId | bigint | 目标文件夹 |
| subjectType | varchar(32) | USER / ROLE / DEPT |
| subjectId | bigint / varchar | 主体 ID，角色可用 roleId 或 roleCode |
| permissionCode | varchar(64) | 权限点编码 |
| grantScope | varchar(32) | SELF / SELF_AND_DESCENDANTS |
| effectiveFrom | timestamp | 生效开始时间 |
| effectiveTo | timestamp | 生效结束时间 |
| createdAt | timestamp | 创建时间 |
| createdBy | bigint | 创建人 |
| updatedAt | timestamp | 更新时间 |
| updatedBy | bigint | 更新人 |
| deleted | boolean | 逻辑删除 |

#### 6.2.3 说明
MVP 虽然不引入菜单/按钮权限编码体系，但 **folder 资源权限点** 仍然需要存在，只是它们作为业务枚举/常量使用，而不是菜单权限系统。

### 6.3 FolderManager
#### 6.3.1 含义
给某个用户授予 folder 管理身份。

#### 6.3.2 建议字段
| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| folderId | bigint | 文件夹 ID |
| userId | bigint | 管理员用户 ID |
| manageScope | varchar(32) | SELF / SELF_AND_DESCENDANTS |
| createdAt | timestamp | 创建时间 |
| createdBy | bigint | 创建人 |
| updatedAt | timestamp | 更新时间 |
| updatedBy | bigint | 更新人 |
| deleted | boolean | 逻辑删除 |

### 6.4 FolderFavorite
#### 6.4.1 含义
记录用户收藏夹关系。

#### 6.4.2 建议字段
| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| folderId | bigint | 文件夹 ID |
| userId | bigint | 用户 ID |
| createdAt | timestamp | 收藏时间 |
| createdBy | bigint | 创建人 |
| deleted | boolean | 逻辑删除 |

### 6.5 AuditOperationLog（替代 FolderOperationLog）
#### 6.5.1 优化建议
原稿中的 `FolderOperationLog` 更适合作为 **事件模型**，不适合作为最终表结构定死在 folder 模块里。

你已明确“业务操作日志独立”，因此建议改成：

- folder 只定义“需要记录哪些事件”
- 由 `audit` 模块提供统一表和查询服务

#### 6.5.2 audit 表建议（通用化）
建议在 `audit` 模块中建设通用审计表，例如：

`audit_operation_log`

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint | 主键 |
| moduleCode | varchar(32) | 模块编码，如 FOLDER |
| bizType | varchar(32) | 业务类型，如 FOLDER |
| bizId | bigint | 业务主键 |
| operationType | varchar(32) | CREATE / UPDATE / MOVE / DELETE / GRANT 等 |
| operatorUserId | bigint | 操作人 |
| operatorDeptId | bigint | 操作人部门 |
| requestId | varchar(64) | traceId |
| operationTime | timestamp | 操作时间 |
| beforeData | jsonb | 变更前 |
| afterData | jsonb | 变更后 |
| extraData | jsonb | 扩展信息 |
| createdAt | timestamp | 创建时间 |
| createdBy | bigint | 创建人 |
| deleted | boolean | 逻辑删除 |

#### 6.5.3 好处
1. 避免 folder 以后再向 log/audit 模块迁移数据
2. `document`、`workflow` 等未来模块可直接复用
3. 管理端查询模型统一

### 6.6 FutureDocumentRelation（仅接口契约，不建表）
为未来 `document` 模块预留约束：

1. `document.folderId` 必填
2. 一个 document 只能有一个主 folder
3. folder 删除前，未来应支持由 document 模块校验是否存在归属 document

---

## 7. 文件夹树设计

### 7.1 根级文件夹设计
#### 7.1.1 建议
系统**不落库单一真实根节点**，而是采用“多个根级文件夹 + `parentId = 0`”的设计。

也就是说：

- 业务上允许存在多个根级文件夹
- 根级文件夹就是树的第一层真实业务节点
- 前端可在展示层把这些根级文件夹挂在一个“虚拟根”下展示，但数据库中不存该虚拟根

#### 7.1.2 实现建议
根级文件夹采用以下约定：

- `parentId = 0`
- `level = 0`
- `ancestorIds = 自身 id`
- 根级文件夹可由 `SUPER_ADMIN` 创建
- 根级文件夹**不允许做资源授权**
- 根级文件夹的删除、重命名、移动、权限配置，仅 `SUPER_ADMIN` 可操作

#### 7.1.3 原因
1. 与你“存在多个根文件夹”的业务约束一致
2. 避免前一版“单一真实根节点”与实际需求冲突
3. 实现上简单，MyBatis-Plus 和 PostgreSQL 都容易处理
4. 未来若前端需要统一树入口，只需在展示层补一个虚拟根，不影响数据库结构

#### 7.1.4 根级文件夹的可见性建议
由于根级文件夹不做资源授权，建议采用以下可见性规则：

1. `SUPER_ADMIN` 可查看全部根级文件夹
2. 普通用户只要对某个根级文件夹下的任一后代目录有查看权限，则允许看到该根级文件夹作为路径容器节点
3. 非 `SUPER_ADMIN` 不允许直接修改、删除、移动根级文件夹

### 7.2 层级深度
理论上允许多层级，但建议在业务上限制最大层级。

#### 7.2.1 建议值
建议最大层级：**8 级**

#### 7.2.2 原因
1. 防止树过深导致 UI 和 SQL 都复杂
2. 满足中小型文档系统足够使用
3. 降低移动、复制、删除的性能风险

### 7.3 同级唯一规则
同一个 `parentId` 下不允许出现两个未删除的同名文件夹。

#### 7.3.1 PostgreSQL 建议索引
```sql
create unique index uk_doc_folder_parent_name_active
on doc_folder(parent_id, name)
where deleted = false;
```

### 7.4 路径展示规则
推荐显示路径时动态拼接，不在表中维护中文全路径字段。

展示时可根据 `ancestorIds` 查询祖先节点名称并拼装：

`根级文件夹 / 二级目录 / 三级目录 / 当前目录`

### 7.5 防循环引用规则
移动时需校验：

1. 目标节点不能等于当前节点
2. 目标节点不能在当前节点的子树内

由于已维护 `ancestorIds`，该校验可以高效完成。

### 7.6 移动规则
#### 7.6.1 主规则
移动时必须同时满足：

1. 当前用户对源节点具备 `FOLDER_MOVE`
2. 当前用户对目标父节点具备 `FOLDER_CREATE`
3. 目标节点不在源节点子树内
4. 目标父节点下无同名文件夹
5. 移动后层级不超过最大层级

#### 7.6.2 数据更新
移动时需要更新：

- 当前节点的 `parentId`
- 当前节点的 `ancestorIds`
- 当前节点的 `level`
- 全部后代节点的 `ancestorIds`
- 全部后代节点的 `level`

### 7.7 复制规则
#### 7.7.1 复制范围
复制当前节点及其全部子树结构。

#### 7.7.2 默认不复制的内容
为降低误授权风险，建议复制时默认 **不复制以下内容**：

1. FolderGrant
2. FolderManager
3. FolderFavorite

#### 7.7.3 名称冲突处理
推荐默认自动追加后缀：

- `名称(复制)`
- 若冲突继续追加流水后缀

### 7.8 删除规则
#### 7.8.1 用户语义
业务上属于“直接删除”，即：

- 删除后用户不可恢复
- 不提供回收站

#### 7.8.2 技术实现建议
尽管业务上是直接删除，仍建议 **数据库层面使用逻辑删除**，原因如下：

1. 与现有项目风格一致
2. 有利于审计追踪
3. 有利于后续误删排查
4. 不等于提供回收站功能

#### 7.8.3 删除范围
删除节点时同步删除其整棵子树，并清理：

- folder_grant
- folder_manager
- folder_favorite

但审计日志不删除。

---

## 8. 权限模型设计（结合你已确认的约束）

### 8.1 权限设计总原则
MVP 阶段不引入菜单/按钮权限编码体系，因此本期采用：

**角色粗粒度兜底 + folder 资源授权 + 数据范围 AOP**

### 8.2 权限来源
用户对某个 folder 的最终权限，来源于以下几层：

1. 平台角色特权（仅 `SUPER_ADMIN` 全局放行）
2. folder 管理员身份（要求用户具备 `FOLDER_ADMIN` 角色资格）
3. folder 显式授权
4. 父节点继承授权
5. 数据范围过滤结果

### 8.3 建议的角色策略
#### 8.3.1 SUPER_ADMIN
全局放行。

#### 8.3.2 FOLDER_ADMIN
`FOLDER_ADMIN` **不是全局放行角色**，而是“具备被授予目录管理资格的角色”。

建议规则：

- 只有具备 `FOLDER_ADMIN` 角色的用户，才允许被 `SUPER_ADMIN` 配置为某个目录的 `FolderManager`
- `FOLDER_ADMIN` 本身不自动拥有所有目录权限
- `FOLDER_ADMIN` 只能管理自己被授予权限的目录及其子目录
- 对根级文件夹的删除、移动、重命名、授权配置，`FOLDER_ADMIN` 一律无权操作

#### 8.3.3 DEPT_MANAGER
不建议在系统层默认赋予其 folder 管理权限。

更稳妥的方式是：

- `DEPT_MANAGER` 只参与数据范围放大
- 是否能管理某个 folder，仍以 FolderGrant / FolderManager 为准

这样可避免“部门经理天然看见所有目录”的越权风险。

### 8.4 资源权限点建议
即便本期不做菜单权限，也需要定义业务权限枚举：

- FOLDER_VIEW
- FOLDER_CREATE
- FOLDER_EDIT
- FOLDER_RENAME
- FOLDER_DELETE
- FOLDER_MOVE
- FOLDER_COPY
- FOLDER_GRANT
- FOLDER_MANAGER_SET
- FOLDER_INHERIT_SET
- FOLDER_SEARCH
- FOLDER_FAVORITE
- FOLDER_AUDIT_VIEW

### 8.5 授权主体
- USER
- ROLE
- DEPT

### 8.6 权限判定优先级
建议最终统一为：

1. `SUPER_ADMIN`
2. 根级文件夹保护规则（若为根级且非 `SUPER_ADMIN`，直接拒绝敏感操作）
3. FolderManager（仅限被 `SUPER_ADMIN` 授权且命中 scope）
4. 当前节点显式授权
5. 父节点继承授权
6. 无权限

### 8.7 权限继承规则
#### 8.7.1 基本规则
- `inheritPermission = true`：继承父级授权
- `inheritPermission = false`：不继承父级授权，仅保留自身显式授权

#### 8.7.2 推荐策略
MVP 阶段 **不做显式拒绝**，只做显式允许，避免复杂度过高。

### 8.8 有效期规则
FolderGrant 支持：

- `effectiveFrom`
- `effectiveTo`

若为空则表示长期有效。

### 8.9 数据范围控制方案（已确认）
你已确定采用 **注解 + AOP**，建议文档里将该方案写实为如下设计：

#### 8.9.1 推荐注解
`@DataScope`

#### 8.9.2 作用位置
优先作用于 Service 层的查询方法，而不是 Controller 层。

#### 8.9.3 推荐能力
- 基于当前用户角色判断是否需要数据范围过滤
- 注入当前用户部门范围
- 生成可访问部门 ID 集合
- 结合 folder 的 `ownerDeptId`、`createdBy`、授权结果进行查询裁剪

#### 8.9.4 建议落地方式
不要让 AOP 直接拼 SQL，建议采用：

1. AOP 解析权限与部门范围
2. 生成 `DataScopeContext`
3. Service / Mapper 在构造查询时读取上下文并拼接条件

这样更可控，也更利于排查问题。

### 8.10 与 Sa-Token 的结合方式
MVP 阶段建议采用如下分层：

#### 8.10.1 Controller 层
使用 Sa-Token 负责：

- 登录校验
- 高危接口角色兜底校验，例如 `@SaCheckRole("SUPER_ADMIN")`

#### 8.10.2 Service 层
使用 `FolderPermissionService` 负责：

- folder 资源级权限校验
- 管理员校验
- 授权有效期判断
- 继承链判断

即：

- **Sa-Token 负责“你是不是 `SUPER_ADMIN` / 是否具备 `FOLDER_ADMIN` 角色资格”**
- **FolderPermissionService 负责“你对这个具体目录有没有资源权限”**

---

## 9. 功能需求清单（收敛版）

### 9.1 新建文件夹
#### 功能目标
在指定父节点下创建子文件夹。

#### 前置条件
- 已登录
- 父节点存在
- 有 `FOLDER_CREATE`
- 同级不重名
- 不超过最大层级

#### 日志要求
记录一条 `audit_operation_log`，`moduleCode = FOLDER`，`operationType = CREATE`

### 9.2 编辑文件夹
可修改：

- remark
- sortNo
- inheritPermission

不建议在“编辑接口”里混入重命名。

### 9.3 重命名
单独接口处理，便于前端交互和日志审计。

### 9.4 删除 / 批量删除
#### 建议
批量删除时要先做“节点去重”，避免父子同时传入导致重复删除。

当前阶段**不设业务数量上限**，但实现上应满足：

- 先做父子节点去重
- 按事务处理当前批次
- 对超大批量场景预留后续分批或异步化空间

### 9.5 移动
重点审计：

- 旧父节点
- 新父节点
- 旧祖先链
- 新祖先链

### 9.6 复制
默认复制结构，不复制授权、不复制管理员、不复制收藏。

### 9.7 查看文件夹树
#### 建议
本期提供两种模式：

1. `loadRootTree`：返回当前用户可见的根级文件夹列表及首层结构
2. `loadChildren(parentId)`：懒加载指定父目录的直接子节点

不要一开始只做全量树，避免后续前端树控件卡顿。

### 9.8 查看文件夹详情
返回：

- 基础信息
- 当前用户权限集合
- 是否收藏
- 管理员信息摘要
- 授权摘要数量

### 9.9 文件夹授权
建议支持：

- 新增授权
- 取消授权
- 查询授权列表

先不建议一口气做“批量复杂编排式授权”。

### 9.10 文件夹管理员设置
建议支持：

- 添加管理员
- 取消管理员
- 查询管理员列表

### 9.11 权限继承控制
建议只对单节点配置 `inheritPermission`，不要做复杂的整棵树批量继承重算接口。

### 9.12 收藏
建议支持：

- 收藏
- 取消收藏
- 我的收藏列表

### 9.13 目录检索 / 条件筛选
这里的“搜索”**不是全文搜索，也不是 document 内容搜索**，而是 folder 模块自己的目录检索能力。

本期建议仅支持 folder 自身维度：

- 名称模糊匹配
- 所属部门
- 创建人
- 创建时间区间
- 父节点 ID
- 根级文件夹 ID / 所属根级目录
- 层级
- 是否收藏（可选，建议支持 `favoriteOnly`）
- 是否只看我可访问的目录（默认是）

建议将该能力在接口和文档中统一命名为“目录检索 / 条件筛选”，避免与未来 `document` 搜索混淆。

### 9.14 操作日志查询
通过 `audit` 模块承载，但 folder 侧可暴露“按 folder 维度查询日志”的查询接口。

---

## 10. 规则设计（优化版）

### 10.1 命名规则
- 长度 1~128
- trim 后不能为空
- 不允许控制字符
- 不建议只由空格组成
- 是否禁止特殊字符，可在前端和后端双重限制

### 10.2 同级唯一
同级唯一必须由“数据库唯一索引 + 业务校验”双保险完成。

### 10.3 删除规则
- 用户视角直接删除
- 技术实现逻辑删除
- 审计保留
- 无回收站接口

### 10.4 复制规则
- 复制结构
- 不复制授权
- 不复制管理员
- 不复制收藏
- 新复制出的整棵子树视为“新资源”
- `ownerDeptId` 默认继承**目标父目录**的 `ownerDeptId`；若目标父目录为根级文件夹且其 `ownerDeptId` 为空，则回退为当前操作人的部门
- `ownerUserId` 统一写当前操作人
- 审计日志中保留来源目录 ID 与原始 owner 信息，便于追溯
- 名称自动避让

### 10.5 移动规则
- 校验源权限与目标权限
- 校验防循环
- 校验目标层级
- 校验目标同级重名

### 10.6 收藏规则
- `(folderId, userId)` 唯一
- 重复收藏幂等成功
- 重复取消幂等成功

### 10.7 搜索规则
本期搜索不做全文检索，只做结构化查询 + 名称模糊匹配。

### 10.8 日志规则
以下操作必须记审计日志：

- CREATE
- UPDATE
- RENAME
- DELETE
- BATCH_DELETE
- MOVE
- COPY
- GRANT_ADD
- GRANT_REMOVE
- MANAGER_ADD
- MANAGER_REMOVE
- FAVORITE
- UNFAVORITE
- INHERIT_SWITCH

---

## 11. 数据库设计建议（优化版）

### 11.1 表命名建议
为与 `sys_*` 保持区别，并与未来 `document` 统一，建议使用 `doc_` 前缀：

- `doc_folder`
- `doc_folder_grant`
- `doc_folder_manager`
- `doc_folder_favorite`

日志统一由 `audit` 模块提供：

- `audit_operation_log`

### 11.2 为什么不建议表名直接叫 `folder`
1. 过于泛化
2. 不利于未来多业务域扩展
3. 与 `document` 命名体系不统一

### 11.3 doc_folder 索引建议
- `pk_doc_folder(id)`
- `idx_doc_folder_parent_id(parent_id)`
- `idx_doc_folder_owner_dept_id(owner_dept_id)`
- `idx_doc_folder_owner_user_id(owner_user_id)`
- `idx_doc_folder_level(level)`
- `idx_doc_folder_created_at(created_at)`
- `uk_doc_folder_parent_name_active(parent_id, name) where deleted = false`

### 11.4 doc_folder_grant 索引建议
- `idx_doc_folder_grant_folder_id(folder_id)`
- `idx_doc_folder_grant_subject(subject_type, subject_id)`
- `idx_doc_folder_grant_effective(effective_from, effective_to)`
- `uk_doc_folder_grant_active(folder_id, subject_type, subject_id, permission_code, deleted)`

### 11.5 doc_folder_manager 索引建议
- `idx_doc_folder_manager_folder_id(folder_id)`
- `idx_doc_folder_manager_user_id(user_id)`
- `uk_doc_folder_manager_active(folder_id, user_id, deleted)`

### 11.6 doc_folder_favorite 索引建议
- `idx_doc_folder_favorite_user_id(user_id)`
- `idx_doc_folder_favorite_folder_id(folder_id)`
- `uk_doc_folder_favorite_active(folder_id, user_id, deleted)`

### 11.7 日志表建议归属
`audit_operation_log` 不要作为 folder 私有表，否则后续 `document` 再上来时必然重复建表或再次抽象。

---

## 12. 缓存与性能设计建议（结合当前 Redis 现状）

### 12.1 总原则
由于当前 Redis 主要用于 Sa-Token，且系统还在开发期，建议对业务缓存采取 **保守启用、有限范围** 的策略。

### 12.2 本期建议缓存什么
只建议缓存两类内容：

1. **目录子节点列表**
   - 例如某个 `parentId` 的直接子节点
2. **用户对 folder 的权限结果**
   - 例如某用户对某 folder 的可操作权限集合

### 12.3 本期不建议缓存什么
1. 全量目录树
2. 搜索结果列表
3. 审计日志查询结果
4. folder 详情整对象
5. 批量授权结果

### 12.4 Key 命名规范建议
建议从一开始就带上应用前缀和环境前缀：

`biddoc:{profile}:{module}:{biz}:{key}`

示例：

- `biddoc:dev:folder:children:1001`
- `biddoc:dev:folder:perm:20001:1001`
- `biddoc:dev:folder:favorite:20001`

如果你当前 Redis 只给本系统使用，也可以简化为：

- `biddoc:folder:children:{parentId}`
- `biddoc:folder:perm:{userId}:{folderId}`

### 12.5 过期策略建议
#### 12.5.1 子节点列表缓存
TTL：**10 分钟**

原因：
- 读取频率高
- 目录变更相对少
- 允许短暂缓存

#### 12.5.2 权限结果缓存
TTL：**5 分钟**

原因：
- 授权准确性更重要
- 失效窗口需要更短

#### 12.5.3 收藏列表缓存（可选）
TTL：**5 分钟**

#### 12.5.4 审计日志
不缓存。

### 12.6 失效策略建议
以下场景必须主动删缓存：

#### 12.6.1 目录树相关
发生以下操作时，删除：

- 当前节点父级的 children 缓存
- 原父节点 children 缓存
- 新父节点 children 缓存
- 当前节点自身相关缓存

涉及操作：

- create
- delete
- batch delete
- move
- copy
- rename（若树展示中显示名称）

#### 12.6.2 权限相关
发生以下操作时，删除权限缓存：

- grant add/remove/update
- manager add/remove
- inheritPermission 变更
- folder move
- auth 中角色关系变化（后续联动）

### 12.7 容量约束建议
本期建议明确以下缓存边界：

1. 单 key 不缓存大于 **256KB** 的大对象
2. 不缓存全量大树
3. 不对搜索分页结果做组合 key 缓存
4. Redis 故障时，folder 功能允许回落数据库，不应导致业务不可用

### 12.8 代码层建议
不要在业务代码中到处直接使用 `RedisTemplate`，建议封装：

- `FolderCacheService`

统一负责：

- key 生成
- TTL
- 序列化
- 失效

---

## 13. 分页接口统一约定建议

这是当前必须尽快拍板的公共规范。建议如下。

### 13.1 请求参数建议
统一使用：

- `pageNum`：页码，从 1 开始
- `pageSize`：每页大小
- `sortBy`：排序字段，可选
- `sortOrder`：`asc` / `desc`，可选

### 13.2 为什么推荐 `pageNum/pageSize`
相较 `page/size`，`pageNum/pageSize` 在 Java 后端里更直观，且更贴近 MyBatis-Plus `Page<>(pageNum, pageSize)` 的使用习惯。

### 13.3 返回结构建议
列表分页接口统一强制使用 `PageResponse<T>`，并通过现有统一响应体返回，即 `ApiResponse<PageResponse<T>>`。

建议结构至少包含：

- `list`
- `pageNum`
- `pageSize`
- `total`
- `totalPages`
- `hasNext`

### 13.4 不使用分页的例外场景
以下接口不应强制包 `PageResponse`：

1. 树查询
2. 下拉枚举
3. 详情查询
4. 简单统计

### 13.5 公共基类建议
建议在 `common` 中定义：

- `PageQuery`

字段：

- `pageNum = 1`
- `pageSize = 20`
- `sortBy`
- `sortOrder`

并统一做参数校验：

- `pageNum >= 1`
- `1 <= pageSize <= 100`（MVP 建议上限 100）

这样后续 `folder` 与 `document` 都能复用。

---

## 14. 接口设计前置说明

### 14.1 Controller 划分建议
建议按职责拆分为：

- `FolderController`
- `FolderGrantController`
- `FolderManagerController`
- `FolderFavoriteController`
- `FolderAuditController`

### 14.2 不建议一开始拆得过细
本期模块刚起步，不建议过度拆 controller。若团队规模较小，也可以先收敛为：

- `FolderController`
- `FolderAdminController`
- `FolderAuditController`

### 14.3 DTO 建议
- `FolderCreateRequest`
- `FolderUpdateRequest`
- `FolderRenameRequest`
- `FolderMoveRequest`
- `FolderCopyRequest`
- `FolderDeleteRequest`
- `FolderGrantSaveRequest`
- `FolderManagerSaveRequest`
- `FolderSearchRequest`

### 14.4 VO 建议
- `FolderTreeNodeVO`
- `FolderDetailVO`
- `FolderGrantVO`
- `FolderManagerVO`
- `FolderFavoriteVO`
- `FolderPermissionVO`
- `AuditLogVO`

### 14.5 树查询返回建议
建议树节点至少包含：

- `id`
- `parentId`
- `name`
- `level`
- `hasChildren`
- `favorite`
- `permissionCodes`（可选）

---

## 15. MVC 落地建议

### 15.1 包结构建议
建议在 `com.example.biddoc.folder` 下建立：

- `controller`
- `service`
- `service.impl`
- `mapper`
- `entity`
- `dto`
- `vo`
- `convertor`
- `enums`
- `constant`

### 15.2 Service 建议拆分
为避免后续一个 Service 过大，建议从一开始做职责拆分：

- `FolderService`
- `FolderGrantService`
- `FolderManagerService`
- `FolderFavoriteService`
- `FolderPermissionService`

### 15.3 权限校验入口建议
统一沉淀在：

- `FolderPermissionService`

不要在每个业务 Service 里各写一套权限判断。

### 15.4 数据范围入口建议
统一通过：

- `@DataScope`
- `DataScopeAspect`
- `DataScopeContext`

### 15.5 审计日志写入入口建议
建议由 `audit` 模块提供：

- `AuditService.record(...)`

folder 只调用，不自行直接拼 audit 表写入逻辑。

---

## 16. 非功能需求与运行约束

### 16.1 当前运行方式
当前明确为本地运行，因此文档应改成：

- 当前运行模式：本地 `application-dev.yml`
- Docker / K8s 仅作为未来部署预留，不纳入本期交付要求

### 16.2 配置约束建议
建议保留：

- `application.yml`：公共配置
- `application-dev.yml`：本地开发
- `application-prod.yml`：未来预留

敏感配置建议逐步外置为环境变量：

- 数据库密码
- Redis 密码
- JWT 密钥

### 16.3 日志分层建议
必须明确区分两类日志：

#### 16.3.1 系统运行日志
- 控制台 / logback 输出
- 用于排查接口错误、性能问题、异常堆栈

#### 16.3.2 业务审计日志
- 落在 `audit` 表
- 供管理员 / 经理查看“谁在什么时候做了什么”

这两类日志不要混为一谈。

### 16.4 性能目标建议
针对当前规模，建议：

- 普通 CRUD 接口 P95 < 300ms
- 单层 children 查询 P95 < 200ms
- 授权变更接口 P95 < 500ms
- 大批量删除 / 复制当前不设业务硬限制，但实现上要支持分批处理，后续必要时异步化

---

## 17. 风险、约束、禁止事项

### 17.1 当前最重要的风险
1. folder 作为主模块，若不收紧范围，很容易因等待 document/audit/search 等模块成熟而阻塞
2. 若继续沿用原稿中“folder 私有日志 + future log 模块”双重叙述，后续一定重复建设
3. 若不现在统一分页规范，`folder` 和 `document` 会出现两套分页参数和返回结构
4. 若直接让 `DEPT_MANAGER` 天然拥有大量目录权限，后续很容易出现越权问题
5. 若上来就缓存全量树，会增加开发和排障复杂度，不适合当前阶段

### 17.2 禁止事项
1. 禁止继续在主文档中把 `file` 作为正式模块名，统一使用 `document`
2. 禁止继续把 `log` 写成未来独立模块名，统一使用 `audit`
3. 禁止在本期接口设计中带入上传、下载、文件导出、标签、审批等能力
4. 禁止把“业务直接删除”理解成“数据库必须物理删除”
5. 禁止在 Controller 层直接写 folder 资源权限计算逻辑
6. 禁止直接在各业务类零散使用 Redis，必须统一封装缓存服务
7. 禁止在没有 document 模块前，承诺“文件清单导出”等依赖文档实体的能力

---

## 18. 本版已落定事项与剩余待确认事项

### 18.1 已落定事项
1. 数据范围：注解 + AOP
2. 菜单/按钮权限：MVP 暂不引入
3. 业务操作日志：独立，建议归 `audit`
4. 部署：当前本地运行
5. `document` 模块在 `folder` 后落地
6. 文件夹删除：业务语义直接删除
7. 根模型：多个根级文件夹，数据库不落单一真实根节点
8. 文件主归属：未来唯一 folder 归属

### 18.2 本轮已补充落定事项
1. 根级文件夹不允许授权
2. 根级文件夹的删除、修改、移动等敏感操作仅 `SUPER_ADMIN` 可执行
3. 系统存在多个根级文件夹，数据库不落单一真实根节点
4. `FOLDER_ADMIN` 不是全局放行角色，只能管理被授予权限的目录及其子目录
5. 文件夹管理员的目录管理权限由 `SUPER_ADMIN` 授予
6. 复制目录时：
   - `ownerDeptId` 默认继承目标父目录；目标父目录无所属部门时回退为当前操作人部门
   - `ownerUserId` 统一写当前操作人
7. 目录检索指 folder 自身维度查询，不包含 document 内容搜索
8. 批量删除当前不设业务数量上限，但实现必须支持节点去重和后续分批扩展
9. 审计日志查询支持按“操作人部门”筛选

### 18.3 仍建议继续确认的 3 个点
1. 根级文件夹是否允许被普通用户收藏
   - 我当前建议：不允许，避免根层收藏失真；仅普通目录支持收藏

2. 根级文件夹是否需要展示给所有登录用户
   - 我当前建议：仅当用户对其下某个目录有可见权限时，展示该根级文件夹作为路径容器

3. 根级文件夹的 `ownerDeptId` 是否允许为空
   - 我当前建议：允许为空，由 `SUPER_ADMIN` 负责管理，不纳入普通数据范围裁剪

---

## 19. 结论

经过本次优化，folder 模块的设计目标应明确为：

1. **以 `auth + common + folder + audit` 形成当前 MVP 闭环**
2. **将原稿中对 `file/log/search/upload/tag` 的依赖全部收敛**
3. **把未来“文件”能力统一归入 `document` 模块**
4. **把未来“业务日志”能力统一归入 `audit` 模块**
5. **先把多根级目录、授权、收藏、审计、目录检索这些 folder 自己能闭环的能力做扎实**

只有这样，后续接口文档和实际开发排期才会稳定、清晰、可落地。
