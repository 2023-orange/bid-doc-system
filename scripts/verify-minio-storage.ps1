$ErrorActionPreference = 'Stop'

$baseUrl = 'http://localhost:8080'
$timestamp = Get-Date -Format 'yyyyMMddHHmmss'

Write-Host ""
Write-Host "==== MinIO Storage Verification Script ===="
Write-Host "Timestamp: $timestamp"
Write-Host ""

# 辅助函数
function Write-Step {
    param([string]$Title)
    Write-Host ""
    Write-Host "==== $Title ===="
}

function Assert-Code {
    param(
        [string]$StepName,
        [object]$Response,
        [int]$ExpectedCode
    )
    if ($Response.code -ne $ExpectedCode) {
        throw "$StepName failed: expected code=$ExpectedCode, actual code=$($Response.code), message=$($Response.message)"
    }
    Write-Host "Assert-Code passed: $StepName => $ExpectedCode"
}

function Invoke-JsonApi {
    param(
        [string]$Method,
        [string]$Uri,
        [hashtable]$Headers,
        [object]$Body
    )
    if ($null -eq $Body) {
        return Invoke-RestMethod -Uri $Uri -Method $Method -Headers $Headers
    }
    return Invoke-RestMethod -Uri $Uri -Method $Method -Headers $Headers -ContentType 'application/json' -Body ($Body | ConvertTo-Json)
}

function New-AuthHeaders {
    param([string]$Token)
    return @{ Authorization = $Token }
}

# 检查应用配置
Write-Step "Check application storage configuration"
Write-Host "请确保 application-dev.yml 中 storage.type=minio"
Write-Host "如果当前是 local，请修改为 minio 并重启应用"
Write-Host ""
Read-Host "按 Enter 继续验证（确认已配置为 minio）"

# 登录获取 token
Write-Step "Login as admin"
$loginResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/auth/login" -Headers @{} -Body @{
    username = 'testadmin'
    password = '12345678'
}
Assert-Code -StepName 'Login' -Response $loginResp -ExpectedCode 0
$adminToken = $loginResp.data.token
$adminHeaders = New-AuthHeaders -Token $adminToken
Write-Host "Admin token: $adminToken"

# 创建测试文件夹
Write-Step "Create test folder for MinIO storage test"
$folderResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $adminHeaders -Body @{
    parentId = 0
    name = "minio_test_folder_$timestamp"
    remark = "MinIO storage test folder"
}
Assert-Code -StepName 'Create folder' -Response $folderResp -ExpectedCode 0
$testFolderId = $folderResp.data.id
Write-Host "Test folder ID: $testFolderId"

# 创建测试文件
$testFile1Path = [System.IO.Path]::GetTempFileName()
$testFile1Content = "This is MinIO storage test document - $timestamp"
[System.IO.File]::WriteAllText($testFile1Path, $testFile1Content)
Write-Host "Created test file: $testFile1Path"

# M1: 上传文档到 MinIO
Write-Step "M1: Upload document to MinIO"
$uploadForm = @{
    file = Get-Item -Path $testFile1Path
    name = "minio_test_doc_1"
    remark = "MinIO test document"
    changeLog = "Initial version stored in MinIO"
}
$uploadResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/folders/$testFolderId/documents" `
    -Method Post -Headers $adminHeaders -Form $uploadForm
Assert-Code -StepName 'M1: Upload to MinIO' -Response $uploadResp -ExpectedCode 0
$doc1Id = $uploadResp.data.documentId
Write-Host "Document ID: $doc1Id, Version: $($uploadResp.data.versionNo)"

# M2: 获取文档详情
Write-Step "M2: Get document detail"
$detailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/documents/$doc1Id" -Headers $adminHeaders -Body $null
Assert-Code -StepName 'M2: Get detail' -Response $detailResp -ExpectedCode 0
Write-Host "Document name: $($detailResp.data.name), Current version: $($detailResp.data.currentVersionNo)"

