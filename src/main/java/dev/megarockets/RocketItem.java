package dev.megarockets;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Deploys the real Titan entity without changing any terrain blocks. */
public final class RocketItem extends Item {
    private final net.minecraft.entity.EntityType<TitanRocket> type;
    public RocketItem(Settings settings,net.minecraft.entity.EntityType<TitanRocket> type){super(settings);this.type=type;}
    @Override public ActionResult useOnBlock(ItemUsageContext context){
        var player=context.getPlayer();
        if(player==null||context.getSide()!=Direction.UP)return ActionResult.PASS;
        if(context.getWorld().isClient)return ActionResult.SUCCESS;
        // A 14-block-wide entity cannot fit directly under a player standing next to
        // a clicked block. Deploy ten blocks farther in the horizontal look direction.
        Vec3d forward=Vec3d.fromPolar(0,player.getYaw());
        BlockPos clicked=context.getBlockPos().up();
        BlockPos base=BlockPos.ofFloored(Vec3d.ofBottomCenter(clicked).add(forward.multiply(10)));
        return deploy((ServerWorld)context.getWorld(),player,context.getStack(),Vec3d.ofBottomCenter(base));
    }
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity player,Hand hand){
        ItemStack stack=player.getStackInHand(hand);
        // Air deployment is useful in zero gravity; on the surface require a block.
        if(!world.getRegistryKey().equals(MegaRockets.ORBIT))return TypedActionResult.pass(stack);
        if(world.isClient)return TypedActionResult.success(stack);
        Vec3d forward=Vec3d.fromPolar(0,player.getYaw());
        var result=deploy((ServerWorld)world,player,stack,player.getPos().add(forward.multiply(16)));
        return result==ActionResult.CONSUME?TypedActionResult.consume(stack):TypedActionResult.fail(stack);
    }
    private ActionResult deploy(ServerWorld world,PlayerEntity player,ItemStack stack,Vec3d base){
        TitanRocket ship=new TitanRocket(type,world);
        ship.refreshPositionAndAngles(base.x,base.y,base.z,0,0);
        BlockPos center=BlockPos.ofFloored(base);
        if(!world.getWorldBorder().contains(ship.getBoundingBox())||
            !world.canPlayerModifyAt(player,center)||
            !world.isSpaceEmpty(ship,ship.getBoundingBox())||
            !world.getOtherEntities(ship,ship.getBoundingBox()).isEmpty()){
            player.sendMessage(Text.translatable("message.megarockets.clearance"),true);
            return ActionResult.FAIL;
        }
        // On solid worlds, a rocket needs a surface beneath its center.
        if(!world.getRegistryKey().equals(MegaRockets.ORBIT)&&
            world.getBlockState(center.down()).getCollisionShape(world,center.down()).isEmpty()){
            player.sendMessage(Text.translatable("message.megarockets.rocket_ground"),true);
            return ActionResult.FAIL;
        }
        if(!world.spawnEntity(ship)){
            player.sendMessage(Text.translatable("message.megarockets.failed"),true);
            return ActionResult.FAIL;
        }
        if(!player.isCreative())stack.decrement(1);
        player.sendMessage(Text.translatable("message.megarockets.rocket_placed"),true);
        return ActionResult.CONSUME;
    }
}
