package com.skyblockexp.ezlifesteal.util;

import com.skyblockexp.ezlifesteal.storage.Storage;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public final class PluginLifecycleSupport {

    private PluginLifecycleSupport() {
    }

    public static void unregisterListener(Listener listener) {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
        }
    }

    public static void closeStorage(Storage storage, Logger logger) {
        if (storage == null) {
            return;
        }
        try {
            storage.close();
        }
        catch (StorageException exception) {
            logger.severe("Failed to close storage: " + exception.getMessage());
        }
    }

    public static void shutdownExecutor(ExecutorService executorService, long timeout, TimeUnit unit) {
        if (executorService == null) {
            return;
        }
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(timeout, unit)) {
                executorService.shutdownNow();
            }
        }
        catch (InterruptedException exception) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
