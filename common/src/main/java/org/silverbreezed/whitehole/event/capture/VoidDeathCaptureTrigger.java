package org.silverbreezed.whitehole.event.capture;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.ItemStack;
import org.silverbreezed.whitehole.enums.RecoveryReason;
import org.silverbreezed.whitehole.manager.ConfigManager;
import org.silverbreezed.whitehole.manager.ItemSnapshotManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Capture trigger for players who die by falling out of the world.
 *
 * A future DespawnCaptureTrigger (for the recoverItemsFromDespawn feature) is expected to
 * live alongside this class as its own, independent trigger - not as a branch inside this
 * one - since it fires from a completely different hook (an item-entity tick sweep, not a
 * player death event). Both triggers only need to agree on calling into
 * ItemSnapshotManager.capture(...).
 */
public class VoidDeathCaptureTrigger {

    /**
     * @return true if the player's inventory was captured and cleared.
     */
    public static boolean tryCapture(ServerPlayer player, DamageSource source) {
        if (!source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return false;
        }

        ServerLevel playerLevel = player.level();
        org.silverbreezed.whitehole.config.VoidRecoveryConfig config = ConfigManager.getModConfig().voidRecovery;

        boolean dimensionEnabled =
                (playerLevel.dimension() == ServerLevel.OVERWORLD && config.overworld) ||
                        (playerLevel.dimension() == ServerLevel.END && config.end) ||
                        (playerLevel.dimension() == ServerLevel.NETHER && config.nether);

        if (!dimensionEnabled) {
            return false;
        }

        List<ItemStack> savedInventory = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                savedInventory.add(stack.copy());
            }
        }

        boolean captured = ItemSnapshotManager.capture(RecoveryReason.VOID_DEATH, player, savedInventory);
        if (captured) {
            player.getInventory().clearContent();
        }
        return captured;
    }
}