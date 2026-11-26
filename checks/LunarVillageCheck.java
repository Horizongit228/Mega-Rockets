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
  for(var h:LunarVillageLayout.HOMES){check(h);int x=LunarVillageLayout.X+h.x()-h.halfX()+2,z=h.z();if(LunarVillageLayout.material(x,2,z)!=LunarVillageLayout.BATTERY||LunarVillageLayout.material(x,h.height()+1,z)!=LunarVillageLayout.SOLAR)throw new AssertionError("Village power");if(h.kind()!=2&&LunarVillageLayout.material(LunarVillageLayout.markerX(h),1,LunarVillageLayout.markerZ(h))!=LunarVillageLayout.MARKER)throw new AssertionError("Resident marker");}
  for(int x=404;x<=420;x++)for(int z=-8;z<=8;z++){if(LunarVillageLayout.material(x,0,z)<=0)throw new AssertionError("Pad");for(int y=1;y<=64;y++)if(LunarVillageLayout.material(x,y,z)!=0)throw new AssertionError("Pad clearance");}
  for(long seed:new long[]{0,1,42,123456,-19}){int y=LunarVillageLayout.base(new MoonTerrain(seed));if(y<800||y>1400)throw new AssertionError("Deck outside north slice "+y);}
  for(long seed:new long[]{0,1,42,123456,-19}){
   var a=new LunarSettlements(seed,new MoonTerrain(seed));var b=new LunarSettlements(seed,new MoonTerrain(seed));
   var sites=a.near(0,0,9000);if(sites.size()<3)throw new AssertionError("Too few villages "+seed);
   if(!sites.equals(b.near(0,0,9000)))throw new AssertionError("Seed instability");
   for(var s:sites){
    if(!s.equals(a.at(s.x(),s.z()))||!s.equals(a.at(s.x()+92,s.z())))throw new AssertionError("Village cell mismatch");
    for(var t:sites)if(!s.equals(t)&&Math.hypot(s.x()-t.x(),s.z()-t.z())<300)throw new AssertionError("Village overlap");
   }
  }
  System.out.println("PASS distributed seeded villages, room seals and pads");
 }
}
