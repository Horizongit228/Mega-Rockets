package dev.megarockets;

/** First lunar settlement. Relative vertical coordinates start at the deck surface. */
public final class LunarVillageLayout {
 public static final int X=320,Z=0,MIN_X=-67,MAX_X=106,MIN_Z=-40,MAX_Z=40;
 public static final int NONE=-1,AIR=0,FOUNDATION=1,WHITE=2,BLUE=3,GLASS=4,LAMP=5,ROAD=6,
 OXYGEN=7,DOOR_LOWER=8,DOOR_UPPER=9,MARKER=10,WORK=11,BOOKS=12,GREEN=13,SOIL=14,
 PAD=15,HAZARD=16,VENT=17,LECTERN=18,LEAVES=19,SOLAR=20,BATTERY=21,CABLE=22;
 public record Home(int x,int z,int kind,int halfX,int halfZ,int height){}
 public static final Home[] HOMES={
  new Home(-32,-21,0,8,7,6),new Home(0,-21,0,8,7,6),new Home(32,-21,0,8,7,6),
  new Home(-32,21,0,8,7,6),new Home(0,21,0,8,7,6),new Home(32,21,0,8,7,6),
  new Home(-53,0,1,10,9,8),new Home(56,0,2,10,9,8)};
 private LunarVillageLayout(){}
 public static boolean bounds(int x,int z){return x>=X+MIN_X&&x<=X+MAX_X&&z>=Z+MIN_Z&&z<=Z+MAX_Z;}
 public static boolean footprint(int x,int z){
  int a=x-X,b=z-Z;
  if(!bounds(x,z))return false;
  if(Math.abs(b)<=3&&a>=-43&&a<=105||Math.abs(a+32)<=2&&Math.abs(b)<=14||Math.abs(a)<=2&&Math.abs(b)<=14||Math.abs(a-32)<=2&&Math.abs(b)<=14)return true;
  if(Math.abs(a-92)<=13&&Math.abs(b)<=13)return true;
  for(Home h:HOMES)if(Math.abs(a-h.x())<=h.halfX()&&Math.abs(b-h.z())<=h.halfZ())return true;
  return false;
 }
 public static int base(MoonTerrain terrain){
  int top=Integer.MIN_VALUE;
  for(int x=X+MIN_X;x<=X+MAX_X;x+=4)for(int z=Z+MIN_Z;z<=Z+MAX_Z;z+=4)
   top=Math.max(top,terrain.column(x,z).top()-MoonTerrain.NORTH_OFFSET);
  return top+3;
 }
 public static int markerX(Home h){return X+h.x()-2;}
 public static int markerZ(Home h){return Z+h.z()+2;}
 public static int material(int x,int y,int z){
  if(!footprint(x,z))return NONE;
  int a=x-X,b=z-Z;
  if(y<0)return FOUNDATION;
  for(Home h:HOMES){
   int u=a-h.x(),v=b-h.z(),ax=Math.abs(u),az=Math.abs(v);
   if(ax>h.halfX()||az>h.halfZ())continue;
   int roof=h.height();
   int ox=-h.halfX()+2;
   if(u==ox&&v==0&&y==2)return BATTERY;
   if(u==ox&&v==0&&y>=3&&y<=roof)return CABLE;
   if(y==roof+1&&v==0&&Math.abs(u-ox)<=1)return SOLAR;
   // Door faces the central avenue. Community buildings use the east/west wall.
   boolean door=h.kind()==0?u==0&&v==(h.z()<0?h.halfZ():-h.halfZ()):v==0&&(u==(h.x()<0?h.halfX():-h.halfX())||h.kind()==2&&u==h.halfX());
   if(door&&(y==1||y==2))return y==1?DOOR_LOWER:DOOR_UPPER;
   if(y==0)return BLUE;
   if(y==roof){
    if(u%4==0&&v%4==0)return LAMP;
    return h.kind()==2?GLASS:WHITE;
   }
   if(y==roof+1&&(ax==h.halfX()||az==h.halfZ()))return BLUE;
   if(y>roof)return AIR;
   if(ax==h.halfX()||az==h.halfZ()){
    if(y>=3&&y<=roof-2&&!door&&ax!=h.halfX()&&u%4!=0||y>=3&&y<=roof-2&&ax==h.halfX()&&az<h.halfZ()-1&&v%4!=0)return GLASS;
    return y==roof-1?BLUE:y==1&&u%4==0?VENT:WHITE;
   }
   if(y==1&&u==-h.halfX()+2&&v==0)return OXYGEN;
   if(h.kind()==0){
    if(y==1&&u==-2&&v==2)return MARKER;
    if(y==1&&v==-h.halfZ()+2&&u>=2&&u<=4)return WORK;
    if(y==1&&v==h.halfZ()-2&&u<=-3&&u>=-5)return BOOKS;
   }else if(h.kind()==1){
    if(y==1&&u==-2&&v==2)return MARKER;
    if(y==1&&v==-5&&ax<=5)return BOOKS;
    if(y==1&&v==4&&u>=2&&u<=6)return WORK;
    if(y==1&&u==4&&v==-2)return LECTERN;
   }else{
    if(ax>=3&&ax<=7&&az<=5){
     if(y==1)return SOIL;
     if(y==2)return (u+v)%3==0?LEAVES:GREEN;
    }
   }
   return AIR;
  }
  // Eastern greenhouse entrance: enclosed vestibule joining the landing apron road.
  if(a>=67&&a<=72&&Math.abs(b)<=2){
   if(a==72&&b==0&&(y==1||y==2))return y==1?DOOR_LOWER:DOOR_UPPER;
   if(y==0)return BLUE;
   if(y==4)return b==0?LAMP:WHITE;
   if(y>4)return AIR;
   if(Math.abs(b)==2||a==72)return y==3?GLASS:WHITE;
   return AIR;
  }
  if(Math.abs(a-92)<=13&&Math.abs(b)<=13){
   int dx=a-92,dz=b;
   if(y==0)return Math.abs(dx)==13||Math.abs(dz)==13||Math.abs(dx)==8&&Math.abs(dz)<=5||Math.abs(dz)==1&&Math.abs(dx)<=8?HAZARD:PAD;
   if(y==1&&Math.abs(dx)==13&&Math.abs(dz)==13)return LAMP;
   return AIR;
  }
  if(y==0)return ROAD;
  if(y==1&&a%16==0&&Math.abs(b)==3)return LAMP;
  return AIR;
 }
 public static int top(int x,int z){for(int y=10;y>=0;y--)if(material(x,y,z)>0)return y;return -1;}
}
