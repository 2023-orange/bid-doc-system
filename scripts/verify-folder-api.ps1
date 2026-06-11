$ErrorActionPreference = 'Stop'

$baseUrl = 'http://localhost:8080'
$timestamp = Get-Date -Format 'yyyyMMddHHmmss'
$prefix = "folder_api_test_${timestamp}"
$nameSuffix = $timestamp.Substring($timestamp.Length - 6)

$rootName = "folder_api_test_root_$nameSuffix"
$childName = "folder_api_test_child_$nameSuffix"
$renamedChildName = "folder_api_test_child_renamed_$nameSuffix"
$sameName = 'folder_api_test_same_name'

$sameDeptUsername = "api_verifysd$($timestamp.Substring($timestamp.Length - 6))"
$otherDeptUsername = "api_verifyod$($timestamp.Substring($timestamp.Length - 6))"
$managerDescUsername = "api_verifymd$($timestamp.Substring($timestamp.Length - 6))"
$grantUserUsername = "api_verifygu$($timestamp.Substring($timestamp.Length - 6))"
$roleGrantUsername = "api_verifyrg$($timestamp.Substring($timestamp.Length - 6))"
$negativeDeptUsername = "api_verifynd$($timestamp.Substring($timestamp.Length - 6))"
$ordinaryPassword = '12345678'
$newDeptName = "api_verify_dept_primary_$nameSuffix"
$secondaryDeptName = "api_verify_dept_secondary_$nameSuffix"
$roleGrantCode = 'FOLDER_ADMIN'

$sameDeptRealName = '张三'
$otherDeptRealName = '李四'
$managerDescRealName = '王五'
$grantUserRealName = '赵六'
$roleGrantRealName = '孙七'
$negativeDeptRealName = '周八'

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

function Assert-True {
    param(
        [string]$StepName,
        [bool]$Actual
    )
    if (-not $Actual) {
        throw "$StepName failed: expected=true, actual=false"
    }
    Write-Host "Assert-True passed: $StepName => true"
}

function Assert-False {
    param(
        [string]$StepName,
        [bool]$Actual
    )
    if ($Actual) {
        throw "$StepName failed: expected=false, actual=true"
    }
    Write-Host "Assert-False passed: $StepName => false"
}

function Assert-NotContainsId {
    param(
        [string]$StepName,
        [object]$Response,
        [string]$UnexpectedId
    )
    if ($Response.data | Where-Object { "$($_.id)" -eq "$UnexpectedId" }) {
        throw "$StepName failed: unexpected id=$UnexpectedId found"
    }
    Write-Host "Assert-NotContainsId passed: $StepName => $UnexpectedId"
}

function Assert-ContainsId {
    param(
        [string]$StepName,
        [object]$Response,
        [string]$ExpectedId
    )
    if (-not ($Response.data | Where-Object { "$($_.id)" -eq "$ExpectedId" })) {
        throw "$StepName failed: expected id=$ExpectedId not found"
    }
    Write-Host "Assert-ContainsId passed: $StepName => $ExpectedId"
}

function Get-ItemById {
    param(
        [object[]]$Items,
        [object]$Id
    )
    return $Items | Where-Object { "$($_.id)" -eq "$Id" } | Select-Object -First 1
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

function Login-User {
    param(
        [string]$Username,
        [string]$Password,
        [string]$Label
    )

    $resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/auth/login" -Headers @{} -Body @{
        username = $Username
        password = $Password
    }
    Show-Response -Label $Label -Response $resp
    return $resp
}

function New-AuthHeaders {
    param(
        [string]$Token
    )

    return @{
        Authorization = $Token
    }
}

function Register-User {
    param(
        [string]$Username,
        [string]$Password,
        [string]$RealName,
        [string]$Email,
        [string]$Mobile,
        [object]$DeptId,
        [object]$JobLevel
    )

    return Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/auth/register" -Headers @{} -Body @{
        username = $Username
        password = $Password
        realName = $RealName
        email = $Email
        mobile = $Mobile
        deptId = $DeptId
        jobLevel = $JobLevel
    }
}

function Update-UserStatus {
    param(
        [hashtable]$Headers,
        [object]$UserId,
        [int]$Status,
        [string]$Label
    )

    return Invoke-JsonApi -Method Patch -Uri "$baseUrl/api/v1/users/$UserId/status" -Headers $Headers -Body @{
        status = $Status
    }
}

function Create-Folder {
    param(
        [hashtable]$Headers,
        [object]$ParentId,
        [string]$Name,
        [string]$Remark,
        [string]$Label
    )

    $resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $Headers -Body @{
        parentId = $ParentId
        name = $Name
        remark = $Remark
    }
    Show-Response -Label $Label -Response $resp
    return $resp
}

function Rename-Folder {
    param(
        [hashtable]$Headers,
        [object]$FolderId,
        [string]$Name,
        [string]$Label
    )

    $resp = Invoke-JsonApi -Method Patch -Uri "$baseUrl/api/v1/folders/$FolderId/name" -Headers $Headers -Body @{
        name = $Name
    }
    Show-Response -Label $Label -Response $resp
    return $resp
}

function Update-Folder {
    param(
        [hashtable]$Headers,
        [object]$FolderId,
        [string]$Remark,
        [int]$Status,
        [bool]$InheritPermission,
        [int]$SortNo,
        [string]$Label
    )

    $resp = Invoke-JsonApi -Method Put -Uri "$baseUrl/api/v1/folders/$FolderId" -Headers $Headers -Body @{
        remark = $Remark
        status = $Status
        inheritPermission = $InheritPermission
        sortNo = $SortNo
    }
    Show-Response -Label $Label -Response $resp
    return $resp
}

function Assign-Role {
    param(
        [hashtable]$Headers,
        [object]$UserId,
        [string]$RoleCode,
        [string]$Label
    )

    $resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/roles/assign" -Headers $Headers -Body @{
        userId = $UserId
        roleCode = $RoleCode
        isPrimary = $false
    }
    Show-Response -Label $Label -Response $resp
    return $resp
}

function Stop-ForRoleAssignFailure {
    param(
        [string]$RequestPath,
        [object]$RequestBody,
        [object]$Response
    )

    Write-Host ''
    Write-Host 'ROLE_SCENARIO_BLOCKED'
    Write-Host "requestPath=$RequestPath"
    Write-Host 'requestBody='
    $RequestBody | ConvertTo-Json -Depth 10
    Write-Host "responseCode=$($Response.code)"
    Write-Host 'responseBody='
    $Response | ConvertTo-Json -Depth 10
    Write-Host 'failureReason=POST /api/v1/roles/assign 未成功，当前脚本按约定暂停 ROLE 场景，不直接写 sys_user_role。'
    Write-Host 'fallbackPlan=等待用户确认后，再决定是否改为非破坏性 INSERT 写入 sys_user_role 测试数据。'
    throw 'ROLE_SCENARIO_BLOCKED'
}

function Get-DbConfig {
    # Goal-0 Phase 3: 配置已抽离到 application-dev.yml
    $applicationDevPath = Join-Path $PSScriptRoot '..\src\main\resources\application-dev.yml'
    if (-not (Test-Path $applicationDevPath)) {
        throw "Resolve datasource config failed: application-dev.yml not found at $applicationDevPath"
    }
    $applicationContent = Get-Content -Path $applicationDevPath -Raw
    $datasourceMatch = [regex]::Match(
        $applicationContent,
        '(?ms)datasource:\s*.*?url:\s*(?<url>[^\r\n]+)\s*.*?username:\s*(?<username>[^\r\n]+)\s*.*?password:\s*(?<password>[^\r\n]+)\s*'
    )
    if (-not $datasourceMatch.Success) {
        throw 'Resolve datasource config failed: datasource url/username/password not found in application-dev.yml'
    }

    return @{
        Url = $datasourceMatch.Groups['url'].Value.Trim()
        Username = $datasourceMatch.Groups['username'].Value.Trim().Trim('"')
        Password = $datasourceMatch.Groups['password'].Value.Trim().Trim('"')
    }
}

