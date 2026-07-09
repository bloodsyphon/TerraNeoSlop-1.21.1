package com.dfsek.terra.neoforge.mixin.invoke;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;


@Mixin(LiquidBlock.class)
public interface FluidBlockInvoker extends com.dfsek.terra.mod.mixin.invoke.FluidBlockInvoker {
    @Invoker("getFluidState")
    public FluidState invokeGetFluidState(BlockState state);
}
