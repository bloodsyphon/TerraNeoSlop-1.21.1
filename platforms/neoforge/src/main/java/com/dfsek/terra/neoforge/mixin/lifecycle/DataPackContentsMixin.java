package com.dfsek.terra.neoforge.mixin.lifecycle;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.ReloadableRegistries;
import net.minecraft.server.DataPackContents;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.dfsek.terra.mod.util.MinecraftUtil;
import com.dfsek.terra.mod.util.TagUtil;


@Mixin(DataPackContents.class)
public class DataPackContentsMixin {
    @Shadow
    @Final
    private ReloadableRegistries.Lookup reloadableRegistries;

    @Inject(method = {"applyPendingTagLoads()V", "updateStaticRegistryTags()V"}, at = @At("RETURN"), require = 0, remap = false)
    private void injectAfterTagLoads(CallbackInfo ci) {
        terra$afterRegistryReload();
    }

    @Unique
    private void terra$afterRegistryReload() {
        DynamicRegistryManager dynamicRegistryManager = terra$resolveRegistryManager();
        if(dynamicRegistryManager == null) {
            return;
        }

        TagUtil.registerWorldPresetTags(dynamicRegistryManager.getOrThrow(RegistryKeys.WORLD_PRESET));

        Registry<Biome> biomeRegistry = dynamicRegistryManager.getOrThrow(RegistryKeys.BIOME);
        TagUtil.registerBiomeTags(biomeRegistry);
        MinecraftUtil.registerFlora(biomeRegistry);
    }

    @Unique
    private DynamicRegistryManager terra$resolveRegistryManager() {
        try {
            RegistryWrapper.WrapperLookup lookup = this.reloadableRegistries.createRegistryLookup();
            if(lookup instanceof DynamicRegistryManager manager) {
                return manager;
            }
        } catch(Throwable ignored) {
            // fall through
        }

        try {
            Method method = this.reloadableRegistries.getClass().getMethod("getRegistryManager");
            Object value = method.invoke(this.reloadableRegistries);
            if(value instanceof DynamicRegistryManager manager) {
                return manager;
            }
        } catch(NoSuchMethodException ignored) {
            // no-op
        } catch(IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to resolve dynamic registry manager.", e);
        }

        return null;
    }
}
