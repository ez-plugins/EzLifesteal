package com.skyblockexp.ezlifesteal.overlay;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.util.ActionBarHelper;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HeartOverlayManagerTest {

    @Test
    void reloadGatesFeatureByEnabledFlag() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        HeartOverlayManager manager = new HeartOverlayManager(plugin);

        ConfigurationSection disabled = new MemoryConfiguration().createSection("overlay");
        disabled.set("enabled", false);

        Player player = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);

        try (MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class);
             MockedStatic<ActionBarHelper> actionBar = mockStatic(ActionBarHelper.class)) {
            manager.reload(disabled, 40.0);
            assertFalse(manager.isEnabled());
            manager.sendHeartStatus(player, 12.0);

            scheduler.verifyNoInteractions();
            actionBar.verifyNoInteractions();
        }

        ConfigurationSection enabled = new MemoryConfiguration().createSection("overlay");
        enabled.set("enabled", true);
        enabled.set("mode", "ACTION_BAR");
        enabled.set("update-interval-ticks", 20L);
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(player.getWorld()).thenReturn(world);
        when(player.getName()).thenReturn("Tester");

        SchedulerAdapter.TaskHandle handle = mock(SchedulerAdapter.TaskHandle.class);
        when(handle.isCancelled()).thenReturn(false);

        try (MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class);
             MockedStatic<ActionBarHelper> actionBar = mockStatic(ActionBarHelper.class)) {
            scheduler.when(() -> SchedulerAdapter.runTimer(any(), any(), anyLong(), anyLong())).thenReturn(handle);
            manager.reload(enabled, 40.0);
            manager.sendHeartStatus(player, 12.0);

            assertTrue(manager.isEnabled());
            scheduler.verify(() -> SchedulerAdapter.runTimer(any(), any(), anyLong(), anyLong()), times(1));
            actionBar.verify(() -> ActionBarHelper.sendActionBar(any(Player.class), anyString()), times(1));
        }
    }

    @Test
    void tickUpdatesOnlinePlayersAndSkipsInvalidContexts() throws Exception {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        HeartOverlayManager manager = new HeartOverlayManager(plugin);
        ConfigurationSection section = new MemoryConfiguration().createSection("overlay");
        section.set("enabled", true);
        section.set("mode", "ACTION_BAR");
        section.set("enabled-worlds", java.util.List.of("allowed"));

        Player online = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        World allowed = mock(World.class);
        when(allowed.getName()).thenReturn("allowed");
        when(online.getUniqueId()).thenReturn(uuid);
        when(online.getName()).thenReturn("Online");
        when(online.getWorld()).thenReturn(allowed);
        when(online.isOnline()).thenReturn(true);

        SchedulerAdapter.TaskHandle handle = mock(SchedulerAdapter.TaskHandle.class);
        when(handle.isCancelled()).thenReturn(false);

        Method tick = HeartOverlayManager.class.getDeclaredMethod("tick");
        tick.setAccessible(true);

        try (MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class);
             MockedStatic<ActionBarHelper> actionBar = mockStatic(ActionBarHelper.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            scheduler.when(() -> SchedulerAdapter.runTimer(any(), any(), anyLong(), anyLong())).thenReturn(handle);
            bukkit.when(() -> Bukkit.getPlayer(uuid)).thenReturn(online);

            manager.reload(section, 40.0);
            manager.sendHeartStatus(online, 14.0);
            tick.invoke(manager);

            actionBar.verify(() -> ActionBarHelper.sendActionBar(eq(online), anyString()), atLeastOnce());

            // Invalid context: player disappears from Bukkit lookup and gets cleared without overlay update.
            bukkit.when(() -> Bukkit.getPlayer(uuid)).thenReturn(null);
            tick.invoke(manager);
            verify(handle, atLeastOnce()).cancel();
            actionBar.verify(() -> ActionBarHelper.sendActionBar(eq(online), anyString()), times(2));
        }
    }

    @Test
    void clearAndShutdownAreIdempotent() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        HeartOverlayManager manager = new HeartOverlayManager(plugin);
        ConfigurationSection section = new MemoryConfiguration().createSection("overlay");
        section.set("enabled", true);
        section.set("mode", "BOSS_BAR");

        Player player = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        World world = mock(World.class);
        BossBar bossBar = mock(BossBar.class);
        SchedulerAdapter.TaskHandle handle = mock(SchedulerAdapter.TaskHandle.class);
        when(handle.isCancelled()).thenReturn(false);

        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(player.getName()).thenReturn("Boss");

        try (MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            scheduler.when(() -> SchedulerAdapter.runTimer(any(), any(), anyLong(), anyLong())).thenReturn(handle);
            bukkit.when(() -> Bukkit.createBossBar(anyString(), any(), any())).thenReturn(bossBar);

            manager.reload(section, 40.0);
            manager.sendHeartStatus(player, 10.0);

            manager.clear(uuid);
            manager.clear(uuid);

            manager.shutdown();
            manager.shutdown();

            verify(bossBar, atLeastOnce()).removeAll();
            verify(handle, atLeastOnce()).cancel();
        }
    }

    @Test
    void ensureTaskPreventsDuplicateSchedulerRegistration() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        HeartOverlayManager manager = new HeartOverlayManager(plugin);
        ConfigurationSection section = new MemoryConfiguration().createSection("overlay");
        section.set("enabled", true);
        section.set("mode", "ACTION_BAR");

        Player player = mock(Player.class);
        UUID uuid = UUID.randomUUID();
        World world = mock(World.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(player.getName()).thenReturn("Scheduler");

        SchedulerAdapter.TaskHandle active = mock(SchedulerAdapter.TaskHandle.class);
        SchedulerAdapter.TaskHandle second = mock(SchedulerAdapter.TaskHandle.class);
        when(active.isCancelled()).thenReturn(false);
        when(second.isCancelled()).thenReturn(false);

        try (MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class);
             MockedStatic<ActionBarHelper> actionBar = mockStatic(ActionBarHelper.class)) {
            scheduler.when(() -> SchedulerAdapter.runTimer(any(), any(), anyLong(), anyLong())).thenReturn(active,
                    second);

            manager.reload(section, 40.0);
            manager.sendHeartStatus(player, 8.0);
            manager.sendHeartStatus(player, 9.0);

            scheduler.verify(() -> SchedulerAdapter.runTimer(any(), any(), anyLong(), anyLong()), times(1));

            manager.clear(uuid);
            manager.sendHeartStatus(player, 11.0);
            scheduler.verify(() -> SchedulerAdapter.runTimer(any(), any(), anyLong(), anyLong()), times(2));

            actionBar.verify(() -> ActionBarHelper.sendActionBar(any(Player.class), anyString()), times(3));
        }
    }
}
