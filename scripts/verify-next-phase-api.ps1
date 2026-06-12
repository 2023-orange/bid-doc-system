param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$AdminUsername,
    [string]$AdminPassword,
    [string]$UserUsername,
    [string]$UserPassword,
    [string]$OwnerUsername = "zhang_manager",
    [string]$OwnerPassword = "12345678",
    [string]$OutsiderUsername = "chen_quality",
    [string]$OutsiderPassword = "12345678",
    [string]$FolderId,
    [string]$DocumentId,
    [string]$ProjectId,
    [string]$TemplateId,
    [switch]$RunEndToEnd,
    [switch]$CleanupEndToEndData,
    [string]$TestDataPrefix = "E2E-CLOSE",
    [string]$EndToEndFolderId,
    [string]$OwnerDeptId = "3001000000000000001",
    [string]$OwnerUserId = "3002000000000000002",
    [string]$MemberUserId = "3002000000000000004",
    [string]$OutsiderUserId = "3002000000000000005",
    [string]$PsqlPath = "D:\PostgreSQL\13\bin\psql.exe",
    [string]$DbHost = "localhost",
    [int]$DbPort = 5432,
    [string]$DbName = "bid_doc_system",
    [string]$DbUser = "postgres"
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
        throw "Login failed for ${Username}: $($resp.message)"
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

function Assert-True {
    param([bool]$Condition, [string]$Name, [string]$Message = "")
    if (-not $Condition) {
        if ([string]::IsNullOrWhiteSpace($Message)) {
            throw "$Name failed"
        }
        throw "$Name failed: $Message"
    }
    Write-Host "[PASS] $Name"
}

