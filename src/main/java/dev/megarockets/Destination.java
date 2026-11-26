package dev.megarockets;
public enum Destination {
    MOON(SolarScale.MOON_ROUTE,MoonTerrain.PAD_Y,0,0.165,384400L,1737.4,0L),
    MARS(SolarScale.travel(78345562L),MarsTerrain.PAD_Y,4500,0.378,78345562L,3389.5,227943824L),
    VENUS(SolarScale.travel(41388787L),128,-4500,0.905,41388787L,6051.8,108209475L),
    EUROPA(SolarScale.travel(629413659L),128,9000,0.134,629413659L,1560.8,778340821L),
    TITAN(SolarScale.travel(1278290060L),128,-9000,0.138,1278290060L,2574.76,1426666422L),
    MERCURY(SolarScale.travel(91689035L),128,14000,0.378,91689035L,2439.4,57909227L),
    JUPITER(SolarScale.travel(628742559L),180,26000,2.53,628742559L,69911,778340821L),
    SATURN(SolarScale.travel(1277068160L),180,-30000,1.066,1277068160L,58232,1426666422L),
    URANUS(SolarScale.travel(2721059924L),180,52000,0.905,2721059924L,25362,2870658186L),
    NEPTUNE(SolarScale.travel(4348798179L),180,-65000,1.138,4348798179L,24622,4498396441L);
    public static final int EARTH_RADIUS=SolarScale.radius(SolarScale.EARTH_RADIUS_KM);
    public final double distance,lane,gravity,meanRadiusKm;
    public final long referenceKm,solarOrbitKm;
    public final int padY,radius;
    Destination(double distance,int padY,double lane,double gravity,long referenceKm,double meanRadiusKm,long solarOrbitKm){
        this.distance=distance;this.padY=padY;this.lane=lane;this.gravity=gravity;this.referenceKm=referenceKm;
        this.meanRadiusKm=meanRadiusKm;this.solarOrbitKm=solarOrbitKm;this.radius=SolarScale.radius(meanRadiusKm);
    }
    public boolean orbitOnly(){return this==JUPITER||this==SATURN||this==URANUS||this==NEPTUNE;}
    public String key(){return name().toLowerCase(java.util.Locale.ROOT);}
    public static Destination read(String value){try{return valueOf(value);}catch(IllegalArgumentException e){return MOON;}}
}
