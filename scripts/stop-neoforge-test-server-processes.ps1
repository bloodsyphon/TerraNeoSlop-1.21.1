param(
    [string]$ServerRoot = "C:\Terra\ServerTesting\NeoForge-1.21.11"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $ServerRoot -PathType Container)) {
    Write-Host ("STOP_SKIP server root does not exist: {0}" -f $ServerRoot)
    exit 0
}

$resolvedRoot = (Resolve-Path $ServerRoot).Path
$escapedRoot = [Regex]::Escape($resolvedRoot)

$targets = Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
    Where-Object { $_.CommandLine -and $_.CommandLine -match $escapedRoot }

if (-not $targets -or $targets.Count -eq 0) {
    Write-Host "STOP_SKIP no matching external NeoForge server java processes found"
    exit 0
}

$stopped = 0
foreach ($target in $targets) {
    try {
        Stop-Process -Id $target.ProcessId -Force -ErrorAction Stop
        $stopped++
    } catch {
        Write-Warning ("Failed stopping pid={0}: {1}" -f $target.ProcessId, $_.Exception.Message)
    }
}

Write-Host ("STOP_OK stopped {0} external NeoForge server java process(es)" -f $stopped)
exit 0
