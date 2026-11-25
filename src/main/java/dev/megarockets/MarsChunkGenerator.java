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

public final class MarsChunkGenerator extends ChunkGenerator {
 public static final MapCodec<MarsChunkGenerator> CODEC=BiomeSource.CODEC.fieldOf("biome_source").xmap(MarsChunkGenerator::new,MarsChunkGenerator::getBiomeSource);
 private NoiseConfig last;private MarsTerrain cached;
 public MarsChunkGenerator(BiomeSource source){super(source);}
 private synchronized MarsTerrain terrain(NoiseConfig config){
  if(last!=config||cached==null){last=config;cached=new MarsTerrain(config.getOrCreateRandomDeriver(MegaRockets.id("mars_seed")).split(0,0,0).nextLong());}return cached;
 }
 @Override protected MapCodec<? extends ChunkGenerator> getCodec(){return CODEC;}
 @Override public int getMinimumY(){return MarsTerrain.MIN_Y;}
 @Override public int getWorldHeight(){return MarsTerrain.HEIGHT;}
 @Override public int getSeaLevel(){return MarsTerrain.MIN_Y;}
 @Override public int getSpawnHeight(HeightLimitView world){return MarsTerrain.PAD_Y;}
 @Override public void buildSurface(ChunkRegion region,StructureAccessor structures,NoiseConfig config,Chunk chunk){}
 @Override public void carve(ChunkRegion region,long seed,NoiseConfig config,BiomeAccess biomes,StructureAccessor structures,Chunk chunk,GenerationStep.Carver step){}
 @Override public void populateEntities(ChunkRegion region){}
 @Override public void getDebugHudText(List<String> lines,NoiseConfig config,BlockPos pos){lines.add("Mega Rockets: Mars | diameter 16384 | 10 mountains");}
 private static BlockState state(int type){return switch(type){
  case MarsTerrain.REGOLITH->MarsBlocks.REGOLITH.getDefaultState();
  case MarsTerrain.ROCK->MarsBlocks.ROCK.getDefaultState();
  case MarsTerrain.ORE->MarsBlocks.ORE.getDefaultState();
  case MarsTerrain.PAD->MoonBlocks.PAD.getDefaultState();
  case MarsTerrain.BEDROCK->Blocks.BEDROCK.getDefaultState();
  default->Blocks.AIR.getDefaultState();
 };}
 @Override public CompletableFuture<Chunk> populateNoise(Blender blender,NoiseConfig config,StructureAccessor structures,Chunk chunk){
  MarsTerrain t=terrain(config);BlockPos.Mutable pos=new BlockPos.Mutable();
  int x0=chunk.getPos().getStartX(),z0=chunk.getPos().getStartZ();
  for(int x=x0;x<x0+16;x++)for(int z=z0;z<z0+16;z++){
   int top=t.top(x,z);var cave=t.caveColumn(x,z);
   for(int y=chunk.getBottomY();y<=Math.min(top,chunk.getTopY()-1);y++){
    int type=t.material(x,y,z,top,cave);if(type!=MarsTerrain.AIR)chunk.setBlockState(pos.set(x,y,z),state(type),false);
   }
  }
  Heightmap.populateHeightmaps(chunk,Set.of(Heightmap.Type.OCEAN_FLOOR_WG,Heightmap.Type.WORLD_SURFACE_WG));
  return CompletableFuture.completedFuture(chunk);
 }
 @Override public int getHeight(int x,int z,Heightmap.Type type,HeightLimitView world,NoiseConfig config){MarsTerrain t=terrain(config);int top=t.top(x,z);var cave=t.caveColumn(x,z);for(int y=Math.min(world.getTopY()-1,top);y>=world.getBottomY();y--)if(t.material(x,y,z,top,cave)!=MarsTerrain.AIR)return y+1;return world.getBottomY();}
 @Override public VerticalBlockSample getColumnSample(int x,int z,HeightLimitView world,NoiseConfig config){
  MarsTerrain t=terrain(config);int top=t.top(x,z);var cave=t.caveColumn(x,z);BlockState[] states=new BlockState[world.getHeight()];
  for(int i=0;i<states.length;i++)states[i]=state(t.material(x,world.getBottomY()+i,z,top,cave));
  return new VerticalBlockSample(world.getBottomY(),states);
 }
}
