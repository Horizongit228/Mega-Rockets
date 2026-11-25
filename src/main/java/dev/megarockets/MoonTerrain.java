package dev.megarockets;

/** Seeded multi-biome terrain shared by all three slices of the finite Moon. */
public final class MoonTerrain {
 public static final int RADIUS=4096,MIN_Y=-1536,HEIGHT=3072,NORTH_OFFSET=3072,PAD_Y=1280,CRUST=192;
 public static final int PAD_RADIUS=24,SAFE_RADIUS=192,PAD_GLOBAL_Y=PAD_Y+NORTH_OFFSET-1;
 public static final int AIR=0,ROCK=1,REGOLITH=2,BASALT=3,ORE=4,PAD=5,WOOD=6,LEAVES=7;
 public enum Region { PLAINS,HIGHLANDS,CRATERS,FOREST }
 public record Column(int bottom,int top,int innerBottom,int innerTop,boolean pad,double tunnelX,double tunnelZ,Region region,long seed){}
 public enum TreeKind { ROUND, SPIRE, ANCIENT }
 public record Tree(int x,int y,int z,int height,TreeKind kind){}
 public record Boulder(int x,int y,int z,int radius,boolean crystal){}
 private final long seed;
 private final double ox,oy,oz;
 public MoonTerrain(long seed){this.seed=seed;ox=10000*hash(seed,0,0,1);oy=10000*hash(seed,0,0,2);oz=10000*hash(seed,0,0,3);}
 private static double clamp(double n,double a,double b){return Math.max(a,Math.min(b,n));}
 private static double smooth(double n){n=clamp(n,0,1);return n*n*(3-2*n);}
 private static double mix(double a,double b,double t){return a+(b-a)*t;}
 private static double hash(long seed,int x,int y,int z){
  long n=seed^ (x*341873128712L+y*132897987541L+z*42317861L);
  n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;n^=n>>>31;return(n>>>11)*0x1.0p-53;
 }
 private double field(double x,double z,int salt){
  int ix=(int)Math.floor(x),iz=(int)Math.floor(z);double tx=smooth(x-ix),tz=smooth(z-iz);
  return 2*mix(mix(hash(seed,ix,salt,iz),hash(seed,ix+1,salt,iz),tx),
      mix(hash(seed,ix,salt,iz+1),hash(seed,ix+1,salt,iz+1),tx),tz)-1;
 }
 public Region region(int x,int z){
  if(Math.hypot(x,z)<SAFE_RADIUS)return Region.PLAINS;
  double relief=field(x/410.0,z/410.0,51),forest=field(x/270.0,z/270.0,63);
  if(relief>.28)return Region.HIGHLANDS;
  if(relief<-.32)return Region.CRATERS;
  return forest>.02?Region.FOREST:Region.PLAINS;
 }
 private double crater(int x,int z){
  int gx=Math.floorDiv(x,256),gz=Math.floorDiv(z,256);double sum=0;
  for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
   int a=gx+dx,b=gz+dz;
   if(hash(seed,a,301,b)>.58)continue;
   double cx=a*256+48+160*hash(seed,a,302,b),cz=b*256+48+160*hash(seed,a,303,b);
   double radius=25+95*hash(seed,a,304,b),d=Math.hypot(x-cx,z-cz)/radius;
   if(d<1.28)sum+=radius*(-.32*Math.pow(Math.max(0,1-d*d),2)+.10*Math.exp(-Math.pow((d-1)/.23,2)))*smooth((1.28-d)/.28);
  }
  return sum;
 }
 public Column column(int x,int z){
  double r=Math.hypot(x,z);Region region=region(x,z);
  if(r>=RADIUS)return new Column(1,0,1,0,false,0,0,region,seed);
  double h=Math.sqrt((double)RADIUS*RADIUS-r*r),taper=smooth((RADIUS-r)/160);
  double macro=field(x/410.0,z/410.0,51),high=smooth((macro+.12)/.65),basin=smooth((-macro-.15)/.5);
  double broad=field(x/260.0,z/260.0,71)*25;
  double rolling=field(x/95.0,z/95.0,73)*12+field(x/40.0,z/40.0,77)*5;
  double ridgeNoise=field(x/145.0,z/145.0,91);
  double ridge=1-ridgeNoise*ridgeNoise;
  double mountain=high*(85*ridge*ridge+14*field(x/40.0,z/40.0,97)+5*field(x/18.0,z/18.0,101));
  double detail=1.4*field(x/12.0,z/12.0,113)+.45*field(x/5.0,z/5.0,127);
  double top=h+(broad+rolling+mountain+detail+crater(x,z)*(.25+.75*basin)-20*basin)*taper;
  if(r<=PAD_RADIUS)top=PAD_GLOBAL_Y;
  else if(r<720){
   // A continuous, broad apron around the highland instead of a steep artificial rim.
   double distance=r-PAD_RADIUS;
   double fall=Math.min(170,.42*distance);
   double apron=PAD_GLOBAL_Y-fall+clamp(rolling*.25+detail,-4,4)*smooth(distance/32);
   top=mix(apron,top,smooth((r-SAFE_RADIUS)/(720-SAFE_RADIUS)));
   if(r<SAFE_RADIUS)top=Math.min(PAD_GLOBAL_Y-1,top);
  }
  int bottom=(int)Math.ceil(-h+35*field(x/250.0,z/250.0,131)*taper),t=(int)Math.floor(top);
  double inner=RADIUS-CRUST;int half=r<inner?(int)Math.floor(Math.sqrt(inner*inner-r*r)):-1;
  int ib=half<0?1:Math.max(bottom+8,-half),it=half<0?0:Math.min(t-24,half);
  return new Column(bottom,t,ib,it,r<=PAD_RADIUS,Math.sin((x+ox)*.078+(z+oz)*.025),Math.cos((z+oz)*.071-(x+ox)*.019),region,seed);
 }
 public int material(Column c,int x,int y,int z){
  if(y<c.bottom()||y>c.top()||(y>=c.innerBottom()&&y<=c.innerTop()))return AIR;
  int depth=Math.min(c.top()-y,y-c.bottom());
  if(c.pad()&&y>=PAD_GLOBAL_Y-15)return y==PAD_GLOBAL_Y?PAD:ROCK;
  if(depth>5){
   double a=c.tunnelX()+Math.sin((y+oy)*.094+(z+oz)*.026),b=c.tunnelZ()+Math.cos((y+oy)*.071-(x+ox)*.019);
   double chamber=Math.sin((x+ox)*.035+(y+oy)*.048)*Math.cos((z+oz)*.043-(y+oy)*.029)*Math.sin((y+oy)*.039);
   double gallery=Math.sin((x+ox)*.018+(z+oz)*.011)*Math.cos((y+oy)*.026)+Math.sin((z+oz)*.021-(y+oy)*.019);
   if(a*a+b*b<.10||chamber>.85||(depth>15&&gallery>1.73))return AIR;
  }
  if(depth<=4)return c.region()==Region.HIGHLANDS&&depth>1?ROCK:REGOLITH;
  double n=hash(c.seed(),x,y,z);
  // Continuous mineral lenses instead of unrelated single-block ore specks.
  if(depth>10){
   double vein=Math.sin((x+ox)*.17+(y+oy)*.095)*Math.cos((z+oz)*.14-(y+oy)*.08)
       +.6*Math.sin((x+ox)*.075-(z+oz)*.063+(y+oy)*.11);
   if(n<.003||vein>1.23&&n<.47)return ORE;
  }
  double strata=Math.sin((y+oy)*.18+Math.sin((x+ox)*.018)*1.7+Math.cos((z+oz)*.017));
  return strata>(c.region()==Region.CRATERS?.25:.72)||n<.035?BASALT:ROCK;
 }
 /** One candidate per 16x16 cell, jittered; canopy is reconstructed in every intersecting chunk. */
 public Tree tree(int gx,int gz){
  int x=gx*16+4+(int)(hash(seed,gx,501,gz)*8),z=gz*16+4+(int)(hash(seed,gx,502,gz)*8);
  if(Math.hypot(x,z)<SAFE_RADIUS+16||Math.hypot(x,z)>RADIUS-64)return null;
  Region region=region(x,z);double chance=region==Region.FOREST?.78:region==Region.PLAINS?.08:0;
  if(hash(seed,gx,503,gz)>=chance)return null;
  Column c=column(x,z);int y=c.top()+1;
  if(y<1||Math.abs(column(x-2,z).top()-c.top())>3||Math.abs(column(x+2,z).top()-c.top())>3||
      Math.abs(column(x,z-2).top()-c.top())>3||Math.abs(column(x,z+2).top()-c.top())>3)return null;
  double kind=hash(seed,gx,505,gz);
  TreeKind type=kind<.48?TreeKind.ROUND:kind<.82?TreeKind.SPIRE:TreeKind.ANCIENT;
  int h=(type==TreeKind.ANCIENT?12:type==TreeKind.SPIRE?9:6)+(int)(hash(seed,gx,504,gz)*5);
  return new Tree(x,y,z,h,type);
 }
 public int treeBlock(Tree t,int x,int y,int z){
  int dx=x-t.x(),dz=z-t.z(),dy=y-t.y(),h=t.height();
  if(dx==0&&dz==0&&dy>=0&&dy<h)return WOOD;
  if(t.kind()==TreeKind.ANCIENT&&dy>=0&&dy<3&&Math.abs(dx)<=1&&Math.abs(dz)<=1)return WOOD;
  int layer=dy-h;
  int radius=switch(t.kind()){
   case ROUND -> layer==-2?2:layer==-1||layer==0?3:layer==1?2:layer==2?1:-1;
   case SPIRE -> layer>=-5&&layer<=2?Math.max(1,3-(layer+5)/3):-1;
   case ANCIENT -> layer==-3?3:layer==-2||layer==-1||layer==0?5:layer==1?4:layer==2?2:-1;
  };
  if(t.kind()==TreeKind.ANCIENT&&dy>=h-4&&dy<=h-3&&
      ((dx==0&&Math.abs(dz)<=3)||(dz==0&&Math.abs(dx)<=3)))return WOOD;
  if(radius<0||Math.abs(dx)>radius||Math.abs(dz)>radius)return AIR;
  return dx*dx+dz*dz<=radius*radius+1?LEAVES:AIR;
 }
 // Scattered stone formations with deterministic placement in 32-block cells.
 public Boulder boulder(int gx,int gz){
  int x=gx*32+6+(int)(hash(seed,gx,601,gz)*20),z=gz*32+6+(int)(hash(seed,gx,602,gz)*20);
  double r=Math.hypot(x,z);if(r<SAFE_RADIUS+24||r>RADIUS-32)return null;
  Region type=region(x,z);
  if(hash(seed,gx,603,gz)>(type==Region.CRATERS?.75:type==Region.HIGHLANDS?.5:.20))return null;
  Column c=column(x,z);
  int radius=2+(int)(hash(seed,gx,604,gz)*3);
  return new Boulder(x,c.top()+1,z,radius,hash(seed,gx,605,gz)<.18);
 }
 public int boulderBlock(Boulder b,int x,int y,int z){
  int dx=x-b.x(),dz=z-b.z(),dy=y-b.y();
  if(dy< -2||dy>b.radius()||dx*dx+dz*dz+2*dy*dy>b.radius()*b.radius()+1)return AIR;
  return b.crystal()&&dy>=0&&hash(seed,x,y,z)<.30?ORE:BASALT;
 }
}
