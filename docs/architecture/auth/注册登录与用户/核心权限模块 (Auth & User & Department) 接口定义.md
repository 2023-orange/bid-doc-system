基于您提供的《注册/登录/用户/部门模块 API 接口文档》及优化要求，我为您重新设计了符合 **RESTful 规范** 的 OpenAPI (Swagger) 风格接口定义。

本次优化主要改进点：

1. **RESTful 语义修正**：如状态修改建议使用 `PATCH` 而非 `PUT`。
2. **结构标准化**：将请求/响应对象（DTO）抽取为独立组件，便于复用。
3. **完整性补充**：补充了原文档中缺失但必要的 CRUD 操作（如部门修改、用户详情查询）。
4. **规范命名**：统一字段命名风格（驼峰）和路径参数规范。

------

# 核心权限模块 (Auth & User & Department) 接口定义

## 一、 接口设计概览

### 1. 基础信息

- **Base URL**: `/api/v1`
- **API Version**: `v1.0.0`
- **Protocol**: HTTP/1.1
- **Media Type**: `application/json; charset=utf-8`

### 2. 鉴权机制

- **Type**: JWT (JSON Web Token)
- **Header**: `Authorization: Bearer <token>`
- **Scope**: 除 `Auth` 分类下的注册/登录接口外，所有接口均需携带 Token。

### 3. 统一响应结构

所有接口（无论成功失败）遵循以下外层结构：

JSON

```
// 成功响应
{
  "code": 0,
  "message": "success",
  "data": { ... }, // 业务数据
  "timestamp": 1709251200000
}

// 失败响应
{
  "code": 4001001,
  "message": "参数校验失败",
  "details": "邮箱格式不正确", // 可选详细描述
  "timestamp": 1709251200000
}
```

------

## 二、 API 接口定义 (OpenAPI YAML)

YAML

```
apiVersion: v1
kind: API
description: |
  核心权限管理模块，包含用户注册、登录认证、组织架构管理及用户状态管理。
  
  业务场景:
  - 新员工入职注册账号并绑定部门
  - 系统管理员维护部门树结构
  - 管理员审核/禁用用户账号

paths:
  # ============================
  # Authentication (认证模块)
  # ============================
  /auth/register:
    post:
      summary: 用户注册
      description: 新用户提交注册申请，绑定部门与职级。注册后状态默认为 PENDING (待审核)。
      tags:
        - Auth
      security: [] # 公开接口
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UserRegisterReq'
      responses:
        '200':
          description: 注册提交成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserRegisterResp'

  /auth/login:
    post:
      summary: 用户登录
      description: 使用用户名和密码获取 JWT 访问令牌。
      tags:
        - Auth
      security: [] # 公开接口
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/LoginReq'
      responses:
        '200':
          description: 登录成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/LoginResp'

  /auth/me:
    get:
      summary: 获取当前登录用户信息
      description: 基于 Token 解析当前用户详情，包含角色和部门信息。
      tags:
        - Auth
      responses:
        '200':
          description: 获取成功
          content:
            application/json:
              schema:
                type: object
                properties:
                  code: { type: integer, example: 0 }
                  data:
                    $ref: '#/components/schemas/UserDetailVO'

  # ============================
  # User Management (用户管理)
  # ============================
  /users:
    get:
      summary: 用户列表查询
      description: 管理员分页查询用户列表，支持按部门、状态筛选。
      tags:
        - User
      parameters:
        - name: page
          in: query
          description: 页码 (默认1)
          schema: { type: integer, default: 1 }
        - name: size
          in: query
          description: 每页条数 (默认20)
          schema: { type: integer, default: 20 }
        - name: deptId
          in: query
          description: 部门ID筛选
          schema: { type: integer }
        - name: status
          in: query
          description: 用户状态筛选 (ACTIVE/DISABLED/PENDING)
          schema: { type: string }
        - name: keyword
          in: query
          description: 搜索关键字 (姓名/手机号)
          schema: { type: string }
      responses:
        '200':
          description: 查询成功
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/BaseResponse'
                  - type: object
                    properties:
                      data:
                        $ref: '#/components/schemas/PageResult_UserDetailVO'

  /users/{id}/status:
    patch:
      summary: 启用/禁用用户
      description: 修改指定用户的账号状态。
      tags:
        - User
      parameters:
        - name: id
          in: path
          required: true
          description: 用户ID
          schema: { type: integer }
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [status]
              properties:
                status:
                  type: string
                  enum: [ACTIVE, DISABLED]
                  description: 目标状态
      responses:
        '200':
          description: 操作成功 (无返回数据)

  # ============================
  # Department Management (部门管理)
  # ============================
  /departments:
    get:
      summary: 获取部门列表/树
      description: 获取全量部门结构，默认返回树形结构。
      tags:
        - Department
      parameters:
        - name: mode
          in: query
          description: 返回模式 (tree: 树形, list: 列表)
          schema: { type: string, default: 'tree' }
      responses:
        '200':
          description: 获取成功
          content:
            application/json:
              schema:
                allOf:
                  - $ref: '#/components/schemas/BaseResponse'
                  - type: object
                    properties:
                      data:
                        type: array
                        items:
                          $ref: '#/components/schemas/DepartmentVO'

    post:
      summary: 创建部门
      description: 创建新的部门节点。
      tags:
        - Department
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/DepartmentCreateReq'
      responses:
        '200':
          description: 创建成功
          content:
            application/json:
              schema:
                type: object
                properties:
                  id: { type: integer, description: 新部门ID }

  /departments/{id}:
    put:
      summary: 更新部门信息
      description: 更新部门名称或负责人。
      tags:
        - Department
      parameters:
        - name: id
          in: path
          required: true
          schema: { type: integer }
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/DepartmentUpdateReq'
      responses:
        '200':
          description: 更新成功

    delete:
      summary: 删除部门
      description: 删除指定部门。若部门下存在用户或子部门，则无法删除。
      tags:
        - Department
      parameters:
        - name: id
          in: path
          required: true
          schema: { type: integer }
      responses:
        '200':
          description: 删除成功

# ============================
# Components / DTO Definitions
# ============================
components:
  schemas:
    # --- 基础响应 ---
    BaseResponse:
      type: object
      properties:
        code: { type: integer, example: 0 }
        message: { type: string, example: "success" }
        timestamp: { type: integer }
        requestId: { type: string }

    # --- 请求对象 ---
    LoginReq:
      type: object
      required: [username, password]
      properties:
        username:
          type: string
          example: "zhangsan"
          description: 用户名
        password:
          type: string
          example: "Aa123456"
          description: 密码 (建议前端加密传输)

    UserRegisterReq:
      type: object
      required: [username, password, realName, email, mobile, deptId, jobLevel]
      properties:
        username:
          type: string
          minLength: 4
          maxLength: 20
          description: 登录账号 (唯一)
        password:
          type: string
          minLength: 8
          description: 登录密码
        realName:
          type: string
          description: 真实姓名
        email:
          type: string
          format: email
          description: 企业邮箱 (唯一)
        mobile:
          type: string
          pattern: "^1[3-9]\\d{9}$"
          description: 手机号
        deptId:
          type: integer
          description: 归属部门ID (必须为二级部门)
        jobLevel:
          type: string
          enum: [MANAGER, SUPERVISOR, STAFF]
          description: 职级 (MANAGER:经理, SUPERVISOR:主管, STAFF:员工)

    DepartmentCreateReq:
      type: object
      required: [name]
      properties:
        name:
          type: string
          description: 部门名称
        parentId:
          type: integer
          description: 上级部门ID (空则为一级部门)

    DepartmentUpdateReq:
      type: object
      properties:
        name:
          type: string
          description: 部门名称
        managerId:
          type: integer
          description: 部门负责人用户ID

    # --- 响应对象 ---
    LoginResp:
      type: object
      properties:
        token:
          type: string
          description: JWT Access Token
        expireIn:
          type: integer
          description: 过期时间(秒)
        user:
          $ref: '#/components/schemas/UserDetailVO'

    UserRegisterResp:
      type: object
      properties:
        userId: { type: integer }
        status: { type: string, example: "PENDING" }

    UserDetailVO:
      type: object
      description: 用户详细信息视图
      properties:
        id: { type: integer }
        username: { type: string }
        realName: { type: string }
        email: { type: string }
        mobile: { type: string }
        jobLevel: { type: string }
        role: { type: string, description: 系统角色(ADMIN/USER) }
        status: { type: string, enum: [ACTIVE, DISABLED, PENDING] }
        dept:
          type: object
          properties:
            id: { type: integer }
            name: { type: string }

    DepartmentVO:
      type: object
      description: 部门树节点
      properties:
        id: { type: integer }
        name: { type: string }
        level: { type: integer, description: 层级(1/2) }
        children:
          type: array
          items:
            $ref: '#/components/schemas/DepartmentVO'

    # --- 分页泛型 ---
    PageResult_UserDetailVO:
      type: object
      properties:
        list:
          type: array
          items:
            $ref: '#/components/schemas/UserDetailVO'
        pagination:
          type: object
          properties:
            page: { type: integer }
            size: { type: integer }
            total: { type: integer }
            hasNext: { type: boolean }
```

