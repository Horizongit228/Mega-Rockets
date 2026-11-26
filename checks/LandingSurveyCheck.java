import dev.megarockets.*;
import java.util.*;
import java.util.function.IntBinaryOperator;
public class LandingSurveyCheck {
 private static LandingSurvey.Site find(IntBinaryOperator ground){
  for(int i=0;i<LandingSurvey.COUNT;i++){
   var p=LandingSurvey.candidate(i);var site=LandingSurvey.inspect(p.x(),p.z(),ground);if(site!=null)return site;
  }
  throw new AssertionError("No natural landing site");
 }
 public static void main(String[] args){
  var points=new HashSet<LandingSurvey.Point>();
  for(int i=0;i<LandingSurvey.COUNT;i++)if(!points.add(LandingSurvey.candidate(i)))throw new AssertionError("Duplicate search");
  if(LandingSurvey.inspect(0,0,(x,z)->x)==null&&LandingSurvey.inspect(0,0,(x,z)->64)!=null){}else throw new AssertionError("Slope rejection");
  if(LandingSurvey.inspect(0,0,(x,z)->x==0&&z==0?LandingSurvey.INVALID:64)!=null)throw new AssertionError("Hole");
  for(long seed:new long[]{42,12345,987654,0,-1,100234,93281,77}){
   var moon=new MoonTerrain(seed);var mars=new MarsTerrain(seed);
   var a=find((x,z)->{var c=moon.column(x,z);return moon.material(c,x,c.top(),z)==MoonTerrain.AIR?LandingSurvey.INVALID:c.top()-MoonTerrain.NORTH_OFFSET+1;});
   var b=find((x,z)->{int h=mars.top(x,z);return mars.material(x,h,z,h)==MarsTerrain.AIR?LandingSurvey.INVALID:h+1;});
   System.out.println("PASS seed "+seed+": Moon "+a+", Mars "+b);
  }
 }
}
