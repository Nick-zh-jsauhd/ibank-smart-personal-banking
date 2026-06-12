param(
    [string]$BaseUrl = "http://localhost:8080/bank",
    [string]$AdminUser = "admin",
    [string]$AdminPassword = "admin123",
    [string]$ReviewerUser = "admin_reviewer",
    [string]$ReviewerPassword = "admin123",
    [decimal]$AdjustmentAmount = 12.34
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$ScriptStart = Get-Date
$RepoRoot = Split-Path -Parent $PSScriptRoot
$DbPropertiesPath = Join-Path $RepoRoot "src\main\resources\db.properties"
$Checks = New-Object System.Collections.Generic.List[object]
$Failures = New-Object System.Collections.Generic.List[string]

Add-Type -AssemblyName System.Web

function Add-Check {
    param(
        [string]$Name,
        [bool]$Passed,
        [string]$Detail = "",
        [switch]$Critical
    )
    $script:Checks.Add([PSCustomObject]@{
        Check = $Name
        Result = $(if ($Passed) { "PASS" } else { "FAIL" })
        Detail = $Detail
    })
    if ($Passed) {
        Write-Host "[PASS] $Name $Detail" -ForegroundColor Green
    } else {
        $script:Failures.Add("$Name $Detail")
        Write-Host "[FAIL] $Name $Detail" -ForegroundColor Red
        if ($Critical) {
            throw "$Name $Detail"
        }
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
    foreach ($key in @("db.url", "db.username", "db.password")) {
        if (-not $props.ContainsKey($key)) {
            throw "$key is missing in db.properties"
        }
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
        $value = & mysql --default-character-set=utf8mb4 --batch --skip-column-names --protocol=TCP `
            --host=localhost --port=3306 --user="$($DbProps['db.username'])" $DatabaseName --execute="$Query"
        if ($LASTEXITCODE -ne 0) {
            throw "mysql exited with code $LASTEXITCODE"
        }
        return (($value | Select-Object -First 1) -as [string]).Trim()
    } finally {
        Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
    }
}

function Invoke-Page {
    param(
        [Microsoft.PowerShell.Commands.WebRequestSession]$Session,
        [string]$Method,
        [string]$Uri,
        [string]$Body = ""
    )
    if ($Body.Length -gt 0) {
        return Invoke-WebRequest -UseBasicParsing -WebSession $Session -Method $Method -Uri $Uri `
            -Body $Body -ContentType "application/x-www-form-urlencoded" -MaximumRedirection 5 -TimeoutSec 20
    }
    return Invoke-WebRequest -UseBasicParsing -WebSession $Session -Method $Method -Uri $Uri `
        -MaximumRedirection 5 -TimeoutSec 20
}

function New-FormBody {
    param([hashtable]$Fields)
    return (($Fields.GetEnumerator() | ForEach-Object {
        [System.Web.HttpUtility]::UrlEncode([string]$_.Key) + "=" +
            [System.Web.HttpUtility]::UrlEncode([string]$_.Value)
    }) -join "&")
}

function Get-ResponseUrl {
    param($Response)
    if ($Response.BaseResponse -and $Response.BaseResponse.ResponseUri) {
        return $Response.BaseResponse.ResponseUri.AbsoluteUri
    }
    return ""
}

function Wait-AppReady {
    param([string]$Url)
    $deadline = (Get-Date).AddSeconds(30)
    $lastError = ""
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri "$($Url.TrimEnd('/'))/login" `
                -MaximumRedirection 5 -TimeoutSec 5
            if ($response.StatusCode -eq 200) {
                return
            }
        } catch {
            $lastError = $_.Exception.Message
        }
        Start-Sleep -Seconds 2
    }
    throw "Application is not ready at $Url. Last error: $lastError"
}

