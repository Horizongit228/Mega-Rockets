package dev.megarockets.client;

import java.util.ArrayList;
import java.util.List;

/** Precomputed fixed model; all coordinates in Minecraft blocks. */
public final class TitanMesh {
    private static final int SIDES=72;
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
        // Raised structural bands, separated from the main hull surface.
        for(float y:new float[]{13.2f,18.0f,23.0f,26.5f,29.2f,34.0f,39.0f,41.5f}) {
            ring(out,0,0,y,5.005f,y+.07f,5.10f,GRAY);
            ring(out,0,0,y+.07f,5.10f,y+.22f,5.10f,DARK);
            ring(out,0,0,y+.22f,5.10f,y+.29f,5.005f,GRAY);
        }
        // Long raised silver raceways and dark access panels.
        for(int i=0;i<8;i++) {
            double a=i*Math.PI/4;
            panel(out,5.02f,5.16f,14,26,.13f,a,GRAY);
            panel(out,5.02f,5.16f,30,41,.13f,a,GRAY);
            panel(out,5.02f,5.12f,16,18,.48f,a,DARK);
            panel(out,3.82f,3.95f,49,51,.36f,a,GRAY);
            panel(out,3.82f,3.97f,52.35f,53.65f,.38f,a,0xff132f49);
        }
        ring(out,0,0,47.8f,3.86f,48.1f,3.91f,DARK);
        ring(out,0,0,55.2f,3.83f,55.4f,3.91f,GRAY);
        for(int i=0;i<16;i++){
            double a=i*Math.PI/8;
            panel(out,5.015f,5.055f,10,12.8f,.72f,a,i%2==0?0xff202a38:0xff455163);
            panel(out,5.01f,5.045f,27.25f,28.75f,.55f,a,0xff69788b);
            panel(out,5.02f,5.07f,42.2f,43.75f,.48f,a,0xff667485);
            panel(out,3.82f,3.99f,54.1f,54.85f,.30f,a,0xff476d85);
            if(i%2==0)for(int j=0;j<5;j++)panel(out,5.12f,5.21f,16.15f+j*.31f,16.25f+j*.31f,.34f,a,0xffa8b6c3);
        }
        for(int i=0;i<8;i++){
            double a=i*Math.PI/4;
            panel(out,3.96f,4.02f,52.40f,53.60f,.34f,a,0xff172b44);
            panel(out,4.025f,4.045f,53.32f,53.53f,.28f,a,0xff90c7e4);
            panel(out,4.05f,4.10f,53.70f,53.88f,.12f,a,i%2==0?0xff59e4ff:0xffff7962);
        }
        for(float y:new float[]{30.2f,30.5f,31.0f,39.5f,40.0f,40.3f})
            ring(out,0,0,y,5.015f,y+.07f,5.08f,0xff516176);
        for(int i=0;i<4;i++)panel(out,5.02f,5.045f,32,38,.65f,i*Math.PI/2+Math.PI/8,0xff286083);
        cap(out,0,0,3,4.3f,false,DARK);
        for(int i=0;i<7;i++) {
            float a=(float)(i*Math.PI/3),x=i==6?0:2.8f*(float)Math.cos(a),z=i==6?0:2.8f*(float)Math.sin(a);
            ring(out,x,z,0,1.12f,1.2f,.87f,DARK);
            ring(out,x,z,1.2f,.87f,3.5f,.65f,GRAY);
            ring(out,x,z,.08f,1.14f,.24f,1.14f,GRAY);
            ring(out,x,z,.54f,1.04f,.67f,1.04f,GOLD);
            ring(out,x,z,1.20f,.91f,1.38f,.91f,DARK);
            ring(out,x,z,2.2f,.80f,2.38f,.80f,GOLD);
            for(int j=0;j<5;j++){
                float y=.28f+j*.15f,rr=1.13f-y*.18f;
                ring(out,x,z,y,rr,y+.045f,rr,0xffb9a17f);
            }
            ring(out,x,z,0,1.12f,0,.72f,GOLD);
            ring(out,x,z,0,.72f,.35f,.55f,DARK);
            cap(out,x,z,.35f,.55f,false,0xff171d25);
        }
        for(int i=0;i<4;i++) {
            double a=i*Math.PI/2;
            Point p=rot(4.6f,5,-.22f,a),q=rot(6.8f,5,-.22f,a),r=rot(4.6f,17,-.22f,a);
            Point P=rot(4.6f,5,.22f,a),Q=rot(6.8f,5,.22f,a),R=rot(4.6f,17,.22f,a);
            quad(out,p,q,r,r,GRAY);
            panel(out,5.1f,6.85f,0.05f,.38f,.32f,a,0xff555e6c);
            panel(out,4.55f,4.85f,3.0f,5.5f,.36f,a,0xffafbbc8); quad(out,R,Q,P,P,GRAY);
            quad(out,p,P,Q,q,DARK); quad(out,q,Q,R,r,GRAY); quad(out,r,R,P,p,GRAY);
        }
        return List.copyOf(out);
    }
    private static void panel(List<Quad> out,float r0,float r1,float y0,float y1,float w,double a,int color) {
        Point p=rot(r0,y0,-w,a),q=rot(r0,y0,w,a),r=rot(r0,y1,w,a),s=rot(r0,y1,-w,a);
        Point P=rot(r1,y0,-w,a),Q=rot(r1,y0,w,a),R=rot(r1,y1,w,a),S=rot(r1,y1,-w,a);
        quad(out,P,Q,R,S,color); quad(out,p,P,S,s,GRAY); quad(out,Q,q,r,R,GRAY);
        quad(out,p,q,Q,P,DARK); quad(out,s,S,R,r,GRAY);
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
