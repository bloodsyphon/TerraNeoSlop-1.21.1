package com.dfsek.terra.neoforge.mixin.lifecycle;

import net.minecraft.server.SaveLoading;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;


@Mixin(SaveLoading.class)
public class SaveLoadingMixin {
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger(SaveLoadingMixin.class);
    @Unique
    private static boolean terra$loggedBootstrapContext = false;

    @Inject(
        method = "load(Lnet/minecraft/server/SaveLoading$ServerConfig;Lnet/minecraft/server/SaveLoading$LoadContextSupplier;" +
                 "Lnet/minecraft/server/SaveLoading$SaveApplierFactory;Ljava/util/concurrent/Executor;Ljava/util/concurrent/Executor;)" +
                 "Ljava/util/concurrent/CompletableFuture;",
        at = @At("HEAD"),
        remap = false
    )
    private static void terra$ensureBootstrap(CallbackInfoReturnable<CompletableFuture<?>> cir) {
        ClassLoader loader = SaveLoading.class.getClassLoader();
        try {
            Class<?> bootstrapClass = Class.forName("net.minecraft.Bootstrap", true, loader);
            Method initialize = bootstrapClass.getMethod("initialize");
            Method ensureBootstrapped = bootstrapClass.getMethod("ensureBootstrapped", Supplier.class);

            boolean ready = terra$isBootstrapped(ensureBootstrapped);
            if(!ready) {
                initialize.invoke(null);
                ready = terra$isBootstrapped(ensureBootstrapped);
            }

            if(!terra$loggedBootstrapContext) {
                LOGGER.info("SaveLoading bootstrap check loader={} classHash={} ready={}",
                    loader, System.identityHashCode(bootstrapClass), ready);
                terra$loggedBootstrapContext = true;
            }

            if(!ready) {
                throw new IllegalStateException("Minecraft bootstrap remained unavailable in SaveLoading phase.");
            }
        } catch(ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to ensure bootstrap state before SaveLoading.", e);
        }
    }

    @Unique
    private static boolean terra$isBootstrapped(Method ensureBootstrapped) throws IllegalAccessException {
        try {
            ensureBootstrapped.invoke(null, (Supplier<String>) () -> "Terra SaveLoadingMixin");
            return true;
        } catch(InvocationTargetException e) {
            if(e.getCause() instanceof IllegalArgumentException) {
                return false;
            }
            throw new IllegalStateException("Unexpected bootstrap check failure.", e.getCause());
        }
    }
}
