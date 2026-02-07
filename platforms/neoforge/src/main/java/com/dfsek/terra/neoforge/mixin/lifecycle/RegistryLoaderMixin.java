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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;


@Mixin(RegistryLoader.class)
public class RegistryLoaderMixin {
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryLoaderMixin.class);

    @Inject(
        method = "load(Lnet/minecraft/registry/RegistryLoader$RegistryLoadable;Ljava/util/List;Ljava/util/List;Z)" +
                 "Lnet/minecraft/registry/DynamicRegistryManager$Immutable;",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V",
            ordinal = 1
        )
    )
    private static void beforeFreeze(@Coerce Object loadable, List<RegistryWrapper.Impl<?>> wrappers,
                                     List<RegistryLoader.Entry<?>> entries, boolean includeGameTestEntries,
                                     CallbackInfoReturnable<DynamicRegistryManager.Immutable> cir,
                                     @Local(ordinal = 2) List<RegistryLoader.Loader<?>> registriesList) {
        if(entries.stream().noneMatch(entry -> entry.key() == RegistryKeys.BIOME)) {
            return;
        }
        MutableRegistry<Biome> biomes = extractRegistry(registriesList, RegistryKeys.BIOME).orElseThrow();
        MutableRegistry<DimensionType> dimensionTypes = extractRegistry(registriesList, RegistryKeys.DIMENSION_TYPE).orElseThrow();
        MutableRegistry<WorldPreset> worldPresets = extractRegistry(registriesList, RegistryKeys.WORLD_PRESET).orElseThrow();
        MutableRegistry<ChunkGeneratorSettings> chunkGeneratorSettings = extractRegistry(registriesList,
            RegistryKeys.CHUNK_GENERATOR_SETTINGS).orElseThrow();
        MutableRegistry<MultiNoiseBiomeSourceParameterList> multiNoiseBiomeSourceParameterLists = extractRegistry(registriesList,
            RegistryKeys.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).orElseThrow();
        MutableRegistry<Enchantment> enchantments = extractRegistry(registriesList, RegistryKeys.ENCHANTMENT).orElseThrow();
        callLifecycleSetRegistries(biomes, dimensionTypes, chunkGeneratorSettings, multiNoiseBiomeSourceParameterLists, enchantments);
        callLifecycleInitialize(biomes, worldPresets);
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
            Method method = registry.getClass().getMethod("terra_bind");
            method.invoke(registry);
        } catch(ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to bind registry entries before Terra lifecycle initialization.", e);
        }
    }

    @Unique
    private static void callLifecycleSetRegistries(Registry<?> biomes, Registry<?> dimensionTypes, Registry<?> chunkSettings,
                                                   Registry<?> noise, Registry<?> enchantments) {
        try {
            Class<?> lifecyclePlatform = resolveTerraClass("com.dfsek.terra.lifecycle.LifecyclePlatform");
            Method setRegistries = findMethodByNameAndArity(lifecyclePlatform, "setRegistries", 5);
            try {
                setRegistries.invoke(null, biomes, dimensionTypes, chunkSettings, noise, enchantments);
            } catch(IllegalArgumentException e) {
                throw new IllegalStateException(buildInvocationDiagnostics("setRegistries", setRegistries,
                    biomes, dimensionTypes, chunkSettings, noise, enchantments), e);
            }
        } catch(ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to update Terra lifecycle registries.", e);
        }
    }

    @Unique
    private static void callLifecycleInitialize(MutableRegistry<?> biomes, MutableRegistry<?> worldPresets) {
        try {
            Class<?> lifecycleUtil = resolveTerraClass("com.dfsek.terra.lifecycle.util.LifecycleUtil");
            Method initialize = findMethodByNameAndArity(lifecycleUtil, "initialize", 2);
            try {
                initialize.invoke(null, biomes, worldPresets);
            } catch(IllegalArgumentException e) {
                throw new IllegalStateException(buildInvocationDiagnostics("initialize", initialize, biomes, worldPresets), e);
            }
        } catch(ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to run Terra lifecycle initialization.", e);
        }
    }

    @Unique
    private static String buildInvocationDiagnostics(String label, Method method, Object... args) {
        StringBuilder builder = new StringBuilder();
        builder.append("Failed invoking Terra lifecycle method ").append(label).append(". ");
        builder.append("owner=").append(method.getDeclaringClass().getName())
            .append(" ownerLoader=").append(method.getDeclaringClass().getClassLoader())
            .append(" ownerModule=").append(method.getDeclaringClass().getModule().getName()).append(' ');
        builder.append("params=[");
        Class<?>[] params = method.getParameterTypes();
        for(int i = 0; i < params.length; i++) {
            Class<?> param = params[i];
            if(i > 0) builder.append(", ");
            builder.append(param.getName())
                .append("@").append(param.getClassLoader())
                .append("#").append(System.identityHashCode(param));
        }
        builder.append("] args=[");
        for(int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if(i > 0) builder.append(", ");
            if(arg == null) {
                builder.append("null");
            } else {
                Class<?> type = arg.getClass();
                builder.append(type.getName())
                    .append("@").append(type.getClassLoader())
                    .append("#").append(System.identityHashCode(type));
            }
        }
        builder.append("]");
        String message = builder.toString();
        LOGGER.error(message);
        return message;
    }

    @Unique
    private static Method findMethodByNameAndArity(Class<?> owner, String name, int arity) throws NoSuchMethodException {
        for(Method method : owner.getMethods()) {
            if(method.getName().equals(name) && method.getParameterCount() == arity) {
                return method;
            }
        }
        throw new NoSuchMethodException(owner.getName() + "." + name + " with " + arity + " parameters");
    }

    @Unique
    private static Class<?> resolveTerraClass(String className) throws ClassNotFoundException {
        ModuleLayer layer = RegistryLoader.class.getModule().getLayer();
        if(layer != null) {
            Optional<Module> terraModule = layer.findModule("terra");
            if(terraModule.isPresent()) {
                Class<?> resolved = Class.forName(terraModule.get(), className);
                if(resolved != null) {
                    return resolved;
                }
            }
        }
        return Class.forName(className, false, RegistryLoader.class.getClassLoader());
    }
}
