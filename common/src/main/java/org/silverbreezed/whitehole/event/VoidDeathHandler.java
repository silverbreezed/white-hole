package org.silverbreezed.whitehole.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class VoidDeathHandler {

    // Kamus data untuk menyimpan manifes item yang selamat dari Void berdasarkan UUID Pemain
    private static final HashMap<UUID, List<ItemStack>> VOID_SAVED_ITEMS = new HashMap<>();

    /**
     * Logika utama yang harus dipanggil oleh jembatan event Fabric dan NeoForge
     */
    public static boolean handlePlayerVoidDeath(ServerPlayer player, DamageSource source) {
        // 1. Memeriksa apakah pemain berada di dimensi The End dan mati karena jatuh ke luar dunia (Void)
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            UUID playerUUID = player.getUUID();
            List<ItemStack> savedInventory = new ArrayList<>();

            // 2. Menyalin seluruh item dari inventaris pemain (Termasuk Armor dan Offhand)
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty()) {
                    // Membuat salinan persis lengkap dengan semua Data Components komponen mutakhirnya
                    savedInventory.add(stack.copy());
                }
            }

            // 3. Jika ada item yang diselamatkan, simpan ke memori dan kosongkan inventaris vanilla
            if (!savedInventory.isEmpty()) {
                VOID_SAVED_ITEMS.put(playerUUID, savedInventory);

                // Menghapus item dari tubuh pemain agar tidak menciptakan item drop vanilla yang hancur di Void
                player.getInventory().clearContent();

                // Mengirim pesan kosmik ke log server / chat konsol sebagai penanda ritual siap dilakukan
                System.out.println("Inventory secured for " + player.getName().getString());
                return true; // Berhasil dicegat
            }
        }
        return false;
    }

    /**
     * Fungsi untuk mengambil kembali item yang disimpan saat Altar diaktifkan
     */
    public static List<ItemStack> getAndClearSavedItems(UUID playerUUID) {
        return VOID_SAVED_ITEMS.remove(playerUUID);
    }

    /**
     * Memeriksa apakah pemain memiliki simpanan barang di lubang putih
     */
    public static boolean hasSavedItems(UUID playerUUID) {
        return VOID_SAVED_ITEMS.containsKey(playerUUID) && !VOID_SAVED_ITEMS.get(playerUUID).isEmpty();
    }
}
