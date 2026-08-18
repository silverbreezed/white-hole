package org.silverbreezed.whitehole;

import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.io.IOException;

@Mod(Constants.MOD_ID)
public class WhiteHoleForge {

    public WhiteHoleForge() throws IOException {
        BusGroup eventBus = FMLJavaModLoadingContext.get().getModBusGroup();

        org.silverbreezed.whitehole.block.ModBlocksForge.register(eventBus);
        org.silverbreezed.whitehole.item.ModItemsForge.register(eventBus);

        Constants.LOG.info("Hello Forge world!");

        CommonClass.init();
    }
}