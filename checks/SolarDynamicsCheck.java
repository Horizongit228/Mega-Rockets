import dev.megarockets.*;
public class SolarDynamicsCheck{
 public static void main(String[] args){
  if(SolarDynamics.solarDayHours(Destination.MERCURY)<4000||SolarDynamics.solarDayHours(Destination.VENUS)<2800||SolarDynamics.rotationHours(Destination.VENUS)>=0||SolarDynamics.rotationHours(Destination.URANUS)>=0)throw new AssertionError("Solar day vs spin");
  for(Destination d:Destination.values()){
   long half=(long)(SolarDynamics.DAY_TICKS*SolarDynamics.solarDayHours(d)/48);
   if(Math.abs(SolarDynamics.localTime(d,half)-12000)>2)throw new AssertionError("Half day "+d);
   if(SolarDynamics.localTime(d,0)!=0)throw new AssertionError("Epoch");
   var a=SolarDynamics.position(d,0);var b=SolarDynamics.position(d,SolarDynamics.DAY_TICKS);
   if(a.distance(b.x(),b.z())<.01)throw new AssertionError("Stationary orbit "+d);
   if(!Double.isFinite(SolarDynamics.spin(d,1234567)))throw new AssertionError("Spin");
   var parent=d==Destination.MOON?SolarDynamics.position(null,0):d==Destination.EUROPA?SolarDynamics.position(Destination.JUPITER,0):d==Destination.TITAN?SolarDynamics.position(Destination.SATURN,0):new SolarDynamics.Position(0,0);
   double expected=d==Destination.MOON?12000:d==Destination.EUROPA?8000:d==Destination.TITAN?9500:d.solarOrbitKm/SolarScale.KM_PER_TRAVEL_BLOCK;
   if(Math.abs(a.distance(parent.x(),parent.z())-expected)>.001)throw new AssertionError("Orbit radius "+d);
  }
  if(SolarDynamics.heating(0,1000)!=0||SolarDynamics.heating(1.8,300)<SolarDynamics.heating(.16,300)||SolarDynamics.heating(1.8,1000)!=1)throw new AssertionError("Entry heat");
  var t=new AsteroidTerrain(42);int count=0;
  for(int i=900;i<1100;i++){var r=t.rock(i,0);if(r!=null){count++;if(!r.equals(t.rock(i,0))||t.material(r,r.x(),r.y(),r.z())==0||t.material(r,r.x()+100,r.y(),r.z())!=0)throw new AssertionError("Asteroid geometry");}}
  if(count<50||t.rock(0,0)!=null||t.rock(3000,0)!=null)throw new AssertionError("Asteroid belt bounds");
  for(Destination d:new Destination[]{Destination.MOON,Destination.MARS,Destination.EUROPA,Destination.NEPTUNE}){
   double x=SolarDynamics.AU+3200,z=0,y=180;var v=ManualFlight.STOP;int i=0;
   for(;i<400000;i++){
    var target=SolarDynamics.position(d,i);double dx=target.x()-x,dz=target.z()-z,dist=Math.hypot(dx,dz);
    if(dist<4&&v.length()<1)break;
    double cap=Math.min(15,Math.min(dist,Math.sqrt(dist*.5)));
    v=ManualFlight.toward(v,new ManualFlight.Motion(dx/dist*cap,0,dz/dist*cap),.5);x+=v.x();z+=v.z();
   }
   if(i>=400000)throw new AssertionError("Moving target guidance "+d);
   System.out.println("PASS moving target "+d+" "+i+" ticks");
  }
  System.out.println("PASS solar days, spin, orbit geometry, asteroid belt and entry heat");
 }
}
