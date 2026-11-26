package dev.megarockets;
/** Gameplay energy units, not watts. Network updates once per second. */
public final class EnergyRules {
 public static final int CAPACITY=60000,DEMAND=1,PEAK=16;
 private EnergyRules(){}
 public static int solar(long time,boolean orbit,boolean exposed,double au,double weather){
  if(!exposed)return 0;
  double angle=orbit?1:Math.max(0,Math.sin(Math.floorMod(time,24000)*Math.PI/12000));
  return (int)Math.floor(PEAK*angle*Math.min(2,1/Math.max(.1,au))*Math.max(0,Math.min(1,weather)));
 }
 public static int stored(int energy,int generation,int consumers,int capacity){
  return Math.max(0,Math.min(capacity,energy+generation-consumers*DEMAND));
 }
}
