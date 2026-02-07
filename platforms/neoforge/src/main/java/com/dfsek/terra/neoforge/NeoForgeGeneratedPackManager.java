package com.dfsek.terra.neoforge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackInfo;
import net.minecraft.resource.ResourcePackPosition;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import com.dfsek.terra.api.config.ConfigPack;
import com.dfsek.terra.api.config.MetaPack;
import com.dfsek.terra.api.util.range.ConstantRange;
import com.dfsek.terra.mod.config.VanillaWorldProperties;


final class NeoForgeGeneratedPackManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(NeoForgeGeneratedPackManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Identifier PACK_ID = Identifier.of("terra", "generated_world_presets");
    private static final String PACK_TITLE = "Terra Generated World Presets";
    private static final String NORMAL_PRESET_TAG = "normal";
    private static final String EXTENDED_PRESET_TAG = "extended";

    private NeoForgeGeneratedPackManager() {
    }

    static Optional<ResourcePackProfile> createPack(NeoForgePlatform platform) {
        Path packRoot = platform.getDataFolder().toPath().resolve("generated-datapacks").resolve("world-presets");
        LOGGER.info("Generating Terra world preset datapack at {}", packRoot);
        try {
            writePack(packRoot, platform);
        } catch(IOException e) {
            LOGGER.error("Failed to generate Terra world preset datapack at {}", packRoot, e);
            return Optional.empty();
        }

        ResourcePackInfo locationInfo = new ResourcePackInfo(
            "mod/" + PACK_ID,
            net.minecraft.text.Text.literal(PACK_TITLE),
            ResourcePackSource.BUILTIN,
            Optional.empty()
        );

        ResourcePackProfile.PackFactory supplier = new ResourcePackProfile.PackFactory() {
            @Override
            public ResourcePack open(ResourcePackInfo info) {
                return new DirectoryResourcePack(info, packRoot);
            }

            @Override
            public ResourcePack openWithOverlays(ResourcePackInfo info, ResourcePackProfile.Metadata metadata) {
                return new DirectoryResourcePack(info, packRoot);
            }
        };

        ResourcePackProfile pack = ResourcePackProfile.create(
            locationInfo,
            supplier,
            ResourceType.SERVER_DATA,
            new ResourcePackPosition(true, ResourcePackProfile.InsertionPosition.TOP, false)
        );

        if(pack == null) {
            LOGGER.error("Generated Terra world preset datapack was invalid and could not be loaded.");
            return Optional.empty();
        }
        return Optional.of(pack);
    }

    private static void writePack(Path packRoot, NeoForgePlatform platform) throws IOException {
        Files.createDirectories(packRoot);
        AtomicLong configPackCount = new AtomicLong();
        platform.getRawConfigRegistry().forEach(pack -> configPackCount.incrementAndGet());
        AtomicLong metaPackCount = new AtomicLong();
        platform.getRawMetaConfigRegistry().forEach(pack -> metaPackCount.incrementAndGet());
        LOGGER.info("Writing generated Terra world presets using {} config packs and {} metapacks.",
            configPackCount.get(), metaPackCount.get());

        Path worldPresetRoot = packRoot.resolve("data").resolve("terra").resolve("worldgen").resolve("world_preset");
        deleteDirectory(worldPresetRoot);
        Files.createDirectories(worldPresetRoot);

        writePackMeta(packRoot);

        Set<String> configPacksInMetaPack = new HashSet<>();
        Set<Identifier> generatedPresetIds = new LinkedHashSet<>();
        platform.getRawMetaConfigRegistry().forEach(metaPack -> {
            generatedPresetIds.add(writeMetaPackPreset(worldPresetRoot, metaPack));
            metaPack.packs().forEach((key, pack) -> configPacksInMetaPack.add(pack.getID()));
        });

        platform.getRawConfigRegistry().forEach(pack -> {
            if(!configPacksInMetaPack.contains(pack.getID())) {
                Identifier id = createPresetId(pack.getID(), pack.getNamespace());
                writePreset(worldPresetRoot, id, createDefaultPreset(pack));
                generatedPresetIds.add(id);
            }
        });

        if(generatedPresetIds.isEmpty()) {
            LOGGER.warn("No Terra config/metapacks were loaded at pack finder time; generating fallback presets from pack archives.");
            generateFallbackPresets(worldPresetRoot, platform, generatedPresetIds);
        }

        writePresetTags(packRoot, generatedPresetIds);
    }

    private static void writePackMeta(Path packRoot) throws IOException {
        JsonObject root = new JsonObject();
        JsonObject pack = new JsonObject();
        int dataPackVersion = SharedConstants.DATA_PACK_VERSION;
        pack.addProperty("pack_format", dataPackVersion);
        pack.addProperty("min_format", dataPackVersion);
        pack.addProperty("max_format", dataPackVersion);
        pack.addProperty("description", PACK_TITLE);
        root.add("pack", pack);
        writeJson(packRoot.resolve("pack.mcmeta"), root);
    }

    private static Identifier writeMetaPackPreset(Path worldPresetRoot, MetaPack metaPack) {
        Identifier id = createPresetId(metaPack.getID(), metaPack.getNamespace());
        JsonObject preset = new JsonObject();
        JsonObject dimensions = new JsonObject();

        metaPack.packs().forEach((dimensionKey, pack) -> dimensions.add(dimensionKey, createTerraDimension(pack)));
        addDefaultDimensions(dimensions);

        preset.add("dimensions", dimensions);
        writePreset(worldPresetRoot, id, preset);
        return id;
    }

    private static void writePresetTags(Path packRoot, Set<Identifier> presetIds) throws IOException {
        Path tagRoot = packRoot.resolve("data").resolve("minecraft").resolve("tags").resolve("worldgen").resolve("world_preset");
        deleteDirectory(tagRoot);
        Files.createDirectories(tagRoot);

        writePresetTag(tagRoot.resolve(NORMAL_PRESET_TAG + ".json"), presetIds);
        writePresetTag(tagRoot.resolve(EXTENDED_PRESET_TAG + ".json"), presetIds);
        LOGGER.info("Generated world preset tags with {} entries.", presetIds.size());
    }

    private static void writePresetTag(Path file, Set<Identifier> presetIds) throws IOException {
        JsonObject tag = new JsonObject();
        tag.addProperty("replace", false);

        JsonArray values = new JsonArray();
        presetIds.forEach(id -> values.add(id.toString()));
        tag.add("values", values);
        writeJson(file, tag);
    }

    private static JsonObject createDefaultPreset(ConfigPack pack) {
        JsonObject preset = new JsonObject();
        JsonObject dimensions = new JsonObject();
        dimensions.add("minecraft:overworld", createTerraDimension(pack));
        addDefaultDimensions(dimensions);
        preset.add("dimensions", dimensions);
        return preset;
    }

    private static JsonObject createDefaultPreset(String namespace, String id) {
        JsonObject preset = new JsonObject();
        JsonObject dimensions = new JsonObject();
        dimensions.add("minecraft:overworld", createTerraDimension(namespace, id));
        addDefaultDimensions(dimensions);
        preset.add("dimensions", dimensions);
        return preset;
    }

    private static JsonObject createTerraDimension(ConfigPack pack) {
        VanillaWorldProperties properties = pack.getContext().has(VanillaWorldProperties.class)
            ? pack.getContext().get(VanillaWorldProperties.class)
            : new VanillaWorldProperties();

        JsonObject dimension = new JsonObject();
        dimension.addProperty("type", properties.getVanillaDimension());
        dimension.add("generator", createTerraGenerator(pack, properties));
        return dimension;
    }

    private static JsonObject createTerraDimension(String namespace, String id) {
        JsonObject dimension = new JsonObject();
        dimension.addProperty("type", "minecraft:overworld");
        dimension.add("generator", createTerraGenerator(namespace, id));
        return dimension;
    }

    private static JsonObject createTerraGenerator(ConfigPack pack, VanillaWorldProperties properties) {
        Identifier packId = Identifier.of(pack.getNamespace(), pack.getID());

        GenerationDefaults defaults = GenerationDefaults.forVanillaGeneration(properties.getVanillaGeneration());
        ConstantRange range = properties.getHeight();
        int min = range == null ? defaults.minY : range.getMin();
        int max = range == null ? defaults.maxY : range.getMax();

        JsonObject settings = new JsonObject();
        JsonObject height = new JsonObject();
        height.addProperty("min", min);
        height.addProperty("max", max);
        settings.add("height", height);
        settings.addProperty("sea_level", properties.getSealevel() == null ? defaults.seaLevel : properties.getSealevel());
        settings.addProperty("mob_generation", properties.getMobGeneration() == null ? defaults.mobGeneration : properties.getMobGeneration());
        settings.addProperty("spawn_height", properties.getSpawnHeight());

        JsonObject packObject = createConfigPackCodecObject(packId.getNamespace(), packId.getPath());

        JsonObject biomeSource = new JsonObject();
        biomeSource.addProperty("type", "terra:terra");
        biomeSource.add("pack", packObject.deepCopy());

        JsonObject generator = new JsonObject();
        generator.addProperty("type", "terra:terra");
        generator.add("biome_source", biomeSource);
        generator.add("pack", packObject);
        generator.add("settings", settings);
        return generator;
    }

    private static JsonObject createTerraGenerator(String namespace, String id) {
        JsonObject settings = new JsonObject();
        JsonObject height = new JsonObject();
        height.addProperty("min", GenerationDefaults.OVERWORLD.minY());
        height.addProperty("max", GenerationDefaults.OVERWORLD.maxY());
        settings.add("height", height);
        settings.addProperty("sea_level", GenerationDefaults.OVERWORLD.seaLevel());
        settings.addProperty("mob_generation", GenerationDefaults.OVERWORLD.mobGeneration());
        settings.addProperty("spawn_height", GenerationDefaults.OVERWORLD.seaLevel());

        JsonObject packObject = createConfigPackCodecObject(namespace, id);

        JsonObject biomeSource = new JsonObject();
        biomeSource.addProperty("type", "terra:terra");
        biomeSource.add("pack", packObject.deepCopy());

        JsonObject generator = new JsonObject();
        generator.addProperty("type", "terra:terra");
        generator.add("biome_source", biomeSource);
        generator.add("pack", packObject);
        generator.add("settings", settings);
        return generator;
    }

    private static JsonObject createConfigPackCodecObject(String namespace, String id) {
        JsonObject registryKey = new JsonObject();
        registryKey.addProperty("namespace", namespace);
        registryKey.addProperty("id", id);

        JsonObject packObject = new JsonObject();
        packObject.add("pack", registryKey);
        return packObject;
    }

    private static void addDefaultDimensions(JsonObject dimensions) {
        if(!dimensions.has("minecraft:overworld")) {
            dimensions.add("minecraft:overworld", createVanillaDimension("minecraft:overworld", "minecraft:overworld", "minecraft:multi_noise", "minecraft:overworld"));
        }
        if(!dimensions.has("minecraft:the_end")) {
            dimensions.add("minecraft:the_end", createVanillaDimension("minecraft:the_end", "minecraft:end", "minecraft:the_end", null));
        }
        if(!dimensions.has("minecraft:the_nether")) {
            dimensions.add("minecraft:the_nether", createVanillaDimension("minecraft:the_nether", "minecraft:nether", "minecraft:multi_noise", "minecraft:nether"));
        }
    }

    private static JsonObject createVanillaDimension(String type, String settings, String biomeSourceType, String biomePreset) {
        JsonObject dimension = new JsonObject();
        JsonObject generator = new JsonObject();
        JsonObject biomeSource = new JsonObject();

        dimension.addProperty("type", type);

        generator.addProperty("type", "minecraft:noise");
        generator.addProperty("settings", settings);

        biomeSource.addProperty("type", biomeSourceType);
        if(biomePreset != null) {
            biomeSource.addProperty("preset", biomePreset);
        }
        generator.add("biome_source", biomeSource);

        dimension.add("generator", generator);
        return dimension;
    }

    private static void writePreset(Path worldPresetRoot, Identifier id, JsonObject preset) {
        Path file = worldPresetRoot.resolve(id.getPath() + ".json");
        try {
            Files.createDirectories(file.getParent());
            writeJson(file, preset);
            LOGGER.info("Generated Terra world preset {}", id);
        } catch(IOException e) {
            LOGGER.error("Failed to write Terra world preset {}", id, e);
        }
    }

    private static void writeJson(Path path, JsonObject jsonObject) throws IOException {
        Files.writeString(path, GSON.toJson(jsonObject), StandardCharsets.UTF_8);
    }

    private static void generateFallbackPresets(Path worldPresetRoot, NeoForgePlatform platform, Set<Identifier> generatedPresetIds) {
        Path packsDir = platform.getDataFolder().toPath().resolve("packs");
        if(!Files.isDirectory(packsDir)) {
            LOGGER.warn("Fallback preset generation skipped; packs directory does not exist: {}", packsDir);
            return;
        }

        try(Stream<Path> stream = Files.list(packsDir)) {
            stream.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip"))
                .sorted()
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    String token = fileName.substring(0, fileName.length() - 4).toLowerCase(Locale.ROOT);
                    Identifier id = createPresetId(token, token);
                    writePreset(worldPresetRoot, id, createDefaultPreset(token, token));
                    generatedPresetIds.add(id);
                });
        } catch(IOException e) {
            LOGGER.error("Failed to discover fallback Terra presets from {}", packsDir, e);
        }

        Identifier overworldPreset = createPresetId("overworld", "overworld");
        Identifier defaultPreset = createPresetId("default", "default");
        if(generatedPresetIds.contains(overworldPreset) && !generatedPresetIds.contains(defaultPreset)) {
            writePreset(worldPresetRoot, defaultPreset, createDefaultPreset("overworld", "overworld"));
            generatedPresetIds.add(defaultPreset);
        }

        LOGGER.info("Generated {} fallback Terra world presets.", generatedPresetIds.size());
    }

    private static Identifier createPresetId(String id, String namespace) {
        return Identifier.of("terra", id.toLowerCase() + "/" + namespace.toLowerCase());
    }

    private static void deleteDirectory(Path root) throws IOException {
        if(!Files.exists(root)) return;
        try(Stream<Path> stream = Files.walk(root)) {
            stream.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch(IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch(RuntimeException e) {
            if(e.getCause() instanceof IOException ioException) {
                throw ioException;
            }
            throw e;
        }
    }

    private record GenerationDefaults(int minY, int maxY, int seaLevel, boolean mobGeneration) {
        private static final GenerationDefaults OVERWORLD = new GenerationDefaults(-64, 384, 63, true);
        private static final GenerationDefaults NETHER = new GenerationDefaults(0, 128, 32, true);
        private static final GenerationDefaults END = new GenerationDefaults(0, 256, 0, true);

        private static GenerationDefaults forVanillaGeneration(String generation) {
            return switch(generation) {
                case "minecraft:nether" -> NETHER;
                case "minecraft:end" -> END;
                default -> OVERWORLD;
            };
        }
    }
}
