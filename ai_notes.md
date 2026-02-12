

# Codex Notes - NeoForge 1.21.1 Worklog

**Date**: February 5, 2026  
**Branch**: `Codex`  
**Scope**: Continue NeoForge enablement for Terra after Claude‚Äôs last pass

This file is meant for Claude Code to quickly understand what I (Codex) changed and why.

---

## Summary of What I Tried

Goal: Fix NeoForge compile classpath so `net.neoforged.fml.*` imports resolve.

Key finding: Gradle was resolving the `neoforge` module using the **Gradle module metadata** and picking the `universalJar` variant. That variant does **not** include FML classes, so compile still fails even though the dependency is present.

---

## Evidence / Diagnostics

1. `:platforms:neoforge:dependencies --configuration compileClasspath` shows:
   - `net.neoforged:neoforge:21.1.211` *is* on the compile classpath.
2. `:platforms:neoforge:dependencyInsight --configuration compileClasspath --dependency net.neoforged:neoforge`
   - Resolves **variant `universalJar`**.
   - Shows mismatch between requested attributes (api/classes) and provided (runtime/jar).
3. The resolved JAR `neoforge-21.1.211-universal.jar`:
   - Contains `net/neoforged/neoforge/**`
   - Does **not** contain `net/neoforged/fml/**` (checked via `jar tf` + grep).
4. The POM for `net.neoforged:neoforge:21.1.211` *does* list FML loader deps:
   - `net.neoforged.fancymodloader:loader:4.0.42`
   - `net.neoforged.fancymodloader:earlydisplay:4.0.42`
   - etc.
   This implies the issue is **variant/metadata selection**, not a missing artifact.

---

## Change Made (Option 1)

**Action:** Force Gradle to prefer POM metadata for the NeoForge repository.

**File changed:**
- `buildSrc/src/main/kotlin/DependencyConfig.kt`

**What changed:**
- Added `metadataSources { mavenPom(); artifact() }` on the NeoForge maven repo.
- Scoped repo content to NeoForge/FML-related groups.

**Attempted (and reverted):**
- Tried to call `ignoreGradleMetadataRedirection()` on the Maven repo to force POM usage.
- Build failed: method not available in this Gradle/Kotlin DSL.

**Why:**
NeoForge‚Äôs Gradle module metadata defines multiple variants. For `compileClasspath`, Gradle picked the `universalJar` variant, which lacks FML classes. POM metadata includes the missing dependencies, so forcing POM metadata should pull the correct compile-time deps.

---

## Next Steps To Validate

1. Re-run:
   - `./gradlew :platforms:neoforge:dependencies --configuration compileClasspath`
   - Verify `net.neoforged.fancymodloader:loader` now appears.
2. Then try:
   - `./gradlew :platforms:neoforge:compileJava`
   - Confirm the `net.neoforged.fml.*` imports resolve.

If this does **not** work, fallback options are:
- Explicitly add `net.neoforged.fancymodloader:loader` and `earlydisplay` as dependencies.
- Force a specific NeoForge variant/capability if Architectury Loom exposes it.

**Status after change:**  
Re-running `dependencyInsight` still shows `net.neoforged:neoforge` resolving to the `universalJar` variant, and the compile classpath does not include `net.neoforged.fancymodloader:loader`. So the POM-preference change alone did **not** fix the classpath yet.

---

## Research Notes (Architectury Loom + NeoForge examples)

I tried to locate another mod (ideally terrain-related) that:
1) supports NeoForge, and
2) builds with Architectury Loom,
so we can compare build wiring.

### Findings
**Distant Horizons** (terrain LOD mod) appears to be a multi-loader project with a `neoforge/` subproject and explicitly lists **Architectury Loom** in its README. This makes it a likely real-world reference for how a NeoForge module is wired in practice. I couldn‚Äôt reliably access the repo‚Äôs `neoforge/build.gradle` via the public GitHub pages in this environment, so I don‚Äôt yet have a concrete Gradle snippet from them.

If needed, the next step would be to pull the repo locally (from GitHub or their GitLab mirror) and inspect `neoforge/build.gradle` and `common/build.gradle` for how they declare the NeoForge dependency and configurations.

---

## Research Notes: Liteminer (NeoForge + Architectury)

User asked to inspect Liteminer for NeoForge + Architectury usage and identify the last version that used Architectury.

**What I did:**
- Cloned `https://github.com/iamkaf/liteminer` into `C:\Terra\Terra\.codex_research\liteminer`.
- Checked branches `1.21.8` and `1.21.9`.

**Findings (from local repo inspection):**
- `1.21.8` branch:
  - Uses Architectury API in NeoForge module (`modImplementation "dev.architectury:architectury-neoforge:..."`).
  - NeoForge dependency is declared via the `neoForge` configuration.
  - Build config is pulled from `modresources` scripts.
- `1.21.9` branch:
  - Still uses `architectury { ... neoForge() }` in the build, but `architectury_api_version` is set to `NOT_AVAILABLE`.
  - Architectury API dependency is removed.
  - Build logic is inlined rather than pulled from `modresources`.

**Conclusion (re: ‚Äúlast Architectury version‚Äù):**
Based on repo branches, the last version using the **Architectury API** is `1.21.8` (and earlier). The switch away from Architectury API occurs at `1.21.9`.

---

## Research Notes: Regions Unexplored (branch `1.21`)

User asked to verify whether the latest `1.21` branch uses Architectury.

**What I did:**
- Cloned `https://github.com/UHQ-GAMES-MODS/RegionsUnexplored` (branch `1.21`) into `C:\Terra\Terra\.codex_research\regions-unexplored`.
- Inspected `build.gradle.kts`.

**Findings:**
- The build uses the `earth.terrarium.cloche` plugin and its `cloche { ... }` DSL to define Fabric + NeoForge targets.
- No `architectury` plugin usage or Architectury API dependency in this branch.
- NeoForge target is configured via `cloche { neoforge("neoforge:21.1") { ... } }`.

**Conclusion:**
`1.21` branch does **not** appear to use Architectury; it‚Äôs using Cloche for multi-loader setup instead.

---

## Fix Attempt: Align Terra NeoForge Module with Template + Liteminer

Based on:
- NeoForge-only template (uses `neoForge` config + layered Yarn mappings patch)
- Liteminer (uses `neoForge` config and `loom.platform=neoforge`)

**Changes made:**
1. `platforms/neoforge/gradle.properties`
   - Added `loom.platform=neoforge` to ensure Loom sets up NeoForge-specific configurations.
   - Added `loom.mixin.useLegacyMixinAp=true` because Loom kept throwing the legacy Mixin AP error.
2. `gradle.properties`
   - Added `loom.mixin.useLegacyMixinAp=true` at root as a stronger signal for Loom.
2. `platforms/neoforge/build.gradle.kts`
   - Switched `mappings(...)` to layered mappings:
     - `net.fabricmc:yarn:${Versions.Mod.yarn}:v2`
     - `dev.architectury:yarn-mappings-patch-neoforge:${Versions.Mod.yarnMappingsPatchNeoForge}`
   - Replaced `modImplementation(neoforge)`/`compileOnly(neoforge)` with:
     - `add("neoForge", "net.neoforged:neoforge:${Versions.NeoForge.neoforge}")`
   - Added `loom.mixin.useLegacyMixinAp.set(true)` to satisfy Loom requirement for Mixin AP configuration.
   - Added explicit NeoForge Maven repo in this subproject (to resolve `neoform` zip).
3. `platforms/mixin-common/build.gradle.kts` and `platforms/mixin-lifecycle/build.gradle.kts`
   - Added `loom.mixin.useLegacyMixinAp.set(true)` for consistency across mixin modules.

**Follow-up change:**
- Removed NeoForge repo `content {}` filtering in `buildSrc/src/main/kotlin/DependencyConfig.kt` because `neoform` was only being searched in Solo Studios (suggesting the NeoForge repo was being ignored).
- Also removed the `metadataSources { mavenPom(); artifact() }` override so the NeoForge repo behaves like the standard template/Liteminer setup.
- Added NeoForge Maven repo to `settings.gradle.kts` `pluginManagement.repositories` because `mcp` resolution seemed to only search pluginManagement repos (Solo Studios).
- Added `dependencyResolutionManagement` with `RepositoriesMode.PREFER_SETTINGS` to force a full repository list (including NeoForge) at the settings level.
3. `buildSrc/src/main/kotlin/Versions.kt`
   - Added `Versions.Mod.yarnMappingsPatchNeoForge = "1.21+build.6"`

**Rationale:**
The template and Liteminer both rely on Loom's `neoForge` configuration, which should resolve NeoForge classes properly. The layered mappings patch is part of the NeoForge-only template and may be required for correct mappings with Loom.

---

## Files Touched

- `buildSrc/src/main/kotlin/DependencyConfig.kt` (NeoForge repo metadata preference)

No other files changed yet on `Codex`.

---

## Update (Feb 5, 2026) - Progress + Fixes Applied

### Key Issue 1: `neoform` resolving only from Solo Studios
**Symptom:** Gradle failed on `:platforms:neoforge:mcp` because it searched only:
`https://maven.solo-studios.ca/releases/net/neoforged/neoform/...`

**Verification:** I confirmed the `neoform` zip **does exist** on NeoForged repo with:
- `https://maven.neoforged.net/releases/net/neoforged/neoform/1.21.1-20240808.144430/neoform-1.21.1-20240808.144430.zip` (HTTP 200)

**Fix Applied:** Restricted Maven content so `net.neoforged*` resolves from NeoForge repo, not Solo:
- `buildSrc/src/main/kotlin/DependencyConfig.kt`
  - Solo Studios repos: `content { excludeGroupByRegex("net\\.neoforged(\\..*)?") }`
  - NeoForge repo: `content { includeGroupByRegex("net\\.neoforged(\\..*)?") }`

Result: `neoform` no longer resolved from Solo Studios.

---

### Key Issue 2: Minecraft version mismatch
**Symptom:** Loom error:
`NeoForge 21.1.211 is not for Minecraft 1.21.10 (expected: 1.21.1)`

**Root cause:** Terra uses `Minecraft 1.21.10` in `Versions.Mod.minecraft`.

**Fix Applied:** Updated NeoForge version to match 1.21.10:
- `buildSrc/src/main/kotlin/Versions.kt`
  - `neoforge = "21.10.64"` (from NeoForged maven metadata)

---

### Key Issue 3: `data/server.lzma` missing (NeoForge 21.10.64)
**Symptom:** Loom failed with:
`Failed to provide net.neoforged:neoforge:21.10.64 : NoSuchFileException: data/server.lzma`