try {
    $base = $BaseUrl.TrimEnd("/")
    $dbProps = Read-DbProperties
    $databaseName = Get-DatabaseName -JdbcUrl $dbProps["db.url"]
    $stamp = Get-Date -Format "yyyyMMddHHmmss"
    $username = "e2e_$stamp"
    $password = "Test123456"
    $phone = "18{0:000000000}" -f (Get-Random -Minimum 0 -Maximum 1000000000)
    $businessId = "E2E-$stamp"
    $amountText = $AdjustmentAmount.ToString("0.00", [System.Globalization.CultureInfo]::InvariantCulture)

    Write-Host "iBank full-cycle simulation started at $($ScriptStart.ToString('yyyy-MM-dd HH:mm:ss'))"
    Write-Host "BaseUrl: $base"
    Write-Host "Test user: $username"

    Wait-AppReady -Url $base
    Add-Check "Application ready" $true "$base/login"

    $customerSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $adminSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $reviewerSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession

    $register = Invoke-Page -Session $customerSession -Method "Post" -Uri "$base/register" -Body (New-FormBody @{
        username = $username
        phone = $phone
        fullName = "E2E Customer $stamp"
        email = "$username@example.test"
        address = "E2E Address"
        password = $password
        confirmPassword = $password
    })
    Add-Check "Customer registration" ($register.StatusCode -eq 200 -and (Get-ResponseUrl $register).EndsWith("/login")) `
        "status=$($register.StatusCode), url=$(Get-ResponseUrl $register)" -Critical

    $customerLogin = Invoke-Page -Session $customerSession -Method "Post" -Uri "$base/login" -Body (New-FormBody @{
        identity = $username
        password = $password
    })
    Add-Check "Customer login" ($customerLogin.StatusCode -eq 200 -and (Get-ResponseUrl $customerLogin).EndsWith("/dashboard")) `
        "status=$($customerLogin.StatusCode), url=$(Get-ResponseUrl $customerLogin)" -Critical

    $profileText = Invoke-MySqlScalar -DbProps $dbProps -DatabaseName $databaseName -Query @"
SELECT CONCAT(u.user_id, '|', c.customer_id, '|', a.account_id, '|', a.account_no, '|', a.available_balance)
FROM t_user u
JOIN t_customer c ON c.customer_id = u.customer_id
JOIN t_account a ON a.customer_id = c.customer_id AND a.default_flag = 1
WHERE u.username = '$username'
LIMIT 1;
"@
    Add-Check "Customer default account exists" ($profileText.Length -gt 0) $profileText -Critical
    $profile = $profileText.Split("|")
    $customerUserId = [long]$profile[0]
    $customerId = [long]$profile[1]
    $accountId = [long]$profile[2]
    $accountNo = $profile[3]
    $initialBalance = [decimal]$profile[4]

    $ticketTitle = "E2E transaction dispute $stamp"
    $ticket = Invoke-Page -Session $customerSession -Method "Post" -Uri "$base/ticket/create" -Body (New-FormBody @{
        ticketType = "TRANSACTION_DISPUTE"
        priority = "HIGH"
        title = $ticketTitle
        relatedBusinessType = "TRANSACTION"
        relatedBusinessId = $businessId
        description = "E2E transaction dispute for closed-loop adjustment simulation. Account $accountNo."
    })
    $ticketUrl = Get-ResponseUrl $ticket
    $ticketId = $null
    if ($ticketUrl -match 'ticketId=(\d+)') {
        $ticketId = [long]$matches[1]
    }
    Add-Check "Customer creates dispute ticket" ($ticket.StatusCode -eq 200 -and $ticketId -ne $null) `
        "status=$($ticket.StatusCode), ticketId=$ticketId" -Critical

    $ticketStateText = Invoke-MySqlScalar -DbProps $dbProps -DatabaseName $databaseName -Query @"
SELECT CONCAT(ticket_no, '|', status, '|', assigned_role_code)
FROM t_service_ticket
WHERE ticket_id = $ticketId;
"@
    $ticketState = $ticketStateText.Split("|")
    $ticketNo = $ticketState[0]
    Add-Check "Ticket starts as submitted accounting case" `
        ($ticketState[1] -eq "SUBMITTED" -and $ticketState[2] -eq "ACCOUNTING_OPERATOR") `
        "status=$($ticketState[1]), role=$($ticketState[2])"

    $ticketAlertCount = [int](Invoke-MySqlScalar -DbProps $dbProps -DatabaseName $databaseName -Query @"
SELECT COUNT(*)
FROM t_admin_alert
WHERE alert_type = 'TICKET_NEW'
  AND target_type = 'SERVICE_TICKET'
  AND target_id = '$ticketId'
  AND status = 'NEW';
"@)
    Add-Check "Ticket creates admin todo" ($ticketAlertCount -ge 1) "newAlerts=$ticketAlertCount"

    $adminLogin = Invoke-Page -Session $adminSession -Method "Post" -Uri "$base/admin/login" -Body (New-FormBody @{
        identity = $AdminUser
        password = $AdminPassword
    })
    Add-Check "Super admin login" ($adminLogin.StatusCode -eq 200 -and (Get-ResponseUrl $adminLogin).EndsWith("/admin/dashboard")) `
        "status=$($adminLogin.StatusCode), url=$(Get-ResponseUrl $adminLogin)" -Critical

    $createAdjustment = Invoke-Page -Session $adminSession -Method "Post" -Uri "$base/admin/ticket/adjustment" -Body (New-FormBody @{
        ticketId = $ticketId
        accountNo = $accountNo
        direction = "INCREASE"
        amount = $amountText
        reason = "E2E ticket adjustment reason"
        evidence = "E2E evidence from transaction dispute ticket"
    })
    $adjustmentUrl = Get-ResponseUrl $createAdjustment
    $adjustmentId = $null
    if ($adjustmentUrl -match 'adjustmentId=(\d+)') {
        $adjustmentId = [long]$matches[1]
    }
    Add-Check "Admin creates adjustment from ticket" ($createAdjustment.StatusCode -eq 200 -and $adjustmentId -ne $null) `
        "status=$($createAdjustment.StatusCode), adjustmentId=$adjustmentId" -Critical

    $afterCreateText = Invoke-MySqlScalar -DbProps $dbProps -DatabaseName $databaseName -Query @"
SELECT CONCAT(ar.status, '|', ar.source_type, '|', ar.source_ticket_id, '|', ar.amount, '|', t.status)
FROM t_adjustment_request ar
JOIN t_service_ticket t ON t.ticket_id = ar.source_ticket_id
WHERE ar.adjustment_id = $adjustmentId;
"@
    $afterCreate = $afterCreateText.Split("|")
    Add-Check "Adjustment source and ticket status after create" `
        ($afterCreate[0] -eq "PENDING_REVIEW" -and $afterCreate[1] -eq "SERVICE_TICKET" `
            -and [long]$afterCreate[2] -eq $ticketId -and $afterCreate[4] -eq "INVESTIGATING") `
        "adjustment=$($afterCreate[0]), source=$($afterCreate[1]), ticket=$($afterCreate[4])"

    $reviewAlertCount = [int](Invoke-MySqlScalar -DbProps $dbProps -DatabaseName $databaseName -Query @"
SELECT COUNT(*)
FROM t_admin_alert
WHERE alert_type = 'ADJUSTMENT_REVIEW'
  AND target_type = 'ADJUSTMENT_REQUEST'
  AND target_id = '$adjustmentId'
  AND status = 'NEW';
"@)
    Add-Check "Adjustment review todo created" ($reviewAlertCount -eq 1) "count=$reviewAlertCount"

    $reviewerLogin = Invoke-Page -Session $reviewerSession -Method "Post" -Uri "$base/admin/login" -Body (New-FormBody @{
        identity = $ReviewerUser
        password = $ReviewerPassword
    })
    Add-Check "Accounting reviewer login" ($reviewerLogin.StatusCode -eq 200 -and (Get-ResponseUrl $reviewerLogin).EndsWith("/admin/dashboard")) `
        "status=$($reviewerLogin.StatusCode), url=$(Get-ResponseUrl $reviewerLogin)" -Critical

    $review = Invoke-Page -Session $reviewerSession -Method "Post" -Uri "$base/admin/adjustment/detail" -Body (New-FormBody @{
        adjustmentId = $adjustmentId
        action = "review"
        decision = "APPROVE"
        note = "E2E review approved"
    })
    Add-Check "Reviewer approves adjustment" ($review.StatusCode -eq 200 -and (Get-ResponseUrl $review).Contains("adjustmentId=$adjustmentId")) `
        "status=$($review.StatusCode), url=$(Get-ResponseUrl $review)" -Critical

    $afterReviewText = Invoke-MySqlScalar -DbProps $dbProps -DatabaseName $databaseName -Query @"
SELECT CONCAT(ar.status, '|', COALESCE(ar.reviewer_admin_user_id, 0), '|', t.status)
FROM t_adjustment_request ar
JOIN t_service_ticket t ON t.ticket_id = ar.source_ticket_id
WHERE ar.adjustment_id = $adjustmentId;
"@
    $afterReview = $afterReviewText.Split("|")
    Add-Check "Adjustment approved, ticket remains in handling" `
        ($afterReview[0] -eq "APPROVED" -and [long]$afterReview[1] -gt 0 -and $afterReview[2] -eq "INVESTIGATING") `
        "adjustment=$($afterReview[0]), ticket=$($afterReview[2])"

    $executeAlertCount = [int](Invoke-MySqlScalar -DbProps $dbProps -DatabaseName $databaseName -Query @"
SELECT COUNT(*)
FROM t_admin_alert
WHERE alert_type = 'ADJUSTMENT_EXECUTE'
  AND target_type = 'ADJUSTMENT_REQUEST'
  AND target_id = '$adjustmentId'
  AND status = 'NEW';
"@)
    Add-Check "Adjustment execute todo created" ($executeAlertCount -eq 1) "count=$executeAlertCount"

    $execute = Invoke-Page -Session $reviewerSession -Method "Post" -Uri "$base/admin/adjustment/detail" -Body (New-FormBody @{
        adjustmentId = $adjustmentId
        action = "execute"
    })
    Add-Check "Reviewer executes adjustment" ($execute.StatusCode -eq 200 -and (Get-ResponseUrl $execute).Contains("adjustmentId=$adjustmentId")) `
        "status=$($execute.StatusCode), url=$(Get-ResponseUrl $execute)" -Critical

    $expectedBalance = $initialBalance + $AdjustmentAmount
    $afterExecuteText = Invoke-MySqlScalar -DbProps $dbProps -DatabaseName $databaseName -Query @"
SELECT CONCAT(ar.status, '|', COALESCE(ar.executed_transaction_id, 0), '|', COALESCE(ar.executed_ledger_id, 0), '|',
              a.available_balance, '|', t.status)
FROM t_adjustment_request ar
JOIN t_account a ON a.account_id = ar.account_id
JOIN t_service_ticket t ON t.ticket_id = ar.source_ticket_id
WHERE ar.adjustment_id = $adjustmentId;
"@
    $afterExecute = $afterExecuteText.Split("|")
    $transactionId = [long]$afterExecute[1]
    $ledgerId = [long]$afterExecute[2]
    $actualBalance = [decimal]$afterExecute[3]
    Add-Check "Adjustment executed and ticket resolved" `
        ($afterExecute[0] -eq "EXECUTED" -and $transactionId -gt 0 -and $ledgerId -gt 0 `
            -and $actualBalance -eq $expectedBalance -and $afterExecute[4] -eq "RESOLVED") `
        "adjustment=$($afterExecute[0]), transaction=$transactionId, ledger=$ledgerId, balance=$actualBalance, ticket=$($afterExecute[4])"

    $ledgerText = Invoke-MySqlScalar -DbProps $dbProps -DatabaseName $databaseName -Query @"
SELECT CONCAT(t.txn_type, '|', t.status, '|', t.amount, '|', COALESCE(t.to_account_id, 0), '|',
              l.direction, '|', l.amount, '|', l.balance_after)
FROM t_transaction t
JOIN t_ledger_entry l ON l.transaction_id = t.transaction_id
WHERE t.transaction_id = $transactionId
  AND l.ledger_id = $ledgerId;
"@
    $ledger = $ledgerText.Split("|")
    Add-Check "Transaction and ledger are consistent" `
        ($ledger[0] -eq "ACCOUNT_ADJUSTMENT" -and $ledger[1] -eq "SUCCESS" `
            -and [decimal]$ledger[2] -eq $AdjustmentAmount -and [long]$ledger[3] -eq $accountId `
            -and $ledger[4] -eq "IN" -and [decimal]$ledger[5] -eq $AdjustmentAmount `
            -and [decimal]$ledger[6] -eq $expectedBalance) `
        "txn=$($ledger[0]), ledgerDirection=$($ledger[4]), balanceAfter=$($ledger[6])"

    $close = Invoke-Page -Session $customerSession -Method "Post" -Uri "$base/ticket/detail" -Body (New-FormBody @{
        ticketId = $ticketId
        action = "close"
        note = "E2E customer confirms the issue is resolved"
    })
    Add-Check "Customer closes resolved ticket" ($close.StatusCode -eq 200 -and (Get-ResponseUrl $close).Contains("ticketId=$ticketId")) `
        "status=$($close.StatusCode), url=$(Get-ResponseUrl $close)" -Critical

    $finalTicketStatus = Invoke-MySqlScalar -DbProps $dbProps -DatabaseName $databaseName -Query @"
SELECT status
FROM t_service_ticket
WHERE ticket_id = $ticketId;
"@
    Add-Check "Ticket final status is closed" ($finalTicketStatus -eq "CLOSED") "status=$finalTicketStatus"

    $ticketLogCount = [int](Invoke-MySqlScalar -DbProps $dbProps -DatabaseName $databaseName -Query @"
SELECT COUNT(*)
FROM t_ticket_action_log
WHERE ticket_id = $ticketId
  AND action_type IN ('CREATE', 'CREATE_ADJUSTMENT', 'ADJUSTMENT_REVIEW', 'ADJUSTMENT_EXECUTED', 'CUSTOMER_CLOSE');
"@)
    Add-Check "Ticket action logs cover full flow" ($ticketLogCount -eq 5) "count=$ticketLogCount"

    $adjustmentLogCount = [int](Invoke-MySqlScalar -DbProps $dbProps -DatabaseName $databaseName -Query @"
SELECT COUNT(*)
FROM t_adjustment_action_log
WHERE adjustment_id = $adjustmentId
  AND action_type IN ('CREATE', 'REVIEW', 'EXECUTE');
"@)
    Add-Check "Adjustment action logs cover full flow" ($adjustmentLogCount -eq 3) "count=$adjustmentLogCount"

    $notificationCount = [int](Invoke-MySqlScalar -DbProps $dbProps -DatabaseName $databaseName -Query @"
SELECT COUNT(*)
FROM t_notification
WHERE customer_id = $customerId
  AND business_type = 'SERVICE_TICKET'
  AND business_id = '$ticketNo';
"@)
    Add-Check "Customer service notifications generated" ($notificationCount -ge 4) "count=$notificationCount"

    $alertOpenCount = [int](Invoke-MySqlScalar -DbProps $dbProps -DatabaseName $databaseName -Query @"
SELECT COUNT(*)
FROM t_admin_alert
WHERE ((
    target_type = 'SERVICE_TICKET' AND target_id = '$ticketId'
  ) OR (
    target_type = 'ADJUSTMENT_REQUEST' AND target_id = '$adjustmentId'
  ))
  AND status = 'NEW';
"@)
    Add-Check "No open todos remain for this flow" ($alertOpenCount -eq 0) "openTodos=$alertOpenCount"

    $auditCount = [int](Invoke-MySqlScalar -DbProps $dbProps -DatabaseName $databaseName -Query @"
SELECT COUNT(*)
FROM t_admin_audit_log
WHERE target_id IN ('$ticketId', '$adjustmentId')
  AND operation_type IN ('CREATE_ADJUSTMENT', 'REVIEW_ADJUSTMENT', 'EXECUTE_ADJUSTMENT', 'HANDLE_SERVICE_TICKET');
"@)
    Add-Check "Admin audit logs generated" ($auditCount -ge 3) "count=$auditCount"

    Write-Host ""
    Write-Host "Flow entities:"
    Write-Host "  customerUserId=$customerUserId customerId=$customerId"
    Write-Host "  accountId=$accountId accountNo=$accountNo"
    Write-Host "  ticketId=$ticketId ticketNo=$ticketNo"
    Write-Host "  adjustmentId=$adjustmentId transactionId=$transactionId ledgerId=$ledgerId"
    Write-Host "  balance: $initialBalance -> $actualBalance"
} catch {
    Add-Check "Full-cycle script runtime" $false $_.Exception.Message
} finally {
    Write-Host ""
    Write-Host "== Summary =="
    $Checks | Format-Table -AutoSize
    if ($Failures.Count -gt 0) {
        Write-Host ""
        Write-Host "Full-cycle simulation failed with $($Failures.Count) failure(s)." -ForegroundColor Red
        exit 1
    }
    Write-Host ""
    Write-Host "Full-cycle simulation passed. Checks: $($Checks.Count)" -ForegroundColor Green
}
