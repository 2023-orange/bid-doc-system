# /documents 与 /folders 边界优化方案

> 日期：2026-06-23
>
> 范围：当前仓库为 Java 后端 Maven 项目。本文的后端现状来自当前仓库代码；前端现状来自用户实际页面走查和截图，不代表本仓库已包含前端源码。

## 1. 结论

推荐保持两个入口，但重新收敛职责：

- `/documents` 是唯一的“文档工作台 / 文档库”入口，负责找文件、筛选文件、上传文件、列表 / 网格展示、预览、下载、版本、元数据、标签、使用授权、审批、作废、恢复、删除。
- `/folders` 是“目录管理 / 文件夹管理”入口，负责文件夹树、目录属性、目录统计、所有者、权限继承、授权、管理员、收藏、快捷访问、移动、复制、删除、审计入口。
- 两者通过 `folderId` 联动：`/folders` 中点击“查看该文件夹文档”跳转到 `/documents?folderId=...`，`/documents` 根据 URL 中的 `folderId` 选中文件夹并加载文档。
- 网格 / 列表切换保留，但应属于 `/documents` 的文档展示能力；`/folders` 不再完整复刻文档浏览和操作菜单。

一句话落地口径：

> `/documents` 管文件，`/folders` 管目录；目录页面只提供目录治理和摘要，文档工作通过 `folderId` 跳转回文档工作台完成。

## 2. 当前仓库已确认事实

### 2.1 项目和模块

当前仓库是 Java 17 + Spring Boot 后端项目，根目录未发现前端工程文件。已确认存在这些后端模块：

- `document`
- `folder`
- `auth`
- `common`
- `workflow`
- `audit`
- `notify`
- `project`
- `dashboard`

本文只把当前代码、配置、构建文件作为事实来源。前端页面行为只作为产品优化背景。

### 2.2 文档模块当前能力

主要入口：

- `DocumentController`
- `DocumentService`
- `DocumentServiceImpl`

已确认文档模块提供：

- `POST /api/v1/folders/{folderId}/documents`：在指定文件夹上传文档。
- `GET /api/v1/folders/{folderId}/documents`：获取指定文件夹下文档列表。
- `GET /api/v1/documents/search`：文档搜索，支持 `folderId`、`recursive`、关键词、MIME、所有者、资料编号、资料状态、业务分类、敏感等级、部门、过期、收藏文件夹、标签、排序、分页。
- `GET /api/v1/documents/{id}`：文档详情。
- `PUT /api/v1/documents/{id}/metadata`：更新资料元数据。
- `POST /api/v1/documents/{id}/submit-approval`：提交资料审批。
- `POST /api/v1/documents/{id}/void`：作废资料。
- `POST /api/v1/documents/{id}/restore`：恢复资料。
- `DELETE /api/v1/documents/{id}`：软删除文档。
- `POST /api/v1/documents/{id}/versions`：上传新版本。
- `GET /api/v1/documents/{id}/versions`：版本列表。
- `GET /api/v1/documents/{id}/download`：下载当前版本。
- `GET /api/v1/documents/{id}/preview`：预览当前版本。
- `GET /api/v1/documents/{id}/versions/{versionNo}/download`：下载指定版本。
- `GET /api/v1/documents/{id}/versions/{versionNo}/preview`：预览指定版本。
- `GET /api/v1/documents/stats/*`：存储、类型、热门文档统计。
- `GET /api/v1/documents/{documentId}/use-grants` 及相关增删接口：资料使用授权。
- `GET /api/v1/documents/{id}/tags`、`PUT /api/v1/documents/{id}/tags`：文档标签。

实现层已确认的关键规则：

- 上传文档时校验目标文件夹存在，并调用 `FolderPermissionService.checkCreateChild(folder)`。
- 文档详情、下载、预览、版本等操作会校验文件夹 view 权限。
- 文档删除和新版本上传有所有者、管理员、超级管理员等限制。
- 文档搜索在指定 `folderId` 时校验文件夹 view 权限；递归查询时会过滤可见子目录，避免父级可见时泄露不可见子目录文档。
- 文件夹删除会调用 `DocumentService.cascadeSoftDeleteByFolderIds(...)` 级联软删除文档。

