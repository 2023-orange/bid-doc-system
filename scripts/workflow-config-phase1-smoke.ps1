param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$AdminUsername = $env:SMOKE_ADMIN_USERNAME,
    [string]$AdminPassword = $env:SMOKE_ADMIN_PASSWORD,
    [string]$SubmitterUsername = $env:SMOKE_SUBMITTER_USERNAME,
    [string]$SubmitterPassword = $env:SMOKE_SUBMITTER_PASSWORD,
    [string]$ApproverUsername = $env:SMOKE_APPROVER_USERNAME,
    [string]$ApproverPassword = $env:SMOKE_APPROVER_PASSWORD,
    [string]$TransferUsername = $env:SMOKE_TRANSFER_USERNAME,
    [string]$TransferPassword = $env:SMOKE_TRANSFER_PASSWORD,
    [string]$AddSignUsername = $env:SMOKE_ADDSIGN_USERNAME,
    [string]$AddSignPassword = $env:SMOKE_ADDSIGN_PASSWORD,
    [Parameter(Mandatory = $true)][long]$ApproverUserId,
    [long]$TransferUserId,
    [long]$AddSignUserId,
    [Parameter(Mandatory = $true)][long]$DocumentId,
    [long]$RejectDocumentId,
    [long]$WithdrawDocumentId,
    [long]$TransferDocumentId,
    [long]$AddSignDocumentId,
    [long]$TerminateDocumentId,
    [string]$Prefix = "codex-smoke-$(Get-Date -Format 'yyyyMMddHHmmss')",
    [switch]$RunFullPhase1
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Name, [string]$Status = "RUN", [string]$BizId = "")
    $suffix = if ([string]::IsNullOrWhiteSpace($BizId)) { "" } else { " id=$BizId" }
    Write-Host "[$Status] $Name$suffix"
}

function Assert-Env {
    param([string]$Name)
    if ([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($Name))) {
        throw "Missing required environment variable: $Name"
    }
}

function Require-Text {
    param([string]$Value, [string]$Name)
    if ([string]::IsNullOrWhiteSpace($Value)) {
        throw "Missing required value: $Name"
    }
    return $Value
}

function Read-SecretText {
    param([string]$Value, [string]$Prompt)
    if (-not [string]::IsNullOrWhiteSpace($Value)) {
        return $Value
    }
    $secure = Read-Host -Prompt $Prompt -AsSecureString
    $credential = [System.Net.NetworkCredential]::new("", $secure)
    return $credential.Password
}

function Invoke-JsonApi {
    param(
        [string]$Method,
        [string]$Path,
        [hashtable]$Headers = @{},
        $Body = $null
    )
    $params = @{
        Method = $Method
        Uri = "$BaseUrl$Path"
        Headers = $Headers
    }
    if ($null -ne $Body) {
        $params.ContentType = "application/json; charset=utf-8"
        $params.Body = ($Body | ConvertTo-Json -Depth 20)
    }
    return Invoke-RestMethod @params
}

function Assert-Code {
    param($Resp, [string]$Name)
    if ($Resp.code -ne 0) {
        throw "$Name failed: code=$($Resp.code), message=$($Resp.message)"
    }
    Write-Step -Name $Name -Status "PASS"
}

function Assert-True {
    param([bool]$Condition, [string]$Name, [string]$Message = "")
    if (-not $Condition) {
        if ([string]::IsNullOrWhiteSpace($Message)) {
            throw "$Name failed"
        }
        throw "$Name failed: $Message"
    }
    Write-Step -Name $Name -Status "PASS"
}

function Login {
    param([string]$Username, [string]$Password, [string]$Name)
    Write-Step -Name "login $Name"
    $resp = Invoke-JsonApi -Method "POST" -Path "/api/v1/auth/login" -Body @{
        username = $Username
        password = $Password
        rememberMe = $false
    }
    Assert-Code -Resp $resp -Name "login $Name"
    if ([string]::IsNullOrWhiteSpace($resp.data.token)) {
        throw "login $Name did not return token"
    }
    return @{ Authorization = $resp.data.token }
}