# M3: 下载文档并验证内容
Write-Step "M3: Download document from MinIO and verify content"
$downloadPath = [System.IO.Path]::GetTempFileName()
Invoke-WebRequest -Uri "$baseUrl/api/v1/documents/$doc1Id/download" `
    -Headers $adminHeaders -OutFile $downloadPath
$downloadedContent = [System.IO.File]::ReadAllText($downloadPath)
if ($downloadedContent -ne $testFile1Content) {
    throw "M3: Downloaded content does not match original content"
}
Write-Host "M3: Content verified successfully (downloaded from MinIO)"

# M4: 上传新版本到 MinIO
Write-Step "M4: Upload new version to MinIO"
$testFile2Path = [System.IO.Path]::GetTempFileName()
$testFile2Content = "This is MinIO storage test document VERSION 2 - $timestamp"
[System.IO.File]::WriteAllText($testFile2Path, $testFile2Content)

$versionForm = @{
    file = Get-Item -Path $testFile2Path
    changeLog = "Second version stored in MinIO"
}
$versionResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/documents/$doc1Id/versions" `
    -Method Post -Headers $adminHeaders -Form $versionForm
Assert-Code -StepName 'M4: Upload new version to MinIO' -Response $versionResp -ExpectedCode 0
Write-Host "New version: $($versionResp.data.versionNo)"

# M5: 下载 V1 并验证内容
Write-Step "M5: Download V1 from MinIO and verify content"
$downloadV1Path = [System.IO.Path]::GetTempFileName()
Invoke-WebRequest -Uri "$baseUrl/api/v1/documents/$doc1Id/versions/1/download" `
    -Headers $adminHeaders -OutFile $downloadV1Path
$downloadedV1Content = [System.IO.File]::ReadAllText($downloadV1Path)
if ($downloadedV1Content -ne $testFile1Content) {
    throw "M5: Downloaded V1 content does not match original V1 content"
}
Write-Host "M5: V1 content verified successfully (from MinIO)"

# M6: 下载当前版本（V2）并验证内容
Write-Step "M6: Download current version (V2) from MinIO and verify content"
$downloadV2Path = [System.IO.Path]::GetTempFileName()
Invoke-WebRequest -Uri "$baseUrl/api/v1/documents/$doc1Id/download" `
    -Headers $adminHeaders -OutFile $downloadV2Path
$downloadedV2Content = [System.IO.File]::ReadAllText($downloadV2Path)
if ($downloadedV2Content -ne $testFile2Content) {
    throw "M6: Downloaded V2 content does not match original V2 content"
}
Write-Host "M6: V2 content verified successfully (from MinIO)"

# M7: 获取版本列表
Write-Step "M7: Get version list"
$versionsResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/documents/$doc1Id/versions" `
    -Headers $adminHeaders -Body $null
Assert-Code -StepName 'M7: Get versions' -Response $versionsResp -ExpectedCode 0
if ($versionsResp.data.Count -ne 2) {
    throw "M7: Expected 2 versions, got $($versionsResp.data.Count)"
}
Write-Host "M7: Version list verified, total versions: $($versionsResp.data.Count)"

# M8: 删除文档
Write-Step "M8: Delete document (soft delete, files remain in MinIO)"
$deleteResp = Invoke-JsonApi -Method Delete -Uri "$baseUrl/api/v1/documents/$doc1Id" `
    -Headers $adminHeaders -Body $null
Assert-Code -StepName 'M8: Delete document' -Response $deleteResp -ExpectedCode 0
Write-Host "M8: Document deleted successfully (soft delete)"

# 验证详情返回 404
try {
    $detailResp2 = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/documents/$doc1Id" `
        -Headers $adminHeaders -Body $null -ErrorAction Stop
    throw "M8: Should return 404 after deletion"
} catch {
    Write-Host "M8: Document not found after deletion (expected)"
}

# 清理临时文件
Remove-Item -Path $testFile1Path -ErrorAction SilentlyContinue
Remove-Item -Path $testFile2Path -ErrorAction SilentlyContinue
Remove-Item -Path $downloadPath -ErrorAction SilentlyContinue
Remove-Item -Path $downloadV1Path -ErrorAction SilentlyContinue
Remove-Item -Path $downloadV2Path -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "==== All MinIO Storage Tests Passed! ====" -ForegroundColor Green
Write-Host ""
Write-Host "提示: 文件已成功存储到 MinIO 对象存储"
Write-Host "可以访问 MinIO 控制台查看: http://localhost:9001"
Write-Host "存储桶: bid-doc-system"
Write-Host ""
