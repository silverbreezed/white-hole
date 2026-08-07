package org.silverbreezed.whitehole;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class WhiteHoleNeoForge {

    public WhiteHoleNeoForge(IEventBus eventBus) {

        org.silverbreezed.whitehole.block.ModBlocks.registerAll();
        org.silverbreezed.whitehole.item.ModItems.registerAll();

        // This method is invoked by the NeoForge mod loader when it is ready
        // to load your mod. You can access NeoForge and Common code in this
        // project.

        // Use NeoForge to bootstrap the Common mod.
        Constants.LOG.info("Hello NeoForge world!");
        CommonClass.init();

    }
}