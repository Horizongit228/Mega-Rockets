package dev.megarockets;

import java.util.*;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.*;
import net.minecraft.block.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.Heightmap;

public final class SpaceEnergy {
 public static final Block PANEL=register("solar_panel",new DeviceBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)));
 public static final Block CABLE=register("energy_cable",new Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)));
 public static final Block BATTERY=register("energy_battery",new BatteryBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)));
 public static final BlockEntityType<Battery> TYPE=Registry.register(Registries.BLOCK_ENTITY_TYPE,MegaRockets.id("energy_battery"),
     FabricBlockEntityTypeBuilder.create(Battery::new,BATTERY).build());
 // Server-only leases, refreshed by the network each second; no persistent powered flag.
 private static final Map<ServerWorld,Map<BlockPos,Long>> POWER=new WeakHashMap<>();
 private SpaceEnergy(){}
 private static Block register(String id,Block block){
  Registry.register(Registries.BLOCK,MegaRockets.id(id),block);
  Registry.register(Registries.ITEM,MegaRockets.id(id),new BlockItem(block,new Item.Settings()));return block;
 }
 public static void init(){
  ServerTickEvents.END_WORLD_TICK.register(w->{if(w.getTime()%200==0){var m=POWER.get(w);if(m!=null)m.values().removeIf(t->t<w.getTime());}});
 }
 public static boolean powered(ServerWorld world,BlockPos pos){return POWER.getOrDefault(world,Map.of()).getOrDefault(pos,-1L)>=world.getTime();}
 private static boolean loaded(ServerWorld w,BlockPos pos){return pos.getY()>=w.getBottomY()&&pos.getY()<w.getTopY()&&w.getChunkManager().getWorldChunk(pos.getX()>>4,pos.getZ()>>4)!=null;}
 private static boolean conductor(BlockState state){return state.isOf(PANEL)||state.isOf(CABLE)||state.isOf(BATTERY);}
 public static int output(ServerWorld w,BlockPos pos){
  boolean orbit=w.getRegistryKey().equals(MegaRockets.ORBIT);
  Destination d=MegaRockets.planet(w);
  if(!orbit&&d==null&&!w.getRegistryKey().equals(World.OVERWORLD))return 0;
  var body=SolarDynamics.position(d,w.getTime());
  double au=orbit?Math.hypot(pos.getX(),pos.getZ())/SolarDynamics.AU:Math.hypot(body.x(),body.z())/SolarDynamics.AU;
  boolean exposed=w.getTopY(Heightmap.Type.WORLD_SURFACE,pos.getX(),pos.getZ())<=pos.getY()+1;
  long time=d==null?w.getTimeOfDay():SolarDynamics.localTime(d,w.getTime());
  return EnergyRules.solar(time,orbit,exposed,au,w.isThundering()?.25:w.isRaining()?.55:1);
 }
 private static final class DeviceBlock extends Block{
  DeviceBlock(Settings s){super(s);}
  @Override protected ActionResult onUse(BlockState s,World world,BlockPos p,PlayerEntity player,BlockHitResult hit){
   if(world instanceof ServerWorld w)player.sendMessage(Text.translatable("energy.megarockets.panel",output(w,p)),true);
   return ActionResult.SUCCESS;
  }
 }
 private static final class BatteryBlock extends BlockWithEntity{
  private static final MapCodec<BatteryBlock> CODEC=createCodec(BatteryBlock::new);
  BatteryBlock(Settings s){super(s);}
  @Override protected MapCodec<? extends BlockWithEntity> getCodec(){return CODEC;}
  @Override protected BlockRenderType getRenderType(BlockState state){return BlockRenderType.MODEL;}
  @Override public BlockEntity createBlockEntity(BlockPos pos,BlockState state){return new Battery(pos,state);}
  @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world,BlockState state,BlockEntityType<T> type){
   return world.isClient?null:validateTicker(type,TYPE,Battery::tick);
  }
  @Override protected ActionResult onUse(BlockState state,World world,BlockPos pos,PlayerEntity player,BlockHitResult hit){
   if(world instanceof ServerWorld&&world.getBlockEntity(pos) instanceof Battery b)
    player.sendMessage(Text.translatable("energy.megarockets.battery",b.energy,EnergyRules.CAPACITY,b.generation,b.consumers),false);
   return ActionResult.SUCCESS;
  }
 }
 public static final class Battery extends BlockEntity {
  private int energy,generation,consumers;
  Battery(BlockPos pos,BlockState state){super(TYPE,pos,state);}
  @Override protected void writeNbt(NbtCompound nbt,RegistryWrapper.WrapperLookup lookup){super.writeNbt(nbt,lookup);nbt.putInt("Energy",energy);}
  @Override protected void readNbt(NbtCompound nbt,RegistryWrapper.WrapperLookup lookup){super.readNbt(nbt,lookup);energy=Math.max(0,Math.min(EnergyRules.CAPACITY,nbt.getInt("Energy")));}
  static void tick(World world,BlockPos pos,BlockState state,Battery self){
   if(!(world instanceof ServerWorld w)||w.getTime()%20!=0)return;
   var visited=new HashSet<BlockPos>();var q=new ArrayDeque<BlockPos>();var batteries=new ArrayList<Battery>();
   var panels=new HashSet<BlockPos>();var loads=new HashSet<BlockPos>();
   visited.add(pos);q.add(pos);
   while(!q.isEmpty()){
    var p=q.removeFirst();
    if(!loaded(w,p))continue;
    var s=w.getBlockState(p);
    if(s.isOf(OxygenStation.STATION)){loads.add(p);continue;}
    if(!conductor(s))continue;
    if(s.isOf(PANEL))panels.add(p);
    if(s.isOf(BATTERY)&&w.getBlockEntity(p) instanceof Battery b)batteries.add(b);
    for(Direction dir:Direction.values()){
     var n=p.offset(dir);if(visited.add(n))q.addLast(n);
    }
    if(visited.size()>4096){self.generation=0;self.consumers=0;return;}
   }
   batteries.sort(Comparator.comparingLong(b->b.getPos().asLong()));
   if(batteries.isEmpty()||batteries.getFirst()!=self)return;
   int generation=panels.stream().mapToInt(p->output(w,p)).sum();
   int total=batteries.stream().mapToInt(b->b.energy).sum()+generation;
   var ordered=new ArrayList<>(loads);ordered.sort(Comparator.comparingLong(BlockPos::asLong));
   var powered=POWER.computeIfAbsent(w,k->new HashMap<>());int supplied=0;
   for(var load:ordered){
    if(total>=EnergyRules.DEMAND){total-=EnergyRules.DEMAND;powered.put(load,w.getTime()+24);supplied++;}
    else powered.remove(load);
   }
   // Conserve charge across connected batteries; persist each battery on disk.
   int remaining=Math.min(total,batteries.size()*EnergyRules.CAPACITY);
   for(int i=0;i<batteries.size();i++){
    var b=batteries.get(i);int stored=remaining/(batteries.size()-i);remaining-=stored;
    if(b.energy!=stored){b.energy=stored;b.markDirty();}
    b.generation=generation;b.consumers=supplied;
   }
  }
 }
}
