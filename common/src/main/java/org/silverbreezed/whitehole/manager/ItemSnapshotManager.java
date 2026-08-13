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
import java.util.stream.Collectors;

/**
 * Central cache + persistence for recoverable item snapshots, regardless of why they were
 * captured. Capture triggers (VoidDeathCaptureTrigger, DespawnCaptureTrigger) all report
 * into {@link #capture}; this class never needs to know which trigger produced a snapshot,
 * or be touched again when a new trigger is added.
 *
 * On disk, snapshots are split into one file per RecoveryReason
 * ("whitehole_data/void/<uuid>.json", "whitehole_data/despawn/<uuid>.json") so a write
 * triggered by one reason never has to re-serialize the other reason's history - despawn
 * captures are expected to be far more frequent than void deaths, so this avoids constantly
 * rewriting unrelated void-death data. In memory, both reasons still live together in one
 * per-player list so the altar's "recover the most recent loss" behaviour and the shared
 * maxSavedItemSnapshots cap both stay reason-agnostic, exactly as before.
 *
 * See getSaveFile() / migrateFromCombinedFile() for the migration chain from older formats.
 */
public class ItemSnapshotManager {
    private static final Map<UUID, LinkedList<RecoverySnapshot>> IN_MEMORY_CACHE = new ConcurrentHashMap<>();

