这份文档是基于你提供的两份接口定义，结合 **DDD（领域驱动设计）** 和 **统一响应/异常处理** 规范优化后的最终版本。它不仅包含接口定义，还详细阐述了数据在系统各层级间的流转逻辑。

---

# 📘 核心权限模块 (Auth/User/Dept) 接口与架构设计文档

## 一、 全局规范与架构设计

### 1. 统一响应结构 (Result Wrapper)
所有 API 无论成功或失败，HTTP 状态码建议统一为 `200 OK`（或遵循 RESTful，但业务层必须返回以下 JSON），前端通过 `code` 判断业务结果。

```json
{
  "code": 0,             // 业务状态码：0-成功，非0-失败
  "message": "success",  // 提示信息
  "data": { ... },       // 业务数据 (泛型 T)
  "traceId": "a1b2-c3d4",// 链路追踪ID，用于排查日志
  "timestamp": 1709251200000
}
```

### 2. 全局异常处理策略 (GlobalExceptionHandler)

| 异常类型                            | HTTP 状态 | 返回 Code | 返回 Message           | 处理逻辑                                                     |
| :---------------------------------- | :-------- | :-------- | :--------------------- | :----------------------------------------------------------- |
| **BusinessException**               | 200       | 4xxxxxx   | e.getMessage()         | 捕获业务逻辑错误（如“用户已存在”），返回具体错误码给前端展示。 |
| **MethodArgumentNotValidException** | 200/400   | 4001001   | "参数校验失败"         | 解析 `@Valid` 错误详情，放入 `data` 或拼接在 message 中。    |
| **Exception** (兜底)                | 500       | 5000000   | "系统繁忙，请稍后重试" | **记录 ERROR 日志**（包含堆栈 + traceId），隐藏具体报错细节以防泄露敏感信息。 |

### 3. DDD 分层数据链路 (Data Flow)

基于你要求的四层架构，数据流向如下：

#### ➡️ **上行链路（Request：前端 -> 数据库）**
1.  **API 层 (User Interface)**
    *   **Input**: 前端发送 JSON。
    *   **Action**: `Controller` 接收 `RegisterRequestDTO`。
    *   **Validation**: 使用 `@Validated` 进行格式校验（非空、正则）。
    *   **Call**: 调用 `ApplicationService.register(dto)`。
2.  **Application 层 (应用服务)**
    *   **Action**: 编排流程。
    *   **Convert**: 使用 `Convertor` 将 `DTO` 转为 `UserEntity`（此时 Entity 是贫血的或仅包含基础数据）。
    *   **Call**: 调用 `DomainService` 或直接调用 `Repository` 检查唯一性。
3.  **Domain 层 (核心领域)**
    *   **Logic**: `UserEntity` 执行业务逻辑，例如 `user.initPassword()`, `user.assignDepartment(dept)`.
    *   **Rule**: 确保业务规则（如：经理只能有一个）在此层校验。
4.  **Infrastructure 层 (基础设施)**
    *   **Action**: `UserRepositoryImpl` 实现接口。
    *   **Convert**: 将 `UserEntity` 转为 `UserPO` (Persistent Object) / `DO`。
    *   **Output**: 执行 MyBatis/JPA SQL 写入数据库。

#### ⬅️ **下行链路（Response：数据库 -> 前端）**
1.  **Infrastructure 层**: SQL 查询 -> `UserPO` -> 转为 `UserEntity` -> 返回给 Domain/App 层。
2.  **Application 层**: 获取 `UserEntity` -> 使用 `Convertor` 组装 `UserVO` (View Object，剔除密码等敏感字段)。
3.  **API 层**: 将 `UserVO` 包装进 `Result<UserVO>` -> 注入 `traceId` -> 返回 JSON。

---

## 二、 API 接口定义详情

> **Base URL**: `/api/v1`
> **Headers**: `Content-Type: application/json`, `Authorization: Bearer {token}`

### 1. 🔐 认证模块 (Auth)

#### 1.1 用户注册
*   **URL**: `POST /auth/register`
*   **权限**: 公开
*   **描述**: 新员工注册，需绑定部门。

**请求参数 (UserRegisterDTO):**
```json
{
  "username": "zhangsan",      // 必填, 4-20位
  "password": "Password123",   // 必填, 需包含大小写字母+数字
  "realName": "张三",          // 必填
  "email": "zhang@corp.com",   // 必填, 邮箱格式
  "mobile": "13800138000",     // 必填, 手机正则
  "deptId": 102,               // 必填, 必须为二级部门ID
  "jobLevel": "STAFF"          // 枚举: MANAGER, SUPERVISOR, STAFF
}
```

**响应数据 (Result<Map>):**
```json
{
  "code": 0,
  "message": "success",
  "traceId": "0a1b2c3d",
  "data": {
    "userId": 10001,
    "status": "PENDING" // 注册后默认为待审核
  }
}
```

#### 1.2 用户登录
*   **URL**: `POST /auth/login`
*   **权限**: 公开

**请求参数 (LoginDTO):**
```json
{
  "username": "zhangsan",
  "password": "Password123"
}
```

