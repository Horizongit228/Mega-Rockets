package dev.megarockets;
/** Deliberately fictional faster-than-light travel, separate from conventional flight. */
public final class JumpRules {
 public static final int CLEARANCE=160, MIN_TICKS=100, MAX_TICKS=280, TIMEOUT=600;
 public static final double ARRIVAL_Y=300;
 private JumpRules(){}
 public static int duration(double distance){return MIN_TICKS+(int)Math.min(MAX_TICKS-MIN_TICKS,Math.max(0,distance)/16000);}
 public static double offset(Destination body){return body!=null&&body.orbitOnly()?1200:450;}
 public static SolarDynamics.Position arrival(Destination body,long tick){
  var p=SolarDynamics.position(body,tick);
  double length=Math.hypot(p.x(),p.z());
  if(length<1)return new SolarDynamics.Position(p.x()+offset(body),p.z());
  return new SolarDynamics.Position(p.x()+offset(body)*p.x()/length,p.z()+offset(body)*p.z()/length);
 }
}
