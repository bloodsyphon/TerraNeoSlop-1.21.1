package com.dfsek.terra.mod.config;

import com.dfsek.tectonic.api.config.template.annotations.Default;
import com.dfsek.tectonic.api.config.template.annotations.Value;
import com.dfsek.tectonic.api.config.template.object.ObjectTemplate;
import net.minecraft.entity.EntityType;
import net.minecraft.world.biome.SpawnSettings.SpawnEntry;


public class SpawnEntryTemplate implements ObjectTemplate<SpawnEntry> {
    @Value("type")
    @Default
    private EntityType<?> type = null;

    @Value("weight")
    @Default
    private Integer weight = null;

    @Value("min-group-size")
    @Default
    private Integer minGroupSize = null;

    @Value("max-group-size")
    @Default
    private Integer maxGroupSize = null;

    @Override
    public SpawnEntry get() {
        int resolvedWeight = weight == null ? 1 : weight;
        int resolvedMin = minGroupSize == null ? 1 : minGroupSize;
        int resolvedMax = maxGroupSize == null ? resolvedMin : Math.max(maxGroupSize, resolvedMin);
        return new SpawnEntry(type, resolvedWeight, resolvedMin, resolvedMax);
    }
}
