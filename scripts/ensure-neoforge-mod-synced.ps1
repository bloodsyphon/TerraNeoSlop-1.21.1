param(
    [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string]$ModsDir = "C:\Users\anelson\curseforge\minecraft\Instances\Terra Neo 1.21.11\mods",
    [string]$JarNamePattern = "Terra-neoforge-*.jar",
    [string]$SourceJarPath = "",
    [switch]$CheckOnly
)

$ErrorActionPreference = "Stop"

function Get-HashHex([string]$Path) {
    return (Get-FileHash -Algorithm SHA256 -Path $Path).Hash.ToLowerInvariant()
}

function Resolve-SourceJar([string]$Root, [string]$Pattern, [string]$ExplicitPath) {
    if ($ExplicitPath -and $ExplicitPath.Trim().Length -gt 0) {
        $resolved = (Resolve-Path $ExplicitPath).Path
        if (-not (Test-Path $resolved -PathType Leaf)) {
            throw "Source jar does not exist: $resolved"
        }
        return $resolved
    }

    $libsDir = Join-Path $Root "platforms\neoforge\build\libs"
    if (-not (Test-Path $libsDir -PathType Container)) {
        throw "Build libs directory does not exist: $libsDir"
    }

    $candidates = Get-ChildItem -Path $libsDir -File -Filter $Pattern |
        Where-Object { $_.Name -notlike "*-sources.jar" -and $_.Name -notlike "*-shaded.jar" } |
        Sort-Object LastWriteTimeUtc -Descending

    if (-not $candidates -or $candidates.Count -eq 0) {
        throw "No source jar found in $libsDir matching $Pattern"
    }

    return $candidates[0].FullName
}

$sourceJar = Resolve-SourceJar -Root $RepoRoot -Pattern $JarNamePattern -ExplicitPath $SourceJarPath
if (-not (Test-Path $ModsDir -PathType Container)) {
    throw "Mods directory does not exist: $ModsDir"
}

$destJar = Join-Path $ModsDir (Split-Path $sourceJar -Leaf)
$sourceHash = Get-HashHex $sourceJar
$destExists = Test-Path $destJar -PathType Leaf
$destHash = if ($destExists) { Get-HashHex $destJar } else { "" }

$copied = $false
if (-not $destExists -or $destHash -ne $sourceHash) {
    if ($CheckOnly) {
        throw "Mods jar is out of sync. Expected: $sourceJar -> $destJar"
    }
    Copy-Item -Path $sourceJar -Destination $destJar -Force
    $copied = $true
}

$verifiedHash = Get-HashHex $destJar
if ($verifiedHash -ne $sourceHash) {
    throw "Hash verification failed after copy. Source: $sourceHash, Dest: $verifiedHash"
}

$otherTerraJars = Get-ChildItem -Path $ModsDir -File -Filter $JarNamePattern |
    Where-Object { $_.FullName -ne $destJar } |
    Select-Object -ExpandProperty Name

if ($otherTerraJars.Count -gt 0) {
    Write-Warning ("Multiple Terra NeoForge jars in mods folder: " + ($otherTerraJars -join ", "))
}

$status = if ($copied) { "copied" } else { "already-synced" }
Write-Host ("SYNC_OK [{0}] {1} -> {2} sha256={3}" -f $status, $sourceJar, $destJar, $sourceHash.Substring(0, 12))