### 2.3 文件夹模块当前能力

主要入口：

- `FolderController`
- `FolderFavoriteController`
- `FolderGrantController`
- `FolderManagerController`
- `FolderService`
- `FolderInsightService`
- `FolderPermissionService`

已确认文件夹模块提供：

- `POST /api/v1/folders`：创建文件夹。
- `GET /api/v1/folders/{id}`：基础文件夹详情。
- `GET /api/v1/folders/{id}/detail`：文件夹治理详情，包含路径、统计、权限、收藏、标签、相关项目等。
- `GET /api/v1/folders/stats`：文件夹统计。
- `GET /api/v1/folders/recent`：最近访问。
- `GET /api/v1/folders/managed`：我管理的文件夹。
- `GET /api/v1/folders/owned`：我拥有的文件夹。
- `PATCH /api/v1/folders/{id}/name`：重命名。
- `PUT /api/v1/folders/{id}`：更新目录属性。
- `GET /api/v1/folders/children`：子文件夹。
- `GET /api/v1/folders/tree/root`：根树。
- `GET /api/v1/folders/{id}/permissions/me`：当前用户在目录上的权限。
- `GET /api/v1/folders/search`：搜索文件夹。
- `DELETE /api/v1/folders/{id}`：删除文件夹。
- `DELETE /api/v1/folders/batch`：批量删除。
- `PATCH /api/v1/folders/{id}/move`：移动。
- `POST /api/v1/folders/{id}/copy`：复制。
- `POST /api/v1/folders/{id}/access`：记录访问。
- `GET /api/v1/folders/{id}/tags`、`PUT /api/v1/folders/{id}/tags`：文件夹标签。
- `GET /api/v1/folders/favorites`、`POST /api/v1/folders/{folderId}/favorite`、`DELETE /api/v1/folders/{folderId}/favorite`：收藏。
- `GET /api/v1/folders/{folderId}/grants` 及相关增删接口：文件夹授权。
- `GET /api/v1/folders/{folderId}/managers` 及相关增删接口：文件夹管理员。

实现层已确认的关键规则：

- 创建非根目录时校验父目录 createChild 权限。
- 文件夹树返回前会过滤可见目录。
- 移动、删除、批量删除、复制已有层级和权限相关保护。
- 批量删除会做父子去重，避免重复处理子树。
- 文件夹详情已提供直接文档数、子树文档数、直接容量、子树容量、子目录数、访问统计、下载统计等摘要能力。

### 2.4 当前后端已有的边界重复迹象

后端整体边界比前端清楚，但仍有几处需要收敛：

- `GET /api/v1/folders/{folderId}/documents` 和 `GET /api/v1/documents/search?folderId=...` 都可以支撑“按文件夹看文档”。前者更像早期直接列表接口，后者更像当前文档工作台主查询接口。
- `DocumentListItemRespDTO` 中存在 `size`、`mimeType`、`status` 等“前端文件夹详情页使用的别名”字段，说明文件夹页面已经开始依赖文档列表 DTO 做完整文档浏览。
- `/documents/search` 的高级筛选暴露 `ownerDeptId`、`ownerUserId` 这类 ID 型字段，后端可用，但直接给业务用户不友好，需要前端选择器和后端选项数据配合。
- `DocumentController` 的 base path 是 `/api/v1`，内部混合 `/folders/{folderId}/documents` 和 `/documents/*`，接口能用，但文档合同需要明确哪个是主入口、哪个是兼容入口。

## 3. 产品边界目标

### 3.1 `/documents`：文档工作台

页面目标：

- 快速找到文件。
- 按文件夹、关键词、标签、状态、业务分类、敏感等级、归属部门、归属用户、时间范围等筛选。
- 在同一结果集上切换列表 / 网格。
- 支持上传、预览、下载、版本、元数据、标签、使用授权、审批、作废、恢复、删除。

推荐页面结构：

