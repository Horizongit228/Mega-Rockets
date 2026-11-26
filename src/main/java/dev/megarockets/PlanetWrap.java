package dev.megarockets;
/** Canonical square tile with a periodic surface. Periods approximate 2*pi*radius and align with chunks. */
public final class PlanetWrap {
 public static final int MOON_PERIOD=24960;
 private PlanetWrap(){}
 public static int period(int radius){return radius==4096?MOON_PERIOD:Math.max(64,32*(int)Math.round(2*Math.PI*radius/32));}
 public static int coordinate(int x,int period){return Math.floorMod(x+period/2,period)-period/2;}
 public static double coordinate(double x,int period){return x-period*Math.floor((x+period/2)/period);}
 public static double edgeWeight(int x,int z,int period){
  double distance=period/2.0-Math.max(Math.abs(coordinate(x,period)),Math.abs(coordinate(z,period)));
  double t=Math.max(0,Math.min(1,distance/192));
  return t*t*(3-2*t);
 }
 public static boolean seam(int x,int z,int period){return edgeWeight(x,z,period)<.02;}
}
