package org.silverbreezed.whitehole.event;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.silverbreezed.whitehole.Constants;
import org.silverbreezed.whitehole.config.ModConfig;
import org.silverbreezed.whitehole.manager.ConfigManager;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VoidDeathHandler {
    private static final Map<UUID, List<ItemStack>> IN_MEMORY_CACHE = new ConcurrentHashMap<>();

    public static void loadPlayerDataAsync(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Level level = player.level();

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            File file = getSaveFile(level, uuid);
            if (file.exists() && file.length() > 0) {
                List<ItemStack> items = getSavedItemsFromDisk(level, uuid);
                if (!items.isEmpty()) {
                    IN_MEMORY_CACHE.put(uuid, items);
                    Constants.LOG.info("Data loaded into cache for " + player.getName().getString());
                }
            }
        }, org.silverbreezed.whitehole.manager.AsyncIOManager.IO_EXECUTOR);
    }

    public static void unloadPlayerData(ServerPlayer player) {
        if (IN_MEMORY_CACHE.remove(player.getUUID()) != null) {
            Constants.LOG.info("Data removed from cache for " + player.getName().getString() + " to free up memory.");
        }
    }

    private static File getSaveFile(Level level, UUID playerUUID) {
        File serverRoot = Objects.requireNonNull(level.getServer()).getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
        File dataDir = new File(serverRoot, "data");
        File whiteHoleDir = new File(dataDir, Constants.MOD_ID + "_data");

        File voidDir = new File(whiteHoleDir, "void");
        if (!voidDir.exists()) {
            voidDir.mkdirs();
        }

        return new File(voidDir, playerUUID.toString() + ".json");
    }

    public static boolean handlePlayerVoidDeath(ServerPlayer player, DamageSource source) {
        ServerLevel playerLevel = player.level();
        ModConfig modConfig = ConfigManager.getModConfig();

        if (source.is(DamageTypes.FELL_OUT_OF_WORLD) && ((playerLevel.dimension() == ServerLevel.OVERWORLD && modConfig.recoverItemFromOverworldVoid) || (playerLevel.dimension() == ServerLevel.END && modConfig.recoverItemFromEndVoid))) {
            UUID playerUUID = player.getUUID();
            Level level = player.level();

            List<ItemStack> savedInventory = new ArrayList<>();
            boolean hasNewItems = false;

            if (hasSavedItems(level, playerUUID)) {
                savedInventory.addAll(getSavedItemsFromDisk(level, playerUUID));
            }

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty()) {
                    savedInventory.add(stack.copy());
                    hasNewItems = true;
                }
            }

            if (hasNewItems) {
                IN_MEMORY_CACHE.put(playerUUID, new ArrayList<>(savedInventory));

                saveItemsToDisk(level, playerUUID, savedInventory);

                player.sendSystemMessage(Component.literal("§5[§lWhite Hole§r§5] §dYou death in the void. You can brings back your items using White Hole Altar in Ancient City."));
                player.getInventory().clearContent();

                System.out.println("Inventory successfully saved to JSON file for " + player.getName().getString());

                return true;
            }
        }
        return false;
    }

    public static List<ItemStack> getAndClearSavedItems(Level level, UUID playerUUID) {
        List<ItemStack> items = IN_MEMORY_CACHE.remove(playerUUID);

        File file = getSaveFile(level, playerUUID);
        org.silverbreezed.whitehole.manager.AsyncIOManager.deleteFileAsync(file);

        return items != null ? items : new ArrayList<>();
    }

    public static boolean hasSavedItems(Level level, UUID playerUUID) {
        return IN_MEMORY_CACHE.containsKey(playerUUID) && !IN_MEMORY_CACHE.get(playerUUID).isEmpty();
    }

    private static void saveItemsToDisk(Level level, UUID playerUUID, List<ItemStack> items) {
        File file = getSaveFile(level, playerUUID);
        HolderLookup.Provider provider = level.registryAccess();
        RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, provider);

        JsonObject root = new JsonObject();
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        for (ItemStack stack : items) {
            ItemStack.CODEC.encodeStart(ops, stack).result().ifPresent(array::add);
        }
        root.add("saved_items", array);

        org.silverbreezed.whitehole.manager.AsyncIOManager.writeJsonAsync(file, root).thenRun(() -> {
            Constants.LOG.info("Inventory successfully saved to disk asynchornously for " + playerUUID);
        });
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
                    ItemStack.CODEC.parse(ops, element).result().ifPresent(list::add);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
