package dev.megarockets;
public final class MarsTerrain {
 public static final int PAD_Y=128,MIN_Y=-64,HEIGHT=384;
 public static final int AIR=0,ROCK=1,REGOLITH=2,ORE=3,PAD=4,BEDROCK=5;
 private final long seed; private final double off;
 public MarsTerrain(long seed){this.seed=seed;off=hash(1,1,1)*8000;}
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
 public int top(int x,int z){
  double r=Math.hypot(x,z);if(r<=24)return PAD_Y-1;
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
  double h=mix(PAD_Y-1-Math.min(35,(r-24)*.22),88+hills+dune+canyon+crater,smooth((r-55)/240));
  if(r<192)h=Math.min(PAD_Y-2,h);
  return (int)Math.max(8,Math.min(190,Math.floor(h)));
 }
 public int material(int x,int y,int z,int top){
  if(y<MIN_Y||y>top)return AIR;if(y==MIN_Y)return BEDROCK;
  int depth=top-y;
  if(Math.hypot(x,z)<=24&&depth<12)return depth==0?PAD:ROCK;
  if(depth>7&&y>MIN_Y+3){
   double a=Math.sin((x+off)*.075+Math.sin(y*.051)),b=Math.cos((z-off)*.071+Math.cos(y*.067));
   double c=Math.sin((x+off)*.03+y*.06)*Math.cos((z-off)*.035-y*.04);
   if(a*a+b*b<.09||c>.88)return AIR;
  }
  if(depth<5)return REGOLITH;return hash(x,y,z)<.012?ORE:ROCK;
 }
}
