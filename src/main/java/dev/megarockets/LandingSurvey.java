package dev.megarockets;
import java.util.function.IntBinaryOperator;
/** Pure, bounded search for a natural landing site. Heights are first air above solid ground. */
public final class LandingSurvey {
    public static final int INVALID=Integer.MIN_VALUE,STEP=16,MAX_RING=32,COUNT=(MAX_RING*2+1)*(MAX_RING*2+1);
    public record Point(int x,int z){}
    public record Site(int x,int y,int z,int exitX,int exitY,int exitZ){}
    private LandingSurvey(){}
    public static Point candidate(int index){
        if(index<0||index>=COUNT)throw new IllegalArgumentException("Candidate index");
        if(index==0)return new Point(0,0);
        int r=1;
        while(index>=(2*r+1)*(2*r+1))r++;
        int i=index-(2*r-1)*(2*r-1),side=2*r;
        int x,z;
        if(i<side){x=-r+i;z=-r;}
        else if(i<2*side){x=r;z=-r+i-side;}
        else if(i<3*side){x=r-(i-2*side);z=r;}
        else{x=-r;z=r-(i-3*side);}
        return new Point(x*STEP,z*STEP);
    }
    public static Site inspect(int x,int z,IntBinaryOperator ground){
        int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;
        // Cheap rejection of steep slopes before surveying every column.
        for(int dx:new int[]{-7,0,7})for(int dz:new int[]{-7,0,7}){
            int y=ground.applyAsInt(x+dx,z+dz);
            if(y==INVALID)return null;min=Math.min(min,y);max=Math.max(max,y);
        }
        if(max-min>1)return null;
        for(int dx=-7;dx<=7;dx++)for(int dz=-7;dz<=7;dz++){
            int y=ground.applyAsInt(x+dx,z+dz);
            if(y==INVALID)return null;min=Math.min(min,y);max=Math.max(max,y);
            if(max-min>1)return null;
        }
        // A dismount point clear of the hull, with a stable 3x3 area.
        for(int[] dir:new int[][]{{1,0},{0,1},{-1,0},{0,-1}}){
            int ex=x+10*dir[0],ez=z+10*dir[1],ey=ground.applyAsInt(ex,ez);
            if(ey==INVALID||Math.abs(ey-max)>2)continue;
            boolean safe=true;
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)
                if(ground.applyAsInt(ex+dx,ez+dz)!=ey)safe=false;
            if(safe)return new Site(x,max,z,ex,ey,ez);
        }
        return null;
    }
}
