# Mega Rockets — 0.7.1 Alpha
Minecraft 1.21.1 Fabric, Java 21. Titan-1 is 64 blocks tall.

## Moon
Nominal radius 4096, diameter 8192. Minecraft 1.21.1 permits only 4064 blocks of vertical build height. The spherical terrain is split into three 3072-block-high dimensions with global Y = local Y + offset:
- megarockets:moon — northern region, offset +3072; landing pad local Y=1280.
- megarockets:moon_middle — equatorial region, offset 0.
- megarockets:moon_south — southern region, offset -3072.
All regions share the same sphere and deterministic terrain, but transitions between the regions are not seamless. Use operator commands to explore other regions. Crust is nominally 192 blocks thick with caves, ores, mountains and craters; the inner core is hollow. Gravity is 16.5% normal and points down, not toward the center. Chunk generation is on demand. Test in a new world; old terrain/height settings are incompatible.

Five original 32×32 pixel textures for lunar blocks; separate metal hull texture, 72 radial segments, thermal tiles, panels, windows, navigation lights, engine rings, landing feet and longer exhaust. Max ascent and lunar transfer speed 300 blocks/s at 20 TPS; landing speed stays low. Space path remains 6000 blocks and actual world coordinates; visual planets are separate sky geometry. The Moon is not physically in the orbit dimension; transfer before descent.

## Test
- /rocket spawn, /rocket launch, /rocket status, /rocket return.
- F5 for wide third-person camera; Shift after landing.
- /execute in megarockets:moon run tp @s 16.5 1280 0.5 (test landing pad).
- Creative/spectator only for direct tests of other regions, e.g. /execute in megarockets:moon_middle run tp @s 4200 0 0.
- Use a new world and low render distance initially; larger height and terrain may increase CPU/RAM usage.
Build: GitHub Actions → Build Mega Rockets → Artifacts → Mega-Rockets-jar. Requires Fabric API and Loader.

## 0.6.1 Visual correction
Earth and Moon sky disks use real satellite map data, projected and shaded into 1024px PNGs at build time. Earth: NASA GSFC / Reto Stöckli, Robert Simmon and MODIS teams, Blue Marble 2002 (Wikimedia Commons archived 2048px image, public domain). Moon: NASA SVS CGI Moon Kit, Ernie Wright / LRO LROC. See ASSET_CREDITS.md. The Moon has no artificial atmosphere; the Earth limb is narrower, with an altitude-dependent apparent size. Exhaust is white/yellow/orange in all environments; vacuum changes its spread, not its palette. Existing worlds can be reused; terrain is unchanged. The disk images are fixed viewpoints, not live 3D terrain. Building requires internet access to the texture sources.

## 0.6.2 Lunar relief
Landing site is now a highland at local Y=1280, with a small 24-block-radius pad and surrounding terrain below it within 192 blocks. More irregular multiscale ridges, terraces, rock steps and debris. The old landing site's generated chunks are incompatible; use a NEW world (or a backed-up world with ungenerated lunar terrain). Autopilot refuses to land on old or obstructed landing blocks instead of overwriting them. Planet radius and flight speed unchanged.

## 0.7.0 Living Moon
Four registered lunar biomes (plains, highlands, crater fields, luminous forests) with multiscale seed-driven terrain and gradual relief transitions. Seed derived deterministically from the world-generation noise seed, shared across all three slices. New worlds with different seeds get different mountains, craters, caves, ores and trees; identical seeds reproduce terrain. Lunar trees have original bark/end-grain/leaf textures, custom logs and gently glowing leaves. Trees generate in forests and occasionally plains, never near the landing pad. Tree placement is deterministic across chunk borders. These trees are decorative world generation; saplings, recipes and regrowth are not implemented. Use a new world for the new biome source and terrain. The finite sphere, hollow core and existing landing site remain. This is a custom generator inspired by vanilla, not the vanilla Overworld generator.

## 0.7.1 startup hotfix
Register lunar wood using PillarBlock (axis) and foliage using LeavesBlock (distance, persistence, waterlogging), matching the copied vanilla settings. Generated foliage is persistent. Add log-axis models and vanilla log/leaf tags. Existing 0.7.0 terrain settings and seeds are unchanged.
