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

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import com.dfsek.terra.api.block.entity.BlockEntity;
import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.entity.Entity;
import com.dfsek.terra.api.entity.EntityType;
import com.dfsek.terra.api.world.ServerWorld;
import com.dfsek.terra.api.world.biome.generation.BiomeProvider;
import com.dfsek.terra.api.world.chunk.Chunk;
import com.dfsek.terra.api.world.chunk.generation.ChunkGenerator;
import com.dfsek.terra.mod.generation.MinecraftChunkGeneratorWrapper;
import com.dfsek.terra.mod.generation.TerraBiomeSource;
import com.dfsek.terra.mod.util.MinecraftUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.LevelAccessor;


@Mixin(net.minecraft.server.level.ServerLevel.class)
@Implements(@Interface(iface = ServerWorld.class, prefix = "terra$"))
public abstract class ServerWorldMixin {
    public Entity terra$spawnEntity(double x, double y, double z, EntityType entityType) {
        net.minecraft.server.level.ServerLevel world = (net.minecraft.server.level.ServerLevel) (Object) this;
        net.minecraft.world.entity.Entity entity = ((net.minecraft.world.entity.EntityType<?>) entityType).create(world, EntitySpawnReason.CHUNK_GENERATION);
        entity.setPosRaw(x, y, z);
        world.addFreshEntity(entity);
        return (Entity) entity;
    }

    public void terra$setBlockState(int x, int y, int z, BlockState data, boolean physics) {
        BlockPos pos = new BlockPos(x, y, z);
        ((net.minecraft.server.level.ServerLevel) (Object) this).setBlock(pos, (net.minecraft.world.level.block.state.BlockState) data,
            physics ? 3 : 1042);
    }

    @Intrinsic
    public long terra$getSeed() {
        return ((net.minecraft.server.level.ServerLevel) (Object) this).getSeed();
    }

    public int terra$getMaxHeight() {
        return (((net.minecraft.server.level.ServerLevel) (Object) this).getMinY()) +
               ((net.minecraft.server.level.ServerLevel) (Object) this).getHeight();
    }

    public Chunk terra$getChunkAt(int x, int z) {
        return (Chunk) ((net.minecraft.server.level.ServerLevel) (Object) this).getChunk(x, z);
    }

    public BlockState terra$getBlockState(int x, int y, int z) {
        return (BlockState) ((net.minecraft.server.level.ServerLevel) (Object) this).getBlockState(new BlockPos(x, y, z));
    }

    public BlockEntity terra$getBlockEntity(int x, int y, int z) {
        return MinecraftUtil.createBlockEntity((LevelAccessor) this, new BlockPos(x, y, z));
    }

    public int terra$getMinHeight() {
        return ((net.minecraft.server.level.ServerLevel) (Object) this).getMinY();
    }

    public ChunkGenerator terra$getGenerator() {
        return ((MinecraftChunkGeneratorWrapper) ((net.minecraft.server.level.ServerLevel) (Object) this).getChunkSource()
            .getGenerator()).getHandle();
    }

    public BiomeProvider terra$getBiomeProvider() {
        return ((TerraBiomeSource) ((net.minecraft.server.level.ServerLevel) (Object) this).getChunkSource()
            .getGenerator()
            .getBiomeSource()).getProvider();
    }

    public ConfigPack terra$getPack() {
        net.minecraft.world.level.chunk.ChunkGenerator generator =
            (((net.minecraft.server.level.ServerLevel) (Object) this).getChunkSource()).getGenerator();
        if(generator instanceof MinecraftChunkGeneratorWrapper minecraftChunkGeneratorWrapper) {
            return minecraftChunkGeneratorWrapper.getPack();
        }
        return null;
    }
}
