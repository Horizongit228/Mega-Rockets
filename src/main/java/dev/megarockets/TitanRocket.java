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
    private Destination destination=Destination.MOON;
    private int countdown;
    private LunarFlight.State lunar;
    private boolean lunarPaused;
    private boolean moonLanded;
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
    public Destination destination(){return destination;}
    public void copyDestination(TitanRocket old){destination=old.destination;dataTracker.set(CABIN_TARGET,destination.ordinal());}
    public boolean selectDestination(Destination value){
        if(countdown>0||flying||lunar!=null||!getWorld().getRegistryKey().equals(World.OVERWORLD))return false;
        destination=value;dataTracker.set(CABIN_TARGET,value.ordinal());return true;
    }
    public void showDestinations(ServerPlayerEntity pilot){
        if(!getWorld().getRegistryKey().equals(World.OVERWORLD)||countdown>0||flying||lunar!=null)return;
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
    @Override protected void initDataTracker(DataTracker.Builder builder) { builder.add(ENGINES, false);builder.add(CABIN_TARGET,0);builder.add(CABIN_SPEED,0);builder.add(CABIN_PHASE,-1);builder.add(CABIN_PROGRESS,0); }
    public Destination cabinTarget(){return Destination.values()[Math.max(0,Math.min(Destination.values().length-1,dataTracker.get(CABIN_TARGET)))];}
    public int cabinSpeed(){return dataTracker.get(CABIN_SPEED);}
    public int cabinProgress(){return dataTracker.get(CABIN_PROGRESS);}
    public boolean cabinReady(){return dataTracker.get(CABIN_PHASE)==-1&&getWorld().getRegistryKey().equals(World.OVERWORLD);}
    public Text cabinPhase(){
        int phase=dataTracker.get(CABIN_PHASE);
        if(phase==-1)return Text.translatable("cockpit.megarockets.ready");
        if(phase==-2)return Text.translatable("cockpit.megarockets.countdown");
        if(phase==-3)return Text.translatable("cockpit.megarockets.ascent");
        return Text.translatable("message.megarockets.phase."+LunarFlight.Phase.values()[Math.min(phase,LunarFlight.Phase.values().length-1)].name().toLowerCase(java.util.Locale.ROOT));
    }
    private void syncCabin(){
        dataTracker.set(CABIN_SPEED,(int)Math.round(flightSpeed*20));
        dataTracker.set(CABIN_PHASE,lunar!=null?lunar.phase().ordinal():countdown>0?-2:flying?-3:-1);
        dataTracker.set(CABIN_PROGRESS,lunar!=null?(int)Math.min(100,100*lunar.x()/destination.distance):flying?(int)Math.min(100,getY()/TARGET_ALTITUDE*100):0);
    }
    public boolean enginesRunning() { return dataTracker.get(ENGINES); }
    @Override protected void readCustomDataFromNbt(NbtCompound nbt) {
        countdown=0; flightTicks=0; flying=false; flightSpeed=0; dataTracker.set(ENGINES,false);
        moonLanded=nbt.getBoolean("MoonLanded");
        destination=Destination.read(nbt.getString("Destination"));dataTracker.set(CABIN_TARGET,destination.ordinal());
        if(nbt.contains("LunarPhase")) {
            try {
                var phase=LunarFlight.Phase.valueOf(nbt.getString("LunarPhase"));
                lunar=LunarFlight.pause(new LunarFlight.State(phase,nbt.getInt("LunarTicks"),nbt.getDouble("LunarX"),
                    nbt.getDouble("LunarY"),0,nbt.getFloat("LunarTilt")),destination.distance,maxSpeed());
                lunarPaused=true;
            } catch(IllegalArgumentException ignored) { lunar=null; }
        }
    }
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putBoolean("MoonLanded",moonLanded);
        nbt.putString("Destination",destination.name());
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
        lunar=LunarFlight.start(); lunarPaused=false; flightSpeed=0; flightTicks=0;
        dataTracker.set(ENGINES,false);
    }
    public void acceptLunarState(LunarFlight.State state) { lunar=state; lunarPaused=false; }
    public boolean canResumeMoon() {
        return lunar!=null && lunar.phase()!=LunarFlight.Phase.LANDED && (getWorld().getRegistryKey().equals(MegaRockets.ORBIT)||getWorld().getRegistryKey().equals(MegaRockets.destinationWorld(destination)));
    }
    public Text missionStatus() {
        if(lunar!=null) {
            var phase=Text.translatable("message.megarockets.phase."+lunar.phase().name().toLowerCase(java.util.Locale.ROOT));
            int percent=(int)Math.floor(100*lunar.x()/destination.distance);
            return Text.translatable("message.megarockets.target_status",Text.translatable("destination.megarockets."+destination.key()),phase,percent,
                (int)Math.ceil(Math.max(0,destination.distance-lunar.x())),(int)Math.round(lunar.speed()*20),
                (int)Math.ceil(Math.max(0,lunar.y()-LunarFlight.LAND_Y)));
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
        if(getWorld().getRegistryKey().equals(MegaRockets.destinationWorld(destination))&&moonLanded)
            return new Vec3d(16.5,getWorld().getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING,16,0),.5);
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
            if (canResumeMoon() ? resumeMoon(pilot) : pilot.startRiding(this)) {pilot.sendMessage(Text.translatable("message.megarockets.seated"), false);showDestinations(pilot);}
            return ActionResult.CONSUME;
        }
        return ActionResult.PASS;
    }

    public boolean beginLaunch(ServerPlayerEntity pilot) {
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
        lerpTicks=2;
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
        syncCabin();
        if (lunar!=null && lunar.phase()!=LunarFlight.Phase.LANDED) { tickMoon(world); return; }
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
    private void tickMoon(ServerWorld world) {
        if(!(getFirstPassenger() instanceof ServerPlayerEntity pilot)) {
            if(!lunarPaused) { lunar=LunarFlight.pause(lunar,destination.distance,maxSpeed()); lunarPaused=true; }
            dataTracker.set(ENGINES,false); return;
        }
        lunarPaused=false;
        boolean onMoon=world.getRegistryKey().equals(MegaRockets.destinationWorld(destination));
        pilot.fallDistance=0;
        var previous=lunar.phase();
        // Transfer only once the interplanetary path and alignment have finished.
        if(!onMoon&&(lunar.phase()==LunarFlight.Phase.DESCENT||lunar.phase()==LunarFlight.Phase.FINAL)) {
            MegaRockets.enterMoon(this,pilot,lunar);
            flightTicks++;
            return;
        }
        lunar=LunarFlight.step(lunar,onMoon||pilot.getServer().getWorld(MegaRockets.destinationWorld(destination))!=null,destination.distance,maxSpeed());
        if(onMoon) setPosition(.5,lunar.y()+destination.padY-LunarFlight.LAND_Y,.5);
        else setPosition(lunar.x()+.5,lunar.y(),destination.lane+.5);
        setPitch(lunar.tilt());
        flightSpeed=lunar.speed();
        dataTracker.set(ENGINES,LunarFlight.powered(lunar));
        flightTicks++;
        if(previous!=lunar.phase()) pilot.sendMessage(
            Text.translatable("message.megarockets.phase."+lunar.phase().name().toLowerCase(java.util.Locale.ROOT)),false);
        if(flightTicks%10==0) pilot.sendMessage(missionStatus(),true);
        if((lunar.phase()==LunarFlight.Phase.DESCENT||lunar.phase()==LunarFlight.Phase.FINAL)&&lunar.y()<MoonWorld.LAND_Y+18&&flightTicks%4==0)
            world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,onMoon?.5:MoonWorld.X+.5,onMoon?destination.padY+.1:MoonWorld.LAND_Y+.1,.5,20,5,.35,5,.035);
        if(lunar.phase()==LunarFlight.Phase.LANDED) {
            moonLanded=true; stopEngines(); setPitch(0);
            pilot.sendMessage(Text.translatable("message.megarockets.target_arrived",Text.translatable("destination.megarockets."+destination.key())),false);
        }
    }
}
