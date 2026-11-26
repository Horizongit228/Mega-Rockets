package dev.megarockets;
/** Distinct seeded terrain profiles. No artificial landing platforms. */
public final class OuterPlanetTerrain {
 public static final int MIN_Y=-64,HEIGHT=768,AIR=0,SURFACE=1,ROCK=2,ORE=3,BEDROCK=4;
 private final boolean flat;private final Destination planet;private final long seed;private final PlanetCaves caves;
 public OuterPlanetTerrain(Destination planet,long seed){this(planet,seed,false);}
 public OuterPlanetTerrain(Destination planet,long seed,boolean flat){this.flat=flat;this.planet=planet;this.seed=seed;caves=new PlanetCaves(seed^planet.ordinal()*341873128712L);}
 private double hash(int x,int y,int z){long n=seed^(x*341873128712L+y*132897987541L+z*42317861L);n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;n^=n>>>31;return(n>>>11)*0x1.0p-53;}
 private static double smooth(double n){n=Math.max(0,Math.min(1,n));return n*n*(3-2*n);}
 private static double mix(double a,double b,double t){return a+(b-a)*t;}
 private double n(double x,double z,int salt){int a=(int)Math.floor(x),b=(int)Math.floor(z);double u=smooth(x-a),v=smooth(z-b);return 2*mix(mix(hash(a,salt,b),hash(a+1,salt,b),u),mix(hash(a,salt,b+1),hash(a+1,salt,b+1),u),v)-1;}
 public int top(int x,int z){
  if(flat){x=PlanetWrap.coordinate(x,PlanetWrap.period(planet.radius));z=PlanetWrap.coordinate(z,PlanetWrap.period(planet.radius));}
  double r=Math.hypot(x,z);if(!flat&&r>=planet.radius)return MIN_Y-1;
  double broad=n(x/240.,z/240.,1),fine=n(x/60.,z/60.,2),detail=n(x/16.,z/16.,3),h;
  switch(planet){
   case MERCURY -> {
    h=106+18*broad+6*fine+2*detail;
    int gx=Math.floorDiv(x,192),gz=Math.floorDiv(z,192);
    for(int a=gx-1;a<=gx+1;a++)for(int b=gz-1;b<=gz+1;b++){
     if(hash(a,81,b)>.85)continue;
     double cx=a*192+24+144*hash(a,82,b),cz=b*192+24+144*hash(a,83,b);
     double radius=22+64*hash(a,84,b),q=Math.hypot(x-cx,z-cz)/radius;
     if(q<1.35)h+=radius*(-.32*Math.pow(Math.max(0,1-q*q),2)+.10*Math.exp(-Math.pow((q-1)/.2,2)))*smooth((1.35-q)/.35);
    }
   }
   case VENUS -> {double dome=Math.pow(Math.max(0,n(x/510.,z/510.,4)),3);h=105+24*broad+8*fine+2*detail+180*dome;}
   case EUROPA -> {double fault=-18*Math.exp(-Math.pow(n(x/180.,z/180.,5)/.10,2));h=80+10*broad+3*fine+detail+fault;}
   case TITAN -> {double dune=7*Math.sin((x+18*n(x/120.,z/120.,6))/19.)*smooth((broad+.6)/1.6);h=95+19*broad+5*fine+dune+detail;}
   default -> h=90;
  }
  h=flat?mix(95,h,PlanetWrap.edgeWeight(x,z,PlanetWrap.period(planet.radius))):mix(8,h,smooth((planet.radius-r)/320));return (int)Math.max(8,Math.min(580,Math.floor(h)));
 }
 public PlanetCaves.Column caveColumn(int x,int z){if(flat){x=PlanetWrap.coordinate(x,PlanetWrap.period(planet.radius));z=PlanetWrap.coordinate(z,PlanetWrap.period(planet.radius));}return caves.column(x,z);}
 public int material(int x,int y,int z,int top,PlanetCaves.Column c){
  if(flat){x=PlanetWrap.coordinate(x,PlanetWrap.period(planet.radius));z=PlanetWrap.coordinate(z,PlanetWrap.period(planet.radius));}
  if(y<MIN_Y||y>top)return AIR;if(y==MIN_Y)return BEDROCK;
  if(flat&&PlanetWrap.seam(x,z,PlanetWrap.period(planet.radius)))return top-y<5?SURFACE:ROCK;
  int depth=top-y;if(y>MIN_Y+3&&caves.air(c,depth))return AIR;
  if(depth<5)return SURFACE;return depth>8&&hash(x,y,z)<.012?ORE:ROCK;
 }
}
