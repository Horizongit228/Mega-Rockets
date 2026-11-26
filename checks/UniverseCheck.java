import dev.megarockets.*;
public class UniverseCheck {
 public static void main(String[] args){
  if(Destination.MOON.radius!=MoonTerrain.RADIUS||Destination.MARS.radius!=MarsTerrain.RADIUS||Destination.JUPITER.radius<Destination.SATURN.radius||Destination.SATURN.radius<Destination.URANUS.radius||Destination.EARTH_RADIUS<Destination.VENUS.radius)throw new AssertionError("Planet scale");
  for(Destination d:Destination.values()){
   if(Math.abs(d.radius*SolarScale.KM_PER_SIZE_BLOCK-d.meanRadiusKm)>SolarScale.KM_PER_SIZE_BLOCK*.51)throw new AssertionError("Radius scale "+d);
   if(d!=Destination.MOON&&Math.abs(d.distance*SolarScale.KM_PER_TRAVEL_BLOCK-d.referenceKm)>1000.01)throw new AssertionError("Route scale "+d);
  }
  for(Destination d:Destination.values())for(double max:new double[]{15,30,50}){
   var s=LunarFlight.start();int ticks=0;double peak=0;
   while(!LunarFlight.finished(s)&&ticks++<250000){s=LunarFlight.step(s,true,d.distance,max,d.orbitOnly());peak=Math.max(peak,s.speed());if(d.orbitOnly()&&(s.phase()==LunarFlight.Phase.DESCENT||s.phase()==LunarFlight.Phase.FINAL||s.phase()==LunarFlight.Phase.LANDED))throw new AssertionError("Gas giant landing");}
   if(s.phase()!=(d.orbitOnly()?LunarFlight.Phase.ORBIT_ARRIVAL:LunarFlight.Phase.LANDED)||s.x()!=d.distance||s.y()!=(d.orbitOnly()?LunarFlight.CRUISE_Y:LunarFlight.LAND_Y)||peak>max+1e-6||peak<max*.9)throw new AssertionError(d+" "+max+" "+s);
   System.out.println("PASS "+d+" "+max*20+" blocks/s "+ticks+" ticks");
  }
  for(Destination d:new Destination[]{Destination.VENUS,Destination.EUROPA,Destination.TITAN,Destination.MERCURY}){
   var a=new OuterPlanetTerrain(d,42);var b=new OuterPlanetTerrain(d,43);int changed=0;
   for(int x=-500;x<=500;x+=16)for(int z=-500;z<=500;z+=16)if(a.top(x,z)!=b.top(x,z))changed++;
   if(changed<1000||a.top(d.radius,0)!=OuterPlanetTerrain.MIN_Y-1)throw new AssertionError(d+" terrain");
   LandingSurvey.Site site=null;
   for(int i=0;i<LandingSurvey.COUNT&&site==null;i++){
    var p=LandingSurvey.candidate(i);site=LandingSurvey.inspect(p.x(),p.z(),(x,z)->{int h=a.top(x,z);return a.material(x,h,z,h,a.caveColumn(x,z))==OuterPlanetTerrain.AIR?LandingSurvey.INVALID:h+1;});
   }
   if(site==null)throw new AssertionError(d+" no landing site");
   System.out.println("PASS "+d+" terrain and "+site);
  }
 }
}
