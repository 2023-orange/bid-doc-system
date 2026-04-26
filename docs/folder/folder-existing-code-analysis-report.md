# 现有代码分析报告

## 1. 项目结构概览

### 1.1 项目形态

- 项目是单体 Maven Spring Boot 工程。
- Spring Boot 版本为 `3.2.4`，JDK 版本为 `17`。
- 启动类为 [BidDocSystemApplication.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/BidDocSystemApplication.java:8)。
- 根包为 `com.example.biddoc`。

### 1.2 模块划分

源码按业务模块平铺在 `src/main/java/com/example/biddoc` 下：

- `auth`
- `common`
- `folder`
- `document`
- `audit`
- `notify`
- `workflow`

### 1.3 当前实际实现情况

按当前 Java 文件数量统计：

| 模块 | Java 文件数 | 结论 |
| --- | ---: | --- |
| `auth` | 34 | 已有真实业务实现 |
| `common` | 16 | 已有公共底座实现 |
| `folder` | 0 | 仅有目录骨架 |
| `document` | 0 | 仅有目录骨架 |
| `audit` | 0 | 仅有目录骨架 |
| `notify` | 0 | 仅有目录骨架 |
| `workflow` | 0 | 仅有目录骨架 |

### 1.4 folder 模块当前状态

`folder` 目录已预留如下子目录，但均为空目录，没有任何 Java 实现：

- `controller`
- `converter`
- `dto`
- `entity`
- `mapper`
- `service`

结论：

- 当前项目真正可分析的业务代码主要集中在 `auth` 和 `common`。
- `folder/document/audit/notify/workflow` 目前属于占位模块，不存在业务实现。

---

## 2. auth 模块已有能力梳理

`auth` 模块当前已经形成了较完整的基础权限与组织管理能力，主要覆盖：

- 用户注册
- 用户登录 / 登出 / 当前用户查询
- 用户详情查询
- 用户启停
- 角色分配与撤销
- 用户有效角色计算
- 部门新增与部门列表

---

## 2.1 用户能力

### 2.1.1 用户实体

用户实体为 `SysUser`，对应表 `sys_user`。

核心字段包括：

- `id`
- `username`
- `password`
- `realName`
- `email`
- `mobile`
- `deptId`
- `jobLevel`
- `status`
- `deleted`
- `lastLoginTime`
- `loginCount`
- `createdAt`
- `createdBy`
- `updatedAt`
- `updatedBy`
- `remark`
- `extensionData`

其中：

- `id` 使用 MyBatis-Plus 的 `ASSIGN_ID`
- `extensionData` 使用 `JacksonTypeHandler` 存储 JSON 扩展字段
- 审计字段支持自动填充

对应文件：

- [SysUser.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/entity/SysUser.java:15)

### 2.1.2 用户注册

注册入口：

- `POST /api/v1/auth/register`

请求 DTO：

- `UserRegisterReqDTO`

已做的校验包括：

- 用户名非空，长度 `4-20`
- 密码非空，长度至少 `8`
- 真实姓名非空
- 邮箱非空且格式正确
- 手机号非空
- 部门 ID 非空
- 职级非空

注册逻辑：

1. 根据 `username + deleted=false` 校验用户是否已存在。
2. 若已存在，抛出 `RESOURCE_CONFLICT`。
3. 新用户默认状态设置为 `0`。
4. 默认 `deleted=false`。
5. 插入用户数据。
6. 注册完成后，自动分配默认角色 `EMPLOYEE`。

实现文件：

- [AuthController.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/controller/AuthController.java:30)
- [UserRegisterReqDTO.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/dto/req/UserRegisterReqDTO.java:16)
- [AuthServiceImpl.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/service/impl/AuthServiceImpl.java:33)
- [UserConvertor.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/convertor/UserConvertor.java:12)

### 2.1.3 用户详情查询

接口：

- `GET /api/v1/users/{id}`

能力：

