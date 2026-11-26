package dev.megarockets;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class OxygenSystem {
    private static final RegistryKey<DamageType> VACUUM=RegistryKey.of(RegistryKeys.DAMAGE_TYPE,MegaRockets.id("vacuum"));
    private static final String TANK="MegaRocketsOxygen";
    private record Display(ServerPlayerEntity player,ServerBossBar bar){}
    private static final Map<UUID,Display> BARS=new HashMap<>();
    private OxygenSystem(){}
    public static void init(){
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(ServerPlayerEntity player:server.getPlayerManager().getPlayerList())tick(player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->remove(handler.player.getUuid()));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server->{
            for(Display display:BARS.values())display.bar().clearPlayers();
            BARS.clear();
        });
    }
    public static boolean vacuum(ServerPlayerEntity p){
        World w=p.getWorld();var key=w.getRegistryKey();
        return MegaRockets.planet(w)!=null||key.equals(MegaRockets.ORBIT)||
            key.equals(World.OVERWORLD)&&p.getY()>=OxygenRules.ALTITUDE;
    }
    public static boolean sealed(ServerPlayerEntity p){
        return p.getEquippedStack(EquipmentSlot.HEAD).isOf(SpaceSuitItems.HELMET)&&
               p.getEquippedStack(EquipmentSlot.CHEST).isOf(SpaceSuitItems.CHEST)&&
               p.getEquippedStack(EquipmentSlot.LEGS).isOf(SpaceSuitItems.LEGS)&&
               p.getEquippedStack(EquipmentSlot.FEET).isOf(SpaceSuitItems.BOOTS);
    }
    public static int oxygen(ItemStack tank){
        NbtComponent data=tank.get(DataComponentTypes.CUSTOM_DATA);
        if(data==null)return OxygenRules.CAPACITY;
        var nbt=data.copyNbt();
        return nbt.contains(TANK)?OxygenRules.clamp(nbt.getInt(TANK)):OxygenRules.CAPACITY;
    }
    public static void refill(ItemStack tank){if(tank.isOf(SpaceSuitItems.CHEST))store(tank,OxygenRules.CAPACITY);}
    private static void store(ItemStack tank,int units){
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA,tank,nbt->nbt.putInt(TANK,OxygenRules.clamp(units)));
    }
    private static void remove(UUID id){
        Display old=BARS.remove(id);if(old!=null)old.bar().clearPlayers();
    }
    private static ServerBossBar display(ServerPlayerEntity p){
        Display current=BARS.get(p.getUuid());
        if(current!=null&&current.player()==p)return current.bar();
        remove(p.getUuid());
        ServerBossBar bar=new ServerBossBar(Text.translatable("oxygen.megarockets.atmosphere"),BossBar.Color.BLUE,BossBar.Style.PROGRESS);
        bar.addPlayer(p);BARS.put(p.getUuid(),new Display(p,bar));return bar;
    }
    private static void tick(ServerPlayerEntity p){
        if(p.interactionManager.getGameMode()!=GameMode.SURVIVAL||!p.isAlive()){remove(p.getUuid());return;}
        boolean cabin=p.getVehicle() instanceof TitanRocket;
        boolean vacuum=vacuum(p)&&!cabin,sealed=sealed(p);
        ItemStack tank=p.getEquippedStack(EquipmentSlot.CHEST);
        boolean equipped=tank.isOf(SpaceSuitItems.CHEST);
        int units=equipped?oxygen(tank):0;
        if(equipped){
            int next=OxygenRules.next(units,vacuum,sealed,p.age%20==0);
            if(next!=units){store(tank,next);units=next;}
        }
        boolean danger=OxygenRules.suffocating(vacuum,sealed,units);
        ServerBossBar bar=display(p);
        if(danger){
            bar.setColor(BossBar.Color.RED);bar.setPercent(0);
            bar.setName(Text.translatable(sealed?"oxygen.megarockets.empty":"oxygen.megarockets.unsealed"));
            if(p.age%OxygenRules.DAMAGE_INTERVAL==0){
                var registry=p.getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE);
                p.damage(new DamageSource(registry.entryOf(VACUUM)),OxygenRules.DAMAGE);
            }
        }else if(equipped){
            int percent=units*100/OxygenRules.CAPACITY;
            bar.setColor(percent<=15?BossBar.Color.RED:percent<=30?BossBar.Color.YELLOW:BossBar.Color.BLUE);
            bar.setPercent(units/(float)OxygenRules.CAPACITY);
            String clock=String.format(java.util.Locale.ROOT,"%02d:%02d",units/60,units%60);
            bar.setName(Text.translatable(vacuum?"oxygen.megarockets.remaining":cabin?"oxygen.megarockets.cabin_refill":"oxygen.megarockets.refilling",percent,clock));
        }else{
            bar.setColor(BossBar.Color.BLUE);bar.setPercent(1);
            bar.setName(Text.translatable(cabin?"oxygen.megarockets.cabin_safe":"oxygen.megarockets.atmosphere"));
        }
    }
}
