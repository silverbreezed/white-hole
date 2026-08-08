package org.silverbreezed.whitehole.item;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import org.silverbreezed.whitehole.Constants;

public class ModItems {

    public static Item COSMIC_EYE;

    public static void registerAll() {
        Identifier id = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "cosmic_eye");
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);

        // Membuat item dengan efek kilau magis (Epic Rarity)
        COSMIC_EYE = new Item(new Item.Properties().setId(key).rarity(Rarity.EPIC));

        Registry.register(BuiltInRegistries.ITEM, key, COSMIC_EYE);
    }
}