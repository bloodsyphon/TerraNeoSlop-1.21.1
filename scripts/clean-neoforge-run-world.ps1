param(
    [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string]$RunDirRelative = "platforms\neoforge\run",
    [switch]$IncludeClientSaves,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

$runDir = Join-Path $RepoRoot $RunDirRelative
if (-not (Test-Path $runDir -PathType Container)) {
    throw "NeoForge run directory does not exist: $runDir"
}

$targets = @(
    (Join-Path $runDir "world")
)

if ($IncludeClientSaves) {
    $targets += (Join-Path $runDir "saves")
}

foreach ($target in $targets) {
    if (-not (Test-Path $target)) {
        Write-Host "CLEAN_SKIP missing $target"
        continue
    }

    if ($DryRun) {
        Write-Host "CLEAN_WOULD_DELETE $target"
        continue
    }

    Remove-Item -Path $target -Recurse -Force
    Write-Host "CLEAN_OK deleted $target"
}
