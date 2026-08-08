package org.silverbreezed.whitehole.block;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import org.silverbreezed.whitehole.block.ModBlocks; // Import Pabrik Anda

public class ModBlocksFabric {

    // Menyimpan referensi objek yang sudah didaftarkan (opsional, berguna untuk referensi nanti)
    public static Block WHITE_HOLE_ALTAR;
    public static BlockItem WHITE_HOLE_ALTAR_ITEM;

    /**
     * Dipanggil HANYA saat Fabric Loader memulai fase inisialisasi utama
     */
    public static void register() {

        // 1. Panggil pabrik Common untuk merakit fisik Blok
        Block altarBlock = ModBlocks.createAltarBlock();

        // 2. Daftarkan secara langsung (Imperatif) menggunakan kunci dari Common
        WHITE_HOLE_ALTAR = Registry.register(
                BuiltInRegistries.BLOCK,
                ModBlocks.ALTAR_BLOCK_KEY,
                altarBlock
        );

        // 3. Panggil pabrik Common untuk merakit Item, lalu daftarkan
        WHITE_HOLE_ALTAR_ITEM = Registry.register(
                BuiltInRegistries.ITEM,
                ModBlocks.ALTAR_ITEM_KEY,
                ModBlocks.createAltarBlockItem(WHITE_HOLE_ALTAR)
        );
    }
}