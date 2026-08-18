package org.silverbreezed.whitehole.block;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.silverbreezed.whitehole.Constants;
import org.silverbreezed.whitehole.registry.block.ModBlocks;

public class ModBlocksForge {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Constants.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Constants.MOD_ID);

    public static final RegistryObject<Block> WHITE_HOLE_ALTAR = BLOCKS.register(
            "white_hole_altar",
            ModBlocks::createAltarBlock
    );

    public static final RegistryObject<Item> WHITE_HOLE_ALTAR_ITEM = ITEMS.register(
            "white_hole_altar",
            () -> ModBlocks.createAltarBlockItem(WHITE_HOLE_ALTAR.get())
    );

    public static void register(BusGroup eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}
