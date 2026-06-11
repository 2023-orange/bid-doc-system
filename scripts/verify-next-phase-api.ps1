param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$AdminUsername,
    [string]$AdminPassword,
    [string]$UserUsername,
    [string]$UserPassword,
    [string]$FolderId,
    [string]$DocumentId
)

$ErrorActionPreference = "Stop"

function Invoke-Json {
    param(
        [string]$Method,
        [string]$Url,
        [hashtable]$Headers = @{},
        $Body = $null
    )
    $params = @{
        Method = $Method
        Uri = $Url
        Headers = $Headers
    }
    if ($null -ne $Body) {
        $params.ContentType = "application/json; charset=utf-8"
        $params.Body = ($Body | ConvertTo-Json -Depth 20)
    }
    return Invoke-RestMethod @params
}

function Login {
    param([string]$Username, [string]$Password)
    if ([string]::IsNullOrWhiteSpace($Username) -or [string]::IsNullOrWhiteSpace($Password)) {
        return $null
    }
    $resp = Invoke-Json -Method "POST" -Url "$BaseUrl/api/v1/auth/login" -Body @{
        username = $Username
        password = $Password
        rememberMe = $false
    }
    if ($resp.code -ne 0) {
        throw "Login failed for $Username: $($resp.message)"
    }
    return $resp.data.token
}

function Assert-Code {
    param($Resp, [int]$Code, [string]$Name)
    if ($Resp.code -ne $Code) {
        throw "$Name expected code $Code but got $($Resp.code): $($Resp.message)"
    }
    Write-Host "[PASS] $Name"
}

$adminToken = Login -Username $AdminUsername -Password $AdminPassword
$userToken = Login -Username $UserUsername -Password $UserPassword

if ($adminToken) {
    $adminHeaders = @{ Authorization = $adminToken }
    $auditResp = Invoke-Json -Method "GET" -Url "$BaseUrl/api/v1/audit/logs?page=1&size=5" -Headers $adminHeaders
    Assert-Code -Resp $auditResp -Code 0 -Name "admin audit query"

    $tagResp = Invoke-Json -Method "GET" -Url "$BaseUrl/api/v1/tags" -Headers $adminHeaders
    Assert-Code -Resp $tagResp -Code 0 -Name "tag list"

    $notificationResp = Invoke-Json -Method "GET" -Url "$BaseUrl/api/v1/notifications/unread-count" -Headers $adminHeaders
    Assert-Code -Resp $notificationResp -Code 0 -Name "notification unread count"
}

if ($userToken) {
    $userHeaders = @{ Authorization = $userToken }
    $auditDenied = Invoke-Json -Method "GET" -Url "$BaseUrl/api/v1/audit/logs?page=1&size=5" -Headers $userHeaders
    Assert-Code -Resp $auditDenied -Code 4031002 -Name "non-admin audit denied"

    if (-not [string]::IsNullOrWhiteSpace($FolderId)) {
        $permResp = Invoke-Json -Method "GET" -Url "$BaseUrl/api/v1/folders/$FolderId/permissions/me" -Headers $userHeaders
        Assert-Code -Resp $permResp -Code 0 -Name "folder permissions me"

        $searchResp = Invoke-Json -Method "GET" -Url "$BaseUrl/api/v1/folders/search?keyword=&page=1&size=5" -Headers $userHeaders
        Assert-Code -Resp $searchResp -Code 0 -Name "folder search"

        $favResp = Invoke-Json -Method "POST" -Url "$BaseUrl/api/v1/folders/$FolderId/favorite" -Headers $userHeaders
        Assert-Code -Resp $favResp -Code 0 -Name "favorite folder"

        $favListResp = Invoke-Json -Method "GET" -Url "$BaseUrl/api/v1/folders/favorites" -Headers $userHeaders
        Assert-Code -Resp $favListResp -Code 0 -Name "favorite list"
    }

    if (-not [string]::IsNullOrWhiteSpace($DocumentId)) {
        $docSearchResp = Invoke-Json -Method "GET" -Url "$BaseUrl/api/v1/documents/search?page=1&size=5&favoriteFolderOnly=false" -Headers $userHeaders
        Assert-Code -Resp $docSearchResp -Code 0 -Name "document enhanced search"

        $previewResp = Invoke-WebRequest -Method "GET" -Uri "$BaseUrl/api/v1/documents/$DocumentId/preview" -Headers $userHeaders -SkipHttpErrorCheck
        if ($previewResp.StatusCode -ge 200 -and $previewResp.StatusCode -lt 500) {
            Write-Host "[PASS] document preview status $($previewResp.StatusCode)"
        } else {
            throw "document preview unexpected status $($previewResp.StatusCode)"
        }
    }
}

Write-Host "next phase api smoke finished"
