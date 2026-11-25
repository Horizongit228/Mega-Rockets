package dev.megarockets;

/** Deterministic lunar relief, with a protected highland landing site. */
public final class MoonTerrain {
 public static final int RADIUS=4096,MIN_Y=-1536,HEIGHT=3072,NORTH_OFFSET=3072,PAD_Y=1280,CRUST=192;
 public static final int PAD_RADIUS=24,SAFE_RADIUS=192;
 public static final int PAD_GLOBAL_Y=PAD_Y+NORTH_OFFSET-1;
 public static final int AIR=0,ROCK=1,REGOLITH=2,BASALT=3,ORE=4,PAD=5;
 public record Column(int bottom,int top,int innerBottom,int innerTop,boolean pad,double tunnelX,double tunnelZ){}
 private static final int[][] CRATERS={{700,520,260},{-728,744,336},{760,-896,304},{-1152,-416,224},{144,-1224,192},{2100,1000,440},{-2300,1400,360},{1600,-2400,300},{-800,-3000,420}};
 private MoonTerrain(){}
 private static double clamp(double n,double a,double b){return Math.max(a,Math.min(b,n));}
 private static double smooth(double n){n=clamp(n,0,1);return n*n*(3-2*n);}
 private static double mix(double a,double b,double t){return a+(b-a)*t;}
 public static double noise(int x,int y,int z){
  long n=x*341873128712L+y*132897987541L+z*42317861L;
  n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;n^=n>>>31;return(n>>>11)*0x1.0p-53;
 }
 private static double field(double x,double z,int seed){
  int ix=(int)Math.floor(x),iz=(int)Math.floor(z);
  double tx=smooth(x-ix),tz=smooth(z-iz);
  return 2*mix(mix(noise(ix,seed,iz),noise(ix+1,seed,iz),tx),
      mix(noise(ix,seed,iz+1),noise(ix+1,seed,iz+1),tx),tz)-1;
 }
 private static double roughness(int x,int z){
  // Multiple spatial scales: broad ridges, broken escarpments, boulders and fine debris.
  double ridge=1-Math.abs(field(x/125.0,z/125.0,91));
  double broken=field(x/44.0,z/44.0,97);
  double terrace=Math.floor((broken+1)*5)*.2-1;
  return 90*(ridge*ridge-.45)+33*terrace+15*field(x/16.0,z/16.0,107)
      +6*field(x/5.0,z/5.0,113)+2.4*field(x/2.0,z/2.0,127);
 }
 public static Column column(int x,int z){
  double r=Math.hypot(x,z);
  if(r>=RADIUS)return new Column(1,0,1,0,false,0,0);
  double h=Math.sqrt((double)RADIUS*RADIUS-r*r),taper=smooth((RADIUS-r)/160);
  double ridge=Math.pow(Math.abs(Math.sin(x*.0034+Math.cos(z*.0021)*1.6)),3)*140;
  double hills=65*Math.sin(x*.0019+z*.0013)+38*Math.cos(z*.0043-x*.0011);
  double crater=0;
  for(int[] c:CRATERS){
   double d=Math.hypot(x-c[0],z-c[1])/c[2];
   if(d<1.4)crater+=-95*Math.max(0,1-d*d)+38*Math.exp(-Math.pow((d-1)/.11,2));
  }
  double top=h+(ridge+hills+crater+roughness(x,z))*taper;
  // A small level landing area on a raised, exposed highland, never in a basin.
  // Surrounding ground is constrained below the pad for a full 192-block radius.
  if(r<=PAD_RADIUS)top=PAD_GLOBAL_Y;
  else if(r<SAFE_RADIUS){
   double fall=5+Math.min(120,(r-PAD_RADIUS)*.72);
   double apron=PAD_GLOBAL_Y-fall+clamp(roughness(x,z)*.26,-12,12)*smooth((r-PAD_RADIUS)/20);
   // Cap nearby terrain, even when a procedural ridge would otherwise surround the site.
   top=Math.min(PAD_GLOBAL_Y-2,mix(apron,top,smooth((r-85)/107)));
  }else if(r<320){
   top=mix(Math.min(top,PAD_GLOBAL_Y-2),top,smooth((r-SAFE_RADIUS)/128));
  }
  int bottom=(int)Math.ceil(-h+35*Math.sin(x*.0031)*Math.cos(z*.0027)*taper),t=(int)Math.floor(top);
  double inner=RADIUS-CRUST;
  int half=r<inner?(int)Math.floor(Math.sqrt(inner*inner-r*r)):-1;
  int ib=half<0?1:Math.max(bottom+8,-half),it=half<0?0:Math.min(t-24,half);
  return new Column(bottom,t,ib,it,r<=PAD_RADIUS,Math.sin(x*.078+z*.025),Math.cos(z*.071-x*.019));
 }
 public static int material(Column c,int x,int y,int z){
  if(y<c.bottom()||y>c.top()||(y>=c.innerBottom()&&y<=c.innerTop()))return AIR;
  int depth=Math.min(c.top()-y,y-c.bottom());
  // Solid foundation under the landing tiles, including the dismount point.
  if(c.pad()&&y>=PAD_GLOBAL_Y-15)return y==PAD_GLOBAL_Y?PAD:ROCK;
  if(depth>5){
   double a=c.tunnelX()+Math.sin(y*.094+z*.026),b=c.tunnelZ()+Math.cos(y*.071-x*.019);
   double chamber=Math.sin(x*.035+y*.048)*Math.cos(z*.043-y*.029)*Math.sin(y*.039);
   if(a*a+b*b<.10||chamber>.85)return AIR;
  }
  if(depth<=4)return REGOLITH;
  double n=noise(x,y,z);if(depth>10&&n<.008)return ORE;return n<.19?BASALT:ROCK;
 }
}
