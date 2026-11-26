import dev.megarockets.LunarFlight;
import dev.megarockets.FlightProfile;
import dev.megarockets.MarsTerrain;
public class InterplanetaryCheck{
 public static void main(String[] args){
  for(double max:new double[]{15,30,50}){
   double y=64,v=0,peak=0;int n=0;
   while(y<10000&&n++<10000){v=FlightProfile.nextSpeed(y,v,10000,max);y=Math.min(10000,y+v);peak=Math.max(peak,v);}
   if(Math.abs(y-10000)>1e-6||peak>max+1e-6||peak<max*.98)throw new AssertionError("Ascent "+max+" "+y+" "+peak);
   for(double distance:new double[]{6000,24000}){
    var s=LunarFlight.start();int ticks=0;double fastest=0;boolean paused=false;
    while(s.phase()!=LunarFlight.Phase.LANDED&&ticks++<10000){
     if(!paused&&s.x()>distance*.4){s=LunarFlight.pause(s,distance,max);paused=true;}
     var next=LunarFlight.step(s,true,distance,max);
     if(next.x()<s.x()||next.x()>distance+1e-6||next.speed()>max+1e-6||next.y()<101||next.speed()<0)throw new AssertionError("Invalid flight "+next);
     if(next.phase()==LunarFlight.Phase.LANDED&&s.y()-next.y()>.15)throw new AssertionError("Landing");
     fastest=Math.max(fastest,next.speed());s=next;
    }
    if(s.phase()!=LunarFlight.Phase.LANDED||s.x()!=distance||s.y()!=101||s.tilt()!=0||s.speed()!=0||!paused||fastest<max*.9)throw new AssertionError("Mission "+max+" "+distance+" "+s+" peak "+fastest);
    System.out.println("PASS mission "+distance+" at "+max*20+" blocks/s; peak "+fastest*20+"; ticks "+ticks);
   }
  }
  var a=new MarsTerrain(42);var b=new MarsTerrain(43);var same=new MarsTerrain(42);int changed=0,lo=999,hi=-999,caves=0;
  for(int x=-600;x<=600;x+=8)for(int z=-600;z<=600;z+=8){
   int h=a.top(x,z);if(h!=same.top(x,z))throw new AssertionError("Seed");
   if(h!=b.top(x,z))changed++;lo=Math.min(lo,h);hi=Math.max(hi,h);
   if(a.material(x,h,z,h)==MarsTerrain.PAD)throw new AssertionError("Artificial pad");
   if(a.material(x,h-20,z,h)==MarsTerrain.AIR)caves++;
  }
  if(changed<1000||hi-lo<25||caves<50)throw new AssertionError("Mars terrain variation");
  System.out.println("PASS Mars terrain: "+changed+" varied columns, "+lo+".."+hi+", caves "+caves);
 }
}