function Convert-ToJavaStringLiteral {
    param(
        [string]$Value
    )
    if ($null -eq $Value) {
        return ''
    }
    return $Value.Replace('\', '\\').Replace('"', '\"')
}

function New-TestId {
    $epochMs = [int64]([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())
    $randomSuffix = Get-Random -Minimum 100 -Maximum 999
    return [int64]("$epochMs$randomSuffix")
}

function Invoke-DbUpdate {
    param(
        [string]$Label,
        [string]$Sql
    )

    if (-not $script:DbConfig) {
        $script:DbConfig = Get-DbConfig
    }
    if (-not $script:PostgresJar) {
        $script:PostgresJar = Get-ChildItem -Path "$env:USERPROFILE\.m2\repository\org\postgresql\postgresql" -Recurse -Filter 'postgresql-*.jar' |
            Sort-Object FullName -Descending |
            Select-Object -First 1 -ExpandProperty FullName
    }
    if ([string]::IsNullOrWhiteSpace($script:PostgresJar)) {
        throw 'Resolve PostgreSQL JDBC jar failed'
    }

    $runnerDir = Join-Path $PSScriptRoot '..\.verify-logs'
    if (-not (Test-Path $runnerDir)) {
        New-Item -ItemType Directory -Path $runnerDir | Out-Null
    }
    $runnerPath = Join-Path $runnerDir "sql-runner-${timestamp}.jsh"

    $urlLiteral = Convert-ToJavaStringLiteral -Value $script:DbConfig.Url
    $userLiteral = Convert-ToJavaStringLiteral -Value $script:DbConfig.Username
    $passwordLiteral = Convert-ToJavaStringLiteral -Value $script:DbConfig.Password

    $runnerContent = @"
import java.sql.*;

String url = "$urlLiteral";
String username = "$userLiteral";
String password = "$passwordLiteral";
String sql = """
$Sql
""";

try (Connection conn = DriverManager.getConnection(url, username, password);
     Statement stmt = conn.createStatement()) {
    int updateCount = stmt.executeUpdate(sql);
    System.out.println("SQL_OK updateCount=" + updateCount);
} catch (Exception e) {
    System.out.println("SQL_ERROR " + e.getClass().getName() + ": " + e.getMessage());
    throw e;
}
"/exit"
"@

    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($runnerPath, $runnerContent, $utf8NoBom)
    $stdoutPath = Join-Path $runnerDir "jshell-sql-${timestamp}.out.log"
    $stderrPath = Join-Path $runnerDir "jshell-sql-${timestamp}.err.log"
    $process = Start-Process -FilePath 'jshell' `
        -ArgumentList @('--class-path', $script:PostgresJar, $runnerPath) `
        -NoNewWindow `
        -Wait `
        -PassThru `
        -RedirectStandardOutput $stdoutPath `
        -RedirectStandardError $stderrPath
    $output = ''
    if (Test-Path $stdoutPath) {
        $output += (Get-Content $stdoutPath -Raw)
    }
    if (Test-Path $stderrPath) {
        $stderrContent = Get-Content $stderrPath -Raw
        if (-not [string]::IsNullOrWhiteSpace($stderrContent)) {
            $output += [Environment]::NewLine + $stderrContent
        }
    }
    Write-Host "DB: $Label"
    Write-Host $output
    if ($process.ExitCode -ne 0 -or $output -notmatch 'SQL_OK updateCount=') {
        throw "$Label failed: database update did not report success"
    }
}

function Invoke-DbQuery {
    param(
        [string]$Label,
        [string]$Sql,
        [switch]$PassThruOutput
    )

    if (-not $script:DbConfig) {
        $script:DbConfig = Get-DbConfig
    }
    if (-not $script:PostgresJar) {
        $script:PostgresJar = Get-ChildItem -Path "$env:USERPROFILE\.m2\repository\org\postgresql\postgresql" -Recurse -Filter 'postgresql-*.jar' |
            Sort-Object FullName -Descending |
            Select-Object -First 1 -ExpandProperty FullName
    }
    if ([string]::IsNullOrWhiteSpace($script:PostgresJar)) {
        throw 'Resolve PostgreSQL JDBC jar failed'
    }

    $runnerDir = Join-Path $PSScriptRoot '..\.verify-logs'
    if (-not (Test-Path $runnerDir)) {
        New-Item -ItemType Directory -Path $runnerDir | Out-Null
    }
    $runnerPath = Join-Path $runnerDir "sql-query-runner-${timestamp}.jsh"

    $urlLiteral = Convert-ToJavaStringLiteral -Value $script:DbConfig.Url
    $userLiteral = Convert-ToJavaStringLiteral -Value $script:DbConfig.Username
    $passwordLiteral = Convert-ToJavaStringLiteral -Value $script:DbConfig.Password

    $runnerContent = @"
import java.sql.*;

String url = "$urlLiteral";
String username = "$userLiteral";
String password = "$passwordLiteral";
String sql = """
$Sql
""";

try (Connection conn = DriverManager.getConnection(url, username, password);
     Statement stmt = conn.createStatement();
     ResultSet rs = stmt.executeQuery(sql)) {
    ResultSetMetaData meta = rs.getMetaData();
    int columns = meta.getColumnCount();
    int rowCount = 0;
    while (rs.next()) {
        rowCount++;
        StringBuilder row = new StringBuilder();
        for (int i = 1; i <= columns; i++) {
            if (i > 1) {
                row.append(" | ");
            }
            row.append(meta.getColumnLabel(i)).append("=").append(rs.getString(i));
        }
        System.out.println("ROW " + row);
    }
    System.out.println("SQL_QUERY_OK rowCount=" + rowCount);
} catch (Exception e) {
    System.out.println("SQL_QUERY_ERROR " + e.getClass().getName() + ": " + e.getMessage());
    throw e;
}
"/exit"
"@

    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($runnerPath, $runnerContent, $utf8NoBom)
    $stdoutPath = Join-Path $runnerDir "jshell-sql-query-${timestamp}.out.log"
    $stderrPath = Join-Path $runnerDir "jshell-sql-query-${timestamp}.err.log"
    $process = Start-Process -FilePath 'jshell' `
        -ArgumentList @('--class-path', $script:PostgresJar, $runnerPath) `
        -NoNewWindow `
        -Wait `
        -PassThru `
        -RedirectStandardOutput $stdoutPath `
        -RedirectStandardError $stderrPath
    $output = ''
    if (Test-Path $stdoutPath) {
        $output += (Get-Content $stdoutPath -Raw)
    }
    if (Test-Path $stderrPath) {
        $stderrContent = Get-Content $stderrPath -Raw
        if (-not [string]::IsNullOrWhiteSpace($stderrContent)) {
            $output += [Environment]::NewLine + $stderrContent
        }
    }
    Write-Host "DB: $Label"
    Write-Host $output
    if ($process.ExitCode -ne 0 -or $output -notmatch 'SQL_QUERY_OK rowCount=') {
        throw "$Label failed: database query did not report success"
    }
    # 仅在显式请求时返回原始 stdout，避免破坏已有 void 调用方
    if ($PassThruOutput) {
        return $output
    }
}

# ============================================================
# DB 断言助手：基于 Invoke-DbQuery -PassThruOutput 的 stdout 解析
# 输出行格式由 jshell runner 决定（见 Invoke-DbQuery 内部）：
#   ROW col1=val1 | col2=val2 | col3=val3
# 末尾还有一行 SQL_QUERY_OK rowCount=N，本助手只关心 "^ROW " 行
# ============================================================

function Assert-DbRowCount {
    param(
        [string]$Label,
        [string]$Output,
        [int]$MinRows
    )
    $rowLines = @($Output -split "`r?`n" | Where-Object { $_ -match '^ROW ' })
    if ($rowLines.Count -lt $MinRows) {
        throw "$Label failed: expected at least $MinRows rows, actual=$($rowLines.Count)"
    }
    Write-Host "Assert-DbRowCount passed: $Label => $($rowLines.Count) >= $MinRows"
}

function Assert-DbRowMatch {
    param(
        [string]$Label,
        [string]$Output,
        [string]$RowSelector
    )
    $rowLines = @($Output -split "`r?`n" | Where-Object { $_ -match '^ROW ' })
    $matched = $rowLines | Where-Object { $_.Contains($RowSelector) }
    if (-not $matched) {
        throw "$Label failed: no ROW matched selector '$RowSelector'"
    }
    Write-Host "Assert-DbRowMatch passed: $Label => matched '$RowSelector'"
}

function Assert-DbCell {
    param(
        [string]$Label,
        [string]$Output,
        [string]$RowSelector,
        [string]$ColumnName,
        [string]$Expected
    )
    $rowLines = @($Output -split "`r?`n" | Where-Object { $_ -match '^ROW ' })
    $matched = $rowLines | Where-Object { $_.Contains($RowSelector) } | Select-Object -First 1
    if (-not $matched) {
        throw "$Label failed: no ROW matched selector '$RowSelector'"
    }
    # 去掉 "ROW " 前缀后按 " | " 拆列对；避免值含特殊字符时正则误判
    $payload = $matched -replace '^ROW ', ''
    $columnPairs = $payload -split '\s\|\s'
    $actual = $null
    foreach ($pair in $columnPairs) {
        $eqIdx = $pair.IndexOf('=')
        if ($eqIdx -lt 0) { continue }
        $name = $pair.Substring(0, $eqIdx)
        $val = $pair.Substring($eqIdx + 1)
        if ($name -eq $ColumnName) {
            $actual = $val
            break
        }
    }
    if ($null -eq $actual) {
        throw "$Label failed: column '$ColumnName' not found in row '$matched'"
    }
    if ("$actual" -ne "$Expected") {
        throw "$Label failed: expected $ColumnName='$Expected', actual='$actual'"
    }
    Write-Host "Assert-DbCell passed: $Label => $ColumnName='$Expected'"
}

Write-Step "Login as SUPER_ADMIN"
$loginResp = Login-User -Username 'testadmin' -Password '12345678' -Label 'POST /api/v1/auth/login (SUPER_ADMIN)'
Assert-Code -StepName 'Login as SUPER_ADMIN' -Response $loginResp -ExpectedCode 0
$adminToken = $loginResp.data.token
if ([string]::IsNullOrWhiteSpace($adminToken)) {
    throw "Login as SUPER_ADMIN failed: token missing"
}
$adminHeaders = New-AuthHeaders -Token $adminToken
$adminDeptId = $loginResp.data.user.deptId
$adminJobLevel = $loginResp.data.user.jobLevel
Write-Host "adminDeptId=$adminDeptId, adminJobLevel=$adminJobLevel"

Write-Step "Create verification department for other-dept user"
$createDeptResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/departments" -Headers $adminHeaders -Body @{
    name = $newDeptName
    parentId = $adminDeptId
    level = 2
    remark = '接口联调用测试部门'
}
Show-Response -Label 'POST /api/v1/departments' -Response $createDeptResp
Assert-Code -StepName 'Create verification department' -Response $createDeptResp -ExpectedCode 0

Write-Step "Query departments and resolve new department id"
$deptListResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/departments" -Headers $adminHeaders -Body $null
Show-Response -Label 'GET /api/v1/departments' -Response $deptListResp
Assert-Code -StepName 'Query departments' -Response $deptListResp -ExpectedCode 0
$otherDept = $deptListResp.data | Where-Object { $_.name -eq $newDeptName } | Select-Object -First 1
if ($null -eq $otherDept) {
    throw "Resolve verification department id failed: dept not found by name=$newDeptName"
}
$otherDeptId = $otherDept.id
Write-Host "otherDeptId=$otherDeptId"

Write-Step "Register same-dept user"
$sameDeptRegisterResp = Register-User `
    -Username $sameDeptUsername `
    -Password $ordinaryPassword `
    -RealName $sameDeptRealName `
    -Email "${sameDeptUsername}@example.com" `
    -Mobile "139$($timestamp.Substring($timestamp.Length - 8))" `
    -DeptId $adminDeptId `
    -JobLevel $adminJobLevel
Show-Response -Label 'POST /api/v1/auth/register (same dept user)' -Response $sameDeptRegisterResp
Assert-Code -StepName 'Register same-dept user' -Response $sameDeptRegisterResp -ExpectedCode 0
$sameDeptUserId = $sameDeptRegisterResp.data.userId

Write-Step "Register other-dept user"
$otherDeptRegisterResp = Register-User `
    -Username $otherDeptUsername `
    -Password $ordinaryPassword `
    -RealName $otherDeptRealName `
    -Email "${otherDeptUsername}@example.com" `
    -Mobile "138$($timestamp.Substring($timestamp.Length - 8))" `
    -DeptId $otherDeptId `
    -JobLevel $adminJobLevel
Show-Response -Label 'POST /api/v1/auth/register (other dept user)' -Response $otherDeptRegisterResp
Assert-Code -StepName 'Register other-dept user' -Response $otherDeptRegisterResp -ExpectedCode 0
$otherDeptUserId = $otherDeptRegisterResp.data.userId

Write-Step "Enable same-dept user"
$enableSameDeptResp = Invoke-JsonApi -Method Patch -Uri "$baseUrl/api/v1/users/$sameDeptUserId/status" -Headers $adminHeaders -Body @{
    status = 1
}
Show-Response -Label "PATCH /api/v1/users/$sameDeptUserId/status" -Response $enableSameDeptResp
Assert-Code -StepName 'Enable same-dept user' -Response $enableSameDeptResp -ExpectedCode 0

Write-Step "Enable other-dept user"
$enableOtherDeptResp = Invoke-JsonApi -Method Patch -Uri "$baseUrl/api/v1/users/$otherDeptUserId/status" -Headers $adminHeaders -Body @{
    status = 1
}
Show-Response -Label "PATCH /api/v1/users/$otherDeptUserId/status" -Response $enableOtherDeptResp
Assert-Code -StepName 'Enable other-dept user' -Response $enableOtherDeptResp -ExpectedCode 0

Write-Step "Login as same-dept user"
$sameDeptLoginResp = Login-User -Username $sameDeptUsername -Password $ordinaryPassword -Label 'POST /api/v1/auth/login (same dept user)'
Assert-Code -StepName 'Login as same-dept user' -Response $sameDeptLoginResp -ExpectedCode 0
$sameDeptHeaders = New-AuthHeaders -Token $sameDeptLoginResp.data.token

Write-Step "Login as other-dept user"
$otherDeptLoginResp = Login-User -Username $otherDeptUsername -Password $ordinaryPassword -Label 'POST /api/v1/auth/login (other dept user)'
Assert-Code -StepName 'Login as other-dept user' -Response $otherDeptLoginResp -ExpectedCode 0
$otherDeptHeaders = New-AuthHeaders -Token $otherDeptLoginResp.data.token

Write-Step "SUPER_ADMIN create root folder"
$rootCreateResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $adminHeaders -Body @{
    parentId = 0
    name = $rootName
    remark = '接口联调根目录'
}
Show-Response -Label 'POST /api/v1/folders (root)' -Response $rootCreateResp
Assert-Code -StepName 'SUPER_ADMIN create root folder' -Response $rootCreateResp -ExpectedCode 0
$rootFolderId = $rootCreateResp.data.id

Write-Step "Ordinary users create root folder should fail"
$sameDeptRootResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $sameDeptHeaders -Body @{
    parentId = 0
    name = "普通用户根目录尝试-$nameSuffix"
}
Show-Response -Label 'POST /api/v1/folders (same dept root)' -Response $sameDeptRootResp
Assert-Code -StepName 'Same-dept user create root folder should fail' -Response $sameDeptRootResp -ExpectedCode 4032001

$otherDeptRootResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $otherDeptHeaders -Body @{
    parentId = 0
    name = "跨部门根目录尝试-$nameSuffix"
}
Show-Response -Label 'POST /api/v1/folders (other dept root)' -Response $otherDeptRootResp
Assert-Code -StepName 'Other-dept user create root folder should fail' -Response $otherDeptRootResp -ExpectedCode 4032001

Write-Step "SUPER_ADMIN root tree contains created root"
$adminRootTreeResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/tree/root" -Headers $adminHeaders -Body $null
Show-Response -Label 'GET /api/v1/folders/tree/root (admin)' -Response $adminRootTreeResp
Assert-Code -StepName 'Admin root tree query' -Response $adminRootTreeResp -ExpectedCode 0
Assert-ContainsId -StepName 'Admin root tree contains root folder' -Response $adminRootTreeResp -ExpectedId $rootFolderId

Write-Step "Same-dept user can view visible root in root tree"
$sameDeptRootTreeResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/tree/root" -Headers $sameDeptHeaders -Body $null
Show-Response -Label 'GET /api/v1/folders/tree/root (same dept user)' -Response $sameDeptRootTreeResp
Assert-Code -StepName 'Same-dept root tree query' -Response $sameDeptRootTreeResp -ExpectedCode 0
Assert-ContainsId -StepName 'Same-dept root tree contains visible root' -Response $sameDeptRootTreeResp -ExpectedId $rootFolderId

Write-Step "Other-dept user root tree should not contain unauthorized root"
$otherDeptRootTreeResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/tree/root" -Headers $otherDeptHeaders -Body $null
Show-Response -Label 'GET /api/v1/folders/tree/root (other dept user)' -Response $otherDeptRootTreeResp
Assert-Code -StepName 'Other-dept root tree query' -Response $otherDeptRootTreeResp -ExpectedCode 0
Assert-NotContainsId -StepName 'Other-dept root tree hides unauthorized root' -Response $otherDeptRootTreeResp -UnexpectedId $rootFolderId

Write-Step "Same-dept user can view root detail"
$sameDeptRootDetailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$rootFolderId" -Headers $sameDeptHeaders -Body $null
Show-Response -Label "GET /api/v1/folders/$rootFolderId (same dept user)" -Response $sameDeptRootDetailResp
Assert-Code -StepName 'Same-dept root detail query' -Response $sameDeptRootDetailResp -ExpectedCode 0

Write-Step "Other-dept user cannot view unauthorized root detail"
$otherDeptRootDetailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$rootFolderId" -Headers $otherDeptHeaders -Body $null
Show-Response -Label "GET /api/v1/folders/$rootFolderId (other dept user)" -Response $otherDeptRootDetailResp
Assert-Code -StepName 'Other-dept root detail should fail' -Response $otherDeptRootDetailResp -ExpectedCode 4032001

Write-Step "SUPER_ADMIN create child folder"
$childCreateResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $adminHeaders -Body @{
    parentId = $rootFolderId
    name = $childName
    remark = '接口联调子目录'
}
Show-Response -Label 'POST /api/v1/folders (child)' -Response $childCreateResp
Assert-Code -StepName 'SUPER_ADMIN create child folder' -Response $childCreateResp -ExpectedCode 0
$childFolderId = $childCreateResp.data.id

Write-Step "Same-dept user can see visible child in children list"
$sameDeptChildrenResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/children?parentId=$rootFolderId" -Headers $sameDeptHeaders -Body $null
Show-Response -Label "GET /api/v1/folders/children?parentId=$rootFolderId (same dept user)" -Response $sameDeptChildrenResp
Assert-Code -StepName 'Same-dept children query' -Response $sameDeptChildrenResp -ExpectedCode 0
Assert-ContainsId -StepName 'Same-dept children contains visible child' -Response $sameDeptChildrenResp -ExpectedId $childFolderId

Write-Step "Other-dept user children query on unauthorized parent should fail"
$otherDeptChildrenResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/children?parentId=$rootFolderId" -Headers $otherDeptHeaders -Body $null
Show-Response -Label "GET /api/v1/folders/children?parentId=$rootFolderId (other dept user)" -Response $otherDeptChildrenResp
Assert-Code -StepName 'Other-dept children query should fail' -Response $otherDeptChildrenResp -ExpectedCode 4032001

Write-Step "Other-dept user cannot rename unauthorized child"
$otherDeptRenameResp = Invoke-JsonApi -Method Patch -Uri "$baseUrl/api/v1/folders/$childFolderId/name" -Headers $otherDeptHeaders -Body @{
    name = "跨部门越权改名-$nameSuffix"
}
Show-Response -Label "PATCH /api/v1/folders/$childFolderId/name (other dept user)" -Response $otherDeptRenameResp
Assert-Code -StepName 'Other-dept rename should fail' -Response $otherDeptRenameResp -ExpectedCode 4032001

Write-Step "Other-dept user cannot edit unauthorized child"
$otherDeptUpdateResp = Invoke-JsonApi -Method Put -Uri "$baseUrl/api/v1/folders/$childFolderId" -Headers $otherDeptHeaders -Body @{
    remark = '跨部门越权修改'
    status = 1
    inheritPermission = $true
    sortNo = 2
}
Show-Response -Label "PUT /api/v1/folders/$childFolderId (other dept user)" -Response $otherDeptUpdateResp
Assert-Code -StepName 'Other-dept update should fail' -Response $otherDeptUpdateResp -ExpectedCode 4032001

Write-Step "Other-dept user cannot create child under unauthorized parent"
$otherDeptCreateChildResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $otherDeptHeaders -Body @{
    parentId = $rootFolderId
    name = "越权新建子目录-$nameSuffix"
    remark = '跨部门越权创建子目录'
}
Show-Response -Label 'POST /api/v1/folders (other dept child)' -Response $otherDeptCreateChildResp
Assert-Code -StepName 'Other-dept create child should fail' -Response $otherDeptCreateChildResp -ExpectedCode 4032001

Write-Step "Same-dept user cannot create child with view-only access"
$sameDeptCreateChildResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $sameDeptHeaders -Body @{
    parentId = $rootFolderId
    name = "同部门新建子目录-$nameSuffix"
    remark = '同部门用户尝试创建子目录'
}
Show-Response -Label 'POST /api/v1/folders (same dept child)' -Response $sameDeptCreateChildResp
Assert-Code -StepName 'Same-dept create child should fail' -Response $sameDeptCreateChildResp -ExpectedCode 4032001

Write-Step "SUPER_ADMIN query child detail"
$childDetailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$childFolderId" -Headers $adminHeaders -Body $null
Show-Response -Label "GET /api/v1/folders/$childFolderId (admin)" -Response $childDetailResp
Assert-Code -StepName 'Admin child detail query' -Response $childDetailResp -ExpectedCode 0
$childSortNoBeforeUpdate = $childDetailResp.data.sortNo
$childParentIdBeforeUpdate = $childDetailResp.data.parentId
$childAncestorIdsBeforeUpdate = $childDetailResp.data.ancestorIds
$childLevelBeforeUpdate = $childDetailResp.data.level

Write-Step "SUPER_ADMIN rename child folder"
$renameResp = Invoke-JsonApi -Method Patch -Uri "$baseUrl/api/v1/folders/$childFolderId/name" -Headers $adminHeaders -Body @{
    name = $renamedChildName
}
Show-Response -Label "PATCH /api/v1/folders/$childFolderId/name (admin)" -Response $renameResp
Assert-Code -StepName 'Admin rename child folder' -Response $renameResp -ExpectedCode 0

Write-Step "SUPER_ADMIN update child folder"
$updatedSortNo = [int]$childSortNoBeforeUpdate + 10
$updateResp = Invoke-JsonApi -Method Put -Uri "$baseUrl/api/v1/folders/$childFolderId" -Headers $adminHeaders -Body @{
    remark = 'api_verify_updated_remark'
    status = 0
    inheritPermission = $false
    sortNo = $updatedSortNo
}
Show-Response -Label "PUT /api/v1/folders/$childFolderId (admin)" -Response $updateResp
Assert-Code -StepName 'Admin update child folder' -Response $updateResp -ExpectedCode 0

Write-Step "SUPER_ADMIN query updated child detail"
$updatedDetailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$childFolderId" -Headers $adminHeaders -Body $null
Show-Response -Label "GET /api/v1/folders/$childFolderId after update (admin)" -Response $updatedDetailResp
Assert-Code -StepName 'Admin query updated child detail' -Response $updatedDetailResp -ExpectedCode 0
Assert-Equal -StepName 'Updated remark verification' -Actual $updatedDetailResp.data.remark -Expected 'api_verify_updated_remark'
Assert-Equal -StepName 'Updated status verification' -Actual $updatedDetailResp.data.status -Expected 0
Assert-Equal -StepName 'Updated inheritPermission verification' -Actual $updatedDetailResp.data.inheritPermission -Expected $false
Assert-Equal -StepName 'Updated sortNo verification' -Actual $updatedDetailResp.data.sortNo -Expected $updatedSortNo
Assert-Equal -StepName 'ParentId unchanged verification' -Actual $updatedDetailResp.data.parentId -Expected $childParentIdBeforeUpdate
Assert-Equal -StepName 'AncestorIds unchanged verification' -Actual $updatedDetailResp.data.ancestorIds -Expected $childAncestorIdsBeforeUpdate
Assert-Equal -StepName 'Level unchanged verification' -Actual $updatedDetailResp.data.level -Expected $childLevelBeforeUpdate
Assert-Equal -StepName 'Name changed by rename verification' -Actual $updatedDetailResp.data.name -Expected $renamedChildName

Write-Step "Invalid status update should fail"
$invalidStatusResp = Invoke-JsonApi -Method Put -Uri "$baseUrl/api/v1/folders/$childFolderId" -Headers $adminHeaders -Body @{
    remark = '非法状态值校验'
    status = 9
    inheritPermission = $true
    sortNo = 1
}
Show-Response -Label "PUT /api/v1/folders/$childFolderId invalid status (admin)" -Response $invalidStatusResp
Assert-Code -StepName 'Invalid status update should fail' -Response $invalidStatusResp -ExpectedCode 4001001

Write-Step "Sibling sortNo should auto increment by max + 1"
$sortParentResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $adminHeaders -Body @{
    parentId = 0
    name = "folder_api_test_sort_parent_$nameSuffix"
    remark = 'api_verify_sort_parent'
}
Show-Response -Label 'POST /api/v1/folders sort parent' -Response $sortParentResp
Assert-Code -StepName 'Create sort parent folder' -Response $sortParentResp -ExpectedCode 0
$sortParentId = $sortParentResp.data.id

$sortChild1Resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $adminHeaders -Body @{
    parentId = $sortParentId
    name = "folder_api_test_sort_child_1_$nameSuffix"
    remark = 'api_verify_sort_child_1'
}
Show-Response -Label 'POST /api/v1/folders sort child 1' -Response $sortChild1Resp
Assert-Code -StepName 'Create sort child 1' -Response $sortChild1Resp -ExpectedCode 0
$sortChild1Id = $sortChild1Resp.data.id

$sortChild2Resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $adminHeaders -Body @{
    parentId = $sortParentId
    name = "folder_api_test_sort_child_2_$nameSuffix"
    remark = 'api_verify_sort_child_2'
}
Show-Response -Label 'POST /api/v1/folders sort child 2' -Response $sortChild2Resp
Assert-Code -StepName 'Create sort child 2' -Response $sortChild2Resp -ExpectedCode 0
$sortChild2Id = $sortChild2Resp.data.id

$sortChild1DetailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$sortChild1Id" -Headers $adminHeaders -Body $null
$sortChild2DetailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$sortChild2Id" -Headers $adminHeaders -Body $null
Assert-Code -StepName 'Query sort child 1 detail' -Response $sortChild1DetailResp -ExpectedCode 0
Assert-Code -StepName 'Query sort child 2 detail' -Response $sortChild2DetailResp -ExpectedCode 0
Assert-Equal -StepName 'Sibling sortNo max+1 verification' -Actual $sortChild2DetailResp.data.sortNo -Expected ([int]$sortChild1DetailResp.data.sortNo + 1)

Write-Step "Same name under different parents should succeed"
$sameNameParent1Resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $adminHeaders -Body @{
    parentId = 0
    name = "folder_api_test_same_parent_a_$nameSuffix"
    remark = 'api_verify_same_parent_a'
}
$sameNameParent2Resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $adminHeaders -Body @{
    parentId = 0
    name = "folder_api_test_same_parent_b_$nameSuffix"
    remark = 'api_verify_same_parent_b'
}
Show-Response -Label 'POST /api/v1/folders same name parent 1' -Response $sameNameParent1Resp
Show-Response -Label 'POST /api/v1/folders same name parent 2' -Response $sameNameParent2Resp
Assert-Code -StepName 'Create same name parent 1' -Response $sameNameParent1Resp -ExpectedCode 0
Assert-Code -StepName 'Create same name parent 2' -Response $sameNameParent2Resp -ExpectedCode 0
$sameNameParent1Id = $sameNameParent1Resp.data.id
$sameNameParent2Id = $sameNameParent2Resp.data.id

$sameNameChild1Resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $adminHeaders -Body @{
    parentId = $sameNameParent1Id
    name = $sameName
    remark = 'api_verify_same_child_a'
}
$sameNameChild2Resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $adminHeaders -Body @{
    parentId = $sameNameParent2Id
    name = $sameName
    remark = 'api_verify_same_child_b'
}
Show-Response -Label 'POST /api/v1/folders same name child 1' -Response $sameNameChild1Resp
Show-Response -Label 'POST /api/v1/folders same name child 2' -Response $sameNameChild2Resp
Assert-Code -StepName 'Create same name child 1' -Response $sameNameChild1Resp -ExpectedCode 0
Assert-Code -StepName 'Create same name child 2' -Response $sameNameChild2Resp -ExpectedCode 0
$sameNameChild1Id = $sameNameChild1Resp.data.id
$sameNameChild2Id = $sameNameChild2Resp.data.id

$sameNameChild1DetailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$sameNameChild1Id" -Headers $adminHeaders -Body $null
$sameNameChild2DetailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$sameNameChild2Id" -Headers $adminHeaders -Body $null
Assert-Code -StepName 'Query same name child 1 detail' -Response $sameNameChild1DetailResp -ExpectedCode 0
Assert-Code -StepName 'Query same name child 2 detail' -Response $sameNameChild2DetailResp -ExpectedCode 0
Assert-Equal -StepName 'Same name child 1 name verification' -Actual $sameNameChild1DetailResp.data.name -Expected $sameName
Assert-Equal -StepName 'Same name child 2 name verification' -Actual $sameNameChild2DetailResp.data.name -Expected $sameName

Write-Step "Create level 1..8 then reject level 9"
$levelRootResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $adminHeaders -Body @{
    parentId = 0
    name = "folder_api_test_level_root_$nameSuffix"
    remark = 'api_verify_level_root'
}
Show-Response -Label 'POST /api/v1/folders level root' -Response $levelRootResp
Assert-Code -StepName 'Create level root' -Response $levelRootResp -ExpectedCode 0
$currentParentId = $levelRootResp.data.id
$levelFolderIds = @($currentParentId)
for ($level = 1; $level -le 8; $level++) {
    $levelResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $adminHeaders -Body @{
        parentId = $currentParentId
        name = "folder_api_test_level_${level}_$nameSuffix"
        remark = "api_verify_level_${level}"
    }
    Show-Response -Label "POST /api/v1/folders level $level" -Response $levelResp
    Assert-Code -StepName "Create level $level folder" -Response $levelResp -ExpectedCode 0
    $currentParentId = $levelResp.data.id
    $levelFolderIds += $currentParentId
}
$levelExceededResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders" -Headers $adminHeaders -Body @{
    parentId = $currentParentId
    name = "folder_api_test_level_9_$nameSuffix"
    remark = 'api_verify_level_9'
}
Show-Response -Label 'POST /api/v1/folders level 9' -Response $levelExceededResp
Assert-Code -StepName 'Create level 9 folder should fail' -Response $levelExceededResp -ExpectedCode 4002002

