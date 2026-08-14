package org.silverbreezed.whitehole.manager;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.silverbreezed.whitehole.Constants;
import org.silverbreezed.whitehole.enums.RecoveryReason;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Groups despawn captures that happen close together in time into a single RecoverySnapshot,
 * so a player who dies with a full inventory gets ONE altar recovery for all their lost
 * items - not one recovery per individual item. Without this, each item independently
 * reaching its own expiry (which items from the same death very rarely do on the exact same
 * server tick - partial pickups, stack merges, or /data modify age tweaks used for testing
 * all skew it slightly) would also silently blow through maxSavedItemSnapshots after just a
 * few items from a single death.
 *
 * Uses an idle window rather than a fixed delay: as long as new items keep arriving for a
 * player, the wait keeps resetting. Only once nothing new has arrived for IDLE_WINDOW_MILLIS
 * is that player's batch finalized into one snapshot.
 *
 * Deliberately in-memory only, same trade-off as DespawnTracker: if the server stops within
 * the idle window, pending items would normally be lost. flushAllImmediately() closes that
 * gap - wire it to a server-stopping hook on each loader.
 */
public class DespawnBatchAggregatorManager {
    private static final long IDLE_WINDOW_MILLIS = 3000;

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "WhiteHole-DespawnBatch-Timer");
        thread.setDaemon(true);
        return thread;
    });

    private static final Map<UUID, PendingBatch> PENDING = new ConcurrentHashMap<>();
    private static final Map<UUID, ScheduledFuture<?>> PENDING_FLUSHES = new ConcurrentHashMap<>();

    private static class PendingBatch {
        final ServerLevel level;
        final List<ItemStack> items = new ArrayList<>();

        PendingBatch(ServerLevel level) {
            this.level = level;
        }
    }

    public static void addItem(ServerLevel level, UUID ownerUUID, ItemStack stack) {
        PendingBatch batch = PENDING.computeIfAbsent(ownerUUID, k -> new PendingBatch(level));
        synchronized (batch.items) {
            batch.items.add(stack);
        }

        // Debounce: cancel any previously scheduled flush for this player and start the
        // idle window over, so a burst of items arriving close together all land in the
        // same eventual snapshot.
        ScheduledFuture<?> existing = PENDING_FLUSHES.remove(ownerUUID);
        if (existing != null) {
            existing.cancel(false);
        }

        ScheduledFuture<?> flush = SCHEDULER.schedule(
                () -> flush(ownerUUID),
                IDLE_WINDOW_MILLIS,
                TimeUnit.MILLISECONDS
        );
        PENDING_FLUSHES.put(ownerUUID, flush);
    }

    private static void flush(UUID ownerUUID) {
        PendingBatch batch = PENDING.remove(ownerUUID);
        PENDING_FLUSHES.remove(ownerUUID);
        if (batch == null) return;

        ServerLevel level = batch.level;
        if (level.getServer() == null) return;

        // The idle-window timer runs off the main server thread - hop back onto it before
        // touching registry-dependent ItemStack codecs or world/data files.
        level.getServer().execute(() -> {
            List<ItemStack> items;
            synchronized (batch.items) {
                items = new ArrayList<>(batch.items);
            }
            if (items.isEmpty()) return;

            ItemSnapshotManager.capture(RecoveryReason.DESPAWN, level, ownerUUID, items);
            Objects.requireNonNull(level.getPlayerInAnyDimension(ownerUUID)).sendSystemMessage(Component.literal("§5[§lWhite Hole§r§5] §dYour items has despawned. You can bring back your items using the White Hole Altar in an Ancient City."));;
            Constants.LOG.info("Finalized a despawn batch of " + items.size() + " item(s) for " + ownerUUID);
        });
    }

    /**
     * Force-finalizes every pending batch immediately, synchronously, without waiting for
     * the idle window. Intended to be called only from a server-stopping hook, which already
     * runs on the main server thread - so unlike the normal timer-driven flush(), this
     * deliberately does NOT hop through level.getServer().execute(): there's no guarantee a
     * newly queued task still runs during shutdown.
     */
    public static void flushAllImmediately() {
        for (UUID ownerUUID : new HashSet<>(PENDING.keySet())) {
            ScheduledFuture<?> scheduled = PENDING_FLUSHES.remove(ownerUUID);
            if (scheduled != null) scheduled.cancel(false);

            PendingBatch batch = PENDING.remove(ownerUUID);
            if (batch == null) continue;

            List<ItemStack> items;
            synchronized (batch.items) {
                items = new ArrayList<>(batch.items);
            }
            if (!items.isEmpty()) {
                ItemSnapshotManager.capture(RecoveryReason.DESPAWN, batch.level, ownerUUID, items);
            }
        }
    }
}