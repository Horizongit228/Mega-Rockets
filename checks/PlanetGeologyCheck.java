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
   for(int gx=-4;gx<=4;gx++)for(int gz=-4;gz<=4;gz++){
    var m=mars.mountain(gx,gz);if(m==null)continue;peaks++;
    int h=mars.top(m.x(),m.z());high=Math.max(high,h);
    if(h<320||h>=MarsTerrain.MIN_Y+MarsTerrain.HEIGHT)throw new AssertionError("Mountain height "+h);
   }
   for(int x=-1800;x<=1800;x+=32)for(int z=-1800;z<=1800;z+=32){
    int h=mars.top(x,z);if(h<150)plains++;else if(h>300)rocks++;
    if(Math.hypot(x,z)<192&&Math.hypot(x,z)>24&&h>=127)throw new AssertionError("Landing clearance");
   }
   for(int x=-16;x<=16;x+=4)for(int z=-16;z<=16;z+=4){
    if(Math.hypot(x,z)>24)continue;var c=moon.column(x,z);
    for(int d=0;d<32;d++)if(moon.material(c,x,c.top()-d,z)==MoonTerrain.AIR||mars.material(x,127-d,z,127)==MarsTerrain.AIR)throw new AssertionError("Landing foundation");
   }
   if(lunarEntrances<80||marsEntrances<80||peaks<40||peaks>80||high<450||plains<7000||rocks<30)throw new AssertionError("Insufficient terrain variety "+lunarEntrances+" "+marsEntrances+" "+peaks+" "+high+" "+plains+" "+rocks);
   System.out.println("PASS seed "+seed+": Moon entrances "+lunarEntrances+", Mars entrances "+marsEntrances+", mountains "+peaks+", max Y "+high+", lowlands "+plains+", high mountain samples "+rocks);
  }
 }
}
