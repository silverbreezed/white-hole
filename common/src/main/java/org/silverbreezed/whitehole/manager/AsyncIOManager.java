package org.silverbreezed.whitehole.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.silverbreezed.whitehole.Constants;

import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncIOManager {
    private static final Gson IO_GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "WhiteHole-IO-Worker");
        thread.setDaemon(true);
        return thread;
    });

    public static CompletableFuture<Void> writeJsonAsync(File file, JsonObject data) {
        return CompletableFuture.runAsync(() -> {
            try (FileWriter writer = new FileWriter(file)) {
                IO_GSON.toJson(data, writer);
            } catch (Exception e) {
                Constants.LOG.error("Failed to write to: " + file.getAbsolutePath(), e);
                throw new RuntimeException(e);
            }
        }, IO_EXECUTOR).exceptionally(ex -> {
            Constants.LOG.error("Background thread crashed!", ex);
            return null;
        });
    }

    public static CompletableFuture<Void> deleteFileAsync(File file) {
        return CompletableFuture.runAsync(() -> {
            if (file.exists() && !file.delete()) {
                Constants.LOG.error("Failed to delete file: " + file.getName());
            }
        }, IO_EXECUTOR);
    }
}