param(
    [string]$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string]$ModsDir = "C:\Users\anelson\curseforge\minecraft\Instances\Terra Neo 1.21.11\mods",
    [string]$LevelType = "terra:overworld/overworld",
    [string]$InitialEnabledPacks = "vanilla",
    [int]$TimeoutSeconds = 240,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

function Set-ServerProperty {
    param(
        [string]$Path,
        [string]$Key,
        [string]$Value
    )

    $escaped = [Regex]::Escape($Key)
    $lines = Get-Content -Path $Path
    $updated = $false
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match "^$escaped=") {
            $lines[$i] = "$Key=$Value"
            $updated = $true
            break
        }
    }

    if (-not $updated) {
        $lines += "$Key=$Value"
    }

    Set-Content -Path $Path -Value $lines -Encoding ASCII
}

Push-Location $RepoRoot
try {
    & powershell -ExecutionPolicy Bypass -File .\scripts\stop-neoforge-run-processes.ps1 -RepoRoot $RepoRoot
    if ($LASTEXITCODE -ne 0) {
        throw "Failed stopping NeoForge run processes."
    }

    & powershell -ExecutionPolicy Bypass -File .\scripts\clean-neoforge-run-world.ps1 -RepoRoot $RepoRoot
    if ($LASTEXITCODE -ne 0) {
        throw "Failed cleaning NeoForge world directory."
    }

    $serverProperties = Join-Path $RepoRoot "platforms\neoforge\run\server.properties"
    if (-not (Test-Path $serverProperties -PathType Leaf)) {
        throw "Missing server.properties: $serverProperties"
    }

    $levelTypeValue = $LevelType -replace ":", "\:"
    Set-ServerProperty -Path $serverProperties -Key "level-type" -Value $levelTypeValue
    Set-ServerProperty -Path $serverProperties -Key "initial-enabled-packs" -Value $InitialEnabledPacks
    Write-Host ("SERVER_PROPERTIES level-type={0} initial-enabled-packs={1}" -f $levelTypeValue, $InitialEnabledPacks)

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
        throw "Build/sync step failed."
    }

    & powershell -ExecutionPolicy Bypass -File .\scripts\ensure-neoforge-mod-synced.ps1 -RepoRoot $RepoRoot -ModsDir $ModsDir -CheckOnly
    if ($LASTEXITCODE -ne 0) {
        throw "Post-build jar sync check failed."
    }

    $latestLog = Join-Path $RepoRoot "platforms\neoforge\run\logs\latest.log"
    if (Test-Path $latestLog) {
        Remove-Item -Path $latestLog -Force
    }

    $process = Start-Process -FilePath ".\gradlew.bat" -ArgumentList ":platforms:neoforge:runServer", "--console=plain" -PassThru
    Write-Host ("SERVER_PROCESS pid={0}" -f $process.Id)

    $status = "timeout"
    $reason = "Did not observe success/failure markers before timeout."

    try {
        $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
        while ((Get-Date) -lt $deadline) {
            Start-Sleep -Seconds 1
            if (-not (Test-Path $latestLog)) {
                if ($process.HasExited) {
                    $status = "failed"
                    $reason = "Gradle runServer process exited before latest.log was created."
                    break
                }
                continue
            }

            $tail = Get-Content -Path $latestLog -Tail 250 -ErrorAction SilentlyContinue
            $text = ($tail -join "`n")

            if ($text -match "Failed to start the minecraft server" -or
                $text -match "ModLoadingException" -or
                $text -match "MixinTransformerError" -or
                $text -match "Crash report saved") {
                $status = "failed"
                $reason = "Detected failure markers in latest.log."
                break
            }

            if ($text -match "Preparing spawn area: 100%" -or $text -match "Done \(") {
                $status = "started"
                $reason = "Observed server startup completion marker."
                break
            }

            if ($process.HasExited) {
                $status = "failed"
                $reason = "Gradle runServer process exited before startup completion marker."
                break
            }
        }
    }
    finally {
        & powershell -ExecutionPolicy Bypass -File .\scripts\stop-neoforge-run-processes.ps1 -RepoRoot $RepoRoot
        if ($LASTEXITCODE -ne 0) {
            throw "Failed stopping NeoForge run processes after test."
        }
    }

    $worldLevelDat = Join-Path $RepoRoot "platforms\neoforge\run\world\level.dat"
    $worldSummaryJson = "{}"
    if (Test-Path $worldLevelDat -PathType Leaf) {
        $worldSummaryJson = @"
import gzip, io, json, struct
from pathlib import Path

path = Path(r"$worldLevelDat")

def u1(f):
    b = f.read(1)
    if len(b) != 1:
        raise EOFError
    return b[0]
def u2(f):
    b = f.read(2)
    if len(b) != 2:
        raise EOFError
    return struct.unpack(">H", b)[0]
def i2(f):
    b = f.read(2)
    if len(b) != 2:
        raise EOFError
    return struct.unpack(">h", b)[0]
def i4(f):
    b = f.read(4)
    if len(b) != 4:
        raise EOFError
    return struct.unpack(">i", b)[0]
def i8(f):
    b = f.read(8)
    if len(b) != 8:
        raise EOFError
    return struct.unpack(">q", b)[0]
def f4(f):
    b = f.read(4)
    if len(b) != 4:
        raise EOFError
    return struct.unpack(">f", b)[0]
def f8(f):
    b = f.read(8)
    if len(b) != 8:
        raise EOFError
    return struct.unpack(">d", b)[0]
def s(f):
    n = u2(f)
    b = f.read(n)
    if len(b) != n:
        raise EOFError
    return b.decode("utf-8", "replace")
def payload(f, t):
    if t == 0:
        return None
    if t == 1:
        return struct.unpack(">b", f.read(1))[0]
    if t == 2:
        return i2(f)
    if t == 3:
        return i4(f)
    if t == 4:
        return i8(f)
    if t == 5:
        return f4(f)
    if t == 6:
        return f8(f)
    if t == 7:
        n = i4(f); return f.read(n)
    if t == 8:
        return s(f)
    if t == 9:
        et = u1(f); n = i4(f)
        return [payload(f, et) for _ in range(n)]
    if t == 10:
        d = {}
        while True:
            nt = u1(f)
            if nt == 0:
                break
            name = s(f)
            d[name] = payload(f, nt)
        return d
    if t == 11:
        n = i4(f); return [i4(f) for _ in range(n)]
    if t == 12:
        n = i4(f); return [i8(f) for _ in range(n)]
    raise ValueError(t)

with gzip.open(path, "rb") as gz:
    data = gz.read()
f = io.BytesIO(data)
root_type = u1(f)
root_name = s(f)
root = payload(f, root_type)

gen_type = None
biome_source_type = None
biome_source_pack = None
try:
    generator = root["Data"]["WorldGenSettings"]["dimensions"]["minecraft:overworld"]["generator"]
    gen_type = generator.get("type")
    biome_source = generator.get("biome_source")
    if isinstance(biome_source, dict):
        biome_source_type = biome_source.get("type")
        pack = biome_source.get("pack")
        if isinstance(pack, dict):
            pack_id = pack.get("pack")
            if isinstance(pack_id, dict):
                ns = pack_id.get("namespace")
                pid = pack_id.get("id")
                if ns and pid:
                    biome_source_pack = f"{ns}:{pid}"
except Exception:
    pass

print(json.dumps({
    "rootName": root_name,
    "generatorType": gen_type,
    "biomeSourceType": biome_source_type,
    "biomeSourcePack": biome_source_pack
}))
"@ | python -
    }

    $worldSummary = $worldSummaryJson | ConvertFrom-Json
    Write-Host ("WORLDGEN_SUMMARY generator={0} biome_source={1} biome_pack={2}" -f $worldSummary.generatorType, $worldSummary.biomeSourceType, $worldSummary.biomeSourcePack)

    if (Test-Path $latestLog -PathType Leaf) {
        $logTail = Get-Content -Path $latestLog -Tail 300
        $createdPresetLine = $logTail | Select-String -Pattern "Created world type" | Select-Object -Last 1
        if ($createdPresetLine) {
            Write-Host ("PRESET_LINE {0}" -f $createdPresetLine.Line.Trim())
        }
    }

    Write-Host ("SERVER_TEST_STATUS={0}" -f $status)
    Write-Host ("SERVER_TEST_REASON={0}" -f $reason)

    if ($status -ne "started") {
        exit 1
    }
    if ($worldSummary.generatorType -ne "terra:terra") {
        Write-Host "SERVER_TEST_RESULT=FAIL_WORLDGEN_NOT_TERRA"
        exit 2
    }
    if ($worldSummary.biomeSourceType -and $worldSummary.biomeSourceType -ne "terra:terra") {
        Write-Host "SERVER_TEST_RESULT=FAIL_WORLDGEN_NOT_TERRA"
        exit 2
    }
    if (-not $worldSummary.biomeSourceType -and -not $worldSummary.biomeSourcePack) {
        Write-Host "SERVER_TEST_RESULT=FAIL_WORLDGEN_NOT_TERRA"
        exit 2
    }

    Write-Host "SERVER_TEST_RESULT=PASS_TERRA_WORLDGEN"
    exit 0
}
finally {
    Pop-Location
}
