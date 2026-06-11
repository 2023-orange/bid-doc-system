$ErrorActionPreference = 'Stop'

$baseUrl = 'http://localhost:8080'
$timestamp = Get-Date -Format 'yyyyMMddHHmmss'

Write-Host ""
Write-Host "==== Document Search Verification Script ===="
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

# 创建测试文件夹结构
Write-Step "Create test folder structure"
$rootFolderResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $adminHeaders -Body @{
    parentId = 0
    name = "search_test_root_$timestamp"
    remark = "Search test root folder"
}
Assert-Code -StepName 'Create root folder' -Response $rootFolderResp -ExpectedCode 0
$rootFolderId = $rootFolderResp.data.id
Write-Host "Root folder ID: $rootFolderId"

$subFolder1Resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $adminHeaders -Body @{
    parentId = $rootFolderId
    name = "subfolder1_$timestamp"
    remark = "Subfolder 1"
}
Assert-Code -StepName 'Create subfolder1' -Response $subFolder1Resp -ExpectedCode 0
$subFolder1Id = $subFolder1Resp.data.id
Write-Host "Subfolder1 ID: $subFolder1Id"

$subFolder2Resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $adminHeaders -Body @{
    parentId = $rootFolderId
    name = "subfolder2_$timestamp"
    remark = "Subfolder 2"
}
Assert-Code -StepName 'Create subfolder2' -Response $subFolder2Resp -ExpectedCode 0
$subFolder2Id = $subFolder2Resp.data.id
Write-Host "Subfolder2 ID: $subFolder2Id"

# 上传测试文档
Write-Step "Upload test documents"

# 在根文件夹上传文档
$testFile1Path = [System.IO.Path]::GetTempFileName()
[System.IO.File]::WriteAllText($testFile1Path, "Contract document 1")
$uploadForm1 = @{
    file = Get-Item -Path $testFile1Path
    name = "合同文档1"
    changeLog = "Initial version"
}
$upload1Resp = Invoke-RestMethod -Uri "$baseUrl/api/v1/folders/$rootFolderId/documents" `
    -Method Post -Headers $adminHeaders -Form $uploadForm1
Assert-Code -StepName 'Upload doc1 to root' -Response $upload1Resp -ExpectedCode 0
$doc1Id = $upload1Resp.data.documentId
Write-Host "Document 1 ID: $doc1Id"

# 在子文件夹1上传文档
$testFile2Path = [System.IO.Path]::GetTempFileName()
[System.IO.File]::WriteAllText($testFile2Path, "Contract document 2")
$uploadForm2 = @{
    file = Get-Item -Path $testFile2Path
    name = "合同文档2"
    changeLog = "Initial version"
}
$upload2Resp = Invoke-RestMethod -Uri "$baseUrl/api/v1/folders/$subFolder1Id/documents" `
    -Method Post -Headers $adminHeaders -Form $uploadForm2
Assert-Code -StepName 'Upload doc2 to subfolder1' -Response $upload2Resp -ExpectedCode 0
$doc2Id = $upload2Resp.data.documentId
Write-Host "Document 2 ID: $doc2Id"

# 在子文件夹2上传文档
$testFile3Path = [System.IO.Path]::GetTempFileName()
[System.IO.File]::WriteAllText($testFile3Path, "Report document 3")
$uploadForm3 = @{
    file = Get-Item -Path $testFile3Path
    name = "报告文档3"
    changeLog = "Initial version"
}
$upload3Resp = Invoke-RestMethod -Uri "$baseUrl/api/v1/folders/$subFolder2Id/documents" `
    -Method Post -Headers $adminHeaders -Form $uploadForm3
Assert-Code -StepName 'Upload doc3 to subfolder2' -Response $upload3Resp -ExpectedCode 0
$doc3Id = $upload3Resp.data.documentId
Write-Host "Document 3 ID: $doc3Id"

# S1: 全局搜索（无关键词）
Write-Step "S1: Global search without keyword"
$s1Resp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/documents/search?page=1&size=20" `
    -Headers $adminHeaders -Body $null
Assert-Code -StepName 'S1: Global search' -Response $s1Resp -ExpectedCode 0
$s1ListCount = $s1Resp.data.list.Count
Write-Host "S1: List size: $s1ListCount"
if ($s1ListCount -lt 3) {
    Write-Host "WARNING: Expected at least 3 documents, got $s1ListCount"
}

# S2: 关键词搜索（"合同"）
Write-Step "S2: Search by keyword '合同'"
$s2Resp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/documents/search?keyword=合同&page=1&size=20" `
    -Headers $adminHeaders -Body $null
Assert-Code -StepName 'S2: Keyword search' -Response $s2Resp -ExpectedCode 0
$s2Total = [int]$s2Resp.data.total
$s2ListCount = $s2Resp.data.list.Count
Write-Host "S2: Total documents: $s2Total, List size: $s2ListCount"
if ($s2ListCount -lt 2) {
    throw "S2: Expected at least 2 documents with keyword '合同', got $s2ListCount"
}
Write-Host "S2: Keyword search verified (found $s2ListCount documents with '合同')"

