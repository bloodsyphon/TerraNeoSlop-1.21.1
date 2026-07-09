package com.dfsek.terra.neoforge.mixin.lifecycle;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.world.level.biome.Biome;
import com.dfsek.terra.mod.util.MinecraftUtil;
import com.dfsek.terra.mod.util.TagUtil;


@Mixin(ReloadableServerResources.class)
public class DataPackContentsMixin {
    @Shadow
    @Final
    private ReloadableServerRegistries.Holder fullRegistryHolder;

    @Inject(method = "updateComponentsAndStaticRegistryTags()V", at = @At("RETURN"), require = 0, remap = false)
    private void injectAfterTagLoads(CallbackInfo ci) {
        terra$afterRegistryReload();
    }

    @Unique
    private void terra$afterRegistryReload() {
        RegistryAccess dynamicRegistryManager = terra$resolveRegistryManager();
        if(dynamicRegistryManager == null) {
            return;
        }

        TagUtil.registerWorldPresetTags(dynamicRegistryManager.lookupOrThrow(Registries.WORLD_PRESET));

        Registry<Biome> biomeRegistry = dynamicRegistryManager.lookupOrThrow(Registries.BIOME);
        TagUtil.registerBiomeTags(biomeRegistry);
        MinecraftUtil.registerFlora(biomeRegistry);
    }

    @Unique
    private RegistryAccess terra$resolveRegistryManager() {
        try {
            HolderLookup.Provider lookup = this.fullRegistryHolder.lookup();
            if(lookup instanceof RegistryAccess manager) {
                return manager;
            }
        } catch(Throwable ignored) {
            // fall through
        }

        try {
            Method method = this.fullRegistryHolder.getClass().getMethod("getRegistryManager");
            Object value = method.invoke(this.fullRegistryHolder);
            if(value instanceof RegistryAccess manager) {
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
