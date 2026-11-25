package dev.megarockets;

/** Pure flight profile; blocks per game tick, server authoritative. */
public final class FlightProfile {
    private FlightProfile() {}
    public static double nextSpeed(double y, double previous, double target) {
        return nextSpeed(y,previous,target,15);
    }
    public static double nextSpeed(double y,double previous,double target,double maxSpeed){
        double accel=maxSpeed<=15?.12:maxSpeed<=30?.38:.65;
        double braking=maxSpeed<=15?.15:maxSpeed<=30?.45:.8;
        double altitudeFactor=Math.max(0,Math.min(1,(y-260)/420.0));
        altitudeFactor=altitudeFactor*altitudeFactor*(3-2*altitudeFactor);
        double cruise=1.8+(maxSpeed-1.8)*altitudeFactor;
        double remaining=Math.max(0,target-y);
        if (remaining==0) return 0;
        double goal=Math.min(cruise,Math.sqrt(2*braking*remaining));
        double change=Math.max(-braking,Math.min(accel,goal-previous));
        return Math.min(remaining,Math.max(.035,previous+change));
    }
}
