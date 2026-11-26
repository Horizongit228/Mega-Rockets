package dev.megarockets;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.util.Set;

/** Same-world wrap; destination collision checks preserve terrain and player builds. */
public final class SurfaceTravel {
 private SurfaceTravel(){}
 public static void init(){ServerTickEvents.END_WORLD_TICK.register(SurfaceTravel::tick);}
 private static void load(ServerWorld w,Box b){
  for(int x=(int)Math.floor(b.minX)>>4;x<=(int)Math.floor(b.maxX)>>4;x++)
   for(int z=(int)Math.floor(b.minZ)>>4;z<=(int)Math.floor(b.maxZ)>>4;z++)w.getChunk(x,z);
 }
 private static void tick(ServerWorld w){
  Destination d=MegaRockets.planet(w);if(d==null)return;
  int period=PlanetWrap.period(d.radius);
  for(ServerPlayerEntity pilot:java.util.List.copyOf(w.getPlayers())){
   if(pilot.isSpectator())continue;
   var ship=pilot.getVehicle() instanceof TitanRocket rocket&&rocket.getFirstPassenger()==pilot?rocket:null;
   if(pilot.hasVehicle()&&ship==null)continue;
   var e=ship==null?pilot:ship;
   double x=PlanetWrap.coordinate(e.getX(),period),z=PlanetWrap.coordinate(e.getZ(),period);
   double dx=x-e.getX(),dz=z-e.getZ();if(Math.abs(dx)<.01&&Math.abs(dz)<.01)continue;
   Box box=e.getBoundingBox().offset(dx,0,dz);load(w,box);
   double rise=0;while(rise<=96&&!w.isSpaceEmpty(e,box.offset(0,rise,0)))rise+=2;
   if(rise>96||!w.getWorldBorder().contains(box.offset(0,rise,0)))continue;
   Vec3d vel=pilot.getVelocity();
   if(ship==null){
    if(pilot.teleport(w,x,pilot.getY()+rise,z,Set.of(),pilot.getYaw(),pilot.getPitch()))pilot.setVelocity(vel);
   }else{
    double px=pilot.getX()+dx,py=pilot.getY()+rise,pz=pilot.getZ()+dz;
    pilot.stopRiding();
    if(pilot.teleport(w,px,py,pz,Set.of(),pilot.getYaw(),pilot.getPitch())){
     ship.setPosition(x,ship.getY()+rise,z);pilot.startRiding(ship,true);
    }else pilot.startRiding(ship,true);
   }
  }
 }
}
