package dev.megarockets;

import com.mojang.serialization.MapCodec;
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

public final class MoonChunkGenerator extends ChunkGenerator {
    public static final MapCodec<MoonChunkGenerator> CODEC=BiomeSource.CODEC.fieldOf("biome_source").xmap(MoonChunkGenerator::new,MoonChunkGenerator::getBiomeSource);
    public MoonChunkGenerator(BiomeSource source){super(source);}
    @Override protected MapCodec<? extends ChunkGenerator> getCodec(){return CODEC;}
    @Override public int getMinimumY(){return MoonTerrain.MIN_Y;}
    @Override public int getWorldHeight(){return MoonTerrain.HEIGHT;}
    @Override public int getSeaLevel(){return MoonTerrain.MIN_Y;}
    @Override public int getSpawnHeight(HeightLimitView world){return MoonTerrain.PAD_Y;}
    @Override public void buildSurface(ChunkRegion region,StructureAccessor structures,NoiseConfig config,Chunk chunk){}
    @Override public void carve(ChunkRegion region,long seed,NoiseConfig config,BiomeAccess biomes,StructureAccessor structures,Chunk chunk,GenerationStep.Carver step){}
    @Override public void populateEntities(ChunkRegion region){}
    @Override public void getDebugHudText(List<String> lines,NoiseConfig config,BlockPos pos){lines.add("Mega Rockets: spherical Moon");}
    private static BlockState state(int material){
        return switch(material){
            case MoonTerrain.ROCK -> MoonBlocks.ROCK.getDefaultState();
            case MoonTerrain.REGOLITH -> MoonBlocks.REGOLITH.getDefaultState();
            case MoonTerrain.BASALT -> MoonBlocks.BASALT.getDefaultState();
            case MoonTerrain.ORE -> MoonBlocks.ORE.getDefaultState();
            case MoonTerrain.PAD -> MoonBlocks.PAD.getDefaultState();
            default -> Blocks.AIR.getDefaultState();
        };
    }
    @Override public CompletableFuture<Chunk> populateNoise(Blender blender,NoiseConfig config,StructureAccessor structures,Chunk chunk){
        int x0=chunk.getPos().getStartX(),z0=chunk.getPos().getStartZ();
        BlockPos.Mutable pos=new BlockPos.Mutable();
        for(int x=x0;x<x0+16;x++)for(int z=z0;z<z0+16;z++){
            var col=MoonTerrain.column(x,z);
            for(int y=Math.max(chunk.getBottomY(),col.bottom());y<=Math.min(chunk.getTopY()-1,col.top());y++){
                int type=MoonTerrain.material(col,x,y,z);
                if(type!=MoonTerrain.AIR)chunk.setBlockState(pos.set(x,y,z),state(type),false);
            }
        }
        Heightmap.populateHeightmaps(chunk,Set.of(Heightmap.Type.OCEAN_FLOOR_WG,Heightmap.Type.WORLD_SURFACE_WG));
        return CompletableFuture.completedFuture(chunk);
    }
    @Override public int getHeight(int x,int z,Heightmap.Type type,HeightLimitView world,NoiseConfig config){
        var col=MoonTerrain.column(x,z);
        return col.top()<col.bottom()?world.getBottomY():Math.min(world.getTopY(),col.top()+1);
    }
    @Override public VerticalBlockSample getColumnSample(int x,int z,HeightLimitView world,NoiseConfig config){
        BlockState[] result=new BlockState[world.getHeight()];
        var col=MoonTerrain.column(x,z);
        for(int i=0;i<result.length;i++)result[i]=state(MoonTerrain.material(col,x,world.getBottomY()+i,z));
        return new VerticalBlockSample(world.getBottomY(),result);
    }
}