function Invoke-UploadDocument {
    param(
        [string]$UploadFolderId,
        [hashtable]$Headers,
        [string]$Name,
        [string]$Content
    )
    $tempFile = Join-Path ([System.IO.Path]::GetTempPath()) "$Name.txt"
    Set-Content -LiteralPath $tempFile -Value $Content -Encoding UTF8
    try {
        return Invoke-RestMethod -Method "POST" `
            -Uri "$BaseUrl/api/v1/folders/$UploadFolderId/documents" `
            -Headers $Headers `
            -Form @{
                file = Get-Item -LiteralPath $tempFile
                name = "$Name.txt"
                remark = "end-to-end smoke document"
                changeLog = "initial upload"
            }
    } finally {
        Remove-Item -LiteralPath $tempFile -Force -ErrorAction SilentlyContinue
    }
}

function Get-PendingTaskId {
    param(
        [hashtable]$Headers,
        [string]$InstanceId
    )
    $tasksResp = Invoke-Json -Method "GET" -Url "$BaseUrl/api/v1/approvals/tasks?status=PENDING" -Headers $Headers
    Assert-Code -Resp $tasksResp -Code 0 -Name "approval pending task query"
    $task = @($tasksResp.data | Where-Object { "$($_.instanceId)" -eq "$InstanceId" } | Select-Object -First 1)
    Assert-True -Condition ($null -ne $task -and $task.Count -gt 0) -Name "approval pending task found" -Message "instanceId=$InstanceId"
    return $task[0].taskId
}

function Assert-DocumentStatusBySearch {
    param(
        [hashtable]$Headers,
        [string]$Name,
        [string]$Status
    )
    $encodedName = [System.Uri]::EscapeDataString($Name)
    $resp = Invoke-Json -Method "GET" -Url "$BaseUrl/api/v1/documents/search?keyword=$encodedName&documentStatus=$Status&page=1&size=10" -Headers $Headers
    Assert-Code -Resp $resp -Code 0 -Name "document status search $Status"
    $found = @($resp.data.list | Where-Object { $_.name -eq $Name })
    Assert-True -Condition ($found.Count -gt 0) -Name "document status is $Status" -Message $Name
}

function Get-ChecklistItem {
    param(
        [hashtable]$Headers,
        [string]$CurrentProjectId,
        [string]$ItemName
    )
    $resp = Invoke-Json -Method "GET" -Url "$BaseUrl/api/v1/projects/$CurrentProjectId/checklist" -Headers $Headers
    Assert-Code -Resp $resp -Code 0 -Name "project checklist query"
    $item = @($resp.data | Where-Object { $_.itemName -eq $ItemName } | Select-Object -First 1)
    Assert-True -Condition ($null -ne $item -and $item.Count -gt 0) -Name "checklist item found" -Message $ItemName
    return $item[0]
}

function Assert-ChecklistStatus {
    param(
        [hashtable]$Headers,
        [string]$CurrentProjectId,
        [string]$ItemName,
        [string]$ExpectedStatus
    )
    $item = Get-ChecklistItem -Headers $Headers -CurrentProjectId $CurrentProjectId -ItemName $ItemName
    Assert-True -Condition ($item.status -eq $ExpectedStatus) -Name "checklist item $ItemName status $ExpectedStatus" -Message "actual=$($item.status)"
}

function Assert-ProjectVisibleInList {
    param(
        [hashtable]$Headers,
        [string]$CurrentProjectId,
        [string]$Name
    )
    $resp = Invoke-Json -Method "GET" -Url "$BaseUrl/api/v1/projects?page=1&size=20" -Headers $Headers
    Assert-Code -Resp $resp -Code 0 -Name $Name
    $found = @($resp.data.list | Where-Object { "$($_.id)" -eq "$CurrentProjectId" })
    Assert-True -Condition ($found.Count -gt 0) -Name "$Name contains project" -Message "projectId=$CurrentProjectId"
}

function Invoke-EndToEndCleanup {
    param(
        [string]$CurrentProjectId,
        [string]$CurrentTemplateId,
        [string[]]$DocumentIds,
        [string[]]$ApprovalInstanceIds
    )
    if (-not $CleanupEndToEndData) {
        Write-Host "[INFO] e2e cleanup skipped; pass -CleanupEndToEndData to soft-delete generated project/template/document data"
        return
    }
    if (-not (Test-Path -LiteralPath $PsqlPath)) {
        throw "Cleanup requires psql path: $PsqlPath"
    }
    if ([string]::IsNullOrWhiteSpace($env:PGPASSWORD)) {
        throw "Cleanup requires PGPASSWORD environment variable for PostgreSQL"
    }

    $docIdSql = if ($DocumentIds -and $DocumentIds.Count -gt 0) { $DocumentIds -join "," } else { "0" }
    $instanceIdSql = if ($ApprovalInstanceIds -and $ApprovalInstanceIds.Count -gt 0) { $ApprovalInstanceIds -join "," } else { "0" }
    $sql = @"
update bid_project_checklist_document
set deleted = true
where checklist_item_id in (select id from bid_project_checklist_item where project_id = $CurrentProjectId);

update bid_project_checklist_item set deleted = true where project_id = $CurrentProjectId;
update bid_project_member set deleted = true where project_id = $CurrentProjectId;
update bid_project set deleted = true where id = $CurrentProjectId;

update bid_checklist_template_item set deleted = true where template_id = $CurrentTemplateId;
update bid_checklist_template set deleted = true where id = $CurrentTemplateId;

update wf_approval_task set deleted = true where instance_id in ($instanceIdSql);
update wf_approval_instance set deleted = true where id in ($instanceIdSql);

update doc_document_version set deleted = true where document_id in ($docIdSql);
update doc_document set deleted = true where id in ($docIdSql);

update sys_notification
set deleted = true
where (biz_type in ('DOCUMENT', 'CHECKLIST_ITEM', 'PROJECT') and biz_id in ($docIdSql, $CurrentProjectId))
   or (biz_type = 'WORKFLOW' and biz_id in ($instanceIdSql));
"@
    $sql | & $PsqlPath -h $DbHost -p $DbPort -U $DbUser -d $DbName -v ON_ERROR_STOP=1 | Out-Host
    if ($LASTEXITCODE -ne 0) {
        throw "e2e cleanup failed"
    }
    Write-Host "[PASS] e2e cleanup soft delete"
}

function Invoke-EndToEndSmoke {
    param(
        [hashtable]$AdminHeaders,
        [hashtable]$OwnerHeaders,
        [hashtable]$MemberHeaders,
        [hashtable]$OutsiderHeaders
    )

    $uploadFolderId = if ([string]::IsNullOrWhiteSpace($EndToEndFolderId)) { $FolderId } else { $EndToEndFolderId }
    if ([string]::IsNullOrWhiteSpace($uploadFolderId)) {
        throw "RunEndToEnd requires -EndToEndFolderId or -FolderId as upload target"
    }

    $suffix = Get-Date -Format "yyyyMMddHHmmss"
    $safePrefix = $TestDataPrefix.Trim()
    $projectName = "$safePrefix-项目-$suffix"
    $templateName = "$safePrefix-模板-$suffix"
    $approvedItemName = "$safePrefix-商务响应-$suffix"
    $rejectedItemName = "$safePrefix-风险说明-$suffix"
    $approvedDocName = "$safePrefix-approved-$suffix.txt"
    $rejectedDocName = "$safePrefix-rejected-$suffix.txt"
    $createdDocumentIds = @()
    $createdApprovalInstanceIds = @()

    $projectResp = Invoke-Json -Method "POST" -Url "$BaseUrl/api/v1/projects" -Headers $AdminHeaders -Body @{
        projectName = $projectName
        tenderUnit = "端到端验收单位"
        ownerDeptId = [long]$OwnerDeptId
        projectType = "E2E"
        bidDeadline = (Get-Date).AddDays(10).ToString("o")
        folderId = [long]$uploadFolderId
        ownerUserIds = @([long]$OwnerUserId)
        materialOwnerUserIds = @()
        memberUserIds = @()
    }
    Assert-Code -Resp $projectResp -Code 0 -Name "e2e create project"
    $currentProjectId = $projectResp.data.id
    Assert-True -Condition ($null -ne $currentProjectId) -Name "e2e project id returned"

    try {
    $ownerGetResp = Invoke-Json -Method "GET" -Url "$BaseUrl/api/v1/projects/$currentProjectId" -Headers $OwnerHeaders
    Assert-Code -Resp $ownerGetResp -Code 0 -Name "permission owner can view own project"

    $ownerManageResp = Invoke-Json -Method "PATCH" -Url "$BaseUrl/api/v1/projects/$currentProjectId/stage" -Headers $OwnerHeaders -Body @{
        projectStage = "REVIEWING"
    }
    Assert-Code -Resp $ownerManageResp -Code 0 -Name "permission owner can manage own project"

    $outsiderGetResp = Invoke-Json -Method "GET" -Url "$BaseUrl/api/v1/projects/$currentProjectId" -Headers $OutsiderHeaders
    Assert-Code -Resp $outsiderGetResp -Code 4035001 -Name "permission outsider cannot view unrelated project"

    $outsiderManageResp = Invoke-Json -Method "PATCH" -Url "$BaseUrl/api/v1/projects/$currentProjectId/status" -Headers $OutsiderHeaders -Body @{
        projectStatus = "PAUSED"
    }
    Assert-Code -Resp $outsiderManageResp -Code 4035001 -Name "permission non-admin cannot manage unrelated project"

    $adminManageResp = Invoke-Json -Method "PATCH" -Url "$BaseUrl/api/v1/projects/$currentProjectId/status" -Headers $AdminHeaders -Body @{
        projectStatus = "NORMAL"
    }
    Assert-Code -Resp $adminManageResp -Code 0 -Name "permission super admin can manage all projects"

    $memberResp = Invoke-Json -Method "POST" -Url "$BaseUrl/api/v1/projects/$currentProjectId/members" -Headers $OwnerHeaders -Body @{
        userIds = @([long]$MemberUserId)
        memberRole = "MEMBER"
    }
    Assert-Code -Resp $memberResp -Code 0 -Name "e2e add project member"

    $memberGetResp = Invoke-Json -Method "GET" -Url "$BaseUrl/api/v1/projects/$currentProjectId" -Headers $MemberHeaders
    Assert-Code -Resp $memberGetResp -Code 0 -Name "permission project member can view participated project"
    Assert-ProjectVisibleInList -Headers $MemberHeaders -CurrentProjectId $currentProjectId -Name "permission project member list"

    $outsiderListResp = Invoke-Json -Method "GET" -Url "$BaseUrl/api/v1/projects?page=1&size=50" -Headers $OutsiderHeaders
    Assert-Code -Resp $outsiderListResp -Code 0 -Name "permission outsider project list"
    $outsiderFound = @($outsiderListResp.data.list | Where-Object { "$($_.id)" -eq "$currentProjectId" })
    Assert-True -Condition ($outsiderFound.Count -eq 0) -Name "permission outsider list excludes unrelated project"

    $templateResp = Invoke-Json -Method "POST" -Url "$BaseUrl/api/v1/checklist-templates" -Headers $AdminHeaders -Body @{
        templateName = $templateName
        projectType = "E2E"
        enabled = $true
    }
    Assert-Code -Resp $templateResp -Code 0 -Name "e2e create checklist template"
    $currentTemplateId = $templateResp.data.id

    foreach ($itemName in @($approvedItemName, $rejectedItemName)) {
        $itemResp = Invoke-Json -Method "POST" -Url "$BaseUrl/api/v1/checklist-templates/$currentTemplateId/items" -Headers $AdminHeaders -Body @{
            itemName = $itemName
            description = "端到端验收清单项"
            required = $true
            businessCategory = "BID"
            tenderStructureCategory = "BUSINESS"
            suggestedSensitiveLevel = "INTERNAL"
            allowedSource = "UPLOAD"
            allowedFileTypes = "txt"
            minCount = 1
            maxCount = 3
            sortOrder = 1
        }
        Assert-Code -Resp $itemResp -Code 0 -Name "e2e add template item $itemName"
    }

    $generateResp = Invoke-Json -Method "POST" -Url "$BaseUrl/api/v1/projects/$currentProjectId/checklist/generate?templateId=$currentTemplateId" -Headers $OwnerHeaders
    Assert-Code -Resp $generateResp -Code 0 -Name "e2e generate project checklist"
    Assert-ChecklistStatus -Headers $OwnerHeaders -CurrentProjectId $currentProjectId -ItemName $approvedItemName -ExpectedStatus "PENDING_COLLECT"
    Assert-ChecklistStatus -Headers $OwnerHeaders -CurrentProjectId $currentProjectId -ItemName $rejectedItemName -ExpectedStatus "PENDING_COLLECT"

    $approvedUpload = Invoke-UploadDocument -UploadFolderId $uploadFolderId -Headers $AdminHeaders -Name "$safePrefix-approved-$suffix" -Content "approved document smoke $suffix"
    Assert-Code -Resp $approvedUpload -Code 0 -Name "e2e upload approved-path document"
    $approvedDocumentId = $approvedUpload.data.documentId
    $createdDocumentIds += "$approvedDocumentId"
    $approvedVersionNo = $approvedUpload.data.versionNo

    $approvedMetadata = Invoke-Json -Method "PUT" -Url "$BaseUrl/api/v1/documents/$approvedDocumentId/metadata" -Headers $AdminHeaders -Body @{
        documentName = $approvedDocName
        businessCategory = "BID"
        tenderStructureCategory = "BUSINESS"
        sensitiveLevel = "INTERNAL"
        ownerDeptId = [long]$OwnerDeptId
        sourceType = "UPLOAD"
        hasExpireDate = $false
        remark = "端到端通过路径资料"
    }
    Assert-Code -Resp $approvedMetadata -Code 0 -Name "e2e update approved-path metadata"
    Assert-DocumentStatusBySearch -Headers $AdminHeaders -Name $approvedDocName -Status "READY_SUBMIT"

    $approvedSubmit = Invoke-Json -Method "POST" -Url "$BaseUrl/api/v1/documents/$approvedDocumentId/approval/submit" -Headers $AdminHeaders -Body @{
        comment = "提交资料审批"
    }
    Assert-Code -Resp $approvedSubmit -Code 0 -Name "e2e submit document approval"
    $createdApprovalInstanceIds += "$($approvedSubmit.data.instanceId)"
    $approvedTaskId = Get-PendingTaskId -Headers $AdminHeaders -InstanceId $approvedSubmit.data.instanceId
    $approveResp = Invoke-Json -Method "POST" -Url "$BaseUrl/api/v1/approvals/$approvedTaskId/approve" -Headers $AdminHeaders -Body @{
        comment = "审批通过"
    }
    Assert-Code -Resp $approveResp -Code 0 -Name "e2e approve document"
    Assert-DocumentStatusBySearch -Headers $AdminHeaders -Name $approvedDocName -Status "APPROVED"

    $approvedItem = Get-ChecklistItem -Headers $OwnerHeaders -CurrentProjectId $currentProjectId -ItemName $approvedItemName
    $bindApproved = Invoke-Json -Method "POST" -Url "$BaseUrl/api/v1/projects/$currentProjectId/checklist/items/$($approvedItem.id)/documents" -Headers $OwnerHeaders -Body @{
        documentId = [long]$approvedDocumentId
        versionNo = [int]$approvedVersionNo
    }
    Assert-Code -Resp $bindApproved -Code 0 -Name "e2e bind approved document"
    Assert-ChecklistStatus -Headers $OwnerHeaders -CurrentProjectId $currentProjectId -ItemName $approvedItemName -ExpectedStatus "COMPLETE"

    $checklistSubmit = Invoke-Json -Method "POST" -Url "$BaseUrl/api/v1/projects/$currentProjectId/checklist/items/$($approvedItem.id)/approval/submit" -Headers $OwnerHeaders -Body @{
        comment = "提交清单项审批"
    }
    Assert-Code -Resp $checklistSubmit -Code 0 -Name "e2e submit checklist approval"
    $createdApprovalInstanceIds += "$($checklistSubmit.data.instanceId)"
    $checklistTaskId = Get-PendingTaskId -Headers $OwnerHeaders -InstanceId $checklistSubmit.data.instanceId
    $checklistApprove = Invoke-Json -Method "POST" -Url "$BaseUrl/api/v1/approvals/$checklistTaskId/approve" -Headers $OwnerHeaders -Body @{
        comment = "清单项审批通过"
    }
    Assert-Code -Resp $checklistApprove -Code 0 -Name "e2e approve checklist item"
    Assert-ChecklistStatus -Headers $OwnerHeaders -CurrentProjectId $currentProjectId -ItemName $approvedItemName -ExpectedStatus "COMPLETE"

    $rejectedUpload = Invoke-UploadDocument -UploadFolderId $uploadFolderId -Headers $AdminHeaders -Name "$safePrefix-rejected-$suffix" -Content "rejected document smoke $suffix"
    Assert-Code -Resp $rejectedUpload -Code 0 -Name "e2e upload rejected-path document"
    $rejectedDocumentId = $rejectedUpload.data.documentId
    $createdDocumentIds += "$rejectedDocumentId"
    $rejectedVersionNo = $rejectedUpload.data.versionNo

    $rejectedMetadata = Invoke-Json -Method "PUT" -Url "$BaseUrl/api/v1/documents/$rejectedDocumentId/metadata" -Headers $AdminHeaders -Body @{
        documentName = $rejectedDocName
        businessCategory = "BID"
        tenderStructureCategory = "BUSINESS"
        sensitiveLevel = "INTERNAL"
        ownerDeptId = [long]$OwnerDeptId
        sourceType = "UPLOAD"
        hasExpireDate = $false
        remark = "端到端驳回路径资料"
    }
    Assert-Code -Resp $rejectedMetadata -Code 0 -Name "e2e update rejected-path metadata"

    $rejectedSubmit = Invoke-Json -Method "POST" -Url "$BaseUrl/api/v1/documents/$rejectedDocumentId/approval/submit" -Headers $AdminHeaders -Body @{
        comment = "提交资料审批"
    }
    Assert-Code -Resp $rejectedSubmit -Code 0 -Name "e2e submit rejected document approval"
    $createdApprovalInstanceIds += "$($rejectedSubmit.data.instanceId)"
    $rejectedTaskId = Get-PendingTaskId -Headers $AdminHeaders -InstanceId $rejectedSubmit.data.instanceId
    $rejectResp = Invoke-Json -Method "POST" -Url "$BaseUrl/api/v1/approvals/$rejectedTaskId/reject" -Headers $AdminHeaders -Body @{
        comment = "审批驳回"
    }
    Assert-Code -Resp $rejectResp -Code 0 -Name "e2e reject document"
    Assert-DocumentStatusBySearch -Headers $AdminHeaders -Name $rejectedDocName -Status "REJECTED"

    $rejectedItem = Get-ChecklistItem -Headers $OwnerHeaders -CurrentProjectId $currentProjectId -ItemName $rejectedItemName
    $bindRejected = Invoke-Json -Method "POST" -Url "$BaseUrl/api/v1/projects/$currentProjectId/checklist/items/$($rejectedItem.id)/documents" -Headers $OwnerHeaders -Body @{
        documentId = [long]$rejectedDocumentId
        versionNo = [int]$rejectedVersionNo
    }
    Assert-Code -Resp $bindRejected -Code 0 -Name "e2e bind rejected document"
    Assert-ChecklistStatus -Headers $OwnerHeaders -CurrentProjectId $currentProjectId -ItemName $rejectedItemName -ExpectedStatus "NEED_SUPPLEMENT"

    $rejectedChecklistSubmit = Invoke-Json -Method "POST" -Url "$BaseUrl/api/v1/projects/$currentProjectId/checklist/items/$($rejectedItem.id)/approval/submit" -Headers $OwnerHeaders -Body @{
        comment = "提交驳回路径清单项审批"
    }
    Assert-Code -Resp $rejectedChecklistSubmit -Code 0 -Name "e2e submit rejected checklist approval"
    $createdApprovalInstanceIds += "$($rejectedChecklistSubmit.data.instanceId)"
    $rejectedChecklistTaskId = Get-PendingTaskId -Headers $OwnerHeaders -InstanceId $rejectedChecklistSubmit.data.instanceId
    $rejectedChecklistReject = Invoke-Json -Method "POST" -Url "$BaseUrl/api/v1/approvals/$rejectedChecklistTaskId/reject" -Headers $OwnerHeaders -Body @{
        comment = "清单项审批驳回"
    }
    Assert-Code -Resp $rejectedChecklistReject -Code 0 -Name "e2e reject checklist item"
    Assert-ChecklistStatus -Headers $OwnerHeaders -CurrentProjectId $currentProjectId -ItemName $rejectedItemName -ExpectedStatus "NEED_SUPPLEMENT"
    } finally {
        if ($currentProjectId) {
            $cleanupTemplateId = if ($currentTemplateId) { "$currentTemplateId" } else { "0" }
            Invoke-EndToEndCleanup -CurrentProjectId "$currentProjectId" -CurrentTemplateId $cleanupTemplateId `
                -DocumentIds $createdDocumentIds -ApprovalInstanceIds $createdApprovalInstanceIds
        }
    }
}

