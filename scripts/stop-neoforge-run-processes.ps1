param(
    [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

$runDirNeedle = [Regex]::Escape((Join-Path $RepoRoot "platforms\neoforge\run"))
$processes = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" |
    Where-Object {
        $_.CommandLine -match $runDirNeedle -or
        $_.CommandLine -like "*:platforms:neoforge:runServer*" -or
        $_.CommandLine -like "*:platforms:neoforge:runClient*"
    } |
    Sort-Object ProcessId

if (-not $processes -or $processes.Count -eq 0) {
    Write-Host "STOP_SKIP no matching NeoForge run java processes found"
    exit 0
}

foreach ($process in $processes) {
    if ($DryRun) {
        Write-Host ("STOP_WOULD_KILL pid={0}" -f $process.ProcessId)
        continue
    }

    Stop-Process -Id $process.ProcessId -Force
    Write-Host ("STOP_OK killed pid={0}" -f $process.ProcessId)
}
