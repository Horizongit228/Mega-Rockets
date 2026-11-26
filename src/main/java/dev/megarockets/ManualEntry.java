package dev.megarockets;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.Heightmap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.server.world.ChunkTicketType;

/** Temporary chunk ticket requests a 5x5 patch; poll full chunks without forcing a synchronous generation loop. */
public final class ManualEntry {
 public record Gate(Destination body,double x,double z){
  public boolean earth(){return body==null;}
  public String key(){return earth()?"earth":body.key();}
  public ServerWorld world(ServerPlayerEntity p){return earth()?p.getServer().getOverworld():p.getServer().getWorld(MegaRockets.destinationWorld(body));}
 }
 public static Gate nearest(double x,double z){
  Gate result=null;double best=ApproachRules.LOAD_RADIUS;
  double earth=ApproachRules.distance(x,z,ApproachRules.EARTH_X,ApproachRules.EARTH_Z);
  if(earth<=best){best=earth;result=new Gate(null,ApproachRules.EARTH_X,ApproachRules.EARTH_Z);}
  for(Destination d:Destination.values()){
   if(d.orbitOnly())continue;
   double distance=ApproachRules.distance(x,z,d.distance,d.lane);
   if(distance<best){best=distance;result=new Gate(d,d.distance,d.lane);}
  }
  return result;
 }
 private static final ChunkTicketType<Integer> ENTRY_TICKET=ChunkTicketType.create("megarockets_entry",Integer::compare,200);
 public final Gate gate;
 private final ServerWorld world;
 private final int x,z,cx,cz,id;
 private final ChunkPos center;
 private int loaded=0,highest;
 public ManualEntry(Gate gate,ServerWorld world,double orbitX,double orbitZ,int id){
  this.gate=gate;this.world=world;
  BlockPos spawn=gate.earth()?world.getSpawnPos():BlockPos.ORIGIN;
  x=spawn.getX()+ApproachRules.local(orbitX-gate.x());z=spawn.getZ()+ApproachRules.local(orbitZ-gate.z());
  cx=x>>4;cz=z>>4;this.id=id;center=new ChunkPos(cx,cz);highest=world.getBottomY();
 }
 public int progress(){return loaded*100/ApproachRules.CHUNKS;}
 public boolean ready(){return loaded==ApproachRules.CHUNKS;}
 public void step(){
  var manager=world.getChunkManager();
  manager.addTicket(ENTRY_TICKET,center,ApproachRules.CHUNK_RADIUS,id);
  loaded=0;
  for(int i=0;i<ApproachRules.CHUNKS;i++){
   int a=cx+ApproachRules.chunkX(i),b=cz+ApproachRules.chunkZ(i);
   if(manager.getWorldChunk(a,b)!=null)loaded++;
  }
 }
 public void cancel(){world.getChunkManager().removeTicket(ENTRY_TICKET,center,ApproachRules.CHUNK_RADIUS,id);}
 private void measure(){
  highest=world.getBottomY();
  for(int i=0;i<ApproachRules.CHUNKS;i++){
   int a=cx+ApproachRules.chunkX(i),b=cz+ApproachRules.chunkZ(i);
   for(int dx=0;dx<16;dx++)for(int dz=0;dz<16;dz++)
    highest=Math.max(highest,world.getTopY(Heightmap.Type.WORLD_SURFACE,(a<<4)+dx,(b<<4)+dz));
  }
 }
 public boolean transfer(TitanRocket old,ServerPlayerEntity pilot){
  if(!ready())return false;measure();if(highest<=world.getBottomY()+2)return false;
  return MegaRockets.manualSurface(old,pilot,world,gate.body(),x+.5,highest+ApproachRules.CLEARANCE,z+.5);
 }
}
