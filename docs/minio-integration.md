# Goal-2: MinIO 对象存储集成

## 概述

将文档存储从本地磁盘迁移到 MinIO 对象存储，提供生产级的分布式存储方案。

## 实现内容

### 1. 依赖添加

**pom.xml**
```xml
<!-- MinIO Java SDK -->
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.7</version>
</dependency>
```

### 2. 配置文件

**application-dev.yml**
```yaml
storage:
  type: minio  # 切换存储类型: local 或 minio
  local:
    root: ${STORAGE_LOCAL_ROOT:./.local-storage}
  minio:
    endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
    access-key: ${MINIO_ACCESS_KEY:minioadmin}
    secret-key: ${MINIO_SECRET_KEY:minioadmin}
    bucket-name: ${MINIO_BUCKET:bid-doc-system}
```

### 3. 核心类

#### MinioProperties.java
- 配置属性类，从 `storage.minio` 节点读取配置
- 包含: endpoint, accessKey, secretKey, bucketName

#### MinioStorageAdapter.java
- 实现 `StorageAdapter` 接口
- 使用 `@ConditionalOnProperty(name = "storage.type", havingValue = "minio")` 条件装配
- 仅在 `storage.type=minio` 时生效

**核心方法**：
- `put()`: 上传文件到 MinIO，返回存储键（格式: yyyy/MM/dd/雪花ID）
- `get()`: 从 MinIO 获取文件输入流
- `size()`: 获取文件大小
- `exists()`: 检查文件是否存在
- `delete()`: 软删除（预留，暂不实际删除）

**初始化逻辑**：
- `@PostConstruct init()`: 初始化 MinIO 客户端
- 自动检查并创建存储桶（如果不存在）

### 4. 存储键格式

与本地磁盘保持一致：
```
yyyy/MM/dd/雪花ID[.ext]
```

例如：
```
2026/05/31/2061030289933570052
2026/05/31/2061030289933570052.pdf
```

## 使用方式

### 启动 MinIO（本地测试）

```powershell
# 使用 Docker 启动 MinIO
pwsh scripts/start-minio.ps1
```

**MinIO 信息**：
- API 端点: http://localhost:9000
- 控制台: http://localhost:9001
- 用户名: minioadmin
- 密码: minioadmin

### 切换存储类型

**方式1：修改配置文件**
```yaml
# application-dev.yml
storage:
  type: minio  # 从 local 改为 minio
```

**方式2：环境变量**
```bash
export STORAGE_TYPE=minio
./mvnw.cmd spring-boot:run
```

### 验证 MinIO 存储

```powershell
# 1. 启动 MinIO
pwsh scripts/start-minio.ps1

# 2. 修改 application-dev.yml: storage.type=minio

# 3. 启动应用
./mvnw.cmd spring-boot:run

# 4. 运行验证脚本
pwsh scripts/verify-minio-storage.ps1
```

## 验证测试

**verify-minio-storage.ps1** 测试场景：
- M1: 上传文档到 MinIO
- M2: 获取文档详情
- M3: 下载文档并验证内容
- M4: 上传新版本到 MinIO
- M5: 下载 V1 并验证内容
- M6: 下载当前版本（V2）并验证内容
- M7: 获取版本列表
- M8: 删除文档（软删除）

## 架构优势

### 1. 适配器模式
- `StorageAdapter` 接口抽象存储操作
- `LocalDiskStorageAdapter` 和 `MinioStorageAdapter` 实现具体存储
- 通过配置切换，对上层业务透明

### 2. 条件装配
- 使用 Spring `@ConditionalOnProperty` 实现条件装配
- 同一时间只有一个 StorageAdapter 实例生效
- 避免冲突和资源浪费

### 3. 存储键一致性
- 本地磁盘和 MinIO 使用相同的存储键格式
- 便于数据迁移和回滚
- 数据库中的 `storage_key` 字段无需修改

## 生产部署建议

### MinIO 集群部署
```yaml
storage:
  type: minio
  minio:
    endpoint: https://minio.example.com
    access-key: ${MINIO_ACCESS_KEY}  # 从环境变量读取
    secret-key: ${MINIO_SECRET_KEY}  # 从环境变量读取
    bucket-name: bid-doc-system-prod
```

### 安全建议
1. **不要在配置文件中硬编码密钥**，使用环境变量或密钥管理服务
2. **启用 HTTPS**：生产环境 MinIO 端点必须使用 HTTPS
3. **访问控制**：配置 MinIO 存储桶策略，限制访问权限
4. **备份策略**：定期备份 MinIO 数据

### 性能优化
1. **分片上传**：大文件使用分片上传（MinIO SDK 自动处理）
2. **CDN 加速**：对外提供下载时，可在 MinIO 前加 CDN
3. **连接池**：MinIO 客户端内置连接池，无需额外配置

## 数据迁移

### 从本地磁盘迁移到 MinIO

```bash
# 使用 MinIO Client (mc) 工具
mc alias set local-minio http://localhost:9000 minioadmin minioadmin
mc cp --recursive .local-storage/ local-minio/bid-doc-system/
```

### 从 MinIO 回滚到本地磁盘

```bash
# 下载 MinIO 数据到本地
mc cp --recursive local-minio/bid-doc-system/ .local-storage/
```

## 故障排查

### 问题1：连接 MinIO 失败
**症状**：应用启动时报错 "MinIO 初始化失败"

**排查**：
1. 检查 MinIO 是否启动：`docker ps | grep minio`
2. 检查端点配置：`storage.minio.endpoint`
3. 检查网络连通性：`curl http://localhost:9000/minio/health/live`

### 问题2：上传失败
**症状**：上传文档时报错 "MinIO 上传失败"

**排查**：
1. 检查存储桶是否存在：访问 MinIO 控制台
2. 检查访问密钥是否正确
3. 查看应用日志中的详细错误信息

### 问题3：下载失败
**症状**：下载文档时报错 "文件不存在"

**排查**：
1. 检查 `storage_key` 是否正确
2. 在 MinIO 控制台中查看对象是否存在
3. 检查存储桶名称是否匹配

## 文件清单

### 新增文件
- `src/main/java/com/example/biddoc/document/storage/config/MinioProperties.java`
- `src/main/java/com/example/biddoc/document/storage/impl/MinioStorageAdapter.java`
- `scripts/start-minio.ps1`
- `scripts/verify-minio-storage.ps1`
- `docs/minio-integration.md`

### 修改文件
- `pom.xml`: 添加 MinIO 依赖
- `src/main/resources/application-dev.yml`: 添加 MinIO 配置

## 下一步建议

1. **生产环境部署**：部署 MinIO 集群到生产环境
2. **数据迁移**：将现有本地磁盘数据迁移到 MinIO
3. **监控告警**：集成 MinIO 监控指标到监控系统
4. **备份策略**：配置 MinIO 数据备份和灾难恢复方案