------

## 三、 状态码定义 (Status Codes)

### 1. HTTP 状态码

- `200 OK`: 请求成功
- `400 Bad Request`: 参数校验失败 / 业务规则不满足
- `401 Unauthorized`: Token 无效或过期
- `403 Forbidden`: 权限不足 (如普通用户尝试获取全部列表)

### 2. 业务错误码 (Code)

| **错误码**  | **错误信息 (Message)** | **详细描述 / 触发场景**                          |
| ----------- | ---------------------- | ------------------------------------------------ |
| **0**       | success                | 成功                                             |
| **4001001** | 参数校验失败           | 必填项缺失、格式错误、密码强度不足               |
| **4001002** | 资源冲突               | 用户名已存在、邮箱已被注册、部门经理已存在       |
| **4001003** | 业务规则非法           | 尝试将用户注册到一级部门、部门层级超过限制       |
| **4011001** | 认证失败               | 用户名或密码错误                                 |
| **4011002** | 账号不可用             | 账号处于禁用 (DISABLED) 或 待审核 (PENDING) 状态 |
| **4011003** | 登录受限               | 连续登录失败次数过多，账号被临时锁定             |
| **4031001** | 权限不足               | 非管理员尝试操作管理接口                         |
| **4041001** | 资源未找到             | 指定的部门ID或用户ID不存在                       |

------

## 四、 接口调用示例

### 1. 注册新用户

**Request:**

Bash

```
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "wangwu",
  "password": "Password123",
  "realName": "王五",
  "email": "wangwu@company.com",
  "mobile": "13912345678",
  "deptId": 102,
  "jobLevel": "STAFF"
}
```

**Response (200 OK):**

JSON

```
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": 10005,
    "status": "PENDING"
  },
  "timestamp": 1709251234000,
  "requestId": "abc-123-xyz"
}
```

### 2. 禁用用户 (管理员)

**Request:**

Bash

```
PATCH /api/v1/users/10005/status
Authorization: Bearer eyJhbGciOiJIUzI1Ni...
Content-Type: application/json

{
  "status": "DISABLED"
}
```

**Response (200 OK):**

JSON

```
{
  "code": 0,
  "message": "success",
  "data": null,
  "timestamp": 1709251255000,
  "requestId": "def-456-uvw"
}
```