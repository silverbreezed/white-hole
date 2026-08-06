package org.silverbreezed.whitehole.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class VoidDeathHandler {

    private static final HashMap<UUID, List<ItemStack>> VOID_SAVED_ITEMS = new HashMap<>();

    public static boolean handlePlayerVoidDeath(ServerPlayer player, DamageSource source) {
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            UUID playerUUID = player.getUUID();

            // 1. STRATEGI GABUNG: Ambil daftar lama jika ada, jika tidak ada buat daftar baru kosong
            List<ItemStack> savedInventory = VOID_SAVED_ITEMS.getOrDefault(playerUUID, new ArrayList<>());

            // 2. Salin isi inventaris saat ini dan gabungkan ke dalam daftar tersebut
            boolean hasNewItems = false;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty()) {
                    savedInventory.add(stack.copy());
                    hasNewItems = true;
                }
            }

            // 3. Masukkan kembali daftar yang sudah digabungkan ke dalam memori map
            if (hasNewItems || !savedInventory.isEmpty()) {
                VOID_SAVED_ITEMS.put(playerUUID, savedInventory);

                // Kosongkan tubuh pemain agar item vanilla tidak drop ke Void
                player.getInventory().clearContent();
                return true;
            }
        }
        return false;
    }

    public static List<ItemStack> getAndClearSavedItems(UUID playerUUID) {
        return VOID_SAVED_ITEMS.remove(playerUUID);
    }

    public static boolean hasSavedItems(UUID playerUUID) {
        return VOID_SAVED_ITEMS.containsKey(playerUUID) && !VOID_SAVED_ITEMS.get(playerUUID).isEmpty();
    }
}
