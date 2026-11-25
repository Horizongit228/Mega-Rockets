package dev.megarockets;

/** Deterministic cinematic guidance, independent of Minecraft. Positions are real block coordinates. */
public final class LunarFlight {
    public static final double DISTANCE=6000, CRUISE_Y=180, LAND_Y=101, MAX_SPEED=5.0;
    public enum Phase { ORBIT, DEPARTURE, COAST, TURN, BRAKE, HOLD, ALIGN, DESCENT, FINAL, LANDED }
    public record State(Phase phase,int ticks,double x,double y,double speed,float tilt) {}
    private LunarFlight() {}
    public static State start() { return new State(Phase.ORBIT,0,0,CRUISE_Y,0,0); }
    private static double approach(double current,double goal,double accel,double brake) {
        return current+Math.max(-brake,Math.min(accel,goal-current));
    }
    public static double arcY(double x) {
        double u=Math.max(0,Math.min(1,x/DISTANCE));
        double a=Math.sin(Math.PI*u);
        return CRUISE_Y+36*a*a;
    }
    public static State pause(State s) {
        Phase phase=s.phase();
        if(phase==Phase.DEPARTURE||phase==Phase.COAST||phase==Phase.TURN||phase==Phase.BRAKE)
            phase=s.x()<DISTANCE-700?Phase.DEPARTURE:Phase.BRAKE;
        return new State(phase,s.ticks(),s.x(),s.y(),0,s.tilt());
    }
    public static boolean powered(State s) {
        return switch(s.phase()) {
            case DEPARTURE,BRAKE,DESCENT,FINAL -> s.speed()>.001;
            default -> false;
        };
    }
    public static State step(State s,boolean moonReady) {
        Phase phase=s.phase(); int ticks=s.ticks()+1;
        double x=s.x(),y=s.y(),speed=s.speed(),remaining=DISTANCE-x;
        float angle=0;
        switch(phase) {
            case ORBIT -> { speed=0; if(ticks>=60){phase=Phase.DEPARTURE;ticks=0;} }
            case DEPARTURE,COAST,TURN,BRAKE -> {
                if(remaining<=360) phase=Phase.BRAKE;
                else if(remaining<=700 && phase!=Phase.BRAKE) phase=Phase.TURN;
                else if(phase==Phase.DEPARTURE && speed>=MAX_SPEED-.001) phase=Phase.COAST;
                double goal=phase==Phase.BRAKE?Math.min(MAX_SPEED,Math.sqrt(2*.045*remaining)):MAX_SPEED;
                // Coast retains its attained speed; engines are off until braking.
                speed=phase==Phase.COAST||phase==Phase.TURN?speed:approach(speed,goal,.035,.045);
                // A resumed cruise must not stall if the passenger left midflight.
                if(speed<.001) { phase=remaining<=700?Phase.BRAKE:Phase.DEPARTURE; speed=Math.min(.035,remaining); }
                speed=Math.min(remaining,speed);
                x+=speed; y=arcY(x);
                angle=phase==Phase.TURN||phase==Phase.BRAKE?85:-85;
                if(x>=DISTANCE-1e-8) {x=DISTANCE;y=CRUISE_Y;speed=0;phase=moonReady?Phase.ALIGN:Phase.HOLD;ticks=0;angle=0;}
            }
            case HOLD -> { speed=0; if(moonReady){phase=Phase.ALIGN;ticks=0;} }
            case ALIGN -> { speed=0; if(Math.abs(s.tilt())<=.01&&ticks>=20){phase=Phase.DESCENT;ticks=0;} }
            case DESCENT,FINAL -> {
                if(!moonReady) { speed=0; break; }
                remaining=Math.max(0,y-LAND_Y);
                phase=remaining<=12?Phase.FINAL:Phase.DESCENT;
                double goal=Math.min(1.25,Math.min(Math.sqrt(2*.035*remaining),.14+Math.max(0,remaining-5)*.045));
                speed=Math.min(remaining,Math.max(.025,approach(speed,goal,.025,.035)));
                y-=speed;
                if(y<=LAND_Y+1e-8){y=LAND_Y;speed=0;phase=Phase.LANDED;ticks=0;}
            }
            case LANDED -> speed=0;
        }
        float tilt=s.tilt()+Math.max(-2f,Math.min(2f,angle-s.tilt()));
        return new State(phase,ticks,x,y,speed,tilt);
    }
}
