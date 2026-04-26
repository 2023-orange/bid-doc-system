$ErrorActionPreference = 'Stop'

$baseUrl = 'http://localhost:8080'
$timestamp = Get-Date -Format 'yyyyMMddHHmmss'
$prefix = "folder_api_test_${timestamp}"

$rootName = "${prefix}_root"
$childName = "${prefix}_child"
$renamedChildName = "${prefix}_child_renamed"
$sameName = "${prefix}_same_name"
$employeeUsername = "api$($timestamp.Substring(4))"
$employeePassword = '12345678'

function Write-Step {
    param(
        [string]$Title
    )
    Write-Host ""
    Write-Host "==== $Title ===="
}

function Show-Response {
    param(
        [string]$Label,
        [object]$Response
    )
    Write-Host "API: $Label"
    $Response | ConvertTo-Json -Depth 10
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

function Assert-NonZero-Code {
    param(
        [string]$StepName,
        [object]$Response
    )
    if ($Response.code -eq 0) {
        throw "$StepName failed: expected non-zero error code, actual response=$($Response | ConvertTo-Json -Depth 10 -Compress)"
    }
    Write-Host "Assert-NonZero-Code passed: $StepName => $($Response.code)"
}

function Assert-Equal {
    param(
        [string]$StepName,
        [object]$Actual,
        [object]$Expected
    )
    if ("$Actual" -ne "$Expected") {
        throw "$StepName failed: expected=$Expected, actual=$Actual"
    }
    Write-Host "Assert-Equal passed: $StepName => $Expected"
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

Write-Step "Login as SUPER_ADMIN"
$loginResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/auth/login" -Headers @{} -Body @{
    username = 'testadmin'
    password = '12345678'
}
Show-Response -Label 'POST /api/v1/auth/login' -Response $loginResp
Assert-Code -StepName 'Login as SUPER_ADMIN' -Response $loginResp -ExpectedCode 0
$token = $loginResp.data.token
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "Login as SUPER_ADMIN failed: token missing"
}
$headers = @{
    Authorization = $token
}
$adminDeptId = $loginResp.data.user.deptId
$adminJobLevel = $loginResp.data.user.jobLevel
if ($null -eq $adminDeptId -or $null -eq $adminJobLevel) {
    throw "Login as SUPER_ADMIN failed: deptId or jobLevel missing in login response"
}
Write-Host "Token acquired, length=$($token.Length), deptId=$adminDeptId, jobLevel=$adminJobLevel"

Write-Step "Register ordinary api_verify user"
$registerResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/auth/register" -Headers @{} -Body @{
    username = $employeeUsername
    password = $employeePassword
    realName = "api verify ${timestamp}"
    email = "${employeeUsername}@example.com"
    mobile = "139$($timestamp.Substring($timestamp.Length - 8))"
    deptId = $adminDeptId
    jobLevel = $adminJobLevel
}
Show-Response -Label 'POST /api/v1/auth/register' -Response $registerResp
Assert-Code -StepName 'Register ordinary api_verify user' -Response $registerResp -ExpectedCode 0
$employeeUserId = $registerResp.data.userId
if ([string]::IsNullOrWhiteSpace([string]$employeeUserId)) {
    throw "Register ordinary api_verify user failed: userId missing"
}
Write-Host "employeeUserId=$employeeUserId"

Write-Step "Enable ordinary api_verify user"
$enableResp = Invoke-JsonApi -Method Patch -Uri "$baseUrl/api/v1/users/$employeeUserId/status" -Headers $headers -Body @{
    status = 1
}
Show-Response -Label "PATCH /api/v1/users/$employeeUserId/status" -Response $enableResp
Assert-Code -StepName 'Enable ordinary api_verify user' -Response $enableResp -ExpectedCode 0

