import dev.megarockets.*;
import java.util.*;
public class LunarVillageCheck{
 record Cell(int x,int y,int z){}
 static void check(LunarVillageLayout.Home h){
  var start=new Cell(LunarVillageLayout.X+h.x(),2,h.z());
  var seen=new HashSet<Cell>();var q=new ArrayDeque<Cell>();q.add(start);seen.add(start);boolean oxygen=false;
  int[][] dirs={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
  while(!q.isEmpty()){
   var c=q.remove();int m=LunarVillageLayout.material(c.x,c.y,c.z);
   if(m==LunarVillageLayout.OXYGEN){oxygen=true;continue;}
   if(m>0)continue;
   if(m==LunarVillageLayout.NONE||c.y<0||c.y>10||seen.size()>4096)throw new AssertionError("Unsealed "+h);
   for(var d:dirs){var n=new Cell(c.x+d[0],c.y+d[1],c.z+d[2]);if(seen.add(n))q.add(n);}
  }
  if(!oxygen)throw new AssertionError("No oxygen "+h);
 }
 public static void main(String[] args){
  for(var h:LunarVillageLayout.HOMES){check(h);if(h.kind()!=2&&LunarVillageLayout.material(LunarVillageLayout.markerX(h),1,LunarVillageLayout.markerZ(h))!=LunarVillageLayout.MARKER)throw new AssertionError("Resident marker");}
  for(int x=404;x<=420;x++)for(int z=-8;z<=8;z++){if(LunarVillageLayout.material(x,0,z)<=0)throw new AssertionError("Pad");for(int y=1;y<=64;y++)if(LunarVillageLayout.material(x,y,z)!=0)throw new AssertionError("Pad clearance");}
  for(long seed:new long[]{0,1,42,123456,-19}){int y=LunarVillageLayout.base(new MoonTerrain(seed));if(y<800||y>1400)throw new AssertionError("Deck outside north slice "+y);}
  System.out.println("PASS Selena village: eight sealed modules, seven resident markers, pad and seeded deck");
 }
}
