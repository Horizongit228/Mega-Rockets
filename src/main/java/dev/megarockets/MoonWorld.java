package dev.megarockets;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import java.util.WeakHashMap;
import java.util.Set;
import java.util.Collections;

/** A real, walkable miniature Moon in the shared space dimension. */
public final class MoonWorld {
    public static final int X=6000, Z=0, CENTER_Y=56, RADIUS=48, LAND_Y=101, CRUISE_Y=180;
    private static final int SIDE=RADIUS*2+1, VOLUME=SIDE*SIDE*SIDE;
    private static final BlockPos MARKER=new BlockPos(X,CENTER_Y,Z);
    private static final WeakHashMap<ServerWorld,Integer> progress=new WeakHashMap<>();
    private static final Set<ServerWorld> ready=Collections.newSetFromMap(new WeakHashMap<>());
    private MoonWorld() {}

    public static double gravityFactor(double x,double y,double z) {
        double dx=x-X, dz=z-Z, dy=y-CENTER_Y;
        double d=Math.sqrt(dx*dx+dy*dy+dz*dz);
        if(d<=85) return .165;
        return .165*Math.max(0,1-(d-85)/100);
    }
    public static void request(ServerWorld world) {
        if(ready.contains(world)||progress.containsKey(world)) return;
        if(world.getBlockState(MARKER).isOf(Blocks.BEDROCK)) ready.add(world);
        else progress.put(world,0);
    }
    public static boolean ready(ServerWorld world) { return ready.contains(world); }

    public static void tick(ServerWorld world) {
        Integer start=progress.get(world);
        if(start==null) return;
        int i=start, scanned=0, placed=0;
        BlockPos.Mutable pos=new BlockPos.Mutable();
        // Bounded work per tick; build well before the arriving rocket.
        while(i<VOLUME && scanned++<14000 && placed<384) {
            int index=i++;
            int dx=index%SIDE-RADIUS, dz=(index/SIDE)%SIDE-RADIUS, dy=index/(SIDE*SIDE)-RADIUS;
            int d2=dx*dx+dy*dy+dz*dz, y=CENTER_Y+dy;
            if(d2>RADIUS*RADIUS||y>=LAND_Y) continue;
            boolean pad=dx*dx+dz*dz<=20*20 && y>=LAND_Y-4;
            // Hollow solid crust, thick enough to walk on and mine. Flatten landing area.
            if(!pad && d2<(RADIUS-4)*(RADIUS-4)) continue;
            pos.set(X+dx,y,Z+dz);
            if(!world.getBlockState(pos).isAir()) continue;
            int hash=Math.floorMod(dx*73428767 ^ dy*19349663 ^ dz*83492791,100);
            BlockState state=pad && y==LAND_Y-1 ? Blocks.SMOOTH_STONE.getDefaultState()
                : hash<8 ? Blocks.ANDESITE.getDefaultState() : hash<22 ? Blocks.TUFF.getDefaultState()
                : Blocks.STONE.getDefaultState();
            world.setBlockState(pos,state,Block.NOTIFY_LISTENERS|Block.FORCE_STATE);
            placed++;
        }
        if(i>=VOLUME) {
            world.setBlockState(MARKER,Blocks.BEDROCK.getDefaultState(),Block.NOTIFY_LISTENERS|Block.FORCE_STATE);
            progress.remove(world); ready.add(world);
        } else progress.put(world,i);
    }
}
