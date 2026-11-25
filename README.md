# Mega Rockets — 0.6.0 Alpha
Minecraft 1.21.1 Fabric, Java 21. Titan-1 is 64 blocks tall.

## Moon
Nominal radius 4096, diameter 8192. Minecraft 1.21.1 permits only 4064 blocks of vertical build height. The spherical terrain is split into three 3072-block-high dimensions with global Y = local Y + offset:
- megarockets:moon — northern region, offset +3072; landing pad local Y=1024.
- megarockets:moon_middle — equatorial region, offset 0.
- megarockets:moon_south — southern region, offset -3072.
All regions share the same sphere and deterministic terrain, but transitions between the regions are not seamless. Use operator commands to explore other regions. Crust is nominally 192 blocks thick with caves, ores, mountains and craters; the inner core is hollow. Gravity is 16.5% normal and points down, not toward the center. Chunk generation is on demand. Test in a new world; old terrain/height settings are incompatible.

Five original 32×32 pixel textures for lunar blocks; separate metal hull texture, 72 radial segments, thermal tiles, panels, windows, navigation lights, engine rings, landing feet and longer exhaust. Max ascent and lunar transfer speed 300 blocks/s at 20 TPS; landing speed stays low. Space path remains 6000 blocks and actual world coordinates; visual planets are separate sky geometry. The Moon is not physically in the orbit dimension; transfer before descent.

## Test
- /rocket spawn, /rocket launch, /rocket status, /rocket return.
- F5 for wide third-person camera; Shift after landing.
- /execute in megarockets:moon run tp @s 16.5 1024 0.5 (test landing pad).
- Creative/spectator only for direct tests of other regions, e.g. /execute in megarockets:moon_middle run tp @s 4200 0 0.
- Use a new world and low render distance initially; larger height and terrain may increase CPU/RAM usage.
Build: GitHub Actions → Build Mega Rockets → Artifacts → Mega-Rockets-jar. Requires Fabric API and Loader.
