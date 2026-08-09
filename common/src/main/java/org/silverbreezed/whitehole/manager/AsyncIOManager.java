package org.silverbreezed.whitehole.manager;

import com.google.gson.JsonObject;
import org.silverbreezed.whitehole.Constants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncIOManager {
    public static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "WhiteHole-IO-Worker");
        thread.setDaemon(true);
        return thread;
    });

    public static CompletableFuture<Void> writeJsonAsync(File file, JsonObject data) {
        return CompletableFuture.runAsync(() -> {
            try (FileWriter writer = new FileWriter(file)) {
                Constants.GSON.toJson(data, writer);
            } catch (IOException e) {
                Constants.LOG.error("Error to write file asynchronously: " + file.getName(), e);
            }
        });
    }

    public static CompletableFuture<Void> deleteFileAsync(File file) {
        return CompletableFuture.runAsync(() -> {
            if (file.exists()) {
                if (!file.delete()) {
                    Constants.LOG.error("Failed to delete file: " + file.getName());
                }
            }
        }, IO_EXECUTOR);
    }

    public static void shutdown() {
        IO_EXECUTOR.shutdown();
    }
}