- 根据用户 ID 查询 `SysUser`
- 若不存在，抛出 `RESOURCE_NOT_FOUND`
- 转换为 `UserDetailRespDTO`
- 补充用户当前有效角色列表 `roleCodes`

实现文件：

- [UserController.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/controller/UserController.java:21)
- [UserServiceImpl.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/service/impl/UserServiceImpl.java:23)
- [UserDetailRespDTO.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/dto/resp/UserDetailRespDTO.java:12)

### 2.1.4 用户启用 / 禁用

接口：

- `PATCH /api/v1/users/{id}/status`

能力：

- 根据用户 ID 修改 `status`
- 当前仅做存在性校验
- 未看到更细的状态流转限制

状态注释显示：

- `1` 表示启用
- `0` 表示禁用

实现文件：

- [UserController.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/controller/UserController.java:29)
- [UserStatusUpdateReqDTO.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/dto/req/UserStatusUpdateReqDTO.java:11)
- [UserServiceImpl.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/service/impl/UserServiceImpl.java:34)

### 2.1.5 用户成熟度判断

已有：

- 注册
- 详情查询
- 状态修改
- 登录成功后登录次数与最后登录时间更新

缺少：

- 用户列表
- 用户分页查询
- 用户更新资料
- 用户删除
- 密码加密
- 重置密码

特别说明：

- 当前登录逻辑使用明文密码直接比对，不适合生产环境。

---

## 2.2 角色能力

### 2.2.1 RoleCodeEnum

当前系统角色通过 `RoleCodeEnum` 固化，现有角色包括：

- `SUPER_ADMIN`
- `FOLDER_ADMIN`
- `DEPT_MANAGER`
- `EMPLOYEE`

提供的方法：

- `getByCode(String code)`
- `isValid(String code)`

作用判断：

- 当前角色体系的事实来源主要是这个枚举，而不是数据库角色主数据。

实现文件：

- [RoleCodeEnum.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/constant/RoleCodeEnum.java:11)

### 2.2.2 角色主数据实体

存在 `SysRole` 实体和 `SysRoleMapper`：

- `SysRole`
- `SysRoleMapper`

但从当前代码引用看：

- 没有看到任何 service/controller 对 `SysRole` 做查询或维护
- 也没有看到角色主数据表真正参与权限判断

结论：

- `sys_role` 当前只是预留结构
- 实际可用的角色来源仍然是 `RoleCodeEnum + sys_user_role`

对应文件：

- [SysRole.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/entity/SysRole.java:11)
- [SysRoleMapper.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/mapper/SysRoleMapper.java:7)

### 2.2.3 用户角色关系

用户角色关系实体为 `SysUserRole`，对应表 `sys_user_role`。

关键字段：

- `userId`
- `roleCode`
- `isPrimary`
- `status`
- `effectiveStartTime`
- `effectiveEndTime`
- `sourceType`
- `deleted`
- 审计字段

说明：

- 支持主角色
- 支持角色有效期
- 支持状态控制
- 支持逻辑删除
- 支持来源类型

对应文件：

- [SysUserRole.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/entity/SysUserRole.java:9)
- [SourceTypeEnum.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/constant/SourceTypeEnum.java:11)

### 2.2.4 角色分配与撤销

接口：

- `POST /api/v1/roles/assign`
- `DELETE /api/v1/roles/revoke`

当前能力：

- 校验角色编码是否合法
- 校验同一用户同一角色是否重复分配
- 支持指定是否主角色
- 如果新角色设置为主角色，会先把该用户现有主角色清空
- 新分配角色默认为 `status=1`
- 来源类型为 `MANUAL`
- 撤销角色时并不是物理删除，而是 `deleted=true`
- 分配或撤销后，会刷新 Sa-Token Session 中缓存的角色列表

权限限制：

- 分配角色仅 `SUPER_ADMIN`
- 撤销角色仅 `SUPER_ADMIN`

对应文件：

- [RoleController.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/controller/RoleController.java:24)
- [UserRoleAssignReqDTO.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/dto/req/UserRoleAssignReqDTO.java:10)
- [UserRoleServiceImpl.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/service/impl/UserRoleServiceImpl.java:29)

