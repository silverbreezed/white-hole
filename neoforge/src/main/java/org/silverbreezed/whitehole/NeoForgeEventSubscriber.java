package org.silverbreezed.whitehole;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.minecraft.server.level.ServerPlayer;
import org.silverbreezed.whitehole.event.capture.VoidDeathCaptureTrigger;

@EventBusSubscriber(modid = "whitehole")
public class NeoForgeEventSubscriber {

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof ServerPlayer player) {
            // NOTE: ServerPlayerMixin#onPlayerDie also calls VoidDeathCaptureTrigger.tryCapture
            // for every loader (the mixin is common). On NeoForge this listener therefore fires
            // a second, redundant call for the same death - harmless today since the first call
            // already clears the inventory, but worth deduplicating in a later pass rather than
            // carried forward silently.
            VoidDeathCaptureTrigger.tryCapture(player, event.getSource());
        }
    }
}