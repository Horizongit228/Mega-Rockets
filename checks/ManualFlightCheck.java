import dev.megarockets.ManualFlight;
public class ManualFlightCheck{
 public static void main(String[] args){
  for(double cap:new double[]{2,12,15,30,50}){
   var v=ManualFlight.STOP;
   for(int i=0;i<300;i++){v=ManualFlight.step(v,ManualFlight.FORWARD|ManualFlight.UP|ManualFlight.RIGHT,0,cap,.5);
    if(v.length()>cap+1e-8||v.x()<0||v.y()<0||v.z()<0)throw new AssertionError("Diagonal or cap");
   }
   if(v.length()<cap-.01)throw new AssertionError("Cruise");
   for(int i=0;i<300;i++)v=ManualFlight.step(v,ManualFlight.BRAKE|ManualFlight.FORWARD,0,cap,.5);
   if(v.length()!=0)throw new AssertionError("Brake");
   for(int i=0;i<300;i++)v=ManualFlight.step(v,ManualFlight.FORWARD,90,cap,.5);
   if(Math.abs(v.x()+cap)>1e-8||Math.abs(v.z())>1e-8)throw new AssertionError("Heading");
   for(int i=0;i<300;i++)v=ManualFlight.step(v,0,0,cap,.5);
   if(v.length()!=0)throw new AssertionError("Release");
  }
  var v=ManualFlight.step(ManualFlight.STOP,ManualFlight.UP|ManualFlight.DOWN,0,10,.5);
  if(v.length()!=0)throw new AssertionError("Opposing controls");
  System.out.println("PASS 3D manual motion, heading, diagonal limits, release and brakes");
 }
}
