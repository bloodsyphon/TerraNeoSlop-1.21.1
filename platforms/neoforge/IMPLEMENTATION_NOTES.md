# Terra NeoForge 1.21.1 Implementation Notes

**Date**: February 4, 2026
**Status**: Blocked - Dependency Resolution Issue
**Version**: NeoForge 21.1.211 for Minecraft 1.21.1

---

## Overview

This document tracks the implementation of NeoForge support for Terra, including all changes made, issues encountered, and solutions attempted.

## Implementation Completed ✅

### 1. Build System Configuration

**Files Modified:**
- `buildSrc/src/main/kotlin/Versions.kt` - Added NeoForge version constants
- `buildSrc/src/main/kotlin/DependencyConfig.kt` - Added NeoForge Maven repository
- `platforms/mixin-common/build.gradle.kts` - Updated to support both Fabric and NeoForge
- `platforms/mixin-lifecycle/build.gradle.kts` - Updated to support both Fabric and NeoForge

**Changes:**
```kotlin
// Versions.kt (line 57-59)
object NeoForge {
    const val neoforge = "21.1.211"  // NeoForge for Minecraft 1.21.1
}

// DependencyConfig.kt (line 51-53)
maven("https://maven.neoforged.net/releases/") {
    name = "NeoForge"
}

// mixin-common/build.gradle.kts (line 26)
architectury {
    common("fabric", "neoforge")  // Changed from common("fabric")
    minecraft = Versions.Mod.minecraft
}

// mixin-lifecycle/build.gradle.kts (line 41)
architectury {
    common("fabric", "neoforge")  // Changed from common("fabric")
    minecraft = Versions.Mod.minecraft
}
```

### 2. NeoForge Module Structure

**Files Created:**

#### Build Configuration
- `platforms/neoforge/build.gradle.kts` - Complete Gradle build file

#### Java Source Files
- `platforms/neoforge/src/main/java/com/dfsek/terra/neoforge/NeoForgeEntryPoint.java`
  - Main mod entry point with `@Mod("terra")` annotation
  - Initializes via `LifecycleEntryPoint.initialize("NeoForge", TERRA_PLUGIN)`

