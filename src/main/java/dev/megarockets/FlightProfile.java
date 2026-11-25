package dev.megarockets;

/** Pure flight profile; blocks per game tick, server authoritative. */
public final class FlightProfile {
    private FlightProfile() {}
    public static double nextSpeed(double y, double previous, double target) {
        double altitudeFactor=Math.max(0,Math.min(1,(y-260)/420.0));
        altitudeFactor=altitudeFactor*altitudeFactor*(3-2*altitudeFactor);
        double cruise=1.8+13.2*altitudeFactor;
        double remaining=Math.max(0,target-y);
        if (remaining==0) return 0;
        double goal=Math.min(cruise,Math.sqrt(2*.15*remaining));
        double change=Math.max(-.15,Math.min(.12,goal-previous));
        return Math.min(remaining,Math.max(.035,previous+change));
    }
}
