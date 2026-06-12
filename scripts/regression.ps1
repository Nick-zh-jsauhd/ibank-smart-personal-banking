param(
    [string]$TomcatHome = "D:\tomcat9",
    [string]$BaseUrl = "http://localhost:8080/bank",
    [switch]$SkipBuild,
    [switch]$SkipDeploy,
    [switch]$SkipHttp,
    [switch]$SkipDb,
    [switch]$RunSchemaMigration
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$ScriptStart = Get-Date
$RepoRoot = Split-Path -Parent $PSScriptRoot
$DbPropertiesPath = Join-Path $RepoRoot "src\main\resources\db.properties"
$SchemaPath = Join-Path $RepoRoot "src\main\resources\schema.sql"
$WarPath = Join-Path $RepoRoot "target\bank.war"
$Failures = New-Object System.Collections.Generic.List[string]
$Checks = New-Object System.Collections.Generic.List[object]

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "== $Message ==" -ForegroundColor Cyan
}

function Add-Check {
    param(
        [string]$Name,
        [bool]$Passed,
        [string]$Detail = ""
    )
    $script:Checks.Add([PSCustomObject]@{
        Check = $Name
        Result = $(if ($Passed) { "PASS" } else { "FAIL" })
        Detail = $Detail
    })
    if (-not $Passed) {
        $script:Failures.Add("$Name $Detail")
        Write-Host "[FAIL] $Name $Detail" -ForegroundColor Red
    } else {
        Write-Host "[PASS] $Name $Detail" -ForegroundColor Green
    }
}

function Read-DbProperties {
    if (-not (Test-Path -LiteralPath $DbPropertiesPath)) {
        throw "db.properties not found: $DbPropertiesPath"
    }
    $props = @{}
    Get-Content -Encoding UTF8 -LiteralPath $DbPropertiesPath | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]+?)\s*=\s*(.*)\s*$') {
            $props[$matches[1].Trim()] = $matches[2].Trim()
        }
    }
    if (-not $props.ContainsKey("db.username")) {
        throw "db.username is missing in db.properties"
    }
    if (-not $props.ContainsKey("db.password")) {
        throw "db.password is missing in db.properties"
    }
    if (-not $props.ContainsKey("db.url")) {
        throw "db.url is missing in db.properties"
    }
    return $props
}

function Get-DatabaseName {
    param([string]$JdbcUrl)
    if ($JdbcUrl -match 'jdbc:mysql://[^/]+/([^?;]+)') {
        return $matches[1]
    }
    return "ibank"
}

function Invoke-MySqlScalar {
    param(
        [hashtable]$DbProps,
        [string]$DatabaseName,
        [string]$Query
    )
    $env:MYSQL_PWD = $DbProps["db.password"]
    try {
        $value = & mysql --default-character-set=utf8mb4 --batch --skip-column-names --protocol=TCP --host=localhost --port=3306 --user="$($DbProps['db.username'])" $DatabaseName --execute="$Query"
        if ($LASTEXITCODE -ne 0) {
            throw "mysql exited with code $LASTEXITCODE"
        }
        return (($value | Select-Object -First 1) -as [string]).Trim()
    } finally {
        Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
    }
}

function Invoke-MySqlFile {
    param(
        [hashtable]$DbProps,
        [string]$FilePath
    )
    $env:MYSQL_PWD = $DbProps["db.password"]
    try {
        $cmdLine = 'mysql --default-character-set=utf8mb4 --protocol=TCP --host=localhost --port=3306 --user="' + $DbProps["db.username"] + '" < "' + $FilePath + '"'
        & cmd /c $cmdLine
        if ($LASTEXITCODE -ne 0) {
            throw "mysql schema migration failed with code $LASTEXITCODE"
        }
    } finally {
        Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
    }
}

function Stop-Tomcat {
    param([string]$TomcatPath)
    $tomcatResolved = (Resolve-Path -LiteralPath $TomcatPath).Path
    $javaProcesses = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" -ErrorAction SilentlyContinue |
        Where-Object { $_.CommandLine -and $_.CommandLine.IndexOf($tomcatResolved, [System.StringComparison]::OrdinalIgnoreCase) -ge 0 }
    foreach ($process in $javaProcesses) {
        Stop-Process -Id $process.ProcessId -Force -ErrorAction SilentlyContinue
    }
    Start-Sleep -Seconds 3
}

