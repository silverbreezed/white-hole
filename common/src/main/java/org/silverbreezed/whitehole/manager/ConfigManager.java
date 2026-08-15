package org.silverbreezed.whitehole.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
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
                JsonObject root = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();

                if (isLegacyFlatSchema(root)) {
                    Constants.LOG.info("Migrating " + Constants.MOD_ID + ".json from the pre-refactor flat config schema.");
                    modConfig = migrateLegacyConfig(root);
                    save(); // persist the migrated, nested shape immediately so this only runs once
                } else {
                    modConfig = GSON.fromJson(root, ModConfig.class);
                    if (modConfig == null) {
                        modConfig = new ModConfig();
                    }

                    if (migrateLegacyGlobalCap(root, modConfig)) {
                        Constants.LOG.info("Migrating " + Constants.MOD_ID + ".json's shared maxSavedItemSnapshots into separate per-reason caps.");
                        save();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("No existing config file exist. Creating new...");
            save();
        }
    }

    /**
     * Configs written before the void/despawn recovery grouping had these keys at the root
     * instead of nested under "voidRecovery" / "despawnRecovery". Their presence at the root
     * (without the new nested keys) signals a pre-refactor file that needs migrating.
     */
    private static boolean isLegacyFlatSchema(JsonObject root) {
        return root.has("recoverItemsFromEndVoid") && !root.has("voidRecovery");
    }

    private static ModConfig migrateLegacyConfig(JsonObject root) {
        ModConfig config = new ModConfig();

        if (root.has("recoverItemsFromEndVoid")) {
            config.voidRecovery.end = root.get("recoverItemsFromEndVoid").getAsBoolean();
        }
        if (root.has("recoverItemsFromOverworldVoid")) {
            config.voidRecovery.overworld = root.get("recoverItemsFromOverworldVoid").getAsBoolean();
        }
        if (root.has("recoverItemsFromNetherVoid")) {
            config.voidRecovery.nether = root.get("recoverItemsFromNetherVoid").getAsBoolean();
        }
        if (root.has("recoverItemsFromDespawn")) {
            config.despawnRecovery.enabled = root.get("recoverItemsFromDespawn").getAsBoolean();
        }
        if (root.has("maxSavedItemSnapshots")) {
            // Pre-refactor files only ever had one shared cap - apply it to both new
            // per-reason caps rather than picking one arbitrarily.
            int legacyCap = root.get("maxSavedItemSnapshots").getAsInt();
            config.voidRecovery.maxSnapshots = legacyCap;
            config.despawnRecovery.maxSnapshots = legacyCap;
        }
        if (root.has("altarCooldown")) {
            config.altarCooldown = root.get("altarCooldown").getAsBoolean();
        }
        if (root.has("defaultAltarCooldown")) {
            config.defaultAltarCooldown = root.get("defaultAltarCooldown").getAsInt();
        }

        return config;
    }

    /**
     * Handles the second migration tier: files already on the nested voidRecovery/
     * despawnRecovery schema, but written before maxSavedItemSnapshots moved from a single
     * shared root field into each reason's own maxSnapshots field. Without this, a
     * server owner's customized cap would silently be dropped in favor of the new field's
     * default (3) the first time they load with this version.
     *
     * @return true if a migration was actually performed (caller should re-save).
     */
    private static boolean migrateLegacyGlobalCap(JsonObject root, ModConfig config) {
        if (!root.has("maxSavedItemSnapshots")) return false;

        int legacyCap = root.get("maxSavedItemSnapshots").getAsInt();
        config.voidRecovery.maxSnapshots = legacyCap;
        config.despawnRecovery.maxSnapshots = legacyCap;
        return true;
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