Write-Step "Create secondary verification department for negative permission checks"
$createSecondaryDeptResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/departments" -Headers $adminHeaders -Body @{
    name = $secondaryDeptName
    parentId = $adminDeptId
    level = 2
    remark = 'api_verify_secondary_dept'
}
Show-Response -Label 'POST /api/v1/departments (secondary)' -Response $createSecondaryDeptResp
Assert-Code -StepName 'Create secondary verification department' -Response $createSecondaryDeptResp -ExpectedCode 0

$deptListResp2 = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/departments" -Headers $adminHeaders -Body $null
Assert-Code -StepName 'Query departments for secondary dept' -Response $deptListResp2 -ExpectedCode 0
$secondaryDept = $deptListResp2.data | Where-Object { $_.name -eq $secondaryDeptName } | Select-Object -First 1
if ($null -eq $secondaryDept) {
    throw "Resolve secondary department id failed: dept not found by name=$secondaryDeptName"
}
$secondaryDeptId = $secondaryDept.id
Write-Host "secondaryDeptId=$secondaryDeptId"

Write-Step "Register second-round permission test users"
$managerDescRegisterResp = Register-User `
    -Username $managerDescUsername `
    -Password $ordinaryPassword `
    -RealName $managerDescRealName `
    -Email "${managerDescUsername}@example.com" `
    -Mobile "137$($timestamp.Substring($timestamp.Length - 8))" `
    -DeptId $otherDeptId `
    -JobLevel $adminJobLevel
Show-Response -Label 'POST /api/v1/auth/register (manager descendants user)' -Response $managerDescRegisterResp
Assert-Code -StepName 'Register manager descendants user' -Response $managerDescRegisterResp -ExpectedCode 0
$managerDescUserId = $managerDescRegisterResp.data.userId

$grantUserRegisterResp = Register-User `
    -Username $grantUserUsername `
    -Password $ordinaryPassword `
    -RealName $grantUserRealName `
    -Email "${grantUserUsername}@example.com" `
    -Mobile "136$($timestamp.Substring($timestamp.Length - 8))" `
    -DeptId $otherDeptId `
    -JobLevel $adminJobLevel
Show-Response -Label 'POST /api/v1/auth/register (grant user)' -Response $grantUserRegisterResp
Assert-Code -StepName 'Register grant user' -Response $grantUserRegisterResp -ExpectedCode 0
$grantUserId = $grantUserRegisterResp.data.userId

$roleGrantRegisterResp = Register-User `
    -Username $roleGrantUsername `
    -Password $ordinaryPassword `
    -RealName $roleGrantRealName `
    -Email "${roleGrantUsername}@example.com" `
    -Mobile "135$($timestamp.Substring($timestamp.Length - 8))" `
    -DeptId $otherDeptId `
    -JobLevel $adminJobLevel
Show-Response -Label 'POST /api/v1/auth/register (role grant user)' -Response $roleGrantRegisterResp
Assert-Code -StepName 'Register role grant user' -Response $roleGrantRegisterResp -ExpectedCode 0
$roleGrantUserId = $roleGrantRegisterResp.data.userId

$negativeDeptRegisterResp = Register-User `
    -Username $negativeDeptUsername `
    -Password $ordinaryPassword `
    -RealName $negativeDeptRealName `
    -Email "${negativeDeptUsername}@example.com" `
    -Mobile "134$($timestamp.Substring($timestamp.Length - 8))" `
    -DeptId $secondaryDeptId `
    -JobLevel $adminJobLevel
Show-Response -Label 'POST /api/v1/auth/register (negative dept user)' -Response $negativeDeptRegisterResp
Assert-Code -StepName 'Register negative dept user' -Response $negativeDeptRegisterResp -ExpectedCode 0
$negativeDeptUserId = $negativeDeptRegisterResp.data.userId

Write-Step "Enable second-round permission test users"
$enableManagerDescResp = Update-UserStatus -Headers $adminHeaders -UserId $managerDescUserId -Status 1 -Label 'Enable manager descendants user'
Show-Response -Label "PATCH /api/v1/users/$managerDescUserId/status" -Response $enableManagerDescResp
Assert-Code -StepName 'Enable manager descendants user' -Response $enableManagerDescResp -ExpectedCode 0

$enableGrantUserResp = Update-UserStatus -Headers $adminHeaders -UserId $grantUserId -Status 1 -Label 'Enable grant user'
Show-Response -Label "PATCH /api/v1/users/$grantUserId/status" -Response $enableGrantUserResp
Assert-Code -StepName 'Enable grant user' -Response $enableGrantUserResp -ExpectedCode 0

$enableRoleGrantResp = Update-UserStatus -Headers $adminHeaders -UserId $roleGrantUserId -Status 1 -Label 'Enable role grant user'
Show-Response -Label "PATCH /api/v1/users/$roleGrantUserId/status" -Response $enableRoleGrantResp
Assert-Code -StepName 'Enable role grant user' -Response $enableRoleGrantResp -ExpectedCode 0

$enableNegativeDeptResp = Update-UserStatus -Headers $adminHeaders -UserId $negativeDeptUserId -Status 1 -Label 'Enable negative dept user'
Show-Response -Label "PATCH /api/v1/users/$negativeDeptUserId/status" -Response $enableNegativeDeptResp
Assert-Code -StepName 'Enable negative dept user' -Response $enableNegativeDeptResp -ExpectedCode 0

Write-Step "Assign role for ROLE grant scenario"
$assignRoleRequest = @{
    userId = $roleGrantUserId
    roleCode = $roleGrantCode
    isPrimary = $false
}
$assignRoleResp = Assign-Role -Headers $adminHeaders -UserId $roleGrantUserId -RoleCode $roleGrantCode -Label 'POST /api/v1/roles/assign'
if ($assignRoleResp.code -ne 0) {
    Stop-ForRoleAssignFailure -RequestPath '/api/v1/roles/assign' -RequestBody $assignRoleRequest -Response $assignRoleResp
}
$roleListResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/roles/user/$roleGrantUserId" -Headers $adminHeaders -Body $null
Show-Response -Label "GET /api/v1/roles/user/$roleGrantUserId" -Response $roleListResp
Assert-Code -StepName 'Query role grant user roles' -Response $roleListResp -ExpectedCode 0
Assert-True -StepName 'Role grant user contains FOLDER_ADMIN role' -Actual ([bool]($roleListResp.data | Where-Object { $_.roleCode -eq $roleGrantCode }))

Write-Step "Login second-round permission test users"
$managerDescLoginResp = Login-User -Username $managerDescUsername -Password $ordinaryPassword -Label 'POST /api/v1/auth/login (manager descendants user)'
Assert-Code -StepName 'Login manager descendants user' -Response $managerDescLoginResp -ExpectedCode 0
$managerDescHeaders = New-AuthHeaders -Token $managerDescLoginResp.data.token

$grantUserLoginResp = Login-User -Username $grantUserUsername -Password $ordinaryPassword -Label 'POST /api/v1/auth/login (grant user)'
Assert-Code -StepName 'Login grant user' -Response $grantUserLoginResp -ExpectedCode 0
$grantUserHeaders = New-AuthHeaders -Token $grantUserLoginResp.data.token

$roleGrantLoginResp = Login-User -Username $roleGrantUsername -Password $ordinaryPassword -Label 'POST /api/v1/auth/login (role grant user)'
Assert-Code -StepName 'Login role grant user' -Response $roleGrantLoginResp -ExpectedCode 0
$roleGrantHeaders = New-AuthHeaders -Token $roleGrantLoginResp.data.token

$negativeDeptLoginResp = Login-User -Username $negativeDeptUsername -Password $ordinaryPassword -Label 'POST /api/v1/auth/login (negative dept user)'
Assert-Code -StepName 'Login negative dept user' -Response $negativeDeptLoginResp -ExpectedCode 0
$negativeDeptHeaders = New-AuthHeaders -Token $negativeDeptLoginResp.data.token

Write-Step "Prepare manager and grant verification folder trees"
$permRootResp = Create-Folder -Headers $adminHeaders -ParentId 0 -Name "folder_api_test_perm_root_$nameSuffix" -Remark 'api_verify permission root' -Label 'POST /api/v1/folders perm root'
Assert-Code -StepName 'Create permission root' -Response $permRootResp -ExpectedCode 0
$permRootId = $permRootResp.data.id

$managerSelfTargetResp = Create-Folder -Headers $adminHeaders -ParentId $permRootId -Name "folder_api_test_manager_self_$nameSuffix" -Remark 'api_verify manager self target' -Label 'POST /api/v1/folders manager self target'
Assert-Code -StepName 'Create manager self target' -Response $managerSelfTargetResp -ExpectedCode 0
$managerSelfTargetId = $managerSelfTargetResp.data.id