- 左侧：文件夹树，作为文档范围过滤器。
- 右侧顶部：当前文件夹面包屑 / 标题、搜索、筛选、上传。
- 右侧工具条：列表 / 网格切换、排序字段、排序方向、批量操作。
- 右侧主体：同一套文档结果的列表视图或网格视图。
- 右侧操作：文档卡片或行操作只出现文档行为。

网格模式建议保留并增强。截图里的网格卡片方向是对的：文件图标、文件名、大小、版本、预览、下载、版本、更多操作，非常适合投标资料库里“扫一眼识别文件”的场景。

### 3.2 `/folders`：目录管理

页面目标：

- 管理目录结构和目录属性。
- 处理目录权限、目录管理员、目录授权、继承规则。
- 查看目录层级统计、容量、文档数量、收藏、快捷入口、审计摘要。
- 对文件夹做新建、编辑、删除、移动、复制、收藏等目录行为。

推荐页面结构：

- 左侧：文件夹树，作为目录管理对象选择器。
- 顶部：文件夹总数、文档总数、存储占用、收藏数、我管理的、我拥有的等统计。
- 主区域：选中文件夹的目录属性、权限、容量、子目录、审计摘要、标签、相关项目。
- 文档摘要：最多显示“最近 5 个文档”或“文档数量 / 容量摘要”，不显示完整文档列表和完整文档操作菜单。
- 主要跳转：`查看该文件夹文档` -> `/documents?folderId={id}`。
- 上传文档：不在 `/folders` 直接完成。可以提供 `去上传文档` 按钮，跳转 `/documents?folderId={id}&upload=1`。

## 4. 推荐方案与备选方案

### 4.1 方案 A：兼容优先的边界收敛（推荐）

做法：

- 保留现有后端接口，明确 `/documents/search` 是文档工作台主查询接口。
- `/folders/{folderId}/documents` 暂时保留，作为旧调用或简单直列兼容接口，不再作为新前端主入口。
- 前端 `/folders` 停止使用完整文档列表和文档操作菜单，只通过 `/documents?folderId=...` 跳转。
- 后端补齐文档列表 DTO 中对工作台有价值的字段和权限能力，逐步减少“文件夹详情页专用别名”。

优点：

- 改动小，不破坏现有调用。
- 最符合当前后端已经具备的能力。
- 可分阶段落地，前后端可并行。

代价：

- 需要在接口文档中明确兼容接口和主接口，否则团队仍可能继续误用。
- DTO 的历史别名字段短期内会继续存在。

### 4.2 方案 B：新增 `/api/v1/documents` 统一查询入口

做法：

- 新增 `GET /api/v1/documents`，接收 `folderId`、`recursive`、筛选、排序、分页。
- 将 `/documents/search` 标记为旧搜索入口，后续前端统一走 `/documents`。

优点：

- REST 语义更自然，文档列表和搜索统一。
- 对前端更好理解。

代价：

- 会产生一段时间的双入口。
- 需要完善接口版本和迁移说明。

### 4.3 方案 C：为前端单独增加页面聚合接口

做法：

- 新增类似 `/api/v1/workbench/documents`、`/api/v1/workbench/folders/{id}` 的页面聚合接口。

优点：

- 前端调用少，页面数据一次拿齐。

代价：

- 当前项目仍是模块化单体，直接引入页面 BFF 会增加边界复杂度。
- 容易把业务规则从 `document`、`folder` 分散到聚合层。

结论：

当前推荐方案 A。除非前端性能或调用编排已经成为明确瓶颈，否则不建议新增 BFF 层。

## 5. 后端优化方案

### 5.1 API 合同收敛

目标：

- 明确“文档工作台主查询接口”。
- 明确“文件夹页面不承接文档操作”。
- 保留兼容入口，避免影响当前页面。

建议：

1. 将 `GET /api/v1/documents/search` 定义为 `/documents` 页面主列表接口。
2. `/documents` 页面加载时统一调用：

```text
GET /api/v1/documents/search?folderId={folderId}&recursive=true&keyword={keyword}&sortBy={sortBy}&sortOrder={sortOrder}&page={page}&size={size}
```

3. 如果只看当前目录直属文档，调用：

```text
GET /api/v1/documents/search?folderId={folderId}&recursive=false&page=1&size=20
```

