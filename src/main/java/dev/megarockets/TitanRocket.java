package dev.megarockets;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
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
    private int countdown;
    private int flightTicks;
    private boolean flying;

    public TitanRocket(EntityType<? extends TitanRocket> type, World world) {
        super(type, world);
        setNoGravity(true);
    }

    @Override protected void initDataTracker(DataTracker.Builder builder) {}
    @Override protected void readCustomDataFromNbt(NbtCompound nbt) {
        countdown = 0; flightTicks = 0; flying = false; // No unattended flight after reload.
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
        if (!getWorld().getRegistryKey().equals(World.OVERWORLD) || getY() > 250 || countdown > 0 || flying) return false;
        if (pilot.getVehicle() != this && !pilot.startRiding(this)) return false;
        countdown = 100;
        pilot.sendMessage(Text.translatable("message.megarockets.countdown", 5), true);
        return true;
    }

    @Override public void tick() {
        super.tick();
        if (!(getWorld() instanceof ServerWorld world)) return;
        if (countdown == 0 && !flying) return;
        if (!(getFirstPassenger() instanceof ServerPlayerEntity pilot)) {
            countdown = 0; flightTicks = 0; flying = false; setVelocity(Vec3d.ZERO); return;
        }
        if (countdown > 0) {
            countdown--;
            if (countdown > 0 && countdown % 20 == 0)
                pilot.sendMessage(Text.translatable("message.megarockets.countdown", countdown/20), true);
            if (countdown == 0) { flying = true; pilot.sendMessage(Text.translatable("message.megarockets.liftoff"), true); }
            return;
        }
        flightTicks++;
        double speed = Math.min(1.8, .06 + flightTicks*.017);
        // Server-authoritative scripted flight; not a physical thrust simulation.
        setPosition(getX(), Math.min(250, getY()+speed), getZ());
        pilot.fallDistance = 0;
        world.spawnParticles(ParticleTypes.FLAME, getX(), getY()-.35, getZ(), 28, 2.2, .45, 2.2, .12);
        if (flightTicks % 3 == 0)
            world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY()-1, getZ(), 8, 2, .5, 2, .02);
        if (flightTicks % 20 == 0) pilot.sendMessage(Text.translatable("message.megarockets.altitude", (int)getY()), true);
        if (getY() >= 250) {
            flying = false; flightTicks = 0;
            if (!MegaRockets.arrive(this, pilot)) pilot.sendMessage(Text.translatable("message.megarockets.failed"), false);
        }
    }
}
