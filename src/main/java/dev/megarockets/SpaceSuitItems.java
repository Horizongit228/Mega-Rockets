package dev.megarockets;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import java.util.List;
import java.util.Map;

public final class SpaceSuitItems {
    public static final RegistryEntry<ArmorMaterial> MATERIAL=Registry.registerReference(Registries.ARMOR_MATERIAL,
        MegaRockets.id("space_suit"),new ArmorMaterial(
        Map.of(ArmorItem.Type.HELMET,2,ArmorItem.Type.CHESTPLATE,5,ArmorItem.Type.LEGGINGS,4,
               ArmorItem.Type.BOOTS,2,ArmorItem.Type.BODY,4),12,SoundEvents.ITEM_ARMOR_EQUIP_IRON,
        ()->Ingredient.ofItems(Items.IRON_INGOT),List.of(new ArmorMaterial.Layer(MegaRockets.id("space_suit"),"",false)),0f,0f));
    public static final Item HELMET=register("space_helmet",ArmorItem.Type.HELMET,240);
    public static final Item CHEST=register("space_chestplate",ArmorItem.Type.CHESTPLATE,352);
    public static final Item LEGS=register("space_leggings",ArmorItem.Type.LEGGINGS,330);
    public static final Item BOOTS=register("space_boots",ArmorItem.Type.BOOTS,286);
    private static Item register(String name,ArmorItem.Type type,int durability){
        return Registry.register(Registries.ITEM,MegaRockets.id(name),new ArmorItem(MATERIAL,type,new Item.Settings().maxDamage(durability)));
    }
    public static void init(){}
}