$managerSelfHiddenChildResp = Create-Folder -Headers $adminHeaders -ParentId $managerSelfTargetId -Name "folder_api_test_manager_self_hidden_$nameSuffix" -Remark 'api_verify manager self hidden child' -Label 'POST /api/v1/folders manager self hidden child'
Assert-Code -StepName 'Create manager self hidden child' -Response $managerSelfHiddenChildResp -ExpectedCode 0
$managerSelfHiddenChildId = $managerSelfHiddenChildResp.data.id

$managerDescParentResp = Create-Folder -Headers $adminHeaders -ParentId $permRootId -Name "folder_api_test_manager_desc_parent_$nameSuffix" -Remark 'api_verify manager descendants parent' -Label 'POST /api/v1/folders manager descendants parent'
Assert-Code -StepName 'Create manager descendants parent' -Response $managerDescParentResp -ExpectedCode 0
$managerDescParentId = $managerDescParentResp.data.id

$managerDescChildResp = Create-Folder -Headers $adminHeaders -ParentId $managerDescParentId -Name "folder_api_test_manager_desc_child_$nameSuffix" -Remark 'api_verify manager descendants child' -Label 'POST /api/v1/folders manager descendants child'
Assert-Code -StepName 'Create manager descendants child' -Response $managerDescChildResp -ExpectedCode 0
$managerDescChildId = $managerDescChildResp.data.id

$managerDescGrandchildResp = Create-Folder -Headers $adminHeaders -ParentId $managerDescChildId -Name "folder_api_test_manager_desc_grandchild_$nameSuffix" -Remark 'api_verify manager descendants grandchild' -Label 'POST /api/v1/folders manager descendants grandchild'
Assert-Code -StepName 'Create manager descendants grandchild' -Response $managerDescGrandchildResp -ExpectedCode 0
$managerDescGrandchildId = $managerDescGrandchildResp.data.id

$grantUserViewResp = Create-Folder -Headers $adminHeaders -ParentId $permRootId -Name "folder_api_test_grant_user_view_$nameSuffix" -Remark 'api_verify grant user view target' -Label 'POST /api/v1/folders grant user view target'
Assert-Code -StepName 'Create grant user view target' -Response $grantUserViewResp -ExpectedCode 0
$grantUserViewTargetId = $grantUserViewResp.data.id

$grantUserEditResp = Create-Folder -Headers $adminHeaders -ParentId $permRootId -Name "folder_api_test_grant_user_edit_$nameSuffix" -Remark 'api_verify grant user edit target' -Label 'POST /api/v1/folders grant user edit target'
Assert-Code -StepName 'Create grant user edit target' -Response $grantUserEditResp -ExpectedCode 0
$grantUserEditTargetId = $grantUserEditResp.data.id

$grantUserRenameResp = Create-Folder -Headers $adminHeaders -ParentId $permRootId -Name "folder_api_test_grant_user_rename_$nameSuffix" -Remark 'api_verify grant user rename target' -Label 'POST /api/v1/folders grant user rename target'
Assert-Code -StepName 'Create grant user rename target' -Response $grantUserRenameResp -ExpectedCode 0
$grantUserRenameTargetId = $grantUserRenameResp.data.id

$roleGrantTargetResp = Create-Folder -Headers $adminHeaders -ParentId $permRootId -Name "folder_api_test_role_grant_$nameSuffix" -Remark 'api_verify role grant target' -Label 'POST /api/v1/folders role grant target'
Assert-Code -StepName 'Create role grant target' -Response $roleGrantTargetResp -ExpectedCode 0
$roleGrantTargetId = $roleGrantTargetResp.data.id

$listParentResp = Create-Folder -Headers $adminHeaders -ParentId $permRootId -Name "folder_api_test_list_parent_$nameSuffix" -Remark 'api_verify list parent' -Label 'POST /api/v1/folders list parent'
Assert-Code -StepName 'Create list parent' -Response $listParentResp -ExpectedCode 0
$listParentId = $listParentResp.data.id

$listVisibleChildResp = Create-Folder -Headers $adminHeaders -ParentId $listParentId -Name "folder_api_test_list_visible_child_$nameSuffix" -Remark 'api_verify list visible child' -Label 'POST /api/v1/folders list visible child'
Assert-Code -StepName 'Create list visible child' -Response $listVisibleChildResp -ExpectedCode 0
$listVisibleChildId = $listVisibleChildResp.data.id

$listHiddenChildResp = Create-Folder -Headers $adminHeaders -ParentId $listParentId -Name "folder_api_test_list_hidden_child_$nameSuffix" -Remark 'api_verify list hidden child' -Label 'POST /api/v1/folders list hidden child'
Assert-Code -StepName 'Create list hidden child' -Response $listHiddenChildResp -ExpectedCode 0
$listHiddenChildId = $listHiddenChildResp.data.id

$listHiddenGrandchildResp = Create-Folder -Headers $adminHeaders -ParentId $listVisibleChildId -Name "folder_api_test_list_hidden_grandchild_$nameSuffix" -Remark 'api_verify list hidden grandchild' -Label 'POST /api/v1/folders list hidden grandchild'
Assert-Code -StepName 'Create list hidden grandchild' -Response $listHiddenGrandchildResp -ExpectedCode 0
$listHiddenGrandchildId = $listHiddenGrandchildResp.data.id

$deptGrantRootResp = Create-Folder -Headers $adminHeaders -ParentId 0 -Name "folder_api_test_dept_grant_root_$nameSuffix" -Remark 'api_verify dept grant root' -Label 'POST /api/v1/folders dept grant root'
Assert-Code -StepName 'Create dept grant root' -Response $deptGrantRootResp -ExpectedCode 0
$deptGrantRootId = $deptGrantRootResp.data.id

$deptGrantChildResp = Create-Folder -Headers $adminHeaders -ParentId $deptGrantRootId -Name "folder_api_test_dept_grant_child_$nameSuffix" -Remark 'api_verify dept grant child' -Label 'POST /api/v1/folders dept grant child'
Assert-Code -StepName 'Create dept grant child' -Response $deptGrantChildResp -ExpectedCode 0
$deptGrantChildId = $deptGrantChildResp.data.id

$deptGrantGrandchildResp = Create-Folder -Headers $adminHeaders -ParentId $deptGrantChildId -Name "folder_api_test_dept_grant_grandchild_$nameSuffix" -Remark 'api_verify dept grant grandchild' -Label 'POST /api/v1/folders dept grant grandchild'
Assert-Code -StepName 'Create dept grant grandchild' -Response $deptGrantGrandchildResp -ExpectedCode 0
$deptGrantGrandchildId = $deptGrantGrandchildResp.data.id

Write-Step "Insert doc_folder_manager verification records"
$managerSelfRecordId = New-TestId
$managerDescRecordId = New-TestId
Invoke-DbUpdate -Label 'INSERT doc_folder_manager SELF' -Sql @"
INSERT INTO doc_folder_manager (
    id, folder_id, user_id, manage_scope, created_at, created_by, updated_at, updated_by, deleted
)
SELECT
    $managerSelfRecordId,
    $managerSelfTargetId,
    $otherDeptUserId,
    'SELF',
    now(),
    'api_verify_manager_self',
    now(),
    'api_verify_manager_self',
    false
WHERE NOT EXISTS (
    SELECT 1
    FROM doc_folder_manager
    WHERE folder_id = $managerSelfTargetId
      AND user_id = $otherDeptUserId
      AND deleted = false
);
"@
Invoke-DbUpdate -Label 'INSERT doc_folder_manager SELF_AND_DESCENDANTS' -Sql @"
INSERT INTO doc_folder_manager (
    id, folder_id, user_id, manage_scope, created_at, created_by, updated_at, updated_by, deleted
)
SELECT
    $managerDescRecordId,
    $managerDescParentId,
    $managerDescUserId,
    'SELF_AND_DESCENDANTS',
    now(),
    'api_verify_manager_desc',
    now(),
    'api_verify_manager_desc',
    false
WHERE NOT EXISTS (
    SELECT 1
    FROM doc_folder_manager
    WHERE folder_id = $managerDescParentId
      AND user_id = $managerDescUserId
      AND deleted = false
);
"@

Write-Step "Insert doc_folder_grant verification records"
$grantUserViewRecordId = New-TestId
$grantUserEditRecordId = New-TestId
$grantUserRenameRecordId = New-TestId
$grantListParentRecordId = New-TestId
$grantListVisibleChildRecordId = New-TestId
$grantDeptRecordId = New-TestId
$grantRoleRecordId = New-TestId
Invoke-DbUpdate -Label 'INSERT doc_folder_grant USER VIEW SELF' -Sql @"
INSERT INTO doc_folder_grant (
    id, folder_id, subject_type, subject_id, permission_code, grant_scope,
    effective_from, effective_to, created_at, created_by, updated_at, updated_by, deleted
)
SELECT
    $grantUserViewRecordId,
    $grantUserViewTargetId,
    'USER',
    '$grantUserId',
    'FOLDER_VIEW',
    'SELF',
    null,
    null,
    now(),
    'api_verify_grant_user_view',
    now(),
    'api_verify_grant_user_view',
    false
WHERE NOT EXISTS (
    SELECT 1
    FROM doc_folder_grant
    WHERE folder_id = $grantUserViewTargetId
      AND subject_type = 'USER'
      AND subject_id = '$grantUserId'
      AND permission_code = 'FOLDER_VIEW'
      AND deleted = false
);
"@
Invoke-DbUpdate -Label 'INSERT doc_folder_grant USER EDIT SELF' -Sql @"
INSERT INTO doc_folder_grant (
    id, folder_id, subject_type, subject_id, permission_code, grant_scope,
    effective_from, effective_to, created_at, created_by, updated_at, updated_by, deleted
)
SELECT
    $grantUserEditRecordId,
    $grantUserEditTargetId,
    'USER',
    '$grantUserId',
    'FOLDER_EDIT',
    'SELF',
    null,
    null,
    now(),
    'api_verify_grant_user_edit',
    now(),
    'api_verify_grant_user_edit',
    false
WHERE NOT EXISTS (
    SELECT 1
    FROM doc_folder_grant
    WHERE folder_id = $grantUserEditTargetId
      AND subject_type = 'USER'
      AND subject_id = '$grantUserId'
      AND permission_code = 'FOLDER_EDIT'
      AND deleted = false
);
"@
Invoke-DbUpdate -Label 'INSERT doc_folder_grant USER RENAME SELF' -Sql @"
INSERT INTO doc_folder_grant (
    id, folder_id, subject_type, subject_id, permission_code, grant_scope,
    effective_from, effective_to, created_at, created_by, updated_at, updated_by, deleted
)
SELECT
    $grantUserRenameRecordId,
    $grantUserRenameTargetId,
    'USER',
    '$grantUserId',
    'FOLDER_RENAME',
    'SELF',
    null,
    null,
    now(),
    'api_verify_grant_user_rename',
    now(),
    'api_verify_grant_user_rename',
    false
WHERE NOT EXISTS (
    SELECT 1
    FROM doc_folder_grant
    WHERE folder_id = $grantUserRenameTargetId
      AND subject_type = 'USER'
      AND subject_id = '$grantUserId'
      AND permission_code = 'FOLDER_RENAME'
      AND deleted = false
);
"@
Invoke-DbUpdate -Label 'INSERT doc_folder_grant USER VIEW SELF (list parent)' -Sql @"
INSERT INTO doc_folder_grant (
    id, folder_id, subject_type, subject_id, permission_code, grant_scope,
    effective_from, effective_to, created_at, created_by, updated_at, updated_by, deleted
)
SELECT
    $grantListParentRecordId,
    $listParentId,
    'USER',
    '$grantUserId',
    'FOLDER_VIEW',
    'SELF',
    null,
    null,
    now(),
    'api_verify_grant_user_list_parent',
    now(),
    'api_verify_grant_user_list_parent',
    false
WHERE NOT EXISTS (
    SELECT 1
    FROM doc_folder_grant
    WHERE folder_id = $listParentId
      AND subject_type = 'USER'
      AND subject_id = '$grantUserId'
      AND permission_code = 'FOLDER_VIEW'
      AND deleted = false
);
"@
Invoke-DbUpdate -Label 'INSERT doc_folder_grant USER VIEW SELF (list visible child)' -Sql @"
INSERT INTO doc_folder_grant (
    id, folder_id, subject_type, subject_id, permission_code, grant_scope,
    effective_from, effective_to, created_at, created_by, updated_at, updated_by, deleted
)
SELECT
    $grantListVisibleChildRecordId,
    $listVisibleChildId,
    'USER',
    '$grantUserId',
    'FOLDER_VIEW',
    'SELF',
    null,
    null,
    now(),
    'api_verify_grant_user_list_child',
    now(),
    'api_verify_grant_user_list_child',
    false
WHERE NOT EXISTS (
    SELECT 1
    FROM doc_folder_grant
    WHERE folder_id = $listVisibleChildId
      AND subject_type = 'USER'
      AND subject_id = '$grantUserId'
      AND permission_code = 'FOLDER_VIEW'
      AND deleted = false
);
"@
Invoke-DbUpdate -Label 'INSERT doc_folder_grant DEPT VIEW SELF_AND_DESCENDANTS' -Sql @"
INSERT INTO doc_folder_grant (
    id, folder_id, subject_type, subject_id, permission_code, grant_scope,
    effective_from, effective_to, created_at, created_by, updated_at, updated_by, deleted
)
SELECT
    $grantDeptRecordId,
    $deptGrantRootId,
    'DEPT',
    '$otherDeptId',
    'FOLDER_VIEW',
    'SELF_AND_DESCENDANTS',
    null,
    null,
    now(),
    'api_verify_grant_dept_view',
    now(),
    'api_verify_grant_dept_view',
    false
WHERE NOT EXISTS (
    SELECT 1
    FROM doc_folder_grant
    WHERE folder_id = $deptGrantRootId
      AND subject_type = 'DEPT'
      AND subject_id = '$otherDeptId'
      AND permission_code = 'FOLDER_VIEW'
      AND deleted = false
);
"@
Invoke-DbUpdate -Label 'INSERT doc_folder_grant ROLE VIEW SELF' -Sql @"
INSERT INTO doc_folder_grant (
    id, folder_id, subject_type, subject_id, permission_code, grant_scope,
    effective_from, effective_to, created_at, created_by, updated_at, updated_by, deleted
)
SELECT
    $grantRoleRecordId,
    $roleGrantTargetId,
    'ROLE',
    '$roleGrantCode',
    'FOLDER_VIEW',
    'SELF',
    null,
    null,
    now(),
    'api_verify_grant_role_view',
    now(),
    'api_verify_grant_role_view',
    false
WHERE NOT EXISTS (
    SELECT 1
    FROM doc_folder_grant
    WHERE folder_id = $roleGrantTargetId
      AND subject_type = 'ROLE'
      AND subject_id = '$roleGrantCode'
      AND permission_code = 'FOLDER_VIEW'
      AND deleted = false
);
"@

Write-Step "Verify doc_folder_manager SELF"
Write-Host "Subject=$otherDeptUsername"
Write-Host "PermissionSource=doc_folder_manager SELF"
$managerSelfDetailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$managerSelfTargetId" -Headers $otherDeptHeaders -Body $null
Show-Response -Label "GET /api/v1/folders/$managerSelfTargetId (manager SELF)" -Response $managerSelfDetailResp
Assert-Code -StepName 'Manager SELF detail query' -Response $managerSelfDetailResp -ExpectedCode 0

$managerSelfChildrenResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/children?parentId=$managerSelfTargetId" -Headers $otherDeptHeaders -Body $null
Show-Response -Label "GET /api/v1/folders/children?parentId=$managerSelfTargetId (manager SELF)" -Response $managerSelfChildrenResp
Assert-Code -StepName 'Manager SELF children query' -Response $managerSelfChildrenResp -ExpectedCode 0
Assert-NotContainsId -StepName 'Manager SELF children hides existing descendant' -Response $managerSelfChildrenResp -UnexpectedId $managerSelfHiddenChildId
Assert-Equal -StepName 'Manager SELF children count before create' -Actual $managerSelfChildrenResp.data.Count -Expected 0

$managerSelfHiddenDetailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$managerSelfHiddenChildId" -Headers $otherDeptHeaders -Body $null
Show-Response -Label "GET /api/v1/folders/$managerSelfHiddenChildId (manager SELF hidden child)" -Response $managerSelfHiddenDetailResp
Assert-Code -StepName 'Manager SELF cannot view hidden descendant detail' -Response $managerSelfHiddenDetailResp -ExpectedCode 4032001

