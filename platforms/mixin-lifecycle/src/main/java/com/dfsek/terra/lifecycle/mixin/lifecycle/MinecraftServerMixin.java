package com.dfsek.terra.lifecycle.mixin.lifecycle;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dfsek.terra.lifecycle.LifecyclePlatform;


@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "<init>", at = @At("RETURN"), remap = false, require = 0)
    private void injectConstructor(CallbackInfo ci) {
        LifecyclePlatform.setServer((MinecraftServer) (Object) this);
    }
}
