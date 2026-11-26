package dev.megarockets;
public enum Destination {
    MOON(12000,MoonTerrain.PAD_Y,0,.165,384400,4096),
    MARS(80000,MarsTerrain.PAD_Y,4500,.38,78000000,8192),
    VENUS(60000,128,-4500,.904,41000000,12000),
    EUROPA(220000,128,9000,.134,630000000,4096),
    TITAN(320000,128,-9000,.138,1270000000,6400);
    public final double distance,lane,gravity;
    public final long referenceKm;
    public final int padY,radius;
    Destination(double distance,int padY,double lane,double gravity,long referenceKm,int radius){this.distance=distance;this.padY=padY;this.lane=lane;this.gravity=gravity;this.referenceKm=referenceKm;this.radius=radius;}
    public String key(){return name().toLowerCase(java.util.Locale.ROOT);}
    public static Destination read(String value){try{return valueOf(value);}catch(IllegalArgumentException e){return MOON;}}
}
