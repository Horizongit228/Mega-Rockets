package dev.megarockets;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
public final class RocketItems {
    public static final Item TITAN=Registry.register(Registries.ITEM,MegaRockets.id("titan_1"),new RocketItem(new Item.Settings().maxCount(1)));
    private RocketItems(){}
    public static void init(){}
}
