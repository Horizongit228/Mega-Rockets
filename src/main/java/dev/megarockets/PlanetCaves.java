package dev.megarockets;

/** Continuous, seeded cave networks. Cave coordinates are absolute X/Z, depth below terrain.
 * A surface opening is aligned with a shallow gallery; intersections connect to deeper levels.
 */
public final class PlanetCaves {
 public static final int CELL=128;
 public record Entrance(int x,int z){}
 public record Column(double xDist,double zDist,double xDepth,double zDepth,double deepX,double deepZ,
                      double entranceDist,boolean enabled){}
 private final long seed;private final double ox,oz;
 public PlanetCaves(long seed){this.seed=seed;ox=10000*hash(0,1,0);oz=10000*hash(0,2,0);}
 private double hash(int x,int y,int z){
  long n=seed^(x*341873128712L+y*132897987541L+z*42317861L);
  n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;n^=n>>>31;return(n>>>11)*0x1.0p-53;
 }
 private static double period(double value){return value-CELL*Math.floor(value/CELL+.5);}
 private double zwave(int x){return 18*Math.sin((x+ox)*.021)+6*Math.sin((x+ox)*.057);}
 private double xwave(int z){return 21*Math.sin((z+oz)*.019)+5*Math.sin((z+oz)*.061);}
 public Entrance entrance(int gx,int gz){
  if(hash(gx,81,gz)>.76)return null;
  int x=gx*CELL+32+(int)(hash(gx,82,gz)*64);
  int z=gz*CELL+64+(int)Math.round(zwave(x));
  if(Math.hypot(x,z)<250)return null;
  return new Entrance(x,z);
 }
 public Column column(int x,int z){
  if(Math.hypot(x,z)<216)return new Column(0,0,0,0,0,0,1000,false);
  double dx=period(x-64-xwave(z)),dz=period(z-64-zwave(x));
  double xd=29+4*Math.sin((z+oz)*.031),zd=29+4*Math.cos((x+ox)*.033);
  double deepX=period(x-64-32*Math.sin((z+oz)*.014+2.1));
  double deepZ=period(z-64-30*Math.sin((x+ox)*.016-1.7));
  Entrance e=entrance(Math.floorDiv(x,CELL),Math.floorDiv(z,CELL));
  double ed=e==null?1000:Math.hypot(x-e.x(),z-e.z());
  return new Column(dx,dz,xd,zd,deepX,deepZ,ed,true);
 }
 private static double sq(double x){return x*x;}
 public boolean air(Column c,int depth){
  if(!c.enabled()||depth<0||depth>145)return false;
  // Wide vertical entrances (6-10 blocks), opening directly into the shallow gallery.
  if(depth<=34&&c.entranceDist()<4.2+1.3*Math.sin(depth*.14))return true;
  if(depth>=10&&depth<=48){
   if(sq(c.xDist()/6.5)+sq((depth-c.xDepth())/5.5)<1||
      sq(c.zDist()/7.5)+sq((depth-c.zDepth())/6.5)<1)return true;
   if(sq(c.xDist()/17)+sq(c.zDist()/17)+sq((depth-29)/11.0)<1)return true;
   if(c.entranceDist()<9&&sq(c.entranceDist()/9)+sq((depth-29)/9.0)<1)return true;
  }
  // Vertical junctions link the shallow network with larger chambers and deep tunnels.
  if(depth>=25&&depth<=98&&sq(c.xDist()/5.5)+sq(c.zDist()/5.5)<1)return true;
  if(depth>=54&&depth<=107){
   if(sq(c.xDist()/8.5)+sq((depth-79)/7.0)<1||
      sq(c.zDist()/9)+sq((depth-82)/8.0)<1)return true;
   if(sq(c.xDist()/20)+sq(c.zDist()/20)+sq((depth-80)/17.0)<1)return true;
  }
  if(depth>=88&&depth<=140){
   if(sq(c.deepX()/6)+sq((depth-115)/6.0)<1||
      sq(c.deepZ()/7)+sq((depth-118)/7.0)<1)return true;
   if(sq(c.deepX()/14)+sq(c.deepZ()/14)+sq((depth-117)/11.0)<1)return true;
  }
  return false;
 }
}