$managerSelfRenameResp = Rename-Folder -Headers $otherDeptHeaders -FolderId $managerSelfTargetId -Name "folder_api_test_manager_self_renamed_$nameSuffix" -Label "PATCH /api/v1/folders/$managerSelfTargetId/name (manager SELF)"
Assert-Code -StepName 'Manager SELF rename target' -Response $managerSelfRenameResp -ExpectedCode 0

$managerSelfUpdateResp = Update-Folder -Headers $otherDeptHeaders -FolderId $managerSelfTargetId -Remark '经理直管目录信息更新' -Status 1 -InheritPermission $true -SortNo 51 -Label "PUT /api/v1/folders/$managerSelfTargetId (manager SELF)"
Assert-Code -StepName 'Manager SELF update target' -Response $managerSelfUpdateResp -ExpectedCode 0

$managerSelfCreateChildResp = Create-Folder -Headers $otherDeptHeaders -ParentId $managerSelfTargetId -Name "folder_api_test_manager_self_created_child_$nameSuffix" -Remark 'api_verify manager self created child' -Label 'POST /api/v1/folders manager self create child'
Assert-Code -StepName 'Manager SELF create child' -Response $managerSelfCreateChildResp -ExpectedCode 0
$managerSelfCreatedChildId = $managerSelfCreateChildResp.data.id

Write-Step "Verify doc_folder_manager SELF_AND_DESCENDANTS"
Write-Host "Subject=$managerDescUsername"
Write-Host "PermissionSource=doc_folder_manager SELF_AND_DESCENDANTS"
$managerDescParentDetailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$managerDescParentId" -Headers $managerDescHeaders -Body $null
$managerDescChildDetailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$managerDescChildId" -Headers $managerDescHeaders -Body $null
$managerDescGrandchildDetailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$managerDescGrandchildId" -Headers $managerDescHeaders -Body $null
$managerDescParentChildrenResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/children?parentId=$managerDescParentId" -Headers $managerDescHeaders -Body $null
$managerDescChildChildrenResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/children?parentId=$managerDescChildId" -Headers $managerDescHeaders -Body $null
Show-Response -Label "GET /api/v1/folders/$managerDescParentId (manager SELF_AND_DESC)" -Response $managerDescParentDetailResp
Show-Response -Label "GET /api/v1/folders/$managerDescChildId (manager SELF_AND_DESC)" -Response $managerDescChildDetailResp
Show-Response -Label "GET /api/v1/folders/$managerDescGrandchildId (manager SELF_AND_DESC)" -Response $managerDescGrandchildDetailResp
Show-Response -Label "GET /api/v1/folders/children?parentId=$managerDescParentId (manager SELF_AND_DESC)" -Response $managerDescParentChildrenResp
Show-Response -Label "GET /api/v1/folders/children?parentId=$managerDescChildId (manager SELF_AND_DESC)" -Response $managerDescChildChildrenResp
Assert-Code -StepName 'Manager SELF_AND_DESC parent detail' -Response $managerDescParentDetailResp -ExpectedCode 0
Assert-Code -StepName 'Manager SELF_AND_DESC child detail' -Response $managerDescChildDetailResp -ExpectedCode 0
Assert-Code -StepName 'Manager SELF_AND_DESC grandchild detail' -Response $managerDescGrandchildDetailResp -ExpectedCode 0
Assert-Code -StepName 'Manager SELF_AND_DESC parent children query' -Response $managerDescParentChildrenResp -ExpectedCode 0
Assert-Code -StepName 'Manager SELF_AND_DESC child children query' -Response $managerDescChildChildrenResp -ExpectedCode 0
Assert-ContainsId -StepName 'Manager SELF_AND_DESC parent children contains child' -Response $managerDescParentChildrenResp -ExpectedId $managerDescChildId
Assert-ContainsId -StepName 'Manager SELF_AND_DESC child children contains grandchild' -Response $managerDescChildChildrenResp -ExpectedId $managerDescGrandchildId
$managerDescParentChildNode = Get-ItemById -Items $managerDescParentChildrenResp.data -Id $managerDescChildId
Assert-True -StepName 'Manager SELF_AND_DESC child hasChildren visible' -Actual ([bool]$managerDescParentChildNode.hasChildren)

$managerDescRenameResp = Rename-Folder -Headers $managerDescHeaders -FolderId $managerDescChildId -Name "folder_api_test_manager_desc_child_renamed_$nameSuffix" -Label "PATCH /api/v1/folders/$managerDescChildId/name (manager SELF_AND_DESC)"
Assert-Code -StepName 'Manager SELF_AND_DESC rename child' -Response $managerDescRenameResp -ExpectedCode 0

$managerDescUpdateResp = Update-Folder -Headers $managerDescHeaders -FolderId $managerDescGrandchildId -Remark '经理递归孙目录信息更新' -Status 1 -InheritPermission $true -SortNo 61 -Label "PUT /api/v1/folders/$managerDescGrandchildId (manager SELF_AND_DESC)"
Assert-Code -StepName 'Manager SELF_AND_DESC update grandchild' -Response $managerDescUpdateResp -ExpectedCode 0

$managerDescCreateUnderParentResp = Create-Folder -Headers $managerDescHeaders -ParentId $managerDescParentId -Name "folder_api_test_manager_desc_created_child_$nameSuffix" -Remark 'api_verify manager descendants create under parent' -Label 'POST /api/v1/folders manager descendants create under parent'
Assert-Code -StepName 'Manager SELF_AND_DESC create child under parent' -Response $managerDescCreateUnderParentResp -ExpectedCode 0
$managerDescCreateUnderChildResp = Create-Folder -Headers $managerDescHeaders -ParentId $managerDescChildId -Name "folder_api_test_manager_desc_created_grandchild_$nameSuffix" -Remark 'api_verify manager descendants create under child' -Label 'POST /api/v1/folders manager descendants create under child'
Assert-Code -StepName 'Manager SELF_AND_DESC create child under child' -Response $managerDescCreateUnderChildResp -ExpectedCode 0

Write-Step "Verify USER grant FOLDER_VIEW SELF"
Write-Host "Subject=$grantUserUsername"
Write-Host "PermissionSource=doc_folder_grant USER FOLDER_VIEW SELF"
$grantUserViewDetailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$grantUserViewTargetId" -Headers $grantUserHeaders -Body $null
Show-Response -Label "GET /api/v1/folders/$grantUserViewTargetId (USER VIEW)" -Response $grantUserViewDetailResp
Assert-Code -StepName 'USER VIEW detail query' -Response $grantUserViewDetailResp -ExpectedCode 0
$grantUserViewRenameResp = Rename-Folder -Headers $grantUserHeaders -FolderId $grantUserViewTargetId -Name "folder_api_test_user_view_rename_denied_$nameSuffix" -Label "PATCH /api/v1/folders/$grantUserViewTargetId/name (USER VIEW)"
Assert-Code -StepName 'USER VIEW rename should fail' -Response $grantUserViewRenameResp -ExpectedCode 4032001
$grantUserViewUpdateResp = Update-Folder -Headers $grantUserHeaders -FolderId $grantUserViewTargetId -Remark '查看权限下尝试修改目录' -Status 1 -InheritPermission $true -SortNo 71 -Label "PUT /api/v1/folders/$grantUserViewTargetId (USER VIEW)"
Assert-Code -StepName 'USER VIEW update should fail' -Response $grantUserViewUpdateResp -ExpectedCode 4032001
$grantUserViewCreateResp = Create-Folder -Headers $grantUserHeaders -ParentId $grantUserViewTargetId -Name "folder_api_test_user_view_create_denied_$nameSuffix" -Remark 'api_verify user view create denied' -Label 'POST /api/v1/folders USER VIEW create child'
Assert-Code -StepName 'USER VIEW create child should fail' -Response $grantUserViewCreateResp -ExpectedCode 4032001

Write-Step "Verify USER grant FOLDER_EDIT SELF"
Write-Host "Subject=$grantUserUsername"
Write-Host "PermissionSource=doc_folder_grant USER FOLDER_EDIT SELF"
$grantUserEditDetailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$grantUserEditTargetId" -Headers $grantUserHeaders -Body $null
Show-Response -Label "GET /api/v1/folders/$grantUserEditTargetId (USER EDIT)" -Response $grantUserEditDetailResp
Assert-Code -StepName 'USER EDIT detail query' -Response $grantUserEditDetailResp -ExpectedCode 0
$grantUserEditUpdateResp = Update-Folder -Headers $grantUserHeaders -FolderId $grantUserEditTargetId -Remark '编辑授权目录信息更新' -Status 1 -InheritPermission $false -SortNo 72 -Label "PUT /api/v1/folders/$grantUserEditTargetId (USER EDIT)"
Assert-Code -StepName 'USER EDIT update target' -Response $grantUserEditUpdateResp -ExpectedCode 0
$grantUserEditRenameResp = Rename-Folder -Headers $grantUserHeaders -FolderId $grantUserEditTargetId -Name "folder_api_test_user_edit_rename_denied_$nameSuffix" -Label "PATCH /api/v1/folders/$grantUserEditTargetId/name (USER EDIT)"
Assert-Code -StepName 'USER EDIT rename should fail' -Response $grantUserEditRenameResp -ExpectedCode 4032001
$grantUserEditCreateResp = Create-Folder -Headers $grantUserHeaders -ParentId $grantUserEditTargetId -Name "folder_api_test_user_edit_created_child_$nameSuffix" -Remark 'api_verify user edit created child' -Label 'POST /api/v1/folders USER EDIT create child'
Assert-Code -StepName 'USER EDIT create child should succeed' -Response $grantUserEditCreateResp -ExpectedCode 0
$grantUserEditCreatedChildId = $grantUserEditCreateResp.data.id

Write-Step "Verify USER grant FOLDER_RENAME SELF"
Write-Host "Subject=$grantUserUsername"
Write-Host "PermissionSource=doc_folder_grant USER FOLDER_RENAME SELF"
$grantUserRenameDetailResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$grantUserRenameTargetId" -Headers $grantUserHeaders -Body $null
Show-Response -Label "GET /api/v1/folders/$grantUserRenameTargetId (USER RENAME)" -Response $grantUserRenameDetailResp
Assert-Code -StepName 'USER RENAME detail query' -Response $grantUserRenameDetailResp -ExpectedCode 0
$grantUserRenameRenameResp = Rename-Folder -Headers $grantUserHeaders -FolderId $grantUserRenameTargetId -Name "folder_api_test_user_rename_target_$nameSuffix" -Label "PATCH /api/v1/folders/$grantUserRenameTargetId/name (USER RENAME)"
Assert-Code -StepName 'USER RENAME rename target' -Response $grantUserRenameRenameResp -ExpectedCode 0
$grantUserRenameUpdateResp = Update-Folder -Headers $grantUserHeaders -FolderId $grantUserRenameTargetId -Remark '重命名权限下尝试修改目录' -Status 1 -InheritPermission $true -SortNo 73 -Label "PUT /api/v1/folders/$grantUserRenameTargetId (USER RENAME)"
Assert-Code -StepName 'USER RENAME update should fail' -Response $grantUserRenameUpdateResp -ExpectedCode 4032001
$grantUserRenameCreateResp = Create-Folder -Headers $grantUserHeaders -ParentId $grantUserRenameTargetId -Name "folder_api_test_user_rename_create_denied_$nameSuffix" -Remark 'api_verify user rename create denied' -Label 'POST /api/v1/folders USER RENAME create child'
Assert-Code -StepName 'USER RENAME create child should fail' -Response $grantUserRenameCreateResp -ExpectedCode 4032001

Write-Step "Verify DEPT grant FOLDER_VIEW SELF_AND_DESCENDANTS"
Write-Host "Subject=$otherDeptUsername / $negativeDeptUsername"
Write-Host "PermissionSource=doc_folder_grant DEPT FOLDER_VIEW SELF_AND_DESCENDANTS"
$deptGrantRootTreePositiveResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/tree/root" -Headers $otherDeptHeaders -Body $null
Show-Response -Label 'GET /api/v1/folders/tree/root (DEPT positive user)' -Response $deptGrantRootTreePositiveResp
Assert-Code -StepName 'DEPT grant positive root tree' -Response $deptGrantRootTreePositiveResp -ExpectedCode 0
Assert-ContainsId -StepName 'DEPT grant root tree contains granted root' -Response $deptGrantRootTreePositiveResp -ExpectedId $deptGrantRootId
$deptGrantRootNode = Get-ItemById -Items $deptGrantRootTreePositiveResp.data -Id $deptGrantRootId
Assert-True -StepName 'DEPT grant root hasChildren visible' -Actual ([bool]$deptGrantRootNode.hasChildren)

$deptGrantRootTreeNegativeResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/tree/root" -Headers $negativeDeptHeaders -Body $null
Show-Response -Label 'GET /api/v1/folders/tree/root (DEPT negative user)' -Response $deptGrantRootTreeNegativeResp
Assert-Code -StepName 'DEPT grant negative root tree' -Response $deptGrantRootTreeNegativeResp -ExpectedCode 0
Assert-NotContainsId -StepName 'DEPT grant root tree hides unauthorized root' -Response $deptGrantRootTreeNegativeResp -UnexpectedId $deptGrantRootId

$deptGrantRootDetailPositiveResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$deptGrantRootId" -Headers $otherDeptHeaders -Body $null
$deptGrantChildDetailPositiveResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$deptGrantChildId" -Headers $otherDeptHeaders -Body $null
$deptGrantGrandchildDetailPositiveResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$deptGrantGrandchildId" -Headers $otherDeptHeaders -Body $null
Assert-Code -StepName 'DEPT grant positive root detail' -Response $deptGrantRootDetailPositiveResp -ExpectedCode 0
Assert-Code -StepName 'DEPT grant positive child detail' -Response $deptGrantChildDetailPositiveResp -ExpectedCode 0
Assert-Code -StepName 'DEPT grant positive grandchild detail' -Response $deptGrantGrandchildDetailPositiveResp -ExpectedCode 0

$deptGrantChildrenPositiveResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/children?parentId=$deptGrantRootId" -Headers $otherDeptHeaders -Body $null
Show-Response -Label "GET /api/v1/folders/children?parentId=$deptGrantRootId (DEPT positive user)" -Response $deptGrantChildrenPositiveResp
Assert-Code -StepName 'DEPT grant positive children query' -Response $deptGrantChildrenPositiveResp -ExpectedCode 0
Assert-ContainsId -StepName 'DEPT grant positive children contains granted child' -Response $deptGrantChildrenPositiveResp -ExpectedId $deptGrantChildId

$deptGrantRootDetailNegativeResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$deptGrantRootId" -Headers $negativeDeptHeaders -Body $null
Show-Response -Label "GET /api/v1/folders/$deptGrantRootId (DEPT negative user)" -Response $deptGrantRootDetailNegativeResp
Assert-Code -StepName 'DEPT grant negative detail should fail' -Response $deptGrantRootDetailNegativeResp -ExpectedCode 4032001
$deptGrantRenameResp = Rename-Folder -Headers $otherDeptHeaders -FolderId $deptGrantRootId -Name "folder_api_test_dept_view_rename_denied_$nameSuffix" -Label "PATCH /api/v1/folders/$deptGrantRootId/name (DEPT VIEW)"
Assert-Code -StepName 'DEPT grant rename should fail' -Response $deptGrantRenameResp -ExpectedCode 4032001
$deptGrantUpdateResp = Update-Folder -Headers $otherDeptHeaders -FolderId $deptGrantRootId -Remark '部门查看权限下尝试修改目录' -Status 1 -InheritPermission $true -SortNo 74 -Label "PUT /api/v1/folders/$deptGrantRootId (DEPT VIEW)"
Assert-Code -StepName 'DEPT grant update should fail' -Response $deptGrantUpdateResp -ExpectedCode 4032001
$deptGrantCreateResp = Create-Folder -Headers $otherDeptHeaders -ParentId $deptGrantRootId -Name "folder_api_test_dept_view_create_denied_$nameSuffix" -Remark 'api_verify dept view create denied' -Label 'POST /api/v1/folders DEPT VIEW create child'
Assert-Code -StepName 'DEPT grant create child should fail' -Response $deptGrantCreateResp -ExpectedCode 4032001