Write-Step "Login as ordinary user"
$employeeLoginResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/auth/login" -Headers @{} -Body @{
    username = $employeeUsername
    password = $employeePassword
}
Show-Response -Label 'POST /api/v1/auth/login (ordinary user)' -Response $employeeLoginResp
Assert-Code -StepName 'Login as ordinary user' -Response $employeeLoginResp -ExpectedCode 0
$employeeToken = $employeeLoginResp.data.token
if ([string]::IsNullOrWhiteSpace($employeeToken)) {
    throw "Login as ordinary user failed: token missing"
}
$employeeHeaders = @{
    Authorization = $employeeToken
}

Write-Step "Create root folder"
$rootCreateResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $headers -Body @{
    parentId = 0
    name = $rootName
    remark = 'api verify root'
}
Show-Response -Label 'POST /api/v1/folders (root)' -Response $rootCreateResp
Assert-Code -StepName 'Create root folder' -Response $rootCreateResp -ExpectedCode 0
$rootFolderId = $rootCreateResp.data.id
if ([string]::IsNullOrWhiteSpace([string]$rootFolderId)) {
    throw "Create root folder failed: rootFolderId missing"
}
Write-Host "rootFolderId=$rootFolderId"

Write-Step "Ordinary user create root folder should fail"
$ordinaryRootResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $employeeHeaders -Body @{
    parentId = 0
    name = "${prefix}_ordinary_root"
    remark = 'api verify ordinary root'
}
Show-Response -Label 'POST /api/v1/folders (ordinary root)' -Response $ordinaryRootResp
Assert-Code -StepName 'Ordinary user create root folder should fail' -Response $ordinaryRootResp -ExpectedCode 4032001

Write-Step "Query root children"
$rootChildrenResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/children?parentId=0" -Headers $headers -Body $null
Show-Response -Label 'GET /api/v1/folders/children?parentId=0' -Response $rootChildrenResp
Assert-Code -StepName 'Query root children' -Response $rootChildrenResp -ExpectedCode 0
if (-not ($rootChildrenResp.data | Where-Object { "$($_.id)" -eq "$rootFolderId" })) {
    throw "Query root children failed: root folder not found"
}

Write-Step "Query root tree"
$rootTreeResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/tree/root" -Headers $headers -Body $null
Show-Response -Label 'GET /api/v1/folders/tree/root' -Response $rootTreeResp
Assert-Code -StepName 'Query root tree' -Response $rootTreeResp -ExpectedCode 0
if (-not ($rootTreeResp.data | Where-Object { "$($_.id)" -eq "$rootFolderId" })) {
    throw "Query root tree failed: root folder not found"
}

Write-Step "Create child folder"
$childCreateResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $headers -Body @{
    parentId = $rootFolderId
    name = $childName
    remark = 'api verify child'
}
Show-Response -Label 'POST /api/v1/folders (child)' -Response $childCreateResp
Assert-Code -StepName 'Create child folder' -Response $childCreateResp -ExpectedCode 0
$childFolderId = $childCreateResp.data.id
if ([string]::IsNullOrWhiteSpace([string]$childFolderId)) {
    throw "Create child folder failed: childFolderId missing"
}
Write-Host "childFolderId=$childFolderId"

Write-Step "Query child children"
$childChildrenResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/children?parentId=$rootFolderId" -Headers $headers -Body $null
Show-Response -Label "GET /api/v1/folders/children?parentId=$rootFolderId" -Response $childChildrenResp
Assert-Code -StepName 'Query child children' -Response $childChildrenResp -ExpectedCode 0
if (-not ($childChildrenResp.data | Where-Object { "$($_.id)" -eq "$childFolderId" })) {
    throw "Query child children failed: child folder not found"
}

