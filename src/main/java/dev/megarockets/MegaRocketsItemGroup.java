package dev.megarockets;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public final class MegaRocketsItemGroup {
    private MegaRocketsItemGroup() {}
    public static void init() {
        Registry.register(Registries.ITEM_GROUP, MegaRockets.id("lunar_blocks"),
            FabricItemGroup.builder()
                .displayName(Text.translatable("itemGroup.megarockets.lunar_blocks"))
                .icon(() -> new ItemStack(RocketItems.TITAN))
                .entries((context, entries) -> {
                    entries.add(RocketItems.TITAN);
                    entries.add(RocketItems.PHANTOM);
                    entries.add(RocketItems.HYPERION);
                    entries.add(SpaceSuitItems.HELMET);
                    entries.add(SpaceSuitItems.CHEST);
                    entries.add(SpaceSuitItems.LEGS);
                    entries.add(SpaceSuitItems.BOOTS);
                    entries.add(OxygenStation.STATION);
                    entries.add(MarsBlocks.REGOLITH);
                    entries.add(MarsBlocks.ROCK);
                    entries.add(MarsBlocks.ORE);
                    for(Destination planet:java.util.List.of(Destination.VENUS,Destination.EUROPA,Destination.TITAN)){var m=OuterPlanetBlocks.get(planet);entries.add(m.surface());entries.add(m.rock());entries.add(m.ore());}
                    entries.add(MoonBlocks.ROCK);
                    entries.add(MoonBlocks.REGOLITH);
                    entries.add(MoonBlocks.BASALT);
                    entries.add(MoonBlocks.ORE);
                    entries.add(MoonBlocks.WOOD);
                    entries.add(MoonBlocks.LEAVES);
                    entries.add(MoonBlocks.PLANKS);
                    entries.add(MoonBlocks.STAIRS);
                    entries.add(MoonBlocks.SLAB);
                    entries.add(MoonBlocks.FENCE);
                    entries.add(MoonBlocks.FENCE_GATE);
                    entries.add(MoonBlocks.DOOR);
                    entries.add(MoonBlocks.TRAPDOOR);
                    entries.add(MoonBlocks.BUTTON);
                    entries.add(MoonBlocks.PRESSURE_PLATE);
                    entries.add(MoonBlocks.PAD);
                    entries.add(MoonBlocks.BRICKS);
                    entries.add(MoonBlocks.BRICK_STAIRS);
                    entries.add(MoonBlocks.BRICK_SLAB);
                    entries.add(MoonBlocks.BRICK_WALL);
                }).build());
    }
}
