# MinIO 本地启动脚本
# 使用 Docker 启动 MinIO 服务用于测试

$ErrorActionPreference = 'Stop'

Write-Host ""
Write-Host "==== Starting MinIO with Docker ===="
Write-Host ""

# 检查 Docker 是否可用
try {
    docker --version | Out-Null
} catch {
    Write-Host "错误: Docker 未安装或未启动" -ForegroundColor Red
    Write-Host "请先安装 Docker Desktop: https://www.docker.com/products/docker-desktop"
    exit 1
}

# 停止并删除已存在的 MinIO 容器
Write-Host "清理已存在的 MinIO 容器..."
docker stop minio-test 2>$null
docker rm minio-test 2>$null

# 创建数据目录
$dataDir = "$PSScriptRoot\..\minio-data"
if (-not (Test-Path $dataDir)) {
    New-Item -ItemType Directory -Path $dataDir | Out-Null
    Write-Host "已创建数据目录: $dataDir"
}

# 启动 MinIO 容器
Write-Host ""
Write-Host "启动 MinIO 容器..."
docker run -d `
    --name minio-test `
    -p 9000:9000 `
    -p 9001:9001 `
    -e "MINIO_ROOT_USER=minioadmin" `
    -e "MINIO_ROOT_PASSWORD=minioadmin" `
    -v "${dataDir}:/data" `
    minio/minio server /data --console-address ":9001"

if ($LASTEXITCODE -ne 0) {
    Write-Host "MinIO 启动失败" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "==== MinIO 启动成功 ====" -ForegroundColor Green
Write-Host ""
Write-Host "API 端点:      http://localhost:9000"
Write-Host "控制台:        http://localhost:9001"
Write-Host "用户名:        minioadmin"
Write-Host "密码:          minioadmin"
Write-Host ""
Write-Host "停止 MinIO:    docker stop minio-test"
Write-Host "删除容器:      docker rm minio-test"
Write-Host ""