Write-Step "Verify ROLE grant FOLDER_VIEW SELF"
Write-Host "Subject=$roleGrantUsername / $negativeDeptUsername"
Write-Host "PermissionSource=doc_folder_grant ROLE FOLDER_VIEW SELF"
$roleGrantDetailPositiveResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$roleGrantTargetId" -Headers $roleGrantHeaders -Body $null
Show-Response -Label "GET /api/v1/folders/$roleGrantTargetId (ROLE positive user)" -Response $roleGrantDetailPositiveResp
Assert-Code -StepName 'ROLE grant positive detail' -Response $roleGrantDetailPositiveResp -ExpectedCode 0
$roleGrantDetailNegativeResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$roleGrantTargetId" -Headers $negativeDeptHeaders -Body $null
Show-Response -Label "GET /api/v1/folders/$roleGrantTargetId (ROLE negative user)" -Response $roleGrantDetailNegativeResp
Assert-Code -StepName 'ROLE grant negative detail should fail' -Response $roleGrantDetailNegativeResp -ExpectedCode 4032001

Write-Step "Verify children visibility filtering and hasChildren leak protection"
Write-Host "Subject=$grantUserUsername"
Write-Host "PermissionSource=doc_folder_grant USER FOLDER_VIEW SELF on parent and one child"
$listChildrenResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/children?parentId=$listParentId" -Headers $grantUserHeaders -Body $null
Show-Response -Label "GET /api/v1/folders/children?parentId=$listParentId (grant user list visibility)" -Response $listChildrenResp
Assert-Code -StepName 'Grant user list parent query' -Response $listChildrenResp -ExpectedCode 0
Assert-ContainsId -StepName 'List visibility contains visible child' -Response $listChildrenResp -ExpectedId $listVisibleChildId
Assert-NotContainsId -StepName 'List visibility hides hidden child' -Response $listChildrenResp -UnexpectedId $listHiddenChildId
$listVisibleChildNode = Get-ItemById -Items $listChildrenResp.data -Id $listVisibleChildId
Assert-False -StepName 'Visible child hasChildren should not leak hidden grandchild' -Actual ([bool]$listVisibleChildNode.hasChildren)

# ============================================================
# G1-G7: FolderGrant API 验证（Phase 3 新增接口）
# 依赖：adminHeaders（SUPER_ADMIN）、childFolderId（非根目录）、rootFolderId（根目录）
# ============================================================
Write-Step "G1: owner POST grants on non-root folder => 200"
$g1Resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders/$childFolderId/grants" -Headers $adminHeaders -Body @{
    subjectType    = 'USER'
    subjectId      = "$sameDeptUserId"
    permissionCodes = @('FOLDER_VIEW')
    grantScope     = 'SELF'
}
Show-Response -Label "G1 POST /api/v1/folders/$childFolderId/grants" -Response $g1Resp
Assert-Code -StepName 'G1 owner add grant on non-root' -Response $g1Resp -ExpectedCode 0

Write-Step "G2: POST grants on root folder => FOLDER_GRANT_ON_ROOT_FORBIDDEN (4002005)"
$g2Resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders/$rootFolderId/grants" -Headers $adminHeaders -Body @{
    subjectType    = 'USER'
    subjectId      = "$sameDeptUserId"
    permissionCodes = @('FOLDER_VIEW')
    grantScope     = 'SELF'
}
Show-Response -Label "G2 POST /api/v1/folders/$rootFolderId/grants (root)" -Response $g2Resp
Assert-Code -StepName 'G2 grant on root forbidden' -Response $g2Resp -ExpectedCode 4002005

Write-Step "G3: non-manager/non-owner POST grants => FOLDER_PERMISSION_DENIED (4032001)"
$g3Resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders/$childFolderId/grants" -Headers $sameDeptHeaders -Body @{
    subjectType    = 'USER'
    subjectId      = "$otherDeptUserId"
    permissionCodes = @('FOLDER_VIEW')
    grantScope     = 'SELF'
}
Show-Response -Label "G3 POST grants by non-manager" -Response $g3Resp
Assert-Code -StepName 'G3 non-manager add grant denied' -Response $g3Resp -ExpectedCode 4032001

Write-Step "G4: duplicate grant => FOLDER_GRANT_DUPLICATED (4002004)"
$g4Resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders/$childFolderId/grants" -Headers $adminHeaders -Body @{
    subjectType    = 'USER'
    subjectId      = "$sameDeptUserId"
    permissionCodes = @('FOLDER_VIEW')
    grantScope     = 'SELF'
}
Show-Response -Label "G4 duplicate grant" -Response $g4Resp
Assert-Code -StepName 'G4 duplicate grant returns 4002004' -Response $g4Resp -ExpectedCode 4002004

Write-Step "G5: GET grants returns list with correct fields"
$g5Resp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$childFolderId/grants" -Headers $adminHeaders -Body $null
Show-Response -Label "G5 GET /api/v1/folders/$childFolderId/grants" -Response $g5Resp
Assert-Code -StepName 'G5 list grants success' -Response $g5Resp -ExpectedCode 0
Assert-True -StepName 'G5 grants list not empty' -Actual ($g5Resp.data.Count -gt 0)
$g5FirstGrant = $g5Resp.data | Select-Object -First 1
Assert-True -StepName 'G5 grant has grantId' -Actual ($null -ne $g5FirstGrant.grantId)
Assert-True -StepName 'G5 grant has permissionCode' -Actual ($null -ne $g5FirstGrant.permissionCode)
$g5GrantId = $g5FirstGrant.grantId

Write-Step "G6: DELETE non-existent grantId => FOLDER_GRANT_NOT_FOUND (4042003)"
$g6Resp = Invoke-JsonApi -Method Delete -Uri "$baseUrl/api/v1/folders/$childFolderId/grants/999999999999" -Headers $adminHeaders -Body $null
Show-Response -Label "G6 DELETE non-existent grantId" -Response $g6Resp
Assert-Code -StepName 'G6 delete non-existent grant returns 4042003' -Response $g6Resp -ExpectedCode 4042003

Write-Step "G7: DELETE grantId belonging to different folder => FOLDER_GRANT_NOT_FOUND (4042003)"
# 用 childFolderId 的 grantId 去删 rootFolderId 下的 grant（跨 folder）
$g7Resp = Invoke-JsonApi -Method Delete -Uri "$baseUrl/api/v1/folders/$rootFolderId/grants/$g5GrantId" -Headers $adminHeaders -Body $null
Show-Response -Label "G7 DELETE grant with wrong folderId" -Response $g7Resp
Assert-Code -StepName 'G7 cross-folder delete returns 4042003' -Response $g7Resp -ExpectedCode 4042003

# ============================================================
# M1-M4: FolderManager API 验证（Phase 3 新增接口）
# 依赖：adminHeaders、childFolderId、roleGrantUserId（持有 FOLDER_ADMIN）、sameDeptUserId（无 FOLDER_ADMIN）
# ============================================================
Write-Step "M1: add manager for user without FOLDER_ADMIN role => FOLDER_MANAGER_REQUIRES_ROLE (4222001)"
$m1Resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders/$childFolderId/managers" -Headers $adminHeaders -Body @{
    userId      = $sameDeptUserId
    manageScope = 'SELF_AND_DESCENDANTS'
}
Show-Response -Label "M1 POST managers user without FOLDER_ADMIN" -Response $m1Resp
Assert-Code -StepName 'M1 requires FOLDER_ADMIN role' -Response $m1Resp -ExpectedCode 4222001

Write-Step "M2: add manager for user with FOLDER_ADMIN role => 200"
$m2Resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders/$childFolderId/managers" -Headers $adminHeaders -Body @{
    userId      = $roleGrantUserId
    manageScope = 'SELF_AND_DESCENDANTS'
}
Show-Response -Label "M2 POST managers user with FOLDER_ADMIN" -Response $m2Resp
Assert-Code -StepName 'M2 add manager success' -Response $m2Resp -ExpectedCode 0

Write-Step "M3: duplicate add manager => FOLDER_MANAGER_DUPLICATED (4002007)"
$m3Resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders/$childFolderId/managers" -Headers $adminHeaders -Body @{
    userId      = $roleGrantUserId
    manageScope = 'SELF_AND_DESCENDANTS'
}
Show-Response -Label "M3 duplicate add manager" -Response $m3Resp
Assert-Code -StepName 'M3 duplicate manager returns 4002007' -Response $m3Resp -ExpectedCode 4002007

Write-Step "M4: GET managers returns list; DELETE manager => 200"
$m4ListResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$childFolderId/managers" -Headers $adminHeaders -Body $null
Show-Response -Label "M4 GET /api/v1/folders/$childFolderId/managers" -Response $m4ListResp
Assert-Code -StepName 'M4 list managers success' -Response $m4ListResp -ExpectedCode 0
Assert-True -StepName 'M4 managers list not empty' -Actual ($m4ListResp.data.Count -gt 0)
$m4ManagerId = ($m4ListResp.data | Where-Object { "$($_.userId)" -eq "$roleGrantUserId" } | Select-Object -First 1).managerId
Assert-True -StepName 'M4 found managerId' -Actual ($null -ne $m4ManagerId)
$m4DeleteResp = Invoke-JsonApi -Method Delete -Uri "$baseUrl/api/v1/folders/$childFolderId/managers/$m4ManagerId" -Headers $adminHeaders -Body $null
Show-Response -Label "M4 DELETE /api/v1/folders/$childFolderId/managers/$m4ManagerId" -Response $m4DeleteResp
Assert-Code -StepName 'M4 delete manager success' -Response $m4DeleteResp -ExpectedCode 0

# ============================================================
# A1-A3: 审计日志写入验证（通过 Invoke-DbQuery + DB 断言助手自动校验）
# 注意：本段必须在 M4 删除 manager 成功之后跑，否则 MANAGER_REMOVE 事件不存在
# ============================================================

Write-Step "A1: audit_operation_log covers all expected operation_types for childFolderId"
# 期望 op 集合解释：
#   CREATE：childFolderId 由 SUPER_ADMIN 创建（脚本上文 "SUPER_ADMIN create child folder"）
#   RENAME：SUPER_ADMIN 改名 childFolderId
#   UPDATE：SUPER_ADMIN 更新 childFolderId 的 remark/status/inherit/sortNo
#   GRANT_ADD：G1 在 childFolderId 上成功授权
#   MANAGER_ADD：M2 在 childFolderId 上成功设管理员
#   MANAGER_REMOVE：M4 在 childFolderId 上成功删除管理员
# 不包含 GRANT_REMOVE：G6/G7 为负向用例（不存在 / 跨 folder），事务回滚，无审计行
$a1Sql = @"
SELECT operation_type
FROM audit_operation_log
WHERE deleted = false
  AND module_code = 'FOLDER'
  AND biz_type = 'FOLDER'
  AND biz_id = $childFolderId
  AND operation_type IN ('CREATE','RENAME','UPDATE','GRANT_ADD','MANAGER_ADD','MANAGER_REMOVE')
GROUP BY operation_type
ORDER BY operation_type
"@
$a1Out = Invoke-DbQuery -Label 'A1 audit operation_type coverage' -Sql $a1Sql -PassThruOutput
Assert-DbRowCount -Label 'A1 audit log has at least 6 distinct op rows' -Output $a1Out -MinRows 6
foreach ($expectedOp in @('CREATE','RENAME','UPDATE','GRANT_ADD','MANAGER_ADD','MANAGER_REMOVE')) {
    Assert-DbRowMatch -Label "A1 audit contains operation_type=$expectedOp" -Output $a1Out -RowSelector "operation_type=$expectedOp"
}

Write-Step "A2: RENAME audit row carries correct before/after name on childFolderId"
# FolderServiceImpl.rename 在 audit 写入时 beforeData/afterData 各放一个 name 字段
# 取该 folder 最近一次 RENAME 事件（脚本内只发生过一次成功重命名，LIMIT 1 锁定它）
$a2Sql = @"
SELECT (before_data->>'name') AS before_name,
       (after_data->>'name')  AS after_name
FROM audit_operation_log
WHERE deleted = false
  AND biz_id = $childFolderId
  AND operation_type = 'RENAME'
ORDER BY operation_time DESC
LIMIT 1
"@
$a2Out = Invoke-DbQuery -Label 'A2 query rename event' -Sql $a2Sql -PassThruOutput
Assert-DbRowCount -Label 'A2 rename audit row exists' -Output $a2Out -MinRows 1
Assert-DbCell -Label 'A2 before_name equals original child name' -Output $a2Out -RowSelector 'before_name=' -ColumnName 'before_name' -Expected $childName
Assert-DbCell -Label 'A2 after_name equals renamed child name' -Output $a2Out -RowSelector 'before_name=' -ColumnName 'after_name' -Expected $renamedChildName

Write-Step "A3: GRANT_ADD audit row carries grantIds + subjectType + subjectId from G1"
# FolderGrantServiceImpl.add 在 extra_data 写入 grantIds / subjectType / subjectId
# G1 是 childFolderId 上唯一成功的 GRANT_ADD（G4 重复授权抛 DuplicateKeyException，同事务回滚不会留行）
# 用 ASC LIMIT 1 锁定 G1 那条
$a3Sql = @"
SELECT (extra_data->>'grantIds')    AS grant_ids,
       (extra_data->>'subjectType') AS subject_type,
       (extra_data->>'subjectId')   AS subject_id
FROM audit_operation_log
WHERE deleted = false
  AND biz_id = $childFolderId
  AND operation_type = 'GRANT_ADD'
ORDER BY operation_time ASC
LIMIT 1
"@
$a3Out = Invoke-DbQuery -Label 'A3 query grant_add event' -Sql $a3Sql -PassThruOutput
Assert-DbRowCount -Label 'A3 grant_add audit row exists' -Output $a3Out -MinRows 1
Assert-DbCell -Label 'A3 subject_type equals USER' -Output $a3Out -RowSelector 'grant_ids=' -ColumnName 'subject_type' -Expected 'USER'
Assert-DbCell -Label 'A3 subject_id equals sameDeptUserId' -Output $a3Out -RowSelector 'grant_ids=' -ColumnName 'subject_id' -Expected "$sameDeptUserId"
# grant_ids 非空校验：G1 一次插一条，所以至少应有一个 id（数字串）
$a3GrantIdsRow = @($a3Out -split "`r?`n" | Where-Object { $_ -match '^ROW ' }) | Select-Object -First 1
$a3GrantIdsValue = $null
if ($a3GrantIdsRow) {
    $a3Pairs = ($a3GrantIdsRow -replace '^ROW ', '') -split '\s\|\s'
    foreach ($pair in $a3Pairs) {
        $eqIdx = $pair.IndexOf('=')
        if ($eqIdx -ge 0 -and $pair.Substring(0, $eqIdx) -eq 'grant_ids') {
            $a3GrantIdsValue = $pair.Substring($eqIdx + 1)
            break
        }
    }
}
if ([string]::IsNullOrWhiteSpace($a3GrantIdsValue)) {
    throw "A3 grant_ids should be non-empty, actual='$a3GrantIdsValue'"
}
Write-Host "Assert-DbCell passed: A3 grant_ids non-empty => '$a3GrantIdsValue'"

# ============================================================
# Phase 4：delete / batchDelete / move / copy 验证用例（P1-P18）+ 审计扩展（A4-A6）
# 专用测试树 $p4Root 下隔离展开，避免影响 Phase 1/2/3 的资源
# ============================================================

Write-Step "Phase 4 setup: 创建专用测试根目录"
$p4RootResp = Create-Folder -Headers $adminHeaders -ParentId 0 -Name "p4_root_$nameSuffix" -Remark 'phase4 dedicated root' -Label 'POST p4 root'
Assert-Code -StepName 'Create p4 root' -Response $p4RootResp -ExpectedCode 0
$p4RootId = $p4RootResp.data.id

