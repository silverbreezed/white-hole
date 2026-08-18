package org.silverbreezed.whitehole.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.silverbreezed.whitehole.Constants;
import org.silverbreezed.whitehole.registry.item.ModItems;

public class ModItemsForge {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Constants.MOD_ID);

    public static final RegistryObject<Item> COSMIC_EYE = ITEMS.register(
            "cosmic_eye",
            () -> ModItems.COSMIC_EYE
    );

    public static void register(BusGroup eventBus) {
        ITEMS.register(eventBus);
    }
}
