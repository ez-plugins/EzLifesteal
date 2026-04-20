package com.skyblockexp.ezlifesteal.util;

import com.skyblockexp.ezlifesteal.storage.Storage;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PluginLifecycleSupportTest {

    @Test
    void unregisterListenerDelegatesToHandlerList() {
        Listener listener = mock(Listener.class);

        try (MockedStatic<HandlerList> handlerList = mockStatic(HandlerList.class)) {
            PluginLifecycleSupport.unregisterListener(listener);

            handlerList.verify(() -> HandlerList.unregisterAll(listener));
        }
    }

    @Test
    void unregisterListenerSkipsNullListener() {
        try (MockedStatic<HandlerList> handlerList = mockStatic(HandlerList.class)) {
            PluginLifecycleSupport.unregisterListener(null);

            handlerList.verifyNoInteractions();
        }
    }

    @Test
    void closeStorageClosesStorage() throws StorageException {
        Storage storage = mock(Storage.class);
        Logger logger = mock(Logger.class);

        PluginLifecycleSupport.closeStorage(storage, logger);

        verify(storage).close();
        verify(logger, never()).severe(contains("Failed to close storage"));
    }

    @Test
    void closeStorageLogsWhenCloseThrows() throws StorageException {
        Storage storage = mock(Storage.class);
        Logger logger = mock(Logger.class);
        doThrow(new StorageException("boom")).when(storage).close();

        PluginLifecycleSupport.closeStorage(storage, logger);

        verify(logger).severe("Failed to close storage: boom");
    }

    @Test
    void closeStorageReturnsWhenStorageIsNull() {
        Logger logger = mock(Logger.class);

        PluginLifecycleSupport.closeStorage(null, logger);

        verify(logger, never()).severe(contains("Failed to close storage"));
    }

    @Test
    void shutdownExecutorShutsDownAndWaitsForTermination() throws InterruptedException {
        ExecutorService executor = mock(ExecutorService.class);
        when(executor.awaitTermination(5L, TimeUnit.SECONDS)).thenReturn(true);

        PluginLifecycleSupport.shutdownExecutor(executor, 5L, TimeUnit.SECONDS);

        verify(executor).shutdown();
        verify(executor).awaitTermination(5L, TimeUnit.SECONDS);
        verify(executor, never()).shutdownNow();
    }

    @Test
    void shutdownExecutorForceStopsWhenTerminationTimesOut() throws InterruptedException {
        ExecutorService executor = mock(ExecutorService.class);
        when(executor.awaitTermination(2L, TimeUnit.SECONDS)).thenReturn(false);

        PluginLifecycleSupport.shutdownExecutor(executor, 2L, TimeUnit.SECONDS);

        verify(executor).shutdown();
        verify(executor).shutdownNow();
    }

    @Test
    void shutdownExecutorForceStopsAndReInterruptsWhenInterrupted() throws InterruptedException {
        ExecutorService executor = mock(ExecutorService.class);
        doThrow(new InterruptedException("interrupted")).when(executor).awaitTermination(1L, TimeUnit.SECONDS);
        Thread.interrupted();

        PluginLifecycleSupport.shutdownExecutor(executor, 1L, TimeUnit.SECONDS);

        verify(executor).shutdown();
        verify(executor).shutdownNow();
        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted();
    }

    @Test
    void shutdownExecutorReturnsWhenExecutorIsNull() {
        PluginLifecycleSupport.shutdownExecutor(null, 1L, TimeUnit.SECONDS);
    }
}