# --- P1-P4 删除用例所需 fixture
$p4DelTargetResp = Create-Folder -Headers $adminHeaders -ParentId $p4RootId -Name "p4_del_target_$nameSuffix" -Remark 'P1 owner DELETE target' -Label 'POST p4 del target'
Assert-Code -StepName 'Create P1 delete target' -Response $p4DelTargetResp -ExpectedCode 0
$p4DelTargetId = $p4DelTargetResp.data.id

# --- P5-P8 batchDelete fixture（父+两个子）
$p4BatchParentResp = Create-Folder -Headers $adminHeaders -ParentId $p4RootId -Name "p4_batch_parent_$nameSuffix" -Remark 'P5 batch parent' -Label 'POST p4 batch parent'
Assert-Code -StepName 'Create P5 batch parent' -Response $p4BatchParentResp -ExpectedCode 0
$p4BatchParentId = $p4BatchParentResp.data.id

$p4BatchChild1Resp = Create-Folder -Headers $adminHeaders -ParentId $p4BatchParentId -Name "p4_batch_child1_$nameSuffix" -Remark 'P5 batch child1' -Label 'POST p4 batch child1'
Assert-Code -StepName 'Create P5 batch child1' -Response $p4BatchChild1Resp -ExpectedCode 0
$p4BatchChild1Id = $p4BatchChild1Resp.data.id

$p4BatchChild2Resp = Create-Folder -Headers $adminHeaders -ParentId $p4BatchParentId -Name "p4_batch_child2_$nameSuffix" -Remark 'P5 batch child2' -Label 'POST p4 batch child2'
Assert-Code -StepName 'Create P5 batch child2' -Response $p4BatchChild2Resp -ExpectedCode 0
$p4BatchChild2Id = $p4BatchChild2Resp.data.id

# P6 级联清理 fixture：单独建一个 folder，给它加 grant + manager + favorite，再用 batchDelete 测试级联
$p4CascadeResp = Create-Folder -Headers $adminHeaders -ParentId $p4RootId -Name "p4_cascade_$nameSuffix" -Remark 'P6 cascade target' -Label 'POST p4 cascade target'
Assert-Code -StepName 'Create P6 cascade target' -Response $p4CascadeResp -ExpectedCode 0
$p4CascadeId = $p4CascadeResp.data.id

# 给 cascade folder 加 grant（admin 操作）
$p4CascadeGrantResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders/$p4CascadeId/grants" -Headers $adminHeaders -Body @{
    subjectType = 'USER'
    subjectId = "$sameDeptUserId"
    permissionCodes = @('FOLDER_VIEW')
    grantScope = 'SELF'
}
Assert-Code -StepName 'Add grant on P6 cascade target' -Response $p4CascadeGrantResp -ExpectedCode 0

# 给 cascade folder 加 manager（roleGrantUser 有 FOLDER_ADMIN）
$p4CascadeMgrResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders/$p4CascadeId/managers" -Headers $adminHeaders -Body @{
    userId = $roleGrantUserId
    manageScope = 'SELF'
}
Assert-Code -StepName 'Add manager on P6 cascade target' -Response $p4CascadeMgrResp -ExpectedCode 0

# 给 cascade folder 直接 INSERT favorite（无接口）
$p4CascadeFavRecordId = New-TestId
Invoke-DbUpdate -Label 'INSERT favorite for P6' -Sql @"
INSERT INTO doc_folder_favorite (id, folder_id, user_id, created_at, created_by, deleted)
VALUES ($p4CascadeFavRecordId, $p4CascadeId, $sameDeptUserId, now(), 'p4_cascade_setup', false);
"@

# 不存在 id 占位（P3/P7）
$p4FakeId = New-TestId

# --- P9-P14 move 用例 fixture
$p4MoveSrcResp = Create-Folder -Headers $adminHeaders -ParentId $p4RootId -Name "p4_move_src_$nameSuffix" -Remark 'P9 move source' -Label 'POST p4 move src'
Assert-Code -StepName 'Create P9 move src' -Response $p4MoveSrcResp -ExpectedCode 0
$p4MoveSrcId = $p4MoveSrcResp.data.id

$p4MoveSrcChildResp = Create-Folder -Headers $adminHeaders -ParentId $p4MoveSrcId -Name "p4_move_src_child_$nameSuffix" -Remark 'P9 move src child' -Label 'POST p4 move src child'
Assert-Code -StepName 'Create P9 move src child' -Response $p4MoveSrcChildResp -ExpectedCode 0
$p4MoveSrcChildId = $p4MoveSrcChildResp.data.id

$p4MoveDstResp = Create-Folder -Headers $adminHeaders -ParentId $p4RootId -Name "p4_move_dst_$nameSuffix" -Remark 'P9 move target parent' -Label 'POST p4 move dst'
Assert-Code -StepName 'Create P9 move dst' -Response $p4MoveDstResp -ExpectedCode 0
$p4MoveDstId = $p4MoveDstResp.data.id

# P11/P12 防自环 + 防后代循环
$p4CycSrcResp = Create-Folder -Headers $adminHeaders -ParentId $p4RootId -Name "p4_cyc_src_$nameSuffix" -Remark 'P11/P12 cycle src' -Label 'POST p4 cyc src'
Assert-Code -StepName 'Create P11/P12 cyc src' -Response $p4CycSrcResp -ExpectedCode 0
$p4CycSrcId = $p4CycSrcResp.data.id

$p4CycChildResp = Create-Folder -Headers $adminHeaders -ParentId $p4CycSrcId -Name "p4_cyc_child_$nameSuffix" -Remark 'P12 cycle descendant' -Label 'POST p4 cyc child'
Assert-Code -StepName 'Create P12 cyc child' -Response $p4CycChildResp -ExpectedCode 0
$p4CycChildId = $p4CycChildResp.data.id

# P13 同名冲突：先在 dst 下放一个同名子目录，再尝试把同名 src move 进去
$p4DupDstParentResp = Create-Folder -Headers $adminHeaders -ParentId $p4RootId -Name "p4_dup_dst_parent_$nameSuffix" -Remark 'P13 dup dst parent' -Label 'POST p4 dup dst parent'
Assert-Code -StepName 'Create P13 dup dst parent' -Response $p4DupDstParentResp -ExpectedCode 0
$p4DupDstParentId = $p4DupDstParentResp.data.id

$p4DupCommonName = "p4_dup_common_$nameSuffix"
$p4DupExistingResp = Create-Folder -Headers $adminHeaders -ParentId $p4DupDstParentId -Name $p4DupCommonName -Remark 'P13 occupant in dst' -Label 'POST p4 dup existing'
Assert-Code -StepName 'Create P13 occupant under dup dst' -Response $p4DupExistingResp -ExpectedCode 0

$p4DupSrcParentResp = Create-Folder -Headers $adminHeaders -ParentId $p4RootId -Name "p4_dup_src_parent_$nameSuffix" -Remark 'P13 dup src parent' -Label 'POST p4 dup src parent'
Assert-Code -StepName 'Create P13 dup src parent' -Response $p4DupSrcParentResp -ExpectedCode 0
$p4DupSrcParentId = $p4DupSrcParentResp.data.id

$p4DupSrcResp = Create-Folder -Headers $adminHeaders -ParentId $p4DupSrcParentId -Name $p4DupCommonName -Remark 'P13 dup src (same name)' -Label 'POST p4 dup src'
Assert-Code -StepName 'Create P13 dup src (same name in different parent)' -Response $p4DupSrcResp -ExpectedCode 0
$p4DupSrcId = $p4DupSrcResp.data.id

# P14 层级溢出：在 p4Root 下建 7 层链（level 1..7），目标=最深；源是 p4Root 下的 level=1 节点+子节点
$p4OvfChainResp1 = Create-Folder -Headers $adminHeaders -ParentId $p4RootId -Name "p4_ovf_chain_1_$nameSuffix" -Remark 'P14 chain l1' -Label 'POST p4 ovf chain 1'
Assert-Code -StepName 'Create P14 chain l1' -Response $p4OvfChainResp1 -ExpectedCode 0
$p4OvfChainLeafId = $p4OvfChainResp1.data.id
for ($lv = 2; $lv -le 7; $lv++) {
    $p4OvfChainNext = Create-Folder -Headers $adminHeaders -ParentId $p4OvfChainLeafId -Name "p4_ovf_chain_${lv}_$nameSuffix" -Remark "P14 chain l$lv" -Label "POST p4 ovf chain $lv"
    Assert-Code -StepName "Create P14 chain l$lv" -Response $p4OvfChainNext -ExpectedCode 0
    $p4OvfChainLeafId = $p4OvfChainNext.data.id
}

$p4OvfSrcResp = Create-Folder -Headers $adminHeaders -ParentId $p4RootId -Name "p4_ovf_src_$nameSuffix" -Remark 'P14 ovf src' -Label 'POST p4 ovf src'
Assert-Code -StepName 'Create P14 ovf src' -Response $p4OvfSrcResp -ExpectedCode 0
$p4OvfSrcId = $p4OvfSrcResp.data.id

$p4OvfSrcChildResp = Create-Folder -Headers $adminHeaders -ParentId $p4OvfSrcId -Name "p4_ovf_src_child_$nameSuffix" -Remark 'P14 ovf src child' -Label 'POST p4 ovf src child'
Assert-Code -StepName 'Create P14 ovf src child' -Response $p4OvfSrcChildResp -ExpectedCode 0
$p4OvfSrcChildId = $p4OvfSrcChildResp.data.id

# --- P15-P18 copy 用例 fixture
$p4CopySrcResp = Create-Folder -Headers $adminHeaders -ParentId $p4RootId -Name "p4_copy_src_$nameSuffix" -Remark 'P15 copy src' -Label 'POST p4 copy src'
Assert-Code -StepName 'Create P15 copy src' -Response $p4CopySrcResp -ExpectedCode 0
$p4CopySrcId = $p4CopySrcResp.data.id

$p4CopySrcChildResp = Create-Folder -Headers $adminHeaders -ParentId $p4CopySrcId -Name "p4_copy_src_child_$nameSuffix" -Remark 'P15 copy src child' -Label 'POST p4 copy src child'
Assert-Code -StepName 'Create P15 copy src child' -Response $p4CopySrcChildResp -ExpectedCode 0
$p4CopySrcChildId = $p4CopySrcChildResp.data.id

$p4CopyDstResp = Create-Folder -Headers $adminHeaders -ParentId $p4RootId -Name "p4_copy_dst_$nameSuffix" -Remark 'P15 copy dst parent' -Label 'POST p4 copy dst'
Assert-Code -StepName 'Create P15 copy dst' -Response $p4CopyDstResp -ExpectedCode 0
$p4CopyDstId = $p4CopyDstResp.data.id

# P16 grant-not-copied fixture
$p4GrantSrcResp = Create-Folder -Headers $adminHeaders -ParentId $p4RootId -Name "p4_grant_src_$nameSuffix" -Remark 'P16 copy w/ grant' -Label 'POST p4 grant src'
Assert-Code -StepName 'Create P16 grant src' -Response $p4GrantSrcResp -ExpectedCode 0
$p4GrantSrcId = $p4GrantSrcResp.data.id
$p4GrantOnSrcResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders/$p4GrantSrcId/grants" -Headers $adminHeaders -Body @{
    subjectType = 'USER'
    subjectId = "$sameDeptUserId"
    permissionCodes = @('FOLDER_VIEW')
    grantScope = 'SELF'
}
Assert-Code -StepName 'Pre-grant on P16 src' -Response $p4GrantOnSrcResp -ExpectedCode 0

# P18 copy 同名冲突 fixture
$p4CopyDupDstResp = Create-Folder -Headers $adminHeaders -ParentId $p4RootId -Name "p4_copy_dup_dst_$nameSuffix" -Remark 'P18 copy dup dst' -Label 'POST p4 copy dup dst'
Assert-Code -StepName 'Create P18 copy dup dst' -Response $p4CopyDupDstResp -ExpectedCode 0
$p4CopyDupDstId = $p4CopyDupDstResp.data.id

$p4CopyDupCommonName = "p4_copy_dup_common_$nameSuffix"
$p4CopyDupOccupantResp = Create-Folder -Headers $adminHeaders -ParentId $p4CopyDupDstId -Name $p4CopyDupCommonName -Remark 'P18 dup occupant' -Label 'POST p4 copy dup occupant'
Assert-Code -StepName 'Create P18 dup occupant' -Response $p4CopyDupOccupantResp -ExpectedCode 0

# P18 源放 p4Root 下，名字与 occupant 同名 → 复制到 dst 时冲突
$p4CopyDupSrcResp = Create-Folder -Headers $adminHeaders -ParentId $p4RootId -Name $p4CopyDupCommonName -Remark 'P18 dup src (same name)' -Label 'POST p4 copy dup src'
Assert-Code -StepName 'Create P18 dup src (same name in different parent)' -Response $p4CopyDupSrcResp -ExpectedCode 0
$p4CopyDupSrcId = $p4CopyDupSrcResp.data.id

# =====
# P1-P4: delete
# =====
Write-Step "P1: owner DELETE non-root → 200"
$p1Resp = Invoke-JsonApi -Method Delete -Uri "$baseUrl/api/v1/folders/$p4DelTargetId" -Headers $adminHeaders -Body $null
Show-Response -Label "P1 DELETE /folders/$p4DelTargetId" -Response $p1Resp
Assert-Code -StepName 'P1 owner delete non-root' -Response $p1Resp -ExpectedCode 0

Write-Step "P2: non-admin DELETE root → 4032001"
$p2Resp = Invoke-JsonApi -Method Delete -Uri "$baseUrl/api/v1/folders/$rootFolderId" -Headers $sameDeptHeaders -Body $null
Show-Response -Label "P2 DELETE root by same dept user" -Response $p2Resp
Assert-Code -StepName 'P2 root delete denied for non-admin' -Response $p2Resp -ExpectedCode 4032001

Write-Step "P3: DELETE non-existent id → 4042001"
$p3Resp = Invoke-JsonApi -Method Delete -Uri "$baseUrl/api/v1/folders/$p4FakeId" -Headers $adminHeaders -Body $null
Show-Response -Label "P3 DELETE non-existent" -Response $p3Resp
Assert-Code -StepName 'P3 delete non-existent returns 4042001' -Response $p3Resp -ExpectedCode 4042001

Write-Step "P4: DELETE already-deleted id → 4042001"
$p4Resp = Invoke-JsonApi -Method Delete -Uri "$baseUrl/api/v1/folders/$p4DelTargetId" -Headers $adminHeaders -Body $null
Show-Response -Label "P4 DELETE already deleted" -Response $p4Resp
Assert-Code -StepName 'P4 re-delete returns 4042001' -Response $p4Resp -ExpectedCode 4042001

# =====
# P5-P8: batchDelete
# =====
Write-Step "P5: batchDelete [parent, child1, child2] 父子去重 + 全部 deleted=true"
$p5Resp = Invoke-JsonApi -Method Delete -Uri "$baseUrl/api/v1/folders/batch" -Headers $adminHeaders -Body @{
    folderIds = @($p4BatchParentId, $p4BatchChild1Id, $p4BatchChild2Id)
}
Show-Response -Label "P5 batchDelete parent+children" -Response $p5Resp
Assert-Code -StepName 'P5 batchDelete success' -Response $p5Resp -ExpectedCode 0
# 验证 DB 中三者均 deleted=true
$p5Out = Invoke-DbQuery -Label 'P5 verify deletion in DB' -Sql @"
SELECT id, deleted FROM doc_folder WHERE id IN ($p4BatchParentId, $p4BatchChild1Id, $p4BatchChild2Id) ORDER BY id;
"@ -PassThruOutput
Assert-DbRowCount -Label 'P5 three rows visible in DB' -Output $p5Out -MinRows 3
Assert-DbRowMatch -Label "P5 batch parent deleted=true" -Output $p5Out -RowSelector "id=$p4BatchParentId | deleted=t"
Assert-DbRowMatch -Label "P5 batch child1 deleted=true" -Output $p5Out -RowSelector "id=$p4BatchChild1Id | deleted=t"
Assert-DbRowMatch -Label "P5 batch child2 deleted=true" -Output $p5Out -RowSelector "id=$p4BatchChild2Id | deleted=t"

