import dev.megarockets.MoonTerrain;
public class MoonTerrainCheck {
 public static void main(String[] args) {
  int caves=0,ore=0,high=0,low=300;
  for(int x=-260;x<=260;x+=4)for(int z=-260;z<=260;z+=4){
   var c=MoonTerrain.column(x,z);
   if(Math.hypot(x,z)>=256){if(c.top()>=c.bottom())throw new AssertionError("Outside sphere");continue;}
   if(c.top()<c.bottom())continue;
   high=Math.max(high,c.top());low=Math.min(low,c.top());
   if(Math.hypot(x,z)<=32 && (c.top()!=255||MoonTerrain.material(c,x,255,z)!=MoonTerrain.PAD))throw new AssertionError("Pad");
   for(int y=c.bottom()+6;y<c.top()-6;y+=4){
    int m=MoonTerrain.material(c,x,y,z);
    if(m==MoonTerrain.AIR)caves++;
    if(m==MoonTerrain.ORE)ore++;
   }
  }
  if(caves<100||ore<100||high<=255||low>=100)throw new AssertionError("Terrain variation missing");
  System.out.println("PASS sphere: nominal diameter 512, sampled top range "+low+".."+high+", cave samples "+caves+", ore samples "+ore);
 }
}
