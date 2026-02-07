package com.dfsek.terra.mod.util;

import net.minecraft.block.spawner.MobSpawnerEntry;
import net.minecraft.block.spawner.MobSpawnerLogic;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.StructureAccessor;

import java.lang.reflect.Field;


public final class ReflectionAccess {
    private static final String[] BIOME_WEATHER_FIELD_NAMES = { "weather", "climateSettings" };
    private static final String[] SPAWNER_ENTRY_FIELD_NAMES = { "spawnEntry", "nextSpawnData" };
    private static final String[] STRUCTURE_ACCESSOR_WORLD_FIELD_NAMES = { "world", "level" };
    private static volatile Field biomeWeatherField;
    private static volatile Field spawnerEntryField;
    private static volatile Field structureAccessorWorldField;

    private ReflectionAccess() {
    }

    public static Biome.Weather getBiomeWeather(Biome biome) {
        try {
            Field field = getOrResolveBiomeWeatherField();
            return (Biome.Weather) field.get(biome);
        } catch(IllegalAccessException e) {
            throw new IllegalStateException("Unable to read biome weather field", e);
        }
    }

    public static MobSpawnerEntry getMobSpawnerEntry(MobSpawnerLogic logic) {
        try {
            Field field = getOrResolveSpawnerEntryField();
            return (MobSpawnerEntry) field.get(logic);
        } catch(IllegalAccessException e) {
            throw new IllegalStateException("Unable to read spawner entry field", e);
        }
    }

    public static Object getStructureAccessorWorld(StructureAccessor accessor) {
        try {
            Field field = getOrResolveStructureAccessorWorldField();
            return field.get(accessor);
        } catch(IllegalAccessException e) {
            throw new IllegalStateException("Unable to read structure accessor world field", e);
        }
    }

    private static Field getOrResolveBiomeWeatherField() {
        Field field = biomeWeatherField;
        if(field == null) {
            field = findField(Biome.class, BIOME_WEATHER_FIELD_NAMES);
            biomeWeatherField = field;
        }
        return field;
    }

    private static Field getOrResolveSpawnerEntryField() {
        Field field = spawnerEntryField;
        if(field == null) {
            field = findField(MobSpawnerLogic.class, SPAWNER_ENTRY_FIELD_NAMES);
            spawnerEntryField = field;
        }
        return field;
    }

    private static Field getOrResolveStructureAccessorWorldField() {
        Field field = structureAccessorWorldField;
        if(field == null) {
            field = findField(StructureAccessor.class, STRUCTURE_ACCESSOR_WORLD_FIELD_NAMES);
            structureAccessorWorldField = field;
        }
        return field;
    }

    private static Field findField(Class<?> owner, String[] candidates) {
        Class<?> current = owner;
        while(current != null) {
            for(String candidate : candidates) {
                try {
                    Field field = current.getDeclaredField(candidate);
                    field.setAccessible(true);
                    return field;
                } catch(NoSuchFieldException ignored) {
                }
            }
            current = current.getSuperclass();
        }
        throw new IllegalStateException("Unable to locate any expected fields on " + owner.getName());
    }
}
