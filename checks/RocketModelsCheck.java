import dev.megarockets.client.*;
import java.util.*;
public class RocketModelsCheck{
 public static void main(String[] args){
  var counts=new HashSet<Integer>();
  for(var style:RocketModels.Style.values()){
   var m=RocketModels.get(style);
   if(m.hull().size()<1000||m.cabin().size()<400||m.glass().size()<40||!counts.add(m.hull().size()))throw new AssertionError(style+" missing geometry");
   float low=999,high=-999;
   for(var q:m.hull())for(var p:q.points()){low=Math.min(low,p.y());high=Math.max(high,p.y());if(Math.hypot(p.x(),p.z())>7.0001||!Float.isFinite(p.y()))throw new AssertionError(style+" outside hull");}
   if(low!=0||high!=64)throw new AssertionError(style+" bounds");
   for(var list:List.of(m.hull(),m.cabin(),m.glass(),m.lamps()))for(var q:list){double n=q.nx()*q.nx()+q.ny()*q.ny()+q.nz()*q.nz();if(!Double.isFinite(n)||Math.abs(n-1)>1e-4)throw new AssertionError("normal");}
   for(var list:List.of(m.cabin(),m.glass(),m.lamps()))for(var q:list)for(var p:q.points())if(Math.hypot(p.x(),p.z())>3.81||p.y()<50.9||p.y()>55.5)throw new AssertionError(style+" cabin outside");
   System.out.println("PASS "+style+" "+m.hull().size()+" hull, "+m.cabin().size()+" cabin, "+m.engines().size()+" engines");
  }
  if(RocketModels.get(RocketModels.Style.TITAN).engines().size()!=7||RocketModels.get(RocketModels.Style.PHANTOM).engines().size()!=5||RocketModels.get(RocketModels.Style.HYPERION).engines().size()!=4)throw new AssertionError("Engine layout");
 }
}
