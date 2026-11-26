package dev.megarockets;
/** A stationary orbital outpost in an otherwise empty region, generated only in new chunks.
 * Pure coordinates permit generation and navigation to share exactly the same footprint. */
public final class StationLayout {
 public static final int Z=-12000,BASE_Y=100,PAD_X=32;
 public static final int AIR=0,HULL=1,GLASS=2,LAMP=3,OXYGEN=4,DOOR_LOWER=5,DOOR_UPPER=6,PAD=7,MARK=8;
 private StationLayout(){}
 public static boolean nearby(double x,double z){return Math.abs(x-PAD_X)<160&&Math.abs(z-Z)<160;}
 public static boolean gravity(double x,double y,double z){return x>=-10&&x<=46&&Math.abs(z-Z)<=14&&y>=99&&y<=112;}
 public static boolean column(int x,int z){return x>=-8&&x<=44&&Math.abs(z-Z)<=12;}
 public static int material(int x,int y,int z){
  int q=z-Z,a=Math.abs(q);
  if(!column(x,z))return AIR;
  // Landing apron with a clearly marked centre, plus catwalk to the airlock.
  if(y==BASE_Y&&((x>=20&&x<=44&&a<=12)||(x>=9&&x<=20&&a<=2))){
   return (x==PAD_X||a==0||x==20||x==44||a==12)?MARK:PAD;
  }
  if(x>=-8&&x<=8&&a<=6&&y>=100&&y<=107){
   if(x==8&&q==0&&(y==101||y==102))return y==101?DOOR_LOWER:DOOR_UPPER;
   if(y==100||y==107||x==-8||x==8||a==6){
    if(y==107&&Math.abs(x)%4==0&&a==3)return LAMP;
    if(a==6&&y>=103&&y<=105&&Math.abs(x)<6)return GLASS;
    if(y==107&&Math.abs(x)<=4&&a<=2)return GLASS;
    return HULL;
   }
   if(x==-5&&q==0&&y==101)return OXYGEN;
   return AIR;
  }
  if(x>=9&&x<=14&&a<=2&&y>=100&&y<=105){
   if(x==14&&q==0&&(y==101||y==102))return y==101?DOOR_LOWER:DOOR_UPPER;
   if(y==100||y==105||a==2||x==14)return y==105&&q==0?LAMP:HULL;
   if(x==11&&q==1&&y==101)return OXYGEN;
  }
  return AIR;
 }
 public static int height(int x,int z){for(int y=107;y>=100;y--)if(material(x,y,z)!=AIR)return y+1;return -64;}
}
