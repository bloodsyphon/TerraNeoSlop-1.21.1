package com.dfsek.terra.lifecycle.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.MappedRegistry;
import com.dfsek.terra.lifecycle.util.RegistryHack;


@Mixin(MappedRegistry.class)
public class SimpleRegistryMixin<T> implements RegistryHack {
    @Shadow
    @Final
    private Map<T, Reference<T>> byValue;

    @Override
    public void terra_bind() {
        byValue.forEach((value, entry) -> {
            //noinspection unchecked
            ((RegistryEntryReferenceInvoker<T>) entry).invokeSetValue(value);
        });
    }
}
