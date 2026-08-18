package org.silverbreezed.whitehole;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.silverbreezed.whitehole.event.capture.VoidDeathCaptureTrigger;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID)
public class ForgeEventSubscriber {

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof ServerPlayer player) {
            VoidDeathCaptureTrigger.tryCapture(player, event.getSource());
        }
    }
}