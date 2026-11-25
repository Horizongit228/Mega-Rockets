package dev.megarockets;
public enum Destination {
    MOON(6000,MoonTerrain.PAD_Y,0), MARS(24000,MarsTerrain.PAD_Y,4500);
    public final double distance,lane;
    public final int padY;
    Destination(double distance,int padY,double lane){this.distance=distance;this.padY=padY;this.lane=lane;}
    public String key(){return name().toLowerCase(java.util.Locale.ROOT);}
    public static Destination read(String value){try{return valueOf(value);}catch(IllegalArgumentException e){return MOON;}}
}
