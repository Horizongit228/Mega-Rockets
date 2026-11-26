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

public final class OuterPlanetGenerator extends ChunkGenerator {
 public static final MapCodec<OuterPlanetGenerator> CODEC=RecordCodecBuilder.mapCodec(i->i.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(OuterPlanetGenerator::getBiomeSource),Codec.STRING.fieldOf("planet").forGetter(g->g.planet.name())).apply(i,OuterPlanetGenerator::new));
 private final Destination planet;
 private NoiseConfig last;private OuterPlanetTerrain cached;
 public OuterPlanetGenerator(BiomeSource source,String name){super(source);planet=Destination.read(name);if(planet==Destination.MOON||planet==Destination.MARS||planet.orbitOnly())throw new IllegalArgumentException(name);}
 private synchronized OuterPlanetTerrain terrain(NoiseConfig config){
  if(last!=config||cached==null){last=config;cached=new OuterPlanetTerrain(planet,config.getOrCreateRandomDeriver(MegaRockets.id(planet.key()+"_seed")).split(0,0,0).nextLong(),true);}return cached;
 }
 @Override protected MapCodec<? extends ChunkGenerator> getCodec(){return CODEC;}
 @Override public int getMinimumY(){return OuterPlanetTerrain.MIN_Y;}
 @Override public int getWorldHeight(){return OuterPlanetTerrain.HEIGHT;}
 @Override public int getSeaLevel(){return OuterPlanetTerrain.MIN_Y;}
 @Override public int getSpawnHeight(HeightLimitView world){return 128;}
 @Override public void buildSurface(ChunkRegion region,StructureAccessor structures,NoiseConfig config,Chunk chunk){}
 @Override public void carve(ChunkRegion region,long seed,NoiseConfig config,BiomeAccess biomes,StructureAccessor structures,Chunk chunk,GenerationStep.Carver step){}
 @Override public void populateEntities(ChunkRegion region){}
 @Override public void getDebugHudText(List<String> lines,NoiseConfig config,BlockPos pos){lines.add("Mega Rockets: "+planet.name()+" | diameter "+planet.radius*2);}
 private BlockState state(int type){var m=OuterPlanetBlocks.get(planet);return switch(type){
  case OuterPlanetTerrain.SURFACE->m.surface().getDefaultState();
  case OuterPlanetTerrain.ROCK->m.rock().getDefaultState();
  case OuterPlanetTerrain.ORE->m.ore().getDefaultState();
  case OuterPlanetTerrain.BEDROCK->Blocks.BEDROCK.getDefaultState();
  default->Blocks.AIR.getDefaultState();
 };}
 @Override public CompletableFuture<Chunk> populateNoise(Blender blender,NoiseConfig config,StructureAccessor structures,Chunk chunk){
  OuterPlanetTerrain t=terrain(config);BlockPos.Mutable pos=new BlockPos.Mutable();
  int x0=chunk.getPos().getStartX(),z0=chunk.getPos().getStartZ();
  for(int x=x0;x<x0+16;x++)for(int z=z0;z<z0+16;z++){
   int top=t.top(x,z);var cave=t.caveColumn(x,z);
   for(int y=chunk.getBottomY();y<=Math.min(top,chunk.getTopY()-1);y++){
    int type=t.material(x,y,z,top,cave);if(type!=OuterPlanetTerrain.AIR)chunk.setBlockState(pos.set(x,y,z),state(type),false);
   }
  }
  Heightmap.populateHeightmaps(chunk,Set.of(Heightmap.Type.OCEAN_FLOOR_WG,Heightmap.Type.WORLD_SURFACE_WG));
  return CompletableFuture.completedFuture(chunk);
 }
 @Override public int getHeight(int x,int z,Heightmap.Type type,HeightLimitView world,NoiseConfig config){OuterPlanetTerrain t=terrain(config);int top=t.top(x,z);var cave=t.caveColumn(x,z);for(int y=Math.min(world.getTopY()-1,top);y>=world.getBottomY();y--)if(t.material(x,y,z,top,cave)!=OuterPlanetTerrain.AIR)return y+1;return world.getBottomY();}
 @Override public VerticalBlockSample getColumnSample(int x,int z,HeightLimitView world,NoiseConfig config){
  OuterPlanetTerrain t=terrain(config);int top=t.top(x,z);var cave=t.caveColumn(x,z);BlockState[] states=new BlockState[world.getHeight()];
  for(int i=0;i<states.length;i++)states[i]=state(t.material(x,world.getBottomY()+i,z,top,cave));
  return new VerticalBlockSample(world.getBottomY(),states);
 }
}
