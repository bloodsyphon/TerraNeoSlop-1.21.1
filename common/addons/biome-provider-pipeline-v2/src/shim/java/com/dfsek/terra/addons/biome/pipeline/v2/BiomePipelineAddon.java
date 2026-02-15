/*
 * Copyright (c) 2020-2026 Polyhedral Development
 *
 * Compatibility shim for branches where the dedicated v2 pipeline addon
 * implementation is not available. This preserves the v2 addon ID for packs
 * such as Origen while delegating behavior to the existing pipeline addon.
 */

package com.dfsek.terra.addons.biome.pipeline.v2;

public class BiomePipelineAddon extends com.dfsek.terra.addons.biome.pipeline.BiomePipelineAddon {
}
