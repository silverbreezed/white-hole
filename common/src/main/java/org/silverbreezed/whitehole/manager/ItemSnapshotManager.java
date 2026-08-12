package org.silverbreezed.whitehole.manager;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.silverbreezed.whitehole.Constants;
import org.silverbreezed.whitehole.config.ModConfig;
import org.silverbreezed.whitehole.enums.RecoveryReason;
import org.silverbreezed.whitehole.event.RecoverySnapshot;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central cache + persistence for recoverable item snapshots, regardless of why they were
 * captured. Capture triggers (VoidDeathCaptureTrigger today, a future DespawnCaptureTrigger,
 * etc.) all report into {@link #capture}; this class never needs to know which trigger
 * produced a snapshot, or be touched again when a new trigger is added.
 *
 * Renamed and generalized from VoidDeathHandler as part of the void-only -> multi-reason
 * refactor. See getSaveFile() for the one-time migration of pre-refactor data.
 */
public class ItemSnapshotManager {
    private static final Map<UUID, LinkedList<RecoverySnapshot>> IN_MEMORY_CACHE = new ConcurrentHashMap<>();

    public static void loadPlayerDataAsync(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Level level = player.level();

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            File file = getSaveFile(level, uuid);
            if (file.exists() && file.length() > 0) {
                LinkedList<RecoverySnapshot> snapshots = loadSnapshotsFromDisk(level, uuid);
                if (!snapshots.isEmpty()) {
                    IN_MEMORY_CACHE.put(uuid, snapshots);
                    Constants.LOG.info("Data loaded into cache for " + player.getName().getString());
                }
            }
        }, org.silverbreezed.whitehole.manager.AsyncIOManager.IO_EXECUTOR);
    }

    public static LinkedList<RecoverySnapshot> loadSnapshotsFromDisk(Level level, UUID playerUUID) {
        LinkedList<RecoverySnapshot> records = new LinkedList<>();
        File file = getSaveFile(level, playerUUID);
        if (!file.exists()) return records;

        HolderLookup.Provider provider = level.registryAccess();
        RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, provider);

        try (java.io.FileReader reader = new java.io.FileReader(file)) {
            com.google.gson.JsonObject root = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();

            // "snapshots" is the current schema key. "death_records" is read as a fallback so
            // files written before this refactor (void-death-only) still load correctly.
            com.google.gson.JsonArray recordsArray = root.has("snapshots")
                    ? root.getAsJsonArray("snapshots")
                    : (root.has("death_records") ? root.getAsJsonArray("death_records") : null);

            if (recordsArray != null) {
                for (com.google.gson.JsonElement recordElement : recordsArray) {
                    com.google.gson.JsonObject recordObj = recordElement.getAsJsonObject();
                    long timestamp = recordObj.has("timestamp") ? recordObj.get("timestamp").getAsLong() : 0L;

                    RecoveryReason reason = RecoveryReason.VOID_DEATH;
                    if (recordObj.has("reason")) {
                        try {
                            reason = RecoveryReason.valueOf(recordObj.get("reason").getAsString());
                        } catch (IllegalArgumentException ignored) {
                            // Unknown reason - likely written by a newer mod version. Keep the
                            // default rather than discarding the whole snapshot.
                        }
                    }

                    List<ItemStack> items = new java.util.ArrayList<>();
                    if (recordObj.has("items")) {
                        com.google.gson.JsonArray itemsArray = recordObj.getAsJsonArray("items");
                        for (com.google.gson.JsonElement itemElement : itemsArray) {
                            ItemStack.CODEC.parse(ops, itemElement).result().ifPresent(items::add);
                        }
                    }
                    records.add(new RecoverySnapshot(timestamp, reason, items));
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

        File snapshotDir = new File(whiteHoleDir, "snapshots");
        if (!snapshotDir.exists()) {
            snapshotDir.mkdirs();
        }

        File newFile = new File(snapshotDir, playerUUID.toString() + ".json");

        // One-time migration from the pre-refactor void-only layout ("<root>/void/<uuid>.json").
        // Safe to call every time: it's a no-op once the file has already been moved.
        if (!newFile.exists()) {
            File legacyFile = new File(new File(whiteHoleDir, "void"), playerUUID.toString() + ".json");
            if (legacyFile.exists() && legacyFile.renameTo(newFile)) {
                Constants.LOG.info("[White Hole] Migrated legacy void snapshot for " + playerUUID);
            }
        }

        return newFile;
    }

    /**
     * Stores a captured batch of items for later recovery at the altar. Shared by every
     * capture trigger so the maxSavedItemSnapshots cap and persistence behave identically
     * no matter why the items were captured.
     *
     * @return true if the snapshot was stored, false if the player's snapshot capacity is
     *         full or there was nothing to capture.
     */
    public static boolean capture(RecoveryReason reason, ServerPlayer player, List<ItemStack> items) {
        if (items.isEmpty()) return false;

        UUID playerUUID = player.getUUID();
        Level level = player.level();
        ModConfig config = ConfigManager.getModConfig();

        LinkedList<RecoverySnapshot> records = IN_MEMORY_CACHE.computeIfAbsent(playerUUID, k -> new LinkedList<>());

        if (records.size() >= config.maxSavedItemSnapshots) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§c[§lWhite Hole§r§c] White Hole Capacity is Full and your items is not saved! You must bring back your past item."));
            return false;
        }

        records.addLast(new RecoverySnapshot(System.currentTimeMillis(), reason, items));
        saveSnapshotsToDiskAsync(level, playerUUID, records);

        return true;
    }

    public static RecoverySnapshot popLastSnapshot(Level level, UUID playerUUID) {
        LinkedList<RecoverySnapshot> records = IN_MEMORY_CACHE.get(playerUUID);

        if (records != null && !records.isEmpty()) {
            RecoverySnapshot last = records.removeLast();

            if (records.isEmpty()) {
                File file = getSaveFile(level, playerUUID);
                org.silverbreezed.whitehole.manager.AsyncIOManager.deleteFileAsync(file);
            } else {
                saveSnapshotsToDiskAsync(level, playerUUID, records);
            }

            return last;
        }

        return null;
    }

    public static LinkedList<RecoverySnapshot> getAndClearSavedItems(Level level, UUID playerUUID) {
        LinkedList<RecoverySnapshot> items = IN_MEMORY_CACHE.remove(playerUUID);

        File file = getSaveFile(level, playerUUID);
        org.silverbreezed.whitehole.manager.AsyncIOManager.deleteFileAsync(file);

        return items != null ? items : new LinkedList<>();
    }

    private static void saveSnapshotsToDiskAsync(Level level, UUID playerUUID, LinkedList<RecoverySnapshot> records) {
        File file = getSaveFile(level, playerUUID);
        HolderLookup.Provider provider = level.registryAccess();
        RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, provider);

        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        com.google.gson.JsonArray recordsArray = new com.google.gson.JsonArray();

        for (RecoverySnapshot record : records) {
            com.google.gson.JsonObject recordObj = new com.google.gson.JsonObject();
            recordObj.addProperty("timestamp", record.getTimestamp());
            recordObj.addProperty("reason", record.getReason().name());

            com.google.gson.JsonArray itemsArray = new com.google.gson.JsonArray();
            for (ItemStack stack : record.getItems()) {
                ItemStack.CODEC.encodeStart(ops, stack).result().ifPresent(itemsArray::add);
            }
            recordObj.add("items", itemsArray);

            recordsArray.add(recordObj);
        }

        root.add("snapshots", recordsArray);

        org.silverbreezed.whitehole.manager.AsyncIOManager.writeJsonAsync(file, root).thenRun(() -> {
            Constants.LOG.info("[White Hole IO] Snapshot berhasil disimpan asinkron untuk: " + playerUUID);
        });
    }
}