package dev.megarockets;

import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Fixed outpost consoles. Controls both halves of each airlock door on the server. */
public final class StationConsole extends Block {
 public static final Block BLOCK=Registry.register(Registries.BLOCK,MegaRockets.id("station_console"),new StationConsole(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).luminance(s->8)));
 public static void init(){}
 private StationConsole(Settings s){super(s);}
 @Override protected ItemActionResult onUseWithItem(ItemStack stack,BlockState state,World world,BlockPos pos,PlayerEntity player,Hand hand,BlockHitResult hit){return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;}
 @Override protected ActionResult onUse(BlockState state,World world,BlockPos pos,PlayerEntity player,BlockHitResult hit){
  if(world.isClient)return ActionResult.SUCCESS;
  if(!(world instanceof ServerWorld w)||!world.getRegistryKey().equals(MegaRockets.ORBIT))return ActionResult.CONSUME;
  int side=StationLayout.consoleSide(pos.getX(),pos.getZ()-StationLayout.Z,pos.getY());
  if(side==0){
   int n=power(w);int online=0;for(int[] p:StationLayout.OXYGEN_POINTS)if(SpaceEnergy.powered(w,new BlockPos(p[0],101,StationLayout.Z+p[1])))online++;player.sendMessage(Text.translatable("station.megarockets.energy_status",n,online,StationLayout.OXYGEN_POINTS.length),false);return ActionResult.CONSUME;
  }
  BlockPos inner=new BlockPos(8,101,StationLayout.Z),outer=new BlockPos(14,101,StationLayout.Z);
  if(!valid(w,inner)||!valid(w,outer)){player.sendMessage(Text.translatable("station.megarockets.damaged"),true);return ActionResult.CONSUME;}
  BlockPos wanted=side<0?inner:outer,other=side<0?outer:inner;
  boolean open=!w.getBlockState(wanted).get(DoorBlock.OPEN);
  set(w,other,false);set(w,wanted,open);
  player.sendMessage(Text.translatable(open?(side<0?"station.megarockets.inner_open":"station.megarockets.outer_open"):"station.megarockets.closed"),true);
  return ActionResult.CONSUME;
 }
 private static boolean valid(ServerWorld w,BlockPos pos){return w.getBlockState(pos).isOf(Blocks.IRON_DOOR)&&w.getBlockState(pos.up()).isOf(Blocks.IRON_DOOR);}
 private static void set(ServerWorld w,BlockPos pos,boolean open){
  w.setBlockState(pos,w.getBlockState(pos).with(DoorBlock.OPEN,open),Block.NOTIFY_LISTENERS);
  w.setBlockState(pos.up(),w.getBlockState(pos.up()).with(DoorBlock.OPEN,open),Block.NOTIFY_LISTENERS);
 }
 public static int power(ServerWorld w){
  int n=0;
  for(int q:new int[]{-28,-19,19,28}){BlockPos pos=new BlockPos(-27,100,StationLayout.Z+q);
   if(w.getChunkManager().getWorldChunk(pos.getX()>>4,pos.getZ()>>4)!=null&&w.getBlockState(pos).isOf(SpaceEnergy.PANEL))n++;
  }return n;
 }
}
