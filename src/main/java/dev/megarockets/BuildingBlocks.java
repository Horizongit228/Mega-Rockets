package dev.megarockets;

import net.minecraft.block.*;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class BuildingBlocks {
 private static final Map<String,Block> ALL=new LinkedHashMap<>();
 private static final List<Block> GLASS=new ArrayList<>();
 private BuildingBlocks(){}
 public static Block get(String id){return Objects.requireNonNull(ALL.get(id),id);}
 public static Collection<Block> glass(){return Collections.unmodifiableList(GLASS);}
 private static Block register(String name,Block block){
  Registry.register(Registries.BLOCK,MegaRockets.id(name),block);
  Registry.register(Registries.ITEM,MegaRockets.id(name),new BlockItem(block,new Item.Settings()));
  ALL.put(name,block);return block;
 }
 public static void init(){
  try(var in=new BufferedReader(new InputStreamReader(Objects.requireNonNull(BuildingBlocks.class.getResourceAsStream("/megarockets_building_catalog.tsv")),StandardCharsets.UTF_8))){
   for(String line;(line=in.readLine())!=null;){
    String[] v=line.split("\t");String id=v[0];Block block;
    var settings=AbstractBlock.Settings.copy(Blocks.IRON_BLOCK);
    switch(v[7]){
     case "glass"->{block=new GlassBlock(AbstractBlock.Settings.copy(Blocks.GLASS));GLASS.add(block);}
     case "lamp"->block=new Block(AbstractBlock.Settings.copy(Blocks.SEA_LANTERN).luminance(s->15));
     default->block=new Block(settings);
    }
    register(id,block);
    if(v[6].equals("1")){
     register(id+"_stairs",new StairsBlock(block.getDefaultState(),AbstractBlock.Settings.copy(block)){});
     register(id+"_slab",new SlabBlock(AbstractBlock.Settings.copy(block)));
     register(id+"_wall",new WallBlock(AbstractBlock.Settings.copy(block)));
    }
   }
  }catch(IOException ex){throw new IllegalStateException("Building block catalog",ex);}
  if(ALL.size()!=96)throw new IllegalStateException("Expected 96 building blocks");
  for(String kind:List.of("architecture","lighting","glazing")){
   Registry.register(Registries.ITEM_GROUP,MegaRockets.id(kind),net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder()
    .displayName(net.minecraft.text.Text.translatable("itemGroup.megarockets."+kind))
    .icon(()->new ItemStack(get(kind.equals("architecture")?"titanium_panel":kind.equals("lighting")?"cyan_light":"cyan_space_glass")))
    .entries((ctx,entries)->ALL.forEach((id,b)->{
     if(kind.equals("glazing")?GLASS.contains(b):kind.equals("lighting")?id.endsWith("_light"):!GLASS.contains(b)&&!id.endsWith("_light"))entries.add(b);
    })).build());
  }
 }
}