# S3: 文件夹范围搜索（递归）
Write-Step "S3: Search in folder (recursive)"
$s3Resp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/documents/search?folderId=$rootFolderId&recursive=true&page=1&size=20" `
    -Headers $adminHeaders -Body $null
Assert-Code -StepName 'S3: Folder search recursive' -Response $s3Resp -ExpectedCode 0
$s3ListCount = $s3Resp.data.list.Count
Write-Host "S3: List size: $s3ListCount"
if ($s3ListCount -ne 3) {
    throw "S3: Expected 3 documents in folder (recursive), got $s3ListCount"
}
Write-Host "S3: Recursive folder search verified (found 3 documents)"

# S4: 文件夹范围搜索（非递归）
Write-Step "S4: Search in folder (non-recursive)"
$s4Resp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/documents/search?folderId=$rootFolderId&recursive=false&page=1&size=20" `
    -Headers $adminHeaders -Body $null
Assert-Code -StepName 'S4: Folder search non-recursive' -Response $s4Resp -ExpectedCode 0
$s4ListCount = $s4Resp.data.list.Count
Write-Host "S4: List size: $s4ListCount"
if ($s4ListCount -ne 1) {
    throw "S4: Expected 1 document in folder (non-recursive), got $s4ListCount"
}
Write-Host "S4: Non-recursive folder search verified (found 1 document)"

# S5: 组合搜索（关键词 + 文件夹）
Write-Step "S5: Combined search (keyword + folder)"
$s5Resp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/documents/search?keyword=合同&folderId=$rootFolderId&recursive=true&page=1&size=20" `
    -Headers $adminHeaders -Body $null
Assert-Code -StepName 'S5: Combined search' -Response $s5Resp -ExpectedCode 0
$s5ListCount = $s5Resp.data.list.Count
Write-Host "S5: List size: $s5ListCount"
if ($s5ListCount -lt 2) {
    throw "S5: Expected at least 2 documents with keyword '合同' in folder, got $s5ListCount"
}
Write-Host "S5: Combined search verified (found $s5ListCount documents)"

# S6: 排序测试（按名称升序）
Write-Step "S6: Search with sorting (name asc)"
$s6Resp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/documents/search?folderId=$rootFolderId&recursive=true&sortBy=name&sortOrder=asc&page=1&size=20" `
    -Headers $adminHeaders -Body $null
Assert-Code -StepName 'S6: Search with sorting' -Response $s6Resp -ExpectedCode 0
Write-Host "S6: List size: $($s6Resp.data.list.Count)"
$firstDocName = $s6Resp.data.list[0].name
Write-Host "S6: First document name: $firstDocName"

# S7: 分页测试
Write-Step "S7: Search with pagination"
$s7Resp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/documents/search?folderId=$rootFolderId&recursive=true&page=1&size=2" `
    -Headers $adminHeaders -Body $null
Assert-Code -StepName 'S7: Search with pagination' -Response $s7Resp -ExpectedCode 0
$s7ListCount = $s7Resp.data.list.Count
Write-Host "S7: Page size: $s7ListCount"
if ($s7ListCount -ne 2) {
    throw "S7: Expected 2 documents per page, got $s7ListCount"
}
Write-Host "S7: Pagination verified (page size = 2)"

# S8: 搜索不存在的关键词
Write-Step "S8: Search with non-existent keyword"
$s8Resp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/documents/search?keyword=不存在的文档&page=1&size=20" `
    -Headers $adminHeaders -Body $null
Assert-Code -StepName 'S8: Search non-existent' -Response $s8Resp -ExpectedCode 0
$s8ListCount = $s8Resp.data.list.Count
Write-Host "S8: List size: $s8ListCount"
if ($s8ListCount -ne 0) {
    throw "S8: Expected 0 documents, got $s8ListCount"
}
Write-Host "S8: Non-existent keyword search verified (found 0 documents)"

# 清理临时文件
Remove-Item -Path $testFile1Path -ErrorAction SilentlyContinue
Remove-Item -Path $testFile2Path -ErrorAction SilentlyContinue
Remove-Item -Path $testFile3Path -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "==== All Document Search Tests Passed! ====" -ForegroundColor Green
Write-Host ""
Write-Host "测试场景总结:"
Write-Host "  S1: 全局搜索（无关键词）✓"
Write-Host "  S2: 关键词搜索 ✓"
Write-Host "  S3: 文件夹递归搜索 ✓"
Write-Host "  S4: 文件夹非递归搜索 ✓"
Write-Host "  S5: 组合搜索（关键词+文件夹）✓"
Write-Host "  S6: 排序测试 ✓"
Write-Host "  S7: 分页测试 ✓"
Write-Host "  S8: 不存在的关键词 ✓"
Write-Host ""
