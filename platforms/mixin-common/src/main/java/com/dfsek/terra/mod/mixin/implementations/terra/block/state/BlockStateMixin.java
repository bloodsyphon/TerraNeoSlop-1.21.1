package com.dfsek.terra.mod.mixin.implementations.terra.block.state;


import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.block.state.StateHolder;
import com.dfsek.terra.api.block.BlockType;
import com.dfsek.terra.api.block.state.BlockState;
import com.dfsek.terra.api.block.state.properties.Property;


@Mixin(BlockStateBase.class)
@Implements(@Interface(iface = BlockState.class, prefix = "terra$"))
public abstract class BlockStateMixin extends StateHolder<Block, net.minecraft.world.level.block.state.BlockState> {
    private BlockStateMixin(Block owner, net.minecraft.world.level.block.state.properties.Property<?>[] properties,
                            Comparable<?>[] values) {
        super(owner, properties, values);
    }

    @Shadow
    public abstract Block getBlock();

    @Shadow
    public abstract boolean isAir();

    public boolean terra$matches(BlockState other) {
        return getBlock() == ((net.minecraft.world.level.block.state.BlockState) other).getBlock();
    }

    @Intrinsic
    public <T extends Comparable<T>> boolean terra$has(Property<T> property) {
        if(property instanceof net.minecraft.world.level.block.state.properties.Property<?> minecraftProperty) {
            return hasProperty(minecraftProperty);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Intrinsic
    public <T extends Comparable<T>> T terra$get(Property<T> property) {
        return getValue((net.minecraft.world.level.block.state.properties.Property<T>) property);
    }

    @SuppressWarnings("unchecked")
    @Intrinsic
    public <T extends Comparable<T>> BlockState terra$set(Property<T> property, T value) {
        return (BlockState) setValue((net.minecraft.world.level.block.state.properties.Property<T>) property, value);
    }

    @Intrinsic
    public BlockType terra$getBlockType() {
        return (BlockType) getBlock();
    }

    @Intrinsic
    public String terra$getAsString(boolean properties) {
        StringBuilder data = new StringBuilder(BuiltInRegistries.BLOCK.getKey(getBlock()).toString());
        if(properties && getValues().findAny().isPresent()) {
            data.append('[');
            data.append(
                getValues().map(BlockStateMixin::formatPropertyEntry).collect(Collectors.joining(",")));
            data.append(']');
        }
        return data.toString();
    }

    private static String formatPropertyEntry(net.minecraft.world.level.block.state.properties.Property.Value<?> entry) {
        return propertyToString(entry.property(), entry.value());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static String propertyToString(net.minecraft.world.level.block.state.properties.Property property, Comparable value) {
        return property.getName() + "=" + property.getName(value);
    }

    @Intrinsic
    public boolean terra$isAir() {
        return isAir();
    }
}
