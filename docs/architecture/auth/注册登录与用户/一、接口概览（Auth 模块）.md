**《注册 / 登录 / 用户 / 部门模块 API 接口文档（Swagger 风格）》**，
**严格面向前后端分离开发**，接口定义可直接用于 **Spring Boot + Swagger/OpenAPI** 实现。

> 接口域：`/api/v1/auth`
> 接口版本：`v1.0`
> 认证方式：`JWT Bearer Token`
> 数据格式：`application/json`
> 统一返回结构：`code + message + data + timestamp + requestId`

------

# 一、接口概览（Auth 模块）

| 接口分类        | 说明        |
| --------------- | ----------- |
| Auth-Register   | 用户注册    |
| Auth-Login      | 用户登录    |
| User            | 用户管理    |
| Department      | 部门管理    |
| Role & JobLevel | 角色 / 职级 |

------

# 二、接口统一说明

## 1. 统一请求头（Headers）

| 参数名        | 类型   | 必需 | 描述               |
| ------------- | ------ | ---- | ------------------ |
| Content-Type  | string | 是   | application/json   |
| Authorization | string | 否   | Bearer {JWT_TOKEN} |

> ⚠️ 除【注册 / 登录】接口外，其余接口 **必须携带 Authorization**

------

## 2. 统一成功响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": 1710000000000,
  "requestId": "uuid"
}
```

------

## 3. 统一分页响应结构

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 100,
      "hasNext": true
    }
  }
}
```

------

# 三、接口详情

------

## 3.1 用户注册接口

### 接口概览

- **接口名称**：用户注册
- **接口版本**：v1.0
- **接口分类**：Auth-Register
- **接口描述**：新用户注册账号并绑定部门
- **使用场景**：新员工首次使用系统

------

### 基本信息

- **接口地址**
  `POST /api/v1/auth/register`
- **认证方式**
  无（公开接口）
- **接口状态**
  已上线

------

### 请求体（Request Body）

```json
{
  "username": {
    "type": "string",
    "description": "登录用户名",
    "required": true,
    "constraints": "唯一，4-20位",
    "example": "zhangsan"
  },
  "password": {
    "type": "string",
    "description": "登录密码",
    "required": true,
    "constraints": "≥8位，包含大小写字母+数字",
    "example": "Aa123456"
  },
  "realName": {
    "type": "string",
    "description": "真实姓名",
    "required": true,
    "example": "张三"
  },
  "email": {
    "type": "string",
    "description": "企业邮箱",
    "required": true,
    "constraints": "唯一",
    "example": "zhangsan@company.com"
  },
  "mobile": {
    "type": "string",
    "description": "手机号",
    "required": true,
    "example": "13800000000"
  },
  "deptId": {
    "type": "long",
    "description": "二级部门ID",
    "required": true,
    "constraints": "必须为二级部门",
    "example": 12
  },
  "jobLevel": {
    "type": "string",
    "description": "职级",
    "required": true,
    "constraints": "MANAGER / SUPERVISOR / STAFF",
    "example": "STAFF"
  }
}
```

------

### 成功响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": 10001,
    "status": "PENDING"
  }
}
```

------

### 错误码

| 错误码  | 描述               |
| ------- | ------------------ |
| 4001001 | 参数校验失败       |
| 4001002 | 用户名或邮箱已存在 |
| 4001003 | 部门非法           |
| 4001004 | 部门经理已存在     |

------

### cURL 示例

```bash
curl -X POST 'https://api.example.com/api/v1/auth/register' \
-H 'Content-Type: application/json' \
-d '{
  "username": "zhangsan",
  "password": "Aa123456",
  "realName": "张三",
  "email": "zhangsan@company.com",
  "mobile": "13800000000",
  "deptId": 12,
  "jobLevel": "STAFF"
}'
```

------

## 3.2 用户登录接口

### 接口概览

- **接口名称**：用户登录
- **接口分类**：Auth-Login
- **接口描述**：获取 JWT 访问令牌

------

### 基本信息

- **接口地址**
  `POST /api/v1/auth/login`

------

### 请求体

```json
{
  "username": {
    "type": "string",
    "required": true,
    "example": "zhangsan"
  },
  "password": {
    "type": "string",
    "required": true,
    "example": "Aa123456"
  }
}
```

------

### 成功响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "jwt-token",
    "expireIn": 7200,
    "user": {
      "id": 10001,
      "realName": "张三",
      "deptId": 12,
      "jobLevel": "STAFF",
      "role": "USER"
    }
  }
}
```