**响应数据 (Result<LoginVO>):**
```json
{
  "code": 0,
  "message": "success",
  "traceId": "e5f6g7h8",
  "data": {
    "token": "eyJhbGciOiJIUz...",
    "expireIn": 7200,
    "user": {
      "id": 10001,
      "realName": "张三",
      "role": "USER"
    }
  }
}
```

---

### 2. 👤 用户管理模块 (User)

#### 2.1 获取当前用户信息
*   **URL**: `GET /auth/me`
*   **权限**: 登录用户

**响应数据 (Result<UserDetailVO>):**
```json
{
  "code": 0,
  "message": "success",
  "traceId": "i9j0k1l2",
  "data": {
    "id": 10001,
    "username": "zhangsan",
    "realName": "张三",
    "email": "zhang@corp.com",
    "mobile": "138****8000",
    "jobLevel": "STAFF",
    "role": "USER",
    "dept": {
      "id": 102,
      "name": "研发一部"
    }
  }
}
```

#### 2.2 用户列表查询 (支持分页与筛选)
*   **URL**: `GET /users`
*   **权限**: 管理员 (ADMIN)
*   **Query参数**: `page=1`, `size=20`, `deptId=102`, `keyword=张`

**响应数据 (Result<PageResult<UserDetailVO>>):**
```json
{
  "code": 0,
  "message": "success",
  "traceId": "m3n4o5p6",
  "data": {
    "list": [
      { "id": 10001, "realName": "张三", "deptName": "研发一部", "status": "ACTIVE" },
      { "id": 10002, "realName": "李四", "deptName": "研发一部", "status": "DISABLED" }
    ],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 52,
      "hasNext": true
    }
  }
}
```

#### 2.3 修改用户状态
*   **URL**: `PATCH /users/{id}/status`
*   **权限**: 管理员
*   **描述**: 禁用/启用用户，踢出当前登录Token。

**请求参数 (UserStatusUpdateDTO):**
```json
{
  "status": "DISABLED" // ACTIVE / DISABLED
}
```

---

### 3. 🏢 部门管理模块 (Department)

#### 3.1 获取部门树
*   **URL**: `GET /departments`
*   **权限**: 登录用户
*   **Query参数**: `mode=tree`

**响应数据 (Result<List<DeptTreeVO>>):**
```json
{
  "code": 0,
  "message": "success",
  "traceId": "q7r8s9t0",
  "data": [
    {
      "id": 1,
      "name": "总部",
      "children": [
        { "id": 101, "name": "研发部", "children": [] },
        { "id": 102, "name": "市场部", "children": [] }
      ]
    }
  ]
}
```

#### 3.2 创建部门
*   **URL**: `POST /departments`
*   **权限**: 管理员

**请求参数 (DeptCreateDTO):**
```json
{
  "name": "测试组",
  "parentId": 101
}
```

---

## 三、 数据上游与返回示例 (Example Data Flow)

以下展示一个完整的 **“用户注册”** 场景的数据变化过程：

### 1. 前端 -> API 层 (DTO)
**数据形态**: `UserRegisterDTO` (Java Bean / JSON)
```json
// Controller 接收到的 JSON
{
  "username": "david",
  "password": "SafePwd123",
  "deptId": 10
}
```
**Controller 动作**: 
1. `@Valid` 校验通过。
2. 转换为 `UserEntity` (此时 id=null, password=明文)。

### 2. Application 层 -> Domain 层 (Entity)
**数据形态**: `UserEntity` (充血模型)
**Domain 动作**:
1. `UserEntity.encryptPassword()`: 密码变为 `BCrypt` 密文。
2. `UserEntity.initStatus()`: 状态设置为 `PENDING`。
3. `DomainService.checkUserUnique("david")`: 检查唯一性。

### 3. Domain 层 -> Infrastructure 层 (PO)
**数据形态**: `UserPO` (对应数据库表结构)
```java
// Mapper 插入数据库的数据
UserPO(id=null, username="david", pwd_hash="$2a$10$...", dept_id=10, created_at=now())
```

### 4. Infrastructure 层 -> Database (Row)
**数据形态**: MySQL Row
| id   | username | password_hash | dept_id | status      | created_at          |
| :--- | :------- | :------------ | :------ | :---------- | :------------------ |
| 205  | david    | $2a$10$XyZ... | 10      | 0 (PENDING) | 2025-01-01 10:00:00 |

### 5. 返回路径 (Response)
**数据形态**: `Result<Map>`
数据库生成 ID `205` -> `UserEntity` 更新 ID -> `Application` 组装返回对象 -> `GlobalExceptionHandler`/`ResponseBodyAdvice` 包装 Result。

```json
// 最终前端收到的
{
  "code": 0,
  "message": "success",
  "data": { "userId": 205, "status": "PENDING" },
  "traceId": "f93k-221s-3321"
}
```

---

## 四、 错误码对照表

| Error Code  | Message                        | 触发层级                      |
| :---------- | :----------------------------- | :---------------------------- |
| **0**       | success                        | API                           |
| **4001001** | 参数校验失败: [邮箱]格式不正确 | API (DTO Validation)          |
| **4001002** | 用户名 [david] 已存在          | Application/Domain            |
| **4001003** | 指定的部门不存在               | Domain                        |
| **4001004** | 密码强度不足                   | Domain                        |
| **5000000** | 系统繁忙，请稍后重试           | GlobalExceptionHandler (兜底) |