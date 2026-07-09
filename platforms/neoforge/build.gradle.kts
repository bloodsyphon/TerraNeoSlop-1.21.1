plugins {
    id("net.neoforged.moddev") version Versions.NeoForge.modDevGradle
}

repositories {
    maven("https://maven.neoforged.net/releases") {
        name = "NeoForge"
    }
}

neoForge {
    version = Versions.NeoForge.neoforge

    runs {
        configureEach {
            systemProperty("mixin.debug.export", "true")
        }
        create("client") {
            client()
        }
        create("server") {
            server()
            programArgument("--nogui")
        }
    }
}

dependencies {
    annotationProcessor("net.fabricmc:sponge-mixin:${Versions.Mod.mixin}")

    shadedApi(project(":common:implementation:base"))

    // NeoForge has platform-specific mixins, but still needs utility classes from mixin modules
    // Shade the modules to include utility classes, but don't load their mixin configs (removed from neoforge.mods.toml)
    shadedApi(project(":platforms:mixin-common")) { isTransitive = false }
    shadedApi(project(":platforms:mixin-lifecycle")) { isTransitive = false }
}

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
        // NeoForge runtime already provides SLF4J; bundling it causes classloader LinkageError in PROD.
        exclude("org/slf4j/**")
        archiveFileName.set("TerraNeoSlop-NeoForge-MC${Versions.Mod.minecraft}-${project.version}.jar")
    }
}
