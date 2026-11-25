package dev.megarockets;
import net.minecraft.block.*;
import net.minecraft.item.*;
import net.minecraft.registry.*;
public final class MarsBlocks {
 public static final Block REGOLITH=add("martian_regolith",Blocks.RED_TERRACOTTA);
 public static final Block ROCK=add("martian_rock",Blocks.TERRACOTTA);
 public static final Block ORE=add("martian_ore",Blocks.COPPER_ORE);
 private static Block add(String name,Block base){
  var id=MegaRockets.id(name);Block block=Registry.register(Registries.BLOCK,id,new Block(AbstractBlock.Settings.copy(base)));
  Registry.register(Registries.ITEM,id,new BlockItem(block,new Item.Settings()));return block;
 }
 public static void init(){}
}
