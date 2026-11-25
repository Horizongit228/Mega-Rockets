# Mega Rockets — 0.8.1 Alpha
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

## 0.7.2 Solar horizon
A circular procedural Sun with shaded warm photosphere and softly animated corona appears during ascent and in space/lunar skies. It is a visual sky body, not a flyable destination; its direction is fixed and it does not alter world lighting. Lunar forests now have round, spire-shaped and ancient broad-canopy trees. Added deterministic basalt boulders, exposed crystal-bearing rocks, layered basalt and clustered ore veins, and larger underground galleries. The landing exclusion area, world seed, 4096 radius and flight mechanics remain unchanged. Existing generated chunks are not modified; create a new world for uniform terrain, or explore new chunks in a backed-up save. No new registered block types in this update.

## 0.7.3 Creative inventory & smoother terrain
All seven lunar blocks appear in the dedicated Mega Rockets creative tab and creative search. Reduced fine-scale height noise, widened hills and softened mountain ridges and crater walls. The landing highland now has a broader, more gradual slope, with the protected 192-block radius preserved. Biomes, world seed, caves, boulders, trees, Sun, lunar dimensions and flight speed remain unchanged. Existing chunks stay as generated; use a new world for consistent new relief. The rocket still spawns with /rocket spawn.

## 0.7.4 Lunar wood building set
Nine craftable building blocks: planks, stairs, slabs, fences, fence gates, doors, trapdoors, buttons, pressure plates. Standard Minecraft shapes, redstone behavior and crafting ratios, own blue-grey plank texture, creative tab, recipe unlocks, loot and vanilla wooden tags. Lunar logs craft four planks; signs, boats, saplings and stripped logs are not part of this release. No terrain changes; existing worlds can be used. Back up saves before updating.

## 0.7.5 Rocket item & lunar plains
Titan-1 is now a creative inventory item in the Mega Rockets tab. Right-click the top of a block to deploy the real 14x64 rocket ten blocks farther in the direction you face, on the same level; the center must have solid support and the volume must be empty. In orbit, right-click air to deploy 16 blocks ahead. Works in any dimension on a suitable surface, without replacing blocks. Creative sneak-right-click on an empty idle rocket removes it. Launch mission still starts in the Overworld, and the existing lunar flight/return commands are unchanged. Lunar plains occupy larger areas and have reduced relief, and tree spacing is 12 rather than 16 blocks, with higher placement probabilities. Landing clearance remains protected. Use a new world for consistent terrain; new items also work in existing worlds.

## 0.8.0 Interplanetary flight
Three rocket items: Titan-1 300, Phantom 600, Hyperion 1000 blocks/s at 20 TPS. New variants have separate entity and item IDs, colored hulls, modified hull contours and longer plumes. All use the same 14x64 collision envelope and observation seat. Select Moon or Mars in clickable chat after boarding or /rocket destination moon|mars, then /rocket launch. Default is Moon. Destination and rocket type persist across dimension transfers and saves. Mars is a continuous seeded terrain dimension, not yet a finite physical sphere, with red plains, canyons, craters, caves, 3 custom blocks using vanilla textures, a protected landing platform and 38% gravity. Distances are game-scale 6000 and 24000 blocks, with separate orbit lanes. Flight starts from Earth only; return command remains an emergency teleport. No fuel or survival crafting for rockets. Existing lunar terrain unchanged; back up saves before updating.

## 0.8.1 Caves & Martian peaks
Original 32x32 Martian ore texture: rust-red matrix and warm golden mineral clusters instead of vanilla copper ore. Both planets now have seeded continuous shallow cave networks, 6–10 block surface shafts opening into galleries, larger junction chambers, vertical connections and additional deep tunnels. No new cave openings within 216 blocks of the landing center; pad foundation strengthened. Mars has rare mountains: approximately three peaks per four 1024x1024 cells, radius 190–299 blocks and 320–479 blocks of added relief at their centers, with an initial peak near the landing region. Mars build height expanded to 768 blocks (-64 through 703); use a NEW world for compatible dimension settings and terrain. Existing generated chunks are not rewritten. Rocket flight unchanged.

## 0.8.2 Finite Mars & lunar masonry
Mars has a finite circular terrain footprint (radius 8192, diameter 16384), with empty space beyond the rim; not a physical sphere. Ten seed-dependent, nonoverlapping mountain centers, 700–1200 blocks across, up to 456 blocks of added elevation. Existing caves and landing coordinates retained. Lunar weather gradients are suppressed and natural snow/ice ticking is disabled in lunar dimensions only; old snow blocks are not automatically deleted. Four craftable lunar masonry blocks and a reworked 32px lunar plank texture. New world recommended for changed Mars generation; existing chunks are not erased.
