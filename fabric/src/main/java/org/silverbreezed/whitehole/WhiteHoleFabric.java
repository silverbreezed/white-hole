package org.silverbreezed.whitehole;

import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.silverbreezed.whitehole.manager.ConfigManager;
import org.silverbreezed.whitehole.manager.DespawnBatchAggregatorManager;
import org.slf4j.LoggerFactory;
import java.io.IOException;

public class WhiteHoleFabric implements ModInitializer {

    @Override
    public void onInitialize() {

        org.silverbreezed.whitehole.block.ModBlocksFabric.register();
        org.silverbreezed.whitehole.item.ModItemsFabric.register();

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> DespawnBatchAggregatorManager.flushAllImmediately());

        try {
            ConfigManager.load();
            LoggerFactory.getLogger(Constants.MOD_ID).info("{}", new GsonBuilder()
                    .setPrettyPrinting()
                    .create().toJson(ConfigManager.getModConfig()));

            CommonClass.init();
            Constants.LOG.info("Enabled White hole for [FABRIC]!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