------

### 错误码

| 错误码  | 描述             |
| ------- | ---------------- |
| 4011001 | 用户名或密码错误 |
| 4011002 | 用户未启用       |
| 4011003 | 登录失败次数过多 |

------

## 3.3 获取当前登录用户信息

### 接口概览

- **接口名称**：当前用户信息
- **接口分类**：User

------

### 基本信息

- **接口地址**
  `GET /api/v1/auth/me`
- **认证方式**
  Bearer Token

------

### 成功响应

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 10001,
    "username": "zhangsan",
    "realName": "张三",
    "email": "zhangsan@company.com",
    "dept": {
      "id": 12,
      "name": "市场支持一室"
    },
    "jobLevel": "STAFF",
    "role": "USER"
  }
}
```

------

## 3.4 用户列表查询（管理员）

### 接口概览

- **接口名称**：用户列表
- **接口分类**：User
- **权限要求**：ADMIN

------

### 基本信息

- **接口地址**
  `GET /api/v1/users`

------

### 查询参数

| 参数   | 类型   | 必需 | 示例   |
| ------ | ------ | ---- | ------ |
| page   | int    | 否   | 1      |
| size   | int    | 否   | 20     |
| deptId | long   | 否   | 12     |
| status | string | 否   | ACTIVE |

------

### 分页响应

（统一分页结构，略）

------

## 3.5 启用 / 禁用用户

### 基本信息

- **接口地址**
  `PUT /api/v1/users/{id}/status`

------

### 路径参数

| 参数 | 类型 | 必需 | 示例  |
| ---- | ---- | ---- | ----- |
| id   | long | 是   | 10001 |

------

### 请求体

```json
{
  "status": {
    "type": "string",
    "required": true,
    "constraints": "ACTIVE / DISABLED",
    "example": "DISABLED"
  }
}
```

------

## 3.6 部门树查询

### 接口概览

- **接口名称**：部门树
- **接口分类**：Department

------

### 基本信息

- **接口地址**
  `GET /api/v1/departments/tree`

------

### 成功响应

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "市场部",
      "children": [
        {
          "id": 12,
          "name": "市场支持一室"
        }
      ]
    }
  ]
}
```

------

## 3.7 新建部门（管理员）

### 基本信息

- **接口地址**
  `POST /api/v1/departments`

------

### 请求体

```json
{
  "name": {
    "type": "string",
    "required": true,
    "example": "市场支持一室"
  },
  "parentId": {
    "type": "long",
    "required": false,
    "example": 1
  }
}
```

------

## 四、错误处理统一说明

### 错误码表（Auth）

| 错误码  | 错误信息     | 解决方案   |
| ------- | ------------ | ---------- |
| 4001001 | 参数校验失败 | 检查参数   |
| 4011001 | 未认证       | 登录       |
| 4031001 | 权限不足     | 联系管理员 |
| 4041001 | 资源不存在   | 校验ID     |
| 5001001 | 系统异常     | 查看日志   |

------

## 五、接口约束

### 性能

- 登录接口：5 次 / 分钟 / IP
- 查询接口：100 次 / 分钟

### 安全

- JWT HS256
- Token 2 小时过期
- 登录失败次数限制

### 业务

- 用户必须绑定二级部门
- 部门最多 2 层
- 一个部门仅 1 个经理

------

## 六、接口版本变更记录

| 版本 | 日期    | 说明     |
| ---- | ------- | -------- |
| v1.0 | 2026-01 | 初始版本 |

