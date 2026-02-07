package com.dfsek.terra.neoforge.mixin.lifecycle;

import net.minecraft.Bootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;


@Mixin(Bootstrap.class)
public class BootstrapMixin {
    @Inject(
        method = "ensureBootstrapped(Ljava/util/function/Supplier;)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void terra$skipEnsureBootstrapped(Supplier<String> callerGetter, CallbackInfo ci) {
        ci.cancel();
    }
}