    public static void loadPlayerDataAsync(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Level level = player.level();

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            LinkedList<RecoverySnapshot> merged = new LinkedList<>();
            merged.addAll(loadReasonFromDisk(level, uuid, RecoveryReason.VOID_DEATH));
            merged.addAll(loadReasonFromDisk(level, uuid, RecoveryReason.DESPAWN));
            // The two reasons are read from separate files, so their relative chronological
            // order isn't preserved by simple concatenation - restore it explicitly. The
            // altar always recovers records.removeLast(), so this ordering is load-bearing.
            merged.sort(Comparator.comparingLong(RecoverySnapshot::getTimestamp));

            if (!merged.isEmpty()) {
                IN_MEMORY_CACHE.put(uuid, merged);
                Constants.LOG.info("Data loaded into cache for " + player.getName().getString());
            }
        }, org.silverbreezed.whitehole.manager.AsyncIOManager.IO_EXECUTOR);
    }

    public static List<RecoverySnapshot> loadReasonFromDisk(Level level, UUID playerUUID, RecoveryReason reason) {
        List<RecoverySnapshot> records = new ArrayList<>();
        File file = getSaveFile(level, playerUUID, reason);
        if (!file.exists()) return records;

        HolderLookup.Provider provider = level.registryAccess();
        RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, provider);

        try (java.io.FileReader reader = new java.io.FileReader(file)) {
            com.google.gson.JsonObject root = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();

            // "snapshots" is the current per-reason schema key. "death_records" is read as a
            // fallback so a pre-Tahap-1 legacy void/<uuid>.json (which happens to sit at the
            // exact same path our new void-reason file uses) still loads correctly.
            com.google.gson.JsonArray recordsArray = root.has("snapshots")
                    ? root.getAsJsonArray("snapshots")
                    : (root.has("death_records") ? root.getAsJsonArray("death_records") : null);

            if (recordsArray != null) {
                for (com.google.gson.JsonElement recordElement : recordsArray) {
                    com.google.gson.JsonObject recordObj = recordElement.getAsJsonObject();
                    long timestamp = recordObj.has("timestamp") ? recordObj.get("timestamp").getAsLong() : 0L;

                    // The containing directory already tells us the reason; an explicit
                    // "reason" field (when present) just double-checks it, since it's cheap
                    // to honour and guards against a file being moved between folders by hand.
                    RecoveryReason parsedReason = reason;
                    if (recordObj.has("reason")) {
                        try {
                            parsedReason = RecoveryReason.valueOf(recordObj.get("reason").getAsString());
                        } catch (IllegalArgumentException ignored) {
                            // Unknown reason - keep the directory-implied default.
                        }
                    }

                    List<ItemStack> items = new java.util.ArrayList<>();
                    if (recordObj.has("items")) {
                        com.google.gson.JsonArray itemsArray = recordObj.getAsJsonArray("items");
                        for (com.google.gson.JsonElement itemElement : itemsArray) {
                            ItemStack.CODEC.parse(ops, itemElement).result().ifPresent(items::add);
                        }
                    }
                    records.add(new RecoverySnapshot(timestamp, parsedReason, items));
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

    private static File getSaveFile(Level level, UUID playerUUID, RecoveryReason reason) {
        File serverRoot = Objects.requireNonNull(level.getServer()).getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
        File dataDir = new File(serverRoot, "data");
        File whiteHoleDir = new File(dataDir, Constants.MOD_ID + "_data");

        String subfolder = (reason == RecoveryReason.DESPAWN) ? "despawn" : "void";
        File reasonDir = new File(whiteHoleDir, subfolder);
        if (!reasonDir.exists()) {
            reasonDir.mkdirs();
        }

        File reasonFile = new File(reasonDir, playerUUID.toString() + ".json");

        // One-time migration from the Tahap-1 combined layout ("whitehole_data/snapshots/<uuid>.json",
        // mixed reasons in one file). No-op once already migrated. Note: a pre-Tahap-1 legacy
        // void-only file needs no separate migration step here - its path already matches this
        // method's own void-reason output path, and loadReasonFromDisk() already tolerates its
        // old "death_records" key.
        if (!reasonFile.exists()) {
            migrateFromCombinedFile(whiteHoleDir, playerUUID);
        }

        return reasonFile;
    }

    private static void migrateFromCombinedFile(File whiteHoleDir, UUID playerUUID) {
        File combinedFile = new File(new File(whiteHoleDir, "snapshots"), playerUUID.toString() + ".json");
        if (!combinedFile.exists()) return;

        // Pure JSON restructuring - deliberately does NOT decode ItemStacks here (that needs
        // a level's RegistryOps), it just routes each record's still-raw "items" array into
        // the right per-reason file based on its "reason" field.
        try (java.io.FileReader reader = new java.io.FileReader(combinedFile)) {
            com.google.gson.JsonObject root = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            com.google.gson.JsonArray recordsArray = root.has("snapshots")
                    ? root.getAsJsonArray("snapshots")
                    : (root.has("death_records") ? root.getAsJsonArray("death_records") : null);
            if (recordsArray == null) return;

            com.google.gson.JsonArray voidArray = new com.google.gson.JsonArray();
            com.google.gson.JsonArray despawnArray = new com.google.gson.JsonArray();

            for (com.google.gson.JsonElement element : recordsArray) {
                com.google.gson.JsonObject obj = element.getAsJsonObject();
                String reasonStr = obj.has("reason") ? obj.get("reason").getAsString() : RecoveryReason.VOID_DEATH.name();
                if (RecoveryReason.DESPAWN.name().equals(reasonStr)) {
                    despawnArray.add(obj);
                } else {
                    voidArray.add(obj);
                }
            }

            writeRawRecordsArray(new File(new File(whiteHoleDir, "void"), playerUUID + ".json"), voidArray);
            writeRawRecordsArray(new File(new File(whiteHoleDir, "despawn"), playerUUID + ".json"), despawnArray);

            // Fully distributed into the two new files now - remove the redundant combined one.
            combinedFile.delete();

            Constants.LOG.info("[White Hole] Split combined snapshot file into void/despawn for " + playerUUID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeRawRecordsArray(File file, com.google.gson.JsonArray recordsArray) throws java.io.IOException {
        if (recordsArray.isEmpty()) return;
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.add("snapshots", recordsArray);
        try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
            Constants.GSON.toJson(root, writer);
        }
    }

    /**
     * Stores a captured batch of items for later recovery at the altar. Shared by every
     * capture trigger so the maxSavedItemSnapshots cap and persistence behave identically
     * no matter why the items were captured.
     *
     * Takes a Level + UUID rather than a live ServerPlayer: unlike void death, a despawn
     * capture can fire well after the owning player has logged off, so we can't assume a
     * ServerPlayer instance exists. See notifyIfOnline() for the (best-effort) player message.
     *
     * @return true if the snapshot was stored, false if the player's snapshot capacity is
     *         full or there was nothing to capture.
     */
    public static boolean capture(RecoveryReason reason, Level level, UUID playerUUID, List<ItemStack> items) {
        if (items.isEmpty()) return false;

        ModConfig config = ConfigManager.getModConfig();

        LinkedList<RecoverySnapshot> records = IN_MEMORY_CACHE.computeIfAbsent(playerUUID, k -> new LinkedList<>());

        if (records.size() >= config.maxSavedItemSnapshots) {
            notifyIfOnline(level, playerUUID,
                    "§c[§lWhite Hole§r§c] White Hole Capacity is Full and your items is not saved! You must bring back your past item.");
            return false;
        }

        records.addLast(new RecoverySnapshot(System.currentTimeMillis(), reason, items));
        persistReason(level, playerUUID, reason, records);

        return true;
    }

    private static void notifyIfOnline(Level level, UUID playerUUID, String message) {
        if (level.getServer() == null) return;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerUUID);
        if (player != null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(message));
        }
    }

    public static RecoverySnapshot popLastSnapshot(Level level, UUID playerUUID) {
        LinkedList<RecoverySnapshot> records = IN_MEMORY_CACHE.get(playerUUID);
        if (records == null || records.isEmpty()) return null;

        RecoverySnapshot last = records.removeLast();
        persistReason(level, playerUUID, last.getReason(), records);

        if (records.isEmpty()) {
            IN_MEMORY_CACHE.remove(playerUUID);
        }

        return last;
    }

    public static LinkedList<RecoverySnapshot> getAndClearSavedItems(Level level, UUID playerUUID) {
        LinkedList<RecoverySnapshot> items = IN_MEMORY_CACHE.remove(playerUUID);

        org.silverbreezed.whitehole.manager.AsyncIOManager.deleteFileAsync(getSaveFile(level, playerUUID, RecoveryReason.VOID_DEATH));
        org.silverbreezed.whitehole.manager.AsyncIOManager.deleteFileAsync(getSaveFile(level, playerUUID, RecoveryReason.DESPAWN));

        return items != null ? items : new LinkedList<>();
    }

    /**
     * Persists only the subset of {@code allRecords} matching {@code reason} to that reason's
     * own file - the other reason's file is never touched by this call. This is the whole
     * point of the void/despawn split: a despawn capture (expected to be frequent) never has
     * to re-serialize void-death history (expected to be rare), and vice versa.
     */
    private static void persistReason(Level level, UUID playerUUID, RecoveryReason reason, LinkedList<RecoverySnapshot> allRecords) {
        List<RecoverySnapshot> subset = allRecords.stream()
                .filter(r -> r.getReason() == reason)
                .toList();

        File file = getSaveFile(level, playerUUID, reason);

        if (subset.isEmpty()) {
            org.silverbreezed.whitehole.manager.AsyncIOManager.deleteFileAsync(file);
            return;
        }

        HolderLookup.Provider provider = level.registryAccess();
        RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, provider);

        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        com.google.gson.JsonArray recordsArray = new com.google.gson.JsonArray();

        for (RecoverySnapshot record : subset) {
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
            Constants.LOG.info("Snapshot (" + reason + ") successfully saved for: " + playerUUID);
        });
    }
}