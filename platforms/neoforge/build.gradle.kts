plugins {
    id("dev.architectury.loom") version Versions.Mod.architecuryLoom
    id("architectury-plugin") version Versions.Mod.architecturyPlugin
}

architectury {
    platformSetupLoomIde()
    loader("neoforge")
}

repositories {
    maven("https://maven.neoforged.net/releases") {
        name = "NeoForge"
    }
}

loom {
    mixin {
        useLegacyMixinAp.set(true)
    }
}

dependencies {
    annotationProcessor("net.fabricmc:sponge-mixin:${Versions.Mod.mixin}")
    annotationProcessor("dev.architectury:architectury-loom:${Versions.Mod.architecuryLoom}")

    shadedApi(project(":common:implementation:base"))

    // NeoForge has platform-specific mixins, but still needs utility classes from mixin modules
    // Shade the modules to include utility classes, but don't load their mixin configs (removed from neoforge.mods.toml)
    shadedApi(project(path = ":platforms:mixin-common", configuration = "namedElements")) { isTransitive = false }
    shadedApi(project(path = ":platforms:mixin-lifecycle", configuration = "namedElements")) { isTransitive = false }

    minecraft("com.mojang:minecraft:${Versions.Mod.minecraft}")
    mappings(loom.layered {
        mappings("net.fabricmc:yarn:${Versions.Mod.yarn}:v2")
        mappings("dev.architectury:yarn-mappings-patch-neoforge:${Versions.Mod.yarnMappingsPatchNeoForge}")
    })

    add("neoForge", "net.neoforged:neoforge:${Versions.NeoForge.neoforge}")
}

loom {
    accessWidenerPath.set(project(":platforms:mixin-common").file("src/main/resources/terra.accesswidener"))

    mixin {
        defaultRefmapName.set("terra.neoforge.refmap.json")
    }

    runs {
        named("client") {
            property("mixin.debug.export", "true")
        }
        named("server") {
            property("mixin.debug.export", "true")
        }
    }
}

addonDir(project.file("./run/config/Terra/addons"), tasks.named("configureLaunch").get())

tasks {
    jar {
        manifest {
            attributes(
                mapOf(
                    "Implementation-Title" to rootProject.name,
                    "Implementation-Version" to project.version,
                )
            )
        }
    }

    shadowJar {
        archiveBaseName.set("TerraNeoSlop-NeoForge-MC${Versions.Mod.minecraft}")
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("shaded")
        // NeoForge uses platform-specific mixins; exclude common/lifecycle mixin classes and configs
        // Keep common access/invoke interfaces for shared code, but avoid Yarn-only mixins/refmaps in the final jar
        exclude("com/dfsek/terra/mod/mixin/implementations/**")
        exclude("com/dfsek/terra/mod/mixin/fix/**")
        exclude("com/dfsek/terra/mod/mixin/lifecycle/**")
        exclude("com/dfsek/terra/lifecycle/mixin/**")
        exclude("terra.common.mixins.json")
        exclude("terra.lifecycle.mixins.json")
        exclude("terra.common.refmap.json")
        exclude("terra.lifecycle.refmap.json")
        // NeoForge runtime provides SLF4J; shading it causes module export conflicts.
        exclude("org/slf4j/**")
        // Compile-time annotations are supplied by NeoForge's module path.
        exclude("com/google/errorprone/**")
    }

    remapJar {
        inputFile.set(shadowJar.get().archiveFile)
        archiveFileName.set("TerraNeoSlop-NeoForge-MC${Versions.Mod.minecraft}-${project.version}.jar")
    }
}
