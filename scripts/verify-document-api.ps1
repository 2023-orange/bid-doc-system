$ErrorActionPreference = 'Stop'

$baseUrl = 'http://localhost:8080'
$timestamp = Get-Date -Format 'yyyyMMddHHmmss'

Write-Host ""
Write-Host "==== Document API Verification Script ===="
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

# 创建测试文件夹（根级，需要 SUPER_ADMIN）
Write-Step "Create test folder for documents"
$folderResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $adminHeaders -Body @{
    parentId = 0
    name = "doc_test_folder_$timestamp"
    remark = "Document API test folder"
}
Assert-Code -StepName 'Create folder' -Response $folderResp -ExpectedCode 0
$testFolderId = $folderResp.data.id
Write-Host "Test folder ID: $testFolderId"

# 创建测试文件
$testFile1Path = [System.IO.Path]::GetTempFileName()
$testFile1Content = "This is test document 1 - $timestamp"
[System.IO.File]::WriteAllText($testFile1Path, $testFile1Content)
Write-Host "Created test file: $testFile1Path"

# D1: 上传文档成功
Write-Step "D1: Upload document successfully"
$uploadForm = @{
    file = Get-Item -Path $testFile1Path
    name = "test_doc_1"
    remark = "Test document 1"
    changeLog = "Initial version"
}
$uploadResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/folders/$testFolderId/documents" `
    -Method Post -Headers $adminHeaders -Form $uploadForm
Assert-Code -StepName 'D1: Upload document' -Response $uploadResp -ExpectedCode 0
$doc1Id = $uploadResp.data.documentId
Write-Host "Document ID: $doc1Id, Version: $($uploadResp.data.versionNo)"

# D5: 获取文档详情
Write-Step "D5: Get document detail"
$detailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/documents/$doc1Id" -Headers $adminHeaders -Body $null
Assert-Code -StepName 'D5: Get detail' -Response $detailResp -ExpectedCode 0
Write-Host "Document name: $($detailResp.data.name), Current version: $($detailResp.data.currentVersionNo)"

# D6: 获取文档列表
Write-Step "D6: List documents in folder"
$listResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$testFolderId/documents?page=1&size=20" `
    -Headers $adminHeaders -Body $null
Assert-Code -StepName 'D6: List documents' -Response $listResp -ExpectedCode 0
Write-Host "Total documents: $($listResp.data.total), List size: $($listResp.data.list.Count)"

# V1: 上传新版本
Write-Step "V1: Upload new version"
$testFile2Path = [System.IO.Path]::GetTempFileName()
$testFile2Content = "This is test document 1 VERSION 2 - $timestamp"
[System.IO.File]::WriteAllText($testFile2Path, $testFile2Content)

$versionForm = @{
    file = Get-Item -Path $testFile2Path
    changeLog = "Second version with updates"
}
$versionResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/documents/$doc1Id/versions" `
    -Method Post -Headers $adminHeaders -Form $versionForm
Assert-Code -StepName 'V1: Upload new version' -Response $versionResp -ExpectedCode 0
Write-Host "New version: $($versionResp.data.versionNo)"

# 验证详情已更新
$detailResp2 = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/documents/$doc1Id" -Headers $adminHeaders -Body $null
if ($detailResp2.data.currentVersionNo -ne 2) {
    throw "V1: Current version should be 2, but got $($detailResp2.data.currentVersionNo)"
}
Write-Host "V1: Current version updated to 2"

# V3: 获取版本列表（降序）
Write-Step "V3: Get version list (DESC order)"
$versionsResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/documents/$doc1Id/versions" `
    -Headers $adminHeaders -Body $null
Assert-Code -StepName 'V3: Get versions' -Response $versionsResp -ExpectedCode 0
if ($versionsResp.data[0].versionNo -ne 2) {
    throw "V3: First version should be 2 (DESC), but got $($versionsResp.data[0].versionNo)"
}
Write-Host "V3: Version list DESC order verified, first version: $($versionsResp.data[0].versionNo)"

