import dev.megarockets.*;
public class PlanetGeologyCheck {
 public static void main(String[] args){
  for(long seed:new long[]{42,12345,987654}){
   MoonTerrain moon=new MoonTerrain(seed);MarsTerrain mars=new MarsTerrain(seed);
   int lunarEntrances=0,marsEntrances=0,peaks=0,high=0,plains=0,rocks=0;
   for(int gx=-8;gx<=8;gx++)for(int gz=-8;gz<=8;gz++){
    var e=moon.caveEntrance(gx,gz);
    if(e!=null){var c=moon.column(e.x(),e.z());
     for(int depth=0;depth<=32;depth++)if(moon.material(c,e.x(),c.top()-depth,e.z())!=MoonTerrain.AIR)throw new AssertionError("Blocked Moon entrance");
     lunarEntrances++;
    }
    var m=mars.caveEntrance(gx,gz);
    if(m!=null){int h=mars.top(m.x(),m.z());var cave=mars.caveColumn(m.x(),m.z());
     if(h-32>MarsTerrain.MIN_Y+3){for(int depth=0;depth<=32;depth++)if(mars.material(m.x(),h-depth,m.z(),h,cave)!=MarsTerrain.AIR)throw new AssertionError("Blocked Mars entrance");marsEntrances++;}
    }
   }
   var mountains=mars.mountains();
   if(mountains.size()!=10||!mountains.equals(new MarsTerrain(seed).mountains()))throw new AssertionError("Mountain count or seed");
   for(var m:mountains){
    peaks++;
    if(m.radius()<350||m.radius()>600||m.height()>456||m.height()<300||Math.hypot(m.x(),m.z())+m.radius()>=MarsTerrain.RADIUS)throw new AssertionError("Mountain dimensions");
    for(var other:mountains)if(other!=m&&Math.hypot(m.x()-other.x(),m.z()-other.z())<=m.radius()+other.radius())throw new AssertionError("Overlapping mountains");
    int h=mars.top(m.x(),m.z());high=Math.max(high,h);
    if(h<300||h>=MarsTerrain.MIN_Y+MarsTerrain.HEIGHT)throw new AssertionError("Mountain height "+h);
   }
   if(mars.top(MarsTerrain.RADIUS,0)!=MarsTerrain.MIN_Y-1||mars.top(0,-MarsTerrain.RADIUS)!=MarsTerrain.MIN_Y-1||mars.material(MarsTerrain.RADIUS,MarsTerrain.MIN_Y,0,mars.top(MarsTerrain.RADIUS,0))!=MarsTerrain.AIR)throw new AssertionError("Mars edge");
   for(int x=-1800;x<=1800;x+=32)for(int z=-1800;z<=1800;z+=32){
    int h=mars.top(x,z);if(h<150)plains++;else if(h>300)rocks++;
    if(mars.material(x,h,z,h)==MarsTerrain.PAD)throw new AssertionError("Artificial Mars pad");
   }
   for(int x=-32;x<=32;x+=2)for(int z=-32;z<=32;z+=2){
    var col=moon.column(x,z);if(col.pad()||moon.material(col,x,col.top(),z)==MoonTerrain.PAD)throw new AssertionError("Artificial lunar pad");
   }
   if(lunarEntrances<80||marsEntrances<80||peaks!=10||high<450||plains<7000)throw new AssertionError("Insufficient terrain variety "+lunarEntrances+" "+marsEntrances+" "+peaks+" "+high+" "+plains+" "+rocks);
   System.out.println("PASS seed "+seed+": Moon entrances "+lunarEntrances+", Mars entrances "+marsEntrances+", mountains "+peaks+", max Y "+high+", lowlands "+plains+", high mountain samples "+rocks);
  }
 }
}
