plugins {
    id("net.neoforged.moddev") version Versions.NeoForge.modDevGradle
}

dependencies {
    shadedApi(project(":common:implementation:base"))

    compileOnly("net.fabricmc:sponge-mixin:${Versions.Mod.mixin}")
    compileOnly("io.github.llamalad7:mixinextras-common:${Versions.Mod.mixinExtras}")
    annotationProcessor("net.fabricmc:sponge-mixin:${Versions.Mod.mixin}")

    implementation(project(":platforms:mixin-common")) { isTransitive = false }

    implementation("org.incendo", "cloud-neoforge", Versions.NeoForge.cloud) {
        exclude("net.fabricmc")
        exclude("net.fabricmc.fabric-api")
        exclude("me.lucko", "fabric-permissions-api")
    }
}

neoForge {
    neoFormVersion = Versions.NeoForge.neoForm
}
