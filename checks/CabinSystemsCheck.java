import dev.megarockets.*;
public class CabinSystemsCheck{
 public static void main(String[] args){
  int p=100;for(int i=0;i<40;i++)p=CabinSystems.pressure(p,false,false);
  if(p!=20||!CabinSystems.breathable(p)||CabinSystems.breathable(CabinSystems.pressure(p,false,false)))throw new AssertionError("Pressure threshold");
  for(int i=0;i<50;i++)p=CabinSystems.pressure(p,false,false);
  if(p!=0)throw new AssertionError("Leak");
  for(int i=0;i<25;i++)p=CabinSystems.pressure(p,true,false);
  if(p!=100||CabinSystems.pressure(100,false,true)!=100)throw new AssertionError("Refill");
  int heat=0;for(int i=0;i<25;i++)heat=CabinSystems.heat(heat,true,100);
  if(heat!=100||CabinSystems.heat(heat,false,100)!=97||CabinSystems.throttle(25,-25)!=25||CabinSystems.throttle(100,25)!=100)throw new AssertionError("Engine controls");
  for(double cap:new double[]{15,30,50}){
   var v=ManualFlight.STOP;double x=0,y=100,z=2500;
   int ticks=0;for(;ticks<100000;ticks++){
    double dx=60000-x,dy=180-y,dz=9000-z,d=Math.sqrt(dx*dx+dy*dy+dz*dz);
    if(d<1&&v.length()<1)break;
    double goal=Math.min(cap,Math.min(d,Math.sqrt(d*.5)));
    v=ManualFlight.toward(v,new ManualFlight.Motion(dx/d*goal,dy/d*goal,dz/d*goal),.5);
    x+=v.x();y+=v.y();z+=v.z();
   }
   if(ticks>=100000)throw new AssertionError("Auto navigation didn't converge");
  }
  System.out.println("PASS pressure, heat, throttle and 3D orbital autopilot");
 }
}
