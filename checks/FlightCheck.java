import dev.megarockets.FlightProfile;
public final class FlightCheck {
    public static void main(String[] args) {
        for(double start:new double[]{64,180,9950,9999.99}) {
            double y=start,s=0; int ticks=0;
            while(y<10000 && ticks++<10000) {
                double next=FlightProfile.nextSpeed(y,s,10000);
                if(!Double.isFinite(next)||next<=0||next>7.50001||next-s>.045001) throw new AssertionError("Invalid speed "+next);
                if(next-s<-.055001 && 10000-y>next+1e-6) throw new AssertionError("Abrupt braking");
                y=Math.min(10000,y+next);s=next;
            }
            if(y!=10000) throw new AssertionError("Target not reached");
            System.out.println("PASS flight from "+start+" in "+ticks+" ticks");
        }
    }
}
