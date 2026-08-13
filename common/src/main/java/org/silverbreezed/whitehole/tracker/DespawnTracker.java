package org.silverbreezed.whitehole.tracker;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks item entities that originated from a player's death drop, so DespawnCaptureTrigger
 * can recognise "this one is ours" without touching entities from farms, thrown items, or
 * manual drops via the Q key.
 *
 * In-memory only, keyed by entity UUID (stable across chunk reload, unlike the Java object
 * reference). Deliberate trade-off: if the server restarts before a tracked entity is either
 * picked up or expires, tracking for that one drop is lost and it simply falls back to plain
 * vanilla despawn behaviour - the same outcome as if the mod weren't installed for that
 * single item drop. This avoids relying on persistent entity NBT/attachment APIs, which
 * differ enough between Fabric and NeoForge that it isn't worth the extra fragility for a
 * best-effort safety net.
 */
public class DespawnTracker {
    private static final Map<UUID, UUID> TRACKED_ENTITY_OWNERS = new ConcurrentHashMap<>();

    public static void track(UUID entityUUID, UUID ownerUUID) {
        TRACKED_ENTITY_OWNERS.put(entityUUID, ownerUUID);
    }

    /**
     * Removes and returns the owner of a tracked entity, if any. Call this exactly once,
     * at the moment you're about to act on the entity (capture-on-expiry), so a given
     * entity can never be captured twice.
     */
    public static UUID untrack(UUID entityUUID) {
        return TRACKED_ENTITY_OWNERS.remove(entityUUID);
    }
}