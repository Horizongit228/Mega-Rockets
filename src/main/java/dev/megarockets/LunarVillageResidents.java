package dev.megarockets;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;

/** One-time inhabitants. The generated marker is consumed only after successful spawn. */
public final class LunarVillageResidents {
 private LunarVillageResidents(){}
 public static void init(){ServerTickEvents.END_WORLD_TICK.register(LunarVillageResidents::tick);}
 private static void tick(ServerWorld world){
  if(!world.getRegistryKey().equals(MegaRockets.MOON)||world.getTime()%40!=0||
      !(world.getChunkManager().getChunkGenerator() instanceof MoonChunkGenerator generator))return;
  if(world.getPlayers().stream().noneMatch(p->Math.hypot(p.getX()-LunarVillageLayout.X,p.getZ()-LunarVillageLayout.Z)<160))return;
  int y=generator.villageBase(world.getChunkManager().getNoiseConfig())+1;
  for(var home:LunarVillageLayout.HOMES){
   if(home.kind()==2)continue;
   int x=LunarVillageLayout.markerX(home),z=LunarVillageLayout.markerZ(home);
   if(world.getChunkManager().getWorldChunk(x>>4,z>>4)==null)continue;
   var pos=new BlockPos(x,y,z);
   if(!world.getBlockState(pos).isOf(BuildingBlocks.get("habitat_core")))continue;
   var resident=EntityType.VILLAGER.create(world);
   if(resident==null)continue;
   resident.setCustomName(Text.translatable("entity.megarockets.lunar_resident"));resident.setPersistent();
   resident.refreshPositionAndAngles(x+.5,y,z+1.5,0,0);
   if(world.isSpaceEmpty(resident,resident.getBoundingBox())&&world.spawnEntity(resident))
    world.setBlockState(pos,BuildingBlocks.get("cyan_light").getDefaultState(),3);
  }
 }
}
