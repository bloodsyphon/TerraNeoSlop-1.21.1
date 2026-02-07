param(
    [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string]$ModsDir = "C:\Users\anelson\curseforge\minecraft\Instances\Terra Neo 1.21.11\mods",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

Push-Location $RepoRoot
try {
    if (-not $SkipBuild) {
        & .\gradlew.bat :platforms:neoforge:build -x test
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle build failed with exit code $LASTEXITCODE"
        }
    }

    & powershell -ExecutionPolicy Bypass -File .\scripts\ensure-neoforge-mod-synced.ps1 -ModsDir $ModsDir
    if ($LASTEXITCODE -ne 0) {
        throw "Jar sync verification failed with exit code $LASTEXITCODE"
    }
}
finally {
    Pop-Location
}
