package org.silverbreezed.whitehole.registry.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class ModBlocks {

    public static final ResourceKey<Block> ALTAR_BLOCK_KEY = ResourceKey.create(
            Registries.BLOCK, Identifier.fromNamespaceAndPath("whitehole", "white_hole_altar")
    );
    public static final ResourceKey<Item> ALTAR_ITEM_KEY = ResourceKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath("whitehole", "white_hole_altar")
    );

    public static Block createAltarBlock() {
        return new WhiteHoleAltarBlock(
                BlockBehaviour.Properties.of()
                        .setId(ALTAR_BLOCK_KEY)
                        .mapColor(MapColor.COLOR_PURPLE)
                        .destroyTime(5.0F)
                        .explosionResistance(1200.0F)
                        .requiresCorrectToolForDrops()
                        .lightLevel(state -> state.getValue(WhiteHoleAltarBlock.ACTIVE) ? 15 : 0)
        );
    }

    public static BlockItem createAltarBlockItem(Block blockTarget) {
        return new BlockItem(
                blockTarget,
                new Item.Properties().setId(ALTAR_ITEM_KEY)
        );
    }
}