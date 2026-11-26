import dev.megarockets.ApproachRules;
import java.util.HashSet;
public class ApproachCheck {
 public static void main(String[] args){
  if(ApproachRules.ENTRY_RADIUS>=ApproachRules.LOAD_RADIUS||ApproachRules.EXIT_OFFSET<=ApproachRules.LOAD_RADIUS)throw new AssertionError("Entry bounds");
  if(ApproachRules.distance(ApproachRules.EARTH_X,0,ApproachRules.EARTH_X,0)!=0||ApproachRules.distance(0,0,ApproachRules.EARTH_X,0)<=ApproachRules.LOAD_RADIUS)throw new AssertionError("Earth departure");
  var chunks=new HashSet<String>();for(int i=0;i<ApproachRules.CHUNKS;i++){
   int x=ApproachRules.chunkX(i),z=ApproachRules.chunkZ(i);
   if(Math.abs(x)>2||Math.abs(z)>2||!chunks.add(x+":"+z))throw new AssertionError("Chunk coverage");
  }
  if(chunks.size()!=25||ApproachRules.local(99999)!=1200||ApproachRules.local(-99999)!=-1200)throw new AssertionError("Region mapping");
  double last=0;for(int h=0;h<200;h++){
   double cap=ApproachRules.descentCap(h);
   if(cap<last||cap>2||cap<=0)throw new AssertionError("Descent speed");last=cap;
  }
  if(ApproachRules.descentCap(0)>.25||ApproachRules.surfaceCap(0)>.5)throw new AssertionError("Touchdown speed");
  System.out.println("PASS entry gates, preload coverage, mapping, landing limits");
 }
}
