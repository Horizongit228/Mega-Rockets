package dev.megarockets.client;

import java.util.ArrayList;
import java.util.List;

/** Precomputed fixed model; all coordinates in Minecraft blocks. */
public final class TitanMesh {
    private static final int SIDES=24;
    private static final int WHITE=0xffe4e8ee, GRAY=0xff9aa6b4, DARK=0xff303b4b, BLUE=0xff286583, GOLD=0xffbf8136;
    public record Point(float x,float y,float z) {}
    public record Quad(Point[] points,float nx,float ny,float nz,int argb) {}
    public static final List<Quad> QUADS=build();
    private static List<Quad> build() {
        List<Quad> out=new ArrayList<>();
        ring(out,0,0,3,4.3f,6,4.9f,DARK);
        ring(out,0,0,6,4.9f,9,5,GRAY);
        ring(out,0,0,9,5,13,5,DARK);
        ring(out,0,0,13,5,27,5,WHITE);
        ring(out,0,0,27,5,29,5,DARK);
        ring(out,0,0,29,5,42,5,WHITE);
        ring(out,0,0,42,5,44,5,DARK);
        ring(out,0,0,44,5,48,3.8f,GRAY);
        ring(out,0,0,48,3.8f,52,3.8f,WHITE);
        ring(out,0,0,52,3.8f,54,3.8f,BLUE);
        ring(out,0,0,54,3.8f,56,3.8f,WHITE);
        ring(out,0,0,56,3.8f,60,2.3f,WHITE);
        ring(out,0,0,60,2.3f,63,0.8f,WHITE);
        ring(out,0,0,63,.8f,64,0,GRAY);
        cap(out,0,0,3,4.3f,false,DARK);
        for(int i=0;i<7;i++) {
            float a=(float)(i*Math.PI/3),x=i==6?0:2.8f*(float)Math.cos(a),z=i==6?0:2.8f*(float)Math.sin(a);
            ring(out,x,z,0,1.12f,1.2f,.87f,DARK);
            ring(out,x,z,1.2f,.87f,3.5f,.65f,GRAY);
            cap(out,x,z,0,1.12f,false,GOLD);
        }
        for(int i=0;i<4;i++) {
            double a=i*Math.PI/2;
            Point p=rot(4.6f,5,-.22f,a),q=rot(6.8f,5,-.22f,a),r=rot(4.6f,17,-.22f,a);
            Point P=rot(4.6f,5,.22f,a),Q=rot(6.8f,5,.22f,a),R=rot(4.6f,17,.22f,a);
            quad(out,p,q,r,r,GRAY); quad(out,R,Q,P,P,GRAY);
            quad(out,p,P,Q,q,DARK); quad(out,q,Q,R,r,GRAY); quad(out,r,R,P,p,GRAY);
        }
        return List.copyOf(out);
    }
    private static Point rot(float x,float y,float z,double a) {
        return new Point((float)(x*Math.cos(a)-z*Math.sin(a)),y,(float)(x*Math.sin(a)+z*Math.cos(a)));
    }
    private static Point polar(float x,float z,float y,float r,int i) {
        double a=i*2*Math.PI/SIDES;
        return new Point(x+r*(float)Math.cos(a),y,z+r*(float)Math.sin(a));
    }
    private static void ring(List<Quad> out,float x,float z,float y0,float r0,float y1,float r1,int color) {
        for(int i=0;i<SIDES;i++) quad(out,polar(x,z,y0,r0,i),polar(x,z,y1,r1,i),polar(x,z,y1,r1,i+1),polar(x,z,y0,r0,i+1),color);
    }
    private static void cap(List<Quad> out,float x,float z,float y,float r,boolean up,int color) {
        Point center=new Point(x,y,z);
        for(int i=0;i<SIDES;i++) {
            Point a=polar(x,z,y,r,i),b=polar(x,z,y,r,i+1);
            if(up) quad(out,center,b,a,a,color); else quad(out,center,a,b,b,color);
        }
    }
    private static void quad(List<Quad> out,Point a,Point b,Point c,Point d,int color) {
        // Cone tip has repeated vertices; use the other edge when necessary.
        float ux=b.x-a.x,uy=b.y-a.y,uz=b.z-a.z,vx=c.x-a.x,vy=c.y-a.y,vz=c.z-a.z;
        float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;
        float len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
        if(len<1e-6f){vx=d.x-a.x;vy=d.y-a.y;vz=d.z-a.z;nx=uy*vz-uz*vy;ny=uz*vx-ux*vz;nz=ux*vy-uy*vx;len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);}
        if(len<1e-6f) throw new IllegalArgumentException("Degenerate surface");
        out.add(new Quad(new Point[]{a,b,c,d},nx/len,ny/len,nz/len,color));
    }
}
