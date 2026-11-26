package dev.megarockets.client;
import dev.megarockets.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class SolarMapScreen extends Screen {
 private static final Destination[] BODIES={null,Destination.MERCURY,Destination.VENUS,Destination.MOON,Destination.MARS,Destination.JUPITER,Destination.EUROPA,Destination.SATURN,Destination.TITAN,Destination.URANUS,Destination.NEPTUNE};
 private int selected=0,zoom=0,cx,cy,radius,px,py,pw;
 private ButtonWidget course,zoomButton,jumpButton;
 private final Screen parent;
 public SolarMapScreen(Screen parent){super(Text.translatable("map.megarockets.title"));this.parent=parent;}
 private Text tr(String key,Object... args){return Text.translatable("map.megarockets."+key,args);}
 private boolean station(){return selected==BODIES.length;}
 private Destination body(){return station()?null:BODIES[selected];}
 private Text name(int i){return i==BODIES.length?tr("station"):Text.translatable("destination.megarockets."+(BODIES[i]==null?"earth":BODIES[i].key()));}
 private SolarDynamics.Position position(int i,double time){return i==BODIES.length?new SolarDynamics.Position(StationLayout.PAD_X,StationLayout.Z):SolarDynamics.position(BODIES[i],time);}
 private double extent(){return SolarDynamics.AU*(zoom==0?3.5:zoom==1?12:32);}
 private TitanRocket rocket(){return client!=null&&client.player!=null&&client.player.getVehicle() instanceof TitanRocket r?r:null;}
 private boolean orbit(){return client!=null&&client.world!=null&&client.world.getRegistryKey().equals(MegaRockets.ORBIT);}
 private void advance(int by){selected=Math.floorMod(selected+by,BODIES.length+1);var p=position(selected,0);double distance=Math.hypot(p.x(),p.z());if(distance>extent())zoom=distance>12*SolarDynamics.AU?2:1;refresh();}
 private void refresh(){if(zoomButton!=null)zoomButton.setMessage(tr("zoom",zoom==0?"3.5":zoom==1?"12":"32"));if(course!=null){course.setMessage(tr(station()?"station_course":selected==0?"earth_course":"choose_course"));TitanRocket r=rocket();course.active=r!=null&&(station()||selected==0?orbit()&&r.armed():r.manualMode()||r.cabinReady());course.active=course.active&&r!=null&&!r.jumping();}if(jumpButton!=null){TitanRocket r=rocket();jumpButton.active=r!=null&&!station()&&r.armed()&&!r.jumping()&&client.world!=null&&(orbit()||client.world.getRegistryKey().equals(net.minecraft.world.World.OVERWORLD)||MegaRockets.planet(client.world)!=null)&&(orbit()||MegaRockets.planet(client.world)!=body());}}
 @Override protected void init(){
  int left=Math.max(8,(width-Math.min(width-16,450))/2),top=30,w=Math.min(width-16,450);
  int mapSize=Math.min(Math.max(90,height-86),Math.max(90,w*3/5));
  cx=left+mapSize/2;cy=top+mapSize/2;radius=mapSize/2-8;px=left+mapSize+9;py=top;pw=Math.max(60,left+w-px);
  addDrawableChild(ButtonWidget.builder(Text.literal("◀"),b->advance(-1)).dimensions(px,py+19,20,20).build());
  addDrawableChild(ButtonWidget.builder(Text.literal("▶"),b->advance(1)).dimensions(px+pw-20,py+19,20,20).build());
  zoomButton=addDrawableChild(ButtonWidget.builder(tr("zoom","3.5"),b->{zoom=(zoom+1)%3;refresh();}).dimensions(left,height-49,Math.min(100,w/3),20).build());
  int zw=Math.min(90,w/3),cw=(w-zw-6)/2;
  zoomButton.setWidth(zw);
  course=addDrawableChild(ButtonWidget.builder(tr("choose_course"),b->choose()).dimensions(left+zw+3,height-49,cw,20).build());
  jumpButton=addDrawableChild(ButtonWidget.builder(tr("jump"),b->{if(rocket()!=null&&!station())client.player.networkHandler.sendChatCommand("rocket jump "+(body()==null?"earth":body().key()));}).dimensions(left+zw+cw+6,height-49,w-zw-cw-6,20).build());
  addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"),b->close()).dimensions(left,height-25,w,20).build());refresh();
 }
 private void choose(){TitanRocket r=rocket();if(r==null)return;
  if(station()||selected==0){if(orbit()&&ClientPlayNetworking.canSend(CockpitAction.ID))ClientPlayNetworking.send(new CockpitAction((station()?CockpitAction.Control.STATION_NAV:CockpitAction.Control.RETURN_EARTH).ordinal()));}
  else client.player.networkHandler.sendChatCommand("rocket destination "+body().key());
 }
 @Override public void tick(){refresh();}
 @Override public void close(){if(client!=null)client.setScreen(parent);}
 @Override public boolean shouldPause(){return false;}
 private int sx(double x){return cx+(int)Math.round(x*radius/extent());}
 private int sy(double z){return cy+(int)Math.round(z*radius/extent());}
 private boolean visible(SolarDynamics.Position p){return Math.hypot(p.x(),p.z())<=extent();}
 private int tint(int i){if(i==BODIES.length)return 0xff9af6b3;var d=BODIES[i];if(d==null)return 0xff64b6ef;return switch(d){case MERCURY,MOON->0xffb2bbc9;case VENUS,TITAN,SATURN->0xffe6bd74;case MARS->0xffe18a65;case JUPITER->0xffcda78d;case EUROPA,URANUS->0xffa8e9ef;case NEPTUNE->0xff769aff;};}
 private void dot(DrawContext c,int x,int y,int r,int color){c.fill(x-r,y-r,x+r+1,y+r+1,color);}
 private void ring(DrawContext c,double r,int color){if(r>extent())return;int steps=Math.max(30,Math.min(720,(int)(2*Math.PI*r*radius/extent())));for(int i=0;i<steps;i++){double a=i*2*Math.PI/steps;dot(c,sx(r*Math.cos(a)),sy(r*Math.sin(a)),0,color);}}
 private SolarDynamics.Position origin(double ticks){
  if(client==null||client.world==null||client.player==null)return SolarDynamics.position(null,ticks);
  if(orbit()){TitanRocket r=rocket();return r==null?new SolarDynamics.Position(client.player.getX(),client.player.getZ()):new SolarDynamics.Position(r.getX(),r.getZ());}
  return SolarDynamics.position(MegaRockets.planet(client.world),ticks);
 }
 @Override public boolean mouseClicked(double x,double y,int button){
  if(button==0&&client!=null&&client.world!=null&&Math.hypot(x-cx,y-cy)<=radius){
   double best=12;int chosen=-1;for(int i=0;i<=BODIES.length;i++){var p=position(i,client.world.getTime());if(!visible(p))continue;double dist=Math.hypot(x-sx(p.x()),y-sy(p.z()));if(dist<best){best=dist;chosen=i;}}
   if(chosen>=0){selected=chosen;refresh();return true;}
  }return super.mouseClicked(x,y,button);
 }
 @Override public void render(DrawContext c,int mouseX,int mouseY,float delta){
  c.fill(0,0,width,height,0xff07131e);c.drawCenteredTextWithShadow(textRenderer,title,width/2,10,0xff8eddf6);
  c.fill(cx-radius-5,cy-radius-5,cx+radius+5,cy+radius+5,0xff0c2030);c.drawBorder(cx-radius-5,cy-radius-5,radius*2+10,radius*2+10,0xff34576d);
  ring(c,SolarDynamics.AU,0xff355063);
  for(Destination d:Destination.values())if(d.solarOrbitKm>0&&d!=Destination.EUROPA&&d!=Destination.TITAN)ring(c,d.solarOrbitKm/SolarScale.KM_PER_TRAVEL_BLOCK,0xff304657);
  for(int i=0;i<350;i++){double a=i*2.399963,r=SolarDynamics.AU*(2.1+1.2*((i*67)%101)/100.);if(r<=extent())dot(c,sx(r*Math.cos(a)),sy(r*Math.sin(a)),0,0xff696458);}
  dot(c,cx,cy,3,0xffffd67a);double ticks=client!=null&&client.world!=null?client.world.getTime():0;
  var start=origin(ticks);var end=position(selected,ticks);
  if(visible(start)&&visible(end))for(int i=0;i<90;i+=2){double t=i/90.;dot(c,sx(start.x()+(end.x()-start.x())*t),sy(start.z()+(end.z()-start.z())*t),0,0xff7cc7e8);}
  for(int i=0;i<=BODIES.length;i++){var p=position(i,ticks);if(!visible(p))continue;int x=sx(p.x()),y=sy(p.z());if(i==selected)c.drawBorder(x-5,y-5,11,11,0xffeaf9ff);dot(c,x,y,i==selected?2:1,tint(i));if(i==selected||i==BODIES.length)c.drawTextWithShadow(textRenderer,name(i),x+6,y-6,tint(i));}
  if(visible(start)){int x=sx(start.x()),y=sy(start.z());dot(c,x,y,1,0xffffffff);c.drawBorder(x-3,y-3,7,7,0xff90f6ff);}
  c.drawTextWithShadow(textRenderer,name(selected),px,py,0xffe5eff6);int ty=py+46;
  c.drawTextWithShadow(textRenderer,Text.literal("X "+(int)end.x()),px,ty,0xffa9c9db);c.drawTextWithShadow(textRenderer,Text.literal("Z "+(int)end.z()),px,ty+12,0xffa9c9db);
  double dist=end.distance(start.x(),start.z());c.drawTextWithShadow(textRenderer,tr("range",(int)dist),px,ty+28,0xffe5eff6);TitanRocket r=rocket();
  if(r!=null){long seconds=(long)Math.ceil((station()?dist/Math.max(.1,r.maxSpeed()):SolarDynamics.arrivalTime(body(),ticks,start.x(),start.z(),r.maxSpeed()*r.throttle()/100.))/20);c.drawTextWithShadow(textRenderer,tr("eta",seconds/60,seconds%60),px,ty+42,0xffe6ba7d);}
  c.drawTextWithShadow(textRenderer,tr(station()?"station_hint":body()!=null&&body().orbitOnly()?"orbit_only":"surface"),px,ty+60,0xff84d7ae);super.render(c,mouseX,mouseY,delta);
 }
}
