$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$launcher = Join-Path $root 'scripts/start-backend.ps1'
$tempFile = Join-Path ([IO.Path]::GetTempPath()) ('obqa-loader-' + [Guid]::NewGuid().ToString('N') + '.env')
$names = @('DB_USERNAME','DB_PASSWORD','JWT_SECRET')
$before = @{}
foreach ($name in $names) {
    $before[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
    [Environment]::SetEnvironmentVariable($name, $null, 'Process')
}
try {
    $literal = 'literal$([IO.File]::WriteAllText("loader-injected", "bad"))#=`value'
    [IO.File]::WriteAllLines($tempFile, @('DB_USERNAME=unit-user', "DB_PASSWORD=$literal", 'JWT_SECRET=0123456789abcdef0123456789abcdef'))
    & $launcher -EnvFile $tempFile -CheckOnly
    if (Test-Path 'loader-injected') { throw 'File contents were executed.' }
    foreach ($name in $names) { if ([Environment]::GetEnvironmentVariable($name, 'Process')) { throw 'Environment was not restored.' } }
    [Environment]::SetEnvironmentVariable('JWT_SECRET', 'short-shell-value', 'Process')
    $failed = $false
    try { & $launcher -EnvFile $tempFile -CheckOnly } catch { $failed = $true }
    if (-not $failed) { throw 'Shell environment did not take precedence.' }
    if ($env:JWT_SECRET -ne 'short-shell-value') { throw 'Shell value was not restored.' }
    [Environment]::SetEnvironmentVariable('JWT_SECRET', $null, 'Process')
    [IO.File]::WriteAllLines($tempFile, @('DB_USERNAME=user', 'DB_PASSWORD=private-marker', 'JWT_SECRET=short-private-marker'))
    $failed = $false
    try { & $launcher -EnvFile $tempFile -CheckOnly } catch {
        $failed = $true
        if ($_.Exception.Message.Contains('private-marker')) { throw 'Error disclosed a secret.' }
    }
    if (-not $failed) { throw 'Weak key was accepted.' }
    [IO.File]::WriteAllLines($tempFile, @('DB_USERNAME=user', 'DB_USERNAME=duplicate'))
    $failed = $false
    try { & $launcher -EnvFile $tempFile -CheckOnly } catch { $failed = $true }
    if (-not $failed) { throw 'Duplicate key accepted.' }
    foreach ($name in $names) { if ([Environment]::GetEnvironmentVariable($name, 'Process')) { throw 'Failure did not restore environment.' } }
    Write-Host 'PASS: literal parsing, environment precedence/restoration, weak-key rejection, duplicate rejection and safe errors.'
} finally {
    foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name, $before[$name], 'Process') }
    Remove-Item -LiteralPath $tempFile -ErrorAction SilentlyContinue
}
