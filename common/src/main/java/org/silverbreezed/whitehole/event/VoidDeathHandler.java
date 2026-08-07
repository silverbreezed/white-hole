package org.silverbreezed.whitehole.event;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class VoidDeathHandler {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Fungsi untuk mendapatkan folder simpanan khusus di dalam folder world server mabar Anda
    private static File getSaveFile(Level level, UUID playerUUID) {
        // 1. Ambil folder pangkalan utama server mabar (Dedicated Server root)
        File serverRoot = Objects.requireNonNull(level.getServer()).getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();

        // 2. Arahkan secara dinamis ke dalam sub-folder penyimpanan duniaaktif resmi ("world/data/")
        File dataDir = new File(serverRoot, "data");

        // 3. Buat folder kustom "whitehole_data" tepat di dalam folder world/data/ tersebut
        File whiteHoleDir = new File(dataDir, "whitehole_data");
        if (!whiteHoleDir.exists()) {
            whiteHoleDir.mkdirs();
        }

        return new File(whiteHoleDir, playerUUID.toString() + ".json");
    }


    /**
     * 1. PENCEGAT KEMATIAN: Menangkap item dan langsung menulisnya ke file JSON fisik di harddisk!
     */
    public static boolean handlePlayerVoidDeath(ServerPlayer player, DamageSource source) {
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            UUID playerUUID = player.getUUID();
            Level level = player.level();

            List<ItemStack> savedInventory = new ArrayList<>();
            boolean hasNewItems = false;

            // Membaca item lama jika ada (Sistem Penggabungan/Merge Mandiri)
            if (hasSavedItems(level, playerUUID)) {
                savedInventory.addAll(getSavedItemsFromDisk(level, playerUUID));
            }

            // Menyalin seluruh isi tas pemain saat ini
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty()) {
                    savedInventory.add(stack.copy());
                    hasNewItems = true;
                }
            }

            if (hasNewItems) {
                // MENULIS DATA KE DISK SECARA MANDIRI
                saveItemsToDisk(level, playerUUID, savedInventory);

                System.out.println("[White Hole STDOUT]: Inventory successfully saved to custom JSON for " + player.getName().getString());
                player.getInventory().clearContent();
                return true;
            }
        }
        return false;
    }

    /**
     * 2. LOGIKA UTAMA AMBIL BARANG UNTUK ALTAR
     */
    public static List<ItemStack> getAndClearSavedItems(Level level, UUID playerUUID) {
        List<ItemStack> items = getSavedItemsFromDisk(level, playerUUID);
        // Hapus file JSON fisik setelah barang berhasil dimuntahkan Altar (Anti-Duplikasi)
        File file = getSaveFile(level, playerUUID);
        if (file.exists()) {
            file.delete();
        }
        return items;
    }

    /**
     * 3. PENGECEKAN KETERSEDIAAN DATA UNTUK ALTAR
     */
    public static boolean hasSavedItems(Level level, UUID playerUUID) {
        File file = getSaveFile(level, playerUUID);
        return file.exists() && file.length() > 0;
    }

    // --- MEKANISME EKSTERNAL BACA-TULIS FILE JSON FISIK (STANDAR 26.2 MURNI) ---

    private static void saveItemsToDisk(Level level, UUID playerUUID, List<ItemStack> items) {
        File file = getSaveFile(level, playerUUID);
        HolderLookup.Provider provider = level.registryAccess();
        RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, provider);

        try (FileWriter writer = new FileWriter(file)) {
            JsonObject root = new JsonObject();
            com.google.gson.JsonArray array = new com.google.gson.JsonArray();

            for (ItemStack stack : items) {
                // Mengonversi komponen data item modern 26.2 menjadi teks JSON bersih
                ItemStack.CODEC.encodeStart(ops, stack).result().ifPresent(array::add);
            }

            root.add("saved_items", array);
            GSON.toJson(root, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<ItemStack> getSavedItemsFromDisk(Level level, UUID playerUUID) {
        List<ItemStack> list = new ArrayList<>();
        File file = getSaveFile(level, playerUUID);
        if (!file.exists()) return list;

        HolderLookup.Provider provider = level.registryAccess();
        RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, provider);

        try (FileReader reader = new FileReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("saved_items")) {
                com.google.gson.JsonArray array = root.getAsJsonArray("saved_items");
                for (com.google.gson.JsonElement element : array) {
                    // Memuat kembali komponen data item dari teks JSON fisik
                    ItemStack.CODEC.parse(ops, element).result().ifPresent(list::add);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