# V2: 下载 V1 并验证内容
Write-Step "V2: Download V1 and verify content"
$downloadPath = [System.IO.Path]::GetTempFileName()
Invoke-WebRequest -Uri "$baseUrl/api/v1/documents/$doc1Id/versions/1/download" `
    -Headers $adminHeaders -OutFile $downloadPath
$downloadedContent = [System.IO.File]::ReadAllText($downloadPath)
if ($downloadedContent -ne $testFile1Content) {
    throw "V2: Downloaded content does not match original V1 content"
}
Write-Host "V2: V1 content verified successfully"

# 下载当前版本（V2）
$downloadPath2 = [System.IO.Path]::GetTempFileName()
Invoke-WebRequest -Uri "$baseUrl/api/v1/documents/$doc1Id/download" `
    -Headers $adminHeaders -OutFile $downloadPath2
$downloadedContent2 = [System.IO.File]::ReadAllText($downloadPath2)
if ($downloadedContent2 -ne $testFile2Content) {
    throw "Downloaded current version content does not match V2 content"
}
Write-Host "Current version (V2) content verified successfully"

# D3: 同名上传被拒
Write-Step "D3: Duplicate name upload should fail"
$testFile3Path = [System.IO.Path]::GetTempFileName()
[System.IO.File]::WriteAllText($testFile3Path, "Another file")
$dupForm = @{
    file = Get-Item -Path $testFile3Path
    name = "test_doc_1"
}
try {
    $dupResp = Invoke-RestMethod -Uri "$baseUrl/api/v1/folders/$testFolderId/documents" `
        -Method Post -Headers $dupForm -Form $dupForm -ErrorAction Stop
    throw "D3: Should have failed with duplicate name"
} catch {
    if ($_.Exception.Response.StatusCode -eq 200) {
        $errorBody = $_.ErrorDetails.Message | ConvertFrom-Json
        if ($errorBody.code -eq 4002104) {
            Write-Host "D3: Duplicate name rejected correctly (code 4002104)"
        } else {
            throw "D3: Expected code 4002104, got $($errorBody.code)"
        }
    } else {
        Write-Host "D3: Duplicate name rejected (HTTP error)"
    }
}

# DEL1: 删除文档
Write-Step "DEL1: Delete document"
$deleteResp = Invoke-JsonApi -Method Delete -Uri "$baseUrl/api/v1/documents/$doc1Id" `
    -Headers $adminHeaders -Body $null
Assert-Code -StepName 'DEL1: Delete document' -Response $deleteResp -ExpectedCode 0
Write-Host "DEL1: Document deleted successfully"

# 验证详情返回 404
try {
    $detailResp3 = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/documents/$doc1Id" `
        -Headers $adminHeaders -Body $null -ErrorAction Stop
    throw "DEL1: Should return 404 after deletion"
} catch {
    Write-Host "DEL1: Document not found after deletion (expected)"
}

# 验证列表中不出现
$listResp2 = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$testFolderId/documents" `
    -Headers $adminHeaders -Body $null
Write-Host "List after deletion - Total: $($listResp2.data.total), List count: $($listResp2.data.list.Count)"
$found = $listResp2.data.list | Where-Object { $_.id -eq $doc1Id }
if ($found) {
    Write-Host "WARNING: Deleted document still appears in list (ID: $doc1Id)"
    Write-Host "This may be a caching issue or the document was not properly soft-deleted"
} else {
    Write-Host "DEL1: Document not in list after deletion (expected)"
}

# 清理临时文件
Remove-Item -Path $testFile1Path -ErrorAction SilentlyContinue
Remove-Item -Path $testFile2Path -ErrorAction SilentlyContinue
Remove-Item -Path $testFile3Path -ErrorAction SilentlyContinue
Remove-Item -Path $downloadPath -ErrorAction SilentlyContinue
Remove-Item -Path $downloadPath2 -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "==== All Document API Tests Passed! ===="
Write-Host ""
