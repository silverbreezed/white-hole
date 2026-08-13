package org.silverbreezed.whitehole.block;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import org.silverbreezed.whitehole.registry.block.ModBlocks;

public class ModBlocksFabric {

    public static Block WHITE_HOLE_ALTAR;
    public static BlockItem WHITE_HOLE_ALTAR_ITEM;

    public static void register() {

        Block altarBlock = ModBlocks.createAltarBlock();

        WHITE_HOLE_ALTAR = Registry.register(
                BuiltInRegistries.BLOCK,
                ModBlocks.ALTAR_BLOCK_KEY,
                altarBlock
        );

        WHITE_HOLE_ALTAR_ITEM = Registry.register(
                BuiltInRegistries.ITEM,
                ModBlocks.ALTAR_ITEM_KEY,
                ModBlocks.createAltarBlockItem(WHITE_HOLE_ALTAR)
        );
    }
}