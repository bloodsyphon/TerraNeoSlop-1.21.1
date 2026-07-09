plugins {
    id("net.neoforged.moddev") version Versions.NeoForge.modDevGradle
}

dependencies {
    shadedApi(project(":common:implementation:base"))

    compileOnly("net.fabricmc:sponge-mixin:${Versions.Mod.mixin}")
    annotationProcessor("net.fabricmc:sponge-mixin:${Versions.Mod.mixin}")
}

neoForge {
    neoFormVersion = Versions.NeoForge.neoForm
}
