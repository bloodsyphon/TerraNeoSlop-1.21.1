package com.dfsek.terra.neoforge.mixin.fix;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationContext;
import net.minecraft.world.level.levelgen.structure.Structure.GenerationStub;
import net.minecraft.world.level.levelgen.structure.structures.NetherFossilStructure;
import com.dfsek.terra.mod.generation.MinecraftChunkGeneratorWrapper;


/**
 * Disable fossil generation in Terra worlds, as they are very expensive due to consistently triggering cache misses.
 * <p>
 * Currently, on Fabric, Terra cannot be specified as a Nether generator. TODO: logic to turn fossils back on if chunk generator is in
 * nether.
 */
@Mixin(NetherFossilStructure.class)
public class NetherFossilOptimization {
    @Inject(method = "findGenerationPoint", at = @At("HEAD"), cancellable = true, remap = false)
    public void injectFossilPositions(GenerationContext context, CallbackInfoReturnable<Optional<GenerationStub>> cir) {
        if(context.chunkGenerator() instanceof MinecraftChunkGeneratorWrapper) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
