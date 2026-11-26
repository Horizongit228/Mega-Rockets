package dev.megarockets;
import java.util.*;
import net.minecraft.block.DoorBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** Bounded flood fill. Full blocks and closed doors seal; an oxygen station on a boundary
 * supplies the enclosed volume. Unloaded terrain and excessive volume fail closed. */
public final class PressureRoom {
 private record Cache(ServerWorld world,BlockPos pos,long tick,boolean sealed){}
 private static final Map<UUID,Cache> CACHE=new HashMap<>();
 private PressureRoom(){}
 public static void remove(UUID id){CACHE.remove(id);}
 public static void clear(){CACHE.clear();}
 public static boolean breathable(ServerPlayerEntity p){
  var w=p.getServerWorld();var pos=p.getBlockPos();var prior=CACHE.get(p.getUuid());
  if(prior!=null&&prior.world()==w&&prior.pos().equals(pos)&&w.getTime()-prior.tick()<10)return prior.sealed();
  boolean sealed=scan(w,pos);
  CACHE.put(p.getUuid(),new Cache(w,pos.toImmutable(),w.getTime(),sealed));return sealed;
 }
 private static boolean scan(ServerWorld w,BlockPos origin){
  Set<BlockPos> seen=new HashSet<>();ArrayDeque<BlockPos> queue=new ArrayDeque<>();
  queue.add(origin);seen.add(origin);boolean oxygen=false;
  while(!queue.isEmpty()){
   BlockPos pos=queue.removeFirst();
   if(Math.abs(pos.getX()-origin.getX())>24||Math.abs(pos.getY()-origin.getY())>24||Math.abs(pos.getZ()-origin.getZ())>24||seen.size()>4096)return false;
   if(pos.getY()<w.getBottomY()||pos.getY()>=w.getTopY()||w.getChunkManager().getWorldChunk(pos.getX()>>4,pos.getZ()>>4)==null)return false;
   var state=w.getBlockState(pos);
   if(state.isOf(OxygenStation.STATION)){oxygen|=SpaceEnergy.powered(w,pos);continue;}
   if(state.getBlock() instanceof DoorBlock&&!state.get(DoorBlock.OPEN))continue;
   if(state.isFullCube(w,pos))continue;
   for(Direction d:Direction.values()){var next=pos.offset(d);if(seen.add(next))queue.addLast(next);}
  }
  return oxygen;
 }
}
