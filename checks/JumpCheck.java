import dev.megarockets.*;
public class JumpCheck{
 public static void main(String[] args){
  if(JumpRules.duration(0)!=100||JumpRules.duration(1e9)!=280||JumpRules.duration(2e6)<=JumpRules.duration(10000))throw new AssertionError("Charge duration");
  for(long time:new long[]{0,12345,987654321L})for(Destination d:Destination.values()){
   var target=SolarDynamics.position(d,time);var arrival=JumpRules.arrival(d,time);
   double r=target.distance(arrival.x(),arrival.z());
   if(Math.abs(r-JumpRules.offset(d))>.01)throw new AssertionError("Moving arrival "+d);
   if(!d.orbitOnly()&&r>=ApproachRules.ENTRY_RADIUS)throw new AssertionError("Entry radius "+d);
  }
  var earth=SolarDynamics.position(null,1000);var arrival=JumpRules.arrival(null,1000);
  if(earth.distance(arrival.x(),arrival.z())>=ApproachRules.ENTRY_RADIUS)throw new AssertionError("Earth gate");
  System.out.println("PASS hyperjump charge limits and moving arrival geometry");
 }
}