Write-Step "Query child detail"
$childDetailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$childFolderId" -Headers $headers -Body $null
Show-Response -Label "GET /api/v1/folders/$childFolderId" -Response $childDetailResp
Assert-Code -StepName 'Query child detail' -Response $childDetailResp -ExpectedCode 0
$childSortNoBeforeUpdate = $childDetailResp.data.sortNo
$childParentIdBeforeUpdate = $childDetailResp.data.parentId
$childAncestorIdsBeforeUpdate = $childDetailResp.data.ancestorIds
$childLevelBeforeUpdate = $childDetailResp.data.level
$childNameBeforeUpdate = $childDetailResp.data.name

Write-Step "Rename child folder"
$renameResp = Invoke-JsonApi -Method Patch -Uri "$baseUrl/api/v1/folders/$childFolderId/name" -Headers $headers -Body @{
    name = $renamedChildName
}
Show-Response -Label "PATCH /api/v1/folders/$childFolderId/name" -Response $renameResp
Assert-Code -StepName 'Rename child folder' -Response $renameResp -ExpectedCode 0

Write-Step "Query renamed child detail"
$renamedDetailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$childFolderId" -Headers $headers -Body $null
Show-Response -Label "GET /api/v1/folders/$childFolderId after rename" -Response $renamedDetailResp
Assert-Code -StepName 'Query renamed child detail' -Response $renamedDetailResp -ExpectedCode 0
Assert-Equal -StepName 'Rename verification' -Actual $renamedDetailResp.data.name -Expected $renamedChildName

Write-Step "Update folder editable fields"
$updatedSortNo = [int]$childSortNoBeforeUpdate + 10
$updateResp = Invoke-JsonApi -Method Put -Uri "$baseUrl/api/v1/folders/$childFolderId" -Headers $headers -Body @{
    remark = 'api verify updated remark'
    status = 0
    inheritPermission = $false
    sortNo = $updatedSortNo
}
Show-Response -Label "PUT /api/v1/folders/$childFolderId" -Response $updateResp
Assert-Code -StepName 'Update folder editable fields' -Response $updateResp -ExpectedCode 0

Write-Step "Query updated folder detail"
$updatedDetailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$childFolderId" -Headers $headers -Body $null
Show-Response -Label "GET /api/v1/folders/$childFolderId after update" -Response $updatedDetailResp
Assert-Code -StepName 'Query updated folder detail' -Response $updatedDetailResp -ExpectedCode 0
Assert-Equal -StepName 'Updated remark verification' -Actual $updatedDetailResp.data.remark -Expected 'api verify updated remark'
Assert-Equal -StepName 'Updated status verification' -Actual $updatedDetailResp.data.status -Expected 0
Assert-Equal -StepName 'Updated inheritPermission verification' -Actual $updatedDetailResp.data.inheritPermission -Expected $false
Assert-Equal -StepName 'Updated sortNo verification' -Actual $updatedDetailResp.data.sortNo -Expected $updatedSortNo
Assert-Equal -StepName 'ParentId unchanged verification' -Actual $updatedDetailResp.data.parentId -Expected $childParentIdBeforeUpdate
Assert-Equal -StepName 'AncestorIds unchanged verification' -Actual $updatedDetailResp.data.ancestorIds -Expected $childAncestorIdsBeforeUpdate
Assert-Equal -StepName 'Level unchanged verification' -Actual $updatedDetailResp.data.level -Expected $childLevelBeforeUpdate
Assert-Equal -StepName 'Name unchanged after update verification' -Actual $updatedDetailResp.data.name -Expected $renamedChildName

Write-Step "Invalid status update should fail"
$invalidStatusResp = Invoke-JsonApi -Method Put -Uri "$baseUrl/api/v1/folders/$childFolderId" -Headers $headers -Body @{
    remark = 'api verify invalid status'
    status = 9
    inheritPermission = $true
    sortNo = 1
}
Show-Response -Label "PUT /api/v1/folders/$childFolderId invalid status" -Response $invalidStatusResp
Assert-Code -StepName 'Invalid status update should fail' -Response $invalidStatusResp -ExpectedCode 4001001

