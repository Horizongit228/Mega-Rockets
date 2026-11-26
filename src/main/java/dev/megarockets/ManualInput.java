package dev.megarockets;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record ManualInput(int mask) implements CustomPayload {
 public static final Id<ManualInput> ID=new Id<>(MegaRockets.id("manual_input"));
 public static final PacketCodec<RegistryByteBuf,ManualInput> CODEC=new PacketCodec<>(){
  @Override public ManualInput decode(RegistryByteBuf buf){return new ManualInput(buf.readVarInt());}
  @Override public void encode(RegistryByteBuf buf,ManualInput value){buf.writeVarInt(value.mask());}
 };
 @Override public Id<ManualInput> getId(){return ID;}
 public static void init(){
  PayloadTypeRegistry.playC2S().register(ID,CODEC);
  ServerPlayNetworking.registerGlobalReceiver(ID,(payload,context)->{
   var player=context.player();
   if(player.getVehicle() instanceof TitanRocket rocket&&rocket.getFirstPassenger()==player)
    rocket.manualInput(player,payload.mask());
  });
 }
}
