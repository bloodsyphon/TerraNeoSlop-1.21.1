package com.dfsek.terra.mod.mixin_ifaces;

import java.util.List;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;


public interface FloraFeatureHolder {
    void setFloraFeatures(List<ConfiguredFeature<?, ?>> features);
}
