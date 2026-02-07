package com.dfsek.terra.neoforge.mixin;


import net.minecraft.registry.entry.RegistryEntry.Reference;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;


@Mixin(Reference.class)
public interface RegistryEntryReferenceInvoker<T> {
    @Accessor("value")
    void terra$setValue(T value);
}
