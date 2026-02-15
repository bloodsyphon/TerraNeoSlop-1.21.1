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

package com.dfsek.terra.mod.handle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.command.argument.BlockArgumentParser.BlockResult;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.block.state.BlockStateExtended;
import com.dfsek.terra.api.entity.EntityType;
import com.dfsek.terra.api.handle.WorldHandle;
import com.dfsek.terra.mod.implmentation.MinecraftEntityTypeExtended;

import static net.minecraft.command.argument.BlockArgumentParser.INVALID_BLOCK_ID_EXCEPTION;


public class MinecraftWorldHandle implements WorldHandle {

    private static final BlockState AIR = (BlockState) Blocks.AIR.getDefaultState();

    private static final Logger logger = LoggerFactory.getLogger(MinecraftWorldHandle.class);

    @SuppressWarnings("DataFlowIssue")
    @Override
    public @NotNull BlockState createBlockState(@NotNull String data) {
        try {
            if(data.equals("minecraft:grass")) {
                data = "minecraft:short_grass";
                logger.warn(
                    "Translating minecraft:grass to minecraft:short_grass. In 1.20.3 minecraft:grass was renamed to minecraft:short_grass");
            }
            if(data.equals("minecraft:chain")) {
                data = "minecraft:iron_chain";
                logger.warn(
                    "Translating minecraft:chain to minecraft:iron_chain. In 1.21.11 minecraft:chain was renamed to minecraft:iron_chain");
            }
            BlockResult blockResult = BlockArgumentParser.block(Registries.BLOCK, data, true);
            BlockState blockState;
            if(blockResult.nbt() != null) {
                net.minecraft.block.BlockState state = blockResult.blockState();
                NbtCompound nbtCompound = blockResult.nbt();
                if(state.hasBlockEntity()) {
                    BlockEntity blockEntity = ((BlockEntityProvider) state.getBlock()).createBlockEntity(new BlockPos(0, 0, 0), state);

                    nbtCompound.putInt("x", 0);
                    nbtCompound.putInt("y", 0);
                    nbtCompound.putInt("z", 0);

                    Identifier blockEntityId = Registries.BLOCK_ENTITY_TYPE.getId(blockEntity.getType());
                    if(blockEntityId == null) {
                        throw new IllegalArgumentException("Unable to resolve block entity id for " + blockEntity.getType());
                    }
                    nbtCompound.putString("id", blockEntityId.toString());

                    blockState = (BlockStateExtended) new BlockStateArgument(state, blockResult.properties().keySet(), nbtCompound);
                } else {
                    blockState = (BlockState) state;
                }

            } else {
                blockState = (BlockState) blockResult.blockState();
            }

            if(blockState == null) throw new IllegalArgumentException("Invalid data: " + data);
            return blockState;
        } catch(CommandSyntaxException e) {
            logger.warn("Unknown block state '{}'; falling back to air for 1.21.11 compatibility.", data);
            return AIR;
        } catch(IllegalArgumentException e) {
            if(e.getCause() instanceof CommandSyntaxException) {
                logger.warn("Unknown block state '{}'; falling back to air for 1.21.11 compatibility.", data);
                return AIR;
            }
            throw e;
        } catch(RuntimeException e) {
            if(e.getCause() instanceof CommandSyntaxException) {
                logger.warn("Unknown block state '{}'; falling back to air for 1.21.11 compatibility.", data);
                return AIR;
            }
            throw e;
        } catch(Exception e) {
            logger.warn("Unknown block state '{}'; falling back to air for 1.21.11 compatibility.", data);
            return AIR;
        }
    }

    @Override
    public @NotNull BlockState air() {
        return (BlockState) Blocks.AIR.getDefaultState();
    }

    @Override
    public @NotNull EntityType getEntity(@NotNull String data) {
        try {
            Identifier identifier;
            NbtCompound nbtData = null;
            StringReader reader = new StringReader(data);

            int i = reader.getCursor();

            identifier = Identifier.fromCommandInput(reader);

            net.minecraft.entity.EntityType<?> entity =
                (net.minecraft.entity.EntityType<?>) ((Reference<?>) Registries.ENTITY_TYPE.getOptional(
                    RegistryKey.of(RegistryKeys.ENTITY_TYPE, identifier)).orElseThrow(() -> {
                    reader.setCursor(i);
                    return INVALID_BLOCK_ID_EXCEPTION.createWithContext(reader, identifier.toString());
                })).value();

            if(reader.canRead() && reader.peek() == '{') {
                nbtData = StringNbtReader.readCompoundAsArgument(reader);
                nbtData.putString("id", entity.getRegistryEntry().registryKey().getValue().toString());
            }

            EntityType entityType;
            if(nbtData != null) {
                entityType = new MinecraftEntityTypeExtended(entity, nbtData);
            } else {
                entityType = (EntityType) entity;
            }

            if(identifier == null) throw new IllegalArgumentException("Invalid data: " + data);
            return entityType;
        } catch(CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
