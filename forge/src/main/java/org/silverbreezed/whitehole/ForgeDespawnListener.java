package org.silverbreezed.whitehole;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.silverbreezed.whitehole.event.capture.DespawnCaptureTrigger;
import org.silverbreezed.whitehole.manager.DespawnBatchAggregatorManager;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID)
public class ForgeDespawnListener {

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