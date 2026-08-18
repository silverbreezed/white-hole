package org.silverbreezed.whitehole.block;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.silverbreezed.whitehole.Constants;
import org.silverbreezed.whitehole.registry.block.ModBlocks;

public class ModBlocksNeoForge {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final DeferredBlock<Block> WHITE_HOLE_ALTAR = BLOCKS.register(
            "white_hole_altar",
            ModBlocks::createAltarBlock
    );

    public static final DeferredItem<Item> WHITE_HOLE_ALTAR_ITEM = ITEMS.register(
            "white_hole_altar",
            () -> ModBlocks.createAltarBlockItem(WHITE_HOLE_ALTAR.get())
    );

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}