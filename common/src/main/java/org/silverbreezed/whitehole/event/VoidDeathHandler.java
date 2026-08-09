package org.silverbreezed.whitehole.event;

import net.minecraft.core.HolderLookup;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VoidDeathHandler {
    private static final Map<UUID, LinkedList<DeathRecord>> IN_MEMORY_CACHE = new ConcurrentHashMap<>();

    public static void loadPlayerDataAsync(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Level level = player.level();

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            File file = getSaveFile(level, uuid);
            if (file.exists() && file.length() > 0) {
                LinkedList<DeathRecord> items = loadRecordsFromDisk(level, uuid);
                if (!items.isEmpty()) {
                    IN_MEMORY_CACHE.put(uuid, items);
                    Constants.LOG.info("Data loaded into cache for " + player.getName().getString());
                }
            }
        }, org.silverbreezed.whitehole.manager.AsyncIOManager.IO_EXECUTOR);
    }

    public static LinkedList<DeathRecord> loadRecordsFromDisk(Level level, UUID playerUUID) {
        LinkedList<DeathRecord> records = new LinkedList<>();
        File file = getSaveFile(level, playerUUID);
        if (!file.exists()) return records;

        HolderLookup.Provider provider = level.registryAccess();
        RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, provider);

        try (java.io.FileReader reader = new java.io.FileReader(file)) {
            com.google.gson.JsonObject root = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();

            if (root.has("death_records")) {
                com.google.gson.JsonArray recordsArray = root.getAsJsonArray("death_records");

                for (com.google.gson.JsonElement recordElement : recordsArray) {
                    com.google.gson.JsonObject recordObj = recordElement.getAsJsonObject();
                    long timestamp = recordObj.has("timestamp") ? recordObj.get("timestamp").getAsLong() : 0L;

                    List<ItemStack> items = new java.util.ArrayList<>();
                    if (recordObj.has("items")) {
                        com.google.gson.JsonArray itemsArray = recordObj.getAsJsonArray("items");
                        for (com.google.gson.JsonElement itemElement : itemsArray) {
                            ItemStack.CODEC.parse(ops, itemElement).result().ifPresent(items::add);
                        }
                    }
                    records.add(new DeathRecord(timestamp, items));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return records;
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

    public static DeathRecord popLastDeathRecord(Level level, UUID playerUUID) {
        LinkedList<DeathRecord> records = IN_MEMORY_CACHE.get(playerUUID);

        if (records != null && !records.isEmpty()) {
            DeathRecord lastDeath = records.removeLast();

            if (records.isEmpty()) {
                File file = getSaveFile(level, playerUUID);
                org.silverbreezed.whitehole.manager.AsyncIOManager.deleteFileAsync(file);
            } else {
                saveRecordsToDiskAsync(level, playerUUID, records);
            }

            return lastDeath;
        }

        return null;
    }

    public static boolean handlePlayerVoidDeath(ServerPlayer player, DamageSource source) {
        ServerLevel playerLevel = player.level();
        ModConfig modConfig = ConfigManager.getModConfig();

        if (source.is(DamageTypes.FELL_OUT_OF_WORLD) &&
                ((playerLevel.dimension() == ServerLevel.OVERWORLD && modConfig.recoverItemsFromOverworldVoid) ||
                (playerLevel.dimension() == ServerLevel.END && modConfig.recoverItemsFromEndVoid) ||
                (playerLevel.dimension() == ServerLevel.NETHER && modConfig.recoverItemsFromNetherVoid))
        ) {
            UUID playerUUID = player.getUUID();
            Level level = player.level();

            List<ItemStack> savedInventory = new ArrayList<>();
            boolean hasNewItems = false;

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty()) {
                    savedInventory.add(stack.copy());
                    hasNewItems = true;
                }
            }

            if (hasNewItems) {
                LinkedList<DeathRecord> records = IN_MEMORY_CACHE.computeIfAbsent(playerUUID, k -> new LinkedList<>());
                ModConfig config = ConfigManager.getModConfig();

                if (records.size() >= config.maxSavedItemSnapshots) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c[§lWhite Hole§r§c] White Hole Capacity is Full and your items is not saved! You must bring back your past item."));

                    return false;
                }

                records.addLast(new DeathRecord(System.currentTimeMillis(), savedInventory));

                saveRecordsToDiskAsync(level, playerUUID, records);

                player.getInventory().clearContent();

                return true;
            }
        }
        return false;
    }

    public static LinkedList<DeathRecord> getAndClearSavedItems(Level level, UUID playerUUID) {
        LinkedList<DeathRecord> items = IN_MEMORY_CACHE.remove(playerUUID);

        File file = getSaveFile(level, playerUUID);
        org.silverbreezed.whitehole.manager.AsyncIOManager.deleteFileAsync(file);

        return items != null ? items : new LinkedList<>();
    }

    private static void saveRecordsToDiskAsync(Level level, UUID playerUUID, LinkedList<DeathRecord> records) {
        File file = getSaveFile(level, playerUUID);
        HolderLookup.Provider provider = level.registryAccess();
        RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, provider);

        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        com.google.gson.JsonArray recordsArray = new com.google.gson.JsonArray();

        for (DeathRecord record : records) {
            com.google.gson.JsonObject recordObj = new com.google.gson.JsonObject();
            recordObj.addProperty("timestamp", record.getTimestamp());

            com.google.gson.JsonArray itemsArray = new com.google.gson.JsonArray();
            for (ItemStack stack : record.getItems()) {
                ItemStack.CODEC.encodeStart(ops, stack).result().ifPresent(itemsArray::add);
            }
            recordObj.add("items", itemsArray);

            recordsArray.add(recordObj);
        }

        root.add("death_records", recordsArray);

        org.silverbreezed.whitehole.manager.AsyncIOManager.writeJsonAsync(file, root).thenRun(() -> {
            org.silverbreezed.whitehole.Constants.LOG.info("[White Hole IO] Snapshot riwayat kematian berhasil disimpan asinkron untuk: " + playerUUID);
        });
    }
}
