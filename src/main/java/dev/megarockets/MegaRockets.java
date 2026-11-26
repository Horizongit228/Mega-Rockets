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
    public static final RegistryKey<World> MERCURY=RegistryKey.of(RegistryKeys.WORLD,id("mercury"));
    public static final RegistryKey<World> VENUS=RegistryKey.of(RegistryKeys.WORLD,id("venus"));
    public static final RegistryKey<World> EUROPA=RegistryKey.of(RegistryKeys.WORLD,id("europa"));
    public static final RegistryKey<World> TITAN_WORLD=RegistryKey.of(RegistryKeys.WORLD,id("titan"));
    public static final RegistryKey<World> MOON=RegistryKey.of(RegistryKeys.WORLD,id("moon"));
    public static final RegistryKey<World> MOON_MIDDLE=RegistryKey.of(RegistryKeys.WORLD,id("moon_middle"));
    public static final RegistryKey<World> MOON_SOUTH=RegistryKey.of(RegistryKeys.WORLD,id("moon_south"));
    public static boolean isMoon(World world){var key=world.getRegistryKey();return key.equals(MOON)||key.equals(MOON_MIDDLE)||key.equals(MOON_SOUTH);}
    public static final EntityType<TitanRocket> TITAN = Registry.register(Registries.ENTITY_TYPE,
        id("titan_1"), EntityType.Builder.<TitanRocket>create(TitanRocket::new, SpawnGroup.MISC)
            .dimensions(14f, 64f).maxTrackingRange(16).trackingTickInterval(1).build("megarockets:titan_1"));

    public static final EntityType<TitanRocket> PHANTOM=rocketType("phantom");
    public static final EntityType<TitanRocket> HYPERION=rocketType("hyperion");
    private static EntityType<TitanRocket> rocketType(String name){
        return Registry.register(Registries.ENTITY_TYPE,id(name),EntityType.Builder.<TitanRocket>create(TitanRocket::new,SpawnGroup.MISC)
            .dimensions(14f,64f).maxTrackingRange(16).trackingTickInterval(1).build("megarockets:"+name));
    }
    public static RegistryKey<World> destinationWorld(Destination d){return switch(d){case MARS->MARS;case VENUS->VENUS;case EUROPA->EUROPA;case TITAN->TITAN_WORLD;case MERCURY->MERCURY;case JUPITER,SATURN,URANUS,NEPTUNE->ORBIT;default->MOON;};}
    public static Destination planet(World world){for(Destination d:Destination.values())if(!d.orbitOnly()&&world.getRegistryKey().equals(destinationWorld(d)))return d;return isMoon(world)?Destination.MOON:null;}
    public static Identifier id(String path) { return Identifier.of(ID, path); }

    @Override public void onInitialize() {
        MoonBlocks.init();
        MarsBlocks.init();
        OuterPlanetBlocks.init();
        Registry.register(Registries.CHUNK_GENERATOR,id("outer_planet"),OuterPlanetGenerator.CODEC);
        Registry.register(Registries.CHUNK_GENERATOR,id("asteroid_belt"),AsteroidGenerator.CODEC);
        Registry.register(Registries.CHUNK_GENERATOR,id("mars_terrain"),MarsChunkGenerator.CODEC);
        RocketItems.init();
        SpaceSuitItems.init();
        OxygenStation.init();
        OxygenSystem.init();
        ManualInput.init();
        CockpitAction.init();
        MegaRocketsItemGroup.init();
        Registry.register(Registries.CHUNK_GENERATOR,id("moon_sphere"),MoonChunkGenerator.CODEC);
        Registry.register(Registries.BIOME_SOURCE,id("moon_biomes"),MoonBiomeSource.CODEC);
        ServerWorldEvents.LOAD.register((server,world)->{
            if(world.getChunkManager().getChunkGenerator() instanceof MoonChunkGenerator moon)
                moon.initializeSeed(world.getChunkManager().getNoiseConfig());
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) ->
            dispatcher.register(CommandManager.literal("rocket")
                .then(CommandManager.literal("spawn").requires(source -> source.hasPermissionLevel(2)||source.getEntity() instanceof ServerPlayerEntity player&&player.isCreative()).executes(ctx -> spawn(ctx.getSource())))
                .then(CommandManager.literal("launch").executes(ctx -> launch(ctx.getSource())))
                .then(CommandManager.literal("destination").executes(ctx -> choose(ctx.getSource(),null))
                    .then(CommandManager.literal("moon").executes(ctx -> choose(ctx.getSource(),Destination.MOON)))
                    .then(CommandManager.literal("mars").executes(ctx -> choose(ctx.getSource(),Destination.MARS)))
                    .then(CommandManager.literal("venus").executes(ctx -> choose(ctx.getSource(),Destination.VENUS)))
                    .then(CommandManager.literal("europa").executes(ctx -> choose(ctx.getSource(),Destination.EUROPA)))
                    .then(CommandManager.literal("titan").executes(ctx -> choose(ctx.getSource(),Destination.TITAN)))
                    .then(CommandManager.literal("mercury").executes(ctx -> choose(ctx.getSource(),Destination.MERCURY)))
                    .then(CommandManager.literal("jupiter").executes(ctx -> choose(ctx.getSource(),Destination.JUPITER)))
                    .then(CommandManager.literal("saturn").executes(ctx -> choose(ctx.getSource(),Destination.SATURN)))
                    .then(CommandManager.literal("uranus").executes(ctx -> choose(ctx.getSource(),Destination.URANUS)))
                    .then(CommandManager.literal("neptune").executes(ctx -> choose(ctx.getSource(),Destination.NEPTUNE))))
                .then(CommandManager.literal("manual").executes(ctx -> manual(ctx.getSource(),false)))
                .then(CommandManager.literal("approach").executes(ctx -> manual(ctx.getSource(),true)))
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
        else if(!source.hasPermissionLevel(2)&&!player.isCreative()){
            source.sendError(Text.translatable("message.megarockets.destination_seat"));return 0;
        }
        else rocket = player.getServerWorld().getEntitiesByClass(TitanRocket.class, player.getBoundingBox().expand(22),
                r -> !r.hasPassengers() && (r.canResumeMoon() || (r.getY() < TitanRocket.TARGET_ALTITUDE && r.getWorld().getRegistryKey().equals(World.OVERWORLD))))
            .stream().min(Comparator.comparingDouble(r -> r.squaredDistanceTo(player))).orElse(null);
        if (rocket == null || !rocket.beginLaunch(player)) {
            source.sendError(Text.translatable("message.megarockets.no_rocket")); return 0;
        }
        return 1;
    }

    private static int choose(ServerCommandSource source,Destination destination) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity pilot=source.getPlayerOrThrow();
        if(!(pilot.getVehicle() instanceof TitanRocket rocket)){
            source.sendError(Text.translatable("message.megarockets.destination_seat"));return 0;
        }
        if(destination==null){rocket.showDestinations(pilot);return 1;}
        if(!rocket.selectDestination(destination)){source.sendError(Text.translatable("message.megarockets.destination_locked"));return 0;}
        rocket.showDestinations(pilot);return 1;
    }

    private static int manual(ServerCommandSource source,boolean approach) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var pilot=source.getPlayerOrThrow();
        if(!(pilot.getVehicle() instanceof TitanRocket rocket)){source.sendError(Text.translatable("message.megarockets.destination_seat"));return 0;}
        return (approach?rocket.requestApproach(pilot):rocket.toggleManual(pilot))?1:0;
    }
    static boolean manualOrbit(TitanRocket old,ServerPlayerEntity pilot){
        ServerWorld orbit=pilot.getServer().getWorld(ORBIT);
        if(orbit==null)return false;
        Destination origin=planet(old.getWorld());
        var center=SolarDynamics.position(origin,orbit.getTime());
        double x=center.x()+ApproachRules.EXIT_OFFSET+.5,z=center.z()+.5;
        TitanRocket ship=new TitanRocket(old.rocketType(),orbit);
        ship.copyDestination(old);ship.refreshPositionAndAngles(x,LunarFlight.CRUISE_Y,z,old.getYaw(),0);ship.startManual();
        if(!orbit.isSpaceEmpty(ship,ship.getBoundingBox())||!orbit.getOtherEntities(ship,ship.getBoundingBox()).isEmpty()||!orbit.spawnEntity(ship))return false;
        pilot.stopRiding();
        if(!pilot.teleport(orbit,x,LunarFlight.CRUISE_Y+51.35,z+.65,Set.of(),pilot.getYaw(),pilot.getPitch())){
            ship.discard();pilot.startRiding(old,true);return false;
        }
        if(!pilot.startRiding(ship,true)){
            ship.discard();pilot.teleport((ServerWorld)old.getWorld(),old.getX(),old.getY()+51.35,old.getZ()+.65,Set.of(),pilot.getYaw(),pilot.getPitch());pilot.startRiding(old,true);return false;
        }
        old.discard();pilot.sendMessage(Text.translatable("message.megarockets.manual_orbit"),false);return true;
    }
    static boolean manualSurface(TitanRocket old,ServerPlayerEntity pilot,ServerWorld target,Destination body,double x,double y,double z,double entrySpeed){
        TitanRocket ship=new TitanRocket(old.rocketType(),target);
        ship.copyDestination(old);ship.setEntryDestination(body);ship.startManual();ship.startEntryHeat(SolarDynamics.atmosphere(body),entrySpeed);
        ship.refreshPositionAndAngles(x,y,z,old.getYaw(),0);
        if(!target.getWorldBorder().contains(ship.getBoundingBox())||!target.isSpaceEmpty(ship,ship.getBoundingBox())||
            !target.getOtherEntities(ship,ship.getBoundingBox()).isEmpty()||!target.spawnEntity(ship))return false;
        pilot.stopRiding();
        if(!pilot.teleport(target,x,y+51.35,z+.65,Set.of(),pilot.getYaw(),pilot.getPitch())){
            ship.discard();pilot.startRiding(old,true);return false;
        }
        if(!pilot.startRiding(ship,true)){
            ship.discard();pilot.teleport((ServerWorld)old.getWorld(),old.getX(),old.getY()+51.35,old.getZ()+.65,Set.of(),pilot.getYaw(),pilot.getPitch());pilot.startRiding(old,true);return false;
        }
        old.discard();
        pilot.sendMessage(Text.translatable("message.megarockets.manual_entry",Text.translatable("destination.megarockets."+(body==null?"earth":body.key()))),false);
        return true;
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
        Destination destination=old.destination();
        var earth=SolarDynamics.position(null,orbit.getTime());double lane=earth.z();double startX=earth.x()+ApproachRules.EXIT_OFFSET+.5;
        TitanRocket ship=new TitanRocket(old.rocketType(),orbit);
        ship.copyDestination(old);
        ship.refreshPositionAndAngles(startX,MoonWorld.CRUISE_Y,lane+.5,0,0);
        if(!orbit.spawnEntity(ship)) return false;
        pilot.stopRiding();
        if(!pilot.teleport(orbit,startX,MoonWorld.CRUISE_Y+51.35,lane+1.15,Set.of(),pilot.getYaw(),0)) {
            ship.discard(); return false;
        }
        if(!pilot.startRiding(ship,true)) {
            ship.discard(); return false;
        }
        old.discard();
        // The large Moon is generated on demand in its own dimension.
        ship.beginMoonFlight();
        pilot.sendMessage(Text.translatable("message.megarockets.departure_target",Text.translatable("destination.megarockets."+destination.key()),(int)destination.distance),false);
        return true;
    }

    static boolean enterMoon(TitanRocket old,ServerPlayerEntity pilot,LunarFlight.State state) {
        Destination destination=old.destination();
        if(destination.orbitOnly())return false;
        ServerWorld moon=pilot.getServer().getWorld(destinationWorld(destination));
        if(moon==null)return false;
        if(old.age%5!=0)return false;
        var site=NaturalLanding.find(moon,old);
        if(site==null){
            if(old.age%40==0)pilot.sendMessage(Text.translatable("message.megarockets.scanning_landing"),true);
            return false;
        }
        TitanRocket ship=new TitanRocket(old.rocketType(),moon);
        ship.copyDestination(old);
        double y=state.y()+site.y()-LunarFlight.LAND_Y;
        ship.setLandingSite(site);
        ship.refreshPositionAndAngles(site.x()+.5,y,site.z()+.5,0,0);
        ship.acceptLunarState(state);ship.startEntryHeat(SolarDynamics.atmosphere(destination),300);
        if(!moon.spawnEntity(ship))return false;
        pilot.stopRiding();
        if(!pilot.teleport(moon,site.x()+.5,y+51.35,site.z()+1.15,Set.of(),pilot.getYaw(),0)){
            ship.discard();pilot.startRiding(old,true);return false;
        }
        if(!pilot.startRiding(ship,true)){ship.discard();return false;}
        old.discard();
        pilot.sendMessage(Text.translatable("message.megarockets.entry_target",Text.translatable("destination.megarockets."+destination.key())),false);
        pilot.sendMessage(Text.translatable("message.megarockets.landing_site",site.x(),site.y(),site.z()),false);
        return true;
    }

    private static int returnHome(ServerCommandSource source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        if(!source.hasPermissionLevel(2)&&!player.isCreative()&&!OxygenSystem.vacuum(player)){
            source.sendError(Text.translatable("message.megarockets.return_space_only"));return 0;
        }
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
