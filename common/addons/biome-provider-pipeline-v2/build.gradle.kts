version = version("1.0.1")

dependencies {
    compileOnlyApi(project(":common:addons:manifest-addon-loader"))
    compileOnlyApi(project(":common:addons:biome-provider-pipeline"))
}

sourceSets {
    main {
        // Use a lightweight compatibility shim for this branch.
        java.setSrcDirs(listOf("src/shim/java"))
        resources.setSrcDirs(listOf("src/main/resources"))
    }
}