### 2.2.5 角色查询能力

当前已实现：

- 查询某用户的角色列表
- 查询某角色下的用户 ID 列表
- 查询某用户当前有效角色编码列表
- 查询某用户主角色编码

其中“当前有效角色”的判断逻辑包括：

- `status = 1`
- `deleted = false`
- 当前时间满足有效期约束

结论：

- `auth` 已经具备较完整的“用户-角色”关系能力
- 可以直接支撑后续模块做角色鉴权

对应文件：

- [UserRoleService.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/service/UserRoleService.java:8)
- [UserRoleServiceImpl.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/service/impl/UserRoleServiceImpl.java:84)

---

## 2.3 部门能力

### 2.3.1 部门实体

部门实体为 `SysDepartment`，对应表 `sys_department`。

核心字段：

- `id`
- `name`
- `parentId`
- `level`
- `managerUserId`
- `status`
- `deleted`
- 审计字段
- `remark`
- `extensionData`

说明：

- 支持部门层级
- 支持部门负责人 `managerUserId`
- 支持 JSON 扩展字段

对应文件：

- [SysDepartment.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/entity/SysDepartment.java:11)

### 2.3.2 部门接口

已实现接口：

- `POST /api/v1/departments`
- `GET /api/v1/departments`

已实现能力：

- 新增部门
- 查询全部部门列表

DTO 校验：

- `name` 非空
- `parentId` 非空
- `level` 非空，且范围 `1-2`

实现文件：

- [DepartmentController.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/controller/DepartmentController.java:24)
- [DepartmentCreateReqDTO.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/dto/req/DepartmentCreateReqDTO.java:13)
- [DepartmentServiceImpl.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/service/impl/DepartmentServiceImpl.java:18)
- [DepartmentConvertor.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/convertor/DepartmentConvertor.java:8)

### 2.3.3 部门能力边界

当前已实现程度较浅，只能算“基础可用”：

已有：

- 新增部门
- 查询部门平铺列表

缺少：

- 部门树构建
- 部门详情
- 部门修改
- 部门删除
- 部门负责人维护
- 部门状态维护

说明：

- `DepartmentTreeRespDTO` 已预留，但未在当前代码中落地使用。

对应文件：

- [DepartmentTreeRespDTO.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/dto/resp/DepartmentTreeRespDTO.java:12)

---

## 2.4 登录态能力

### 2.4.1 登录实现

登录入口：

- `POST /api/v1/auth/login`

逻辑：

1. 按 `username + deleted=false` 查询用户。
2. 校验用户存在。
3. 校验密码相等。
4. 校验用户状态必须为 `1`。
5. 更新 `lastLoginTime` 与 `loginCount`。
6. 调用 `StpUtil.login(user.getId())` 完成登录。
7. 把脱敏后的用户对象写入 Session。
8. 把当前有效角色列表写入 Session。

对应文件：

- [AuthServiceImpl.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/service/impl/AuthServiceImpl.java:64)

### 2.4.2 登录后 Session 内容

当前登录成功后，Session 中至少缓存两类数据：

- `user`
- `roleCodes`

说明：

- `user` 为脱敏后的 `SysUser`
- `roleCodes` 为当前时间下生效的角色集合

对应文件：

- [AuthServiceImpl.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/service/impl/AuthServiceImpl.java:82)

### 2.4.3 对外登录相关接口

已实现接口：

- `POST /api/v1/auth/login`
- `GET /api/v1/auth/me`
- `POST /api/v1/auth/logout`

返回内容：

- 登录返回 token、过期时间、当前用户信息、当前角色列表
- `me` 返回当前用户信息与当前角色列表

对应文件：

- [LoginRespDTO.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/dto/resp/LoginRespDTO.java:12)
- [AuthController.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/auth/controller/AuthController.java:38)

### 2.4.4 当前登录态技术路线

当前项目真实使用的是：

- `Sa-Token`
- `Redis`

