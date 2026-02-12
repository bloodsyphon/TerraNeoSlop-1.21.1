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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.dfsek.terra.mod.util.MinecraftUtil;
import com.dfsek.terra.mod.util.TagUtil;


@Mixin(DataPackContents.class)
public class DataPackContentsMixin {
    @Shadow
    @Final
    private ReloadableRegistries.Lookup reloadableRegistries;

    @Inject(method = "applyPendingTagLoads()V", at = @At("RETURN"), remap = false)
    private void injectAfterTagLoads(CallbackInfo ci) {
        RegistryWrapper.WrapperLookup lookup = this.reloadableRegistries.createRegistryLookup();
        if(lookup instanceof DynamicRegistryManager dynamicRegistryManager) {
            TagUtil.registerWorldPresetTags(dynamicRegistryManager.getOrThrow(RegistryKeys.WORLD_PRESET));

            Registry<Biome> biomeRegistry = dynamicRegistryManager.getOrThrow(RegistryKeys.BIOME);
            TagUtil.registerBiomeTags(biomeRegistry);
            MinecraftUtil.registerFlora(biomeRegistry);
        }
    }
}
