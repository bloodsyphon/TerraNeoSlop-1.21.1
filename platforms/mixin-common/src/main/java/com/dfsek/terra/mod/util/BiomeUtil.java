package com.dfsek.terra.mod.util;

import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.Builder;
import net.minecraft.world.biome.BiomeEffects;
import net.minecraft.world.biome.GenerationSettings;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.mod.config.VanillaBiomeProperties;


public class BiomeUtil {
    public static final Map<Identifier, List<Identifier>>
        TERRA_BIOME_MAP = new HashMap<>();

    public static Biome createBiome(Biome vanilla, VanillaBiomeProperties vanillaBiomeProperties) {
        // In Minecraft 1.21.11, many biome properties were moved to environment attributes
        // For now, we'll use a simplified approach that just uses the vanilla biome's effects
        // TODO: Implement proper environment attributes support for custom biome properties

        net.minecraft.world.biome.Biome.Builder builder = new Builder();

        builder.precipitation(Objects.requireNonNullElse(vanillaBiomeProperties.getPrecipitation(), vanilla.hasPrecipitation()));

        builder.temperature(Objects.requireNonNullElse(vanillaBiomeProperties.getTemperature(), vanilla.getTemperature()));

        builder.downfall(Objects.requireNonNullElse(vanillaBiomeProperties.getDownfall(),
            ReflectionAccess.getBiomeWeather(vanilla).downfall()));

        builder.temperatureModifier(Objects.requireNonNullElse(vanillaBiomeProperties.getTemperatureModifier(),
            ReflectionAccess.getBiomeWeather(vanilla).temperatureModifier()));

        builder.spawnSettings(Objects.requireNonNullElse(vanillaBiomeProperties.getSpawnSettings(), vanilla.getSpawnSettings()));

        // Use vanilla biome's effects for now since the API has changed significantly
        return builder
            .effects(vanilla.getEffects())
            .generationSettings(new GenerationSettings.Builder().build())
            .build();
    }

    public static String createBiomeID(ConfigPack pack, com.dfsek.terra.api.registry.key.RegistryKey biomeID) {
        return pack.getID()
                   .toLowerCase() + "/" + biomeID.getNamespace().toLowerCase(Locale.ROOT) + "/" + biomeID.getID().toLowerCase(Locale.ROOT);
    }

    public static Map<Identifier, List<Identifier>> getTerraBiomeMap() {
        return Map.copyOf(TERRA_BIOME_MAP);
    }
}
