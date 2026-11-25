package dev.megarockets;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import java.util.Comparator;
import java.util.Set;

public final class MegaRockets implements ModInitializer {
    public static final String ID = "megarockets";
    public static final RegistryKey<World> ORBIT = RegistryKey.of(RegistryKeys.WORLD, id("orbit"));
    public static final EntityType<TitanRocket> TITAN = Registry.register(Registries.ENTITY_TYPE,
        id("titan_1"), EntityType.Builder.<TitanRocket>create(TitanRocket::new, SpawnGroup.MISC)
            .dimensions(14f, 64f).maxTrackingRange(16).trackingTickInterval(1).build("megarockets:titan_1"));

    public static Identifier id(String path) { return Identifier.of(ID, path); }

    @Override public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) ->
            dispatcher.register(CommandManager.literal("rocket")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("spawn").executes(ctx -> spawn(ctx.getSource())))
                .then(CommandManager.literal("launch").executes(ctx -> launch(ctx.getSource())))
                .then(CommandManager.literal("return").executes(ctx -> returnHome(ctx.getSource())))));
    }

    private static int spawn(ServerCommandSource source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerWorld world = player.getServerWorld();
        if (!world.getRegistryKey().equals(World.OVERWORLD) || player.getY() > 180) {
            source.sendError(Text.translatable("message.megarockets.spawn_world"));
            return 0;
        }
        Vec3d direction = Vec3d.fromPolar(0, player.getYaw());
        BlockPos base = BlockPos.ofFloored(player.getPos().add(direction.multiply(15)));
        TitanRocket rocket = new TitanRocket(TITAN, world);
        rocket.refreshPositionAndAngles(base.getX() + .5, base.getY(), base.getZ() + .5, 0, 0);
        if (!world.isSpaceEmpty(rocket, rocket.getBoundingBox()) ||
            !world.getOtherEntities(rocket, rocket.getBoundingBox()).isEmpty()) {
            source.sendError(Text.translatable("message.megarockets.clearance"));
            return 0;
        }
        if (!world.spawnEntity(rocket)) { source.sendError(Text.translatable("message.megarockets.failed")); return 0; }
        source.sendFeedback(() -> Text.translatable("message.megarockets.spawned"), false);
        return 1;
    }

    private static int launch(ServerCommandSource source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        TitanRocket rocket;
        if (player.getVehicle() instanceof TitanRocket mounted) rocket = mounted;
        else rocket = player.getServerWorld().getEntitiesByClass(TitanRocket.class, player.getBoundingBox().expand(22),
                r -> !r.hasPassengers() && r.getY() < TitanRocket.TARGET_ALTITUDE && r.getWorld().getRegistryKey().equals(World.OVERWORLD))
            .stream().min(Comparator.comparingDouble(r -> r.squaredDistanceTo(player))).orElse(null);
        if (rocket == null || !rocket.beginLaunch(player)) {
            source.sendError(Text.translatable("message.megarockets.no_rocket")); return 0;
        }
        return 1;
    }

    static boolean arrive(TitanRocket old, ServerPlayerEntity pilot) {
        ServerWorld orbit = pilot.getServer().getWorld(ORBIT);
        if (orbit == null) return false;
        // Independent small observation deck next to the arriving rocket.
        int x = (int)Math.floor(old.getX()), z = (int)Math.floor(old.getZ());
        int floor = 133;
        for (int dx=-3; dx<=3; dx++) for (int dz=-3; dz<=3; dz++) {
            BlockPos pos = new BlockPos(x+12+dx, floor, z+dz);
            if (orbit.getBlockState(pos).isAir()) orbit.setBlockState(pos, Blocks.SMOOTH_STONE.getDefaultState());
            if (Math.abs(dx)==3 || Math.abs(dz)==3) {
                BlockPos rail = pos.up();
                if (orbit.getBlockState(rail).isAir()) orbit.setBlockState(rail, Blocks.IRON_BARS.getDefaultState());
            }
        }
        BlockPos lamp = new BlockPos(x+12, floor, z);
        if (orbit.getBlockState(lamp).isOf(Blocks.SMOOTH_STONE)) orbit.setBlockState(lamp, Blocks.SEA_LANTERN.getDefaultState());
        TitanRocket orbitalRocket = new TitanRocket(TITAN, orbit);
        orbitalRocket.refreshPositionAndAngles(x+.5, 80, z+.5, 0, 0);
        if (!orbit.spawnEntity(orbitalRocket)) return false;
        pilot.stopRiding();
        if (!pilot.teleport(orbit, x+12.5, floor+1, z+.5, Set.of(), 90, 0)) {
            orbitalRocket.discard(); return false;
        }
        old.discard();
        pilot.sendMessage(Text.translatable("message.megarockets.orbit"), false);
        return true;
    }

    private static int returnHome(ServerCommandSource source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerWorld world = player.getServer().getOverworld();
        BlockPos spawn = world.getSpawnPos();
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
        player.stopRiding();
        if (!player.teleport(world, spawn.getX()+.5, y, spawn.getZ()+.5, Set.of(), player.getYaw(), 0)) {
            source.sendError(Text.translatable("message.megarockets.failed")); return 0;
        }
        player.sendMessage(Text.translatable("message.megarockets.returned"), false);
        return 1;
    }
}