而不是项目里的 `JwtUtil`。

判断依据：

- 登录、会话、角色校验全部围绕 `StpUtil`、`SaInterceptor`、`StpInterface` 运转
- `JwtUtil` 只是一个占位类，当前未接入认证链路

对应文件：

- [WebMvcConfig.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/common/config/WebMvcConfig.java:41)
- [StpInterfaceImpl.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/common/config/StpInterfaceImpl.java:25)
- [JwtUtil.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/common/util/JwtUtil.java:5)

---

## 2.5 UserContext 能力

`UserContext` 位于 `common`，但从业务使用上看，是 `auth` 登录态的重要组成部分。

### 2.5.1 UserContext 中保存的信息

当前线程上下文中保存：

- `userId`
- `username`
- `roleCodes`
- `deptId`

对应文件：

- [UserContext.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/common/constant/UserContext.java:25)

### 2.5.2 提供的辅助方法

支持：

- `getRole()`
- `hasRole(String roleCode)`
- `hasAnyRole(String... codes)`
- `isAdmin()`
- `isSuperAdmin()`
- `isDeptManager()`
- `isFolderAdmin()`

价值：

- 后续业务模块可直接基于当前用户角色和部门做权限判断
- `folder` 模块尤其可以直接用 `isFolderAdmin()` 和 `deptId`

### 2.5.3 上下文写入时机

写入时机在 `TokenInterceptor.preHandle()`：

1. `StpUtil.checkLogin()`
2. 从 Session 中取出 `user`
3. 从 Session 中取出 `roleCodes`
4. 构建 `UserContext.UserInfo`
5. 写入 `ThreadLocal`
6. 请求结束时清理

对应文件：

- [TokenInterceptor.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/common/interceptor/TokenInterceptor.java:23)

结论：

- `UserContext` 当前已经可作为后续业务模块统一获取当前用户信息的入口。

---

## 3. common 模块已有能力梳理

`common` 模块当前已经具备基础后端项目常用底座，包括：

- 统一响应
- 统一异常
- 基础断言工具
- 登录态拦截
- 权限拦截
- 请求日志链路
- MyBatis-Plus 自动填充
- Jackson 全局序列化配置

---

## 3.1 统一响应能力

### 3.1.1 ApiResponse

统一响应类为 `ApiResponse<T>`。

字段：

- `code`
- `message`
- `data`
- `timestamp`

已提供静态工厂：

- `success(T data)`
- `success()`
- `fail(ErrorCode errorCode)`
- `fail(ErrorCode errorCode, String message)`

当前约定：

- 成功码固定为 `0`
- 成功消息固定为 `"success"`

对应文件：

- [ApiResponse.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/common/result/ApiResponse.java:11)

### 3.1.2 使用情况

当前所有已实现 controller 均返回 `ApiResponse`。

说明：

- 统一响应约定已在项目中落地
- `folder` 模块后续可以直接沿用

---

## 3.2 异常体系能力

### 3.2.1 ErrorCode

已定义统一错误码枚举：

- `PARAM_INVALID`
- `RESOURCE_CONFLICT`
- `BUSINESS_ILLEGAL`
- `AUTH_FAILED`
- `ACCOUNT_DISABLED`
- `LOGIN_LIMITED`
- `PERMISSION_DENIED`
- `ROLE_NOT_MATCH`
- `RESOURCE_NOT_FOUND`
- `SYSTEM_ERROR`

对应文件：

- [ErrorCode.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/common/exception/ErrorCode.java:8)

### 3.2.2 BusinessException

业务异常统一使用 `BusinessException`，内部持有 `ErrorCode`。

支持两种构造：

- 仅传 `ErrorCode`
- 传 `ErrorCode + 自定义 message`

对应文件：

- [BusinessException.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/common/exception/BusinessException.java:6)

### 3.2.3 AssertUtil

当前提供两个常用断言：

- `notNull`
- `isTrue`

效果：

- 断言失败时直接抛 `BusinessException`

说明：