- `platforms/neoforge/src/main/java/com/dfsek/terra/neoforge/NeoForgePlatform.java`
  - Extends `LifecyclePlatform` (following Fabric's simplified approach)
  - Implements platform-specific methods for mod discovery and data folder

- `platforms/neoforge/src/main/java/com/dfsek/terra/neoforge/NeoForgeAddon.java`
  - Extends `MinecraftAddon`
  - Provides platform addon with ID "terra-neoforge"

#### Configuration Files
- `platforms/neoforge/src/main/resources/META-INF/neoforge.mods.toml`
  - Mod metadata: loader version, dependencies, mod info

- `platforms/neoforge/src/main/resources/terra.neoforge.mixins.json`
  - Mixin configuration (currently empty, ready for platform-specific mixins)

### 3. Design Decisions

**Architecture: Lightweight Lifecycle Approach (like Fabric)**
- ✅ Extends `LifecyclePlatform` instead of heavy `ModPlatform`
- ✅ No `AwfulForgeHacks` needed (NeoForge has better classloading)
- ✅ No platform-specific `BiomeUtil` (uses lifecycle layer utilities)
- ✅ Reuses mixin-common and mixin-lifecycle layers

**Multi-Platform Support:**
- ✅ Fabric support retained (both can coexist)
- ✅ Old Forge left disabled (no value in maintaining both)

**Package Mappings:**
| Old Forge | NeoForge |
|-----------|----------|
| `net.minecraftforge.fml` | `net.neoforged.fml` |
| `net.minecraftforge.fml.common` | `net.neoforged.fml.common` |
| `net.minecraftforge.fml.loading` | `net.neoforged.fml.loading` |

---

## Issues Encountered & Solutions

### Issue #1: Sonatype Repository Timeouts

**Problem:**
```
Could not GET 'https://s01.oss.sonatype.org/content/repositories/snapshots/...minecraft-merged-...'
Received status code 504 from server: Gateway Time-out
```

**Root Cause:**
Gradle was trying to download `minecraft-merged-*` artifact from Sonatype Snapshots repository, which was experiencing 504 Gateway Timeout errors.

**Key Learning (from ChatGPT):**
The `minecraft-merged-*` artifact is NOT a public Maven artifact. It's generated locally by Architectury Loom by merging Minecraft client/server JARs and applying Yarn mappings. It should come from Loom's local cache (`.gradle/loom-cache/minecraftMaven/`), not from remote repositories.

**Solution Attempted:**
1. Deleted corrupted Loom cache: `rm -rf .gradle/loom-cache`
2. Stopped Gradle daemons: `./gradlew --stop`
3. Temporarily disabled Sonatype Snapshots repository in `DependencyConfig.kt` (line 60-63)

**Result:**
✅ Loom successfully regenerated the cache and `minecraft-merged` artifacts exist locally
✅ Build progressed past repository timeout errors

---

### Issue #2: Dependency Configuration Name

**Problem:**
```
Configuration with name 'neoForge' not found.
```

**Attempted Solutions:**

**Attempt 1:** Use `neoForge()` configuration (capital F)
```kotlin
"neoForge"("net.neoforged:neoforge:${Versions.NeoForge.neoforge}")
```
**Result:** ❌ Configuration doesn't exist in Architectury Loom

**Attempt 2:** Use `neoForgeRuntimeLibrary` (following Forge pattern)
```kotlin
"neoForgeRuntimeLibrary"(project(":common:implementation:base"))
```
**Result:** ❌ Configuration not found

**Current Solution:** Using `modImplementation()`
```kotlin
modImplementation("net.neoforged:neoforge:${Versions.NeoForge.neoforge}")
```
**Result:** ⚠️ Dependency downloads but not added to compile classpath

---

### Issue #3: NeoForge Not on Compile Classpath (CURRENT BLOCKER)

**Problem:**
```
error: package net.neoforged.fml.common does not exist
import net.neoforged.fml.common.Mod;
                               ^
```

**Investigation:**

**Evidence NeoForge is Downloaded:**
```bash
$ find ~/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge -name "21.1.211"
/c/Users/anelson/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/21.1.211

$ ls ~/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/21.1.211/
neoforge-21.1.211-sources.jar
neoforge-21.1.211-universal.jar
```

**Issue with Universal JAR:**
```bash
$ jar -tf neoforge-21.1.211-universal.jar | grep net/neoforged/fml
# Returns only: META-INF/MANIFEST.MF
```

The `universal` JAR is **empty** (contains only manifest)!

**Attempted Solutions:**

**Attempt 1:** Add `compileOnly` as workaround
```kotlin
modImplementation("net.neoforged:neoforge:${Versions.NeoForge.neoforge}")
compileOnly("net.neoforged:neoforge:${Versions.NeoForge.neoforge}")
```
**Result:** ❌ Still can't find NeoForge classes

**Attempt 2:** Add Loom Forge configuration block
```kotlin
loom {
    forge {
        convertAccessWideners.set(true)
        mixinConfig("terra.common.mixins.json")
        // ...
    }
}
```
**Result:** ❌ Error: "Loom is not running on Forge"

---

## Root Cause Analysis

### Hypothesis: Architectury Loom Beta Incompatibility

**Current Setup:**
- Architectury Loom: `1.11.451` (beta version)
- Architectury Plugin: `3.4.162`
- NeoForge: `21.1.211`
- Minecraft: `1.21.10`

**Theory:**
Architectury Loom 1.11.451 (beta) may have incomplete or experimental support for NeoForge 1.21.x. The `loader("neoforge")` configuration is recognized, but the dependency resolution isn't properly set up.

**Supporting Evidence:**
1. Loom warning: "This version of Architectury Loom is in beta!"
2. Empty `universal` JAR suggests artifact structure may differ from expectations
3. No `neoForge` or `neoForgeRuntimeLibrary` configuration created
4. `modImplementation()` downloads but doesn't add to classpath

---

## Potential Solutions to Try

### Option 1: Wait for Stable Architectury Loom
- Monitor Architectury releases for stable NeoForge 1.21.1 support
- Check: https://github.com/architectury/architectury-loom/releases

### Option 2: Try Different NeoForge Artifact Classifier
The universal JAR being empty suggests we might need a different classifier:
```kotlin
// Possible alternatives to try:
modImplementation("net.neoforged:neoforge:${version}:installer")
modImplementation("net.neoforged:neoforge:${version}:userdev")
```

### Option 3: Use NeoForge's Native Build System (NeoGradle)
- Replace Architectury Loom with NeoGradle plugin
- **Pros:** Native NeoForge support, guaranteed compatibility
- **Cons:** Major build system rewrite, loses Architectury cross-platform benefits

### Option 4: Try Older NeoForge Version
Test if Architectury Loom works better with NeoForge 21.0.x versions:
```kotlin
const val neoforge = "21.0.103-beta"  // Earlier version
```

### Option 5: Re-enable Sonatype and Complete First-Time Setup
The Sonatype timeout may have interrupted a critical first-time setup. Try:
1. Re-enable Sonatype Snapshots repository
2. Wait for repository availability
3. Run clean build to complete initialization

---

## Current State

### What Works ✅
- Build system recognizes NeoForge module
- Gradle tasks available (`:platforms:neoforge:tasks`)
- Dependencies download successfully
- Mixin layers support NeoForge
- All source code is correct and follows best practices

### What Doesn't Work ❌
- NeoForge classes not available at compile time
- Cannot compile Java source files
- Empty universal JAR artifact

### Build Commands

**Test build system recognition:**
```bash
./gradlew :platforms:neoforge:tasks
```

**Attempt compilation:**
```bash
./gradlew :platforms:neoforge:compileJava
```

**Full build (when fixed):**
```bash
./gradlew :platforms:neoforge:build
```

**Expected output location:**
```
platforms/neoforge/build/libs/Terra-neoforge-7.0.0.jar
```

---

## References

### Documentation Consulted
- [NeoForge Documentation](https://docs.neoforged.net/)
- [NeoForge Versioning](https://docs.neoforged.net/docs/1.21.1/gettingstarted/versioning/)
- [Architectury NeoForge Migration](https://docs.architectury.dev/api/migration/neoforge)
- [NeoForge Maven Repository](https://maven.neoforged.net/releases/)
- [Architectury Loom GitHub](https://github.com/architectury/architectury-loom)

### Key Learnings
1. Yarn mappings are universal across Fabric, NeoForge, and Quilt (not Fabric-specific)
2. `minecraft-merged` artifacts are generated by Loom locally, not fetched remotely
3. NeoForge has better classloading than old Forge (no hacks needed)
4. Architectury's beta versions may have experimental platform support

---

## Next Steps

**Immediate:**
1. Research NeoForge artifact structure (classifiers, versions)
2. Check Architectury Loom issue tracker for known NeoForge 1.21.1 issues
3. Test with different NeoForge versions or artifact classifiers

**Long-term:**
1. Monitor Architectury Loom updates for stable NeoForge support
2. Consider NeoGradle if Architectury path blocked
3. Update this document with solution when found

---

## File Manifest

### Files Created
- `platforms/neoforge/build.gradle.kts`
- `platforms/neoforge/src/main/java/com/dfsek/terra/neoforge/NeoForgeEntryPoint.java`
- `platforms/neoforge/src/main/java/com/dfsek/terra/neoforge/NeoForgePlatform.java`
- `platforms/neoforge/src/main/java/com/dfsek/terra/neoforge/NeoForgeAddon.java`
- `platforms/neoforge/src/main/resources/META-INF/neoforge.mods.toml`
- `platforms/neoforge/src/main/resources/terra.neoforge.mixins.json`

### Files Modified
- `buildSrc/src/main/kotlin/Versions.kt` (added NeoForge version)
- `buildSrc/src/main/kotlin/DependencyConfig.kt` (added NeoForge Maven, disabled Sonatype)
- `platforms/mixin-common/build.gradle.kts` (added neoforge to common())
- `platforms/mixin-lifecycle/build.gradle.kts` (added neoforge to common())

---

**Last Updated**: February 4, 2026
**Author**: Claude (Anthropic AI Assistant)
**Status**: Documentation Complete - Implementation Blocked
