package org.silverbreezed.whitehole;

import com.google.gson.GsonBuilder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.silverbreezed.whitehole.manager.ConfigManager;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Mod(Constants.MOD_ID)
public class WhiteHoleNeoForge {
    public WhiteHoleNeoForge(IEventBus eventBus) throws IOException {

        org.silverbreezed.whitehole.block.ModBlocksNeoForge.register(eventBus);
        org.silverbreezed.whitehole.item.ModItemsNeoForge.register(eventBus);

        org.silverbreezed.whitehole.manager.ConfigManager.load();
        LoggerFactory.getLogger(Constants.MOD_ID).info("Mod Config:\n{}", new GsonBuilder().setPrettyPrinting().create().toJson(ConfigManager.getModConfig()));

        // This method is invoked by the NeoForge mod loader when it is ready
        // to load your mod. You can access NeoForge and Common code in this
        // project.

        // Use NeoForge to bootstrap the Common mod.
        Constants.LOG.info("Hello NeoForge world!");
        CommonClass.init();

    }
}