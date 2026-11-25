package dev.megarockets;
import net.minecraft.block.*;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class MoonBlocks {
    public static final Block ROCK=add("lunar_rock",Blocks.STONE);
    public static final Block REGOLITH=add("lunar_regolith",Blocks.ANDESITE);
    public static final Block BASALT=add("lunar_basalt",Blocks.DEEPSLATE);
    public static final Block ORE=add("lunar_crystal_ore",Blocks.IRON_ORE);
    public static final Block PAD=add("lunar_landing_tile",Blocks.SMOOTH_STONE);
    public static final Block WOOD=addWood();
    public static final Block LEAVES=addLeaves();
    public static final Block PLANKS=add("lunar_planks",Blocks.OAK_PLANKS);
    public static final Block STAIRS=register("lunar_stairs",new StairsBlock(PLANKS.getDefaultState(),AbstractBlock.Settings.copy(Blocks.OAK_STAIRS)){});
    public static final Block SLAB=register("lunar_slab",new SlabBlock(AbstractBlock.Settings.copy(Blocks.OAK_SLAB)));
    public static final Block FENCE=register("lunar_fence",new FenceBlock(AbstractBlock.Settings.copy(Blocks.OAK_FENCE)));
    public static final Block FENCE_GATE=register("lunar_fence_gate",new FenceGateBlock(WoodType.OAK,AbstractBlock.Settings.copy(Blocks.OAK_FENCE_GATE)));
    public static final Block DOOR=register("lunar_door",new DoorBlock(BlockSetType.OAK,AbstractBlock.Settings.copy(Blocks.OAK_DOOR)){});
    public static final Block TRAPDOOR=register("lunar_trapdoor",new TrapdoorBlock(BlockSetType.OAK,AbstractBlock.Settings.copy(Blocks.OAK_TRAPDOOR)){});
    public static final Block BUTTON=register("lunar_button",new ButtonBlock(BlockSetType.OAK,30,AbstractBlock.Settings.copy(Blocks.OAK_BUTTON)){});
    public static final Block PRESSURE_PLATE=register("lunar_pressure_plate",new PressurePlateBlock(BlockSetType.OAK,AbstractBlock.Settings.copy(Blocks.OAK_PRESSURE_PLATE)){});
    private static Block register(String name,Block block){
        var id=MegaRockets.id(name);
        Registry.register(Registries.BLOCK,id,block);
        Registry.register(Registries.ITEM,id,new BlockItem(block,new Item.Settings()));
        return block;
    }
    private static Block addWood(){
        var id=MegaRockets.id("lunar_log");
        Block block=Registry.register(Registries.BLOCK,id,new PillarBlock(AbstractBlock.Settings.copy(Blocks.OAK_LOG)));
        Registry.register(Registries.ITEM,id,new BlockItem(block,new Item.Settings()));
        return block;
    }
    private static Block addLeaves(){
        var id=MegaRockets.id("lunar_leaves");
        Block block=Registry.register(Registries.BLOCK,id,new LeavesBlock(AbstractBlock.Settings.copy(Blocks.OAK_LEAVES).luminance(s->5)));
        Registry.register(Registries.ITEM,id,new BlockItem(block,new Item.Settings()));return block;
    }
    private static Block add(String name,Block base) {
        var id=MegaRockets.id(name);
        Block block=Registry.register(Registries.BLOCK,id,new Block(AbstractBlock.Settings.copy(base)));
        Registry.register(Registries.ITEM,id,new BlockItem(block,new Item.Settings()));
        return block;
    }
    public static void init() {}
}
