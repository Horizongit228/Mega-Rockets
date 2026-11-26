import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** Generates every model, blockstate, loot table, recipe and original 32px material texture. */
public class BuildingResources {
 static Path out,source;static int count;
 static String id(String s){return s.contains(":")?s:"minecraft:"+s;}
 static void write(String path,String s)throws Exception{Path p=out.resolve(path);Files.createDirectories(p.getParent());Files.writeString(p,s);}
 static void asset(String path,String s)throws Exception{write("assets/megarockets/"+path,s);}
 static void data(String path,String s)throws Exception{write("data/megarockets/"+path,s);}
 static String ref(String n){return "megarockets:block/"+n;}
 static void model(String n,String parent,String textures)throws Exception{asset("models/block/"+n+".json","{\"parent\":\"minecraft:block/"+parent+"\",\"textures\":{"+textures+"}}");}
 static void item(String n,String model)throws Exception{asset("models/item/"+n+".json","{\"parent\":\""+ref(model)+"\"}");}
 static void state(String n,String model)throws Exception{asset("blockstates/"+n+".json","{\"variants\":{\"\":{\"model\":\""+ref(model)+"\"}}}");}
 static String tex(String n,String... keys){StringJoiner j=new StringJoiner(",");for(String k:keys)j.add("\""+k+"\":\""+ref(n)+"\"");return j.toString();}
 static void loot(String n,boolean slab)throws Exception{
  String functions=slab?",\"functions\":[{\"function\":\"minecraft:set_count\",\"count\":2,\"conditions\":[{\"condition\":\"minecraft:block_state_property\",\"block\":\"megarockets:"+n+"\",\"properties\":{\"type\":\"double\"}}]},{\"function\":\"minecraft:explosion_decay\"}]":"";
  data("loot_table/blocks/"+n+".json","{\"type\":\"minecraft:block\",\"pools\":[{\"rolls\":1,\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"megarockets:"+n+"\""+functions+"}],\"conditions\":[{\"condition\":\"minecraft:survives_explosion\"}]}]}");
 }
 static void recipe(String n,String ingredient,int qty,String pattern)throws Exception{
  data("recipe/"+n+".json","{\"type\":\"minecraft:crafting_shaped\",\"category\":\"building\",\"pattern\":"+pattern+",\"key\":{\"X\":{\"item\":\""+id(ingredient)+"\"}},\"result\":{\"id\":\"megarockets:"+n+"\",\"count\":"+qty+"}}");
 }
 static void baseRecipe(String[] v)throws Exception{
  String n=v[0],ingredient=id(v[3]),extra=v[7].equals("lamp")?"minecraft:"+n.substring(0,n.length()-6)+"_dye":v[7].equals("glass")?"minecraft:"+n.substring(0,n.length()-12)+"_dye":"minecraft:iron_ingot";
  if(v[7].equals("solid")){
   data("recipe/"+n+".json","{\"type\":\"minecraft:stonecutting\",\"ingredient\":{\"item\":\""+ingredient+"\"},\"result\":{\"id\":\"megarockets:"+n+"\",\"count\":1}}");return;
  }
  data("recipe/"+n+".json","{\"type\":\"minecraft:crafting_shapeless\",\"category\":\"building\",\"ingredients\":[{\"item\":\""+ingredient+"\"},{\"item\":\""+extra+"\"}],\"result\":{\"id\":\"megarockets:"+n+"\",\"count\":1}}");
 }
 static int clamp(double v){return (int)Math.max(0,Math.min(255,v));}
 static void texture(String n,String kind,int rgb)throws Exception{
  BufferedImage img=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);Random random=new Random(n.hashCode());
  for(int y=0;y<32;y++)for(int x=0;x<32;x++){
   double v=(random.nextDouble()-.5)*11;int alpha=255;int c=rgb;
   switch(kind){
    case "panel"->{v+=x==0||y==0||x==31||y==31?-48:x==1||y==1?26:0;if((x==3||x==28)&&(y==3||y==28))v-=65;v+=8*Math.sin(y*.18);}
    case "brick"->{int xx=(x+(y/8%2)*8)%16;v+=y%8==0||xx==0?-50:y%8==1||xx==1?20:0;}
    case "tile"->{v+=x%16==0||y%16==0?-47:x%16==1||y%16==1?19:0;}
    case "weave"->v+=((x/2+y/2)%2==0?13:-13)+5*Math.sin(x*2.2);
    case "stone"->v+=16*Math.sin(x*.4+Math.cos(y*.6))*Math.cos(y*.35);
    case "vent"->{v+=x>3&&x<28&&y%5<2?-76:12;}
    case "grate"->{v+=x%5<2||y%5<2?17:-56;}
    case "circuit"->{if(x%8==2||y%8==3)c=0x98d8af;if(x%8==2&&y%8==3)c=0xe3b85e;}
    case "hazard"->{if((x+y)%16<8)c=0x303640;}
    case "solar"->{v+=x%8==0||y%8==0?42:-9;}
    case "lamp"->{if(x<3||x>28||y<3||y>28)c=0x344553;else v+=20*Math.cos((x-16)*.09)*Math.cos((y-16)*.09);}
    case "glass"->{alpha=x<2||x>29||y<2||y>29?255:(x+y)%20<2?105:38;v+=alpha==255?-35:20;}
   }
   img.setRGB(x,y,(alpha<<24)|(clamp((c>>16&255)+v)<<16)|(clamp((c>>8&255)+v)<<8)|clamp((c&255)+v));
  }
  Path path=out.resolve("assets/megarockets/textures/block/"+n+".png");Files.createDirectories(path.getParent());ImageIO.write(img,"PNG",path.toFile());
 }
 static void shapes(String n)throws Exception{
  String slab=n+"_slab",stairs=n+"_stairs",wall=n+"_wall";
  model(slab,"slab",tex(n,"bottom","top","side"));model(slab+"_top","slab_top",tex(n,"bottom","top","side"));
  asset("blockstates/"+slab+".json","{\"variants\":{\"type=bottom\":{\"model\":\""+ref(slab)+"\"},\"type=top\":{\"model\":\""+ref(slab+"_top")+"\"},\"type=double\":{\"model\":\""+ref(n)+"\"}}}");
  for(String suffix:List.of("","_inner","_outer"))model(stairs+suffix,suffix.isEmpty()?"stairs":suffix.substring(1)+"_stairs",tex(n,"bottom","top","side"));
  for(String suffix:List.of("post","side","side_tall","inventory"))model(wall+"_"+suffix,"template_wall_"+suffix,tex(n,"wall"));
  // Vanilla inventory wall model uses wall_inventory, not template_wall_inventory.
  model(wall+"_inventory","wall_inventory",tex(n,"wall"));
  for(String type:List.of("stairs","wall")){
   String template=Files.readString(source.resolve("assets/megarockets/blockstates/lunar_brick_"+type+".json"));
   asset("blockstates/"+n+"_"+type+".json",template.replace("lunar_brick_"+type,n+"_"+type));
  }
  item(slab,slab);item(stairs,stairs);item(wall,wall+"_inventory");
  loot(slab,true);loot(stairs,false);loot(wall,false);
  recipe(slab,"megarockets:"+n,6,"[\"XXX\"]");
  recipe(stairs,"megarockets:"+n,4,"[\"X  \",\"XX \",\"XXX\"]");
  recipe(wall,"megarockets:"+n,6,"[\"XXX\",\"XXX\"]");
  count+=3;
 }
 public static void main(String[] args)throws Exception{
  source=Path.of(args[0]);out=Path.of(args[1]);
  for(String line:Files.readAllLines(source.resolve("megarockets_building_catalog.tsv"))){
   String[] v=line.split("\t");String n=v[0];texture(n,v[4],Integer.parseInt(v[5],16));model(n,"cube_all",tex(n,"all"));state(n,n);item(n,n);loot(n,false);baseRecipe(v);count++;if(v[6].equals("1"))shapes(n);
  }
  if(count!=96)throw new IllegalStateException("Wrong asset count "+count);
  System.out.println("Generated "+count+" complete building block resource sets.");
 }
}
