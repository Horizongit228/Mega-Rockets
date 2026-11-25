package dev.megarockets;

/** Deterministic finite, spherical voxel terrain. Independent of Minecraft for testing. */
public final class MoonTerrain {
    public static final int RADIUS=256, MIN_Y=-320, HEIGHT=640, PAD_Y=256;
    public static final int AIR=0, ROCK=1, REGOLITH=2, BASALT=3, ORE=4, PAD=5;
    public record Column(int bottom,int top,boolean pad,double tunnelX,double tunnelZ) {}
    private MoonTerrain() {}
    private static double clamp(double n,double lo,double hi){return Math.max(lo,Math.min(hi,n));}
    private static double smooth(double n){n=clamp(n,0,1);return n*n*(3-2*n);}
    public static double noise(int x,int y,int z) {
        long n=x*341873128712L+y*132897987541L+z*42317861L;
        n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;
        n=(n^(n>>>27))*0x94d049bb133111ebL;n^=n>>>31;
        return (n>>>11)*0x1.0p-53;
    }
    public static Column column(int x,int z) {
        double r=Math.hypot(x,z);
        if(r>=RADIUS) return new Column(1,0,false,0,0);
        double h=Math.sqrt(RADIUS*RADIUS-r*r), taper=smooth((RADIUS-r)/45);
        double ridge=Math.pow(Math.abs(Math.sin(x*.034+Math.cos(z*.021)*1.6)),2)*25;
        double hills=12*Math.sin(x*.019+z*.013)+8*Math.cos(z*.043-x*.011);
        double crater=0;
        // Fixed crater centers make large recognizable impact basins.
        for(int[] c:new int[][]{{88,65,32},{-91,93,42},{95,-112,38},{-144,-52,28},{18,-153,24}}) {
            double d=Math.hypot(x-c[0],z-c[1])/c[2];
            if(d<1.4) crater+=-19*Math.max(0,1-d*d)+9*Math.exp(-Math.pow((d-1)/.14,2));
        }
        double top=h+(ridge+hills+crater)*taper;
        top=PAD_Y-1+(top-(PAD_Y-1))*smooth((r-32)/34);
        int bottom=(int)Math.ceil(-h+(6*Math.sin(x*.041)*Math.cos(z*.037))*taper);
        return new Column(bottom,Math.min(298,(int)Math.floor(top)),r<=32,
            Math.sin(x*.078+z*.025),Math.cos(z*.071-x*.019));
    }
    public static int material(Column c,int x,int y,int z) {
        if(y<c.bottom()||y>c.top())return AIR;
        int depth=Math.min(c.top()-y,y-c.bottom());
        // Keep the landing plateau and its foundation solid.
        if(c.pad() && y>=PAD_Y-8) return y==PAD_Y-1?PAD:ROCK;
        if(depth>5) {
            double a=c.tunnelX()+Math.sin(y*.094+z*.026), b=c.tunnelZ()+Math.cos(y*.071-x*.032);
            double chamber=Math.sin(x*.035+y*.048)*Math.cos(z*.043-y*.029)*Math.sin(y*.039);
            if(a*a+b*b<.10 || chamber>.85) return AIR;
        }
        if(depth<=3) return REGOLITH;
        double n=noise(x,y,z);
        if(depth>10&&n<.006)return ORE;
        return n<.17?BASALT:ROCK;
    }
}
