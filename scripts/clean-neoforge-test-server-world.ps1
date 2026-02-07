param(
    [string]$ServerRoot = "C:\Terra\ServerTesting\NeoForge-1.21.11"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $ServerRoot -PathType Container)) {
    throw "Server root does not exist: $ServerRoot"
}

$pathsToDelete = @(
    (Join-Path $ServerRoot "world"),
    (Join-Path $ServerRoot "world_nether"),
    (Join-Path $ServerRoot "world_the_end"),
    (Join-Path $ServerRoot "logs\latest.log"),
    (Join-Path $ServerRoot "config\Terra\addons")
)

$deleted = @()
foreach ($path in $pathsToDelete) {
    if (Test-Path $path) {
        Remove-Item -Path $path -Recurse -Force
        $deleted += $path
    }
}

if ($deleted.Count -eq 0) {
    Write-Host ("CLEAN_SKIP no world artifacts found under {0}" -f $ServerRoot)
} else {
    Write-Host ("CLEAN_OK deleted {0}" -f ($deleted -join ", "))
}

exit 0
