package org.silverbreezed.whitehole.item;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public class ModItemsFabric {

    public static Item COSMIC_EYE;
    public static void register() {
        COSMIC_EYE = Registry.register(
                BuiltInRegistries.ITEM,
                ModItems.COSMIC_EYE_KEY,
                ModItems.COSMIC_EYE
        );
    }
}