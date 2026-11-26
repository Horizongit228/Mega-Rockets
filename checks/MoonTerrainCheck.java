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
    var col=world.column(x,z);
    if(col.pad()||world.material(col,x,col.top(),z)==MoonTerrain.PAD)throw new AssertionError("Artificial pad");
   }
  }
  // Ordinary traversable terrain should have mostly one-block steps.
  int samples=0,steep=0;long totalDelta=0;
  for(int x=360;x<1100;x+=3)for(int z=360;z<1100;z+=3){
   int y=a.column(x,z).top();
   for(int delta:new int[]{Math.abs(a.column(x+1,z).top()-y),Math.abs(a.column(x,z+1).top()-y)}){
    samples++;totalDelta+=delta;if(delta>2)steep++;
   }
  }
  if(totalDelta>samples||steep>samples/20)throw new AssertionError("Surface too steep: "+steep+"/"+samples);
  System.out.println("PASS smoother terrain: mean adjacent height difference "+((double)totalDelta/samples)+", steep steps "+steep+"/"+samples);
  int plains=0,flatPlains=0;
  for(int x=-1200;x<=1200;x+=16)for(int z=-1200;z<=1200;z+=16){
   if(Math.hypot(x,z)<720||a.region(x,z)!=MoonTerrain.Region.PLAINS)continue;
   plains++;int h=a.column(x,z).top();
   if(Math.abs(a.column(x+4,z).top()-h)<=2&&Math.abs(a.column(x,z+4).top()-h)<=2)flatPlains++;
  }
  if(plains<100||flatPlains<plains*.75)throw new AssertionError("Not enough walkable plains: "+flatPlains+"/"+plains);
  System.out.println("PASS plains: "+flatPlains+"/"+plains+" gentle samples");
  var kinds=EnumSet.noneOf(MoonTerrain.TreeKind.class);
  int rocks=0,crystals=0,forestCells=0,forestTrees=0;
  for(int x=-128;x<=128;x++)for(int z=-128;z<=128;z++){
   var rock=a.boulder(x,z);
   if(rock!=null){rocks++;if(rock.crystal())crystals++;if(!rock.equals(same.boulder(x,z))||Math.hypot(rock.x(),rock.z())<216||a.boulderBlock(rock,rock.x(),rock.y(),rock.z())==MoonTerrain.AIR)throw new AssertionError("Boulder");}
   boolean forest=a.region(x*MoonTerrain.TREE_CELL+6,z*MoonTerrain.TREE_CELL+6)==MoonTerrain.Region.FOREST;
   if(forest&&Math.hypot(x*MoonTerrain.TREE_CELL+6,z*MoonTerrain.TREE_CELL+6)>220)forestCells++;
   var t=a.tree(x,z);if(t==null)continue;trees++;kinds.add(t.kind());if(forest)forestTrees++;
   if(!t.equals(same.tree(x,z))||Math.hypot(t.x(),t.z())<208||a.treeBlock(t,t.x(),t.y(),t.z())!=MoonTerrain.WOOD||
     a.treeBlock(t,t.x(),t.y()+t.height(),t.z())!=MoonTerrain.LEAVES)throw new AssertionError("Tree");
  }
  if(forestTrees<forestCells*.65)throw new AssertionError("Forest density too low");
  System.out.println("PASS denser forest: "+forestTrees+" trees / "+forestCells+" forest cells");
  if(kinds.size()!=3||rocks<100||crystals<10)throw new AssertionError("Missing forest or rock variety");
  System.out.println("PASS tree forms "+kinds+", rock formations "+rocks+", crystal formations "+crystals);
  if(caves<100||ore<100||changed<1000||trees<100||biomes.size()!=4||regions!=7||high<4000||low>=1000)throw new AssertionError("Terrain variation missing");
  System.out.println("PASS seeded Moon: "+changed+" changed columns; "+trees+" trees; "+biomes+"; caves "+caves+"; ore "+ore);
 }
}
