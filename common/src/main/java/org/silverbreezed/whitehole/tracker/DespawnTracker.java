package org.silverbreezed.whitehole.manager;

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
 * Also tracks, per owner, the full set of currently-outstanding entities from their death(s) -
 * this is what lets DespawnCaptureTrigger "sweep" siblings that haven't individually expired
 * yet once one of them does (see peekOutstanding()). Without this, items scattered far enough
 * apart that simulation distance staggers their expiry - a real scenario, since entities only
 * tick while their chunk is within a player's simulation distance - would each finalize their
 * own separate batch instead of one unified snapshot.
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
    private static final Map<UUID, UUID> ENTITY_TO_OWNER = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<UUID>> OWNER_TO_OUTSTANDING_ENTITIES = new ConcurrentHashMap<>();

    public static void track(UUID entityUUID, UUID ownerUUID) {
        ENTITY_TO_OWNER.put(entityUUID, ownerUUID);
        OWNER_TO_OUTSTANDING_ENTITIES
                .computeIfAbsent(ownerUUID, k -> ConcurrentHashMap.newKeySet())
                .add(entityUUID);
    }

    /**
     * Removes and returns the owner of a tracked entity, if any. Call this exactly once,
     * at the moment you're about to act on the entity (capture-on-expiry or a successful
     * sweep), so a given entity can never be captured twice.
     */
    public static UUID untrack(UUID entityUUID) {
        UUID owner = ENTITY_TO_OWNER.remove(entityUUID);
        if (owner != null) {
            Set<UUID> siblings = OWNER_TO_OUTSTANDING_ENTITIES.get(owner);
            if (siblings != null) {
                siblings.remove(entityUUID);
            }
        }
        return owner;
    }

    /**
     * Every entity UUID still tracked for this owner (i.e. not yet expired, swept, or
     * otherwise untracked) - a defensive copy, safe to iterate while untrack() is called
     * concurrently for entries within it.
     */
    public static Set<UUID> peekOutstanding(UUID ownerUUID) {
        Set<UUID> siblings = OWNER_TO_OUTSTANDING_ENTITIES.get(ownerUUID);
        return siblings == null ? Set.of() : new HashSet<>(siblings);
    }
}