package dev.megarockets;
import net.minecraft.block.*;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import java.util.*;
public final class OuterPlanetBlocks {
 public record Materials(Block surface,Block rock,Block ore){}
 private static final Map<Destination,Materials> BLOCKS=new EnumMap<>(Destination.class);
 private static Block add(String name,Block base){
  var id=MegaRockets.id(name);Block b=Registry.register(Registries.BLOCK,id,new Block(AbstractBlock.Settings.copy(base)));
  Registry.register(Registries.ITEM,id,new BlockItem(b,new Item.Settings()));return b;
 }
 public static void init(){
  for(Destination d:List.of(Destination.VENUS,Destination.EUROPA,Destination.TITAN)){
   String n=d.key();BLOCKS.put(d,new Materials(add(n+"_surface",Blocks.STONE),add(n+"_rock",Blocks.DEEPSLATE),add(n+"_ore",Blocks.IRON_ORE)));
  }
 }
 public static Materials get(Destination d){return BLOCKS.get(d);}
 public static boolean isGround(BlockState s){for(Materials m:BLOCKS.values())if(s.isOf(m.surface())||s.isOf(m.rock())||s.isOf(m.ore()))return true;return false;}
}
