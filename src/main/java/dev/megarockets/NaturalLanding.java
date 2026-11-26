package dev.megarockets;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Heightmap;

/** Server-side validation of actual loaded terrain, never edits blocks. */
public final class NaturalLanding {
    private NaturalLanding(){}
    private static boolean ground(BlockState b){
        return OuterPlanetBlocks.isGround(b)||b.isOf(MoonBlocks.ROCK)||b.isOf(MoonBlocks.REGOLITH)||b.isOf(MoonBlocks.BASALT)||b.isOf(MoonBlocks.ORE)||
            b.isOf(MarsBlocks.ROCK)||b.isOf(MarsBlocks.REGOLITH)||b.isOf(MarsBlocks.ORE);
    }
    private static int actualGround(ServerWorld world,int x,int z){
        world.getChunk(x>>4,z>>4);
        int y=world.getTopY(Heightmap.Type.WORLD_SURFACE,x,z);
        if(y<=world.getBottomY()+3||y+64>=world.getTopY())return LandingSurvey.INVALID;
        for(int depth=1;depth<=3;depth++)if(!ground(world.getBlockState(new BlockPos(x,y-depth,z))))return LandingSurvey.INVALID;
        return y;
    }
    public static LandingSurvey.Site find(ServerWorld world,TitanRocket rocket){
        var manager=world.getChunkManager();
        var generator=manager.getChunkGenerator();
        var noise=manager.getNoiseConfig();
        // Limit chunk loading to one plausible candidate per call. Sample generator first.
        for(int attempt=0;attempt<16;attempt++){
            var p=LandingSurvey.candidate(rocket.nextLandingCandidate());
            var forecast=LandingSurvey.inspect(p.x(),p.z(),(x,z)->generator.getHeight(x,z,Heightmap.Type.WORLD_SURFACE_WG,world,noise));
            if(forecast==null)continue;
            var site=LandingSurvey.inspect(p.x(),p.z(),(x,z)->actualGround(world,x,z));
            if(site==null)return null;
            double x=site.x()+.5,z=site.z()+.5,y=site.y();
            Box descent=new Box(x-7,y,z-7,x+7,y+64+LunarFlight.CRUISE_Y-LunarFlight.LAND_Y,z+7);
            if(descent.maxY>=world.getTopY()||!world.getWorldBorder().contains(descent)||
                !world.isSpaceEmpty(null,descent)||!world.getOtherEntities(null,descent).isEmpty())return null;
            return site;
        }
        return null;
    }
}
