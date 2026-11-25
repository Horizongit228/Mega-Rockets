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
  for(int x=-16;x<=16;x++)for(int z=-16;z<=16;z++){var c=MoonTerrain.column(x,z);if(c.top()!=MoonTerrain.PAD_GLOBAL_Y||MoonTerrain.material(c,x,MoonTerrain.PAD_GLOBAL_Y,z)!=MoonTerrain.PAD)throw new AssertionError("Pad");}
  if(caves<100||ore<100||high<=4095||low>=1000||regions!=7)throw new AssertionError("Terrain variation missing");
  int outside=0,steep=0;
  for(int x=-192;x<=192;x++)for(int z=-192;z<=192;z++){
   double r=Math.hypot(x,z);if(r>192)continue;
   var c=MoonTerrain.column(x,z);
   if(r<=24){
    if(c.top()!=MoonTerrain.PAD_GLOBAL_Y||MoonTerrain.material(c,x,c.top(),z)!=MoonTerrain.PAD||MoonTerrain.material(c,x,c.top()-15,z)==MoonTerrain.AIR)throw new AssertionError("Unsafe pad");
   }else if(r<192){if(c.top()>=MoonTerrain.PAD_GLOBAL_Y)throw new AssertionError("Crater wall");outside++;}
  }
  for(int x=400;x<500;x++)for(int z=400;z<500;z++){
   if(Math.abs(MoonTerrain.column(x,z).top()-MoonTerrain.column(x+1,z).top())>=3)steep++;
  }
  if(outside<100000||steep<100)throw new AssertionError("Relief too smooth");
  if(MoonTerrain.PAD_Y+MoonTerrain.NORTH_OFFSET!=4352)throw new AssertionError("Coordinate mapping");
  System.out.println("PASS clear highland: "+outside+" surrounding columns below pad; "+steep+" abrupt relief samples");
  System.out.println("PASS radius 4096, all three slices, top "+low+".."+high+", caves "+caves+", ore "+ore);
 }
}
