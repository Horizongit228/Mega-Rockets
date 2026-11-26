package dev.megarockets;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AsteroidGenerator extends ChunkGenerator {
 public static final MapCodec<AsteroidGenerator> CODEC=BiomeSource.CODEC.fieldOf("biome_source").xmap(AsteroidGenerator::new,AsteroidGenerator::getBiomeSource);
 private NoiseConfig last;private AsteroidTerrain cached;
 public AsteroidGenerator(BiomeSource source){super(source);}
 private synchronized AsteroidTerrain terrain(NoiseConfig config){
  if(last!=config||cached==null){last=config;cached=new AsteroidTerrain(config.getOrCreateRandomDeriver(MegaRockets.id("asteroid_seed")).split(0,0,0).nextLong());}return cached;
 }
 @Override protected MapCodec<? extends ChunkGenerator> getCodec(){return CODEC;}
 @Override public int getMinimumY(){return -64;}
 @Override public int getWorldHeight(){return 384;}
 @Override public int getSeaLevel(){return -64;}
 @Override public int getSpawnHeight(HeightLimitView world){return 180;}
 @Override public void buildSurface(ChunkRegion region,StructureAccessor structures,NoiseConfig config,Chunk chunk){}
 @Override public void carve(ChunkRegion region,long seed,NoiseConfig config,BiomeAccess biomes,StructureAccessor structures,Chunk chunk,GenerationStep.Carver step){}
 @Override public void populateEntities(ChunkRegion region){}
 @Override public void getDebugHudText(List<String> lines,NoiseConfig config,BlockPos pos){lines.add("Mega Rockets: asteroid belt 2.1–3.3 AU");}
 private BlockState stationState(int type){
  return switch(type){
   case StationLayout.HULL->Blocks.IRON_BLOCK.getDefaultState();
   case StationLayout.GLASS->Blocks.GLASS.getDefaultState();
   case StationLayout.LAMP->Blocks.SEA_LANTERN.getDefaultState();
   case StationLayout.OXYGEN->OxygenStation.STATION.getDefaultState();
   case StationLayout.DOOR_LOWER,StationLayout.DOOR_UPPER->Blocks.IRON_DOOR.getDefaultState()
     .with(net.minecraft.block.DoorBlock.FACING,net.minecraft.util.math.Direction.EAST)
     .with(net.minecraft.block.DoorBlock.HALF,type==StationLayout.DOOR_LOWER?net.minecraft.block.enums.DoubleBlockHalf.LOWER:net.minecraft.block.enums.DoubleBlockHalf.UPPER);
   case StationLayout.NORTH_DOOR_LOWER,StationLayout.NORTH_DOOR_UPPER,StationLayout.INNER_DOOR_LOWER,StationLayout.INNER_DOOR_UPPER->Blocks.OAK_DOOR.getDefaultState()
     .with(net.minecraft.block.DoorBlock.FACING,type==StationLayout.NORTH_DOOR_LOWER||type==StationLayout.NORTH_DOOR_UPPER?net.minecraft.util.math.Direction.NORTH:net.minecraft.util.math.Direction.EAST)
     .with(net.minecraft.block.DoorBlock.HALF,type==StationLayout.NORTH_DOOR_LOWER||type==StationLayout.INNER_DOOR_LOWER?net.minecraft.block.enums.DoubleBlockHalf.LOWER:net.minecraft.block.enums.DoubleBlockHalf.UPPER);
   case StationLayout.CONSOLE,StationLayout.STATUS->StationConsole.BLOCK.getDefaultState();
   case StationLayout.BLUE->Blocks.LIGHT_BLUE_CONCRETE.getDefaultState();
   case StationLayout.FRAME->Blocks.POLISHED_DEEPSLATE.getDefaultState();
   case StationLayout.WORKBENCH->Blocks.CRAFTING_TABLE.getDefaultState();
   case StationLayout.BOOKS->Blocks.BOOKSHELF.getDefaultState();
   case StationLayout.MOSS->Blocks.MOSS_BLOCK.getDefaultState();
   case StationLayout.LEAVES->Blocks.AZALEA_LEAVES.getDefaultState().with(net.minecraft.block.LeavesBlock.PERSISTENT,true);
   case StationLayout.SOIL->Blocks.DIRT.getDefaultState();
   case StationLayout.SOLAR->Blocks.BLUE_STAINED_GLASS.getDefaultState();
   case StationLayout.PAD->Blocks.SMOOTH_STONE.getDefaultState();
   case StationLayout.MARK->Blocks.YELLOW_CONCRETE.getDefaultState();
   default->Blocks.AIR.getDefaultState();
  };
 }
 private BlockState state(int type){return switch(type){
  case 1->Blocks.STONE.getDefaultState();case 2->Blocks.IRON_ORE.getDefaultState();case 3->Blocks.BASALT.getDefaultState();default->Blocks.AIR.getDefaultState();
 };}
 @Override public CompletableFuture<Chunk> populateNoise(Blender blender,NoiseConfig config,StructureAccessor structures,Chunk chunk){
  AsteroidTerrain t=terrain(config);BlockPos.Mutable pos=new BlockPos.Mutable();
  int x0=chunk.getPos().getStartX(),z0=chunk.getPos().getStartZ();
  for(int x=x0;x<x0+16;x++)for(int z=z0;z<z0+16;z++){
   if(StationLayout.column(x,z)){
    for(int y=98;y<=StationLayout.TOP_Y;y++){int type=StationLayout.material(x,y,z);if(type!=0)chunk.setBlockState(pos.set(x,y,z),stationState(type),false);}
    continue;
   }
   var rock=t.at(x,z);if(rock==null||Math.abs(x-rock.x())>rock.rx()+3||Math.abs(z-rock.z())>rock.rz()+3)continue;
   for(int y=rock.y()-rock.ry()-3;y<=rock.y()+rock.ry()+3;y++){
    int type=t.material(rock,x,y,z);if(type!=0)chunk.setBlockState(pos.set(x,y,z),state(type),false);
   }
  }
  Heightmap.populateHeightmaps(chunk,Set.of(Heightmap.Type.OCEAN_FLOOR_WG,Heightmap.Type.WORLD_SURFACE_WG));
  return CompletableFuture.completedFuture(chunk);
 }
 @Override public int getHeight(int x,int z,Heightmap.Type type,HeightLimitView world,NoiseConfig config){
  if(StationLayout.column(x,z))return StationLayout.height(x,z);
  var t=terrain(config);var rock=t.at(x,z);if(rock==null)return world.getBottomY();
  for(int y=rock.y()+rock.ry()+3;y>=rock.y()-rock.ry()-3;y--)if(t.material(rock,x,y,z)!=0)return y+1;return world.getBottomY();
 }
 @Override public VerticalBlockSample getColumnSample(int x,int z,HeightLimitView world,NoiseConfig config){
  var t=terrain(config);var rock=t.at(x,z);BlockState[] states=new BlockState[world.getHeight()];
  for(int i=0;i<states.length;i++)states[i]=StationLayout.column(x,z)?stationState(StationLayout.material(x,world.getBottomY()+i,z)):state(t.material(rock,x,world.getBottomY()+i,z));
  return new VerticalBlockSample(world.getBottomY(),states);
 }
}