4. `GET /api/v1/folders/{folderId}/documents` 保留为兼容接口，后续接口文档标注：

```text
兼容用途：查询指定文件夹直属文档。
新页面主入口：请使用 /api/v1/documents/search?folderId=...
```

5. 上传接口仍可保留：

```text
POST /api/v1/folders/{folderId}/documents
```

原因：

- 上传是“向某个文件夹添加文档”，嵌套资源路径可读性强。
- 现有实现已经在该接口中校验 `createChild` 权限。

可选增强：

- 新增 `POST /api/v1/documents`，multipart 中传 `folderId`，作为工作台语义入口。
- 该增强不是必须，若新增，需要保证两个上传入口复用同一 service 方法，避免权限和校验分叉。

### 5.2 文档列表响应 DTO 增强

当前 `DocumentListItemRespDTO` 已能支撑基本列表 / 网格，但建议补充工作台所需字段，减少前端二次请求：

建议新增字段：

```java
private String folderId;
private String folderName;
private String folderPath;
private String ownerDeptId;
private String ownerDeptName;
private String ownerUserName;
private Boolean previewable;
private DocumentActionPermissionRespDTO actions;
```

`actions` 建议包含：

```text
canPreview
canDownload
canUploadVersion
canEditMetadata
canBindTags
canGrantUse
canSubmitApproval
canVoid
canRestore
canDelete
```

注意：

- `actions` 只用于前端展示按钮，不是安全边界。
- 后端每个操作接口仍必须独立校验权限。
- `previewable` 只代表 MIME 类型和当前版本状态适合直接预览；实际预览仍由 `/preview` 接口最终校验。

收益：

- 列表和网格共用同一份数据。
- 前端不用在每张卡片上额外请求详情判断按钮。
- 权限显示逻辑从前端猜测变成后端给出。

兼容建议：

- 保留现有 `latestSize`、`latestMime`、`currentVersionNo`。
- `size`、`mimeType`、`status` 这类为旧文件夹详情页适配的别名先保留，但在接口文档中标注兼容字段。
- 新前端统一使用业务语义更明确的字段：`latestSize`、`latestMime`、`documentStatus`。

### 5.3 文档查询参数治理

当前 `DocumentSearchReqDTO` 已有较完整的筛选字段。建议做三类优化：

#### 5.3.1 参数校验

后端应限制排序字段白名单：

```text
name
size
createdAt
updatedAt
```

建议补充：

```text
documentStatus
businessCategory
expireDate
```

排序方向只允许：

```text
asc
desc
```

页大小建议继续限制最大值，例如当前实现中的 `100`。

#### 5.3.2 高级筛选业务化

后端查询仍使用 ID，但前端不应让业务用户填写 ID。

前端筛选项建议：

- 归属部门：部门选择器，提交 `ownerDeptId`。
- 归属用户：用户选择器，提交 `ownerUserId`。
- 资料状态：字典 / 枚举选择器，提交 `documentStatus`。
- 业务分类：字典 / 枚举选择器，提交 `businessCategory`。
- 敏感等级：字典 / 枚举选择器，提交 `sensitiveLevel`。
- 标签：标签选择器，提交 `tagIds`。

后端配合：

- 现有 `GET /api/v1/users/options` 可支撑用户选择器。
- 现有部门接口可支撑部门选择器，但需要前端实际确认调用方式。
- 字典项可优先复用 `DictController`，不要为每个筛选单独写硬编码接口。

#### 5.3.3 收藏文件夹过滤

`favoriteFolderOnly` 可以保留在文档工作台，含义应明确为：

> 仅查询当前用户收藏文件夹范围内的文档。

它不应该出现在文件夹管理页的完整文档列表中，因为该列表应被取消。

### 5.4 文件夹详情响应收敛

`FolderDetailRespDTO` 已有以下适合 `/folders` 的治理字段：

- `fullPath`
- `parentName`
- `documentCount`
- `totalDocumentCount`
- `folderSize`
- `totalSize`
- `childFolderCount`
- `totalChildFolderCount`
- `permissions`
- `viewCount`
- `lastAccessTime`
- `downloadCount`
- `isFavorite`
- `tags`
- `relatedProjects`

