package com.dfsek.terra.mod.mixin.implementations.terra.nbt;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;

import com.dfsek.terra.api.data.ExtendedData;
import net.minecraft.nbt.CompoundTag;


@Mixin(CompoundTag.class)
@Implements(@Interface(iface = ExtendedData.class, prefix = "terra$"))
public abstract class NbtCompoundMixin {
    @Intrinsic
    public String terra$toString() {
        return this.toString();
    }
}
