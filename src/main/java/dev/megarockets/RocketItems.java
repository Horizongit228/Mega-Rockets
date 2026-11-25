package dev.megarockets;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
public final class RocketItems {
    public static final Item TITAN=Registry.register(Registries.ITEM,MegaRockets.id("titan_1"),new RocketItem(new Item.Settings().maxCount(1),MegaRockets.TITAN));
    public static final Item PHANTOM=Registry.register(Registries.ITEM,MegaRockets.id("phantom"),new RocketItem(new Item.Settings().maxCount(1),MegaRockets.PHANTOM));
    public static final Item HYPERION=Registry.register(Registries.ITEM,MegaRockets.id("hyperion"),new RocketItem(new Item.Settings().maxCount(1),MegaRockets.HYPERION));
    private RocketItems(){}
    public static void init(){}
}
