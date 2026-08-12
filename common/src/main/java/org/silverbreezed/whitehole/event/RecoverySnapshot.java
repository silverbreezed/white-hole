package org.silverbreezed.whitehole.event;

import net.minecraft.world.item.ItemStack;
import org.silverbreezed.whitehole.enums.RecoveryReason;

import java.util.List;

/**
 * A single recoverable batch of items, tagged with the reason it was captured.
 *
 * Replaces the old void-only DeathRecord so the same cache/persistence pipeline in
 * ItemSnapshotManager can serve any future capture trigger (void death today, despawn
 * recovery later) without duplicating storage logic.
 */
public class RecoverySnapshot {
    private final long timestamp;
    private final RecoveryReason reason;
    private final List<ItemStack> items;

    public RecoverySnapshot(long timestamp, RecoveryReason reason, List<ItemStack> items) {
        this.timestamp = timestamp;
        this.reason = reason;
        this.items = items;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public RecoveryReason getReason() {
        return reason;
    }

    public List<ItemStack> getItems() {
        return items;
    }
}