- `auth` 模块已经大量使用这个工具
- `folder` 模块完全可以继续复用

对应文件：

- [AssertUtil.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/common/util/AssertUtil.java:6)

### 3.2.4 GlobalExceptionHandler

统一异常处理已覆盖：

- `MethodArgumentNotValidException`
- `BindException`
- `HttpMessageNotReadableException`
- `BusinessException`
- `NotLoginException`
- `NotRoleException`
- `NotPermissionException`
- `NoHandlerFoundException`
- `HttpRequestMethodNotSupportedException`
- `Exception`

输出统一为 `ApiResponse<Void>`。

说明：

- 参数校验、鉴权异常、业务异常、系统异常都已有统一出口
- 对后续模块开发价值很高

对应文件：

- [GlobalExceptionHandler.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/common/exception/GlobalExceptionHandler.java:18)

### 3.2.5 一个明确问题

当前 `GlobalExceptionHandler` 中导入的是：

- `java.net.BindException`

而不是 Spring MVC 常见的参数绑定异常类型。

结论：

- 这一处应记录为现有缺陷
- 本次仅记录，不实现

---

## 3.3 分页能力

### 3.3.1 已有内容

存在分页返回模型：

- `PageResponse<T>`

内部包含：

- `list`
- `pagination.page`
- `pagination.size`
- `pagination.total`
- `pagination.hasNext`

对应文件：

- [PageResponse.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/common/result/PageResponse.java:10)

### 3.3.2 当前缺失内容

在当前代码中未看到：

- MyBatis-Plus 分页拦截器 `MybatisPlusInterceptor`
- `PaginationInnerInterceptor`
- 统一分页请求 DTO
- controller/service 中实际分页查询落地

结论：

- 当前只有“分页返回对象定义”
- 还不能算完整分页公共能力

---

## 3.4 MyBatis-Plus 能力

### 3.4.1 已有接入

项目已引入：

- `mybatis-plus-spring-boot3-starter`

全局配置包括：

- 驼峰命名映射
- SQL 控制台日志
- 逻辑删除全局字段

对应文件：

- [pom.xml](D:/JavaProject/bid-doc-system/bid-doc-system/pom.xml:66)
- [application.yml](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/resources/application.yml:60)

### 3.4.2 当前使用方式

当前实体普遍使用：

- `@TableName`
- `@TableId`
- `@TableField`

当前 mapper 普遍使用：

- `BaseMapper<T>`

当前 service 中普遍使用：

- `Wrappers.lambdaQuery()`
- `Wrappers.lambdaUpdate()`

结论：

- MyBatis-Plus 已经是项目的标准 ORM 用法
- `folder` 模块可以直接复用同样模式

---

## 3.5 逻辑删除能力

### 3.5.1 全局配置

`application.yml` 中已配置：

- `logic-delete-field: deleted`
- `logic-delete-value: "true"`
- `logic-not-delete-value: "false"`

对应文件：

- [application.yml](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/resources/application.yml:67)

### 3.5.2 当前代码现状

虽然全局配置已经存在，但当前代码使用方式仍然比较手工：

- 查询时显式写 `.eq(deleted, false)`
- 删除时手动 `setDeleted(true)`

例如：

- 注册用户时手动 `setDeleted(false)`
- 撤销角色时手动 `setDeleted(true)`

结论：

- 项目有“逻辑删除约定”
- 但还没有形成完全统一、标准化的使用方式

因此对这一项的判断应为：

- 已有基础能力
- 但成熟度一般

---

## 3.6 审计字段自动填充能力

### 3.6.1 自动填充实现

公共配置类：

- `MyMetaObjectHandler`

插入时自动填充：

- `createdAt`
- `updatedAt`
- `createdBy`
- `updatedBy`

更新时自动填充：

- `updatedAt`
- `updatedBy`

当前 `createdBy/updatedBy` 的值来源：

- `UserContext.get().getUserId()`

对应文件：

- [MyMetaObjectHandler.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/common/config/MyMetaObjectHandler.java:11)