**Notes:**
- `neoform` for 1.21.10 contains patches but no `data/server.lzma`.
- This appears to be an **Architectury Loom compatibility** issue with newer NeoForge artifacts.

**Fix Applied:** Upgraded Architectury Loom to latest snapshot:
- `buildSrc/src/main/kotlin/Versions.kt`
  - `architecuryLoom = "1.13-SNAPSHOT"`

After this change, the `data/server.lzma` error disappeared.

---

## Current Status (Confirmed)

‚úÖ `./gradlew :platforms:neoforge:compileJava` **succeeds**  
- No more `neoform` or `server.lzma` errors.  
- NeoForge classes resolve on compile classpath.  
- Warnings remain (existing code warnings + mixin annotation warnings), but build succeeds.

---

## Files Touched Since Last Entry

- `buildSrc/src/main/kotlin/DependencyConfig.kt`
  - Added repo content filters to force NeoForge artifacts to NeoForge repo.
- `buildSrc/src/main/kotlin/Versions.kt`
  - `neoforge = "21.10.64"`
  - `architecuryLoom = "1.13-SNAPSHOT"`
- `settings.gradle.kts`
  - Added/removed temporary repo resolution attempts (final state: no `dependencyResolutionManagement` block; a `gradle.beforeProject` hook remains but is likely unnecessary now).

---

## Follow-Up TODOs

1. Decide whether to keep `gradle.beforeProject { ... }` in `settings.gradle.kts`.  
   It may no longer be needed now that repo content filtering is fixed.
2. Consider reverting warning-causing `loom.mixin` blocks later if desired.  
   Build currently succeeds; warnings are informational.

---

## Log (Chronological, Command-Level)

### Feb 5, 2026 - Clean up settings
**Goal:** Remove no-longer-needed NeoForge repo injection hook in `settings.gradle.kts`.

**Commands Run:**
- `Get-Content settings.gradle.kts`
  - Confirmed the temporary `gradle.beforeProject` hook that injected the NeoForge repo.

**Changes Made:**
- `settings.gradle.kts`
  - Removed the `gradle.beforeProject { ... }` block that added the NeoForge repo.
  - Rationale: repo content filtering in `DependencyConfig.kt` now routes `net.neoforged*` correctly, so the extra hook is unnecessary.

**Result:** No build run after this removal yet (expected to be safe cleanup).

### Feb 5, 2026 - Verify build after cleanup
**Goal:** Ensure NeoForge compile still succeeds after removing the settings hook.

**Commands Run:**
- `./gradlew :platforms:neoforge:compileJava`
  - Result: **BUILD SUCCESSFUL** (6s)
  - Notes: warnings about deprecated Gradle APIs and Loom mixin AP remain, but compilation completes.

### Feb 5, 2026 - Attempted NeoForge runServer
**Goal:** Verify server runtime in NeoForge dev environment.

**Commands Run:**
- `./gradlew :platforms:neoforge:runServer`

**Key Output (trimmed):**
- Mod list included `Terra 7.0.0-BETA+4d962e760`.
- Server failed during mod loading:
  - `net.neoforged.fml.ModLoadingException: Terra (terra) has failed to load correctly`
  - `java.lang.ExceptionInInitializerError`
- Afterwards: `FatalStartupException: Couldn't find Minecraft server thread. Startup likely failed.`

**Result:** `runServer` failed. Needs deeper stacktrace for `ExceptionInInitializerError` to identify failing static init.

### Feb 5, 2026 - Build distributable NeoForge jar
**Goal:** Produce a remapped jar for user testing.

**Commands Run:**
- `./gradlew :platforms:neoforge:remapJar`
  - Result: **BUILD SUCCESSFUL** (24s)

**Artifacts Produced:**
- `platforms/neoforge/build/libs/Terra-neoforge-7.0.0-BETA+4d962e760.jar`
- `platforms/neoforge/build/libs/Terra-neoforge-7.0.0-BETA+4d962e760-shaded.jar`

**Notes:**
- Use the non-`-shaded` jar first unless you specifically need the shaded one for dependency bundling.

### Feb 5, 2026 - Fix JPMS module conflict (errorprone annotations)
**Goal:** Address user runtime error: `Modules terra and com.google.errorprone.annotations export package com.google.errorprone.annotations.concurrent to module mixinextras.neoforge`.

**Root Cause:** The NeoForge remapped jar was built from `shadowJar` and included `com/google/errorprone/annotations/**`, which conflicts with the `com.google.errorprone.annotations` module on NeoForge‚Äôs module path.

**Changes Made:**
- `buildSrc/src/main/kotlin/DistributionConfig.kt`
  - Added `exclude("com/google/errorprone/**")` to the `shadowJar` config to avoid bundling those annotations.

**Commands Run:**
- `jar tf platforms/neoforge/build/libs/Terra-neoforge-7.0.0-BETA+4d962e760.jar | Select-String com/google/errorprone/annotations`
  - Confirmed annotations were bundled prior to the fix.
- `./gradlew :platforms:neoforge:remapJar`
  - Result: **BUILD SUCCESSFUL** (1m 18s)
- `jar tf platforms/neoforge/build/libs/Terra-neoforge-7.0.0-BETA+4d962e760.jar | Select-String com/google/errorprone/annotations`
  - Confirmed **no** errorprone annotations after rebuild.

**Artifacts Produced (updated):**
- `platforms/neoforge/build/libs/Terra-neoforge-7.0.0-BETA+4d962e760.jar` (now without errorprone annotations)
- `platforms/neoforge/build/libs/Terra-neoforge-7.0.0-BETA+4d962e760-shaded.jar`

### Feb 5, 2026 - Attempt to retarget Minecraft 1.21.1
**Goal:** Align build with user request to target Minecraft 1.21.1 / NeoForge 21.1.x.

**Changes Made:**
- `buildSrc/src/main/kotlin/Versions.kt`
  - `Mod.minecraft = "1.21.1"`
  - `NeoForge.neoforge = "21.1.219"`
  - `Fabric.fabricAPI = "0.116.8+${Mod.minecraft}"`
  - Reverted `Bukkit.minecraft` back to `1.21.10` after a dev-bundle resolution failure.

**Commands Run:**
- `./gradlew :platforms:neoforge:remapJar`
  - Failed due to Fabric API mismatch (0.134.1+1.21.1 not found), fixed by updating `Fabric.fabricAPI`.
- `./gradlew :platforms:neoforge:remapJar` (after Fabric API fix)
  - Failed in `:platforms:mixin-common:compileJava` with missing Minecraft classes (API changes):
    - `ChunkLoadProgress`, `PalettesFactory`, `MergedComponentMap`, `Registry.PendingTagLoad`, `TagGroupLoader.RegistryTags`, etc.

**Conclusion:** The codebase currently targets newer Minecraft APIs (1.21.10). Retargeting to 1.21.1 requires **source-level backports** in mixins and utility code. No NeoForge jar was produced for 1.21.1 yet.

### Feb 5, 2026 - User request: switch to Terra `dev/1.21.1` branch
**Request:** Use the 1.21.1 version of Terra from the `dev/1.21.1` branch.
**Status:** Pending decision on workflow:
- Option A (recommended): create a separate git worktree for `dev/1.21.1` to avoid losing current changes.
- Option B: stash/reset current changes and checkout the branch directly in-place.

---

## Merged From C:\\Terra\\Terra-1.21.1

# Codex Notes - NeoForge (Terra dev/1.21.1 worktree)

**Date**: February 5, 2026  
**Worktree**: `C:\Terra\Terra-1.21.1` (detached HEAD at `upstream/dev/1.21.1`)  
**Goal**: Port NeoForge support to Terra `dev/1.21.1` for Minecraft 1.21.1 and build a test jar.

---

## Context / Reason for New Worktree
User requested the `dev/1.21.1` branch from upstream (`https://github.com/PolyhedralDev/Terra/tree/dev/1.21.1`).
The existing workspace (`C:\Terra\Terra`) has local NeoForge work aimed at 1.21.10 and cannot compile against 1.21.1 APIs.
We created a separate worktree to avoid losing current changes.

---

## Work Performed (So Far)

### Feb 5, 2026 - Create worktree + copy NeoForge module
**Commands Run:**
- `git fetch upstream`
- `git worktree add C:\Terra\Terra-1.21.1 upstream/dev/1.21.1`
- `Copy-Item -Recurse -Force C:\Terra\Terra\platforms\neoforge C:\Terra\Terra-1.21.1\platforms\neoforge`
- `Copy-Item -Force C:\Terra\Terra\platforms\neoforge\gradle.properties C:\Terra\Terra-1.21.1\platforms\neoforge\gradle.properties`

**Notes:**
- The upstream `dev/1.21.1` branch does NOT include a `platforms/neoforge` module, so it had to be copied in.

### Feb 5, 2026 - Version + repo wiring for NeoForge
**Files edited:**
- `buildSrc/src/main/kotlin/Versions.kt`
  - Added `Versions.Mod.yarnMappingsPatchNeoForge = "1.21+build.6"`
  - Added `object NeoForge { neoforge = "21.1.219" }`
- `buildSrc/src/main/kotlin/DependencyConfig.kt`
  - Added NeoForge Maven repo with content filter `net.neoforged*`
- `buildSrc/src/main/kotlin/DistributionConfig.kt`
  - Excluded `com/google/errorprone/**` from `shadowJar` to avoid JPMS conflicts.

---

## Next Steps
1. Verify `platforms/neoforge` compiles under 1.21.1 branch.
2. Build a new NeoForge jar:
   - `./gradlew :platforms:neoforge:remapJar`
3. Hand the jar path to the user for testing.

---

## Known Risks
- NeoForge module was copied from a 1.21.10-based workspace and may reference newer APIs.
- If build fails, expect missing/changed MC classes. Backports may be needed.

---

## Build Attempt Log

### Feb 5, 2026 - First remapJar attempt (failed)
**Command:** `./gradlew :platforms:neoforge:remapJar`  
**Result:** Failed due to `yarn-mappings-patch-neoforge` resolution.

**Error (trimmed):**
- `Could not resolve dev.architectury:yarn-mappings-patch-neoforge:1.21+build.6`
- Tried to fetch from Sonatype snapshots and got `504 Gateway Time-out`.

**Fix Applied:**
- Added Architectury Maven repo to `buildSrc/src/main/kotlin/DependencyConfig.kt`.
- Disabled Sonatype snapshots repo (commented) to avoid 504.

### Feb 5, 2026 - Loom patch failure + attempt to upgrade Loom
**Command:** `./gradlew :platforms:neoforge:remapJar`  
**Result:** Failed during NeoForge jar patching.

