package com.dfsek.terra.neoforge.mixin.lifecycle;

import com.dfsek.terra.lifecycle.LifecyclePlatform;
import com.dfsek.terra.lifecycle.util.LifecycleUtil;
import com.dfsek.terra.neoforge.mixin.access.RegistryLoadTaskAccessor;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryLoadTask;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.Optional;


@Mixin(RegistryDataLoader.class)
public class RegistryDataLoaderMixin {
    @Inject(
        method = "lambda$load$2(Ljava/util/List;Ljava/util/Map;Ljava/lang/Void;)Lnet/minecraft/core/RegistryAccess$Frozen;",
        at = @At("HEAD"),
        remap = false
    )
    private static void beforeFreeze(List<RegistryLoadTask<?>> loadTasks, Map<ResourceKey<?>, Exception> loadingErrors,
                                     Void ignored, CallbackInfoReturnable<RegistryAccess.Frozen> cir) {
        if(loadTasks.stream().noneMatch(task -> terra$registry(task).key().equals(Registries.BIOME))) {
            return;
        }

        Optional<WritableRegistry<Biome>> biomes = terra$extractRegistry(loadTasks, Registries.BIOME);
        Optional<WritableRegistry<DimensionType>> dimensionTypes = terra$extractRegistry(loadTasks, Registries.DIMENSION_TYPE);
        Optional<WritableRegistry<WorldPreset>> worldPresets = terra$extractRegistry(loadTasks, Registries.WORLD_PRESET);
        Optional<WritableRegistry<NoiseGeneratorSettings>> noiseSettings = terra$extractRegistry(loadTasks, Registries.NOISE_SETTINGS);
        Optional<WritableRegistry<MultiNoiseBiomeSourceParameterList>> multiNoise = terra$extractRegistry(loadTasks,
            Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);
        Optional<WritableRegistry<Enchantment>> enchantments = terra$extractRegistry(loadTasks, Registries.ENCHANTMENT);

        if(biomes.isEmpty() || dimensionTypes.isEmpty() || worldPresets.isEmpty() || noiseSettings.isEmpty()
           || multiNoise.isEmpty() || enchantments.isEmpty()) {
            return;
        }

        LifecyclePlatform.setRegistries(biomes.get(), dimensionTypes.get(), noiseSettings.get(), multiNoise.get(), enchantments.get());
        LifecycleUtil.initialize(biomes.get(), worldPresets.get());
    }

    @Unique
    @SuppressWarnings("unchecked")
    private static <T> Optional<WritableRegistry<T>> terra$extractRegistry(List<RegistryLoadTask<?>> tasks,
                                                                           ResourceKey<? extends Registry<T>> key) {
        List<WritableRegistry<T>> matches = tasks.stream()
            .map(RegistryDataLoaderMixin::terra$registry)
            .filter(registry -> registry.key().equals(key))
            .map(registry -> (WritableRegistry<T>) registry)
            .toList();
        if(matches.size() > 1) {
            throw new IllegalStateException("Illegal number of registries returned: " + matches);
        } else if(matches.isEmpty()) {
            return Optional.empty();
        }

        WritableRegistry<T> registry = matches.getFirst();
        terra$invokeRegistryBind(registry);
        return Optional.of(registry);
    }

    @Unique
    private static WritableRegistry<?> terra$registry(RegistryLoadTask<?> task) {
        return ((RegistryLoadTaskAccessor<?>) task).terra$getRegistry();
    }

    @Unique
    private static void terra$invokeRegistryBind(WritableRegistry<?> registry) {
        try {
            registry.getClass().getMethod("terra_bind").invoke(registry);
        } catch(ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to bind registry entries before Terra lifecycle initialization.", e);
        }
    }
}