### 3.6.2 依赖条件

自动填充依赖两个条件：

1. 实体字段上声明 `@TableField(fill = ...)`
2. 当前请求链路已通过 `TokenInterceptor` 写入 `UserContext`

### 3.6.3 当前已接入实体

当前已明确接入填充注解的实体包括：

- `SysUser`
- `SysDepartment`
- `SysUserRole`

说明：

- `folder` 后续新实体如果遵循同样字段命名和注解方式，可以直接复用该能力

---

## 3.7 鉴权与拦截能力

### 3.7.1 WebMvc 拦截器链

当前拦截器注册顺序：

1. `LoggingInterceptor`
2. `SaInterceptor`
3. `TokenInterceptor`
4. `AuthInterceptor`

对应文件：

- [WebMvcConfig.java](D:/JavaProject/bid-doc-system/bid-doc-system/src/main/java/com/example/biddoc/common/config/WebMvcConfig.java:33)

### 3.7.2 LoggingInterceptor

能力：

- 生成 `traceId`
- 记录请求开始日志
- 记录响应结束日志
- 统计耗时
- 使用 MDC 传递链路信息

结论：

- 已具备基础访问日志能力

### 3.7.3 TokenInterceptor

能力：

- 校验是否登录
- 从 Session 取用户信息
- 从 Session 取角色列表
- 写入 `UserContext`
- 写入 MDC 用户信息
- 请求结束清理上下文

结论：

- 当前登录态上下文链路是完整可用的

### 3.7.4 AuthInterceptor

当前只覆盖两类路径：

- `/api/v1/users/**` 仅允许 `SUPER_ADMIN`
- `/api/v1/departments/**` 允许 `SUPER_ADMIN` 或 `DEPT_MANAGER`

结论：

- 这是一个针对已存在模块的“定制化权限拦截器”
- 并不是可自动覆盖全业务模块的通用权限框架

这一点对 `folder` 很关键：

- 未来 `folder` 如果走 `/api/v1/folders/**`
- 当前不会自动被这个拦截器管控

---

## 4. folder 模块开发时可复用的现有能力

虽然 `folder` 模块本身还没有任何 Java 实现，但从现有 `auth/common` 看，已有不少能力可以直接复用。

---

## 4.1 可以直接复用的能力

### 4.1.1 统一接口风格

可以直接复用：

- `ApiResponse`
- `BusinessException`
- `ErrorCode`
- `AssertUtil`
- Jakarta Validation 注解风格

意义：

- `folder` 接口不需要重新设计统一响应和异常机制

### 4.1.2 认证与当前用户获取

可以直接复用：

- Sa-Token 登录态
- `TokenInterceptor`
- `UserContext`

意义：

- `folder` 业务代码可以直接拿当前用户 ID、当前角色列表、当前部门 ID
- 不需要自己写用户上下文获取逻辑

### 4.1.3 角色体系

可以直接复用：

- `RoleCodeEnum`
- `UserRoleService`

特别是：

- `FOLDER_ADMIN` 已经在枚举中预留

这意味着：

- `folder` 模块可以直接围绕 `FOLDER_ADMIN` 设计管理员权限
- 不需要重新引入一套角色标识

### 4.1.4 数据访问模式

可以直接复用：

- MyBatis-Plus 实体注解风格
- `BaseMapper`
- `Wrappers.lambdaQuery/lambdaUpdate`
- `deleted` 字段约定
- 审计字段自动填充

意义：

- `folder` 实体、mapper、service 的基础模式已经在项目里存在

---

## 4.2 对 folder 特别有价值的复用点

### 4.2.1 FOLDER_ADMIN 已经预留

这是 `folder` 模块最直接的现成基础：

- 当前系统角色枚举已经包含 `FOLDER_ADMIN`
- `UserContext` 也已经提供了 `isFolderAdmin()`

结论：

- `folder` 模块权限设计可以直接沿用这一角色，不需要新增概念

### 4.2.2 deptId 已经进入 UserContext

