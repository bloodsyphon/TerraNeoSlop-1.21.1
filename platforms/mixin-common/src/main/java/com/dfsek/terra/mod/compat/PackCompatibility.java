package com.dfsek.terra.mod.compat;

import java.util.Map;

public final class PackCompatibility {
    private static final Map<String, String> BIOME_ALIASES = Map.of(
        "minecraft:pale_garden", "minecraft:dark_forest"
    );

    private static final Map<String, String> BLOCK_ALIASES = Map.ofEntries(
        Map.entry("minecraft:iron_chain", "minecraft:chain"),
        Map.entry("minecraft:closed_eyeblossom", "minecraft:spore_blossom"),
        Map.entry("minecraft:firefly_bush", "minecraft:fern"),
        Map.entry("minecraft:cactus_flower", "minecraft:cactus"),
        Map.entry("minecraft:short_dry_grass", "minecraft:short_grass"),
        Map.entry("minecraft:tall_dry_grass", "minecraft:tall_grass"),
        Map.entry("minecraft:pale_oak_log", "minecraft:oak_log"),
        Map.entry("minecraft:pale_oak_wood", "minecraft:oak_wood"),
        Map.entry("minecraft:stripped_pale_oak_log", "minecraft:stripped_oak_log"),
        Map.entry("minecraft:stripped_pale_oak_wood", "minecraft:stripped_oak_wood"),
        Map.entry("minecraft:pale_oak_leaves", "minecraft:oak_leaves"),
        Map.entry("minecraft:pale_moss_block", "minecraft:moss_block"),
        Map.entry("minecraft:pale_hanging_moss", "minecraft:hanging_roots"),
        Map.entry("minecraft:bush", "minecraft:dead_bush")
    );

    private PackCompatibility() { }

    public static String normalizeBiomeId(String raw) {
        return BIOME_ALIASES.getOrDefault(raw, raw);
    }

    public static String normalizeParticle(String raw) {
        if(raw == null) return null;
        if(raw.startsWith("minecraft:trail")) return "minecraft:ash";
        return raw.replace("target:[0.5,85,0.5]", "target:[0.5,85.0,0.5]");
    }

    public static String normalizeBlockState(String raw) {
        String out = raw;
        for(Map.Entry<String, String> entry : BLOCK_ALIASES.entrySet()) {
            out = out.replace(entry.getKey(), entry.getValue());
        }

        // 1.21.1 cannot parse pale/multi-property moss carpet states from newer packs.
        out = out.replaceAll("minecraft:pale_moss_carpet\\[[^\\]]*\\]", "minecraft:moss_carpet");
        out = out.replace("minecraft:pale_moss_carpet", "minecraft:moss_carpet");

        // Keep leaf litter where available, but strip incompatible state keys.
        out = out.replaceAll("minecraft:leaf_litter\\[[^\\]]*\\]", "minecraft:leaf_litter");

        return out;
    }
}
