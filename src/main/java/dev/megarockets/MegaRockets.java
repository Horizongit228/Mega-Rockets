package dev.megarockets;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
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
    public static final RegistryKey<World> MARS=RegistryKey.of(RegistryKeys.WORLD,id("mars"));
    public static final RegistryKey<World> MOON=RegistryKey.of(RegistryKeys.WORLD,id("moon"));
    public static final RegistryKey<World> MOON_MIDDLE=RegistryKey.of(RegistryKeys.WORLD,id("moon_middle"));
    public static final RegistryKey<World> MOON_SOUTH=RegistryKey.of(RegistryKeys.WORLD,id("moon_south"));
    public static boolean isMoon(World world){var key=world.getRegistryKey();return key.equals(MOON)||key.equals(MOON_MIDDLE)||key.equals(MOON_SOUTH);}
    public static final EntityType<TitanRocket> TITAN = Registry.register(Registries.ENTITY_TYPE,
        id("titan_1"), EntityType.Builder.<TitanRocket>create(TitanRocket::new, SpawnGroup.MISC)
            .dimensions(14f, 64f).maxTrackingRange(16).trackingTickInterval(1).build("megarockets:titan_1"));

    public static Identifier id(String path) { return Identifier.of(ID, path); }

    @Override public void onInitialize() {
        MoonBlocks.init();
        MarsBlocks.init();
        Registry.register(Registries.CHUNK_GENERATOR,id("mars_terrain"),MarsChunkGenerator.CODEC);
        RocketItems.init();
        MegaRocketsItemGroup.init();
        Registry.register(Registries.CHUNK_GENERATOR,id("moon_sphere"),MoonChunkGenerator.CODEC);
        Registry.register(Registries.BIOME_SOURCE,id("moon_biomes"),MoonBiomeSource.CODEC);
        ServerWorldEvents.LOAD.register((server,world)->{
            if(world.getChunkManager().getChunkGenerator() instanceof MoonChunkGenerator moon)
                moon.initializeSeed(world.getChunkManager().getNoiseConfig());
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) ->
            dispatcher.register(CommandManager.literal("rocket")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("spawn").executes(ctx -> spawn(ctx.getSource())))
                .then(CommandManager.literal("launch").executes(ctx -> launch(ctx.getSource())))
                .then(CommandManager.literal("status").executes(ctx -> status(ctx.getSource())))
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
                r -> !r.hasPassengers() && (r.canResumeMoon() || (r.getY() < TitanRocket.TARGET_ALTITUDE && r.getWorld().getRegistryKey().equals(World.OVERWORLD))))
            .stream().min(Comparator.comparingDouble(r -> r.squaredDistanceTo(player))).orElse(null);
        if (rocket == null || !rocket.beginLaunch(player)) {
            source.sendError(Text.translatable("message.megarockets.no_rocket")); return 0;
        }
        return 1;
    }

    private static int status(ServerCommandSource source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player=source.getPlayerOrThrow();
        TitanRocket ship=player.getVehicle() instanceof TitanRocket mounted?mounted:
            player.getServerWorld().getEntitiesByClass(TitanRocket.class,player.getBoundingBox().expand(100),e->true)
                .stream().min(Comparator.comparingDouble(e->e.squaredDistanceTo(player))).orElse(null);
        if(ship==null){source.sendError(Text.translatable("message.megarockets.no_rocket"));return 0;}
        source.sendFeedback(ship::missionStatus,false);return 1;
    }

    static boolean arrive(TitanRocket old, ServerPlayerEntity pilot) {
        ServerWorld orbit=pilot.getServer().getWorld(ORBIT);
        if(orbit==null) return false;
        // Only the transition from the Overworld to space uses a dimension transfer.
        // The subsequent 6000-block flight and landing occur entirely within space.
        TitanRocket ship=new TitanRocket(TITAN,orbit);
        ship.refreshPositionAndAngles(.5,MoonWorld.CRUISE_Y,.5,0,0);
        if(!orbit.spawnEntity(ship)) return false;
        pilot.stopRiding();
        if(!pilot.teleport(orbit,.5,MoonWorld.CRUISE_Y+53,-4.2,Set.of(),pilot.getYaw(),0)) {
            ship.discard(); return false;
        }
        if(!pilot.startRiding(ship,true)) {
            ship.discard(); return false;
        }
        old.discard();
        // The large Moon is generated on demand in its own dimension.
        ship.beginMoonFlight();
        pilot.sendMessage(Text.translatable("message.megarockets.moon_departure"),false);
        return true;
    }

    static boolean enterMoon(TitanRocket old,ServerPlayerEntity pilot,LunarFlight.State state) {
        ServerWorld moon=pilot.getServer().getWorld(MOON);
        if(moon==null)return false;
        // Check actual blocks, not just theoretical terrain. Old generated chunks must not
        // cause a landing in empty air or rock. Never overwrite a player's existing builds.
        for(int[] point:new int[][]{{0,0},{-7,-7},{-7,7},{7,-7},{7,7},{16,0}}) {
            BlockPos floor=new BlockPos(point[0],MoonTerrain.PAD_Y-1,point[1]);
            if(!moon.getBlockState(floor).isOf(MoonBlocks.PAD)) {
                if(old.age%40==0)pilot.sendMessage(Text.translatable("message.megarockets.old_moon"),true);
                return false;
            }
        }
        for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++) {
            // Heightmap checks every column in the rocket footprint for obstructions.
            if(moon.getTopY(Heightmap.Type.MOTION_BLOCKING,x,z)>MoonTerrain.PAD_Y) {
                if(old.age%40==0)pilot.sendMessage(Text.translatable("message.megarockets.pad_blocked"),true);
                return false;
            }
        }
        if(moon.getTopY(Heightmap.Type.MOTION_BLOCKING,16,0)>MoonTerrain.PAD_Y) {
            if(old.age%40==0)pilot.sendMessage(Text.translatable("message.megarockets.pad_blocked"),true);
            return false;
        }
        TitanRocket ship=new TitanRocket(TITAN,moon);
        double y=state.y()+MoonTerrain.PAD_Y-LunarFlight.LAND_Y;
        ship.refreshPositionAndAngles(.5,y,.5,0,0);
        ship.acceptLunarState(state);
        if(!moon.spawnEntity(ship))return false;
        pilot.stopRiding();
        if(!pilot.teleport(moon,.5,y+53,-4.2,Set.of(),pilot.getYaw(),0)){
            ship.discard();pilot.startRiding(old,true);return false;
        }
        if(!pilot.startRiding(ship,true)){ship.discard();return false;}
        old.discard();
        pilot.sendMessage(Text.translatable("message.megarockets.moon_entry"),false);
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
