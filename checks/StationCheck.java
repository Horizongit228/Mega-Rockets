import dev.megarockets.StationLayout;
import java.util.*;
public class StationCheck{
 record Cell(int x,int y,int z){}
 static void sealed(Cell start){
  var seen=new HashSet<Cell>();var queue=new ArrayDeque<Cell>();seen.add(start);queue.add(start);boolean oxygen=false;
  int[][] sides={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
  while(!queue.isEmpty()){
   var c=queue.remove();int material=StationLayout.material(c.x(),c.y(),c.z());
   if(material==StationLayout.OXYGEN){oxygen=true;continue;}
   if(material!=StationLayout.AIR)continue;
   if(c.y()<98||c.y()>110||!StationLayout.column(c.x(),c.z())||seen.size()>4096)throw new AssertionError("Leaky module "+start);
   for(var s:sides){var n=new Cell(c.x()+s[0],c.y()+s[1],c.z()+s[2]);if(seen.add(n))queue.add(n);}
  }
  if(!oxygen)throw new AssertionError("No oxygen "+start);
 }
 public static void main(String[] args){
  int z=StationLayout.Z;
  for(int i=0;i<StationLayout.PAD_COUNT;i++){
   int pz=StationLayout.padZ(i);
   for(int x=24;x<=40;x++)for(int q=pz-8;q<=pz+8;q++){
    if(StationLayout.material(x,100,q)==0)throw new AssertionError("Pad floor");
    for(int y=101;y<165;y++)if(StationLayout.material(x,y,q)!=0)throw new AssertionError("Pad blocked");
   }
   if(!StationLayout.gravity(32,101,pz))throw new AssertionError("Pad gravity");
  }
  for(var c:List.of(new Cell(0,102,z),new Cell(11,102,z),new Cell(-25,102,z),new Cell(0,102,z-20),new Cell(0,102,z+20)))sealed(c);
  for(int q:new int[]{-28,-19,19,28})if(StationLayout.material(-27,100,z+q)!=StationLayout.SOLAR)throw new AssertionError("Solar");
  for(int x:new int[]{7,10,12,15})if(StationLayout.material(x,101,z+1)!=StationLayout.CONSOLE||StationLayout.consoleSide(x,1,101)==0)throw new AssertionError("Airlock console");
  if(StationLayout.material(8,101,z)!=StationLayout.DOOR_LOWER||StationLayout.material(14,102,z)!=StationLayout.DOOR_UPPER)throw new AssertionError("Airlock doors");
  System.out.println("PASS three clear landing pads, five sealed supplied rooms, solar anchors and airlock controls");
 }
}
