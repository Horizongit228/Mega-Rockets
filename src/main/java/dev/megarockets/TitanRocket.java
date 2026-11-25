package dev.megarockets;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
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
        countdown = 0; flightTicks = 0; flying = false; flightSpeed = 0; dataTracker.set(ENGINES, false); // No unattended flight after reload.
    }
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) {}
    @Override public boolean canHit() { return true; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean damage(DamageSource source, float amount) { return false; }
    @Override protected boolean canAddPassenger(Entity passenger) { return !hasPassengers() && passenger instanceof PlayerEntity; }
    @Override protected Vec3d getPassengerAttachmentPos(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        // External observation seat near the upper stage, not a walkable interior.
        return new Vec3d(0, 53, -4.7);
    }

    @Override public ActionResult interact(PlayerEntity player, Hand hand) {
        if (!getWorld().getRegistryKey().equals(World.OVERWORLD)) return ActionResult.PASS;
        if (getWorld().isClient) return ActionResult.SUCCESS;
        if (player instanceof ServerPlayerEntity pilot && pilot.hasPermissionLevel(2)) {
            if (pilot.startRiding(this)) pilot.sendMessage(Text.translatable("message.megarockets.seated"), false);
            return ActionResult.CONSUME;
        }
        return ActionResult.PASS;
    }

    public boolean beginLaunch(ServerPlayerEntity pilot) {
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
        lerpTicks=3;
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
        // Keep server authoritative and keep per-tick movement below the relative packet limit.
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
}
