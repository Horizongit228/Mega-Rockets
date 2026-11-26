package dev.megarockets;

/** Aurora outpost: deterministic, block-built modules. Coordinates are relative to Z. */
public final class StationLayout {
 public static final int Z=-12000,BASE_Y=100,PAD_X=32,TOP_Y=110,PAD_COUNT=3;
 public static final int AIR=0,HULL=1,GLASS=2,LAMP=3,OXYGEN=4,DOOR_LOWER=5,DOOR_UPPER=6,PAD=7,MARK=8,
  BLUE=9,FRAME=10,WORKBENCH=11,BOOKS=12,MOSS=13,LEAVES=14,SOIL=15,CONSOLE=16,SOLAR=17,
  NORTH_DOOR_LOWER=18,NORTH_DOOR_UPPER=19,INNER_DOOR_LOWER=20,INNER_DOOR_UPPER=21,STATUS=22,BATTERY=23,CABLE=24;
 public static final int[][] OXYGEN_POINTS={{11,-1,105},{-5,0,107},{-21,3,108},{5,-17,109},{5,17,109}};
 private StationLayout(){}
 public static int padZ(int index){return Z+(index==1?-36:index==2?36:0);}
 public static boolean nearby(double x,double z){return Math.abs(x-PAD_X)<190&&Math.abs(z-Z)<190;}
 public static boolean gravity(double x,double y,double z){return x>=-52&&x<=46&&Math.abs(z-Z)<=51&&y>=99&&y<=114;}
 public static boolean column(int x,int z){return x>=-50&&x<=46&&Math.abs(z-Z)<=50;}
 public static boolean solarAnchor(int x,int q){return x==-27&&(q==-28||q==-19||q==19||q==28);}
 public static boolean padFloor(int x,int q,int pad){
  return Math.abs(x-PAD_X)<=12&&Math.abs(q-(padZ(pad)-Z))<=12;
 }
 public static int consoleSide(int x,int q,int y){
  if(y!=101||q!=1)return 0;
  if(x==7||x==10)return -1;
  if(x==12||x==15)return 1;
  return 0;
 }
 public static int material(int x,int y,int z){
  int q=z-Z,a=Math.abs(q);
  if(!column(x,z)||y<98||y>TOP_Y)return AIR;
  // Independent roof arrays for four modules; workshop draws from the two large wings.
  for(int[] point:OXYGEN_POINTS){
   int ox=point[0],oz=point[1],roof=point[2];
   if(ox==-21){
    if(x==ox&&q==oz&&y==100)return BATTERY;
    if(y==99&&(x==-27&&q>=-28&&q<=28||q==oz&&x>=-27&&x<=ox))return CABLE;
   }else{
    if(x==ox&&q==oz&&y==102)return BATTERY;
    if(x==ox&&q==oz&&y>=103&&y<=roof)return CABLE;
    if(y==roof+1&&q==oz&&Math.abs(x-ox)<=1)return SOLAR;
   }
  }
  // Three separate 25x25 landing decks, runway markers and a connecting exterior spine.
  for(int pad=0;pad<PAD_COUNT;pad++)if(padFloor(x,q,pad)){
   int dx=x-PAD_X,dz=q-(padZ(pad)-Z);
   if(y==100)return Math.abs(dx)==12||Math.abs(dz)==12||Math.abs(dx)==8&&Math.abs(dz)<=5||Math.abs(dz)==1&&Math.abs(dx)<=8?MARK:PAD;
   if(y==99&&(Math.abs(dx)==12||Math.abs(dz)==12))return FRAME;
   if(y==101&&Math.abs(dx)==12&&Math.abs(dz)==12)return LAMP;
   return AIR;
  }
  if(y==100&&((x>=15&&x<=19&&a<=36)||(x>=9&&x<=19&&a<=2)))return a==0||x==17?MARK:PAD;
  if(y==101&&x==15&&q==1)return CONSOLE;
  // Main airlock, original coordinates retained, both doors are iron and normally shut.
  if(x>=9&&x<=14&&a<=2&&y>=100&&y<=105){
   if(x==14&&q==0&&(y==101||y==102))return y==101?DOOR_LOWER:DOOR_UPPER;
   if(y==100||y==105||a==2||x==14)return y==105&&q==0?LAMP:HULL;
   if(q==1&&(x==10||x==12)&&y==101)return CONSOLE;
   if(x==11&&q==-1&&y==101)return OXYGEN;
   return AIR;
  }
  // Solar wing: two large arrays attached to the west truss. Four anchor cells are monitored.
  if(x>=-31&&x<=-23&&a>=17&&a<=30){
   if(y==100)return x==-31||x==-23||a==17||a==30?FRAME:SOLAR;
   if(y==99&&(x==-27||a==23))return FRAME;
  }
  if(y==100&&x==-27&&a>=7&&a<=17)return FRAME;
  if(y==99&&x>=-50&&x<=8&&q==0)return FRAME;
  // Connect the three auxiliary rooms through narrow sealed passageways.
  if(x>=-17&&x<=-9&&a<=2&&y>=100&&y<=105){
   if(y==100||y==105||a==2)return y==105&&q==0?LAMP:HULL;
   return AIR;
  }
  if(x>=-2&&x<=2&&a>=7&&a<=13&&y>=100&&y<=105){
   if(y==100||y==105||Math.abs(x)==2)return y==105&&x==0?LAMP:HULL;
   return AIR;
  }
  // Command and habitation module.
  if(x>=-8&&x<=8&&a<=6&&y>=100&&y<=107){
   if(x==8&&q==0&&(y==101||y==102))return y==101?DOOR_LOWER:DOOR_UPPER;
   if(x==-8&&q==0&&(y==101||y==102))return y==101?INNER_DOOR_LOWER:INNER_DOOR_UPPER;
   if(x==0&&a==6&&(y==101||y==102))return y==101?NORTH_DOOR_LOWER:NORTH_DOOR_UPPER;
   if(y==100)return (x==0||q==0)?BLUE:HULL;
   if(y==107||x==-8||x==8||a==6){
    if(y==107&&Math.abs(x)%4==0&&a==3)return LAMP;
    if(a==6&&y>=103&&y<=105&&Math.abs(x)<6)return GLASS;
    if(y==107&&Math.abs(x)<=4&&a<=2)return GLASS;
    return y==106?FRAME:HULL;
   }
   if(y==101&&x==-5&&q==0)return OXYGEN;
   if(y==101&&x==7&&q==1)return CONSOLE;
   if(y==101&&x==5&&q==-4)return STATUS;
   if(y==101&&q==4&&x>=-5&&x<=-2)return BOOKS;
   if(y==101&&q==-4&&x>=-5&&x<=-2)return WORKBENCH;
   return AIR;
  }
  // Workshop, observation room and greenhouse; each has its own oxygen source.
  if(x>=-32&&x<=-18&&a<=7&&y>=100&&y<=108){
   if(x==-18&&q==0&&(y==101||y==102))return y==101?INNER_DOOR_LOWER:INNER_DOOR_UPPER;
   if(y==100||y==108||x==-32||x==-18||a==7){
    if(y==108&&x%4==0&&a==3)return LAMP;
    if(x==-32&&y>=103&&y<=106&&a<=5)return GLASS;
    return y==107?FRAME:HULL;
   }
   if(y==101&&x==-21&&q==3)return OXYGEN;
   if(y==101&&a==5&&x<=-23&&x>=-29)return q<0?WORKBENCH:BOOKS;
   if(y==101&&x==-30&&a<=3)return BLUE;
   return AIR;
  }
  if(x>=-7&&x<=7&&a>=14&&a<=28&&y>=100&&y<=109){
   if(x==0&&a==14&&(y==101||y==102))return y==101?NORTH_DOOR_LOWER:NORTH_DOOR_UPPER;
   if(y==100||y==109||Math.abs(x)==7||a==14||a==28){
    if(y==109&&Math.abs(x)%4==0&&a%4==0)return LAMP;
    if(y>=103&&((q<0&&a==28)||(q>0&&(Math.abs(x)==7||y==109))))return GLASS;
    return y==108?FRAME:HULL;
   }
   if(y==101&&x==5&&a==17)return OXYGEN;
   if(q<0){if(y==101&&a==25&&Math.abs(x)<=4)return BLUE;if(y==101&&x==-5&&a==17)return STATUS;}
   else if(Math.abs(x)>=3&&Math.abs(x)<=5&&a>=18&&a<=25){
    if(y==101)return SOIL;
    if(y==102)return (x+a)%3==0?LEAVES:MOSS;
   }
   return AIR;
  }
  return AIR;
 }
 public static int height(int x,int z){for(int y=TOP_Y;y>=98;y--)if(material(x,y,z)!=AIR)return y+1;return -64;}
}
