/*
 * This file is part of Terra.
 *
 * Terra is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Terra is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Terra.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.dfsek.terra.neoforge;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.RegisterEvent;
import com.dfsek.terra.mod.data.Codecs;


@Mod("terra")
public class NeoForgeEntryPoint {
    private static final NeoForgePlatform TERRA_PLUGIN = new NeoForgePlatform();

    public NeoForgeEntryPoint(IEventBus modEventBus) {
        Bootstrap.bootStrap();
        modEventBus.addListener(this::onRegister);
    }

    private void onRegister(RegisterEvent event) {
        event.register(Registries.CHUNK_GENERATOR, Identifier.parse("terra:terra"), () -> Codecs.MINECRAFT_CHUNK_GENERATOR_WRAPPER);
        event.register(Registries.BIOME_SOURCE, Identifier.parse("terra:terra"), () -> Codecs.TERRA_BIOME_SOURCE);
    }

    public static NeoForgePlatform getPlatform() {
        return TERRA_PLUGIN;
    }
}
