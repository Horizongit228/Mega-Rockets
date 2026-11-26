package dev.megarockets;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class TitanRocket extends Entity {
    public static final int TARGET_ALTITUDE = 10_000;
    private static final TrackedData<Boolean> ENGINES = DataTracker.registerData(TitanRocket.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> CABIN_TARGET=DataTracker.registerData(TitanRocket.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> CABIN_SPEED=DataTracker.registerData(TitanRocket.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> CABIN_PHASE=DataTracker.registerData(TitanRocket.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> CABIN_PROGRESS=DataTracker.registerData(TitanRocket.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> MANUAL=DataTracker.registerData(TitanRocket.class,TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> ARMED=DataTracker.registerData(TitanRocket.class,TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> LIGHTS=DataTracker.registerData(TitanRocket.class,TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> LIFE=DataTracker.registerData(TitanRocket.class,TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> BEACON=DataTracker.registerData(TitanRocket.class,TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> NAV=DataTracker.registerData(TitanRocket.class,TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> PRESSURE=DataTracker.registerData(TitanRocket.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> HEAT=DataTracker.registerData(TitanRocket.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> THROTTLE=DataTracker.registerData(TitanRocket.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> ENTRY_PROGRESS=DataTracker.registerData(TitanRocket.class,TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> GROUND_CLEARANCE=DataTracker.registerData(TitanRocket.class,TrackedDataHandlerRegistry.INTEGER);
    private ManualEntry entry;
    private boolean manualLanded;
    private int manualMask,lastManualAge=-1000;
    private ManualFlight.Motion manualVelocity=ManualFlight.STOP;
    private Destination destination=Destination.MOON;
    private int countdown;
    private LunarFlight.State lunar;
    private boolean lunarPaused;
    private boolean moonLanded;
    private LandingSurvey.Site landingSite;
    private int landingCandidate;
    private double flightSpeed;
    private double lerpX, lerpY, lerpZ;
    private int lerpTicks;
    private float lerpYaw, lerpPitch;
    private int flightTicks;
    private boolean flying;

    public TitanRocket(EntityType<? extends TitanRocket> type, World world) {
        super(type, world);
        setNoGravity(true);
    }

    public EntityType<TitanRocket> rocketType(){return getType()==MegaRockets.HYPERION?MegaRockets.HYPERION:getType()==MegaRockets.PHANTOM?MegaRockets.PHANTOM:MegaRockets.TITAN;}
    public double maxSpeed(){return getType()==MegaRockets.HYPERION?50:getType()==MegaRockets.PHANTOM?30:15;}
    public int nextLandingCandidate(){int index=landingCandidate;landingCandidate=(landingCandidate+1)%LandingSurvey.COUNT;return index;}
    public void setLandingSite(LandingSurvey.Site site){landingSite=site;}
    private LandingSurvey.Site site(){
        return landingSite!=null?landingSite:new LandingSurvey.Site(0,destination.padY,0,16,destination.padY,0);
    }
    public Destination destination(){return destination;}
    public void copyDestination(TitanRocket old){destination=old.destination;dataTracker.set(CABIN_TARGET,destination.ordinal());copySystems(old);}
    private void copySystems(TitanRocket old){
        dataTracker.set(ARMED,old.armed());dataTracker.set(LIGHTS,old.cabinLights());dataTracker.set(LIFE,old.lifeSupport());
        dataTracker.set(BEACON,old.beacon());dataTracker.set(PRESSURE,old.cabinPressure());dataTracker.set(HEAT,old.engineHeat());dataTracker.set(THROTTLE,old.throttle());
    }
    public boolean selectDestination(Destination value){
        if(!manualMode()&&(countdown>0||flying||lunar!=null||!getWorld().getRegistryKey().equals(World.OVERWORLD)))return false;
        destination=value;dataTracker.set(CABIN_TARGET,value.ordinal());return true;
    }
    public void showDestinations(ServerPlayerEntity pilot){
        if(!manualMode()&&(!getWorld().getRegistryKey().equals(World.OVERWORLD)||countdown>0||flying||lunar!=null))return;
        pilot.sendMessage(Text.translatable("message.megarockets.destination_current",Text.translatable("destination.megarockets."+destination.key()),(int)(maxSpeed()*20)),false);
        var menu=Text.empty();
        for(Destination d:Destination.values()){
            menu.append(Text.literal("[ ").append(Text.translatable("destination.megarockets."+d.key())).append(" | "+(int)d.distance+" ] ")
                .styled(style->style.withColor(d==destination?net.minecraft.util.Formatting.GREEN:net.minecraft.util.Formatting.AQUA)
                    .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.RUN_COMMAND,"/rocket destination "+d.key()))));
        }
        menu.append(Text.translatable("message.megarockets.launch_button").styled(style->style.withColor(net.minecraft.util.Formatting.GOLD)
            .withClickEvent(new net.minecraft.text.ClickEvent(net.minecraft.text.ClickEvent.Action.RUN_COMMAND,"/rocket launch"))));
        pilot.sendMessage(menu,false);
    }
    @Override protected void initDataTracker(DataTracker.Builder builder) { builder.add(ENGINES, false);builder.add(CABIN_TARGET,0);builder.add(CABIN_SPEED,0);builder.add(CABIN_PHASE,-1);builder.add(CABIN_PROGRESS,0);builder.add(MANUAL,false);builder.add(ARMED,true);builder.add(LIGHTS,true);builder.add(LIFE,true);builder.add(BEACON,false);builder.add(NAV,false);builder.add(PRESSURE,100);builder.add(HEAT,0);builder.add(THROTTLE,100);builder.add(ENTRY_PROGRESS,-1);builder.add(GROUND_CLEARANCE,-1); }
    public Destination cabinTarget(){return Destination.values()[Math.max(0,Math.min(Destination.values().length-1,dataTracker.get(CABIN_TARGET)))];}
    public int cabinSpeed(){return dataTracker.get(CABIN_SPEED);}
    public int cabinProgress(){return dataTracker.get(CABIN_PROGRESS);}
    public int entryProgress(){return dataTracker.get(ENTRY_PROGRESS);}
    public int groundClearance(){return dataTracker.get(GROUND_CLEARANCE);}
    public boolean armed(){return dataTracker.get(ARMED);}
    public boolean cabinLights(){return dataTracker.get(LIGHTS);}
    public boolean lifeSupport(){return dataTracker.get(LIFE);}
    public boolean beacon(){return dataTracker.get(BEACON);}
    public boolean autoNavigation(){return dataTracker.get(NAV);}
    public int cabinPressure(){return dataTracker.get(PRESSURE);}
    public int engineHeat(){return dataTracker.get(HEAT);}
    public int throttle(){return dataTracker.get(THROTTLE);}
    public boolean cabinBreathable(){return CabinSystems.breathable(cabinPressure());}
    public boolean manualMode(){return dataTracker.get(MANUAL);}
    public boolean cabinReady(){return !manualMode()&& dataTracker.get(CABIN_PHASE)==-1&&getWorld().getRegistryKey().equals(World.OVERWORLD);}
    public Text cabinPhase(){
        int phase=dataTracker.get(CABIN_PHASE);
        if(entryProgress()>=0)return Text.translatable("cockpit.megarockets.entry_loading",entryProgress());
        if(manualMode()&&groundClearance()>=0)return Text.translatable("cockpit.megarockets.manual_descent",groundClearance());
        if(autoNavigation())return Text.translatable("cockpit.megarockets.auto_nav");
        if(manualMode())return Text.translatable("cockpit.megarockets.manual");
        if(phase==-1)return Text.translatable("cockpit.megarockets.ready");
        if(phase==-2)return Text.translatable("cockpit.megarockets.countdown");
        if(phase==-3)return Text.translatable("cockpit.megarockets.ascent");
        return Text.translatable("message.megarockets.phase."+LunarFlight.Phase.values()[Math.min(phase,LunarFlight.Phase.values().length-1)].name().toLowerCase(java.util.Locale.ROOT));
    }
    private void syncCabin(){
        dataTracker.set(CABIN_SPEED,(int)Math.round(flightSpeed*20));
        dataTracker.set(CABIN_PHASE,lunar!=null?lunar.phase().ordinal():countdown>0?-2:flying?-3:-1);
        dataTracker.set(CABIN_PROGRESS,entryProgress()>=0?entryProgress():manualMode()?0:lunar!=null?(int)Math.min(100,100*lunar.x()/destination.distance):flying?(int)Math.min(100,getY()/TARGET_ALTITUDE*100):0);
    }
    public boolean enginesRunning() { return dataTracker.get(ENGINES); }
    @Override protected void readCustomDataFromNbt(NbtCompound nbt) {
        countdown=0; flightTicks=0; flying=false; flightSpeed=0; dataTracker.set(ENGINES,false);
        moonLanded=nbt.getBoolean("MoonLanded");manualLanded=nbt.getBoolean("ManualLanded");
        dataTracker.set(MANUAL,nbt.getBoolean("ManualMode"));manualVelocity=ManualFlight.STOP;manualMask=0;
        dataTracker.set(ARMED,!nbt.contains("EnginesArmed")||nbt.getBoolean("EnginesArmed"));
        dataTracker.set(LIGHTS,!nbt.contains("CabinLights")||nbt.getBoolean("CabinLights"));
        dataTracker.set(LIFE,!nbt.contains("LifeSupport")||nbt.getBoolean("LifeSupport"));
        dataTracker.set(BEACON,nbt.getBoolean("Beacon"));dataTracker.set(NAV,false);
        dataTracker.set(PRESSURE,nbt.contains("CabinPressure")?CabinSystems.clamp(nbt.getInt("CabinPressure")):100);
        dataTracker.set(HEAT,CabinSystems.clamp(nbt.getInt("EngineHeat")));
        dataTracker.set(THROTTLE,nbt.contains("Throttle")?CabinSystems.throttle(nbt.getInt("Throttle"),0):100);
        landingCandidate=Math.floorMod(nbt.getInt("LandingCandidate"),LandingSurvey.COUNT);
        if(nbt.contains("LandingY"))landingSite=new LandingSurvey.Site(nbt.getInt("LandingX"),nbt.getInt("LandingY"),nbt.getInt("LandingZ"),nbt.getInt("ExitX"),nbt.getInt("ExitY"),nbt.getInt("ExitZ"));
        destination=Destination.read(nbt.getString("Destination"));dataTracker.set(CABIN_TARGET,destination.ordinal());
        if(nbt.contains("LunarPhase")) {
            try {
                var phase=LunarFlight.Phase.valueOf(nbt.getString("LunarPhase"));
                double oldDistance=nbt.contains("RouteDistance")?nbt.getDouble("RouteDistance"):(destination==Destination.MARS?24000:6000);
                double routeX=Math.max(0,Math.min(destination.distance,nbt.getDouble("LunarX")*destination.distance/Math.max(1,oldDistance)));
                lunar=LunarFlight.pause(new LunarFlight.State(phase,nbt.getInt("LunarTicks"),routeX,
                    nbt.getDouble("LunarY"),0,nbt.getFloat("LunarTilt")),destination.distance,maxSpeed());
                lunarPaused=true;
            } catch(IllegalArgumentException ignored) { lunar=null; }
        }
    }
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putBoolean("MoonLanded",moonLanded);nbt.putBoolean("ManualLanded",manualLanded);
        nbt.putBoolean("ManualMode",manualMode());
        nbt.putBoolean("EnginesArmed",armed());nbt.putBoolean("CabinLights",cabinLights());nbt.putBoolean("LifeSupport",lifeSupport());nbt.putBoolean("Beacon",beacon());
        nbt.putInt("CabinPressure",cabinPressure());nbt.putInt("EngineHeat",engineHeat());nbt.putInt("Throttle",throttle());
        nbt.putInt("LandingCandidate",landingCandidate);
        if(landingSite!=null){
            nbt.putInt("LandingX",landingSite.x());nbt.putInt("LandingY",landingSite.y());nbt.putInt("LandingZ",landingSite.z());
            nbt.putInt("ExitX",landingSite.exitX());nbt.putInt("ExitY",landingSite.exitY());nbt.putInt("ExitZ",landingSite.exitZ());
        }
        nbt.putString("Destination",destination.name());
        nbt.putDouble("RouteDistance",destination.distance);
        if(lunar!=null) {
            nbt.putString("LunarPhase",lunar.phase().name()); nbt.putInt("LunarTicks",lunar.ticks());
            nbt.putDouble("LunarX",lunar.x()); nbt.putDouble("LunarY",lunar.y()); nbt.putFloat("LunarTilt",lunar.tilt());
        }
    }
    @Override public boolean canHit() { return true; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean damage(DamageSource source, float amount) { return false; }
    @Override protected boolean canAddPassenger(Entity passenger) { return !hasPassengers() && passenger instanceof PlayerEntity; }
    @Override protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        // The pilot sits inside the pressurized upper-stage cabin at local Y 51.35.
        double a=Math.toRadians(getPitch()),offset=19.35;
        return new Vec3d(-offset*Math.sin(a),32+offset*Math.cos(a),.65);
    }

    public void beginMoonFlight() {
        dataTracker.set(MANUAL,false);
        lunar=LunarFlight.start(); lunarPaused=false; flightSpeed=0; flightTicks=0;
        dataTracker.set(ENGINES,false);
    }
    public void acceptLunarState(LunarFlight.State state) { lunar=state; lunarPaused=false; }
    public boolean canResumeMoon() {
        return lunar!=null && !LunarFlight.finished(lunar) && (getWorld().getRegistryKey().equals(MegaRockets.ORBIT)||getWorld().getRegistryKey().equals(MegaRockets.destinationWorld(destination)));
    }
    public Text missionStatus() {
        if(manualMode()){
            if(entryProgress()>=0)return Text.translatable("cockpit.megarockets.entry_loading",entryProgress());
            if(groundClearance()>=0)return Text.translatable("message.megarockets.descent_status",groundClearance(),(int)Math.round(flightSpeed*20),(int)getX(),(int)getZ());
            return Text.translatable("message.megarockets.manual_status",(int)getX(),(int)getY(),(int)getZ(),(int)Math.round(flightSpeed*20));
        }
        if(lunar!=null) {
            var phase=Text.translatable("message.megarockets.phase."+lunar.phase().name().toLowerCase(java.util.Locale.ROOT));
            int percent=(int)Math.floor(100*lunar.x()/destination.distance);
            return Text.translatable("message.megarockets.target_status",Text.translatable("destination.megarockets."+destination.key()),phase,percent,
                (int)Math.ceil(Math.max(0,destination.distance-lunar.x())),(int)Math.round(lunar.speed()*20),
                destination.orbitOnly()?0:(int)Math.ceil(Math.max(0,lunar.y()-LunarFlight.LAND_Y)));
        }
        if(moonLanded) return Text.translatable("message.megarockets.phase.landed");
        return Text.translatable("message.megarockets.altitude",(int)getY(),TARGET_ALTITUDE,(int)(flightSpeed*20));
    }
    public boolean resumeMoon(ServerPlayerEntity player) {
        if(!canResumeMoon() || (hasPassengers() && getFirstPassenger()!=player)) return false;
        if(player.getVehicle()!=this && !player.startRiding(this,true)) return false;
        lunarPaused=false; return true;
    }

    @Override public Vec3d updatePassengerForDismount(LivingEntity passenger) {
        if(manualLanded&&!getWorld().getRegistryKey().equals(MegaRockets.ORBIT)&&Math.abs(flightSpeed)<.05){
            for(int[] d:new int[][]{{10,0},{0,10},{-10,0},{0,-10}}){
                int x=(int)Math.floor(getX())+d[0],z=(int)Math.floor(getZ())+d[1];
                int y=getWorld().getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,x,z);
                if(Math.abs(y-getY())>3)continue;
                var base=new net.minecraft.util.math.BlockPos(x,y-1,z);
                if(!getWorld().getBlockState(base).isSolidBlock(getWorld(),base))continue;
                var exit=new net.minecraft.util.math.Box(x+.15,y,z+.15,x+.85,y+2,z+.85);
                if(getWorld().isSpaceEmpty(passenger,exit))return new Vec3d(x+.5,y,z+.5);
            }
        }
        if(getWorld().getRegistryKey().equals(MegaRockets.destinationWorld(destination))&&moonLanded){
            var site=site();
            int y=getWorld().getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING,site.exitX(),site.exitZ());
            if(Math.abs(y-site.y())<=2)return new Vec3d(site.exitX()+.5,y,site.exitZ()+.5);
        }
        if(getWorld().getRegistryKey().equals(MegaRockets.ORBIT) && Math.abs(getX()-MoonWorld.X-.5)<1
                && Math.abs(getY()-MoonWorld.LAND_Y)<.1 && Math.abs(getZ()-.5)<1)
            return new Vec3d(MoonWorld.X+16.5,MoonWorld.LAND_Y,.5);
        // A grounded rocket has an elevator exit at its base, not a 51-block fall.
        if(!flying&&countdown==0&&(lunar==null||moonLanded)&&!getWorld().getRegistryKey().equals(MegaRockets.ORBIT)){
            int x=(int)Math.floor(getX())+9,z=(int)Math.floor(getZ());
            int y=getWorld().getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,x,z);
            if(Math.abs(y-getY())<12)return new Vec3d(x+.5,y,z+.5);
        }
        return super.updatePassengerForDismount(passenger);
    }

    @Override public ActionResult interact(PlayerEntity player, Hand hand) {
        if (getWorld().isClient) return ActionResult.SUCCESS;
        if (player instanceof ServerPlayerEntity pilot && !pilot.isSpectator()) {
            if(pilot.isCreative()&&pilot.isSneaking()&&!hasPassengers()&&!flying&&countdown==0&&!canResumeMoon()){
                discard();
                pilot.sendMessage(Text.translatable("message.megarockets.rocket_removed"),true);
                return ActionResult.CONSUME;
            }
            if (canResumeMoon() ? resumeMoon(pilot) : pilot.startRiding(this)) {pilot.setYaw(getYaw()+180);pilot.setPitch(0);pilot.sendMessage(Text.translatable("message.megarockets.seated"), false);showDestinations(pilot);}
            return ActionResult.CONSUME;
        }
        return ActionResult.PASS;
    }

    public boolean beginLaunch(ServerPlayerEntity pilot) {
        if(manualMode()||!armed())return false;
        if (getWorld().getRegistryKey().equals(MegaRockets.ORBIT)||getWorld().getRegistryKey().equals(MegaRockets.destinationWorld(destination))) return resumeMoon(pilot);
        if (!getWorld().getRegistryKey().equals(World.OVERWORLD) || getY() >= TARGET_ALTITUDE || countdown > 0 || flying) return false;
        if (pilot.getVehicle() != this && !pilot.startRiding(this)) return false;
        countdown = 100;syncCabin();
        pilot.sendMessage(Text.translatable("message.megarockets.countdown", 5), true);
        return true;
    }

    @Override public void updateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int steps) {
        // Do not move the entity from the network handler between rendered frames.
        if (!getWorld().isClient || getPos().squaredDistanceTo(x,y,z)>Math.pow(maxSpeed()*4+16,2)) {
            super.updateTrackedPositionAndAngles(x,y,z,yaw,pitch,steps);
            lerpTicks=0;
            return;
        }
        lerpX=x; lerpY=y; lerpZ=z; lerpYaw=yaw; lerpPitch=pitch;
        lerpTicks=getType()==MegaRockets.HYPERION?5:getType()==MegaRockets.PHANTOM?4:3;
    }

    @Override public double getLerpTargetX() { return lerpTicks>0 ? lerpX : getX(); }
    @Override public double getLerpTargetY() { return lerpTicks>0 ? lerpY : getY(); }
    @Override public double getLerpTargetZ() { return lerpTicks>0 ? lerpZ : getZ(); }
    @Override public float getLerpTargetYaw() { return lerpTicks>0 ? lerpYaw : getYaw(); }
    @Override public float getLerpTargetPitch() { return lerpTicks>0 ? lerpPitch : getPitch(); }

    private void interpolateClient() {
        if (lerpTicks<=0) return;
        double f=1.0/lerpTicks;
        setPosition(getX()+(lerpX-getX())*f, getY()+(lerpY-getY())*f, getZ()+(lerpZ-getZ())*f);
        setYaw(getYaw()+net.minecraft.util.math.MathHelper.wrapDegrees(lerpYaw-getYaw())*(float)f);
        setPitch(getPitch()+(lerpPitch-getPitch())*(float)f);
        lerpTicks--;
    }

    private void stopEngines() {
        countdown=0; flightTicks=0; flying=false; flightSpeed=0;
        dataTracker.set(ENGINES,false);
        setVelocity(Vec3d.ZERO);
    }

    @Override public void tick() {
        super.tick();
        if (getWorld().isClient) { interpolateClient(); return; }
        if (!(getWorld() instanceof ServerWorld world)) return;
        tickCabinSystems(world);
        if(manualMode()){tickManual(world);syncCabin();return;}
        syncCabin();
        if (lunar!=null && !LunarFlight.finished(lunar)) { tickMoon(world); return; }
        if (countdown == 0 && !flying) return;
        if (!(getFirstPassenger() instanceof ServerPlayerEntity pilot)) {
            stopEngines(); return;
        }
        if (countdown > 0) {
            countdown--;
            if (countdown > 0 && countdown % 20 == 0)
                pilot.sendMessage(Text.translatable("message.megarockets.countdown", countdown/20), true);
            if (countdown == 0) { flying = true; flightSpeed=0; dataTracker.set(ENGINES,true); pilot.sendMessage(Text.translatable("message.megarockets.liftoff"), true); }
            return;
        }
        flightTicks++;
        flightSpeed=FlightProfile.nextSpeed(getY(), flightSpeed, TARGET_ALTITUDE,maxSpeed());
        double speed=flightSpeed;
        // Server-authoritative motion; absolute position packets are interpolated client-side.
        setPosition(getX(), Math.min(TARGET_ALTITUDE, getY()+speed), getZ());
        pilot.fallDistance = 0;
        // Only supplemental ground smoke; the main flames are attached to the client model.
        if (getY()<320 && flightTicks%4==0)
            world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(),getY()-1,getZ(),10,2,.6,2,.03);
        if (flightTicks % 20 == 0) pilot.sendMessage(Text.translatable("message.megarockets.altitude", (int)getY(), TARGET_ALTITUDE, (int)(speed*20)), true);
        if (getY() >= TARGET_ALTITUDE) {
            pilot.sendMessage(Text.translatable("message.megarockets.target_reached", TARGET_ALTITUDE), false);
            stopEngines();
            if (!MegaRockets.arrive(this, pilot)) pilot.sendMessage(Text.translatable("message.megarockets.failed"), false);
        }
    }
    public boolean toggleManual(ServerPlayerEntity pilot){
        if(getFirstPassenger()!=pilot||pilot.isSpectator())return false;
        if(autoNavigation()){hold();return true;}
        boolean enabled=!manualMode();clearEntry();
        if(enabled&&!getWorld().getRegistryKey().equals(World.OVERWORLD)&&!getWorld().getRegistryKey().equals(MegaRockets.ORBIT)&&MegaRockets.planet(getWorld())==null)return false;
        // Taking over ends the previous automatic mission; position is never rewound.
        stopEngines();lunar=null;lunarPaused=false;moonLanded=false;landingSite=null;
        manualVelocity=ManualFlight.STOP;manualMask=0;lastManualAge=-1000;
        dataTracker.set(NAV,false);dataTracker.set(MANUAL,enabled);setPitch(0);syncCabin();
        pilot.sendMessage(Text.translatable(enabled?"message.megarockets.manual_on":"message.megarockets.manual_off"),false);
        return true;
    }
    public void startManual(){
        manualLanded=false;clearEntry();
        stopEngines();lunar=null;moonLanded=false;manualVelocity=ManualFlight.STOP;manualMask=0;dataTracker.set(NAV,false);dataTracker.set(MANUAL,true);
    }
    public void manualInput(ServerPlayerEntity player,int mask){
        if(manualMode()&&!autoNavigation()&&getFirstPassenger()==player&&!player.isSpectator()){
            manualMask=mask&ManualFlight.VALID;lastManualAge=age;
        }
    }
    public boolean requestApproach(ServerPlayerEntity pilot){
        if(!manualMode()||!armed()||getFirstPassenger()!=pilot||!getWorld().getRegistryKey().equals(MegaRockets.ORBIT))return false;
        double dx=getX()-destination.distance,dz=getZ()-destination.lane;
        if(dx*dx+dz*dz>2000*2000||manualVelocity.length()>1){
            pilot.sendMessage(Text.translatable("message.megarockets.approach_range"),false);return false;
        }
        if(destination.orbitOnly()){
            dataTracker.set(NAV,false);manualVelocity=ManualFlight.STOP;manualMask=0;
            pilot.sendMessage(Text.translatable("message.megarockets.giant_arrived",Text.translatable("destination.megarockets."+destination.key())),false);
            return true;
        }
        stopEngines();dataTracker.set(NAV,false);dataTracker.set(MANUAL,false);manualVelocity=ManualFlight.STOP;manualMask=0;landingCandidate=0;
        lunar=new LunarFlight.State(LunarFlight.Phase.DESCENT,0,destination.distance,LunarFlight.CRUISE_Y,0,0);
        pilot.sendMessage(Text.translatable("message.megarockets.scanning_landing"),false);
        return true;
    }
    private void tickCabinSystems(ServerWorld world){
        if(age%20!=0)return;
        boolean earthAir=world.getRegistryKey().equals(World.OVERWORLD)&&getY()+51.35<OxygenRules.ALTITUDE;
        dataTracker.set(PRESSURE,CabinSystems.pressure(cabinPressure(),lifeSupport(),earthAir));
        dataTracker.set(HEAT,CabinSystems.heat(engineHeat(),enginesRunning(),manualMode()?throttle():100));
    }
    private void hold(){
        clearEntry();
        stopEngines();lunar=null;lunarPaused=false;moonLanded=false;landingSite=null;manualVelocity=ManualFlight.STOP;
        manualMask=0;lastManualAge=-1000;dataTracker.set(NAV,false);dataTracker.set(MANUAL,true);setPitch(0);syncCabin();
    }
    public void cockpitAction(ServerPlayerEntity pilot,CockpitAction.Control control){
        if(getFirstPassenger()!=pilot||pilot.isSpectator())return;
        boolean ok=true;
        switch(control){
            case MANUAL -> ok=toggleManual(pilot);
            case LAUNCH -> ok=beginLaunch(pilot);
            case APPROACH -> ok=requestApproach(pilot);
            case HOLD -> hold();
            case ARM -> {if(armed()){if(flying||countdown>0||manualMode()||lunar!=null&&!LunarFlight.finished(lunar))hold();dataTracker.set(ARMED,false);}else dataTracker.set(ARMED,true);}
            case LIGHTS -> dataTracker.set(LIGHTS,!cabinLights());
            case LIFE -> dataTracker.set(LIFE,!lifeSupport());
            case BEACON -> dataTracker.set(BEACON,!beacon());
            case THROTTLE_DOWN -> dataTracker.set(THROTTLE,CabinSystems.throttle(throttle(),-25));
            case THROTTLE_UP -> dataTracker.set(THROTTLE,CabinSystems.throttle(throttle(),25));
            case AUTO_NAV -> {
                if(!armed()||!getWorld().getRegistryKey().equals(MegaRockets.ORBIT))ok=false;
                else{hold();dataTracker.set(NAV,true);}
            }
        }
        if(!ok)pilot.sendMessage(Text.translatable("cockpit.megarockets.action_unavailable"),true);
    }
    private void clearEntry(){entry=null;dataTracker.set(ENTRY_PROGRESS,-1);}
    public void setEntryDestination(Destination body){
        if(body!=null){destination=body;dataTracker.set(CABIN_TARGET,body.ordinal());}
    }
    private boolean tickEntry(ServerPlayerEntity pilot,int mask){
        if(!armed()||autoNavigation()){clearEntry();return false;}
        var gate=ManualEntry.nearest(getX(),getZ());
        if(gate==null){clearEntry();return false;}
        if(entry==null||!entry.gate.equals(gate)){
            var target=gate.world(pilot);if(target==null)return false;
            entry=new ManualEntry(gate,target,getX(),getZ());
            pilot.sendMessage(Text.translatable("message.megarockets.preloading",Text.translatable("destination.megarockets."+gate.key())),false);
        }
        entry.step();dataTracker.set(ENTRY_PROGRESS,entry.progress());
        if(ApproachRules.distance(getX(),getZ(),gate.x(),gate.z())>ApproachRules.ENTRY_RADIUS||(mask&ManualFlight.BRAKE)!=0)return false;
        // Hold at the edge until the local patch exists; B delays entry.
        manualVelocity=ManualFlight.STOP;flightSpeed=0;dataTracker.set(ENGINES,false);
        if(entry.ready()){
            if(entry.transfer(this,pilot))return true;
            pilot.sendMessage(Text.translatable("message.megarockets.entry_blocked"),true);
            clearEntry();return false;
        }
        if(age%10==0)pilot.sendMessage(Text.translatable("cockpit.megarockets.entry_loading",entry.progress()),true);
        return true;
    }
    private double surfaceClearance(ServerWorld world){
        int max=world.getBottomY();
        for(int dx:new int[]{-6,0,6})for(int dz:new int[]{-6,0,6}){
            int x=(int)Math.floor(getX())+dx,z=(int)Math.floor(getZ())+dz;
            max=Math.max(max,world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING,x,z));
        }
        return Math.max(0,getY()-max);
    }
    private boolean stableTouchdown(ServerWorld world){
        // Require a solid supporting center and a reasonably level footprint.
        int supports=0;
        for(int[] d:new int[][]{{0,0},{-4,-4},{-4,4},{4,-4},{4,4}}){
            int x=(int)Math.floor(getX())+d[0],z=(int)Math.floor(getZ())+d[1];
            int y=world.getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,x,z);
            if(y>getY()+.1||getY()-y>1.1)continue;
            var pos=new net.minecraft.util.math.BlockPos(x,y-1,z);
            if(world.getBlockState(pos).isSolidBlock(world,pos))supports++;
        }
        return supports>=3&&!world.isSpaceEmpty(this,getBoundingBox().offset(0,-.12,0));
    }
    private void tickManual(ServerWorld world){
        if(!(getFirstPassenger() instanceof ServerPlayerEntity pilot)){
            clearEntry();dataTracker.set(NAV,false);manualVelocity=ManualFlight.STOP;manualMask=0;flightSpeed=0;dataTracker.set(ENGINES,false);return;
        }
        boolean orbit=world.getRegistryKey().equals(MegaRockets.ORBIT),earth=world.getRegistryKey().equals(World.OVERWORLD);
        int mask=!armed()||age-lastManualAge>6?0:manualMask;
        double agl=orbit?-1:surfaceClearance(world);
        dataTracker.set(GROUND_CLEARANCE,orbit?-1:(int)Math.floor(agl));
        if(!orbit)clearEntry();
        double cap=orbit?maxSpeed():ApproachRules.surfaceCap(agl);
        cap*=throttle()/100.0;
        double accel=orbit?.5:.12;
        setPitch(0);
        if(autoNavigation()&&armed()&&orbit){
            double dx=destination.distance+.5-getX(),dy=LunarFlight.CRUISE_Y-getY(),dz=destination.lane+.5-getZ();
            double distance=Math.sqrt(dx*dx+dy*dy+dz*dz);
            if(distance<1&&manualVelocity.length()<1){
                manualVelocity=ManualFlight.STOP;dataTracker.set(NAV,false);requestApproach(pilot);return;
            }
            double goal=Math.min(cap,Math.min(distance,Math.sqrt(distance*accel)));
            manualVelocity=ManualFlight.toward(manualVelocity,distance<1e-8?ManualFlight.STOP:new ManualFlight.Motion(dx/distance*goal,dy/distance*goal,dz/distance*goal),accel);
            float heading=(float)Math.toDegrees(Math.atan2(-dx,dz))+180;
            setYaw(getYaw()+Math.max(-4,Math.min(4,net.minecraft.util.math.MathHelper.wrapDegrees(heading-getYaw()))));
        }else{
            setYaw(getYaw()+Math.max(-4,Math.min(4,net.minecraft.util.math.MathHelper.wrapDegrees(pilot.getYaw()+180-getYaw()))));
            manualVelocity=ManualFlight.step(manualVelocity,mask,pilot.getYaw(),cap,accel);
        }
        if(!orbit&&manualVelocity.y()<0)manualVelocity=new ManualFlight.Motion(manualVelocity.x(),Math.max(-ApproachRules.descentCap(agl),manualVelocity.y()),manualVelocity.z());
        Vec3d delta=new Vec3d(manualVelocity.x(),manualVelocity.y(),manualVelocity.z());
        double ceiling=orbit?1024:earth?TARGET_ALTITUDE:world.getTopY()+160;
        double floor=world.getBottomY()+1;
        delta=new Vec3d(delta.x,Math.max(floor-getY(),Math.min(ceiling-getY(),delta.y)),delta.z);
        // Sweep the entire upright hull: do not tunnel through a single block at high speed.
        var box=getBoundingBox();
        if(delta.lengthSquared()>1e-12){
            var swept=box.stretch(delta);
            if(world.getWorldBorder().contains(swept)&&world.isSpaceEmpty(this,swept)){
                setPosition(getPos().add(delta));
            }else{
                // Advance only the collision-free fraction; stop rather than break blocks.
                double lo=0,hi=1;
                for(int i=0;i<9;i++){
                    double mid=(lo+hi)*.5;var test=box.stretch(delta.multiply(mid));
                    if(world.getWorldBorder().contains(test)&&world.isSpaceEmpty(this,test))lo=mid;else hi=mid;
                }
                if(lo>0)setPosition(getPos().add(delta.multiply(lo)));
                manualVelocity=ManualFlight.STOP;dataTracker.set(NAV,false);
            }
        }
        if(!orbit){
            boolean landed=stableTouchdown(world)&&manualVelocity.length()<.06;
            if(landed&&!manualLanded)pilot.sendMessage(Text.translatable("message.megarockets.manual_touchdown"),false);
            manualLanded=landed;
        }else manualLanded=false;
        flightSpeed=manualVelocity.length();
        dataTracker.set(ENGINES,armed()&&flightSpeed>.025&&(autoNavigation()||(mask&ManualFlight.BRAKE)==0&&(mask&63)!=0));
        pilot.fallDistance=0;
        if(orbit&&tickEntry(pilot,mask))return;
        if(age%20==0)pilot.sendMessage(missionStatus(),true);
        if(!orbit&&getY()>=ceiling-.01&&(mask&ManualFlight.UP)!=0)MegaRockets.manualOrbit(this,pilot);
    }
    private void tickMoon(ServerWorld world) {
        if(!(getFirstPassenger() instanceof ServerPlayerEntity pilot)) {
            if(!lunarPaused) { lunar=LunarFlight.pause(lunar,destination.distance,maxSpeed()); lunarPaused=true; }
            dataTracker.set(ENGINES,false); return;
        }
        lunarPaused=false;
        boolean onMoon=!destination.orbitOnly()&&world.getRegistryKey().equals(MegaRockets.destinationWorld(destination));
        pilot.fallDistance=0;
        var previous=lunar.phase();
        // Transfer only once the interplanetary path and alignment have finished.
        if(!destination.orbitOnly()&&!onMoon&&(lunar.phase()==LunarFlight.Phase.DESCENT||lunar.phase()==LunarFlight.Phase.FINAL)) {
            MegaRockets.enterMoon(this,pilot,lunar);
            flightTicks++;
            return;
        }
        var next=LunarFlight.step(lunar,destination.orbitOnly()||onMoon||pilot.getServer().getWorld(MegaRockets.destinationWorld(destination))!=null,destination.distance,maxSpeed(),destination.orbitOnly());
        if(onMoon){
            var site=site();
            double nextY=next.y()+site.y()-LunarFlight.LAND_Y;
            if(!world.isSpaceEmpty(this,getBoundingBox().offset(0,nextY-getY(),0))){
                dataTracker.set(ENGINES,false);flightSpeed=0;
                if(age%40==0)pilot.sendMessage(Text.translatable("message.megarockets.landing_obstructed"),true);
                return;
            }
        }
        lunar=next;
        if(onMoon) setPosition(site().x()+.5,lunar.y()+site().y()-LunarFlight.LAND_Y,site().z()+.5);
        else setPosition(lunar.x()+.5,lunar.y(),destination.lane+.5);
        setPitch(lunar.tilt());
        flightSpeed=lunar.speed();
        dataTracker.set(ENGINES,LunarFlight.powered(lunar));
        flightTicks++;
        if(previous!=lunar.phase()) pilot.sendMessage(
            Text.translatable("message.megarockets.phase."+lunar.phase().name().toLowerCase(java.util.Locale.ROOT)),false);
        if(flightTicks%20==0) pilot.sendMessage(missionStatus(),true);
        if((lunar.phase()==LunarFlight.Phase.DESCENT||lunar.phase()==LunarFlight.Phase.FINAL)&&lunar.y()<MoonWorld.LAND_Y+18&&flightTicks%4==0)
            world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,onMoon?site().x()+.5:MoonWorld.X+.5,onMoon?site().y()+.1:MoonWorld.LAND_Y+.1,onMoon?site().z()+.5:.5,20,5,.35,5,.035);
        if(lunar.phase()==LunarFlight.Phase.ORBIT_ARRIVAL){
            stopEngines();setPitch(0);syncCabin();
            pilot.sendMessage(Text.translatable("message.megarockets.giant_arrived",Text.translatable("destination.megarockets."+destination.key())),false);
        }
        if(lunar.phase()==LunarFlight.Phase.LANDED) {
            moonLanded=true; stopEngines(); setPitch(0);
            pilot.sendMessage(Text.translatable("message.megarockets.target_arrived",Text.translatable("destination.megarockets."+destination.key())),false);
        }
    }
}
