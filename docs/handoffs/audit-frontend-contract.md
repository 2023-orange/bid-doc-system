# 审计日志业务轨迹前端对接说明

## 目标

本次审计日志增强面向“用户业务操作轨迹”展示，前端优先展示谁在什么时间对哪个业务对象做了什么操作。重点覆盖文件预览、下载、上传、版本、授权、项目清单绑定、审批流转、文件夹权限变化等场景。

当前仓库未确认存在前端工程，本文件仅作为前后端接口契约和页面调整建议，不包含前端实现代码。

## 权限

所有 v1 审计查询接口仍仅允许 `SUPER_ADMIN` 访问：

- `GET /api/v1/audit/logs`
- `GET /api/v1/audit/logs/{id}`
- `GET /api/v1/audit/timeline`

## 通用响应

统一响应外层结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": 1782017656000
}
```

## 审计列表

```http
GET /api/v1/audit/logs?moduleCode=DOCUMENT&keyword=投标&page=1&size=20
```

### 查询参数

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `moduleCode` | string | 模块编码，如 `DOCUMENT`、`WORKFLOW`、`PROJECT`、`FOLDER` |
| `bizType` | string | 主业务对象类型，如 `DOCUMENT`、`FOLDER`、`CHECKLIST_ITEM` |
| `bizId` | number | 主业务对象 ID |
| `operationType` | string | 操作类型，如 `DOWNLOAD`、`PREVIEW`、`APPROVAL_APPROVE` |
| `operatorUserId` | number | 操作人用户 ID |
| `operatorDeptId` | number | 操作人部门 ID |
| `relatedBizType` | string | 关联上下文类型，如 `APPROVAL_INSTANCE`、`PROJECT` |
| `relatedBizId` | number | 关联上下文 ID |
| `keyword` | string | 关键词，后端当前匹配 `objectName`、`actionSummary`、`requestId` |
| `startTime` | string | 操作开始时间，ISO OffsetDateTime |
| `endTime` | string | 操作结束时间，ISO OffsetDateTime |
| `page` | number | 页码，默认 `1` |
| `size` | number | 每页条数，默认 `20` |

### 列表响应核心结构

`data` 为分页对象：

```json
{
  "list": [
    {
      "id": 206800000000000001,
      "operationTime": "2026-06-20T15:34:16+08:00",
      "moduleCode": "DOCUMENT",
      "bizType": "DOCUMENT",
      "bizId": 300400000000000001,
      "operationType": "DOWNLOAD",
      "operatorUserId": 300200000000000001,
      "operatorName": "张三",
      "operatorDeptId": 100100000000000001,
      "objectName": "投标文件.pdf",
      "actionSummary": "张三 下载了《投标文件.pdf》 v3",
      "relatedBizType": "APPROVAL_INSTANCE",
      "relatedBizId": 206800000000000001,
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
}
```

列表页建议优先展示这些列：

- 时间：`operationTime`
- 摘要：`actionSummary`
- 模块：`moduleCode`
- 业务对象：`objectName`
- 操作人：优先 `operatorName`，缺失时展示 `operatorUserId`
- 请求 ID：`requestId`

建议筛选项：

- 模块：`moduleCode`
- 业务类型：`bizType`
- 操作类型：`operationType`
- 操作人：`operatorUserId`
- 关键词：`keyword`
- 时间范围：`startTime`、`endTime`
- 关联上下文：`relatedBizType`、`relatedBizId`

## 审计详情

```http
GET /api/v1/audit/logs/{id}
```

详情响应 `data` 为单个 `AuditLogRespDTO`，包含列表字段以及完整 JSON 数据：

```json
{
  "id": 206800000000000001,
  "moduleCode": "DOCUMENT",
  "bizType": "DOCUMENT",
  "bizId": 300400000000000001,
  "operationType": "DOWNLOAD",
  "operatorUserId": 300200000000000001,
  "operatorName": "张三",
  "operatorDeptId": 100100000000000001,
  "requestId": "33eabaa...",
  "objectName": "投标文件.pdf",
  "actionSummary": "张三 下载了《投标文件.pdf》 v3",
  "relatedBizType": "APPROVAL_INSTANCE",
  "relatedBizId": 206800000000000001,
  "clientIp": "10.0.0.8",
  "userAgent": "Mozilla/5.0 ...",
  "operationTime": "2026-06-20T15:34:16+08:00",
  "beforeData": {},
  "afterData": {},
  "extraData": {
    "versionNo": 3
  },
  "createdAt": "2026-06-20T15:34:16+08:00",
  "createdBy": "system"
}
```

详情抽屉建议分区：

- 基础信息：摘要、模块、业务对象、操作类型、操作人、时间、请求 ID、IP、User-Agent
- 变更前：`beforeData`
- 变更后：`afterData`
- 扩展数据：`extraData`

`beforeData`、`afterData`、`extraData` 都是 JSON 对象，前端不要直接把对象拼进文本或表格单元格，否则会显示为 `[object Object]`。建议使用格式化 JSON 查看器、键值表，或 `JSON.stringify(value, null, 2)` 后放入等宽文本区域。

## 对象时间线

文件详情页和审批详情页建议接入时间线接口，用于聚合某个对象或某个业务上下文的操作轨迹。

### 文件时间线

```http
GET /api/v1/audit/timeline?bizType=DOCUMENT&bizId=300400000000000001
```

用于文件详情页“操作轨迹”标签。后端按 `operationTime asc` 返回该文件的上传、预览、下载、新版本、删除、审批等动作。

### 审批流转时间线

```http
GET /api/v1/audit/timeline?relatedBizType=APPROVAL_INSTANCE&relatedBizId=206800000000000001
```

用于审批详情页“流转轨迹”标签。后端按 `operationTime asc` 返回提交、通过、驳回、撤回、转交、加签、终止等动作。

### 时间线响应

`data` 为 `AuditLogRespDTO[]`，字段与详情接口一致。时间线页面建议主要使用：

- `operationTime`
- `actionSummary`
- `operatorName`
- `operationType`
- `objectName`
- `beforeData`、`afterData`、`extraData` 作为展开详情

## 模块中文映射

| code | 中文 |
| --- | --- |
| `FOLDER` | 文件夹模块 |
| `DOCUMENT` | 文档模块 |
| `PROJECT` | 项目模块 |
| `WORKFLOW` | 审批流程模块 |

## 操作类型中文映射

| code | 中文 |
| --- | --- |
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

## 页面调整建议

- 审计列表默认优先显示 `actionSummary`，不要让用户先读 `beforeData/afterData/extraData` 才知道发生了什么。
- 文件详情页可新增“操作轨迹”标签，调用 `bizType=DOCUMENT&bizId={documentId}` 的时间线。
- 审批详情页可新增“流转轨迹”标签，调用 `relatedBizType=APPROVAL_INSTANCE&relatedBizId={instanceId}` 的时间线。
- 列表中的 `clientIp`、`userAgent` 建议放到详情抽屉，不作为默认主列，避免页面过宽。
- `requestId` 保留为排查字段，可在列表末列或详情基础信息中展示。
- 当 `actionSummary` 为空时，前端可降级为“操作人 + 操作类型中文 + 业务对象名”的组合展示。

