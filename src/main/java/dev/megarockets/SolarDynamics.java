package dev.megarockets;
/** Circular coplanar mean orbits; one simulated Earth day per 24000 ticks. */
public final class SolarDynamics {
 public static final double DAY_TICKS=24000,AU=149598262.0/SolarScale.KM_PER_TRAVEL_BLOCK;
 public record Position(double x,double z){
  public Position add(Position b){return new Position(x+b.x,z+b.z);}
  public double distance(double x,double z){return Math.hypot(this.x-x,this.z-z);}
 }
 private SolarDynamics(){}
 public static double solarDayHours(Destination d){
  if(d==null)return 24;
  return switch(d){case MERCURY->4222.6;case VENUS->2802;case MOON->708.7;case MARS->24.7;
   case EUROPA->85.2;case TITAN->382.7;case JUPITER->9.9;case SATURN->10.7;case URANUS->17.2;case NEPTUNE->16.1;};
 }
 public static double rotationHours(Destination d){
  if(d==null)return 23.9345;
  return switch(d){case MERCURY->1407.6;case VENUS->-5832.5;case MOON->655.7;case MARS->24.6;
   case EUROPA->85.2;case TITAN->382.7;case JUPITER->9.9;case SATURN->10.7;case URANUS->-17.2;case NEPTUNE->16.1;};
 }
 public static double periodDays(Destination d){
  if(d==null)return 365.256;
  return switch(d){case MERCURY->87.969;case VENUS->224.701;case MOON->27.322;case MARS->686.98;
   case EUROPA->3.55118;case TITAN->15.9454;case JUPITER->4332.59;case SATURN->10759.22;case URANUS->30688.5;case NEPTUNE->60182;};
 }
 public static double phase(double ticks,double days){return 2*Math.PI*Math.IEEEremainder(ticks/DAY_TICKS/days,1);}
 private static Position circle(double radius,double angle){return new Position(radius*Math.cos(angle),radius*Math.sin(angle));}
 public static Position position(Destination d,double ticks){
  if(d==null)return circle(AU,phase(ticks,periodDays(null)));
  double a=phase(ticks,periodDays(d));
  if(d==Destination.MOON)return position(null,ticks).add(circle(12000,a+.5));
  if(d==Destination.EUROPA)return position(Destination.JUPITER,ticks).add(circle(8000,a+1.2));
  if(d==Destination.TITAN)return position(Destination.SATURN,ticks).add(circle(9500,a+2.1));
  return circle(d.solarOrbitKm/SolarScale.KM_PER_TRAVEL_BLOCK,a);
 }
 public static long localTime(Destination d,long ticks){
  double cycle=ticks/(DAY_TICKS*solarDayHours(d)/24);
  double fraction=cycle-Math.floor(cycle);
  if(d==Destination.VENUS||d==Destination.URANUS)fraction=(1-fraction)%1;
  return (long)(fraction*24000);
 }
 public static double spin(Destination d,double ticks){return phase(ticks,rotationHours(d)/24);}
 public static double atmosphere(Destination d){
  if(d==null)return 1;
  return switch(d){case VENUS->1.8;case MARS->.16;case TITAN->.8;default->0;};
 }
 public static double heating(double density,double speed){return Math.max(0,Math.min(1,density*speed*speed/90000));}
}
