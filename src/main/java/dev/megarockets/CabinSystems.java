package dev.megarockets;
/** Gameplay model, not a thermodynamics simulator. Percent values are integer and saved. */
public final class CabinSystems {
 public static final int SAFE_PRESSURE=20;
 private CabinSystems(){}
 public static int clamp(int p){return Math.max(0,Math.min(100,p));}
 public static int pressure(int current,boolean life,boolean breathable){return clamp(current+(life||breathable?4:-2));}
 public static int heat(int current,boolean thrust,int throttle){return clamp(current+(thrust?Math.max(1,throttle/25): -3));}
 public static int throttle(int value,int delta){return Math.max(25,Math.min(100,value+delta));}
 public static boolean breathable(int pressure){return pressure>=SAFE_PRESSURE;}
}
