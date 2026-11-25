import dev.megarockets.MoonTerrain;
import java.util.EnumSet;
public class MoonTerrainCheck {
 public static void main(String[] args){
  var a=new MoonTerrain(12345);var same=new MoonTerrain(12345);var b=new MoonTerrain(987654);
  int caves=0,ore=0,changed=0,trees=0,high=-10000,low=10000,regions=0;var biomes=EnumSet.noneOf(MoonTerrain.Region.class);
  for(int x=-4160;x<=4160;x+=64)for(int z=-4160;z<=4160;z+=64){
   var c=a.column(x,z);
   if(!c.equals(same.column(x,z)))throw new AssertionError("Non-deterministic");
   if(Math.hypot(x,z)>=4096){if(c.top()>=c.bottom())throw new AssertionError("Outside sphere");continue;}
   if(c.top()<c.bottom())continue;
   if(c.top()!=b.column(x,z).top())changed++;
   biomes.add(c.region());high=Math.max(high,c.top());low=Math.min(low,c.top());
   for(int y=c.top()-120;y<c.top()-5;y+=4){int m=a.material(c,x,y,z);if(m==MoonTerrain.AIR)caves++;if(m==MoonTerrain.ORE)ore++;}
   for(int offset:new int[]{-3072,0,3072})if(c.top()>=offset-1536&&c.bottom()<offset+1536)regions|=offset<0?1:offset==0?2:4;
  }
  for(var world:new MoonTerrain[]{a,b}){
   for(int x=-192;x<=192;x+=2)for(int z=-192;z<=192;z+=2){
    double r=Math.hypot(x,z);if(r>=192)continue;var c=world.column(x,z);
    if(r<=24){if(c.top()!=MoonTerrain.PAD_GLOBAL_Y||world.material(c,x,c.top(),z)!=MoonTerrain.PAD)throw new AssertionError("Pad");}
    else if(c.top()>=MoonTerrain.PAD_GLOBAL_Y)throw new AssertionError("Crater wall");
   }
  }
  for(int x=-128;x<=128;x++)for(int z=-128;z<=128;z++){
   var t=a.tree(x,z);if(t==null)continue;trees++;
   if(!t.equals(same.tree(x,z))||Math.hypot(t.x(),t.z())<208||a.treeBlock(t,t.x(),t.y(),t.z())!=MoonTerrain.WOOD||
     a.treeBlock(t,t.x(),t.y()+t.height(),t.z())!=MoonTerrain.LEAVES)throw new AssertionError("Tree");
  }
  if(caves<100||ore<100||changed<1000||trees<100||biomes.size()!=4||regions!=7||high<=4095||low>=1000)throw new AssertionError("Terrain variation missing");
  System.out.println("PASS seeded Moon: "+changed+" changed columns; "+trees+" trees; "+biomes+"; caves "+caves+"; ore "+ore);
 }
}
