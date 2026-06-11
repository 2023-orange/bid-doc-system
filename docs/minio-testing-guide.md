# MinIO 集成测试指南

## 测试环境准备

由于本地环境没有 Docker，有以下几种测试方案：

### 方案1：使用 MinIO 独立可执行文件（推荐）

#### Windows 环境

1. **下载 MinIO Server**
```powershell
# 下载 MinIO Windows 可执行文件
Invoke-WebRequest -Uri "https://dl.min.io/server/minio/release/windows-amd64/minio.exe" -OutFile "$env:USERPROFILE\minio.exe"
```

2. **启动 MinIO**
```powershell
# 创建数据目录
mkdir C:\minio-data

# 启动 MinIO（在新的 PowerShell 窗口中运行）
$env:MINIO_ROOT_USER="minioadmin"
$env:MINIO_ROOT_PASSWORD="minioadmin"
& "$env:USERPROFILE\minio.exe" server C:\minio-data --console-address ":9001"
```

3. **访问 MinIO**
- API 端点: http://localhost:9000
- 控制台: http://localhost:9001
- 用户名: minioadmin
- 密码: minioadmin

### 方案2：使用 Docker Desktop（如果已安装）

如果已安装 Docker Desktop，运行：
```powershell
pwsh scripts/start-minio.ps1
```

### 方案3：使用公共 MinIO 测试服务

MinIO 官方提供的测试服务：
- 端点: https://play.min.io
- Access Key: Q3AM3UQ867SPQQA43P2F
- Secret Key: zuf+tfteSlswRu7BJ86wekitnifILbZam1KYY3TG
- 存储桶: 需要自己创建

**注意**：公共服务不稳定，仅用于快速测试。

## 测试步骤

### 步骤1：启动 MinIO

选择上述任一方案启动 MinIO 服务。

### 步骤2：修改配置

编辑 `src/main/resources/application-dev.yml`：

```yaml
storage:
  type: minio  # 从 local 改为 minio
  minio:
    endpoint: http://localhost:9000
    access-key: minioadmin
    secret-key: minioadmin
    bucket-name: bid-doc-system
```

### 步骤3：重启应用

```bash
# 停止当前应用（如果正在运行）
# Ctrl+C 或 taskkill

# 重新启动
./mvnw.cmd spring-boot:run
```

**观察启动日志**，应该看到：
```
MinIO 存储桶已创建: bid-doc-system
MinIO 存储适配器初始化成功，端点: http://localhost:9000, 存储桶: bid-doc-system
```

### 步骤4：运行验证脚本

```powershell
pwsh scripts/verify-minio-storage.ps1
```

### 步骤5：验证 MinIO 控制台

1. 访问 http://localhost:9001
2. 登录（minioadmin / minioadmin）
3. 进入 `bid-doc-system` 存储桶
4. 查看上传的文件（按日期目录组织）

## 预期结果

### 应用启动日志
```
MinIO 存储桶已创建: bid-doc-system
MinIO 存储适配器初始化成功，端点: http://localhost:9000, 存储桶: bid-doc-system
```

### 验证脚本输出
```
==== All MinIO Storage Tests Passed! ====

提示: 文件已成功存储到 MinIO 对象存储
可以访问 MinIO 控制台查看: http://localhost:9001
存储桶: bid-doc-system
```

### MinIO 控制台
在 `bid-doc-system` 存储桶中应该看到：
```
2026/
  05/
    31/
      2061030289933570052
      2061030289933570053
```

## 手动测试（不使用脚本）

如果验证脚本无法运行，可以手动测试：

### 1. 登录
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testadmin","password":"12345678"}'
```

保存返回的 token。

### 2. 创建文件夹
```bash
curl -X POST http://localhost:8080/api/v1/folders \
  -H "Authorization: YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"parentId":0,"name":"minio_test","remark":"MinIO test"}'
```

保存返回的 folderId。

### 3. 上传文档
```bash
curl -X POST http://localhost:8080/api/v1/folders/FOLDER_ID/documents \
  -H "Authorization: YOUR_TOKEN" \
  -F "file=@test.txt" \
  -F "name=test_doc" \
  -F "changeLog=Initial version"
```

保存返回的 documentId。

### 4. 下载文档
```bash
curl -X GET http://localhost:8080/api/v1/documents/DOCUMENT_ID/download \
  -H "Authorization: YOUR_TOKEN" \
  -o downloaded.txt
```

### 5. 验证内容
```bash
cat downloaded.txt
```

应该与上传的文件内容一致。

## 切换回本地存储

如果需要切换回本地磁盘存储：

1. 修改 `application-dev.yml`：
```yaml
storage:
  type: local  # 改回 local
```

2. 重启应用

3. 运行原有验证脚本：
```powershell
pwsh scripts/verify-document-api.ps1
```

## 故障排查

### 问题1：MinIO 启动失败

**症状**：`minio.exe` 无法启动或报错

**解决**：
1. 检查端口是否被占用：`netstat -ano | findstr :9000`
2. 检查数据目录权限
3. 查看 MinIO 错误日志

### 问题2：应用连接 MinIO 失败

**症状**：应用启动时报错 "MinIO 初始化失败"

**解决**：
1. 确认 MinIO 已启动：访问 http://localhost:9000/minio/health/live
2. 检查配置文件中的 endpoint、access-key、secret-key
3. 查看应用详细错误日志

### 问题3：上传成功但下载失败

**症状**：上传返回成功，但下载时报 "文件不存在"

**解决**：
1. 在 MinIO 控制台中检查文件是否真的存在
2. 检查 storage_key 格式是否正确
3. 检查存储桶名称是否匹配

## 性能对比

### 本地磁盘 vs MinIO

| 指标 | 本地磁盘 | MinIO |
|------|---------|-------|
| 上传速度 | 快 | 中等（网络开销） |
| 下载速度 | 快 | 中等（网络开销） |
| 可扩展性 | 差（单机限制） | 好（分布式） |
| 高可用性 | 差（单点故障） | 好（多副本） |
| 运维成本 | 低 | 中等 |
| 适用场景 | 开发测试 | 生产环境 |

## 下一步

完成 MinIO 集成测试后，可以考虑：

1. **生产部署**：部署 MinIO 集群到生产环境
2. **数据迁移**：将现有数据从本地磁盘迁移到 MinIO
3. **CDN 集成**：在 MinIO 前加 CDN 加速下载
4. **监控告警**：集成 MinIO 监控指标
