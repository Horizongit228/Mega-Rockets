import dev.megarockets.MoonTerrain;
public class MoonTerrainCheck {
 public static void main(String[] args){
  int caves=0,ore=0,high=0,low=10000,regions=0;
  for(int x=-4160;x<=4160;x+=64)for(int z=-4160;z<=4160;z+=64){
   var c=MoonTerrain.column(x,z);
   if(Math.hypot(x,z)>=4096){if(c.top()>=c.bottom())throw new AssertionError("Outside sphere");continue;}
   if(c.top()<c.bottom())continue;
   high=Math.max(high,c.top());low=Math.min(low,c.top());
   for(int y=c.top()-120;y<c.top()-5;y+=4){int m=MoonTerrain.material(c,x,y,z);if(m==MoonTerrain.AIR)caves++;if(m==MoonTerrain.ORE)ore++;}
   for(int offset:new int[]{-3072,0,3072})if(c.top()>=offset-1536&&c.bottom()<offset+1536)regions|=offset<0?1:offset==0?2:4;
  }
  for(int x=-16;x<=16;x++)for(int z=-16;z<=16;z++){var c=MoonTerrain.column(x,z);if(c.top()!=4095||MoonTerrain.material(c,x,4095,z)!=MoonTerrain.PAD)throw new AssertionError("Pad");}
  if(caves<100||ore<100||high<=4095||low>=1000||regions!=7)throw new AssertionError("Terrain variation missing");
  if(MoonTerrain.PAD_Y+MoonTerrain.NORTH_OFFSET!=MoonTerrain.RADIUS)throw new AssertionError("Coordinate mapping");
  System.out.println("PASS radius 4096, all three slices, top "+low+".."+high+", caves "+caves+", ore "+ore);
 }
}
