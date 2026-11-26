package dev.megarockets;
/** NASA JPL volume-equivalent mean radii. Travel is a separate fixed, compressed coordinate model. */
public final class SolarScale {
 public static final double MOON_RADIUS_KM=1737.4,EARTH_RADIUS_KM=6371.0084,KM_PER_TRAVEL_BLOCK=2000;
 public static final int MOON_RADIUS_BLOCKS=4096,MOON_ROUTE=12000;
 public static final long EARTH_ORBIT_KM=149598262L;
 public static final double KM_PER_SIZE_BLOCK=MOON_RADIUS_KM/MOON_RADIUS_BLOCKS;
 private SolarScale(){}
 public static int radius(double meanKm){return (int)Math.round(meanKm/KM_PER_SIZE_BLOCK);}
 public static double travel(long referenceKm){return Math.round(referenceKm/KM_PER_TRAVEL_BLOCK);}
 /** Far-away apparent radius from reference kilometres; close approach is deliberately enlarged. */
 public static double apparent(double radiusKm,double rangeBlocks){
  return Math.max(.0015,Math.min(.67,radiusKm/(Math.max(0,rangeBlocks)*KM_PER_TRAVEL_BLOCK+1.5*radiusKm)));
 }
}
