package dev.megarockets;
public final class MarsTerrain {
 public static final int PAD_Y=128,MIN_Y=-64,HEIGHT=768,RADIUS=7991,DIAMETER=2*RADIUS;
 public static final int AIR=0,ROCK=1,REGOLITH=2,ORE=3,PAD=4,BEDROCK=5;
 private final boolean flat;
 private final long seed; private final double off;
 private final PlanetCaves caveNetwork;
 private final Mountain[] peaks=new Mountain[10];
 public MarsTerrain(long seed){this(seed,false);}
 public MarsTerrain(long seed,boolean flat){this.flat=flat;this.seed=seed;this.caveNetwork=new PlanetCaves(seed^0x4d415253L);off=hash(1,1,1)*8000;createMountains();}
 private double hash(int x,int y,int z){
  long n=seed^(x*341873128712L+y*132897987541L+z*42317861L);
  n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;n^=n>>>31;
  return(n>>>11)*0x1.0p-53;
 }
 private static double smooth(double n){n=Math.max(0,Math.min(1,n));return n*n*(3-2*n);}
 private static double mix(double a,double b,double t){return a+(b-a)*t;}
 private double noise(double x,double z,int salt){
  int a=(int)Math.floor(x),b=(int)Math.floor(z);double u=smooth(x-a),v=smooth(z-b);
  return 2*mix(mix(hash(a,salt,b),hash(a+1,salt,b),u),mix(hash(a,salt,b+1),hash(a+1,salt,b+1),u),v)-1;
 }
 public record Mountain(int x,int z,int radius,int height){}
 private void createMountains(){
  double rotation=hash(0,200,0)*2*Math.PI;
  for(int i=0;i<10;i++){
   int ring=i/5,slot=i%5;
   double angle=rotation+slot*2*Math.PI/5+ring*.56+(hash(i,201,0)-.5)*.20;
   double distance=(ring==0?1550:4800)+hash(i,202,0)*(ring==0?950:1900);
   int x=(int)Math.round(Math.cos(angle)*distance),z=(int)Math.round(Math.sin(angle)*distance);
   int radius=350+(int)(hash(i,203,0)*251);
   int height=i==0?456:300+(int)(hash(i,204,0)*157);
   peaks[i]=new Mountain(x,z,radius,height);
  }
 }
 public java.util.List<Mountain> mountains(){return java.util.List.of(peaks);}
 private double mountains(int x,int z){
  double result=0;
  for(Mountain m:peaks){
   double dx=x-m.x(),dz=z-m.z();
   if(Math.abs(dx)>=m.radius()||Math.abs(dz)>=m.radius())continue;
   double d=Math.hypot(dx,dz)/m.radius();
   if(d<1){
    double cone=Math.pow(1-d,1.6);
    double ridges=(12*noise(x/48.,z/48.,211)+7*noise(x/23.,z/23.,212))*Math.sin(Math.PI*d);
    result=Math.max(result,Math.min(m.height(),Math.max(0,m.height()*cone+ridges)));
   }
  }
  return result;
 }
 public PlanetCaves.Entrance caveEntrance(int gx,int gz){return caveNetwork.entrance(gx,gz);}
 public PlanetCaves.Column caveColumn(int x,int z){if(flat){x=PlanetWrap.coordinate(x,PlanetWrap.period(RADIUS));z=PlanetWrap.coordinate(z,PlanetWrap.period(RADIUS));}return caveNetwork.column(x,z);}
 public int top(int x,int z){
  if(flat){x=PlanetWrap.coordinate(x,PlanetWrap.period(RADIUS));z=PlanetWrap.coordinate(z,PlanetWrap.period(RADIUS));}
  double r=Math.hypot(x,z);if(!flat&&r>=RADIUS)return MIN_Y-1;
  double hills=22*noise(x/220.,z/220.,11)+12*noise(x/75.,z/75.,12)+3*noise(x/18.,z/18.,13);
  double dune=4*Math.sin((x+noise(x/100.,z/100.,15)*22)/14.)*smooth((noise(x/300.,z/300.,16)+.5)/1.5);
  double canyon=-45*Math.exp(-Math.pow(noise(x/310.,z/310.,31)/.13,2))*smooth((noise(x/600.,z/600.,32)+.15)/.7);
  double crater=0;int gx=Math.floorDiv(x,256),gz=Math.floorDiv(z,256);
  for(int a=gx-1;a<=gx+1;a++)for(int b=gz-1;b<=gz+1;b++){
   if(hash(a,91,b)>.55)continue;
   double cx=a*256+40+176*hash(a,92,b),cz=b*256+40+176*hash(a,93,b);
   double rad=25+75*hash(a,94,b),d=Math.hypot(x-cx,z-cz)/rad;
   if(d<1.3)crater+=rad*(-.34*Math.pow(Math.max(0,1-d*d),2)+.11*Math.exp(-Math.pow((d-1)/.21,2)))*smooth((1.3-d)/.3);
  }
  double h=88+hills+dune+canyon+crater+mountains(x,z);
  // The finite disk tapers into a low rim over its outer 320 blocks.
  h=flat?mix(88,h,PlanetWrap.edgeWeight(x,z,PlanetWrap.period(RADIUS))):mix(8,h,smooth((RADIUS-r)/320));
  return (int)Math.max(8,Math.min(670,Math.floor(h)));
 }
 public int material(int x,int y,int z,int top){
  return material(x,y,z,top,caveColumn(x,z));
 }
 public int material(int x,int y,int z,int top,PlanetCaves.Column column){
  if(flat){x=PlanetWrap.coordinate(x,PlanetWrap.period(RADIUS));z=PlanetWrap.coordinate(z,PlanetWrap.period(RADIUS));}
  if(y<MIN_Y||y>top)return AIR;if(y==MIN_Y)return BEDROCK;
  if(flat&&PlanetWrap.seam(x,z,PlanetWrap.period(RADIUS)))return top-y<5?REGOLITH:ROCK;
  int depth=top-y;
  if(y>MIN_Y+3&&caveNetwork.air(column,depth))return AIR;
  if(depth>7&&y>MIN_Y+3){
   double a=Math.sin((x+off)*.075+Math.sin(y*.051)),b=Math.cos((z-off)*.071+Math.cos(y*.067));
   double c=Math.sin((x+off)*.03+y*.06)*Math.cos((z-off)*.035-y*.04);
   if(a*a+b*b<.09||c>.88)return AIR;
  }
  if(depth<5)return REGOLITH;return hash(x,y,z)<.012?ORE:ROCK;
 }
}
