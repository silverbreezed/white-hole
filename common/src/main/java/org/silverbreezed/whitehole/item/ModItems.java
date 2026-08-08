// Lokasi: modul COMMON
package org.silverbreezed.whitehole.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import org.silverbreezed.whitehole.Constants;

public class ModItems {

    public static final ResourceKey<Item> COSMIC_EYE_KEY = ResourceKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "cosmic_eye")
    );

    public static final Item COSMIC_EYE = new Item(new Item.Properties()
            .setId(COSMIC_EYE_KEY)
            .rarity(Rarity.EPIC)
    );
}