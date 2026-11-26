package dev.megarockets;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Box;

/** Preload the arrival sector while the drive charges; ticket expires if the ship unloads. */
public final class JumpTransit {
 private static final ChunkTicketType<Integer> TICKET=ChunkTicketType.create("megarockets_jump",Integer::compare,200);
 private final ServerWorld orbit;
 private final int id,duration;
 private final Destination body;
 private ChunkPos center;
 private int elapsed;
 public JumpTransit(ServerWorld orbit,int id,Destination body,double originX,double originZ){
  this.orbit=orbit;this.id=id;this.body=body;
  duration=JumpRules.duration(SolarDynamics.position(body,orbit.getTime()).distance(originX,originZ));
 }
 public Destination body(){return body;}
 public ServerWorld orbit(){return orbit;}
 public int progress(){return Math.max(1,Math.min(99,elapsed*100/duration));}
 public boolean expired(){return elapsed>duration+JumpRules.TIMEOUT;}
 public SolarDynamics.Position arrival(){return JumpRules.arrival(body,orbit.getTime());}
 public boolean step(){
  elapsed++;
  var p=JumpRules.arrival(body,orbit.getTime()+Math.max(0,duration-elapsed));
  var next=new ChunkPos((int)Math.floor(p.x())>>4,(int)Math.floor(p.z())>>4);
  if(center!=null&&!center.equals(next))cancel();
  center=next;var manager=orbit.getChunkManager();manager.addTicket(TICKET,center,2,id);
  if(elapsed<duration)return false;
  for(int x=center.x-1;x<=center.x+1;x++)for(int z=center.z-1;z<=center.z+1;z++)if(manager.getWorldChunk(x,z)==null)return false;
  return true;
 }
 public void cancel(){if(center!=null){orbit.getChunkManager().removeTicket(TICKET,center,2,id);center=null;}}
}