建议 `/folders` 页面围绕这些字段构建，不再拉取完整文档列表。

可增强字段：

```java
private FolderOperationHintRespDTO operationHints;
```

示例含义：

```text
canCreateChild
canRename
canEdit
canDelete
canMove
canCopy
canGrant
canManage
canFavorite
```

当前已有 `FolderPermissionRespDTO permissions`，如果字段已覆盖，则无需新增 `operationHints`，只需要在接口文档里明确前端使用它控制目录操作按钮。

### 5.5 `/folders` 的文档摘要做法

如果产品仍希望在文件夹详情下看到少量文档，不建议新增 `/folders/{id}/documents/preview` 这类接口。

推荐做法：

```text
GET /api/v1/documents/search?folderId={id}&recursive=false&sortBy=updatedAt&sortOrder=desc&page=1&size=5
```

前端只展示：

- 文件名
- 更新时间
- 大小
- 版本
- 状态

操作只保留：

- 查看该文件夹文档

如需预览 / 下载，应跳转或打开 `/documents` 的文档操作体验，而不是在 `/folders` 完整复刻。

这样做的原因：

- 文档摘要仍由文档模块提供。
- 文件夹模块不承接文档列表 DTO 和文档行为。
- 权限逻辑仍复用文档搜索接口。

### 5.6 权限与安全边界

必须坚持：

- 前端隐藏按钮不是安全边界。
- 文档预览、下载、删除、授权、审批等接口必须各自做后端权限校验。
- 文件夹移动、删除、授权、管理员设置必须各自做后端权限校验。

当前已确认的好做法应保留：

- 文档搜索按可见文件夹过滤。
- 文档上传校验目标文件夹 `createChild` 权限。
- 文档详情、预览、下载校验 view 权限。
- 敏感资料访问由后端做二次判断。
- 文件夹删除级联软删除文档，避免孤儿文档。
- 文件夹移动防止非法层级关系。

建议补强：

- 文档列表响应增加 `actions` 权限提示，减少前端自行推断。
- 权限提示和真实操作校验必须共用同一套权限服务或同等规则，避免“按钮能点但后端拒绝”大量出现。
- 审批、作废、恢复、删除等高风险动作继续记录审计。
- 下载和预览日志继续避免记录敏感内容或真实存储路径。

### 5.7 审计与操作记录

文档模块建议保留或增强审计：

- 预览。
- 下载。
- 上传。
- 新版本上传。
- 元数据更新。
- 标签变更。
- 使用授权。
- 审批提交。
- 作废 / 恢复。
- 删除。

文件夹模块建议保留或增强审计：

- 创建。
- 重命名。
- 编辑属性。
- 移动。
- 复制。
- 删除 / 批量删除。
- 授权变更。
- 管理员变更。
- 收藏可不一定进入强审计，但访问记录应保留。

前端展示建议：

- `/folders` 可以展示目录审计摘要。
- `/documents` 可以展示文档审计 / 版本 / 审批历史。
- 不要在 `/folders` 展示完整文档审批链路，避免页面职责变重。

### 5.8 性能优化方向

当前不建议因为页面优化就引入全文搜索引擎、消息队列或新中间件。

原因：

- 现有问题主要是页面职责和接口使用边界重复，不是已确认的基础设施瓶颈。
- 当前后端已有 PostgreSQL、MyBatis-Plus、Redis 依赖；没有代码事实证明必须新增搜索引擎或队列。
- 新中间件会增加部署、监控、备份、权限、故障恢复成本。

优先做轻量优化：

1. 查询接口统一走 `/documents/search`，减少重复实现。
2. 文档列表 DTO 一次返回网格 / 列表都需要的数据。
3. 文件夹树按需加载，避免一次性展开大树。
4. 文件夹统计尽量批量查询，避免每个节点单独统计。
5. 对高频查询字段评估索引，但必须先确认实际表结构和迁移顺序。

候选索引方向，实施前需要检查当前 SQL / migration：

