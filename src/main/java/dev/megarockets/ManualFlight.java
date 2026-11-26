package dev.megarockets;
/** Assisted free flight in blocks/tick. Pure math shared by tests and server. */
public final class ManualFlight {
 public static final int FORWARD=1,BACK=2,LEFT=4,RIGHT=8,UP=16,DOWN=32,BRAKE=64,VALID=127;
 public record Motion(double x,double y,double z){
  public double length(){return Math.sqrt(x*x+y*y+z*z);}
 }
 public static final Motion STOP=new Motion(0,0,0);
 private ManualFlight(){}
 public static Motion step(Motion previous,int mask,double yaw,double cap,double accel){
  mask&=VALID;
  double a=Math.toRadians(yaw),forward=((mask&FORWARD)!=0?1:0)-((mask&BACK)!=0?1:0),
   right=((mask&RIGHT)!=0?1:0)-((mask&LEFT)!=0?1:0),up=((mask&UP)!=0?1:0)-((mask&DOWN)!=0?1:0);
  double x=-Math.sin(a)*forward+Math.cos(a)*right,z=Math.cos(a)*forward+Math.sin(a)*right;
  double len=Math.sqrt(x*x+up*up+z*z),scale=len==0||(mask&BRAKE)!=0?0:cap/len;
  double dx=x*scale-previous.x(),dy=up*scale-previous.y(),dz=z*scale-previous.z();
  double diff=Math.sqrt(dx*dx+dy*dy+dz*dz);
  double max=accel*((mask&BRAKE)!=0?4:len==0?2:1);
  double t=diff==0?0:Math.min(1,max/diff);
  Motion next=new Motion(previous.x()+dx*t,previous.y()+dy*t,previous.z()+dz*t);
  if(next.length()>cap){double k=cap/next.length();next=new Motion(next.x()*k,next.y()*k,next.z()*k);}
  return next.length()<1e-6?STOP:next;
 }
}
