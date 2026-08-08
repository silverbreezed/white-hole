package org.silverbreezed.whitehole.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.silverbreezed.whitehole.Constants;

public class ModItemsNeoForge {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final DeferredItem<Item> COSMIC_EYE = ITEMS.register(
            "cosmic_eye",
            () -> ModItems.COSMIC_EYE
    );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}