**Error (trimmed):**
- `Error: Could not find or load main class net.minecraftforge.binarypatcher.ConsoleTool`

**Action Taken:**
- Updated `Versions.Mod.architecuryLoom` to `1.13-SNAPSHOT` in `buildSrc/src/main/kotlin/Versions.kt`.
- Updated `Versions.Mod.architecturyPlugin` to `3.4.162` (to align with newer Loom).

**New Failure After Loom Upgrade:**
- Build now fails in `:platforms:fabric` configuration with:
  - `'java.lang.String org.gradle.api.artifacts.ProjectDependency.getPath()'`
  - Indicates a compatibility issue between the newer Loom and this older 1.21.1 branch build scripts.

**Status:** NeoForge jar still not produced for the 1.21.1 worktree due to Loom/plugin compatibility issues.

### Feb 5, 2026 - Limit build to NeoForge + update Gradle
**Goal:** Avoid Fabric configuration and fix Loom compatibility by updating Gradle.

**Changes Made:**
- `settings.gradle.kts`
  - Limited platform includes to `:platforms:mixin-common`, `:platforms:mixin-lifecycle`, `:platforms:neoforge`.
- `build.gradle.kts`
  - Guarded `:platforms:bukkit:common` access with `findProject(...)` to avoid missing project errors.
- `gradle/wrapper/gradle-wrapper.properties`
  - Updated Gradle wrapper to **8.14.1** (matching main workspace).

**Reasoning:**
- Loom 1.13 requires newer Gradle APIs (error: `ProjectDependency.getPath()` missing on Gradle 8.10.1).

**Next Step:**
- Re-run `./gradlew :platforms:neoforge:remapJar` after wrapper update.

### Feb 5, 2026 - Build blocked by Loom cache lock / daemon crash
**Command:** `./gradlew :platforms:neoforge:remapJar`  
**Result:** Failed waiting on Loom cache lock (`C:\Users\anelson\.gradle\caches\fabric-loom`) held by pid `38024`; daemon later disappeared.

**Cleanup Commands:**
- `./gradlew --stop`
- `Stop-Process -Id 38024 -Force`

**Status:** Ready to retry `remapJar`.

### Feb 5, 2026 - Missing transformProductionNeoForge variants
**Command:** `./gradlew :platforms:neoforge:remapJar`  
**Result:** Failed: `transformProductionNeoForge` configuration missing in `:platforms:mixin-common` and `:platforms:mixin-lifecycle`.

**Investigation:**
- Ran `./gradlew :platforms:mixin-common:outgoingVariants`
  - Confirmed no `transformProductionNeoForge` variant exists.

**Fix Applied:**
- `platforms/neoforge/build.gradle.kts`
  - Added fallback to use `shadowRuntimeElements` when `transformProductionNeoForge` is absent:
    - `val mixinCommonProduction = ... ?: "shadowRuntimeElements"`
    - `val mixinLifecycleProduction = ... ?: "shadowRuntimeElements"`

**Follow-up Fix:**
- Moved `mixinCommonProduction` / `mixinLifecycleProduction` definitions **outside** the `dependencies {}` block to avoid Kotlin DSL `Unresolved reference: configurations` errors.

### Feb 5, 2026 - Mixin AP strictness errors
**Command:** `./gradlew :platforms:neoforge:remapJar`  
**Result:** Failed in `:platforms:mixin-common:compileJava` with Mixin AP errors like:
- `Unable to locate obfuscation mapping for @Inject target getStructurePosition`
- `... getFlowerFeatures`
- `... refresh`

**Attempted Fix:**
- Enabled legacy Mixin AP in both mixin modules:
  - `platforms/mixin-common/build.gradle.kts` ‚Üí `useLegacyMixinAp.set(true)`
  - `platforms/mixin-lifecycle/build.gradle.kts` ‚Üí `useLegacyMixinAp.set(true)`

### Feb 5, 2026 - Successful 1.21.1 NeoForge build
**Command:** `./gradlew :platforms:neoforge:remapJar`  
**Result:** **BUILD SUCCESSFUL** (warnings only).

**Artifacts (1.21.1 branch):**
- `C:/Terra/Terra-1.21.1/platforms/neoforge/build/libs/Terra-neoforge-6.5.0-BETA+15a298304.jar`
- `C:/Terra/Terra-1.21.1/platforms/neoforge/build/libs/Terra-neoforge-6.5.0-BETA+15a298304-shaded.jar`

**Note:** These are the 1.21.1-targeted jars; older 7.0.0 jars in this folder are from previous work and should be ignored.

### Feb 5, 2026 - Fix Checker Framework JPMS conflict
**Issue from user test:**
- `java.lang.module.ResolutionException: Module terra contains package org.checkerframework.checker.i18nformatter.qual`

**Fix Applied:**
- `buildSrc/src/main/kotlin/DistributionConfig.kt`
  - Added `exclude("org/checkerframework/**")` to `shadowJar` to prevent bundling Checker Framework annotations.

**Command:** `./gradlew :platforms:neoforge:remapJar`  
**Result:** **BUILD SUCCESSFUL** (warnings only)

**Artifacts (updated, 1.21.1 branch):**
- `C:/Terra/Terra-1.21.1/platforms/neoforge/build/libs/Terra-neoforge-6.5.0-BETA+15a298304.jar`
- `C:/Terra/Terra-1.21.1/platforms/neoforge/build/libs/Terra-neoforge-6.5.0-BETA+15a298304-shaded.jar`

**Verification:**
- `jar tf ... | Select-String org/checkerframework/` ‚Üí no matches

### Feb 5, 2026 - Fix mixin configs not loading (ClassCastException)
**Issue from user test:**
- `ClassCastException: net.minecraft.world.level.block.state.BlockState cannot be cast to com.dfsek.terra.api.block.state.BlockState`
  - Indicates mixin that implements `BlockState` on MC BlockState was not applied.

**Fix Applied:**
- Added mixin configs to NeoForge mod metadata:
  - `platforms/neoforge/src/main/resources/META-INF/neoforge.mods.toml`
    - `[[mixins]] config = "terra.common.mixins.json"`
    - `[[mixins]] config = "terra.lifecycle.mixins.json"`
    - `[[mixins]] config = "terra.neoforge.mixins.json"`

**Command:** `./gradlew :platforms:neoforge:remapJar`  
**Result:** **BUILD SUCCESSFUL**

**Artifacts (updated):**
- `C:/Terra/Terra-1.21.1/platforms/neoforge/build/libs/Terra-neoforge-6.5.0-BETA+15a298304.jar`
- `C:/Terra/Terra-1.21.1/platforms/neoforge/build/libs/Terra-neoforge-6.5.0-BETA+15a298304-shaded.jar`

**Verification:**
- `jar tf ... | Select-String terra.common.mixins.json` ‚Üí present


---

## Feb 5, 2026 - Moved 1.21.1 worktree back into C:\Terra\Terra

**Request:** User asked to consolidate and remove `C:\Terra\Terra-1.21.1`, returning the 1.21.1 workspace to `C:\Terra\Terra`.

**Actions taken:**
- Merged notes from `C:\Terra\Terra` and `C:\Terra\Terra-1.21.1` into this file.
- Checked out `upstream/dev/1.21.1` in `C:\Terra\Terra` (detached HEAD).
- Mirrored files from `C:\Terra\Terra-1.21.1` into `C:\Terra\Terra` (excluded `.git`).
- Removed the secondary worktree and deleted `C:\Terra\Terra-1.21.1`.

**Current workspace:** `C:\Terra\Terra` is now the only worktree and contains the 1.21.1 NeoForge changes.

---

## 2026-02-04 22:14 PST ‚Äî Runtime Mixin Failures Investigation