function Deploy-War {
    param([string]$TomcatPath)
    if (-not (Test-Path -LiteralPath $WarPath)) {
        throw "WAR not found. Run build first: $WarPath"
    }
    $webapps = Join-Path $TomcatPath "webapps"
    $startup = Join-Path $TomcatPath "bin\startup.bat"
    $targetWar = Join-Path $webapps "bank.war"
    $explodedApp = Join-Path $webapps "bank"

    if (-not (Test-Path -LiteralPath $startup)) {
        throw "Tomcat startup.bat not found: $startup"
    }

    Stop-Tomcat -TomcatPath $TomcatPath

    $webappsResolved = (Resolve-Path -LiteralPath $webapps).Path
    if (Test-Path -LiteralPath $explodedApp) {
        $explodedResolved = (Resolve-Path -LiteralPath $explodedApp).Path
        if (-not $explodedResolved.StartsWith($webappsResolved, [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing to remove unexpected path: $explodedResolved"
        }
        Remove-Item -LiteralPath $explodedResolved -Recurse -Force
    }
    if (Test-Path -LiteralPath $targetWar) {
        $targetWarResolved = (Resolve-Path -LiteralPath $targetWar).Path
        if (-not $targetWarResolved.StartsWith($webappsResolved, [System.StringComparison]::OrdinalIgnoreCase)) {
            throw "Refusing to remove unexpected path: $targetWarResolved"
        }
        Remove-Item -LiteralPath $targetWarResolved -Force
    }
    Copy-Item -LiteralPath $WarPath -Destination $targetWar
    Start-Process -FilePath $startup -WorkingDirectory (Join-Path $TomcatPath "bin") -WindowStyle Hidden
}

function Wait-AppReady {
    param(
        [string]$Url,
        [int]$TimeoutSeconds = 45
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $lastError = ""
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri "$Url/admin/login" -MaximumRedirection 5 -TimeoutSec 5
            if ($response.StatusCode -eq 200) {
                return $true
            }
        } catch {
            $lastError = $_.Exception.Message
        }
        Start-Sleep -Seconds 2
    }
    throw "Application did not become ready at $Url. Last error: $lastError"
}

function Invoke-Page {
    param(
        [Microsoft.PowerShell.Commands.WebRequestSession]$Session,
        [string]$Method,
        [string]$Uri,
        [string]$Body = ""
    )
    try {
        if ($Body.Length -gt 0) {
            $response = Invoke-WebRequest -UseBasicParsing -WebSession $Session -Method $Method -Uri $Uri `
                -Body $Body -ContentType "application/x-www-form-urlencoded" -MaximumRedirection 5 -TimeoutSec 15
        } else {
            $response = Invoke-WebRequest -UseBasicParsing -WebSession $Session -Method $Method -Uri $Uri `
                -MaximumRedirection 5 -TimeoutSec 15
        }
        $title = ""
        if ($response.Content -match '<title>(.*?)</title>') {
            $title = $matches[1]
        }
        return [PSCustomObject]@{
            Status = [int]$response.StatusCode
            Title = $title
            Content = $response.Content
        }
    } catch {
        $resp = $_.Exception.Response
        if ($null -ne $resp) {
            return [PSCustomObject]@{
                Status = [int]$resp.StatusCode
                Title = ""
                Content = ""
            }
        }
        throw
    }
}

function Invoke-Build {
    Write-Step "Build"
    Push-Location $RepoRoot
    try {
        & mvn clean package
        if ($LASTEXITCODE -ne 0) {
            throw "mvn clean package failed with code $LASTEXITCODE"
        }
        Add-Check "Maven package" $true "target\bank.war generated"
    } finally {
        Pop-Location
    }
}

function Invoke-HttpRegression {
    Write-Step "HTTP regression"
    $base = $BaseUrl.TrimEnd("/")
    $adminSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $reviewerSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession

    $loginPage = Invoke-Page -Session $adminSession -Method "Get" -Uri "$base/admin/login"
    Add-Check "Admin login page" ($loginPage.Status -eq 200 -and $loginPage.Content.Contains("iBank Admin")) "status=$($loginPage.Status)"

    $adminLogin = Invoke-Page -Session $adminSession -Method "Post" -Uri "$base/admin/login" -Body "identity=admin&password=admin123"
    Add-Check "Admin login" ($adminLogin.Status -eq 200 -and $adminLogin.Content.Contains("/admin/logout")) "status=$($adminLogin.Status)"

    $audit = Invoke-Page -Session $adminSession -Method "Get" -Uri "$base/admin/audit"
    Add-Check "Super admin can view audit" ($audit.Status -eq 200 -and $audit.Content.Contains("Audit Center")) "status=$($audit.Status)"

    $adminLogs = Invoke-Page -Session $adminSession -Method "Get" -Uri "$base/admin/audit/admin-logs?highRiskOnly=1"
    Add-Check "Super admin can view high-risk logs" ($adminLogs.Status -eq 200 -and $adminLogs.Content.Contains("Admin Audit Logs")) "status=$($adminLogs.Status)"

    $loginLogs = Invoke-Page -Session $adminSession -Method "Get" -Uri "$base/admin/audit/login-logs"
    Add-Check "Super admin can view login logs" ($loginLogs.Status -eq 200 -and $loginLogs.Content.Contains("Login Logs")) "status=$($loginLogs.Status)"

    $adminMessages = Invoke-Page -Session $adminSession -Method "Get" -Uri "$base/admin/messages"
    Add-Check "Super admin can view message center" ($adminMessages.Status -eq 200 -and $adminMessages.Content.Contains("Message Center")) "status=$($adminMessages.Status)"

    $adminTickets = Invoke-Page -Session $adminSession -Method "Get" -Uri "$base/admin/tickets"
    Add-Check "Super admin can view ticket center" ($adminTickets.Status -eq 200 -and $adminTickets.Content.Contains("Ticket Center")) "status=$($adminTickets.Status)"

    $directJsp = Invoke-Page -Session $adminSession -Method "Get" -Uri "$base/admin/audit.jsp"
    Add-Check "Direct admin JSP blocked" ($directJsp.Status -eq 404) "status=$($directJsp.Status)"

    $reviewerLogin = Invoke-Page -Session $reviewerSession -Method "Post" -Uri "$base/admin/login" -Body "identity=admin_reviewer&password=admin123"
    Add-Check "Reviewer login" ($reviewerLogin.Status -eq 200 -and $reviewerLogin.Content.Contains("/admin/logout")) "status=$($reviewerLogin.Status)"

    $reviewerAudit = Invoke-Page -Session $reviewerSession -Method "Get" -Uri "$base/admin/audit"
    Add-Check "Reviewer audit forbidden" ($reviewerAudit.Status -eq 403) "status=$($reviewerAudit.Status)"

    $reviewerMessages = Invoke-Page -Session $reviewerSession -Method "Get" -Uri "$base/admin/messages"
    Add-Check "Reviewer can view assigned message center" ($reviewerMessages.Status -eq 200 -and $reviewerMessages.Content.Contains("Message Center")) "status=$($reviewerMessages.Status)"

    $reviewerTickets = Invoke-Page -Session $reviewerSession -Method "Get" -Uri "$base/admin/tickets"
    Add-Check "Reviewer can view assigned ticket center" ($reviewerTickets.Status -eq 200 -and $reviewerTickets.Content.Contains("Ticket Center")) "status=$($reviewerTickets.Status)"

    $reviewerAdmins = Invoke-Page -Session $reviewerSession -Method "Get" -Uri "$base/admin/security/admins"
    Add-Check "Reviewer admin-user forbidden" ($reviewerAdmins.Status -eq 403) "status=$($reviewerAdmins.Status)"
}

function Invoke-DatabaseRegression {
    param(
        [hashtable]$DbProps,
        [string]$DatabaseName
    )
    Write-Step "Database regression"
    $startText = $ScriptStart.ToString("yyyy-MM-dd HH:mm:ss")

    $superAdmins = [int](Invoke-MySqlScalar -DbProps $DbProps -DatabaseName $DatabaseName -Query @"
SELECT COUNT(DISTINCT u.user_id)
FROM t_user u
JOIN t_admin_user_role ur ON ur.user_id = u.user_id
JOIN t_admin_role r ON r.role_id = ur.role_id
WHERE u.role = 'ADMIN'
  AND u.status = 'NORMAL'
  AND r.role_code = 'SUPER_ADMIN'
  AND r.status = 'ACTIVE';
"@)
    Add-Check "At least one active SUPER_ADMIN" ($superAdmins -ge 1) "count=$superAdmins"

    $securityPermissions = [int](Invoke-MySqlScalar -DbProps $DbProps -DatabaseName $DatabaseName -Query @"
SELECT COUNT(*)
FROM t_admin_permission
WHERE permission_code IN ('ADMIN_AUDIT_VIEW', 'ADMIN_USER_VIEW', 'ADMIN_USER_MANAGE',
  'ROLE_PERMISSION_VIEW', 'ADMIN_ALERT_VIEW', 'ADMIN_ALERT_HANDLE',
  'TICKET_VIEW', 'TICKET_HANDLE', 'TICKET_ASSIGN', 'TICKET_ALL_VIEW');
"@)
    Add-Check "Security, message and ticket permissions are seeded" ($securityPermissions -eq 10) "count=$securityPermissions"

    $superAdminPermissionCount = [int](Invoke-MySqlScalar -DbProps $DbProps -DatabaseName $DatabaseName -Query @"
SELECT COUNT(*)
FROM t_admin_role r
JOIN t_admin_role_permission rp ON rp.role_id = r.role_id
WHERE r.role_code = 'SUPER_ADMIN';
"@)
    Add-Check "SUPER_ADMIN has broad permissions" ($superAdminPermissionCount -ge 28) "count=$superAdminPermissionCount"

    $ticketTables = [int](Invoke-MySqlScalar -DbProps $DbProps -DatabaseName $DatabaseName -Query @"
SELECT COUNT(*)
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('t_service_ticket', 't_ticket_reply', 't_ticket_action_log');
"@)
    Add-Check "Ticket tables exist" ($ticketTables -eq 3) "count=$ticketTables"

    $adjustmentSourceColumns = [int](Invoke-MySqlScalar -DbProps $DbProps -DatabaseName $DatabaseName -Query @"
SELECT COUNT(*)
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 't_adjustment_request'
  AND (
    (column_name = 'reconciliation_item_id' AND is_nullable = 'YES')
    OR column_name IN ('source_type', 'source_ticket_id')
  );
"@)
    Add-Check "Adjustment requests support ticket sources" ($adjustmentSourceColumns -eq 3) "count=$adjustmentSourceColumns"

    $successWithoutLedger = [int](Invoke-MySqlScalar -DbProps $DbProps -DatabaseName $DatabaseName -Query @"
SELECT COUNT(*)
FROM t_transaction t
WHERE t.status = 'SUCCESS'
  AND NOT EXISTS (
    SELECT 1 FROM t_ledger_entry l WHERE l.transaction_id = t.transaction_id
  );
"@)
    Add-Check "Successful transactions have ledger entries" ($successWithoutLedger -eq 0) "missing=$successWithoutLedger"

    $balanceMismatch = [int](Invoke-MySqlScalar -DbProps $DbProps -DatabaseName $DatabaseName -Query @"
SELECT COUNT(*)
FROM t_account a
JOIN (
  SELECT account_id, MAX(ledger_id) AS ledger_id
  FROM t_ledger_entry
  GROUP BY account_id
) last_ledger ON last_ledger.account_id = a.account_id
JOIN t_ledger_entry l ON l.ledger_id = last_ledger.ledger_id
WHERE a.available_balance <> l.balance_after;
"@)
    Add-Check "Account balances match latest ledger" ($balanceMismatch -eq 0) "mismatches=$balanceMismatch"

    if (-not $SkipHttp) {
        $deniedSinceStart = [int](Invoke-MySqlScalar -DbProps $DbProps -DatabaseName $DatabaseName -Query @"
SELECT COUNT(*)
FROM t_admin_audit_log
WHERE operation_type = 'ADMIN_PERMISSION_DENIED'
  AND created_at >= '$startText';
"@)
        Add-Check "Forbidden HTTP access was audited" ($deniedSinceStart -ge 1) "count=$deniedSinceStart"
    }
}

try {
    Write-Host "iBank regression started at $($ScriptStart.ToString('yyyy-MM-dd HH:mm:ss'))"
    Write-Host "Repo: $RepoRoot"
    Write-Host "BaseUrl: $BaseUrl"

    $dbProps = Read-DbProperties
    $databaseName = Get-DatabaseName -JdbcUrl $dbProps["db.url"]

    if (-not $SkipBuild) {
        Invoke-Build
    }

    if ($RunSchemaMigration) {
        Write-Step "Schema migration"
        Invoke-MySqlFile -DbProps $dbProps -FilePath $SchemaPath
        Add-Check "Schema migration" $true "schema.sql applied"
    }

    if (-not $SkipDeploy) {
        Write-Step "Deploy"
        Deploy-War -TomcatPath $TomcatHome
        [void](Wait-AppReady -Url $BaseUrl)
        Add-Check "Tomcat deployment" $true "$BaseUrl/admin/login ready"
    }

    if (-not $SkipHttp) {
        Invoke-HttpRegression
    }

    if (-not $SkipDb) {
        Invoke-DatabaseRegression -DbProps $dbProps -DatabaseName $databaseName
    }
} catch {
    Add-Check "Regression script runtime" $false $_.Exception.Message
} finally {
    Write-Step "Summary"
    $Checks | Format-Table -AutoSize
    if ($Failures.Count -gt 0) {
        Write-Host ""
        Write-Host "Regression failed with $($Failures.Count) failure(s)." -ForegroundColor Red
        exit 1
    }
    Write-Host ""
    Write-Host "Regression passed. Checks: $($Checks.Count)" -ForegroundColor Green
}
