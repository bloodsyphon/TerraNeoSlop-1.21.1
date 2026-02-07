param(
    [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string]$ModsDir = "C:\Users\anelson\curseforge\minecraft\Instances\Terra Neo 1.21.11\mods",
    [switch]$SkipBuild,
    [switch]$SkipClean,
    [switch]$SkipStop,
    [switch]$IncludeClientSaves
)

$ErrorActionPreference = "Stop"

Push-Location $RepoRoot
try {
    if (-not $SkipStop) {
        & powershell -ExecutionPolicy Bypass -File .\scripts\stop-neoforge-run-processes.ps1 -RepoRoot $RepoRoot
        if ($LASTEXITCODE -ne 0) {
            throw "Stopping NeoForge run processes failed with exit code $LASTEXITCODE"
        }
    }

    if (-not $SkipClean) {
        $cleanArgs = @(
            "-ExecutionPolicy", "Bypass",
            "-File", ".\scripts\clean-neoforge-run-world.ps1",
            "-RepoRoot", $RepoRoot
        )
        if ($IncludeClientSaves) {
            $cleanArgs += "-IncludeClientSaves"
        }
        & powershell @cleanArgs
        if ($LASTEXITCODE -ne 0) {
            throw "Run directory cleanup failed with exit code $LASTEXITCODE"
        }
    }

    $buildArgs = @(
        "-ExecutionPolicy", "Bypass",
        "-File", ".\scripts\build-and-sync-neoforge.ps1",
        "-RepoRoot", $RepoRoot,
        "-ModsDir", $ModsDir
    )
    if ($SkipBuild) {
        $buildArgs += "-SkipBuild"
    }

    & powershell @buildArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Build/sync failed with exit code $LASTEXITCODE"
    }

    & powershell -ExecutionPolicy Bypass -File .\scripts\ensure-neoforge-mod-synced.ps1 -RepoRoot $RepoRoot -ModsDir $ModsDir -CheckOnly
    if ($LASTEXITCODE -ne 0) {
        throw "Jar sync check failed with exit code $LASTEXITCODE"
    }

    & .\gradlew.bat :platforms:neoforge:runServer --console=plain
}
finally {
    Pop-Location
}
