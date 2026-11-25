package dev.megarockets;
/** Pure rules: one unit of stored oxygen is one second of breathing at 20 TPS. */
public final class OxygenRules {
    public static final int CAPACITY=600,REFILL_PER_SECOND=20,ALTITUDE=1000;
    public static final int DAMAGE_INTERVAL=10;
    public static final float DAMAGE=4f;
    private OxygenRules(){}
    public static int clamp(int value){return Math.max(0,Math.min(CAPACITY,value));}
    public static int next(int oxygen,boolean vacuum,boolean sealed,boolean second){
        oxygen=clamp(oxygen);
        if(!second)return oxygen;
        if(!vacuum)return clamp(oxygen+REFILL_PER_SECOND);
        return sealed?Math.max(0,oxygen-1):oxygen;
    }
    public static boolean suffocating(boolean vacuum,boolean sealed,int oxygen){
        return vacuum&&(!sealed||oxygen<=0);
    }
}