```text
document(folder_id, deleted)
document(folder_id, deleted, created_at)
document(folder_id, deleted, updated_at)
document(owner_user_id, deleted)
document(owner_dept_id, deleted)
document(document_status, deleted)
folder(parent_id, deleted, sort_no)
folder(deleted, owner_user_id)
folder(deleted, owner_dept_id)
folder_favorite(user_id, folder_id, deleted)
```

如果后续文档量达到数据库模糊搜索明显吃力，再考虑 PostgreSQL trigram 或全文检索能力；只有当业务证明数据库内搜索无法满足，再讨论独立搜索引擎。

### 5.9 API 文档与兼容策略

建议在后端接口文档中增加“页面使用合同”：

```text
/documents 页面：
- 文件夹树：GET /api/v1/folders/tree/root + GET /api/v1/folders/children
- 当前文件夹详情：GET /api/v1/folders/{id}/detail
- 文档查询：GET /api/v1/documents/search
- 上传：POST /api/v1/folders/{folderId}/documents
- 文档操作：/api/v1/documents/*

/folders 页面：
- 统计：GET /api/v1/folders/stats
- 文件夹树：GET /api/v1/folders/tree/root + GET /api/v1/folders/children
- 文件夹详情：GET /api/v1/folders/{id}/detail
- 文件夹操作：/api/v1/folders/*
- 授权 / 管理员 / 收藏：/api/v1/folders/*
- 文档摘要：可选调用 GET /api/v1/documents/search?folderId=...&size=5
- 完整文档工作：跳转 /documents?folderId=...
```

兼容策略：

- 第一阶段：不删旧接口，只调整前端调用。
- 第二阶段：接口文档标注推荐入口和兼容入口。
- 第三阶段：如果确认无旧调用，再考虑废弃 `GET /api/v1/folders/{folderId}/documents` 或将其仅保留为“直属文档简表接口”。

## 6. 前端优化方案

前端代码不在当前仓库，以下是基于用户走查和截图的建议。

### 6.1 导航和命名统一

当前命名有混淆：

- “文档管理”
- “文档中心”
- `/documents` 标题“文档列表”
- `/folders` 标题“文档管理”

推荐命名：

- `/documents`：文档工作台 或 文档库。
- `/folders`：目录管理 或 文件夹管理。

导航建议：

```text
文档库
目录管理
```

如果系统面向企业归档和投标资料处理，“文档库 + 目录管理”比“文档管理 + 文档中心”更清楚。

### 6.2 `/documents` 页面保留并增强网格 / 列表

列表模式适合：

- 多字段比较。
- 审批状态、归属、更新时间、大小、版本批量查看。
- 批量操作。

网格模式适合：

- 快速识别文件类型。
- 资料库浏览。
- 投标模板、资质证明、合同模板等目录下的视觉浏览。

网格卡片建议字段：

- 文件类型图标。
- 文件名。
- 文件大小。
- 当前版本。
- 资料状态。
- 业务分类。
- 更新时间。
- 操作：预览、下载、版本、更多。

更多菜单建议：

- 元数据。
- 标签。
- 使用授权。
- 审批。
- 作废。
- 恢复。
- 删除。

注意：

- 网格和列表只改变展示，不改变后端接口。
- 切换状态建议存 `localStorage`，例如 `documentViewMode=grid|list`。
- 当前文件夹状态必须进 URL：`/documents?folderId=...`。

### 6.3 `/documents` 的 URL 状态

必须支持：

```text
/documents?folderId=123
/documents?folderId=123&keyword=合同
/documents?folderId=123&view=grid
/documents?folderId=123&upload=1
```

页面初始化逻辑：

1. 读取 `folderId`。
2. 调用 `GET /api/v1/folders/{folderId}/detail` 获取路径和 `ancestorIds`。
3. 展开文件夹树到该节点。
4. 选中该文件夹。
5. 调用 `GET /api/v1/documents/search?folderId=...` 加载文档。

刷新后必须保持选中目录，不再回到默认目录。

### 6.4 `/folders` 删除完整文档列表

`/folders` 右侧不再显示完整“文件夹内容”文档列表 / 网格。

建议替换为：

