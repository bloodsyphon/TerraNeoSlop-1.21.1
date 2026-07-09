package com.dfsek.terra.mod.implmentation;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import com.dfsek.terra.api.util.range.Range;
import com.dfsek.terra.mod.data.Codecs;
import com.dfsek.terra.mod.util.MinecraftAdapter;


public class TerraIntProvider implements IntProvider {
    public Range delegate;

    public TerraIntProvider(Range delegate) {
        this.delegate = delegate;
    }

    @Override
    public int sample(RandomSource random) {
        return delegate.get(MinecraftAdapter.adapt(random));
    }

    @Override
    public int minInclusive() {
        return delegate.getMin();
    }

    @Override
    public int maxInclusive() {
        return delegate.getMax();
    }

    @Override
    public MapCodec<? extends IntProvider> codec() {
        return Codecs.TERRA_CONSTANT_RANGE_INT_PROVIDER_TYPE;
    }
}
