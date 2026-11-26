package dev.megarockets;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record CockpitAction(int action) implements CustomPayload{
 public enum Control{MANUAL,LAUNCH,APPROACH,HOLD,ARM,LIGHTS,LIFE,THROTTLE_DOWN,THROTTLE_UP,BEACON,AUTO_NAV,RETURN_EARTH,STATION_NAV}
 public static final Id<CockpitAction> ID=new Id<>(MegaRockets.id("cockpit_action"));
 public static final PacketCodec<RegistryByteBuf,CockpitAction> CODEC=new PacketCodec<>(){
  @Override public CockpitAction decode(RegistryByteBuf buf){return new CockpitAction(buf.readVarInt());}
  @Override public void encode(RegistryByteBuf buf,CockpitAction value){buf.writeVarInt(value.action());}
 };
 @Override public Id<CockpitAction> getId(){return ID;}
 public static void init(){
  PayloadTypeRegistry.playC2S().register(ID,CODEC);
  ServerPlayNetworking.registerGlobalReceiver(ID,(payload,context)->{
   var player=context.player();int id=payload.action();
   if(id>=0&&id<Control.values().length&&player.getVehicle() instanceof TitanRocket ship&&ship.getFirstPassenger()==player&&!player.isSpectator())
    ship.cockpitAction(player,Control.values()[id]);
  });
 }
}