当前登录用户上下文中已经有：

- `deptId`

这对 `folder` 后续做以下能力很有帮助：

- 按部门可见
- 按部门授权
- 部门管理员查看本部门文件夹

结论：

- 组织维度的数据权限具备基础输入条件

---

## 4.3 只能部分复用、但还不完整的能力

### 4.3.1 权限拦截

当前 `AuthInterceptor` 并不会自动覆盖 `folder` 路径。

因此：

- 登录态校验可以复用
- 但 `folder` 自己的接口权限规则还不能直接复用现有 `AuthInterceptor`

换句话说：

- `folder` 需要后续补自己的权限接入方式
- 这次只能记为复用基础，不是复用完整方案

### 4.3.2 分页

当前只能复用：

- `PageResponse`

不能直接复用：

- 完整分页查询框架

因此：

- `folder` 若要开发列表接口，分页基础能力仍不足

### 4.3.3 逻辑删除

当前可以复用：

- `deleted` 字段命名约定
- 全局逻辑删除配置

但不能认为项目已经有非常成熟的统一逻辑删除规范。

因此：

- `folder` 能复用约定
- 但仍需补规范

---

## 4.4 当前缺少、但 folder 很可能会需要的公共能力

以下内容当前项目中未形成成熟公共能力，只能记录为建议，不能直接认为已具备：

### 4.4.1 通用分页基础设施

当前缺少：

- 分页请求 DTO
- MyBatis-Plus 分页拦截器
- 分页结果统一装配工具

### 4.4.2 通用基础实体

当前多个实体都重复定义了：

- `id`
- `deleted`
- `createdAt`
- `createdBy`
- `updatedAt`
- `updatedBy`

说明：

- 尚未形成公共 `BaseEntity`
- `folder` 若开发实体，大概率会重复这套字段

### 4.4.3 通用数据权限助手

虽然 `UserContext` 里有 `deptId` 和角色信息，但当前没有看到：

- “当前用户可访问哪些部门/哪些数据”的公共判断工具
- 面向业务模块的数据权限封装

这意味着：

- `folder` 一旦做部门隔离或共享范围控制，就会缺少公共支撑

### 4.4.4 通用目录/树结构工具

当前项目只有部门层级字段，没有通用树结构构建工具。

若 `folder` 后续要做：

- 文件夹树
- 递归查询
- 节点路径构建

则当前没有现成公共能力。

---

## 5. 结论

### 5.1 当前项目真实可用的核心基础

目前项目里，真正可复用、且已经落地的基础主要集中在两块：

1. `auth`
2. `common`

其中：

- `auth` 提供了用户、角色、部门、登录态、角色缓存、当前用户信息能力
- `common` 提供了统一响应、异常、拦截器链、MyBatis-Plus 接入、审计字段自动填充等基础底座

### 5.2 对 folder 模块的判断

`folder` 模块当前没有任何业务实现，但开发时可以明确复用以下现有能力：

- 统一响应 `ApiResponse`
- 统一异常体系 `ErrorCode + BusinessException + AssertUtil`
- 请求参数校验风格
- Sa-Token 登录态
- `UserContext`
- `RoleCodeEnum.FOLDER_ADMIN`
- `UserRoleService`
- MyBatis-Plus 基础 ORM 使用模式
- 审计字段自动填充
- `deleted` 字段约定

### 5.3 需要记录为建议而非现有能力的部分

当前项目仍缺少或不完整的公共能力，应记录为建议：

- 完整分页能力
- 通用基础实体
- 通用数据权限封装
- 通用树结构工具
- 更成熟的逻辑删除规范
- 密码加密机制

### 5.4 最终判断

可以认为当前项目状态是：

- `auth/common` 已经形成“能支撑新模块起步开发”的基础底座
- 但底座成熟度还未达到“所有业务公共问题都已抽象完成”的程度
- `folder` 模块开发时可以明显复用已有能力，但仍会遇到若干公共能力缺口

这些缺口本次仅作记录，不做实现。
