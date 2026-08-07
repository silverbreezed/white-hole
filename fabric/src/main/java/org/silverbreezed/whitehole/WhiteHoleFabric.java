package org.silverbreezed.whitehole;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import org.silverbreezed.whitehole.event.VoidDeathHandler;

public class WhiteHoleFabric implements ModInitializer {

    @Override
    public void onInitialize() {

        ServerLivingEntityEvents.ALLOW_DEATH.register(((entity, damageSource, damageAmount) -> {
            if (entity instanceof ServerPlayer player) {
                boolean secured = VoidDeathHandler.handlePlayerVoidDeath(player, damageSource);

                return !secured;
            }
            return true;
        }));

        org.silverbreezed.whitehole.block.ModBlocks.registerAll();
        org.silverbreezed.whitehole.item.ModItems.registerAll();

        // This method is invoked by the Fabric mod loader when it is ready
        // to load your mod. You can access Fabric and Common code in this
        // project.

        // Use Fabric to bootstrap the Common mod.
        Constants.LOG.info("Enabled White hole for [FABRIC]!");
        CommonClass.init();
    }
}
