package dev.megarockets;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
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
    public static final Block WOOD=add("lunar_log",Blocks.OAK_LOG);
    public static final Block LEAVES=addLeaves();
    private static Block addLeaves(){
        var id=MegaRockets.id("lunar_leaves");
        Block block=Registry.register(Registries.BLOCK,id,new Block(AbstractBlock.Settings.copy(Blocks.OAK_LEAVES).luminance(s->5)));
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
