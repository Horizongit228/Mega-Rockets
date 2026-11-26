import dev.megarockets.*;
public class PlanetWrapCheck{
 public static void main(String[] args){
  var moon=new MoonTerrain(42,true);int p=PlanetWrap.MOON_PERIOD;
  for(int x:new int[]{-p/2,-5000,-100,0,100,p/2-1})for(int z:new int[]{-500,0,500}){
   var a=moon.column(x,z);var b=moon.column(x+p,z-p);
   if(!a.equals(b)||a.bottom()!=MoonTerrain.NORTH_OFFSET-64||a.top()<a.bottom())throw new AssertionError("Moon periodic column");
   if(moon.material(a,x,a.top()-30,z)!=moon.material(b,x+p,b.top()-30,z-p))throw new AssertionError("Moon material");
  }
  for(Destination d:Destination.values()){
   if(d.orbitOnly())continue;
   int n=PlanetWrap.period(d.radius);if(n%32!=0||Math.abs(n-2*Math.PI*d.radius)>2000)throw new AssertionError("Period");
   if(PlanetWrap.coordinate(n/2,n)!=-n/2||PlanetWrap.coordinate(-n/2-1,n)!=n/2-1)throw new AssertionError("Wrap");
   if(d==Destination.MOON)continue;
   if(d==Destination.MARS){
    var t=new MarsTerrain(42,true);
    for(int x:new int[]{-n/2,0,n/2-1})if(t.top(x,10)!=t.top(x+n,10)||t.top(x,10)<8)throw new AssertionError("Mars periodic");
    if(Math.abs(t.top(-n/2,0)-t.top(n/2-1,0))>1)throw new AssertionError("Mars seam");
   }else{
    var t=new OuterPlanetTerrain(d,42,true);
    for(int x:new int[]{-n/2,0,n/2-1})if(t.top(x,10)!=t.top(x+n,10)||t.top(x,10)<8)throw new AssertionError("Periodic "+d);
    if(Math.abs(t.top(-n/2,0)-t.top(n/2-1,0))>1)throw new AssertionError("Seam "+d);
   }
  }
  int x=LunarVillageLayout.X,z=LunarVillageLayout.Z;
  if(LunarVillageLayout.material(x+66,1,z)!=LunarVillageLayout.DOOR_LOWER||LunarVillageLayout.material(x+72,1,z)!=LunarVillageLayout.DOOR_LOWER)throw new AssertionError("Greenhouse entrances");
  for(int i=67;i<=71;i++)if(LunarVillageLayout.material(x+i,1,z)!=0||LunarVillageLayout.material(x+i,2,z)!=0)throw new AssertionError("Blocked entrance");
  System.out.println("PASS repeatable flat surfaces, smooth seams and greenhouse entrance");
 }
}
