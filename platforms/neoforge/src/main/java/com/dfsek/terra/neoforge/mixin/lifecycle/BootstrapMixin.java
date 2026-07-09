package com.dfsek.terra.neoforge.mixin.lifecycle;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;
import net.minecraft.server.Bootstrap;


@Mixin(Bootstrap.class)
public class BootstrapMixin {
    @Inject(
        method = "checkBootstrapCalled(Ljava/util/function/Supplier;)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void terra$skipEnsureBootstrapped(Supplier<String> callerGetter, CallbackInfo ci) {
        ci.cancel();
    }
}
