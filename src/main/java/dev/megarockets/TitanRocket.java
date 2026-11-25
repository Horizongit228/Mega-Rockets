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

    @Override protected void initDataTracker(DataTracker.Builder builder) { builder.add(ENGINES, false); }
    public boolean enginesRunning() { return dataTracker.get(ENGINES); }
    @Override protected void readCustomDataFromNbt(NbtCompound nbt) {
        countdown=0; flightTicks=0; flying=false; flightSpeed=0; dataTracker.set(ENGINES,false);
        moonLanded=nbt.getBoolean("MoonLanded");
        if(nbt.contains("LunarPhase")) {
            try {
                var phase=LunarFlight.Phase.valueOf(nbt.getString("LunarPhase"));
                lunar=LunarFlight.pause(new LunarFlight.State(phase,nbt.getInt("LunarTicks"),nbt.getDouble("LunarX"),
                    nbt.getDouble("LunarY"),0,nbt.getFloat("LunarTilt")));
                lunarPaused=true;
            } catch(IllegalArgumentException ignored) { lunar=null; }
        }
    }
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putBoolean("MoonLanded",moonLanded);
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
        // External observation seat near the upper stage, not a walkable interior.
        // Same pivot and rotation as the client model; the passenger stays on the upper deck.
        double a=Math.toRadians(getPitch()), offset=21;
        return new Vec3d(-offset*Math.sin(a),32+offset*Math.cos(a),-4.7);
    }

    public void beginMoonFlight() {
        lunar=LunarFlight.start(); lunarPaused=false; flightSpeed=0; flightTicks=0;
        dataTracker.set(ENGINES,false);
    }
    public void acceptLunarState(LunarFlight.State state) { lunar=state; lunarPaused=false; }
    public boolean canResumeMoon() {
        return lunar!=null && lunar.phase()!=LunarFlight.Phase.LANDED && (getWorld().getRegistryKey().equals(MegaRockets.ORBIT)||getWorld().getRegistryKey().equals(MegaRockets.MOON));
    }
    public Text missionStatus() {
        if(lunar!=null) {
            var phase=Text.translatable("message.megarockets.phase."+lunar.phase().name().toLowerCase(java.util.Locale.ROOT));
            int percent=(int)Math.floor(100*lunar.x()/LunarFlight.DISTANCE);
            return Text.translatable("message.megarockets.mission_status",phase,percent,
                (int)Math.ceil(Math.max(0,LunarFlight.DISTANCE-lunar.x())),(int)Math.round(lunar.speed()*20),
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
        if(getWorld().getRegistryKey().equals(MegaRockets.MOON)&&moonLanded)
            return new Vec3d(16.5,getWorld().getTopY(net.minecraft.world.Heightmap.Type.MOTION_BLOCKING,16,0),.5);
        if(getWorld().getRegistryKey().equals(MegaRockets.ORBIT) && Math.abs(getX()-MoonWorld.X-.5)<1
                && Math.abs(getY()-MoonWorld.LAND_Y)<.1 && Math.abs(getZ()-.5)<1)
            return new Vec3d(MoonWorld.X+16.5,MoonWorld.LAND_Y,.5);
        return super.updatePassengerForDismount(passenger);
    }

    @Override public ActionResult interact(PlayerEntity player, Hand hand) {
        if (!getWorld().getRegistryKey().equals(World.OVERWORLD) && !getWorld().getRegistryKey().equals(MegaRockets.ORBIT) && !getWorld().getRegistryKey().equals(MegaRockets.MOON)) return ActionResult.PASS;
        if (getWorld().isClient) return ActionResult.SUCCESS;
        if (player instanceof ServerPlayerEntity pilot && pilot.hasPermissionLevel(2)) {
            if (canResumeMoon() ? resumeMoon(pilot) : pilot.startRiding(this)) pilot.sendMessage(Text.translatable("message.megarockets.seated"), false);
            return ActionResult.CONSUME;
        }
        return ActionResult.PASS;
    }

    public boolean beginLaunch(ServerPlayerEntity pilot) {
        if (getWorld().getRegistryKey().equals(MegaRockets.ORBIT)||getWorld().getRegistryKey().equals(MegaRockets.MOON)) return resumeMoon(pilot);
        if (!getWorld().getRegistryKey().equals(World.OVERWORLD) || getY() >= TARGET_ALTITUDE || countdown > 0 || flying) return false;
        if (pilot.getVehicle() != this && !pilot.startRiding(this)) return false;
        countdown = 100;
        pilot.sendMessage(Text.translatable("message.megarockets.countdown", 5), true);
        return true;
    }

    @Override public void updateTrackedPositionAndAngles(double x, double y, double z, float yaw, float pitch, int steps) {
        // Do not move the entity from the network handler between rendered frames.
        if (!getWorld().isClient || getPos().squaredDistanceTo(x,y,z)>64*64) {
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
        flightSpeed=FlightProfile.nextSpeed(getY(), flightSpeed, TARGET_ALTITUDE);
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
            if(!lunarPaused) { lunar=LunarFlight.pause(lunar); lunarPaused=true; }
            dataTracker.set(ENGINES,false); return;
        }
        lunarPaused=false;
        boolean onMoon=world.getRegistryKey().equals(MegaRockets.MOON);
        pilot.fallDistance=0;
        var previous=lunar.phase();
        // Transfer only once the interplanetary path and alignment have finished.
        if(!onMoon&&(lunar.phase()==LunarFlight.Phase.DESCENT||lunar.phase()==LunarFlight.Phase.FINAL)) {
            MegaRockets.enterMoon(this,pilot,lunar);
            flightTicks++;
            return;
        }
        lunar=LunarFlight.step(lunar,onMoon||pilot.getServer().getWorld(MegaRockets.MOON)!=null);
        if(onMoon) setPosition(.5,lunar.y()+MoonTerrain.PAD_Y-LunarFlight.LAND_Y,.5);
        else setPosition(lunar.x()+.5,lunar.y(),.5);
        setPitch(lunar.tilt());
        flightSpeed=lunar.speed();
        dataTracker.set(ENGINES,LunarFlight.powered(lunar));
        flightTicks++;
        if(previous!=lunar.phase()) pilot.sendMessage(
            Text.translatable("message.megarockets.phase."+lunar.phase().name().toLowerCase(java.util.Locale.ROOT)),false);
        if(flightTicks%10==0) pilot.sendMessage(missionStatus(),true);
        if((lunar.phase()==LunarFlight.Phase.DESCENT||lunar.phase()==LunarFlight.Phase.FINAL)&&lunar.y()<MoonWorld.LAND_Y+18&&flightTicks%4==0)
            world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,onMoon?.5:MoonWorld.X+.5,onMoon?MoonTerrain.PAD_Y+.1:MoonWorld.LAND_Y+.1,.5,20,5,.35,5,.035);
        if(lunar.phase()==LunarFlight.Phase.LANDED) {
            moonLanded=true; stopEngines(); setPitch(0);
            pilot.sendMessage(Text.translatable("message.megarockets.moon_arrived"),false);
        }
    }
}
