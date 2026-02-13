package com.dfsek.terra.neoforge.mixin.lifecycle;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.MutableRegistry;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryLoader;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.WorldPreset;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

import com.dfsek.terra.lifecycle.LifecyclePlatform;
import com.dfsek.terra.lifecycle.util.LifecycleUtil;

@Mixin(RegistryLoader.class)
public class RegistryLoaderMixin {
    @Inject(
        method = {
            "load(Lnet/minecraft/registry/RegistryLoader$RegistryLoadable;Ljava/util/List;Ljava/util/List;Z)" +
            "Lnet/minecraft/registry/DynamicRegistryManager$Immutable;",
            "load(Lnet/minecraft/resources/RegistryDataLoader$LoadingFunction;Ljava/util/List;Ljava/util/List;Z)" +
            "Lnet/minecraft/core/RegistryAccess$Frozen;"
        },
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V",
            ordinal = 1
        ),
        require = 0,
        remap = false
    )
    private static void beforeFreeze(@Coerce Object loadable, List<RegistryWrapper.Impl<?>> wrappers,
                                     List<RegistryLoader.Entry<?>> entries, boolean includeGameTestEntries,
                                     CallbackInfoReturnable<DynamicRegistryManager.Immutable> cir,
                                     @Local(ordinal = 2) List<RegistryLoader.Loader<?>> registriesList) {
        if(entries.stream().noneMatch(entry -> entry.key() == RegistryKeys.BIOME)) {
            return;
        }
        Optional<MutableRegistry<Biome>> biomes = extractRegistry(registriesList, RegistryKeys.BIOME);
        Optional<MutableRegistry<DimensionType>> dimensionTypes = extractRegistry(registriesList, RegistryKeys.DIMENSION_TYPE);
        Optional<MutableRegistry<WorldPreset>> worldPresets = extractRegistry(registriesList, RegistryKeys.WORLD_PRESET);
        Optional<MutableRegistry<ChunkGeneratorSettings>> chunkGeneratorSettings = extractRegistry(registriesList,
            RegistryKeys.CHUNK_GENERATOR_SETTINGS);
        Optional<MutableRegistry<MultiNoiseBiomeSourceParameterList>> multiNoiseBiomeSourceParameterLists = extractRegistry(registriesList,
            RegistryKeys.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);
        Optional<MutableRegistry<Enchantment>> enchantments = extractRegistry(registriesList, RegistryKeys.ENCHANTMENT);
        if(biomes.isEmpty() || dimensionTypes.isEmpty() || worldPresets.isEmpty() || chunkGeneratorSettings.isEmpty()
           || multiNoiseBiomeSourceParameterLists.isEmpty() || enchantments.isEmpty()) {
            return;
        }
        LifecyclePlatform.setRegistries(biomes.get(), dimensionTypes.get(), chunkGeneratorSettings.get(),
            multiNoiseBiomeSourceParameterLists.get(), enchantments.get());
        LifecycleUtil.initialize(biomes.get(), worldPresets.get());
    }

    @Unique
    @SuppressWarnings("unchecked")
    private static <T> Optional<MutableRegistry<T>> extractRegistry(List<RegistryLoader.Loader<?>> instance,
                                                                    RegistryKey<Registry<T>> key) {
        List<? extends MutableRegistry<?>> matches = instance
            .stream().map(RegistryLoader.Loader::registry)
            .filter(r -> r.getKey().equals(key))
            .toList();
        if(matches.size() > 1) {
            throw new IllegalStateException("Illegal number of registries returned: " + matches);
        } else if(matches.isEmpty()) {
            return Optional.empty();
        }
        MutableRegistry<T> registry = (MutableRegistry<T>) matches.getFirst();
        invokeRegistryBind(registry);
        return Optional.of(registry);
    }

    @Unique
    private static void invokeRegistryBind(MutableRegistry<?> registry) {
        try {
            java.lang.reflect.Method method = registry.getClass().getMethod("terra_bind");
            method.invoke(registry);
        } catch(ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to bind registry entries before Terra lifecycle initialization.", e);
        }
    }
}
