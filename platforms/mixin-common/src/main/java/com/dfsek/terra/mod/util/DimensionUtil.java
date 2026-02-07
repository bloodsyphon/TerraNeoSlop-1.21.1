package com.dfsek.terra.mod.util;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionType.MonsterSettings;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalLong;

import com.dfsek.terra.mod.ModPlatform;
import com.dfsek.terra.mod.config.MonsterSettingsConfig;
import com.dfsek.terra.mod.config.VanillaWorldProperties;
import com.dfsek.terra.mod.implmentation.TerraIntProvider;


public class DimensionUtil {
    public static DimensionType createDimension(VanillaWorldProperties vanillaWorldProperties, DimensionType defaultDimension,
                                                ModPlatform platform) {

        MonsterSettingsConfig monsterSettingsConfig;
        if(vanillaWorldProperties.getMonsterSettings() != null) {
            monsterSettingsConfig = vanillaWorldProperties.getMonsterSettings();
        } else {
            monsterSettingsConfig = new MonsterSettingsConfig();
        }

        MonsterSettings monsterSettings = getMonsterSettings(defaultDimension, monsterSettingsConfig);

        // In Minecraft 1.21.11, many DimensionType properties were moved to Environment Attributes
        // For now, we use the default dimension as-is since the constructor signature changed significantly
        // TODO: Implement proper environment attributes support for custom dimension properties
        return defaultDimension;
    }

    @NotNull
    private static MonsterSettings getMonsterSettings(DimensionType defaultDimension, MonsterSettingsConfig monsterSettingsConfig) {
        MonsterSettings defaultMonsterSettings = defaultDimension.monsterSettings();

        // In Minecraft 1.21.11, piglinSafe and hasRaids were moved to environment attributes
        // The MonsterSettings constructor now only takes (IntProvider, int)
        return new MonsterSettings(
            monsterSettingsConfig.getMonsterSpawnLight() == null
                ? defaultMonsterSettings.monsterSpawnLightTest()
                : new TerraIntProvider(monsterSettingsConfig.getMonsterSpawnLight()),
            monsterSettingsConfig.getMonsterSpawnBlockLightLimit() == null
                ? defaultMonsterSettings.monsterSpawnBlockLightLimit()
                : monsterSettingsConfig.getMonsterSpawnBlockLightLimit()
        );
    }
}