function Find-PendingTask {
    param([hashtable]$Headers, [long]$InstanceId, [string]$Name = "approval")
    $resp = Invoke-JsonApi -Method "GET" -Path "/api/v1/approvals/tasks?status=PENDING" -Headers $Headers
    Assert-Code -Resp $resp -Name "query pending tasks $Name"
    $task = @($resp.data | Where-Object { [long]$_.instanceId -eq $InstanceId } | Select-Object -First 1)
    if ($task.Count -eq 0) {
        throw "pending task not found for $Name instanceId=$InstanceId"
    }
    $taskId = [long]$task[0].taskId
    Write-Step -Name "pending task found $Name" -Status "INFO" -BizId $taskId
    return $taskId
}

function Submit-DocumentApproval {
    param([hashtable]$Headers, [long]$CurrentDocumentId, [string]$Name)
    $submit = Invoke-JsonApi -Method "POST" -Path "/api/v1/documents/$CurrentDocumentId/approval/submit" -Headers $Headers -Body @{
        comment = "$Prefix-$Name-submit"
    }
    Assert-Code -Resp $submit -Name "submit $Name approval"
    $instanceId = [long]$submit.data.instanceId
    Write-Step -Name "$Name approval instance created" -Status "INFO" -BizId $instanceId
    return $instanceId
}

function Require-FullPhase1Value {
    param([long]$Value, [string]$Name)
    if ($Value -le 0) {
        throw "Missing required value for -RunFullPhase1: $Name"
    }
    return $Value
}

Assert-Env -Name "DB_USERNAME"
Assert-Env -Name "DB_PASSWORD"
if ($null -eq [Environment]::GetEnvironmentVariable("REDIS_PASSWORD")) {
    $env:REDIS_PASSWORD = ""
    Write-Step -Name "REDIS_PASSWORD not set; Redis will be used without password" -Status "INFO"
}

$AdminUsername = Require-Text -Value $AdminUsername -Name "AdminUsername or SMOKE_ADMIN_USERNAME"
$SubmitterUsername = Require-Text -Value $SubmitterUsername -Name "SubmitterUsername or SMOKE_SUBMITTER_USERNAME"
$ApproverUsername = Require-Text -Value $ApproverUsername -Name "ApproverUsername or SMOKE_APPROVER_USERNAME"
$AdminPassword = Read-SecretText -Value $AdminPassword -Prompt "Admin password"
$SubmitterPassword = Read-SecretText -Value $SubmitterPassword -Prompt "Submitter password"
$ApproverPassword = Read-SecretText -Value $ApproverPassword -Prompt "Approver password"

if ($RunFullPhase1) {
    $TransferUsername = Require-Text -Value $TransferUsername -Name "TransferUsername or SMOKE_TRANSFER_USERNAME"
    $AddSignUsername = Require-Text -Value $AddSignUsername -Name "AddSignUsername or SMOKE_ADDSIGN_USERNAME"
    $TransferPassword = Read-SecretText -Value $TransferPassword -Prompt "Transfer password"
    $AddSignPassword = Read-SecretText -Value $AddSignPassword -Prompt "AddSign password"
    $TransferUserId = Require-FullPhase1Value -Value $TransferUserId -Name "TransferUserId"
    $AddSignUserId = Require-FullPhase1Value -Value $AddSignUserId -Name "AddSignUserId"
    $RejectDocumentId = Require-FullPhase1Value -Value $RejectDocumentId -Name "RejectDocumentId"
    $WithdrawDocumentId = Require-FullPhase1Value -Value $WithdrawDocumentId -Name "WithdrawDocumentId"
    $TransferDocumentId = Require-FullPhase1Value -Value $TransferDocumentId -Name "TransferDocumentId"
    $AddSignDocumentId = Require-FullPhase1Value -Value $AddSignDocumentId -Name "AddSignDocumentId"
    $TerminateDocumentId = Require-FullPhase1Value -Value $TerminateDocumentId -Name "TerminateDocumentId"
}

$definitionId = $null

