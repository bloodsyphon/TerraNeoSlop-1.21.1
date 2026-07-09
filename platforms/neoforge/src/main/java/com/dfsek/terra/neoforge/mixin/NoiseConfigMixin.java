package com.dfsek.terra.neoforge.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dfsek.terra.mod.util.SeedHack;
import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;


/**
 * Hack to map noise sampler to seeds
 */
@Mixin(RandomState.class)
public class NoiseConfigMixin {
    @Shadow
    @Final
    private Sampler sampler;

    @Inject(
        method = {
            "<init>(Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;Lnet/minecraft/core/HolderGetter;J)V",
            "<init>(Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;Lnet/minecraft/core/HolderGetter;J)V"
        },
        at = @At("TAIL"),
        require = 0,
        remap = false
    )
    private void mapMultiNoise(NoiseGeneratorSettings chunkGeneratorSettings,
                               HolderGetter<NormalNoise.NoiseParameters> noiseParametersLookup, long seed,
                               CallbackInfo ci) {
        SeedHack.register(sampler, seed);
    }
}
