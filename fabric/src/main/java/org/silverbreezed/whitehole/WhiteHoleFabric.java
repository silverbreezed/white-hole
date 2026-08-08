package org.silverbreezed.whitehole;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;
import org.silverbreezed.whitehole.manager.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class WhiteHoleFabric implements ModInitializer {

    @Override
    public void onInitialize() {

        org.silverbreezed.whitehole.block.ModBlocksFabric.register();
        org.silverbreezed.whitehole.item.ModItemsFabric.register();

        try {
            ConfigManager.load();
            LOGGER.info("{}", GSON.toJson(ConfigManager.getModConfig()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // This method is invoked by the Fabric mod loader when it is ready
        // to load your mod. You can access Fabric and Common code in this
        // project.

        // Use Fabric to bootstrap the Common mod.
        Constants.LOG.info("Enabled White hole for [FABRIC]!");
        CommonClass.init();
    }

    public static final Logger LOGGER =
            LoggerFactory.getLogger(Constants.MOD_ID);

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();
}