Write-Step "Duplicate sibling name should fail"
$duplicateResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $headers -Body @{
    parentId = $rootFolderId
    name = $renamedChildName
    remark = 'duplicate verify'
}
Show-Response -Label 'POST /api/v1/folders duplicate sibling' -Response $duplicateResp
Assert-NonZero-Code -StepName 'Duplicate sibling name validation' -Response $duplicateResp

Write-Step "Missing parent should fail"
$orphanResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $headers -Body @{
    parentId = 999999999999
    name = "${prefix}_orphan"
}
Show-Response -Label 'POST /api/v1/folders orphan' -Response $orphanResp
Assert-NonZero-Code -StepName 'Missing parent validation' -Response $orphanResp

Write-Step "Missing folder detail should fail"
$notFoundResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/999999999999" -Headers $headers -Body $null
Show-Response -Label 'GET /api/v1/folders/999999999999' -Response $notFoundResp
Assert-NonZero-Code -StepName 'Missing folder detail validation' -Response $notFoundResp

Write-Step "Sibling sortNo should auto increment by max + 1"
$sortParentResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $headers -Body @{
    parentId = 0
    name = "${prefix}_sort_parent"
    remark = 'api verify sort parent'
}
Show-Response -Label 'POST /api/v1/folders sort parent' -Response $sortParentResp
Assert-Code -StepName 'Create sort parent folder' -Response $sortParentResp -ExpectedCode 0
$sortParentId = $sortParentResp.data.id

$sortChild1Resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $headers -Body @{
    parentId = $sortParentId
    name = "${prefix}_sort_child_1"
    remark = 'api verify sort child 1'
}
Show-Response -Label 'POST /api/v1/folders sort child 1' -Response $sortChild1Resp
Assert-Code -StepName 'Create sort child 1' -Response $sortChild1Resp -ExpectedCode 0
$sortChild1Id = $sortChild1Resp.data.id

$sortChild2Resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $headers -Body @{
    parentId = $sortParentId
    name = "${prefix}_sort_child_2"
    remark = 'api verify sort child 2'
}
Show-Response -Label 'POST /api/v1/folders sort child 2' -Response $sortChild2Resp
Assert-Code -StepName 'Create sort child 2' -Response $sortChild2Resp -ExpectedCode 0
$sortChild2Id = $sortChild2Resp.data.id

$sortChild1DetailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$sortChild1Id" -Headers $headers -Body $null
$sortChild2DetailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$sortChild2Id" -Headers $headers -Body $null
Show-Response -Label "GET /api/v1/folders/$sortChild1Id" -Response $sortChild1DetailResp
Show-Response -Label "GET /api/v1/folders/$sortChild2Id" -Response $sortChild2DetailResp
Assert-Code -StepName 'Query sort child 1 detail' -Response $sortChild1DetailResp -ExpectedCode 0
Assert-Code -StepName 'Query sort child 2 detail' -Response $sortChild2DetailResp -ExpectedCode 0
Assert-Equal -StepName 'Sibling sortNo max+1 verification' -Actual $sortChild2DetailResp.data.sortNo -Expected ([int]$sortChild1DetailResp.data.sortNo + 1)

Write-Step "Same name under different parents should succeed"
$sameNameParent1Resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $headers -Body @{
    parentId = 0
    name = "${prefix}_same_parent_1"
    remark = 'api verify same name parent 1'
}
$sameNameParent2Resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $headers -Body @{
    parentId = 0
    name = "${prefix}_same_parent_2"
    remark = 'api verify same name parent 2'
}
Show-Response -Label 'POST /api/v1/folders same name parent 1' -Response $sameNameParent1Resp
Show-Response -Label 'POST /api/v1/folders same name parent 2' -Response $sameNameParent2Resp
Assert-Code -StepName 'Create same name parent 1' -Response $sameNameParent1Resp -ExpectedCode 0
Assert-Code -StepName 'Create same name parent 2' -Response $sameNameParent2Resp -ExpectedCode 0
$sameNameParent1Id = $sameNameParent1Resp.data.id
$sameNameParent2Id = $sameNameParent2Resp.data.id

