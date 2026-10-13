param(
    [string]$EnvFile = (Join-Path $PSScriptRoot '../.env.backend.local'),
    [switch]$CheckOnly
)

$ErrorActionPreference = 'Stop'
$allowed = @('DB_URL', 'DB_USERNAME', 'DB_PASSWORD', 'JWT_SECRET',
    'SPRING_PROFILES_ACTIVE', 'APP_FRONTEND_URL', 'MAIL_HOST', 'MAIL_PORT',
    'MAIL_USERNAME', 'MAIL_PASSWORD', 'MAIL_FROM', 'AUDIT_RETENTION_DAYS', 'AUDIT_CLEANUP_CRON')
$previous = @{}
$loaded = @{}
try {
    if (Test-Path -LiteralPath $EnvFile) {
        $lineNumber = 0
        foreach ($line in [IO.File]::ReadAllLines((Resolve-Path -LiteralPath $EnvFile).Path)) {
            $lineNumber++
            if ([string]::IsNullOrWhiteSpace($line) -or $line.TrimStart().StartsWith('#')) { continue }
            # Values are literal, including $, #, backticks and =. Never execute file contents.
            if ($line -notmatch '^([A-Z][A-Z0-9_]*)=(.*)$' -or $allowed -notcontains $Matches[1]) {
                throw "Unsupported environment entry at line $lineNumber."
            }
            $name = $Matches[1]
            $value = $Matches[2]
            if ($loaded.ContainsKey($name)) { throw "Duplicate environment entry at line $lineNumber." }
            $loaded[$name] = $true
            $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
            # An explicitly supplied shell environment takes precedence over the local file.
            if ($null -eq $previous[$name]) {
                [Environment]::SetEnvironmentVariable($name, $value, 'Process')
            }
        }
    }
    foreach ($name in @('DB_USERNAME', 'DB_PASSWORD', 'JWT_SECRET')) {
        $value = [Environment]::GetEnvironmentVariable($name, 'Process')
        if ([string]::IsNullOrWhiteSpace($value)) { throw "Set $name in the environment or private backend environment file." }
    }
    $key = [Environment]::GetEnvironmentVariable('JWT_SECRET', 'Process')
    if ([Text.Encoding]::UTF8.GetByteCount($key) -lt 32 -or $key -match '^\$\{' -or
        $key -match '(?i)^(change[-_]?me|replace[-_]?me|your[-_])') {
        throw 'JWT_SECRET must be a generated signing key of at least 32 UTF-8 bytes.'
    }
    if ($CheckOnly) {
        Write-Host 'Backend environment validated; no secret values displayed.'
    } else {
        Push-Location (Join-Path $PSScriptRoot '../Software-project-Backend')
        try {
            & mvn -B spring-boot:run
            if ($LASTEXITCODE -ne 0) { throw 'Backend exited unsuccessfully; review the backend log.' }
        } finally { Pop-Location }
    }
} finally {
    foreach ($name in $previous.Keys) {
        [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process')
    }
}
