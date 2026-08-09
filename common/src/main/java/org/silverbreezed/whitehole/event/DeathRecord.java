package org.silverbreezed.whitehole.event;

import net.minecraft.world.item.ItemStack;
import java.util.List;

public class DeathRecord {
    private final long timestamp;
    private final List<ItemStack> items;

    public DeathRecord(long timestamp, List<ItemStack> items) {
        this.timestamp = timestamp;
        this.items = items;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public long getTimestamp() {
        return timestamp;
    }
}