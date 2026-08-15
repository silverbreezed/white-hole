package org.silverbreezed.whitehole.event.capture;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.silverbreezed.whitehole.manager.ConfigManager;
import org.silverbreezed.whitehole.manager.DespawnBatchAggregatorManager;
import org.silverbreezed.whitehole.tracker.DespawnTracker;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Capture trigger for items that survive ANY player death long enough to reach vanilla's
 * own despawn point - universal, no exception for how the player died. If nobody (the owner
 * or another player) retrieves the item before it would naturally despawn, it's captured.
 *
 * Split into two calls that loader-specific wiring invokes separately, because the two hook
 * points don't exist together the same way on every loader:
 *
 *  - markOwned(...): called once, right after a player's death-loot items are dropped into
 *    the world, to tag each resulting entity as belonging to that player's CURRENT death
 *    (see the deathId minted in beginDeath()). On NeoForge this is wired from
 *    LivingDropsEvent; on Fabric from a mixin around the death-drop call.
 *  - tryIntercept(...): called right at the moment an owned entity is about to be removed
 *    for being expired. Doesn't persist it immediately - hands it to
 *    DespawnBatchAggregator, since items from the same death very rarely expire on the
 *    exact same tick and we want ONE snapshot per death, not one per item.
 *
 * Deliberately does NOT use a fixed tick threshold or a spatial radius - it hooks the real
 * despawn decision for the specific tagged entity, wherever it ends up, so it stays correct
 * regardless of a server's configured despawn duration or items drifting off in water.
**/
public class DespawnCaptureTrigger {

    private static final Map<UUID, UUID> ACTIVE_DEATH = new ConcurrentHashMap<>();

    public static boolean isEnabled() {
        return ConfigManager.getModConfig().despawnRecovery.enabled;
    }

    /** Called at HEAD of the player's die() to mint a fresh deathId for this specific death,
     *  and so Fabric's drop-creation hook can tell a death drop apart from a live manual drop
     *  via isActivelyDying(). */
    public static void beginDeath(UUID playerUUID) {
        if (isEnabled()) {
            ACTIVE_DEATH.put(playerUUID, UUID.randomUUID());
        }
    }

    /** Called at TAIL of the player's die(), once all death-loot drops have finished, so a
     *  later unrelated drop (e.g. after respawn) is never mistaken for a death drop. */
    public static void endDeath(UUID playerUUID) {
        ACTIVE_DEATH.remove(playerUUID);
    }

    /** Used only by loader wiring that can't otherwise tell a death drop apart from a live
     *  manual drop (Fabric). */
    public static boolean isActivelyDying(UUID playerUUID) {
        return ACTIVE_DEATH.containsKey(playerUUID);
    }

    public static void markOwned(ItemEntity entity, UUID ownerUUID) {
        UUID deathId = ACTIVE_DEATH.get(ownerUUID);
        if (deathId == null) {
            // Shouldn't normally happen (markOwned should only fire while a death is active),
            // but fall back to a one-off id rather than silently dropping the tag - worst
            // case this single item just won't be grouped with any siblings.
            deathId = UUID.randomUUID();
        }
        DespawnTracker.track(entity.getUUID(), ownerUUID, deathId);
    }

    /**
     * @return true if the entity was claimed (added to a pending batch) and the caller
     *         should discard it itself instead of letting vanilla remove it via normal
     *         expiry. The item isn't written to disk yet at this point - see
     *         DespawnBatchAggregator for when/why it's finalized.
     */
    public static boolean tryIntercept(ServerLevel level, ItemEntity entity) {
        UUID entityUUID = entity.getUUID();
        UUID deathId = DespawnTracker.peekDeathId(entityUUID);
        UUID ownerUUID = DespawnTracker.untrack(entityUUID);
        if (ownerUUID == null) return false;

        ItemStack remaining = entity.getItem();
        if (!remaining.isEmpty()) {
            DespawnBatchAggregatorManager.addItem(level, ownerUUID, remaining.copy());
        }

        if (deathId != null) {
            sweepOutstandingSiblings(level, ownerUUID, deathId);
        }

        return true;
    }

    /**
     * This entity reaching real expiry confirms ITS death's loot window has closed - so any
     * other still-tracked entities from the SAME death (same deathId, never a different one)
     * are force-captured right now via UUID lookup, rather than waiting for each to
     * individually tick its way to expiry. Anything not currently found (chunk unloaded, or
     * already picked up by a player) is left tracked - it'll either get swept next time this
     * death's siblings resolve, expire naturally later, or sit as a harmless stale entry if
     * it was actually picked up.
     */
    private static void sweepOutstandingSiblings(ServerLevel level, UUID ownerUUID, UUID deathId) {
        for (UUID siblingUUID : DespawnTracker.peekOutstanding(deathId)) {
            Entity siblingEntity = level.getEntity(siblingUUID);
            if (!(siblingEntity instanceof ItemEntity siblingItem)) continue;

            DespawnTracker.untrack(siblingUUID);
            ItemStack siblingStack = siblingItem.getItem();
            if (!siblingStack.isEmpty()) {
                DespawnBatchAggregatorManager.addItem(level, ownerUUID, siblingStack.copy());
            }
            siblingItem.discard();
        }
    }
}