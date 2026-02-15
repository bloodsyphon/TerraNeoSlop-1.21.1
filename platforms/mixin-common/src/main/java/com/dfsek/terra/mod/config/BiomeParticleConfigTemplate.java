package com.dfsek.terra.mod.config;

import com.dfsek.tectonic.api.config.template.annotations.Default;
import com.dfsek.tectonic.api.config.template.annotations.Value;
import com.dfsek.tectonic.api.config.template.object.ObjectTemplate;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.ParticleEffectArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.biome.BiomeParticleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Stream;

import com.dfsek.terra.mod.compat.PackCompatibility;


public class BiomeParticleConfigTemplate implements ObjectTemplate<BiomeParticleConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BiomeParticleConfigTemplate.class);

    @Value("particle")
    @Default
    private String particle = null;

    @Value("probability")
    @Default
    private Float probability = 0.1f;

    @Override
    public BiomeParticleConfig get() {
        if(particle == null || probability == null) {
            return null;
        }

        String normalized = PackCompatibility.normalizeParticle(particle);
        try {
            return new BiomeParticleConfig(
                ParticleEffectArgumentType.readParameters(new StringReader(normalized),
                    RegistryWrapper.WrapperLookup.of(Stream.of(Registries.PARTICLE_TYPE.getReadOnlyWrapper()))),
                probability);
        } catch(CommandSyntaxException e) {
            LOGGER.warn("Unknown particle config '{}'; skipping particle effect for compatibility.", particle);
            return null;
        }
    }
}
