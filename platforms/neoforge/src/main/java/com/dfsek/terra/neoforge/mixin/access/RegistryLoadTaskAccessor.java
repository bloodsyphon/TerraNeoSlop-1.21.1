package com.dfsek.terra.neoforge.mixin.access;

import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.RegistryLoadTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;


@Mixin(RegistryLoadTask.class)
public interface RegistryLoadTaskAccessor<T> {
    @Accessor("registry")
    WritableRegistry<T> terra$getRegistry();
}
