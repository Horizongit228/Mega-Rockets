package dev.megarockets;
/** Shared geometry for a gameplay-scale transition from space to terrain. */
public final class ApproachRules {
 public static final double LOAD_RADIUS=2800,ENTRY_RADIUS=900,EARTH_X=-3500,EARTH_Z=0,EXIT_OFFSET=3200;
 public static final int CHUNK_RADIUS=2,CHUNKS=25,CLEARANCE=160;
 private ApproachRules(){}
 public static double distance(double x,double z,double gateX,double gateZ){return Math.hypot(x-gateX,z-gateZ);}
 public static int local(double delta){return (int)Math.round(Math.max(-1200,Math.min(1200,delta*.45)));}
 public static double descentCap(double agl){return agl<=8?.25:agl<=32?.65:agl<=100?1.2:2.0;}
 public static double surfaceCap(double agl){return agl<=12?.5:agl<=80?2:Math.min(8,2+(agl-80)*.025);}
 public static int chunkX(int index){return index%5-2;}
 public static int chunkZ(int index){return index/5-2;}
}
