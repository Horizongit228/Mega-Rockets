package dev.megarockets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Seeded settlement regions, cached independently of chunk generation order. */
public final class LunarSettlements {
 public static final int SPACING=640;
 public record Site(int x,int z,int base){
  public int localX(int x){return x-this.x+LunarVillageLayout.X;}
  public int localZ(int z){return z-this.z+LunarVillageLayout.Z;}
  public boolean covers(int x,int z){return LunarVillageLayout.footprint(localX(x),localZ(z));}
  public int material(int x,int y,int z){return LunarVillageLayout.material(localX(x),y-base,localZ(z));}
 }
 private final long seed;
 private final MoonTerrain terrain;
 private final Map<Long,Optional<Site>> cache=new ConcurrentHashMap<>();
 public LunarSettlements(long seed,MoonTerrain terrain){this.seed=seed;this.terrain=terrain;}
 private static long hash(long n){n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;return n^(n>>>31);}
 private Site choose(int gx,int gz){
  long n=hash(seed^gx*341873128712L^gz*132897987541L);
  if(Math.floorMod(n,100)>=76)return null;
  Site best=null;int bestRelief=Integer.MAX_VALUE;
  for(int attempt=0;attempt<5;attempt++){
   int x=gx*SPACING+320+(int)Math.floorMod(hash(n+1+attempt*2),321)-160,z=gz*SPACING+320+(int)Math.floorMod(hash(n+2+attempt*2),321)-160;
   if(Math.hypot(x,z)>3000||Math.hypot(x,z)<190)continue;
   int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;
   for(int dx=LunarVillageLayout.MIN_X;dx<=LunarVillageLayout.MAX_X;dx+=8)for(int dz=LunarVillageLayout.MIN_Z;dz<=LunarVillageLayout.MAX_Z;dz+=8){
    int y=terrain.column(x+dx,z+dz).top()-MoonTerrain.NORTH_OFFSET;min=Math.min(min,y);max=Math.max(max,y);
   }
   int relief=max-min;
   if(relief>90||max>MoonTerrain.MIN_Y+MoonTerrain.HEIGHT-100||min<MoonTerrain.MIN_Y+32)continue;
   if(relief<bestRelief){bestRelief=relief;best=new Site(x,z,max+4);}
   if(relief<=28)break;
  }
  return best;
 }
 public Site cell(int gx,int gz){
  if(gx<-6||gx>5||gz<-6||gz>5)return null;
  long k=((long)gx<<32)^(gz&0xffffffffL);
  return cache.computeIfAbsent(k,v->Optional.ofNullable(choose(gx,gz))).orElse(null);
 }
 public Site at(int x,int z){
  // Every layout fits entirely inside its own 640-block region.
  var s=cell(Math.floorDiv(x,SPACING),Math.floorDiv(z,SPACING));
  return s!=null&&s.covers(x,z)?s:null;
 }
 public List<Site> near(double x,double z,int radius){
  var result=new ArrayList<Site>();
  for(int gx=Math.max(-6,Math.floorDiv((int)Math.floor(x-radius),SPACING));gx<=Math.min(5,Math.floorDiv((int)Math.floor(x+radius),SPACING));gx++)
   for(int gz=Math.max(-6,Math.floorDiv((int)Math.floor(z-radius),SPACING));gz<=Math.min(5,Math.floorDiv((int)Math.floor(z+radius),SPACING));gz++){
    var s=cell(gx,gz);if(s!=null&&Math.hypot(s.x-x,s.z-z)<=radius)result.add(s);
   }
  result.sort(Comparator.comparingDouble(s->Math.hypot(s.x-x,s.z-z)));return result;
 }
}
