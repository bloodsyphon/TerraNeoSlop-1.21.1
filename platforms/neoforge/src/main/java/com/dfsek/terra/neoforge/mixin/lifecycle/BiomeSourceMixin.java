package com.dfsek.terra.neoforge.mixin.lifecycle;

import net.minecraft.world.biome.source.BiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;


@Mixin(BiomeSource.class)
public class BiomeSourceMixin {
    @Inject(method = "<clinit>", at = @At("HEAD"), remap = false)
    private static void terra$ensureBootstrapBeforeBiomeSourceInit(CallbackInfo ci) {
        Logger logger = LoggerFactory.getLogger("TerraBiomeSourceMixin");
        try {
            ClassLoader loader = BiomeSource.class.getClassLoader();
            Class<?> bootstrapClass = Class.forName("net.minecraft.Bootstrap", true, loader);
            Method initialize = bootstrapClass.getMethod("initialize");
            Method ensureBootstrapped = bootstrapClass.getMethod("ensureBootstrapped", Supplier.class);

            boolean ready = terra$isBootstrapped(ensureBootstrapped);
            logger.info("BiomeSource bootstrap guard loader={} classHash={} readyBefore={}",
                loader, System.identityHashCode(bootstrapClass), ready);
            if(!ready) {
                initialize.invoke(null);
                ready = terra$isBootstrapped(ensureBootstrapped);
            }

            if(!ready) {
                Field initialized = bootstrapClass.getDeclaredField("initialized");
                initialized.setAccessible(true);
                initialized.setBoolean(null, true);
                ready = terra$isBootstrapped(ensureBootstrapped);
            }
            logger.info("BiomeSource bootstrap guard readyAfter={} classHash={}", ready, System.identityHashCode(bootstrapClass));
        } catch(ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to ensure bootstrap before BiomeSource initialization.", e);
        }
    }

    @Unique
    private static boolean terra$isBootstrapped(Method ensureBootstrapped) throws IllegalAccessException {
        try {
            ensureBootstrapped.invoke(null, (Supplier<String>) () -> "Terra BiomeSourceMixin");
            return true;
        } catch(InvocationTargetException e) {
            if(e.getCause() instanceof IllegalArgumentException) {
                return false;
            }
            throw new IllegalStateException("Unexpected bootstrap check failure.", e.getCause());
        }
    }
}
