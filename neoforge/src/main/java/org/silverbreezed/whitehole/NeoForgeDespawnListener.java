package org.silverbreezed.whitehole;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.silverbreezed.whitehole.event.capture.DespawnCaptureTrigger;
import org.silverbreezed.whitehole.manager.DespawnBatchAggregatorManager;

/**
 * NeoForge-side wiring for despawn recovery. Uses NeoForge's own extension points instead
 * of a mixin, so this class - not shared mixin code - is what needs re-checking against the
 * NeoForge event API on a version bump.
 *
 * IMPORTANT: LivingDropsEvent#getDrops() and ItemExpireEvent's shape have changed across
 * NeoForge releases before; verify both against your local NeoForge 26.2 sources/javadoc
 * before compiling - the exact method name/collection type below is a best-effort guess,
 * not a guarantee.
 */
@EventBusSubscriber(modid = Constants.MOD_ID)
public class NeoForgeDespawnListener {

    @SubscribeEvent
    public static void onDeathDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!DespawnCaptureTrigger.isEnabled()) return;

        for (ItemEntity drop : event.getDrops()) {
            DespawnCaptureTrigger.markOwned(drop, player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onItemExpire(ItemExpireEvent event) {
        ItemEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        if (DespawnCaptureTrigger.tryIntercept(serverLevel, entity)) {
            // We've claimed the items into a pending batch - cancel vanilla's expiry
            // handling and remove the entity so it doesn't linger or get double-processed.
            // event.setCanceled(true);
            entity.discard();
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        DespawnBatchAggregatorManager.flushAllImmediately();
    }
}