Write-Step "P6: batchDelete 级联清理 grant/manager/favorite"
$p6Resp = Invoke-JsonApi -Method Delete -Uri "$baseUrl/api/v1/folders/batch" -Headers $adminHeaders -Body @{
    folderIds = @($p4CascadeId)
}
Show-Response -Label "P6 batchDelete cascade" -Response $p6Resp
Assert-Code -StepName 'P6 batchDelete cascade success' -Response $p6Resp -ExpectedCode 0
# 验证 grant/manager/favorite 全 deleted=true
$p6Out = Invoke-DbQuery -Label 'P6 verify cascade in DB' -Sql @"
SELECT 'folder' AS kind, COUNT(*) AS active FROM doc_folder WHERE id = $p4CascadeId AND deleted = false
UNION ALL
SELECT 'grant' AS kind, COUNT(*) AS active FROM doc_folder_grant WHERE folder_id = $p4CascadeId AND deleted = false
UNION ALL
SELECT 'manager' AS kind, COUNT(*) AS active FROM doc_folder_manager WHERE folder_id = $p4CascadeId AND deleted = false
UNION ALL
SELECT 'favorite' AS kind, COUNT(*) AS active FROM doc_folder_favorite WHERE folder_id = $p4CascadeId AND deleted = false
ORDER BY kind;
"@ -PassThruOutput
Assert-DbCell -Label 'P6 folder no longer active' -Output $p6Out -RowSelector 'kind=folder' -ColumnName 'active' -Expected '0'
Assert-DbCell -Label 'P6 grant no longer active' -Output $p6Out -RowSelector 'kind=grant' -ColumnName 'active' -Expected '0'
Assert-DbCell -Label 'P6 manager no longer active' -Output $p6Out -RowSelector 'kind=manager' -ColumnName 'active' -Expected '0'
Assert-DbCell -Label 'P6 favorite no longer active' -Output $p6Out -RowSelector 'kind=favorite' -ColumnName 'active' -Expected '0'

Write-Step "P7: batchDelete 含非法 id → 4042001 + 无副作用"
# 这里用 $p4MoveSrcId（仍存活） + $p4FakeId 组合
$p7Resp = Invoke-JsonApi -Method Delete -Uri "$baseUrl/api/v1/folders/batch" -Headers $adminHeaders -Body @{
    folderIds = @($p4MoveSrcId, $p4FakeId)
}
Show-Response -Label "P7 batchDelete with invalid id" -Response $p7Resp
Assert-Code -StepName 'P7 batchDelete partial-fail returns 4042001' -Response $p7Resp -ExpectedCode 4042001
# 验证 $p4MoveSrcId 仍未删除
$p7Out = Invoke-DbQuery -Label 'P7 verify rollback' -Sql "SELECT id, deleted FROM doc_folder WHERE id = $p4MoveSrcId;" -PassThruOutput
Assert-DbRowMatch -Label "P7 move src 未被删除（事务回滚）" -Output $p7Out -RowSelector "id=$p4MoveSrcId | deleted=f"

Write-Step "P8: 非管理员 batchDelete 含根级 id → 4032001"
$p8Resp = Invoke-JsonApi -Method Delete -Uri "$baseUrl/api/v1/folders/batch" -Headers $sameDeptHeaders -Body @{
    folderIds = @($rootFolderId)
}
Show-Response -Label "P8 batchDelete root by non-admin" -Response $p8Resp
Assert-Code -StepName 'P8 root batchDelete denied' -Response $p8Resp -ExpectedCode 4032001

# =====
# P9-P14: move
# =====
Write-Step "P9: owner move 普通目录到新父 → 200 + ancestorIds 重写"
$p9Resp = Invoke-JsonApi -Method Patch -Uri "$baseUrl/api/v1/folders/$p4MoveSrcId/move" -Headers $adminHeaders -Body @{
    targetParentId = $p4MoveDstId
}
Show-Response -Label "P9 move src under dst" -Response $p9Resp
Assert-Code -StepName 'P9 move success' -Response $p9Resp -ExpectedCode 0
# 验证 move 后 ancestorIds：源新 ancestor 应以 dst 的 ancestor + ",src" 结尾
$p9Out = Invoke-DbQuery -Label 'P9 verify ancestorIds' -Sql @"
SELECT id, parent_id, ancestor_ids, level FROM doc_folder WHERE id IN ($p4MoveSrcId, $p4MoveSrcChildId) ORDER BY id;
"@ -PassThruOutput
Assert-DbRowCount -Label 'P9 src + child rows visible' -Output $p9Out -MinRows 2
Assert-DbCell -Label 'P9 src parent_id 已切到 dst' -Output $p9Out -RowSelector "id=$p4MoveSrcId" -ColumnName 'parent_id' -Expected "$p4MoveDstId"

Write-Step "P10: move targetParentId=0 → 4002009"
$p10Resp = Invoke-JsonApi -Method Patch -Uri "$baseUrl/api/v1/folders/$p4CycSrcId/move" -Headers $adminHeaders -Body @{
    targetParentId = 0
}
Show-Response -Label "P10 move to root" -Response $p10Resp
Assert-Code -StepName 'P10 move to root forbidden' -Response $p10Resp -ExpectedCode 4002009

Write-Step "P11: move 到自身 → 4002008"
$p11Resp = Invoke-JsonApi -Method Patch -Uri "$baseUrl/api/v1/folders/$p4CycSrcId/move" -Headers $adminHeaders -Body @{
    targetParentId = $p4CycSrcId
}
Show-Response -Label "P11 move to self" -Response $p11Resp
Assert-Code -StepName 'P11 move to self cycle' -Response $p11Resp -ExpectedCode 4002008

Write-Step "P12: move 到自己的后代 → 4002008"
$p12Resp = Invoke-JsonApi -Method Patch -Uri "$baseUrl/api/v1/folders/$p4CycSrcId/move" -Headers $adminHeaders -Body @{
    targetParentId = $p4CycChildId
}
Show-Response -Label "P12 move to descendant" -Response $p12Resp
Assert-Code -StepName 'P12 move to descendant cycle' -Response $p12Resp -ExpectedCode 4002008

Write-Step "P13: move 至同名占位的目标父 → 4002001"
$p13Resp = Invoke-JsonApi -Method Patch -Uri "$baseUrl/api/v1/folders/$p4DupSrcId/move" -Headers $adminHeaders -Body @{
    targetParentId = $p4DupDstParentId
}
Show-Response -Label "P13 move name conflict" -Response $p13Resp
Assert-Code -StepName 'P13 move name duplicated' -Response $p13Resp -ExpectedCode 4002001

Write-Step "P14: move 后层级超过 8 → 4002002"
$p14Resp = Invoke-JsonApi -Method Patch -Uri "$baseUrl/api/v1/folders/$p4OvfSrcId/move" -Headers $adminHeaders -Body @{
    targetParentId = $p4OvfChainLeafId
}
Show-Response -Label "P14 move level overflow" -Response $p14Resp
Assert-Code -StepName 'P14 move level exceeded' -Response $p14Resp -ExpectedCode 4002002

# =====
# P15-P18: copy
# =====
Write-Step "P15: owner copy 子树 → 200 + 新根子树存在"
$p15Resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders/$p4CopySrcId/copy" -Headers $adminHeaders -Body @{
    targetParentId = $p4CopyDstId
}
Show-Response -Label "P15 copy src to dst" -Response $p15Resp
Assert-Code -StepName 'P15 copy success' -Response $p15Resp -ExpectedCode 0
$p15NewRootId = $p15Resp.data.id
Assert-True -StepName 'P15 returned newRootId not null' -Actual ($null -ne $p15NewRootId)
# 用 children 列表验证新根下也有子节点（与源同名）
$p15ChildrenResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/children?parentId=$p15NewRootId" -Headers $adminHeaders -Body $null
Assert-Code -StepName 'P15 list children of new root' -Response $p15ChildrenResp -ExpectedCode 0
Assert-True -StepName 'P15 new root has child copied' -Actual ($p15ChildrenResp.data.Count -ge 1)

Write-Step "P16: copy 不复制 grant"
$p16Resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders/$p4GrantSrcId/copy" -Headers $adminHeaders -Body @{
    targetParentId = $p4CopyDstId
    targetName = "p4_grant_copy_$nameSuffix"
}
Show-Response -Label "P16 copy folder with grant" -Response $p16Resp
Assert-Code -StepName 'P16 copy w/ grant success' -Response $p16Resp -ExpectedCode 0
$p16NewRootId = $p16Resp.data.id
$p16GrantsResp = Invoke-JsonApi -Method Get -Uri "$baseUrl/api/v1/folders/$p16NewRootId/grants" -Headers $adminHeaders -Body $null
Assert-Code -StepName 'P16 list new root grants' -Response $p16GrantsResp -ExpectedCode 0
Assert-Equal -StepName 'P16 new root has no grants (copy did not bring grant)' -Actual $p16GrantsResp.data.Count -Expected 0

Write-Step "P17: copy 后层级超过 8 → 4002002"
$p17Resp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders/$p4OvfSrcId/copy" -Headers $adminHeaders -Body @{
    targetParentId = $p4OvfChainLeafId
}
Show-Response -Label "P17 copy level overflow" -Response $p17Resp
Assert-Code -StepName 'P17 copy level exceeded' -Response $p17Resp -ExpectedCode 4002002

Write-Step "P18: copy 同名冲突——无 targetName 报错；有 targetName 成功"
$p18FailResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders/$p4CopyDupSrcId/copy" -Headers $adminHeaders -Body @{
    targetParentId = $p4CopyDupDstId
}
Show-Response -Label "P18 copy without targetName" -Response $p18FailResp
Assert-Code -StepName 'P18 copy dup name fails without targetName' -Response $p18FailResp -ExpectedCode 4002001

$p18OkResp = Invoke-JsonApi -Method Post -Uri "$baseUrl/api/v1/folders/$p4CopyDupSrcId/copy" -Headers $adminHeaders -Body @{
    targetParentId = $p4CopyDupDstId
    targetName = "p4_copy_dup_renamed_$nameSuffix"
}
Show-Response -Label "P18 copy with targetName" -Response $p18OkResp
Assert-Code -StepName 'P18 copy with targetName succeeds' -Response $p18OkResp -ExpectedCode 0

# =====
# A4-A6: 审计扩展
# =====
Write-Step "A4: audit_operation_log 覆盖 Phase 4 各 op_type"
$a4Sql = @"
SELECT operation_type, COUNT(*) AS cnt
FROM audit_operation_log
WHERE deleted = false
  AND module_code = 'FOLDER'
  AND biz_type = 'FOLDER'
  AND biz_id IN ($p4DelTargetId, $p4BatchParentId, $p4MoveSrcId, $p15NewRootId)
  AND operation_type IN ('DELETE','BATCH_DELETE','MOVE','COPY')
GROUP BY operation_type
ORDER BY operation_type
"@
$a4Out = Invoke-DbQuery -Label 'A4 audit phase4 op coverage' -Sql $a4Sql -PassThruOutput
foreach ($expectedOp in @('DELETE','BATCH_DELETE','MOVE','COPY')) {
    Assert-DbRowMatch -Label "A4 audit contains operation_type=$expectedOp" -Output $a4Out -RowSelector "operation_type=$expectedOp"
}

Write-Step "A5: MOVE 审计 extraData.affectedFolderIds 含源根 id"
$a5Sql = @"
SELECT (extra_data->>'affectedFolderIds') AS affected_ids,
       (extra_data->>'affectedCount') AS affected_count
FROM audit_operation_log
WHERE deleted = false
  AND biz_id = $p4MoveSrcId
  AND operation_type = 'MOVE'
ORDER BY operation_time DESC
LIMIT 1
"@
$a5Out = Invoke-DbQuery -Label 'A5 query move audit extraData' -Sql $a5Sql -PassThruOutput
Assert-DbRowCount -Label 'A5 move audit row exists' -Output $a5Out -MinRows 1
$a5Row = @($a5Out -split "`r?`n" | Where-Object { $_ -match '^ROW ' }) | Select-Object -First 1
$a5AffectedIds = $null
if ($a5Row) {
    $a5Pairs = ($a5Row -replace '^ROW ', '') -split '\s\|\s'
    foreach ($pair in $a5Pairs) {
        $eqIdx = $pair.IndexOf('=')
        if ($eqIdx -ge 0 -and $pair.Substring(0, $eqIdx) -eq 'affected_ids') {
            $a5AffectedIds = $pair.Substring($eqIdx + 1)
            break
        }
    }
}
if ([string]::IsNullOrWhiteSpace($a5AffectedIds) -or -not ($a5AffectedIds.Contains("$p4MoveSrcId"))) {
    throw "A5 affectedFolderIds 应当含源根 id=$p4MoveSrcId，实际='$a5AffectedIds'"
}
Write-Host "Assert-DbCell passed: A5 affectedFolderIds contains src id => '$a5AffectedIds'"

Write-Step "A6: COPY 审计 afterData.rootNewId 等于 P15 返回的 newRootId"
$a6Sql = @"
SELECT (after_data->>'rootNewId') AS root_new_id
FROM audit_operation_log
WHERE deleted = false
  AND biz_id = $p15NewRootId
  AND operation_type = 'COPY'
ORDER BY operation_time DESC
LIMIT 1
"@
$a6Out = Invoke-DbQuery -Label 'A6 query copy audit afterData' -Sql $a6Sql -PassThruOutput
Assert-DbRowCount -Label 'A6 copy audit row exists' -Output $a6Out -MinRows 1
Assert-DbCell -Label 'A6 rootNewId 与接口返回一致' -Output $a6Out -RowSelector 'root_new_id=' -ColumnName 'root_new_id' -Expected "$p15NewRootId"

Write-Step "Verification finished"
Write-Host "prefix=$prefix"
Write-Host "adminDeptId=$adminDeptId"
Write-Host "otherDeptId=$otherDeptId"
Write-Host "secondaryDeptId=$secondaryDeptId"
Write-Host "sameDeptUserId=$sameDeptUserId"
Write-Host "otherDeptUserId=$otherDeptUserId"
Write-Host "managerDescUserId=$managerDescUserId"
Write-Host "grantUserId=$grantUserId"
Write-Host "roleGrantUserId=$roleGrantUserId"
Write-Host "negativeDeptUserId=$negativeDeptUserId"
Write-Host "rootFolderId=$rootFolderId"
Write-Host "childFolderId=$childFolderId"
Write-Host "sortParentId=$sortParentId"
Write-Host "sortChild1Id=$sortChild1Id"
Write-Host "sortChild2Id=$sortChild2Id"
Write-Host "sameNameParent1Id=$sameNameParent1Id"
Write-Host "sameNameParent2Id=$sameNameParent2Id"
Write-Host "sameNameChild1Id=$sameNameChild1Id"
Write-Host "sameNameChild2Id=$sameNameChild2Id"
Write-Host "permRootId=$permRootId"
Write-Host "managerSelfTargetId=$managerSelfTargetId"
Write-Host "managerSelfHiddenChildId=$managerSelfHiddenChildId"
Write-Host "managerSelfCreatedChildId=$managerSelfCreatedChildId"
Write-Host "managerDescParentId=$managerDescParentId"
Write-Host "managerDescChildId=$managerDescChildId"
Write-Host "managerDescGrandchildId=$managerDescGrandchildId"
Write-Host "grantUserViewTargetId=$grantUserViewTargetId"
Write-Host "grantUserEditTargetId=$grantUserEditTargetId"
Write-Host "grantUserRenameTargetId=$grantUserRenameTargetId"
Write-Host "grantUserEditCreatedChildId=$grantUserEditCreatedChildId"
Write-Host "roleGrantTargetId=$roleGrantTargetId"
Write-Host "listParentId=$listParentId"
Write-Host "listVisibleChildId=$listVisibleChildId"
Write-Host "listHiddenChildId=$listHiddenChildId"
Write-Host "listHiddenGrandchildId=$listHiddenGrandchildId"
Write-Host "deptGrantRootId=$deptGrantRootId"
Write-Host "deptGrantChildId=$deptGrantChildId"
Write-Host "deptGrantGrandchildId=$deptGrantGrandchildId"
Write-Host "levelFolderIds=$($levelFolderIds -join ',')"
