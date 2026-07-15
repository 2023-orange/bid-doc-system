param(
    [string]$PsqlPath = "D:\PostgreSQL\13\bin\psql.exe",
    [string]$DbHost = "localhost",
    [int]$DbPort = 5432,
    [string]$DbName = "bid_doc_system",
    [string]$DbUser = $env:DB_USERNAME,
    [switch]$IncludeSmokeData
)

$ErrorActionPreference = "Stop"

function Assert-Value {
    param([string]$Name, [string]$Actual, [string]$Expected)
    if ($Actual.Trim() -ne $Expected) {
        throw "$Name expected $Expected but got $Actual"
    }
    Write-Host "[PASS] $Name => $Actual"
}

function Invoke-ScalarSql {
    param([string]$Sql)
    $result = & $PsqlPath `
        -h $DbHost `
        -p $DbPort `
        -U $DbUser `
        -d $DbName `
        -t `
        -A `
        -v ON_ERROR_STOP=1 `
        -c $Sql
    if ($LASTEXITCODE -ne 0) {
        throw "psql failed"
    }
    return ($result | Select-Object -Last 1).Trim()
}

if (-not (Test-Path -LiteralPath $PsqlPath)) {
    throw "psql not found: $PsqlPath"
}
if ([string]::IsNullOrWhiteSpace($DbUser)) {
    throw "Missing DB user. Set DB_USERNAME or pass -DbUser."
}
if ([string]::IsNullOrWhiteSpace($env:PGPASSWORD) -and [string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
    throw "Missing database password. Set PGPASSWORD or DB_PASSWORD in the current shell."
}
if ([string]::IsNullOrWhiteSpace($env:PGPASSWORD)) {
    $env:PGPASSWORD = $env:DB_PASSWORD
}

$requiredTables = @(
    "wf_approval_definition",
    "wf_approval_node",
    "wf_approval_condition",
    "wf_approval_action_log",
    "wf_approval_task_candidate",
    "doc_document_use_grant",
    "bid_project_archive_record",
    "bid_project_archive_checklist_snapshot",
    "bid_project_archive_document_snapshot"
)

$tableList = "'" + ($requiredTables -join "','") + "'"
$tableCount = Invoke-ScalarSql -Sql @"
select count(*)
from unnest(array[$tableList]) as t(name)
where to_regclass('public.' || t.name) is not null;
"@
Assert-Value -Name "V109/V110 table count" -Actual $tableCount -Expected "$($requiredTables.Count)"

$instanceColumnCount = Invoke-ScalarSql -Sql @"
select count(*)
from information_schema.columns
where table_schema = 'public'
  and table_name = 'wf_approval_instance'
  and column_name in ('definition_id', 'definition_version', 'current_node_id', 'current_node_code');
"@
Assert-Value -Name "wf_approval_instance extended columns" -Actual $instanceColumnCount -Expected "4"

$taskColumnCount = Invoke-ScalarSql -Sql @"
select count(*)
from information_schema.columns
where table_schema = 'public'
  and table_name = 'wf_approval_task'
  and column_name in ('definition_id', 'node_id', 'node_code', 'transferred_from_task_id', 'add_sign');
"@
Assert-Value -Name "wf_approval_task extended columns" -Actual $taskColumnCount -Expected "5"

$notNullAddSign = Invoke-ScalarSql -Sql @"
select case when is_nullable = 'NO' then 1 else 0 end
from information_schema.columns
where table_schema = 'public'
  and table_name = 'wf_approval_task'
  and column_name = 'add_sign';
"@
Assert-Value -Name "wf_approval_task.add_sign not null" -Actual $notNullAddSign -Expected "1"

$commentCount = Invoke-ScalarSql -Sql @"
select count(*)
from pg_description d
join pg_class c on c.oid = d.objoid
join pg_namespace n on n.oid = c.relnamespace
where n.nspname = 'public'
  and c.relname in (
    'wf_approval_definition',
    'wf_approval_node',
    'wf_approval_condition',
    'wf_approval_action_log',
    'wf_approval_task_candidate',
    'doc_document_use_grant',
    'bid_project_archive_record',
    'bid_project_archive_checklist_snapshot',
    'bid_project_archive_document_snapshot'
  )
  and d.description is not null
  and length(trim(d.description)) > 0;
"@
Write-Host "[INFO] V109/V110 comment count => $commentCount"

if ($IncludeSmokeData) {
    $definitionCount = Invoke-ScalarSql -Sql "select count(*) from wf_approval_definition where name like 'codex-smoke-%';"
    Write-Host "[INFO] codex-smoke workflow definitions => $definitionCount"

    $instanceCount = Invoke-ScalarSql -Sql "select count(*) from wf_approval_instance where submit_comment like 'codex-smoke-%';"
    Write-Host "[INFO] codex-smoke approval instances => $instanceCount"

    $candidateCount = Invoke-ScalarSql -Sql @"
select count(*)
from wf_approval_task_candidate c
where exists (
    select 1 from wf_approval_instance i
    where i.id = c.instance_id
      and i.submit_comment like 'codex-smoke-%'
);
"@
    Write-Host "[INFO] codex-smoke task candidates => $candidateCount"
}

Write-Host "[PASS] workflow config database verification finished"
