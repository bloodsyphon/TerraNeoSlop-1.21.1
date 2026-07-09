package com.dfsek.terra.neoforge.mixin.implementations.compat;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import com.dfsek.terra.mod.mixin_ifaces.FloraFeatureHolder;


@Mixin(BiomeGenerationSettings.class)
@Implements(@Interface(iface = FloraFeatureHolder.class, prefix = "terra$"))
public class GenerationSettingsFloraFeaturesMixin {
    private List<ConfiguredFeature<?, ?>> flora;

    public void terra$setFloraFeatures(List<ConfiguredFeature<?, ?>> features) {
        this.flora = features;
    }

    @Inject(method = "getBoneMealFeatures", cancellable = true, at = @At("HEAD"), remap = false)
    public void inject(CallbackInfoReturnable<List<ConfiguredFeature<?, ?>>> cir) {
        if(flora != null) {
            cir.setReturnValue(flora);
        }
    }
}