$sameNameChild1Resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $headers -Body @{
    parentId = $sameNameParent1Id
    name = $sameName
    remark = 'api verify same name child 1'
}
$sameNameChild2Resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $headers -Body @{
    parentId = $sameNameParent2Id
    name = $sameName
    remark = 'api verify same name child 2'
}
Show-Response -Label 'POST /api/v1/folders same name child 1' -Response $sameNameChild1Resp
Show-Response -Label 'POST /api/v1/folders same name child 2' -Response $sameNameChild2Resp
Assert-Code -StepName 'Create same name child 1' -Response $sameNameChild1Resp -ExpectedCode 0
Assert-Code -StepName 'Create same name child 2' -Response $sameNameChild2Resp -ExpectedCode 0
$sameNameChild1Id = $sameNameChild1Resp.data.id
$sameNameChild2Id = $sameNameChild2Resp.data.id

$sameNameChild1DetailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$sameNameChild1Id" -Headers $headers -Body $null
$sameNameChild2DetailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$sameNameChild2Id" -Headers $headers -Body $null
Assert-Code -StepName 'Query same name child 1 detail' -Response $sameNameChild1DetailResp -ExpectedCode 0
Assert-Code -StepName 'Query same name child 2 detail' -Response $sameNameChild2DetailResp -ExpectedCode 0
Assert-Equal -StepName 'Same name child 1 name verification' -Actual $sameNameChild1DetailResp.data.name -Expected $sameName
Assert-Equal -StepName 'Same name child 2 name verification' -Actual $sameNameChild2DetailResp.data.name -Expected $sameName

Write-Step "Create level 1..8 then reject level 9"
$levelRootResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $headers -Body @{
    parentId = 0
    name = "${prefix}_level_root"
    remark = 'api verify level root'
}
Show-Response -Label 'POST /api/v1/folders level root' -Response $levelRootResp
Assert-Code -StepName 'Create level root' -Response $levelRootResp -ExpectedCode 0
$currentParentId = $levelRootResp.data.id
$levelFolderIds = @($currentParentId)
for ($level = 1; $level -le 8; $level++) {
    $levelResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $headers -Body @{
        parentId = $currentParentId
        name = "${prefix}_level_${level}"
        remark = "api verify level $level"
    }
    Show-Response -Label "POST /api/v1/folders level $level" -Response $levelResp
    Assert-Code -StepName "Create level $level folder" -Response $levelResp -ExpectedCode 0
    $currentParentId = $levelResp.data.id
    $levelFolderIds += $currentParentId
}
$levelExceededResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $headers -Body @{
    parentId = $currentParentId
    name = "${prefix}_level_9"
    remark = 'api verify level 9'
}
Show-Response -Label 'POST /api/v1/folders level 9' -Response $levelExceededResp
Assert-Code -StepName 'Create level 9 folder should fail' -Response $levelExceededResp -ExpectedCode 4002002

Write-Step "Verification finished"
Write-Host "prefix=$prefix"
Write-Host "employeeUsername=$employeeUsername"
Write-Host "employeeUserId=$employeeUserId"
Write-Host "rootFolderId=$rootFolderId"
Write-Host "childFolderId=$childFolderId"
Write-Host "sortParentId=$sortParentId"
Write-Host "sortChild1Id=$sortChild1Id"
Write-Host "sortChild2Id=$sortChild2Id"
Write-Host "sameNameParent1Id=$sameNameParent1Id"
Write-Host "sameNameParent2Id=$sameNameParent2Id"
Write-Host "sameNameChild1Id=$sameNameChild1Id"
Write-Host "sameNameChild2Id=$sameNameChild2Id"
Write-Host "levelFolderIds=$($levelFolderIds -join ',')"
