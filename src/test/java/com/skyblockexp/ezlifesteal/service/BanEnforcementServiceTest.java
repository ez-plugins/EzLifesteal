package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import com.skyblockexp.ezlifesteal.storage.repository.BanRepository;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BanEnforcementServiceTest {

    @Test
    void applyBanWithStoragePersistsThenAddsBanAndKicksPlayer() throws Exception {
        PluginAccessor pluginAccessor = mock(PluginAccessor.class);
        JavaPlugin plugin = mock(JavaPlugin.class);
        BanRepository banRepository = mock(BanRepository.class);
        Player player = mock(Player.class);
        BanList banList = mock(BanList.class);

        when(pluginAccessor.getPlugin()).thenReturn(plugin);
        when(pluginAccessor.getPluginName()).thenReturn("EzLifesteal");
        when(pluginAccessor.getBanRepository()).thenReturn(banRepository);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Target");
        when(player.isOnline()).thenReturn(true);
        when(banList.isBanned("Target")).thenReturn(false);

        BanEnforcementService service = new BanEnforcementService(pluginAccessor);
        try (MockedStatic<SchedulerAdapter> schedulerAdapter = org.mockito.Mockito.mockStatic(SchedulerAdapter.class);
             MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            schedulerAdapter.when(() -> SchedulerAdapter.run(eq(plugin), any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        invocation.<Runnable>getArgument(1).run();
                        return null;
                    });
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.NAME)).thenReturn(banList);

            service.applyBanWithStorage(player, "reason", "kick");

            verify(banRepository).saveBan(any());
            verify(banList).addBan("Target", "reason", (Instant) null, "EzLifesteal");
            verify(player).kickPlayer("kick");
        }
    }

    @Test
    void applyBanWithStorageLogsAndContinuesWhenRepositoryThrows() throws Exception {
        PluginAccessor pluginAccessor = mock(PluginAccessor.class);
        JavaPlugin plugin = mock(JavaPlugin.class);
        BanRepository banRepository = mock(BanRepository.class);
        Player player = mock(Player.class);
        BanList banList = mock(BanList.class);
        Logger logger = mock(Logger.class);

        when(pluginAccessor.getPlugin()).thenReturn(plugin);
        when(pluginAccessor.getPluginName()).thenReturn("EzLifesteal");
        when(pluginAccessor.getBanRepository()).thenReturn(banRepository);
        when(pluginAccessor.getLogger()).thenReturn(logger);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Target");
        when(player.isOnline()).thenReturn(false);
        when(banList.isBanned("Target")).thenReturn(false);
        org.mockito.Mockito.doThrow(new StorageException("boom")).when(banRepository).saveBan(any());

        BanEnforcementService service = new BanEnforcementService(pluginAccessor);
        try (MockedStatic<SchedulerAdapter> schedulerAdapter = org.mockito.Mockito.mockStatic(SchedulerAdapter.class);
             MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            schedulerAdapter.when(() -> SchedulerAdapter.run(eq(plugin), any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        invocation.<Runnable>getArgument(1).run();
                        return null;
                    });
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.NAME)).thenReturn(banList);

            service.applyBanWithStorage(player, "reason", "kick");

            verify(logger).warning(org.mockito.ArgumentMatchers.contains("Failed to persist ban record"));
            verify(banList).addBan("Target", "reason", (Instant) null, "EzLifesteal");
            verify(player, never()).kickPlayer(anyString());
        }
    }

    @Test
    void applyBanWithStorageSkipsAddBanWhenNameIsNullOrAlreadyBanned() {
        PluginAccessor pluginAccessor = mock(PluginAccessor.class);
        JavaPlugin plugin = mock(JavaPlugin.class);
        Player player = mock(Player.class);
        BanList banList = mock(BanList.class);

        when(pluginAccessor.getPlugin()).thenReturn(plugin);
        when(pluginAccessor.getPluginName()).thenReturn("EzLifesteal");
        when(pluginAccessor.getBanRepository()).thenReturn(null);
        when(player.getName()).thenReturn(null);
        when(player.isOnline()).thenReturn(true);

        BanEnforcementService service = new BanEnforcementService(pluginAccessor);
        try (MockedStatic<SchedulerAdapter> schedulerAdapter = org.mockito.Mockito.mockStatic(SchedulerAdapter.class);
             MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            schedulerAdapter.when(() -> SchedulerAdapter.run(eq(plugin), any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        invocation.<Runnable>getArgument(1).run();
                        return null;
                    });
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.NAME)).thenReturn(banList);

            service.applyBanWithStorage(player, "reason", "kick");

            verify(banList, never()).addBan(anyString(), anyString(), org.mockito.ArgumentMatchers.<Instant>any(),
                    anyString());
            verify(player).kickPlayer("kick");
        }
    }
}
