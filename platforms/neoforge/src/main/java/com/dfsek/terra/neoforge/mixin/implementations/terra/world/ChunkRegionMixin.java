/*
 * This file is part of Terra.
 *
 * Terra is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Terra is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Terra.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.dfsek.terra.neoforge.mixin.implementations.terra.world;

import com.dfsek.terra.neoforge.mixin.invoke.FluidBlockInvoker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.WorldGenTickAccess;
import com.dfsek.terra.api.block.entity.BlockEntity;
import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.entity.Entity;
import com.dfsek.terra.api.entity.EntityType;
import com.dfsek.terra.api.world.ServerWorld;
import com.dfsek.terra.api.world.biome.generation.BiomeProvider;
import com.dfsek.terra.api.world.chunk.generation.ChunkGenerator;
import com.dfsek.terra.api.world.chunk.generation.ProtoWorld;
import com.dfsek.terra.mod.generation.MinecraftChunkGeneratorWrapper;
import com.dfsek.terra.mod.util.MinecraftUtil;


@Mixin(WorldGenRegion.class)
@Implements(@Interface(iface = ProtoWorld.class, prefix = "terraWorld$"))
public abstract class ChunkRegionMixin {
    private ConfigPack terra$config;


    @Shadow
    @Final
    private net.minecraft.server.level.ServerLevel level;

    @Shadow
    @Final
    private long seed;
    @Shadow
    @Final
    private ChunkAccess center;

    @Shadow
    @Final
    private WorldGenTickAccess<Fluid> fluidTicks;


    @Inject(at = @At("RETURN"),
            method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/util/StaticCache2D;Lnet/minecraft/world/level/chunk/status/ChunkStep;Lnet/minecraft/world/level/chunk/ChunkAccess;)V",
            require = 0,
            remap = false)
    private void injectConstructorLocal(net.minecraft.server.level.ServerLevel world, StaticCache2D<?> chunks,
                                        ChunkStep generationStep, ChunkAccess centerPos, CallbackInfo ci) {
        terra$setConfigFromWorld(world);
    }

    @Inject(at = @At("RETURN"),
            method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/util/StaticCache2D;Lnet/minecraft/world/level/chunk/status/ChunkStep;Lnet/minecraft/world/level/chunk/ChunkAccess;)V",
            require = 0,
            remap = false)
    private void injectConstructorProd(@Coerce Object world, @Coerce Object chunks,
                                       @Coerce Object generationStep, @Coerce Object centerPos, CallbackInfo ci) {
        terra$setConfigFromWorld(world);
    }

    @Unique
    private void terra$setConfigFromWorld(Object worldObj) {
        this.terra$config = ((ServerWorld) worldObj).getPack();
    }


    @Intrinsic(displace = true)
    public void terraWorld$setBlockState(int x, int y, int z, BlockState data, boolean physics) {
        BlockPos pos = new BlockPos(x, y, z);
        ((WorldGenRegion) (Object) this).setBlock(pos, (net.minecraft.world.level.block.state.BlockState) data, physics ? 3 : 1042);
        if(physics && ((net.minecraft.world.level.block.state.BlockState) data).getBlock() instanceof LiquidBlock) {
            fluidTicks.schedule(
                ScheduledTick.probe((((FluidBlockInvoker) ((net.minecraft.world.level.block.state.BlockState) data).getBlock())).invokeGetFluidState(
                    (net.minecraft.world.level.block.state.BlockState) data).getType(), pos));
        }
    }

    @Intrinsic
    public long terraWorld$getSeed() {
        return seed;
    }

    public int terraWorld$getMaxHeight() {
        return level.getMaxY();
    }

    @Intrinsic(displace = true)
    public BlockState terraWorld$getBlockState(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return (BlockState) ((WorldGenRegion) (Object) this).getBlockState(pos);
    }

    public BlockEntity terraWorld$getBlockEntity(int x, int y, int z) {
        return MinecraftUtil.createBlockEntity((LevelAccessor) this, new BlockPos(x, y, z));
    }

    public int terraWorld$getMinHeight() {
        return level.getMinY();
    }

    public ChunkGenerator terraWorld$getGenerator() {
        return ((MinecraftChunkGeneratorWrapper) level.getChunkSource().getGenerator()).getHandle();
    }

    public BiomeProvider terraWorld$getBiomeProvider() {
        return terra$config.getBiomeProvider();
    }

    public Entity terraWorld$spawnEntity(double x, double y, double z, EntityType entityType) {
        net.minecraft.world.entity.Entity entity = ((net.minecraft.world.entity.EntityType<?>) entityType).create(level, EntitySpawnReason.CHUNK_GENERATION);
        entity.setPosRaw(x, y, z);
        ((WorldGenRegion) (Object) this).addFreshEntity(entity);
        return (Entity) entity;
    }

    public int terraWorld$centerChunkX() {
        return center.getPos().x();
    }

    public int terraWorld$centerChunkZ() {
        return center.getPos().z();
    }

    public ServerWorld terraWorld$getWorld() {
        return (ServerWorld) level;
    }

    public ConfigPack terraWorld$getPack() {
        return terra$config;
    }
}
