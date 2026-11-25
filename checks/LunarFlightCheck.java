import dev.megarockets.LunarFlight;
import java.util.EnumSet;

public class LunarFlightCheck {
    public static void main(String[] args) {
        for(boolean delayed:new boolean[]{false,true}) {
            var s=LunarFlight.start();
            var seen=EnumSet.of(s.phase());
            boolean coastOff=false, resumed=false; int count=0,hold=0;
            while(s.phase()!=LunarFlight.Phase.LANDED && count++<5000) {
                boolean ready=!delayed||hold>=40;
                if(s.phase()==LunarFlight.Phase.HOLD) hold++;
                if(!resumed && s.x()>2000 && s.x()<3000) { s=LunarFlight.pause(s); resumed=true; }
                var n=LunarFlight.step(s,ready);
                seen.add(n.phase());
                if(n.phase()==LunarFlight.Phase.COAST && !LunarFlight.powered(n)) coastOff=true;
                if(n.x()<s.x()-1e-6 || n.x()>6000.000001 || n.y()<101-1e-6 || n.y()>216.000001
                  || Math.abs(n.tilt()-s.tilt())>2.00001 || n.speed()>5.00001 || n.speed()<0
                  || Math.hypot(n.x()-s.x(),n.y()-s.y())>5.01) throw new AssertionError(n);
                if(n.phase()==LunarFlight.Phase.LANDED && s.y()-n.y()>.15) throw new AssertionError("Hard landing");
                s=n;
            }
            if(s.phase()!=LunarFlight.Phase.LANDED||s.x()!=6000||s.y()!=101||s.speed()!=0||s.tilt()!=0||!coastOff||!resumed
                ||!seen.contains(LunarFlight.Phase.TURN)||!seen.contains(LunarFlight.Phase.BRAKE)
                ||!seen.contains(LunarFlight.Phase.FINAL)||delayed&&!seen.contains(LunarFlight.Phase.HOLD))
                throw new AssertionError("Mission incomplete "+s+" "+seen);
            System.out.println("PASS lunar mission ("+count+" ticks), delayed="+delayed+", "+seen);
        }
    }
}
