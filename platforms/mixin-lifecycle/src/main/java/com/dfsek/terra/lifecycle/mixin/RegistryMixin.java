package com.dfsek.terra.lifecycle.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dfsek.terra.lifecycle.util.RegistryUtil;
import net.minecraft.core.registries.BuiltInRegistries;


@Mixin(BuiltInRegistries.class)
public class RegistryMixin {
    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void registerTerraGenerators(CallbackInfo ci) {
        RegistryUtil.register();
    }
}