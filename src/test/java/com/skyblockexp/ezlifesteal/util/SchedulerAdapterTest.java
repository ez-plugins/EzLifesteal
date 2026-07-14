package com.skyblockexp.ezlifesteal.util;

import java.lang.reflect.Field;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchedulerAdapterTest {

    @AfterEach
    void resetFoliaCache() throws Exception {
        Field foliaField = SchedulerAdapter.class.getDeclaredField("FOLIA");
        foliaField.setAccessible(true);
        foliaField.set(null, null);
    }

    @Test
    void runRoutesToBukkitSchedulerWhenNotFolia() {
        Plugin plugin = mock(Plugin.class);
        Runnable runnable = mock(Runnable.class);
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            when(server.getName()).thenReturn("Paper");
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

            SchedulerAdapter.run(plugin, runnable);

            verify(scheduler).runTask(plugin, runnable);
            bukkit.verify(Bukkit::getGlobalRegionScheduler, times(0));
        }
    }

    @Test
    void runRoutesToGlobalSchedulerWhenFolia() {
        Plugin plugin = mock(Plugin.class);
        Runnable runnable = mock(Runnable.class);
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            when(server.getName()).thenReturn("Folia");
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            // In test/runtime without Folia API, reflective call should fall back to Bukkit scheduler.
            bukkit.when(Bukkit::getGlobalRegionScheduler)
                    .thenThrow(new RuntimeException("Global scheduler unavailable"));

            SchedulerAdapter.run(plugin, runnable);

            verify(scheduler).runTask(plugin, runnable);
        }
    }

    @Test
    void runLaterUsesBukkitSchedulerWhenNotFoliaAndHandleCancelsDelegate() {
        Plugin plugin = mock(Plugin.class);
        Runnable runnable = mock(Runnable.class);
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask bukkitTask = mock(BukkitTask.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            when(server.getName()).thenReturn("Paper");
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            when(scheduler.runTaskLater(plugin, runnable, 20L)).thenReturn(bukkitTask);
            when(bukkitTask.isCancelled()).thenReturn(false, true);

            SchedulerAdapter.TaskHandle handle = SchedulerAdapter.runLater(plugin, runnable, 20L);
            assertFalse(handle.isCancelled());

            handle.cancel();

            verify(scheduler).runTaskLater(plugin, runnable, 20L);
            verify(bukkitTask).cancel();
            assertTrue(handle.isCancelled());
        }
    }

    @Test
    void runTimerUsesGlobalSchedulerWhenFoliaAndHandleCancelsOnlyOnce() {
        Plugin plugin = mock(Plugin.class);
        Runnable runnable = mock(Runnable.class);
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask bukkitTask = mock(BukkitTask.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            when(server.getName()).thenReturn("Folia");
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(Bukkit::getGlobalRegionScheduler)
                    .thenThrow(new RuntimeException("Global scheduler unavailable"));
            when(scheduler.runTaskTimer(plugin, runnable, 10L, 5L)).thenReturn(bukkitTask);
            when(bukkitTask.isCancelled()).thenReturn(false, true);

            SchedulerAdapter.TaskHandle handle = SchedulerAdapter.runTimer(plugin, runnable, 10L, 5L);
            assertFalse(handle.isCancelled());

            handle.cancel();
            handle.cancel();

            verify(scheduler).runTaskTimer(plugin, runnable, 10L, 5L);
            verify(bukkitTask, times(2)).cancel();
            assertTrue(handle.isCancelled());
        }
    }

    @Test
    void runAndRunLaterGuardNullArguments() {
        Plugin plugin = mock(Plugin.class);
        Runnable runnable = mock(Runnable.class);

        assertThrows(NullPointerException.class, () -> SchedulerAdapter.run(null, runnable));
        assertThrows(NullPointerException.class, () -> SchedulerAdapter.run(plugin, null));
        assertThrows(NullPointerException.class, () -> SchedulerAdapter.runLater(null, runnable, 1L));
        assertThrows(NullPointerException.class, () -> SchedulerAdapter.runLater(plugin, null, 1L));
        assertThrows(NullPointerException.class, () -> SchedulerAdapter.runTimer(null, runnable, 1L, 1L));
        assertThrows(NullPointerException.class, () -> SchedulerAdapter.runTimer(plugin, null, 1L, 1L));
    }

    @Test
    void runFallsBackToBukkitSchedulerWhenFoliaApiUnavailable() {
        Plugin plugin = mock(Plugin.class);
        Runnable runnable = mock(Runnable.class);
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            when(server.getName()).thenReturn("Folia");
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(Bukkit::getGlobalRegionScheduler)
                    .thenThrow(new RuntimeException("No global scheduler on this platform"));

            SchedulerAdapter.run(plugin, runnable);

            verify(scheduler).runTask(plugin, runnable);
        }
    }

    @Test
    void detectFoliaUsesServerName() {
        Server server = mock(Server.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);
            when(server.getName()).thenReturn("Folia");
            assertTrue(SchedulerAdapter.detectFolia());

            when(server.getName()).thenReturn("Paper");
            assertFalse(SchedulerAdapter.detectFolia());
        }
    }
}
