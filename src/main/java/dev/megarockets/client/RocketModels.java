package dev.megarockets.client;

import java.util.ArrayList;
import java.util.List;
import static dev.megarockets.client.TitanMesh.*;
/** Three independently built hulls, matched cabins, glazing and engine layouts.
 * All upright model vertices stay in the existing 14 x 64 entity envelope. */
public final class RocketModels {
 public enum Style{TITAN,PHANTOM,HYPERION}
 public record Engine(float x,float z,float radius){}
 public record Model(List<Quad> hull,List<Quad> cabin,List<Quad> glass,List<Quad> lamps,List<Engine> engines,int accent){}
 private static final int LIGHT=0xffe4e8ee,DARK=0xff293544,METAL=0xffa8b5c5;
 private static final Model[] MODELS={build(Style.TITAN),build(Style.PHANTOM),build(Style.HYPERION)};
 public static Model get(Style s){return MODELS[s.ordinal()];}
 private static Model build(Style style){
  var hull=new ArrayList<Quad>();var inside=new ArrayList<Quad>();var glass=new ArrayList<Quad>();var lamps=new ArrayList<Quad>();var engines=new ArrayList<Engine>();
  int accent=switch(style){case TITAN->0xffe35542;case PHANTOM->0xffff8a3b;case HYPERION->0xff65dcff;};
  int trim=switch(style){case TITAN->0xffc8d4e1;case PHANTOM->0xff667589;case HYPERION->0xffe3c078;};
  switch(style){
   case TITAN->{
    // Wide industrial, two-stage body with a thrust skirt and six radial braces.
    profile(hull,new float[][]{{3,5.8f},{7,5.8f},{12,5.45f},{28,5.45f},{30,5.2f},{44,5.2f},{48,3.8f},{52,3.8f}},LIGHT);
    for(float y:new float[]{7,12,28,30,42,44})band(hull,y,y<29?5.48f:5.23f,DARK);
    for(int i=0;i<8;i++){double a=i*Math.PI/4;panel(hull,5.46f,5.65f,13,26,.22f,a,METAL);panel(hull,5.22f,5.42f,32,40,.45f,a,trim);}
    for(int i=0;i<6;i++){double a=i*Math.PI/3;fin(hull,a,5.25f,6.85f,1,14,.35f,trim);engines.add(new Engine(3.65f*(float)Math.cos(a),3.65f*(float)Math.sin(a),1.06f));}
    engines.add(new Engine(0,0,1.38f));cap(hull,0,0,3,5.8f,false,DARK);
    profile(hull,new float[][]{{54,3.8f},{57,3.65f},{61,2.2f},{64,0}},LIGHT);
   }
   case PHANTOM->{
    // Needle-shaped central fuselage; twin long swept chines and four booster fairings.
    profile(hull,new float[][]{{3,3.7f},{8,4.1f},{19,3.25f},{33,2.95f},{43,3.1f},{48,3.8f},{52,3.8f}},LIGHT);
    cap(hull,0,0,3,3.7f,false,DARK);
    for(int i=0;i<4;i++){double a=i*Math.PI/2+Math.PI/4;
     fin(hull,a,3.15f,6.72f,4,29,.23f,trim);
     float x=2.5f*(float)Math.cos(a),z=2.5f*(float)Math.sin(a);
     ring(hull,x,z,3,1.12f,8,1.45f,trim);ring(hull,x,z,8,1.45f,18,1.25f,LIGHT);ring(hull,x,z,18,1.25f,23,0,DARK);
     engines.add(new Engine(x,z,.94f));
     panel(hull,3.04f,3.19f,24,40,.15f,a,accent);
    }
    engines.add(new Engine(0,0,1.65f));
    profile(hull,new float[][]{{54,3.8f},{55.6f,3.8f},{60,1.65f},{64,0}},LIGHT);
   }
   case HYPERION->{
    // Central spindle and three independent nacelles with flared annular drive rings.
    profile(hull,new float[][]{{3,4.35f},{9,4.8f},{18,3.6f},{32,3.6f},{43,4.45f},{48,3.8f},{52,3.8f}},LIGHT);
    cap(hull,0,0,3,4.35f,false,DARK);
    for(int i=0;i<3;i++){double a=i*2*Math.PI/3;float x=4.25f*(float)Math.cos(a),z=4.25f*(float)Math.sin(a);
     ring(hull,x,z,3,1.50f,10,2.05f,trim);ring(hull,x,z,10,2.05f,29,1.55f,LIGHT);ring(hull,x,z,29,1.55f,37,0,DARK);
     ring(hull,x,z,12,1.99f,12.45f,2.15f,accent);ring(hull,x,z,24,1.70f,24.45f,1.88f,accent);
     fin(hull,a,3.3f,5.7f,9,33,.24f,trim);engines.add(new Engine(x,z,1.44f));
    }
    engines.add(new Engine(0,0,1.7f));
    for(float y:new float[]{19,24,30}){band(hull,y,4.25f,trim);band(hull,y+.25f,4.28f,accent);}
    profile(hull,new float[][]{{54,3.8f},{57.5f,3.95f},{60,3.15f},{62.5f,1.8f},{64,0}},LIGHT);
   }
  }
  // Independent window segmentation; same seat height and shell interface.
  for(int i=0;i<72;i++){
   boolean pillar=switch(style){case TITAN->i%9==0||i%9==8;case PHANTOM->i%12<4;case HYPERION->i%18==0||i%18==17;};
   List<Quad> dest=pillar?hull:glass;float r=pillar?3.8f:3.79f;
   quad(dest,polar(0,0,52,r,i),polar(0,0,54,r,i),polar(0,0,54,r,i+1),polar(0,0,52,r,i+1),pillar?DARK:0x3279c7ed);
  }
  band(hull,51.9f,3.83f,trim);band(hull,53.9f,3.83f,trim);
  for(Engine e:engines){
   float x=e.x(),z=e.z(),r=e.radius();
   ring(hull,x,z,0,r,1.1f,r*.79f,DARK);ring(hull,x,z,1.1f,r*.79f,3.3f,r*.58f,METAL);
   for(int j=0;j<4;j++){float y=.18f+j*.22f;ring(hull,x,z,y,r*(1-y*.19f)+.02f,y+.065f,r*(1-y*.19f)+.02f,trim);}
   ring(hull,x,z,0,r,0,r*.62f,trim);ring(hull,x,z,0,r*.62f,.45f,r*.48f,DARK);cap(hull,x,z,.45f,r*.48f,false,0xff101722);
  }
  cabin(style,inside,lamps,accent,trim);
  return new Model(List.copyOf(hull),List.copyOf(inside),List.copyOf(glass),List.copyOf(lamps),List.copyOf(engines),accent);
 }
 private static void profile(List<Quad> q,float[][] stations,int color){
  for(int i=1;i<stations.length;i++)ring(q,0,0,stations[i-1][0],stations[i-1][1],stations[i][0],stations[i][1],color);
 }
 private static void band(List<Quad> q,float y,float r,int color){ring(q,0,0,y,r,y+.18f,r,color);}
 private static void fin(List<Quad> q,double a,float root,float tip,float bottom,float top,float half,int color){
  Point p=rot(root,bottom,-half,a),b=rot(tip,bottom,-half,a),c=rot(root,top,-half,a),
   P=rot(root,bottom,half,a),B=rot(tip,bottom,half,a),C=rot(root,top,half,a);
  quad(q,p,b,c,c,color);quad(q,P,C,B,B,color);quad(q,p,P,B,b,DARK);quad(q,b,B,C,c,METAL);quad(q,c,C,P,p,DARK);
 }
 private static void cabin(Style style,List<Quad> q,List<Quad> lamps,int accent,int trim){
  int wall=style==Style.PHANTOM?0xff546172:0xffc3ced8;
  int floor=style==Style.HYPERION?0xff53677c:0xff6b7989,seat=0xff263544;
  cap(q,0,0,50.95f,3.72f,true,floor);cap(q,0,0,55.45f,3.72f,false,wall);
  ring(q,0,0,51,3.68f,52,3.68f,wall);ring(q,0,0,54,3.68f,55.45f,3.68f,wall);
  ring(lamps,0,0,54.92f,3.48f,55.04f,3.48f,accent);
  // Pilot seat with harness, pedestal, footrests and dual flight sticks.
  box(q,-.38f,50.96f,.40f,.38f,51.12f,.95f,METAL);
  box(q,-.47f,51.12f,.27f,.47f,51.35f,1.02f,seat);
  box(q,-.49f,51.35f,1.02f,.49f,52.64f,1.26f,seat);
  box(q,-.35f,52.64f,1.03f,.35f,52.9f,1.24f,trim);
  for(int side:new int[]{-1,1}){
   float x=side*.57f;box(q,x-.12f,51.30f,.20f,x+.12f,51.75f,1.06f,trim);
   box(q,x-.08f,51.75f,.29f,x+.08f,52.03f,.45f,DARK);
   box(q,x-.13f,52.03f,.25f,x+.13f,52.15f,.49f,seat);
   box(lamps,x-.055f,52.15f,.29f,x+.055f,52.17f,.40f,accent);
   box(q,side*.23f-.08f,50.98f,-.95f,side*.23f+.08f,51.10f,-.6f,trim);
  }
  // The three central MFDs have common live readout positions (see renderer).
  box(q,-1.98f,51,-2.67f,1.98f,52.04f,-1.62f,seat);
  box(q,-2.02f,52.04f,-2.70f,2.02f,52.18f,-1.55f,trim);
  for(int i=-1;i<=1;i++){
   float x=i*1.24f;
   box(q,x-.56f,52.18f,-2.49f,x+.56f,52.91f,-2.24f,DARK);
   box(q,x-.49f,52.26f,-2.24f,x+.49f,52.84f,-2.21f,0xff091923);
   for(int j=0;j<4;j++)box(q,x-.43f+j*.24f,52.19f,-2.21f,x-.30f+j*.24f,52.24f,-2.12f,trim);
  }
  for(int j=0;j<12;j++){float x=-1.73f+j*.30f;box(q,x,52.18f,-1.95f,x+.17f,52.25f,-1.72f,DARK);box(lamps,x+.02f,52.25f,-1.93f,x+.15f,52.27f,-1.75f,j%3==0?0xffe3ac58:accent);}
  for(int side:new int[]{-1,1}){
   float x=side*2.55f;
   box(q,x-.42f,51,-1.05f,x+.42f,52.14f,1.22f,seat);
   box(q,x-.43f,52.14f,-1.06f,x+.43f,52.25f,1.23f,trim);
   for(int j=0;j<6;j++)box(lamps,x-.26f,52.25f,-.86f+j*.31f,x+.26f,52.30f,-.72f+j*.31f,j%2==0?accent:0xffe0b56e);
   ring(q,side*1.18f,2.55f,51,.36f,53.2f,.36f,METAL);
   cap(q,side*1.18f,2.55f,53.2f,.36f,true,trim);
   ring(lamps,side*1.18f,2.55f,52.5f,.37f,52.64f,.37f,accent);
   // Vent grille and equipment cabinet, unique to each cabin palette.
   for(int j=0;j<6;j++)panel(q,3.53f,3.63f,54.15f+j*.12f,54.21f+j*.12f,.30f,side*Math.PI/2,DARK);
  }
  box(q,-.48f,51,2.64f,.48f,53.25f,3.18f,trim);
  box(q,-.38f,51.19f,2.61f,.38f,53.08f,2.64f,DARK);
  box(lamps,-.25f,52.72f,2.60f,.25f,52.82f,2.62f,accent);
  // Overhead switch banks, illuminated rails, floor anti-slip strips.
  box(q,-1.12f,54.92f,-1.4f,1.12f,55.18f,-.25f,DARK);
  for(int j=0;j<9;j++){float x=-.96f+j*.22f;box(lamps,x,54.88f,-1.19f,x+.13f,54.92f,-.85f,j%3==0?trim:accent);}
  for(int j=0;j<8;j++){float z=-1.6f+j*.42f;box(q,-1.22f,50.95f,z,1.22f,50.99f,z+.065f,trim);}
  if(style==Style.TITAN){
   for(int i=0;i<4;i++)panel(q,3.50f,3.61f,54.12f,54.83f,.35f,i*Math.PI/2,trim);
  }else if(style==Style.PHANTOM){
   box(q,-1.92f,52.24f,-1.61f,-1.12f,52.38f,-1.10f,seat);
   box(lamps,-1.84f,52.38f,-1.52f,-1.18f,52.40f,-1.19f,accent);
   box(q,1.12f,52.24f,-1.61f,1.92f,52.38f,-1.10f,seat);
  }else{
   for(int side:new int[]{-1,1}){
    float x=side*1.45f;box(q,x-.35f,53.20f,-2.4f,x+.35f,53.55f,-2.17f,DARK);
    box(lamps,x-.29f,53.25f,-2.17f,x+.29f,53.50f,-2.15f,accent);
   }
  }
 }
 private RocketModels(){}
}
