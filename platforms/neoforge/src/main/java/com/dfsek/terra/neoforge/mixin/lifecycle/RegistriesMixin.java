package com.dfsek.terra.neoforge.mixin.lifecycle;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;


@Mixin(BuiltInRegistries.class)
public class RegistriesMixin {
    @Inject(
        method = "internalRegister(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/WritableRegistry;Lnet/minecraft/core/registries/BuiltInRegistries$RegistryBootstrap;)Lnet/minecraft/core/WritableRegistry;",
        at = @At("HEAD"),
        remap = false
    )
    private static void terra$forceBootstrapReady(ResourceKey<?> key, WritableRegistry<?> registry, @Coerce Object initializer,
                                                  CallbackInfoReturnable<WritableRegistry<?>> cir) {
        try {
            Class<?> bootstrapClass = Class.forName("net.minecraft.Bootstrap", true, BuiltInRegistries.class.getClassLoader());
            Field initialized = bootstrapClass.getDeclaredField("initialized");
            initialized.setAccessible(true);
            if(!initialized.getBoolean(null)) {
                initialized.setBoolean(null, true);
            }
        } catch(ReflectiveOperationException e) {
            throw new IllegalStateException("Failed forcing bootstrap readiness in Registries.create", e);
        }
    }
}
