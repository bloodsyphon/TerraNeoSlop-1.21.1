package com.dfsek.terra.mod.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.event.events.platform.PlatformInitializationEvent;
import com.dfsek.terra.api.registry.key.RegistryKey;
import com.dfsek.terra.api.util.range.ConstantRange;
import com.dfsek.terra.mod.CommonPlatform;
import com.dfsek.terra.mod.generation.GenerationSettings;
import com.dfsek.terra.mod.generation.MinecraftChunkGeneratorWrapper;
import com.dfsek.terra.mod.generation.TerraBiomeSource;
import com.dfsek.terra.mod.implmentation.TerraIntProvider;


public final class Codecs {
    private static final Object CONFIG_PACK_LOAD_LOCK = new Object();

    public static final Codec<RegistryKey> TERRA_REGISTRY_KEY = RecordCodecBuilder
        .create(registryKey -> registryKey.group(Codec.STRING.fieldOf("namespace")
                    .stable()
                    .forGetter(RegistryKey::getNamespace),
                Codec.STRING.fieldOf("id")
                    .stable()
                    .forGetter(RegistryKey::getID))
            .apply(registryKey, registryKey.stable(RegistryKey::of)));

    public static final Codec<ConfigPack> CONFIG_PACK = RecordCodecBuilder
        .create(config -> config.group(TERRA_REGISTRY_KEY.fieldOf("pack")
                .stable()
                .forGetter(ConfigPack::getRegistryKey))
            .apply(config, config.stable(id -> CommonPlatform.get()
                .getConfigRegistry()
                .get(id)
                .orElseGet(() -> createDeferredConfigPack(id)))));

    public static final MapCodec<TerraBiomeSource> TERRA_BIOME_SOURCE = RecordCodecBuilder
        .mapCodec(instance -> instance.group(
                CONFIG_PACK.fieldOf("pack")
                    .stable()
                    .forGetter(TerraBiomeSource::getPack))
            .apply(instance, instance.stable(TerraBiomeSource::new)));

    public static final Codec<ConstantRange> TERRA_CONSTANT_RANGE = RecordCodecBuilder.create(range -> range.group(
        Codec.INT.fieldOf("min").stable().forGetter(ConstantRange::getMin),
        Codec.INT.fieldOf("max").stable().forGetter(ConstantRange::getMax)).apply(range, range.stable(ConstantRange::new)));

    public static final Codec<GenerationSettings> TERRA_GENERATION_SETTINGS = RecordCodecBuilder
        .create(instance -> instance.group(
                TERRA_CONSTANT_RANGE.fieldOf("height").stable().forGetter(GenerationSettings::height),
                Codec.INT.fieldOf("sea_level").forGetter(GenerationSettings::sealevel),
                Codec.BOOL.fieldOf("mob_generation").forGetter(GenerationSettings::mobGeneration),
                Codec.INT.fieldOf("spawn_height").forGetter(GenerationSettings::sealevel))
            .apply(instance, instance.stable(GenerationSettings::new)));


    public static final MapCodec<MinecraftChunkGeneratorWrapper> MINECRAFT_CHUNK_GENERATOR_WRAPPER = RecordCodecBuilder
        .mapCodec(
            instance -> instance.group(
                TERRA_BIOME_SOURCE.fieldOf("biome_source")
                    .stable()
                    .forGetter(MinecraftChunkGeneratorWrapper::getBiomeSource),
                CONFIG_PACK.fieldOf("pack")
                    .stable()
                    .forGetter(MinecraftChunkGeneratorWrapper::getPack),
                TERRA_GENERATION_SETTINGS.fieldOf("settings")
                    .stable()
                    .forGetter(MinecraftChunkGeneratorWrapper::getSettings)
            ).apply(instance, instance.stable(
                MinecraftChunkGeneratorWrapper::new))
        );

    public static final MapCodec<TerraIntProvider> TERRA_CONSTANT_RANGE_INT_PROVIDER_TYPE = RecordCodecBuilder.mapCodec(
        range -> range.group(
                Codec.INT.fieldOf("min").stable().forGetter(TerraIntProvider::getMin),
                Codec.INT.fieldOf("max").stable().forGetter(TerraIntProvider::getMax))
            .apply(range, range.stable((min, max) -> new TerraIntProvider(new ConstantRange(
                min, max)))));

    private static ConfigPack createDeferredConfigPack(RegistryKey key) {
        return (ConfigPack) Proxy.newProxyInstance(
            Codecs.class.getClassLoader(),
            new Class<?>[]{ ConfigPack.class },
            new DeferredConfigPackHandler(key)
        );
    }

    private static final class DeferredConfigPackHandler implements InvocationHandler {
        private final RegistryKey key;
        private ConfigPack resolved;

        private DeferredConfigPackHandler(RegistryKey key) {
            this.key = key;
        }

        private synchronized ConfigPack resolve() {
            if(resolved != null) {
                return resolved;
            }

            ConfigPack pack = CommonPlatform.get().getConfigRegistry().get(key).orElse(null);
            if(pack == null) {
                ensureConfigPacksLoaded();
                pack = CommonPlatform.get().getConfigRegistry().get(key).orElse(null);
            }
            if(pack == null) {
                throw new IllegalStateException("No such config pack " + key);
            }
            resolved = pack;
            return pack;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            return switch(method.getName()) {
                case "getRegistryKey" -> key;
                case "getID" -> key.getID();
                case "getNamespace" -> key.getNamespace();
                case "toString" -> "DeferredConfigPack[" + key + "]";
                case "hashCode" -> key.hashCode();
                case "equals" -> args != null && args.length == 1 && args[0] instanceof ConfigPack other &&
                                  key.equals(other.getRegistryKey());
                default -> invokeResolved(method, args);
            };
        }

        private Object invokeResolved(Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(resolve(), args);
            } catch(InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }

    private static void ensureConfigPacksLoaded() {
        synchronized(CONFIG_PACK_LOAD_LOCK) {
            CommonPlatform.get().getEventManager().callEvent(new PlatformInitializationEvent());
        }
    }
}
