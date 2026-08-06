package org.silverbreezed.whitehole;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.minecraft.server.level.ServerPlayer;
import org.silverbreezed.whitehole.event.VoidDeathHandler;

@EventBusSubscriber(modid = "whitehole")
public class NeoForgeEventSubscriber {

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof ServerPlayer player) {
            // Memanggil fungsi logika utama dari folder Common
            boolean secured = VoidDeathHandler.handlePlayerVoidDeath(player, event.getSource());

            if (secured) {
                // Di NeoForge/Forge, mengosongkan inventaris manual di VoidDeathHandler
                // sudah cukup untuk mengamankan item tanpa membatalkan seluruh event kematiannya.
            }
        }
    }
}
