package dev.megarockets;
/** Sparse deterministic main-belt rock clusters. Material: 0 air, 1 stone, 2 iron, 3 basalt. */
public final class AsteroidTerrain {
 public static final int CELL=192;
 public static final double INNER=2.1*SolarDynamics.AU,OUTER=3.3*SolarDynamics.AU;
 public record Rock(int x,int y,int z,int rx,int ry,int rz,long seed){}
 private final long seed;
 public AsteroidTerrain(long seed){this.seed=seed;}
 private static long hash(long n){n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;return n^(n>>>31);}
 private static int value(long n,int bound){return (int)Math.floorMod(hash(n),bound);}
 public Rock rock(int gx,int gz){
  long k=hash(seed+gx*341873128712L+gz*132897987541L);
  if(value(k,100)>=58)return null;
  int x=gx*CELL+32+value(k+1,CELL-64),z=gz*CELL+32+value(k+2,CELL-64);
  double r=Math.hypot(x,z);if(r<INNER||r>OUTER)return null;
  return new Rock(x,65+value(k+3,180),z,6+value(k+4,19),5+value(k+5,15),6+value(k+6,19),k);
 }
 public int material(Rock r,int x,int y,int z){
  if(r==null)return 0;
  double a=(x-r.x())/(double)r.rx(),b=(y-r.y())/(double)r.ry(),c=(z-r.z())/(double)r.rz();
  double rough=.12*Math.sin(x*.41+y*.28)+.08*Math.cos(z*.37-x*.17);
  double d=a*a+b*b+c*c;if(d>1+rough)return 0;
  int n=value(r.seed()+x*42317861L+y*19349663L+z*83492791L,100);
  return n<4&&d<.8?2:n<40?3:1;
 }
 public Rock at(int x,int z){return rock(Math.floorDiv(x,CELL),Math.floorDiv(z,CELL));}
}
