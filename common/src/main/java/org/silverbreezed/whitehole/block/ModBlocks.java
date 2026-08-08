package org.silverbreezed.whitehole.block;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class ModBlocks {

    // 1. Deklarasikan variabelnya saja tanpa langsung membuat objek (Kosongkan dulu)
    public static Block WHITE_HOLE_ALTAR;

    /**
     * Fungsi inisialisasi utama yang dipanggil oleh system loader platform
     */
    public static void registerAll() {
        // Daftarkan Blok ke dalam game resmi Minecraft
        WHITE_HOLE_ALTAR = registerBlock("white_hole_altar");
    }

    private static Block registerBlock(String name) {
        // Pembuatan ID Pengenal kosmik mod Anda
        Identifier id = Identifier.fromNamespaceAndPath("whitehole", name);

        // KRUSIAL UNTUK VERSI 26.2: Membuat Registry Key untuk Blok dan Item
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);

        // 2. Buat objek blok dengan menyuntikkan ID (.setId) terlebih dahulu ke propertinya
        Block block = new WhiteHoleAltarBlock(
                BlockBehaviour.Properties.of()
                        .setId(blockKey)
                        .mapColor(MapColor.COLOR_PURPLE)
                        .destroyTime(5.0F)
                        .explosionResistance(1200.0F)
                        .requiresCorrectToolForDrops()
                        .lightLevel(state -> state.getValue(WhiteHoleAltarBlock.ACTIVE) ? 15 : 0)

        );

        // Mendaftarkan fisik bloknya menggunakan RegistryKey bawaan 26.2
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

        // 3. Buat dan daftarkan BlockItem-nya dengan menyuntikkan ID Item juga
        BlockItem blockItem = new BlockItem(block, new Item.Properties().setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);

        return block;
    }
}