try {
    $adminHeaders = Login -Username $AdminUsername -Password $AdminPassword -Name "admin"
    $submitterHeaders = Login -Username $SubmitterUsername -Password $SubmitterPassword -Name "submitter"
    $approverHeaders = Login -Username $ApproverUsername -Password $ApproverPassword -Name "approver"
    $transferHeaders = $null
    $addSignHeaders = $null
    if ($RunFullPhase1) {
        $transferHeaders = Login -Username $TransferUsername -Password $TransferPassword -Name "transfer"
        $addSignHeaders = Login -Username $AddSignUsername -Password $AddSignPassword -Name "add-sign"
    }

    $definitionName = "$Prefix-document-approval"
    $createDefinition = Invoke-JsonApi -Method "POST" -Path "/api/v1/workflow/definitions" -Headers $adminHeaders -Body @{
        name = $definitionName
        scenario = "DOCUMENT_APPROVAL"
        bizModule = "DOCUMENT"
        bizType = "DOCUMENT"
        deptId = $null
        businessCategory = $Prefix
    }
    Assert-Code -Resp $createDefinition -Name "create workflow definition"
    $definitionId = [long]$createDefinition.data.id
    Write-Step -Name "definition created" -Status "INFO" -BizId $definitionId

    $addNode = Invoke-JsonApi -Method "POST" -Path "/api/v1/workflow/definitions/$definitionId/nodes" -Headers $adminHeaders -Body @{
        nodeCode = "$Prefix-approve-1"
        nodeName = "$Prefix 一级审批"
        nodeType = "APPROVAL"
        approveMode = "ANY"
        assigneeType = "USER"
        assigneeValue = "$ApproverUserId"
        sortOrder = 1
        nextNodeCode = $null
        rejectToNodeCode = $null
        conditions = @()
    }
    Assert-Code -Resp $addNode -Name "add approval node"
    Write-Step -Name "node created" -Status "INFO" -BizId $addNode.data.id

    $enable = Invoke-JsonApi -Method "POST" -Path "/api/v1/workflow/definitions/$definitionId/enable" -Headers $adminHeaders
    Assert-Code -Resp $enable -Name "enable workflow definition"

    $instanceId = Submit-DocumentApproval -Headers $submitterHeaders -CurrentDocumentId $DocumentId -Name "approve"
    $taskId = Find-PendingTask -Headers $approverHeaders -InstanceId $instanceId -Name "approve"

    $approve = Invoke-JsonApi -Method "POST" -Path "/api/v1/approvals/$taskId/approve" -Headers $approverHeaders -Body @{
        comment = "$Prefix-approve"
    }
    Assert-Code -Resp $approve -Name "approve document"

    $history = Invoke-JsonApi -Method "GET" -Path "/api/v1/documents/$DocumentId/approval/history" -Headers $submitterHeaders
    Assert-Code -Resp $history -Name "query approval history"
    if (@($history.data).Count -lt 2) {
        throw "approval history should contain submit and approve records"
    }

    if ($RunFullPhase1) {
        $rejectInstanceId = Submit-DocumentApproval -Headers $submitterHeaders -CurrentDocumentId $RejectDocumentId -Name "reject"
        $rejectTaskId = Find-PendingTask -Headers $approverHeaders -InstanceId $rejectInstanceId -Name "reject"
        $reject = Invoke-JsonApi -Method "POST" -Path "/api/v1/approvals/$rejectTaskId/reject" -Headers $approverHeaders -Body @{
            comment = "$Prefix-reject"
        }
        Assert-Code -Resp $reject -Name "reject document"

        $withdrawInstanceId = Submit-DocumentApproval -Headers $submitterHeaders -CurrentDocumentId $WithdrawDocumentId -Name "withdraw"
        $withdrawTaskId = Find-PendingTask -Headers $approverHeaders -InstanceId $withdrawInstanceId -Name "withdraw"
        Assert-True -Condition ($withdrawTaskId -gt 0) -Name "withdraw pending task exists"
        $withdraw = Invoke-JsonApi -Method "POST" -Path "/api/v1/approvals/$withdrawInstanceId/withdraw" -Headers $submitterHeaders -Body @{
            comment = "$Prefix-withdraw"
        }
        Assert-Code -Resp $withdraw -Name "withdraw approval"

        $transferInstanceId = Submit-DocumentApproval -Headers $submitterHeaders -CurrentDocumentId $TransferDocumentId -Name "transfer"
        $transferTaskId = Find-PendingTask -Headers $approverHeaders -InstanceId $transferInstanceId -Name "transfer-source"
        $transfer = Invoke-JsonApi -Method "POST" -Path "/api/v1/approvals/tasks/$transferTaskId/transfer" -Headers $approverHeaders -Body @{
            targetUserId = $TransferUserId
            comment = "$Prefix-transfer"
        }
        Assert-Code -Resp $transfer -Name "transfer approval task"
        $transferredTaskId = Find-PendingTask -Headers $transferHeaders -InstanceId $transferInstanceId -Name "transfer-target"
        $transferApprove = Invoke-JsonApi -Method "POST" -Path "/api/v1/approvals/$transferredTaskId/approve" -Headers $transferHeaders -Body @{
            comment = "$Prefix-transfer-approve"
        }
        Assert-Code -Resp $transferApprove -Name "approve transferred task"

        $addSignInstanceId = Submit-DocumentApproval -Headers $submitterHeaders -CurrentDocumentId $AddSignDocumentId -Name "add-sign"
        $addSignSourceTaskId = Find-PendingTask -Headers $approverHeaders -InstanceId $addSignInstanceId -Name "add-sign-source"
        $addSign = Invoke-JsonApi -Method "POST" -Path "/api/v1/approvals/tasks/$addSignSourceTaskId/add-sign" -Headers $approverHeaders -Body @{
            assigneeUserId = $AddSignUserId
            comment = "$Prefix-add-sign"
        }
        Assert-Code -Resp $addSign -Name "add-sign approval task"
        $addSignTaskId = Find-PendingTask -Headers $addSignHeaders -InstanceId $addSignInstanceId -Name "add-sign-assignee"
        $addSignApprove = Invoke-JsonApi -Method "POST" -Path "/api/v1/approvals/$addSignTaskId/approve" -Headers $addSignHeaders -Body @{
            comment = "$Prefix-add-sign-approve"
        }
        Assert-Code -Resp $addSignApprove -Name "approve add-sign task"
        $addSignSourceApprove = Invoke-JsonApi -Method "POST" -Path "/api/v1/approvals/$addSignSourceTaskId/approve" -Headers $approverHeaders -Body @{
            comment = "$Prefix-add-sign-source-approve"
        }
        Assert-Code -Resp $addSignSourceApprove -Name "approve add-sign source task"

        $terminateInstanceId = Submit-DocumentApproval -Headers $submitterHeaders -CurrentDocumentId $TerminateDocumentId -Name "terminate"
        $terminateTaskId = Find-PendingTask -Headers $approverHeaders -InstanceId $terminateInstanceId -Name "terminate"
        Assert-True -Condition ($terminateTaskId -gt 0) -Name "terminate pending task exists"
        $terminate = Invoke-JsonApi -Method "POST" -Path "/api/v1/approvals/$terminateInstanceId/terminate" -Headers $adminHeaders -Body @{
            reason = "$Prefix-terminate"
        }
        Assert-Code -Resp $terminate -Name "terminate approval"
    }

    Write-Step -Name "workflow config phase1 smoke finished" -Status "PASS"
} finally {
    if ($null -ne $definitionId) {
        try {
            $disable = Invoke-JsonApi -Method "POST" -Path "/api/v1/workflow/definitions/$definitionId/disable" -Headers $adminHeaders
            if ($disable.code -eq 0) {
                Write-Step -Name "disable smoke workflow definition" -Status "PASS" -BizId $definitionId
            } else {
                Write-Step -Name "disable smoke workflow definition failed code=$($disable.code)" -Status "WARN" -BizId $definitionId
            }
        } catch {
            Write-Step -Name "disable smoke workflow definition failed: $($_.Exception.Message)" -Status "WARN" -BizId $definitionId
        }
    }
}