- 文件夹基础信息。
- 文件夹统计。
- 权限和继承。
- 所有者和所属部门。
- 子文件夹列表。
- 标签。
- 相关项目。
- 最近访问。
- 审计摘要。
- 最近文档 5 条，作为摘要，不给完整文档操作。
- 主按钮：查看该文件夹文档。

按钮行为：

```text
查看该文件夹文档 -> /documents?folderId={folderId}
去上传文档 -> /documents?folderId={folderId}&upload=1
```

这样可以保留用户从目录进入文档的顺滑路径，同时不让 `/folders` 变成第二个文档工作台。

### 6.5 交互统一

需要马上修的体验问题：

- `/folders` 的“创建子文件夹”“编辑”点击反馈不明显，应与顶部“新建”统一为弹窗、抽屉或明确 toast。
- 网格里的图标按钮必须有 `aria-label` 和 tooltip。
- 高级筛选不要让用户输入“归属部门 ID / 归属用户 ID”，改成部门选择器和用户选择器。
- `/documents` 刷新后应保留当前文件夹。
- 上传入口只保留一个真实流程，其他页面跳转或预填 folderId。

### 6.6 共享组件建议

前端可以共享组件，但不要共享页面职责。

可共享：

- `FolderTree`：文件夹树。
- `DocumentResultView`：文档列表 / 网格结果。
- `DocumentActionMenu`：文档操作菜单。
- `FolderStatsPanel`：目录统计。
- `FolderPermissionPanel`：目录权限。

但使用边界应明确：

- `DocumentResultView` 主要由 `/documents` 使用。
- `/folders` 只使用精简的 `RecentDocumentSummary`，不使用完整 `DocumentResultView`。
- `FolderTree` 在 `/documents` 是筛选器，在 `/folders` 是管理对象选择器。

## 7. 推荐落地顺序

### 阶段 1：先改前端调用边界

目标：

- 不改后端业务逻辑，先让页面职责清楚。

动作：

- `/documents` 统一用 `/api/v1/documents/search` 加载文档。
- `/documents` 支持 `folderId` URL 状态。
- `/folders` 移除完整文档列表 / 网格和文档操作菜单。
- `/folders` 增加“查看该文件夹文档”和“去上传文档”跳转。
- `/folders` 保留目录统计、详情、权限、标签、授权、管理员、收藏。

验证：

- 从 `/folders` 选中文件夹点击“查看该文件夹文档”，能进入 `/documents?folderId=...`。
- 刷新 `/documents?folderId=...` 后仍选中同一文件夹。
- `/documents` 的网格 / 列表切换结果一致。
- 上传默认落到 URL 中选中的 folderId。

### 阶段 2：补后端 DTO 和接口文档

目标：

- 让 `/documents` 页面不需要额外猜权限和补信息。

动作：

- `DocumentListItemRespDTO` 增加 folder 信息、部门 / 用户展示名、`previewable`、`actions`。
- 补充接口文档，明确 `/documents/search` 是主查询接口。
- 标注 `GET /folders/{folderId}/documents` 为兼容接口。
- 更新相关 service 测试。

验证：

- 文档搜索接口在 `folderId`、`recursive`、高级筛选、排序、分页下均返回正确。
- 无权限目录下的文档不会出现在结果里。
- `actions` 与实际操作接口权限一致。

### 阶段 3：清理重复语义

目标：

- 降低维护成本。

动作：

- 前端停止依赖 `DocumentListItemRespDTO` 中的文件夹详情页别名字段。
- 后端保留兼容字段一段时间，并在文档里说明不推荐新代码使用。
- 若后续确认没有旧调用，再评估废弃或弱化 `GET /folders/{folderId}/documents`。

验证：

- 前端 `/folders` 不再触发完整文档列表请求。
- 文档操作只在 `/documents` 页面集中出现。
- 接口调用日志中可观察到主查询集中到 `/documents/search`。

### 阶段 4：体验增强

目标：

- 在边界清晰后增强工作效率。

动作：

- `/documents` 网格卡片增强资料状态、业务分类、版本、更新时间。
- 高级筛选改选择器。
- 支持最近访问文件夹、收藏文件夹快速筛选。
- 支持“复制当前筛选链接”。
- 支持批量下载、批量标签、批量提交审批时再评估后端接口。

