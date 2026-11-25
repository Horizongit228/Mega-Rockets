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

public final class MoonChunkGenerator extends ChunkGenerator {
    public static final MapCodec<MoonChunkGenerator> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
        BiomeSource.CODEC.fieldOf("biome_source").forGetter(MoonChunkGenerator::getBiomeSource),
        Codec.INT.optionalFieldOf("y_offset",MoonTerrain.NORTH_OFFSET).forGetter(g->g.yOffset)
    ).apply(instance,MoonChunkGenerator::new));
    private final int yOffset;
    private volatile NoiseConfig lastConfig;
    private MoonTerrain cachedTerrain;
    private synchronized MoonTerrain terrain(NoiseConfig config){
        if(lastConfig!=config||cachedTerrain==null){
            long seed=config.getOrCreateRandomDeriver(MegaRockets.id("lunar_world_seed")).split(0,0,0).nextLong();
            cachedTerrain=new MoonTerrain(seed);lastConfig=config;
            if(getBiomeSource() instanceof MoonBiomeSource lunar)lunar.setTerrain(cachedTerrain);
        }
        return cachedTerrain;
    }
    public void initializeSeed(NoiseConfig config){terrain(config);}
    @Override public CompletableFuture<Chunk> populateBiomes(NoiseConfig config,Blender blender,StructureAccessor structures,Chunk chunk){
        terrain(config);return super.populateBiomes(config,blender,structures,chunk);
    }
    public MoonChunkGenerator(BiomeSource source,int yOffset){super(source);this.yOffset=yOffset;}
    @Override protected MapCodec<? extends ChunkGenerator> getCodec(){return CODEC;}
    @Override public int getMinimumY(){return MoonTerrain.MIN_Y;}
    @Override public int getWorldHeight(){return MoonTerrain.HEIGHT;}
    @Override public int getSeaLevel(){return MoonTerrain.MIN_Y;}
    @Override public int getSpawnHeight(HeightLimitView world){return yOffset==MoonTerrain.NORTH_OFFSET?MoonTerrain.PAD_Y:0;}
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
            case MoonTerrain.WOOD -> MoonBlocks.WOOD.getDefaultState();
            case MoonTerrain.LEAVES -> MoonBlocks.LEAVES.getDefaultState().with(net.minecraft.block.LeavesBlock.PERSISTENT,true);
            default -> Blocks.AIR.getDefaultState();
        };
    }
    @Override public CompletableFuture<Chunk> populateNoise(Blender blender,NoiseConfig config,StructureAccessor structures,Chunk chunk){
        MoonTerrain terrain=terrain(config);
        int x0=chunk.getPos().getStartX(),z0=chunk.getPos().getStartZ();
        BlockPos.Mutable pos=new BlockPos.Mutable();
        for(int x=x0;x<x0+16;x++)for(int z=z0;z<z0+16;z++){
            var col=terrain.column(x,z);
            int lo=Math.max(chunk.getBottomY(),col.bottom()-yOffset),hi=Math.min(chunk.getTopY()-1,col.top()-yOffset);
            for(int y=lo;y<=hi;y++){
                int global=y+yOffset;
                if(global>=col.innerBottom()&&global<=col.innerTop()){y=col.innerTop()-yOffset;continue;}
                int type=terrain.material(col,x,global,z);
                if(type!=MoonTerrain.AIR)chunk.setBlockState(pos.set(x,y,z),state(type),false);
            }
        }
        for(int gx=Math.floorDiv(x0-4,32);gx<=Math.floorDiv(x0+19,32);gx++)
        for(int gz=Math.floorDiv(z0-4,32);gz<=Math.floorDiv(z0+19,32);gz++){
            var rock=terrain.boulder(gx,gz);if(rock==null)continue;
            int r=rock.radius();
            for(int x=Math.max(x0,rock.x()-r);x<=Math.min(x0+15,rock.x()+r);x++)
            for(int z=Math.max(z0,rock.z()-r);z<=Math.min(z0+15,rock.z()+r);z++)
            for(int global=rock.y()-2;global<=rock.y()+r;global++){
                int y=global-yOffset;if(y<chunk.getBottomY()||y>=chunk.getTopY())continue;
                int mat=terrain.boulderBlock(rock,x,global,z);
                if(mat!=MoonTerrain.AIR&&chunk.getBlockState(pos.set(x,y,z)).isAir())
                    chunk.setBlockState(pos,state(mat),false);
            }
        }
        // Populate only blocks belonging to this chunk, including canopies rooted across a boundary.
        for(int gx=Math.floorDiv(x0-5,MoonTerrain.TREE_CELL);gx<=Math.floorDiv(x0+20,MoonTerrain.TREE_CELL);gx++)
        for(int gz=Math.floorDiv(z0-5,MoonTerrain.TREE_CELL);gz<=Math.floorDiv(z0+20,MoonTerrain.TREE_CELL);gz++){
            var tree=terrain.tree(gx,gz);if(tree==null)continue;
            for(int x=Math.max(x0,tree.x()-5);x<=Math.min(x0+15,tree.x()+5);x++)
            for(int z=Math.max(z0,tree.z()-5);z<=Math.min(z0+15,tree.z()+5);z++)
            for(int global=tree.y();global<=tree.y()+tree.height()+2;global++){
                int y=global-yOffset;if(y<chunk.getBottomY()||y>=chunk.getTopY())continue;
                int mat=terrain.treeBlock(tree,x,global,z);
                if(mat!=MoonTerrain.AIR&&chunk.getBlockState(pos.set(x,y,z)).isAir())
                    chunk.setBlockState(pos,state(mat),false);
            }
        }
        Heightmap.populateHeightmaps(chunk,Set.of(Heightmap.Type.OCEAN_FLOOR_WG,Heightmap.Type.WORLD_SURFACE_WG));
        return CompletableFuture.completedFuture(chunk);
    }
    @Override public int getHeight(int x,int z,Heightmap.Type type,HeightLimitView world,NoiseConfig config){
        MoonTerrain terrain=terrain(config);
        var col=terrain.column(x,z);
        int hi=Math.min(world.getTopY()-1,col.top()-yOffset),lo=Math.max(world.getBottomY(),col.bottom()-yOffset);
        for(int y=hi;y>=lo;y--){int global=y+yOffset;if(global>=col.innerBottom()&&global<=col.innerTop()){y=col.innerBottom()-yOffset;continue;}if(terrain.material(col,x,global,z)!=MoonTerrain.AIR)return y+1;}
        return world.getBottomY();
    }
    @Override public VerticalBlockSample getColumnSample(int x,int z,HeightLimitView world,NoiseConfig config){
        MoonTerrain terrain=terrain(config);
        BlockState[] result=new BlockState[world.getHeight()];
        var col=terrain.column(x,z);
        for(int i=0;i<result.length;i++)result[i]=state(terrain.material(col,x,world.getBottomY()+i+yOffset,z));
        return new VerticalBlockSample(world.getBottomY(),result);
    }
}
