package org.silverbreezed.whitehole.tracker;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks item entities that originated from a player's death drop, so DespawnCaptureTrigger
 * can recognise "this one is ours" without touching entities from farms, thrown items, or
 * manual drops via the Q key.
 *
 * Entities are grouped by deathId, NOT just by owner - a player can have items outstanding
 * from more than one unresolved death at once (e.g. died again before the first death's
 * items were captured), and those must never be merged into the same snapshot just because
 * they share an owner. See DespawnCaptureTrigger's beginDeath()/markOwned() for where a
 * deathId is minted and attached.
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

    private record TrackedEntry(UUID ownerUUID, UUID deathId) {}

    private static final Map<UUID, TrackedEntry> ENTITY_TO_OWNER = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<UUID>> DEATH_TO_OUTSTANDING_ENTITIES = new ConcurrentHashMap<>();

    public static void track(UUID entityUUID, UUID ownerUUID, UUID deathId) {
        ENTITY_TO_OWNER.put(entityUUID, new TrackedEntry(ownerUUID, deathId));
        DEATH_TO_OUTSTANDING_ENTITIES
                .computeIfAbsent(deathId, k -> ConcurrentHashMap.newKeySet())
                .add(entityUUID);
    }

    /**
     * The deathId a tracked entity currently belongs to, if any. Call this BEFORE untrack()
     * for the same entity - untrack() removes the mapping this reads from.
     */
    public static UUID peekDeathId(UUID entityUUID) {
        TrackedEntry entry = ENTITY_TO_OWNER.get(entityUUID);
        return entry == null ? null : entry.deathId();
    }

    /**
     * Removes and returns the owner of a tracked entity, if any. Call this exactly once,
     * at the moment you're about to act on the entity (capture-on-expiry or a successful
     * sweep), so a given entity can never be captured twice.
     */
    public static UUID untrack(UUID entityUUID) {
        TrackedEntry entry = ENTITY_TO_OWNER.remove(entityUUID);
        if (entry == null) return null;

        Set<UUID> siblings = DEATH_TO_OUTSTANDING_ENTITIES.get(entry.deathId());
        if (siblings != null) {
            siblings.remove(entityUUID);
            if (siblings.isEmpty()) {
                DEATH_TO_OUTSTANDING_ENTITIES.remove(entry.deathId());
            }
        }
        return entry.ownerUUID();
    }

    /**
     * Every entity UUID still tracked for this SPECIFIC death - deliberately scoped to one
     * deathId, not "every outstanding entity this player happens to have", so sweeping
     * siblings on one death's expiry can never pull in an unrelated, still-pending death.
     */
    public static Set<UUID> peekOutstanding(UUID deathId) {
        Set<UUID> siblings = DEATH_TO_OUTSTANDING_ENTITIES.get(deathId);
        return siblings == null ? Set.of() : new HashSet<>(siblings);
    }
}