$adminToken = Login -Username $AdminUsername -Password $AdminPassword
$userToken = Login -Username $UserUsername -Password $UserPassword
$ownerToken = if ($RunEndToEnd) { Login -Username $OwnerUsername -Password $OwnerPassword } else { $null }
$outsiderToken = if ($RunEndToEnd) { Login -Username $OutsiderUsername -Password $OutsiderPassword } else { $null }

if ($adminToken) {
    $adminHeaders = @{ Authorization = $adminToken }
    $auditResp = Invoke-Json -Method "GET" -Url "$BaseUrl/api/v1/audit/logs?page=1&size=5" -Headers $adminHeaders
    Assert-Code -Resp $auditResp -Code 0 -Name "admin audit query"

    $tagResp = Invoke-Json -Method "GET" -Url "$BaseUrl/api/v1/tags" -Headers $adminHeaders
    Assert-Code -Resp $tagResp -Code 0 -Name "tag list"

    $notificationResp = Invoke-Json -Method "GET" -Url "$BaseUrl/api/v1/notifications/unread-count" -Headers $adminHeaders
    Assert-Code -Resp $notificationResp -Code 0 -Name "notification unread count"

    $projectListResp = Invoke-Json -Method "GET" -Url "$BaseUrl/api/v1/projects?page=1&size=5" -Headers $adminHeaders
    Assert-Code -Resp $projectListResp -Code 0 -Name "project list"

    if (-not [string]::IsNullOrWhiteSpace($ProjectId)) {
        $projectResp = Invoke-Json -Method "GET" -Url "$BaseUrl/api/v1/projects/$ProjectId" -Headers $adminHeaders
        Assert-Code -Resp $projectResp -Code 0 -Name "project detail"

        $checklistResp = Invoke-Json -Method "GET" -Url "$BaseUrl/api/v1/projects/$ProjectId/checklist" -Headers $adminHeaders
        Assert-Code -Resp $checklistResp -Code 0 -Name "project checklist"
    }

    if (-not [string]::IsNullOrWhiteSpace($ProjectId) -and -not [string]::IsNullOrWhiteSpace($TemplateId)) {
        $generateResp = Invoke-Json -Method "POST" -Url "$BaseUrl/api/v1/projects/$ProjectId/checklist/generate?templateId=$TemplateId" -Headers $adminHeaders
        Assert-Code -Resp $generateResp -Code 0 -Name "generate project checklist"
    }

    if ($RunEndToEnd) {
        Assert-True -Condition ($null -ne $ownerToken -and $null -ne $userToken -and $null -ne $outsiderToken) `
            -Name "e2e required tokens ready"
        $ownerHeaders = @{ Authorization = $ownerToken }
        $memberHeaders = @{ Authorization = $userToken }
        $outsiderHeaders = @{ Authorization = $outsiderToken }
        Invoke-EndToEndSmoke -AdminHeaders $adminHeaders -OwnerHeaders $ownerHeaders `
            -MemberHeaders $memberHeaders -OutsiderHeaders $outsiderHeaders
    }
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

        $metadataResp = Invoke-Json -Method "GET" -Url "$BaseUrl/api/v1/documents/$DocumentId" -Headers $userHeaders
        Assert-Code -Resp $metadataResp -Code 0 -Name "document detail with lifecycle metadata"

        $previewResp = Invoke-WebRequest -Method "GET" -Uri "$BaseUrl/api/v1/documents/$DocumentId/preview" -Headers $userHeaders -SkipHttpErrorCheck
        if ($previewResp.StatusCode -ge 200 -and $previewResp.StatusCode -lt 500) {
            Write-Host "[PASS] document preview status $($previewResp.StatusCode)"
        } else {
            throw "document preview unexpected status $($previewResp.StatusCode)"
        }
    }
}

Write-Host "next phase api smoke finished"
