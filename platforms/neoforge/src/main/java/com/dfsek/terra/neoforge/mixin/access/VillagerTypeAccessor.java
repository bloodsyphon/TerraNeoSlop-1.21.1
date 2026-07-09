package com.dfsek.terra.neoforge.mixin.access;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.level.biome.Biome;


@Mixin(VillagerType.class)
public interface VillagerTypeAccessor extends com.dfsek.terra.mod.mixin.access.VillagerTypeAccessor {
    @Accessor("BY_BIOME")
    static Map<ResourceKey<Biome>, VillagerType> getBiomeTypeToIdMap() {
        throw new AssertionError("Untransformed Accessor!");
    }
}