验证：

- 业务用户无需填写 ID。
- 同一份筛选链接发给有权限的人可以复现查询范围。
- 无权限用户打开链接时只看到自己有权查看的内容。

## 8. 测试建议

### 8.1 后端单元 / 集成测试

建议覆盖：

- `DocumentServiceImpl.searchDocuments`
  - 指定 `folderId` 且 `recursive=true`。
  - 指定 `folderId` 且 `recursive=false`。
  - 无 `folderId` 时只返回当前用户可见目录下的文档。
  - 无权子目录不泄露文档。
  - `favoriteFolderOnly=true`。
  - 标签、状态、部门、用户、时间范围筛选。
  - 排序字段和排序方向。

- `DocumentServiceImpl.uploadDocument`
  - 无 createChild 权限不能上传。
  - 根目录禁止上传的规则保持。
  - 同名校验保持。

- `FolderInsightServiceImpl.getDetail`
  - 直接文档数、子树文档数。
  - 直接容量、子树容量。
  - 权限字段。
  - 收藏状态。

- `FolderServiceImpl.delete / batchDelete`
  - 删除文件夹级联软删文档。
  - 父子去重。
  - 无权限删除失败。

### 8.2 前端验收场景

建议覆盖：

1. 从目录管理进入文档库：
   - 打开 `/folders`。
   - 选择“采购合同模板”。
   - 点击“查看该文件夹文档”。
   - 跳转 `/documents?folderId=...`。
   - 左侧树展开并选中该目录。
   - 右侧显示该目录文档。

2. 文档库刷新保持状态：
   - 打开 `/documents?folderId=...&view=grid`。
   - 刷新页面。
   - 仍选中原文件夹，仍是网格模式。

3. 上传路径一致：
   - 从 `/folders` 点击“去上传文档”。
   - 跳到 `/documents?folderId=...&upload=1`。
   - 上传完成后文档出现在该文件夹。

4. 权限过滤：
   - 用户 A 有父目录权限但无某子目录权限。
   - A 在 `/documents?folderId=父目录&recursive=true` 查询。
   - 不应看到无权子目录里的文档。

5. 高级筛选：
   - 部门和用户通过选择器选择。
   - 请求参数仍是 `ownerDeptId`、`ownerUserId`。

6. 可访问性：
   - 网格卡片的预览、下载、版本、更多按钮有 tooltip 和 `aria-label`。
   - 键盘可以访问主要操作。

## 9. 风险与注意事项

- 当前仓库没有前端源码，前端组件名、路由实现、状态管理方式需要在前端仓库中再次确认。
- 当前后端已有 Flyway 依赖，但本地 `dev` profile 下自动迁移状态需按仓库规则重新确认；不要直接把 SQL 草稿当作已上线表结构。
- 不要为了这次页面边界优化引入消息队列、对象存储新方案、全文搜索引擎或调度器。
- 如果要调整数据库索引，必须先确认当前 migration、实际表结构、数据量和查询计划。
- 兼容字段不要立即删除，先让前端完成迁移。
- `actions` 权限提示不能代替后端真实权限校验。
- `/folders` 取消完整文档列表后，业务用户可能短期不适应，需要保留清晰的“查看该文件夹文档”按钮。

## 10. 本次建议的最小闭环

最小闭环不需要后端大改：

1. `/documents` 支持并稳定使用 `folderId` URL 参数。
2. `/documents` 统一调用 `/api/v1/documents/search`。
3. `/documents` 保留并优化网格 / 列表切换。
4. `/folders` 移除完整文档列表，只显示目录治理信息和文档摘要。
5. `/folders` 提供跳转：

```text
/documents?folderId={folderId}
/documents?folderId={folderId}&upload=1
```

6. 后端文档补充页面接口合同。
7. 第二步再补 `DocumentListItemRespDTO` 的 folder 展示字段和 `actions` 权限提示。

这个闭环能最快实现目标：让 `/documents` 管文件，让 `/folders` 管目录，同时保留用户喜欢的网格模式，并把后续增强放在清晰边界上继续推进。
