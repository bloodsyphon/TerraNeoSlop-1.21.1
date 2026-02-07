package com.dfsek.terra.neoforge.mixin.lifecycle;

import net.minecraft.registry.MutableRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;


@Mixin(Registries.class)
public class RegistriesMixin {
    @Inject(
        method = "create(Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/registry/MutableRegistry;Lnet/minecraft/registry/Registries$Initializer;)Lnet/minecraft/registry/MutableRegistry;",
        at = @At("HEAD"),
        remap = false
    )
    private static void terra$forceBootstrapReady(RegistryKey<?> key, MutableRegistry<?> registry, @Coerce Object initializer,
                                                  CallbackInfoReturnable<MutableRegistry<?>> cir) {
        try {
            Class<?> bootstrapClass = Class.forName("net.minecraft.Bootstrap", true, Registries.class.getClassLoader());
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
