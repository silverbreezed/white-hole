package org.silverbreezed.whitehole.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.silverbreezed.whitehole.Constants;
import org.silverbreezed.whitehole.config.ModConfig;
import org.silverbreezed.whitehole.platform.Services;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Services.PLATFORM.getConfigDirectory().resolve(Constants.MOD_ID + ".json");

    private static ModConfig modConfig = new ModConfig();

    public static void load() throws IOException {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                modConfig = GSON.fromJson(reader, ModConfig.class);

                if (modConfig == null) {
                    modConfig = new ModConfig();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("No existing config file exist. Creating new...");
            save();
        }
    }

    public static void save() throws IOException {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(modConfig, writer);
        } catch (Exception e) {
            System.out.println("Failed to save config.");
            e.printStackTrace();
        }
    }

    public static ModConfig getModConfig() {
        return modConfig;
    }
}
