# TerraNeoSlop - A Terra fork made to work in NeoForge through the magic of AI vibe coding. 

This is a shoddy AI slop generated fork of Terra to get it working in NeoForge, hence the name. 

This version is particularly unique because it includes data packs designed for newer minecraft. 
A compatibility layer was added to Terra to fix those incompatiblities and I have no idea what 
the impact of that will be. Some biomes might be a bit off. Here are the replacements:
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

## Building and Running Terra

To build, simply run `./gradlew build` (`gradlew.bat build` on Windows). This
will build only the NeoForge platform. The other platforms have been disabled. 

## Contributing

Contributions are welcome! 

## Licensing

Parts of Terra are licensed under either the MIT License or the GNU General
Public License, version 3.0.

* Our API is licensed under the [MIT License](LICENSE), to ensure that everyone
  is able to freely use it however they want.
* Our core addons are also licensed under the [MIT License](LICENSE), to ensure
  that people can freely use code from them to learn and make their own addons,
  without worrying about GPL infection.
* Our platform-agnostic implementations and platform implementations are
  licensed under
  the [GNU General Public License, version 3.0](common/implementation/LICENSE),
  to ensure that they remain free software wherever they are used.

If you're not sure which license a particular file is under, check:

* The file's header
* The LICENSE file in the closest parent folder of the file in question