### Context
- Minecraft: 1.21.1
- Terra branch: dev/1.21.1
- Loader target: NeoForge 21.1.219
- Architectury Loom: 1.13-SNAPSHOT (newer than template's 1.11-SNAPSHOT)
- Environment: User testing built jar `Terra-neoforge-6.5.0-BETA+15a298304.jar`

### Goal
Fix runtime mixin failures preventing Terra from loading in NeoForge.

### Hypothesis
Refmaps generated during mixin module compilation contain only Yarn intermediary mappings (`named:intermediary`), but NeoForge runtime expects Mojang-mapped class/method names. Loom's remapJar task should remap these refmaps when building the final jar, but it's not happening for shaded mixin modules.

### Actions Taken

1. **Analyzed runtime crash logs** (`latest.log` and `debug.log`):
   - **Error 1**: `RegistryMixin` - `@Inject annotation on registerTerraGenerators specifies a target class 'net/minecraft/class_7923'`
     - The refmap references `class_7923` (Yarn intermediary name) but NeoForge runtime uses Mojang mappings
   - **Error 2**: `EntityMixin` - `@Shadow method updatePosition was not located in the target class net.minecraft.world.entity.Entity`
     - Method name not found, likely because refmap isn't translating Yarn names to Mojang names

2. **Examined source files**:
   - `RegistryMixin.java`: Targets `@Mixin(Registries.class)` using Yarn imports
   - `EntityMixin.java`: Uses Yarn imports like `net.minecraft.entity.Entity`
   - This is correct for compilation with Yarn mappings

3. **Inspected generated refmaps**:
   - Extracted `terra.lifecycle.refmap.json` from build output and final jar
   - **Critical finding**: Refmaps only contain `"named:intermediary"` mapping data
   - Missing Mojang/SRG mappings needed for NeoForge runtime
   - Example from refmap:
     ```json
     "com/dfsek/terra/lifecycle/mixin/RegistryMixin": {
       "<clinit>": "Lnet/minecraft/class_7923;<clinit>()V"
     }
     ```
   - At runtime, Mixin tries to use `class_7923` but NeoForge has `net.minecraft.core.registries.BuiltInRegistries`

4. **Verified mixin module transformation**:
   - Checked `transformProductionNeoForge` jars from mixin modules
   - These also contain only Yarn intermediary refmaps
   - Architectury transformation doesn't add Mojang mappings to refmaps

5. **Compared against working examples**:
   - Examined `example_mod-1.21.1-neoforge-only-template`
   - Uses same Yarn + NeoForge patch layered mappings approach
   - Uses Loom 1.11-SNAPSHOT (Terra uses 1.13-SNAPSHOT)
   - Has `loom.platform=neoforge` (Terra has this too)
   - Template doesn't have multi-module mixin setup, so no shading issue

6. **Researched Architectury Loom documentation**:
   - Source: https://docs.architectury.dev/loom/mixins
   - Source: https://docs.architectury.dev/loom/fg_mixin_refmaps
   - Key finding: "Architectury Loom injects a remapper into your development environment to properly remap srg reference maps to named"
   - For Forge/NeoForge, mixin configs should be declared: `loom.forge.mixinConfig("...")`
   - But this is for Forge platform, and NeoForge platform doesn't support `loom.forge {}` block

7. **Attempted fixes** (all reverted):
   - **Attempt 1**: Added `loom.forge { mixinConfig(...) }` - **FAILED**: "Loom is not running on Forge" error
   - **Attempt 2**: Added `mixin.add("main", "...")` - **FAILED**: "sourceSet main configured for mixin AP multiple times"
   - **Attempt 3**: Added refmap remapping properties to run configs - **REVERTED**: These only affect dev environment, not distributed jar

### Files Touched
- None (all attempts reverted)

### Commands Run
```bash
# Check dependencies
./gradlew :platforms:neoforge:dependencies --configuration compileClasspath

# Extract and examine refmaps
jar tf platforms/neoforge/build/libs/Terra-neoforge-6.5.0-BETA+15a298304.jar | grep refmap
jar xf platforms/neoforge/build/libs/Terra-neoforge-6.5.0-BETA+15a298304.jar terra.lifecycle.refmap.json

# Check mixin module outputs
jar xf platforms/mixin-lifecycle/build/libs/mixin-lifecycle-6.5.0-BETA+15a298304-transformProductionNeoForge.jar terra.lifecycle.refmap.json

# Attempted rebuilds (all failed/reverted)
./gradlew :platforms:neoforge:clean :platforms:neoforge:remapJar
```

### Current Status
**BLOCKED**: Unable to find correct configuration to make Loom remap shaded mixin refmaps.

**Root Cause Confirmed**:
- Mixin modules compile with Yarn mappings ‚Üí refmaps contain only `named:intermediary`
- Mixin modules are shaded into NeoForge shadowJar
- Loom's remapJar task should add/remap to Mojang mappings, but doesn't for shaded refmaps
- At runtime, Mixin can't find injection points because it's looking for Yarn intermediary names in a Mojang-mapped environment

**Possible Solutions Still to Investigate**:
1. Downgrade Loom from 1.13-SNAPSHOT to 1.11-SNAPSHOT (match example template)
2. Check Loom 1.13 changelog/issues for refmap remapping bugs
3. Configure remapJar task to explicitly handle shaded refmaps
4. Use a different mixin shading strategy (include mixin source in NeoForge module instead of shading compiled modules)

---

## 2026-02-04 23:30 PST ‚Äî Solution: Exclude Refmaps from NeoForge Jar

### Context
Continued investigation from previous entry.

### Breakthrough Finding
**Source**: https://github.com/Sinytra/Connector/discussions/383 - "The state of Mixins on (Neo)Forge"

**Key Quote**: "NeoForge has removed the need for refmaps due to standardizing mappings. If you build your mod and don't find a refmap in the jar, that's normal and it will work in production."

**Explanation**: Unlike Fabric which uses Yarn mappings at runtime (requiring refmaps to translate from intermediary), NeoForge uses Mojang mappings consistently. Since the source code is compiled with Yarn‚ÜíMojang layered mappings patch, and runtime also uses Mojang mappings, Mixin can resolve injection points at runtime without needing a refmap translation layer.

### Solution Applied
Configured the NeoForge shadowJar task to exclude refmap files entirely.

### Actions Taken
1. Edited `platforms/neoforge/build.gradle.kts`:
   - Added `exclude("*.refmap.json")` to shadowJar task configuration
   - This removes refmaps from the final jar
   - Mixin will fall back to runtime resolution using Mojang mappings

2. Rebuilt jar:
   - `./gradlew :platforms:neoforge:clean :platforms:neoforge:remapJar`
   - Build successful

3. Verified refmaps are excluded:
   - `jar tf Terra-neoforge-6.5.0-BETA+15a298304.jar | grep refmap` ‚Üí no results
   - Confirmed refmaps are NOT in the final jar

### Files Touched
- `platforms/neoforge/build.gradle.kts` - Added refmap exclusion to shadowJar task

### Commands Run
```bash
./gradlew :platforms:neoforge:clean :platforms:neoforge:remapJar
jar tf platforms/neoforge/build/libs/Terra-neoforge-6.5.0-BETA+15a298304.jar | grep refmap
```

### Result
**Status**: Fix applied, awaiting user testing

**Artifact for Testing**:
- `platforms/neoforge/build/libs/Terra-neoforge-6.5.0-BETA+15a298304.jar`

**Expected Outcome**: Mixins should now load correctly at runtime because:
1. No refmap files in jar
2. Mixin uses runtime resolution
3. Mojang mappings used consistently (layered Yarn patch compiles to Mojang, runtime uses Mojang)
4. Injection targets should be found correctly

**Test Result**: FAILED - New errors appeared:
- `SimpleRegistryMixin` - `@Shadow field valueToEntry was not located`
- `EntityMixin` - `@Shadow method updatePosition` not found
- Errors changed from "Yarn intermediary class not supported" to "No refMap loaded"
- Root cause: Mixin source code uses Yarn names but runtime expects Mojang names
- Removing refmaps prevented translation, breaking all @Shadow declarations

---

## 2026-02-04 23:45 PST ‚Äî Investigation: How Do Working Examples Handle This?

### Context
Previous fix (excluding refmaps) didn't work. Examined reference examples to understand correct approach for NeoForge 1.21.1.

### Key Finding: Liteminer Uses Platform-Specific Mixins

**Examined**: `C:\Terra\Terra\Examples\liteminer-1.21.1`

**Discovery**: Liteminer has **separate mixin source files** for each platform:
- `common/src/main/java/.../mixin/PlayerMixin.java` - Uses Yarn mappings
- `neoforge/src/main/java/.../neoforge/mixin/PlayerMixin.java` - Uses Mojang mappings

**Example Comparison**:

Common (Yarn):
```java
@Inject(method = "getDestroySpeed", at = @At("TAIL"), cancellable = true)
public void getDestroySpeed(BlockState state, CallbackInfoReturnable<Float> cir)
```

NeoForge (Mojang):
```java
@Inject(method = "getDigSpeed", at = @At("TAIL"), cancellable = true)
public void liteminer$getDestroySpeed(BlockState state, BlockPos pos, CallbackInfoReturnable<Float> cir)
```

**Differences**:
1. Method name: `getDestroySpeed` (Yarn) vs `getDigSpeed` (Mojang)
2. Method signature differs (extra BlockPos parameter in Mojang)
3. **Neither mixin config references a refmap**

**Conclusion**: For NeoForge 1.21.1, the working pattern is:
- Write platform-specific mixins with Mojang mappings in the NeoForge module
- Do NOT rely on refmap remapping from Yarn‚ÜíMojang
- No refmap needed when source uses correct mappings for runtime

### Critical Discovery: Loom Version Mismatch

**Official template** (`example_mod-1.21.1-neoforge-only-template`):
- Loom: **1.11-SNAPSHOT**
- NeoForge: 21.1.215
- MC: 1.21.1

**Terra current state**:
- Loom: **1.13-SNAPSHOT**
- NeoForge: 21.1.219
- MC: 1.21.1

**Why Terra uses 1.13**: According to earlier notes, Codex upgraded Loom to 1.13-SNAPSHOT to fix `data/server.lzma` error when targeting MC **1.21.10** with NeoForge 21.10.64. That issue does NOT apply to 1.21.1!

**Implication**: Terra is using a newer, beta Loom version unnecessarily. The official 1.21.1 template proves 1.11-SNAPSHOT works correctly for this MC version. Loom 1.13 may have refmap remapping bugs or different behavior for shaded modules.

### Decision: Downgrade Loom to Match Working Template

**Action**: Downgrade to Loom 1.11-SNAPSHOT to match the proven-working template configuration.

**Rationale**:
1. Official template for 1.21.1 uses Loom 1.11 successfully
2. Loom 1.13 upgrade was for a different MC version (1.21.10)
3. Loom 1.11 may have better/different refmap remapping for shaded modules
4. Eliminates a variable - test with known-working Loom version first

---

## 2026-02-04 23:50 PST ‚Äî Downgrade Loom to 1.11-SNAPSHOT

### Goal
Test if Loom 1.11-SNAPSHOT properly remaps shaded mixin refmaps for NeoForge 1.21.1.

### Hypothesis
Loom 1.11 may handle refmap remapping differently than 1.13-SNAPSHOT, potentially fixing the Yarn‚ÜíMojang translation issue.

### Actions Taken
1. Reverted refmap exclusion in `platforms/neoforge/build.gradle.kts`
   - Removed `exclude("*.refmap.json")` from shadowJar
   - Want refmaps present to test if Loom 1.11 remaps them correctly

2. Downgraded Loom version in `buildSrc/src/main/kotlin/Versions.kt`
   - Changed `architecuryLoom` from "1.13-SNAPSHOT" to "1.11-SNAPSHOT"

3. Rebuilt and checked refmaps
   - Build successful
   - Refmap inspection: Still contains ONLY `"named:intermediary"` mappings
   - **NO Mojang/SRG mappings added**

### Files Touched
- `platforms/neoforge/build.gradle.kts` - Removed shadowJar refmap exclusion
- `buildSrc/src/main/kotlin/Versions.kt` - Downgraded Loom to 1.11-SNAPSHOT

### Commands Run
```bash
./gradlew :platforms:neoforge:clean :platforms:neoforge:remapJar
jar xf platforms/neoforge/build/libs/Terra-neoforge-6.5.0-BETA+15a298304.jar terra.lifecycle.refmap.json
```

### Result
**Status**: FAILED - Loom 1.11 has the same refmap issue as Loom 1.13

**Conclusion**: Neither Loom 1.11 nor 1.13 properly remaps shaded mixin refmaps for NeoForge. This appears to be a fundamental architectural limitation: mixin modules compile independently with Yarn mappings and generate Yarn-only refmaps. When shaded into the NeoForge jar, Loom's remapJar task does NOT add/convert these refmaps to Mojang mappings.

---

## 2026-02-05 00:00 PST ‚Äî Path Forward: Options Analysis

### Confirmed Problem
Architectury Loom (both 1.11 and 1.13) does not remap refmaps from shaded mixin modules when building NeoForge jars. The refmaps remain Yarn-only (`named:intermediary`), causing runtime failures when Mixin tries to use Yarn names in a Mojang-mapped NeoForge environment.

### Working Pattern (Liteminer Reference)
Liteminer avoids this issue by:
- Writing separate mixin source files for each platform
- NeoForge mixins use Mojang mappings directly
- No refmap needed (or refmap uses Mojang names)
- No reliance on Loom refmap remapping

### Available Options

**Option 1: Platform-Specific Mixins (Proven)**
- Create NeoForge-specific copies of all mixins in `platforms/neoforge/src/main/java`
- Write them using Mojang mappings (e.g., `getDigSpeed` instead of `getDestroySpeed`)
- Remove dependency on common mixin modules for NeoForge
- **Pros**: Proven to work (liteminer), no Loom dependencies
- **Cons**: Code duplication, maintenance burden, mapping research needed

**Option 2: Report Loom Bug + Temporary Workaround**
- File issue with Architectury Loom about refmap remapping for shaded modules
- Use Option 1 as temporary workaround until fixed
- **Pros**: Long-term fix, helps community
- **Cons**: Uncertain timeline, still need workaround now

**Option 3: Restructure Build**
- Move mixin source into NeoForge module instead of shading
- Compile mixins directly in NeoForge context with layered mappings
- Let Loom generate refmaps in NeoForge module
- **Pros**: Might work with current Loom, cleaner than shading
- **Cons**: Requires significant build refactor, breaks Fabric/common pattern

**Recommendation**: Option 1 (with Option 2 follow-up)
- Fastest path to working NeoForge support
- Proven pattern from real-world mod
- Can file Loom bug report after getting NeoForge working

---

## 2026-02-05 00:30 PST ‚Äî SOLUTION FOUND: Yarn Mappings for NeoForge Mixins

### Critical Discovery
After analyzing liteminer's build configuration, discovered the key difference:
- **Liteminer**: Uses Parchment/Mojang mappings at compile time
- **Terra**: Uses Yarn mappings + yarn-mappings-patch-neoforge at compile time

### The Mistake
Initially created NeoForge mixins using Mojang class/method names:
- `net.minecraft.core.registries.BuiltInRegistries`
- `net.minecraft.world.entity.Entity`
- `moveTo()`, `bindValue()`, etc.

**Result**: Compilation failed because Terra's NeoForge module compiles with Yarn mappings, so Mojang names don't exist at compile time.

### The Solution
NeoForge mixins must be written using **Yarn names** (not Mojang names):
- `net.minecraft.registry.Registries` (not BuiltInRegistries)
- `net.minecraft.entity.Entity` (not world.entity.Entity)
- `updatePosition()` (not moveTo())
- `setValue()` (not bindValue())
- `valueToEntry` (not byValue)
- `blockPos` (not blockPosition)
- `world` (not level)

### Why This Works
1. Source code uses Yarn names (available at compile time)
2. Loom generates refmaps for mixins in NeoForge module
3. **Key**: Refmaps generated in NeoForge module include Mojang translations
4. At runtime, Mixin uses refmaps to translate Yarn ‚Üí Mojang

### Verification
Checked generated refmap at `terra.neoforge.refmap.json`:
```json
{
  "mappings": {
    "com/dfsek/terra/neoforge/mixin/RegistryMixin": {
      "<clinit>": "Lnet/minecraft/core/registries/BuiltInRegistries;<clinit>()V"
    },
    "com/dfsek/terra/neoforge/mixin/RegistryEntryReferenceInvoker": {
      "setValue": "bindValue(Ljava/lang/Object;)V"
    }
  }
}
```

**Success**: Refmap contains Mojang class names (`BuiltInRegistries`) and method names (`bindValue`), exactly what NeoForge runtime needs!

### Implementation Status
**Completed**:
- ‚úÖ RegistryMixin.java (Yarn: `Registries` ‚Üí Mojang: `BuiltInRegistries`)
- ‚úÖ SimpleRegistryMixin.java (Yarn: `SimpleRegistry`, `valueToEntry` ‚Üí Mojang: `MappedRegistry`, `byValue`)
- ‚úÖ EntityMixin.java (Yarn: `Entity`, `world`, `blockPos`, `updatePosition()` ‚Üí Mojang: `Entity`, `level`, `blockPosition`, `moveTo()`)
- ‚úÖ RegistryEntryReferenceInvoker.java (Yarn: `setValue` ‚Üí Mojang: `bindValue`)

**Build**: SUCCESS ‚úÖ
- Jar built successfully: `Terra-neoforge-6.5.0-BETA+15a298304.jar`
- Copied to test instance for runtime verification

### Next Steps
1. Test runtime - verify mixins apply correctly with Mojang mappings
2. Convert remaining 36 mixins from common/lifecycle modules
3. Update ai_notes.md with final results

---


## 2026-02-04 23:35 PST ‚Äî Runtime Test #1: ClassNotFoundException

### Issue
First runtime test crashed with missing utility classes:
- `com.dfsek.terra.lifecycle.util.RegistryUtil` - ClassNotFoundException
- `com.dfsek.terra.mod.util.MinecraftAdapter` - ClassNotFoundException

### Root Cause
Changed mixin module dependencies from `shadedApi` to `compileOnly` in build.gradle.kts.
This prevented utility classes from being included in the final jar.

### Fix
Reverted dependencies back to `shadedApi`:
```kotlin
// Shade the modules to include utility classes, but don't load their mixin configs
shadedApi(project(path = ":platforms:mixin-common", configuration = "namedElements"))
shadedApi(project(path = ":platforms:mixin-lifecycle", configuration = "namedElements"))
```

**Key Insight**: Mixin modules must be shaded to include utility classes, but their mixin configs are NOT loaded (already removed from neoforge.mods.toml).

---

## 2026-02-04 23:36 PST ‚Äî Runtime Test #2: Refmap Not Being Applied

### Issue
Second runtime test crashed with:
```
InvalidAccessorException: No candidates were found matching setValue(Ljava/lang/Object;)V 
in net/minecraft/core/Holder$Reference
```

Mixin tried to find Yarn method name `setValue` in Mojang class `Holder.Reference`, but actual method is `bindValue`.

### Root Cause Analysis
- Refmap correctly maps `setValue` ‚Üí `bindValue`
- BUT: Multiple refmap files exist in jar:
  - `terra.common.refmap.json` (Yarn intermediary mappings)
  - `terra.lifecycle.refmap.json` (Yarn intermediary mappings)  
  - `terra.neoforge.refmap.json` (Mojang mappings) ‚úÖ
- Mixin config `terra.neoforge.mixins.json` did NOT specify which refmap to use
- Possible refmap confusion/conflict

### Fix Attempt
Added explicit refmap reference to terra.neoforge.mixins.json:
```json
{
  "required": true,
  "refmap": "terra.neoforge.refmap.json",
  ...
}
```

**Status**: Testing now...

---


## 2026-02-04 23:39 PST ‚Äî Runtime Test #3: ClassCastException + Mixin Conflict

### Issue
Third runtime test crashed with ClassCastException:
```
ClassCastException: class net.minecraft.world.level.block.state.BlockState 
cannot be cast to class com.dfsek.terra.api.block.state.BlockState
at MinecraftWorldHandle.<clinit>(MinecraftWorldHandle.java:37)
```

### Root Cause
When shading mixin-common and mixin-lifecycle modules to get utility classes, we also included ALL their mixin classes:
- `com/dfsek/terra/mod/mixin/**/*.class` (from mixin-common)
- `com/dfsek/terra/lifecycle/mixin/**/*.class` (from mixin-lifecycle)
- `terra.common.refmap.json` and `terra.lifecycle.refmap.json`

Even though these mixin configs were removed from neoforge.mods.toml, the mixin classes were still in the jar and likely causing conflicts or being auto-discovered.

### Fix
Added exclusions to shadowJar task in build.gradle.kts:
```kotlin
shadowJar {
    // Exclude common/lifecycle mixin classes and their refmaps
    exclude("com/dfsek/terra/mod/mixin/**")
    exclude("com/dfsek/terra/lifecycle/mixin/**")
    exclude("terra.common.mixins.json")
    exclude("terra.lifecycle.mixins.json")  
    exclude("terra.common.refmap.json")
    exclude("terra.lifecycle.refmap.json")
}
```

### Verification
After rebuild, jar now only contains:
- ‚úÖ NeoForge mixin classes (EntityMixin, RegistryMixin, SimpleRegistryMixin, RegistryEntryReferenceInvoker)
- ‚úÖ Only terra.neoforge.refmap.json
- ‚úÖ Utility classes from common/lifecycle modules (RegistryUtil, MinecraftAdapter, RegistryHack, etc.)
- ‚ùå NO common/lifecycle mixin classes
- ‚ùå NO common/lifecycle refmaps

**Status**: Testing now...

---


## 2026-02-04 23:43 PST ‚Äî Runtime Test #4: Missing BlockStateMixin

### Issue
Fourth runtime test crashed with same ClassCastException:
```
ClassCastException: class net.minecraft.world.level.block.state.BlockState 
cannot be cast to class com.dfsek.terra.api.block.state.BlockState
```

### Root Cause Analysis
The error occurs in Terra's own code, not mixin loading. Investigating found:
- `BlockStateMixin` exists in mixin-common (implements Terra API on Minecraft's BlockState)
- We excluded ALL common/lifecycle mixins to avoid refmap conflicts
- Without BlockStateMixin, Minecraft's BlockState doesn't implement Terra's interface
- Terra's code at MinecraftWorldHandle.java:37 tries to cast and fails

### Scope of Work
Total mixins needing NeoForge versions:
- **32** common mixins
- **7** lifecycle mixins  
- **39 total** mixins
- **4 completed** (RegistryMixin, SimpleRegistryMixin, EntityMixin, RegistryEntryReferenceInvoker)
- **35 remaining** to convert

### Critical Mixins Required
Not just the 4 we created - need many more including:
- `BlockStateMixin` - Makes BlockState implement Terra API (CRITICAL for basic functionality)
- `BlockMixin` - Block interface implementation
- `BiomeMixin` - Biome interface implementation
- All entity/block/world interface mixins
- Plus various accessor/invoker mixins

### Decision Point
**Cannot proceed with only 4 mixins.** Need to either:
1. Convert all 35 remaining mixins to NeoForge versions (significant work)
2. Find a way to make common/lifecycle refmaps work with Mojang mappings
3. Different architectural approach

---


## 2026-02-04 23:50 PST ‚Äî Refmap Investigation: Option 2 Approach

### Strategy
Instead of converting 35 mixins or trying to remap Yarn refmaps to Mojang, test if Mixin can work **without refmaps** for common/lifecycle mixins.

### Theory
- Refmaps are optimization caches for faster mixin resolution
- Without refmaps, Mixin uses the runtime obfuscation environment to resolve targets
- NeoForge runtime = Mojang mappings
- If Mixin can find Mojang names at runtime, mixins should work even without refmaps

### Implementation
1. **Removed refmap exclusions** from shadowJar (allow common/lifecycle mixin classes)
2. **Kept refmap exclusions** for terra.common.refmap.json and terra.lifecycle.refmap.json
3. **Restored mixin configs** in neoforge.mods.toml:
   - terra.common.mixins.json
   - terra.lifecycle.mixins.json
   - terra.neoforge.mixins.json (our 4 mixins with explicit refmap)

### Build Result
‚úÖ Build successful
‚úÖ BlockStateMixin.class present in jar
‚úÖ Only terra.neoforge.refmap.json in jar (common/lifecycle refmaps excluded)

**Status**: Testing if Mixin can resolve common/lifecycle mixins without refmaps...

---


## 2026-02-04 23:47 PST ‚Äî Refmap-less Approach: FAILED

### Result
Runtime test failed with multiple mixin errors:
```
@Shadow field valueToEntry was not located in net.minecraft.core.MappedRegistry. No refMap loaded.
@Shadow method updatePosition was not located in net.minecraft.world.entity.Entity. No refMap loaded.
```

### Why It Failed
- Mixins use Yarn names: `valueToEntry`, `updatePosition`, `world`, `blockPos`, etc.
- NeoForge runtime has Mojang names: `byValue`, `moveTo`, `level`, `blockPosition`, etc.
- **Refmaps are essential** for translating Yarn ‚Üí Mojang at runtime
- Without refmaps, Mixin searches for Yarn names in Mojang-mapped classes and fails

### Conclusion
**Option 2 (refmap investigation) is not viable** without significantly modifying Loom's behavior or writing custom refmap generation.

Refmaps are NOT optional optimization - they're **required** for cross-mapping-environment mixins.

---

## 2026-02-04 23:50 PST ‚Äî Path Forward: Option 1 (Convert All Mixins)

### Decision
Must convert all 35 remaining mixins to NeoForge versions using Yarn names (like we did for the first 4).

### Approach
Use the pattern established with RegistryMixin, SimpleRegistryMixin, EntityMixin, RegistryEntryReferenceInvoker:
1. Copy mixin from common/lifecycle to platforms/neoforge/src/main/java/com/dfsek/terra/neoforge/mixin/
2. Keep all class/method/field names as Yarn (what compiles)
3. Let Loom generate proper Mojang refmaps for NeoForge module
4. Add to terra.neoforge.mixins.json

### Remaining Work
**35 mixins** to convert from common (32) and lifecycle (7):
- Already completed: 4 (Registry, SimpleRegistry, Entity, RegistryEntryReference)
- Remaining lifecycle: 3 (NoiseConfig, MinecraftServer, RegistryLoader, SaveLoading)
- Remaining common: 28 (BlockState, Block, Biome, World, various implementations)

---


## 2026-02-05 00:12 -05:00 ó Create Snapshot (Git + Zip)

### Context
- Minecraft: 1.21.1
- Terra branch: dev/1.21.1
- Loader target: NeoForge
- Architectury Loom: required/present
- Environment: Windows 11, JDK 21, Gradle 8.14.1

### Goal
Create a Git snapshot (branch + tag) and a filesystem zip snapshot of the current project state.

### Hypothesis
A lightweight Git tag/branch plus a zip archive outside the repo will allow safe rollback or comparison without altering working files.

### Actions Taken
- Created Git tag and branch at current HEAD.
- Created `C:\Terra\Snapshots` folder.
- Compressed `C:\Terra\Terra` into a zip snapshot.
- Verified the zip file exists and size is stable after the initial timeout.

### Files Touched
- C:\Terra\Snapshots\Terra-snapshot-20260205-0012.zip ó created
- .git/refs/tags/snapshot-20260205-0012 ó created
- .git/refs/heads/snapshot-20260205-0012 ó created

### Commands Run
```text
Get-Date -Format "yyyy-MM-dd HH:mm zzz"
$stamp = Get-Date -Format "yyyyMMdd-HHmm"; $tag = "snapshot-$stamp"; $branch = "snapshot-$stamp"; git -C C:\Terra\Terra tag $tag; git -C C:\Terra\Terra branch $branch; $tag; $branch
New-Item -ItemType Directory -Force -Path C:\Terra\Snapshots
Compress-Archive -Path C:\Terra\Terra\* -DestinationPath C:\Terra\Snapshots\Terra-snapshot-20260205-0012.zip -Force
Get-Item -Path C:\Terra\Snapshots\Terra-snapshot-20260205-0012.zip -ErrorAction SilentlyContinue | Select-Object FullName,Length
Start-Sleep -Seconds 3; Get-Item -Path C:\Terra\Snapshots\Terra-snapshot-20260205-0012.zip | Select-Object FullName,Length
```

### Result
- Git snapshot created: tag and branch `snapshot-20260205-0012`.
- Zip snapshot created at `C:\Terra\Snapshots\Terra-snapshot-20260205-0012.zip` (size ~93,476,103 bytes).
- `Compress-Archive` timed out in this session, but the zip file exists and size did not change on recheck.

## 2026-02-05 00:14 -05:00 ó Read ai_notes.md And Assess Status

### Context
- Minecraft: 1.21.1
- Terra branch: dev/1.21.1
- Loader target: NeoForge
- Architectury Loom: required/present
- Environment: Windows 11, JDK 21, Gradle 8.14.1

### Goal
Re-read ai_notes.md in full to comply with AGENTS.md and establish current status before continuing work.

### Hypothesis
The latest entries will confirm the mixin-conversion strategy and identify the next concrete step.

### Actions Taken
- Read `C:\Terra\Terra\ai_notes.md` in full.
- Confirmed the current plan: convert remaining common/lifecycle mixins into NeoForge module using Yarn names so Loom generates Mojang refmaps.

### Files Touched
- None

### Commands Run
```text
Get-Content -Path C:\Terra\Terra\ai_notes.md
Get-Date -Format "yyyy-MM-dd HH:mm zzz"
```

### Result
- Ready to continue work from the mixin conversion and NeoForge mixin config updates.

## 2026-02-05 00:16 -05:00 ó Inspect NeoForge Mixin State

### Context
- Minecraft: 1.21.1
- Terra branch: dev/1.21.1
- Loader target: NeoForge
- Architectury Loom: required/present
- Environment: Windows 11, JDK 21, Gradle 8.14.1

### Goal
Verify how many NeoForge mixins exist and whether configs/build files reflect the new mixin set.

### Hypothesis
All common/lifecycle mixins have been copied into NeoForge module, but the NeoForge mixin config and mods.toml still list only the original entries.

### Actions Taken
- Enumerated NeoForge mixin source files and counted them.
- Counted mixin files in common and lifecycle modules for comparison.
- Inspected `terra.neoforge.mixins.json`, `neoforge.mods.toml`, and `platforms/neoforge/build.gradle.kts`.
- Generated a normalized list of NeoForge mixin class names (relative to `com.dfsek.terra.neoforge.mixin`).

### Files Touched
- None

### Commands Run
```text
Get-ChildItem -Path C:\Terra\Terra\platforms\neoforge\src\main\java\com\dfsek\terra\neoforge\mixin -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
(Get-ChildItem -Path C:\Terra\Terra\platforms\neoforge\src\main\java\com\dfsek\terra\neoforge\mixin -Recurse -Filter *.java).Count
(Get-ChildItem -Path C:\Terra\Terra\platforms\mixin-common\src\main\java\com\dfsek\terra\mod\mixin -Recurse -Filter *.java).Count
(Get-ChildItem -Path C:\Terra\Terra\platforms\mixin-lifecycle\src\main\java\com\dfsek\terra\lifecycle\mixin -Recurse -Filter *.java).Count
Get-Content -Path C:\Terra\Terra\platforms\neoforge\src\main\resources\terra.neoforge.mixins.json
Get-Content -Path C:\Terra\Terra\platforms\neoforge\src\main\resources\META-INF\neoforge.mods.toml
Get-Content -Path C:\Terra\Terra\platforms\neoforge\build.gradle.kts
$root = 'C:\Terra\Terra\platforms\neoforge\src\main\java\com\dfsek\terra\neoforge\mixin'; Get-ChildItem -Path $root -Recurse -Filter *.java | ForEach-Object { $_.FullName.Substring($root.Length + 1) -replace '\.java$','' -replace '\\','.' } | Sort-Object
```

### Result
- NeoForge mixin sources: 42 files (matches 35 common + 7 lifecycle).
- `terra.neoforge.mixins.json` still lists only 4 mixins.
- `neoforge.mods.toml` still includes common + lifecycle mixin configs.
- `platforms/neoforge/build.gradle.kts` still excludes only common/lifecycle refmaps (refmap-less approach), which is known to fail.
- Need to update NeoForge mixin config and exclude common/lifecycle mixin classes in the shaded jar.

## 2026-02-05 00:19 -05:00 ó Update NeoForge Mixin Configs And Excludes

### Context
- Minecraft: 1.21.1
- Terra branch: dev/1.21.1
- Loader target: NeoForge
- Architectury Loom: required/present
- Environment: Windows 11, JDK 21, Gradle 8.14.1

### Goal
Switch NeoForge to use only platform-specific mixins and ensure the mixin list is complete, while excluding common/lifecycle mixin classes and refmaps from the shaded jar.

### Hypothesis
With all mixins copied into the NeoForge module and listed in `terra.neoforge.mixins.json`, removing common/lifecycle mixin configs and excluding their classes/refmaps will prevent Yarn-only mixin conflicts at runtime.

### Actions Taken
- Removed the duplicate root `EntityMixin` (NeoForge now uses the copied common mixin in `implementations.terra.entity`).
- Replaced `terra.neoforge.mixins.json` mixin list with the full set of NeoForge mixins (41 entries).
- Removed `terra.common.mixins.json` and `terra.lifecycle.mixins.json` from `neoforge.mods.toml`.
- Updated `shadowJar` to exclude common/lifecycle mixin classes, mixin configs, and refmaps.
- Verified no missing mixins relative to common/lifecycle sources (only `package-info` differs).

### Files Touched
- platforms/neoforge/src/main/java/com/dfsek/terra/neoforge/mixin/EntityMixin.java ó deleted (duplicate)
- platforms/neoforge/src/main/resources/terra.neoforge.mixins.json ó updated mixin list
- platforms/neoforge/src/main/resources/META-INF/neoforge.mods.toml ó removed common/lifecycle mixin configs
- platforms/neoforge/build.gradle.kts ó updated `shadowJar` excludes

### Commands Run
```text
rg -n "neoforge\\.mixin\\.EntityMixin|EntityMixin" C:\Terra\Terra\platforms\neoforge\src\main\resources\terra.neoforge.mixins.json
Get-Content -Path C:\Terra\Terra\platforms\neoforge\src\main\java\com\dfsek\terra\neoforge\mixin\EntityMixin.java
Get-Content -Path C:\Terra\Terra\platforms\neoforge\src\main\java\com\dfsek\terra\neoforge\mixin\implementations\terra\entity\EntityMixin.java
(Apply patch) Delete File: C:\Terra\Terra\platforms\neoforge\src\main\java\com\dfsek\terra\neoforge\mixin\EntityMixin.java
(Apply patch) Update File: C:\Terra\Terra\platforms\neoforge\src\main\resources\terra.neoforge.mixins.json
(Apply patch) Update File: C:\Terra\Terra\platforms\neoforge\src\main\resources\META-INF\neoforge.mods.toml
(Apply patch) Update File: C:\Terra\Terra\platforms\neoforge\build.gradle.kts
(Get-ChildItem count) C:\Terra\Terra\platforms\neoforge\src\main\java\com\dfsek\terra\neoforge\mixin
Compare-Object expected vs actual mixin class names (common+lifecycle vs neoforge)
rg -n "terra\.common\.mixins\.json|terra\.lifecycle\.mixins\.json" C:\Terra\Terra\platforms\neoforge -S
```

### Result
- NeoForge mixin config now lists all required mixins.
- Common/lifecycle mixin configs are removed from mods.toml.
- Shaded jar will exclude Yarn-only mixin classes and refmaps from common/lifecycle.
- Only `implementations.terra.package-info` differs from common/lifecycle source list (expected).

## 2026-02-05 00:21 -05:00 ó Build NeoForge Remap Jar After Mixin Updates

### Context
- Minecraft: 1.21.1
- Terra branch: dev/1.21.1
- Loader target: NeoForge
- Architectury Loom: required/present
- Environment: Windows 11, JDK 21, Gradle 8.14.1

### Goal
Produce an updated NeoForge jar using the full NeoForge mixin set and updated shadowJar exclusions.

### Hypothesis
With common/lifecycle mixins excluded and NeoForge mixins listed, `remapJar` should succeed and output a new jar ready for runtime testing.

### Actions Taken
- Ran the NeoForge remapJar task.
- Listed `platforms/neoforge/build/libs` to confirm artifact names and timestamps.

### Files Touched
- platforms/neoforge/build/libs/Terra-neoforge-6.5.0-BETA+15a298304.jar ó rebuilt
- platforms/neoforge/build/libs/Terra-neoforge-6.5.0-BETA+15a298304-shaded.jar ó rebuilt

### Commands Run
```text
./gradlew :platforms:neoforge:remapJar
Get-ChildItem -Path C:\Terra\Terra\platforms\neoforge\build\libs | Select-Object Name,LastWriteTime,Length
```

### Result
- BUILD SUCCESSFUL (warnings only).
- Output jars:
  - C:\Terra\Terra\platforms\neoforge\build\libs\Terra-neoforge-6.5.0-BETA+15a298304.jar
  - C:\Terra\Terra\platforms\neoforge\build\libs\Terra-neoforge-6.5.0-BETA+15a298304-shaded.jar
- Warnings observed:
  - Mixin annotation processor warnings (no processor claimed annotations).
  - Deprecation warning for `AbstractBlockState.isLiquid()` in `WorldChunkMixin`.
  - Raw type warning for `BoundedRegionArray` in `ChunkRegionMixin`.
  - Unchecked cast warning in `SimpleRegistryMixin`.

## 2026-02-05 00:28 -05:00 ó Avoid Fabric Command Manager Crash On NeoForge

### Context
- Minecraft: 1.21.1
- Terra branch: dev/1.21.1
- Loader target: NeoForge
- Architectury Loom: required/present
- Environment: Windows 11, JDK 21, Gradle 8.14.1

### Goal
Prevent NeoForge startup from crashing due to missing `org.incendo.cloud.fabric.FabricServerCommandManager`.

### Hypothesis
If command manager construction is done via reflection and skipped when Fabric classes are absent, NeoForge can continue loading without a hard dependency on cloud-fabric.

### Actions Taken
- Reworked `LifecycleEntryPoint` to create the Fabric command manager via reflection.
- Added a safe fallback to skip command registration when the Fabric class is not present.
- Made brigadier manager configuration reflective to avoid compile-time dependency.

### Files Touched
- platforms/mixin-lifecycle/src/main/java/com/dfsek/terra/lifecycle/LifecycleEntryPoint.java ó edited (reflective command manager + fallback)

### Commands Run
```text
Get-Date -Format "yyyy-MM-dd HH:mm zzz"
```

### Result
- NeoForge should no longer crash with `NoClassDefFoundError` for `FabricServerCommandManager`.
- Command registration will be skipped on NeoForge until a native command manager is implemented.

## 2026-02-05 00:28 -05:00 ó Rebuild NeoForge Jar After Command Manager Fix

### Context
- Minecraft: 1.21.1
- Terra branch: dev/1.21.1
- Loader target: NeoForge
- Architectury Loom: required/present
- Environment: Windows 11, JDK 21, Gradle 8.14.1

### Goal
Rebuild NeoForge jar so the updated `LifecycleEntryPoint` (reflective Fabric command manager) is included.

### Hypothesis
Rebuilding should remove the `NoClassDefFoundError` for `FabricServerCommandManager` on NeoForge.

### Actions Taken
- Ran `:platforms:neoforge:remapJar`.
- Checked jar timestamps in `platforms/neoforge/build/libs`.

### Files Touched
- platforms/neoforge/build/libs/Terra-neoforge-6.5.0-BETA+15a298304.jar ó rebuilt
- platforms/neoforge/build/libs/Terra-neoforge-6.5.0-BETA+15a298304-shaded.jar ó rebuilt

### Commands Run
```text
./gradlew :platforms:neoforge:remapJar
Get-ChildItem -Path C:\Terra\Terra\platforms\neoforge\build\libs | Select-Object Name,LastWriteTime,Length
```

### Result
- BUILD SUCCESSFUL (warnings only).
- Updated jar timestamps: 2026-02-05 00:28.

## 2026-02-05 00:52 -05:00 ó Review Crash Logs After World Creation

### Context
- Minecraft: 1.21.1
- Terra branch: dev/1.21.1
- Loader target: NeoForge
- Architectury Loom: required/present
- Environment: Windows 11, JDK 21, Gradle 8.14.1

### Goal
Identify the root cause of the crash when creating a new world by reviewing latest.log and debug.log.

### Hypothesis
The crash is likely due to a missing class or mixin-related classpath issue introduced by recent shadowJar exclusions.

### Actions Taken
- Read AGENTS.md and ai_notes.md to confirm constraints and current plan.
- Opened latest.log and debug.log from the Terra Code 2 instance.
- Located the first fatal exception and its cause.

### Files Touched
- AGENTS.md ó inspected
- ai_notes.md ó inspected
- C:\Users\anelson\curseforge\minecraft\Instances\Terra Code 2\logs\latest.log ó inspected
- C:\Users\anelson\curseforge\minecraft\Instances\Terra Code 2\logs\debug.log ó inspected

### Commands Run
```text
Get-Content -Path AGENTS.md
Get-Content -Path ai_notes.md
Get-Content -Path "C:\Users\anelson\curseforge\minecraft\Instances\Terra Code 2\logs\latest.log" -Tail 200
Get-Content -Path "C:\Users\anelson\curseforge\minecraft\Instances\Terra Code 2\logs\debug.log" -Tail 200
Get-Date -Format "yyyy-MM-dd HH:mm zzz"
```

### Result
- Crash occurs during world creation while registering biomes.
- Root cause in both logs: `NoClassDefFoundError: com/dfsek/terra/mod/mixin/access/BiomeAccessor` thrown from `MinecraftUtil.createBiome`.
- Indicates the common mixin accessor interface is missing from the NeoForge jar (likely excluded by shadowJar).

## 2026-02-05 00:55 -05:00 ó Re-Read ai_notes.md Before Fix

### Context
- Minecraft: 1.21.1
- Terra branch: dev/1.21.1
- Loader target: NeoForge
- Architectury Loom: required/present
- Environment: Windows 11, JDK 21, Gradle 8.14.1

### Goal
Comply with AGENTS.md by re-reading ai_notes.md in full before making changes.

### Hypothesis
The latest log entries will confirm the current crash cause and the intended fix.

### Actions Taken
- Read `C:\Terra\Terra\ai_notes.md` in full.

### Files Touched
- ai_notes.md ó inspected

### Commands Run
```text
Get-Content -Path ai_notes.md
Get-Date -Format "yyyy-MM-dd HH:mm zzz"
```

### Result
- Confirmed the current blocker is a missing `com.dfsek.terra.mod.mixin.access.BiomeAccessor` class in the NeoForge jar.
- Proceeding to restore common accessor/invoker classes and bridge NeoForge accessors to them.

## 2026-02-05 01:02 -05:00 ó Update Accessor Bridges And Attempt Rebuild (Timed Out)

### Context
- Minecraft: 1.21.1
- Terra branch: dev/1.21.1
- Loader target: NeoForge
- Architectury Loom: required/present
- Environment: Windows 11, JDK 21, Gradle 8.14.1

### Goal
Fix the missing `com.dfsek.terra.mod.mixin.access.BiomeAccessor` class and prevent common accessor crashes, then rebuild the NeoForge jar.

### Hypothesis
Keeping common access/invoke interfaces in the shaded jar and having NeoForge accessors extend them will satisfy shared code, while a reflective fallback for `VillagerTypeAccessor` will prevent static accessor failures. Rebuilding should produce a testable jar.

### Actions Taken
- Updated NeoForge accessors/invoker to extend common access/invoke interfaces.
- Adjusted NeoForge `shadowJar` exclusions to keep common access/invoke classes while still excluding other common/lifecycle mixins/refmaps.
- Added reflective fallback in common `VillagerTypeAccessor` to call the NeoForge accessor if present.
- Started `:platforms:neoforge:remapJar` rebuild.

### Files Touched
- platforms/neoforge/src/main/java/com/dfsek/terra/neoforge/mixin/access/MobSpawnerLogicAccessor.java ó edited (extends common accessor)
- platforms/neoforge/src/main/java/com/dfsek/terra/neoforge/mixin/access/StateAccessor.java ó edited (extends common accessor)
- platforms/neoforge/src/main/java/com/dfsek/terra/neoforge/mixin/access/StructureAccessorAccessor.java ó edited (extends common accessor)
- platforms/neoforge/src/main/java/com/dfsek/terra/neoforge/mixin/access/VillagerTypeAccessor.java ó edited (extends common accessor)
- platforms/neoforge/src/main/java/com/dfsek/terra/neoforge/mixin/invoke/FluidBlockInvoker.java ó edited (extends common invoker)
- platforms/neoforge/build.gradle.kts ó edited (shadowJar exclusions refined)
- platforms/mixin-common/src/main/java/com/dfsek/terra/mod/mixin/access/VillagerTypeAccessor.java ó edited (reflective fallback)

### Commands Run
```text
./gradlew :platforms:neoforge:remapJar
Get-Date -Format "yyyy-MM-dd HH:mm zzz"
```

### Result
- `:platforms:neoforge:remapJar` timed out in this session; rebuild needs to be re-run with a longer timeout.

## 2026-02-05 01:02 -05:00 ó Rebuild NeoForge Jar After Accessor Fixes

### Context
- Minecraft: 1.21.1
- Terra branch: dev/1.21.1
- Loader target: NeoForge
- Architectury Loom: required/present
- Environment: Windows 11, JDK 21, Gradle 8.14.1

### Goal
Produce an updated NeoForge jar with common access/invoke interfaces preserved and the VillagerType accessor fallback included.

### Hypothesis
Re-running `:platforms:neoforge:remapJar` after the accessor fixes will produce a jar that no longer crashes on missing accessors.

### Actions Taken
- Re-ran `:platforms:neoforge:remapJar` with a longer timeout.
- Listed `platforms/neoforge/build/libs` to confirm updated artifacts.

### Files Touched
- platforms/neoforge/build/libs/Terra-neoforge-6.5.0-BETA+15a298304.jar ó rebuilt
- platforms/neoforge/build/libs/Terra-neoforge-6.5.0-BETA+15a298304-shaded.jar ó rebuilt

### Commands Run
```text
./gradlew :platforms:neoforge:remapJar
Get-ChildItem -Path "C:\Terra\Terra\platforms\neoforge\build\libs" | Select-Object Name,LastWriteTime,Length
Get-Date -Format "yyyy-MM-dd HH:mm zzz"
```

### Result
- BUILD SUCCESSFUL.
- Updated jars:
  - C:\Terra\Terra\platforms\neoforge\build\libs\Terra-neoforge-6.5.0-BETA+15a298304.jar
  - C:\Terra\Terra\platforms\neoforge\build\libs\Terra-neoforge-6.5.0-BETA+15a298304-shaded.jar

## 2026-02-05 01:09 -05:00 ó Review Logs After Successful World Creation

### Context
- Minecraft: 1.21.1
- Terra branch: dev/1.21.1
- Loader target: NeoForge
- Architectury Loom: required/present
- Environment: Windows 11, JDK 21, Gradle 8.14.1

### Goal
Verify that the latest client run completed without errors and note any remaining warnings that stand out.

### Hypothesis
The latest logs will show a clean world creation flow with only non-fatal warnings.

### Actions Taken
- Read the tail of latest.log and debug.log from the Terra Code 2 instance.
- Scanned for errors, warnings, and unusual messages during world creation.

### Files Touched
- C:\Users\anelson\curseforge\minecraft\Instances\Terra Code 2\logs\latest.log ó inspected
- C:\Users\anelson\curseforge\minecraft\Instances\Terra Code 2\logs\debug.log ó inspected

### Commands Run
```text
Get-Content -Path "C:\Users\anelson\curseforge\minecraft\Instances\Terra Code 2\logs\latest.log" -Tail 200
Get-Content -Path "C:\Users\anelson\curseforge\minecraft\Instances\Terra Code 2\logs\debug.log" -Tail 200
Get-Date -Format "yyyy-MM-dd HH:mm zzz"
```

### Result
- World creation completed successfully; Terra biomes registered and server started.
- Non-fatal warnings observed:
  - Missing sound events for goat horns (vanilla warning).
  - Cloud Fabric command manager not available (expected due to reflective fallback).
  - NeoForge warnings about ocelot mob category mismatches in Terra biomes.
  - Mixin debug warnings about unused CallbackInfo and Architectury mixin class version 65 vs Mixin JAVA_16 (not fatal).
- No crash or error stacktraces found.

## 2026-02-05 01:18 -05:00 ó Inspect ìDetected setBlock in a far chunkî Errors

### Context
- Minecraft: 1.21.1
- Terra branch: dev/1.21.1
- Loader target: NeoForge
- Architectury Loom: required/present
- Environment: Windows 11, JDK 21, Gradle 8.14.1

### Goal
Locate the reported ìDetected setBlock in a far chunkî errors in the logs and see if there is stack trace context.

### Hypothesis
These are vanilla worldgen warnings indicating features placing blocks outside the current generation region; logs may not include a stack trace in production.

### Actions Taken
- Searched `latest.log` for the error string.
- Searched `debug.log` for the error string with nearby context lines.
- Opened the NeoForge ChunkRegion mixin to understand the setBlock path used during worldgen.

### Files Touched
- C:\Users\anelson\curseforge\minecraft\Instances\Terra Code 2\logs\latest.log ó inspected
- C:\Users\anelson\curseforge\minecraft\Instances\Terra Code 2\logs\debug.log ó inspected
- platforms/neoforge/src/main/java/com/dfsek/terra/neoforge/mixin/implementations/terra/world/ChunkRegionMixin.java ó inspected

### Commands Run
```text
Select-String -Path "C:\Users\anelson\curseforge\minecraft\Instances\Terra Code 2\logs\latest.log" -Pattern "Detected setBlock in a far chunk" -Context 0,0 | Select-Object -First 20
Select-String -Path "C:\Users\anelson\curseforge\minecraft\Instances\Terra Code 2\logs\debug.log" -Pattern "Detected setBlock in a far chunk" -Context 3,6 | Select-Object -First 5
Get-Content -Path "C:\Terra\Terra\platforms\neoforge\src\main\java\com\dfsek\terra\neoforge\mixin\implementations\terra\world\ChunkRegionMixin.java"
Get-Date -Format "yyyy-MM-dd HH:mm zzz"
```

### Result
- The error appears multiple times in latest.log and debug.log during worldgen; no stack trace was printed around the entries.
- Warnings are emitted during `minecraft:features` generation and appear to be standard far-chunk write detection.
- ChunkRegion mixin uses `ChunkRegion.setBlockState`, which is where the warning is emitted if the target chunk is outside the region bounds.

## 2026-02-05 01:21 -05:00 ó Re-Read ai_notes.md Before Snapshot

### Context
- Minecraft: 1.21.1
- Terra branch: dev/1.21.1
- Loader target: NeoForge
- Architectury Loom: required/present
- Environment: Windows 11, JDK 21, Gradle 8.14.1

### Goal
Comply with AGENTS.md by re-reading ai_notes.md in full before taking further action.

### Hypothesis
The latest entries will confirm the current state before creating a new snapshot.

### Actions Taken
- Read `C:\Terra\Terra\ai_notes.md` in full.

### Files Touched
- ai_notes.md ó inspected

### Commands Run
```text
Get-Content -Path ai_notes.md
Get-Date -Format "yyyy-MM-dd HH:mm zzz"
```

### Result
- Current state confirmed; proceeding to create a new snapshot.

## 2026-02-05 01:22 -05:00 ó Snapshot Attempt Failed (Locked File)

### Context
- Minecraft: 1.21.1
- Terra branch: dev/1.21.1
- Loader target: NeoForge
- Architectury Loom: required/present
- Environment: Windows 11, JDK 21, Gradle 8.14.1

### Goal
Create a new snapshot (git tag/branch + zip archive) of the current workspace.

### Hypothesis
A simple `Compress-Archive` of the workspace will succeed as before.

### Actions Taken
- Created a new git tag and branch with a timestamped snapshot name.
- Attempted to compress `C:\Terra\Terra\*` into `C:\Terra\Snapshots`.

### Files Touched
- .git/refs/tags/snapshot-20260205-0121 ó created
- .git/refs/heads/snapshot-20260205-0121 ó created
- C:\Terra\Snapshots ó created (if missing)

### Commands Run
```text
$stamp = Get-Date -Format "yyyyMMdd-HHmm"; $tag = "snapshot-$stamp"; $branch = "snapshot-$stamp"; git -C C:\Terra\Terra tag $tag; git -C C:\Terra\Terra branch $branch; New-Item -ItemType Directory -Force -Path C:\Terra\Snapshots | Out-Null; Compress-Archive -Path C:\Terra\Terra\* -DestinationPath ("C:\Terra\Snapshots\Terra-snapshot-{0}.zip" -f $stamp) -Force; Get-Item -Path ("C:\Terra\Snapshots\Terra-snapshot-{0}.zip" -f $stamp) | Select-Object FullName,Length; $tag; $branch
Get-Date -Format "yyyy-MM-dd HH:mm zzz"
```

### Result
- Git tag/branch created: `snapshot-20260205-0121`.
- Zip creation failed because `C:\Terra\Terra\platforms\mixin-common\build\resources\main\packs\default.zip` was locked by another process.
- No snapshot zip was produced; will retry with exclusions or alternate method.

## 2026-02-05 01:23 -05:00 ó Snapshot Attempt Failed (Compress-Archive -Exclude Unsupported)

### Context
- Minecraft: 1.21.1
- Terra branch: dev/1.21.1
- Loader target: NeoForge
- Architectury Loom: required/present
- Environment: Windows 11, JDK 21, Gradle 8.14.1

### Goal
Create a snapshot zip while excluding a locked file.

### Hypothesis
`Compress-Archive` supports `-Exclude` for skipping the locked `default.zip`.

### Actions Taken
- Created a new git tag and branch with timestamp `snapshot-20260205-0123`.
- Tried to run `Compress-Archive` with `-Exclude`.

### Files Touched
- .git/refs/tags/snapshot-20260205-0123 ó created
- .git/refs/heads/snapshot-20260205-0123 ó created

### Commands Run
```text
$stamp = Get-Date -Format "yyyyMMdd-HHmm"; $tag = "snapshot-$stamp"; $branch = "snapshot-$stamp"; git -C C:\Terra\Terra tag $tag; git -C C:\Terra\Terra branch $branch; New-Item -ItemType Directory -Force -Path C:\Terra\Snapshots | Out-Null; Compress-Archive -Path C:\Terra\Terra\* -Exclude "default.zip" -DestinationPath ("C:\Terra\Snapshots\Terra-snapshot-{0}.zip" -f $stamp) -Force; Get-Item -Path ("C:\Terra\Snapshots\Terra-snapshot-{0}.zip" -f $stamp) | Select-Object FullName,Length; $tag; $branch
Get-Date -Format "yyyy-MM-dd HH:mm zzz"
```

### Result
- `Compress-Archive` does not support `-Exclude` in this environment; zip not created.
- Will retry using `tar` with explicit `--exclude`.
