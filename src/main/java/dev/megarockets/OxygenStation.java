package dev.megarockets;

import net.minecraft.block.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class OxygenStation extends Block {
    public static final Block STATION=Registry.register(Registries.BLOCK,MegaRockets.id("oxygen_station"),
        new OxygenStation(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).luminance(s->7)));
    static {Registry.register(Registries.ITEM,MegaRockets.id("oxygen_station"),new BlockItem(STATION,new Item.Settings()));}
    public static void init(){}
    private OxygenStation(Settings settings){super(settings);}
    @Override protected ItemActionResult onUseWithItem(ItemStack stack,BlockState state,World world,BlockPos pos,PlayerEntity player,Hand hand,BlockHitResult hit){
        return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected ActionResult onUse(BlockState state,World world,BlockPos pos,PlayerEntity player,BlockHitResult hit){
        if(world.isClient)return ActionResult.SUCCESS;
        ItemStack tank=player.getEquippedStack(EquipmentSlot.CHEST);
        if(!tank.isOf(SpaceSuitItems.CHEST)){
            player.sendMessage(Text.translatable("oxygen.megarockets.station_wear"),true);return ActionResult.CONSUME;
        }
        if(OxygenSystem.oxygen(tank)>=OxygenRules.CAPACITY){
            player.sendMessage(Text.translatable("oxygen.megarockets.station_full"),true);return ActionResult.CONSUME;
        }
        if(!player.isCreative()){
            if(player.getOffHandStack().isOf(Items.WATER_BUCKET)){
                player.setStackInHand(Hand.OFF_HAND,new ItemStack(Items.BUCKET));
            }else{
                var inventory=player.getInventory();int slot=-1;
                for(int i=0;i<inventory.size();i++)if(inventory.getStack(i).isOf(Items.WATER_BUCKET)){slot=i;break;}
                if(slot<0){player.sendMessage(Text.translatable("oxygen.megarockets.station_water"),true);return ActionResult.CONSUME;}
                inventory.setStack(slot,new ItemStack(Items.BUCKET));
            }
        }
        OxygenSystem.refill(tank);
        player.sendMessage(Text.translatable("oxygen.megarockets.station_done"),true);
        world.playSound(null,pos,net.minecraft.sound.SoundEvents.BLOCK_BEACON_POWER_SELECT,net.minecraft.sound.SoundCategory.BLOCKS,.7f,1.4f);
        return ActionResult.CONSUME;
    }
}
