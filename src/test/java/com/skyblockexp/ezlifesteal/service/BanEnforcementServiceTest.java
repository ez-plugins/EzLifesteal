package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import com.skyblockexp.ezlifesteal.storage.repository.BanRepository;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import com.skyblockexp.ezlifesteal.util.ban.PlatformBanAdapter;
import java.time.Instant;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
        PlatformBanAdapter banAdapter = mock(PlatformBanAdapter.class);
        Player player = mock(Player.class);

        UUID playerId = UUID.randomUUID();
        when(pluginAccessor.getPlugin()).thenReturn(plugin);
        when(pluginAccessor.getPluginName()).thenReturn("EzLifesteal");
        when(pluginAccessor.getBanRepository()).thenReturn(banRepository);
        when(pluginAccessor.getBanAdapter()).thenReturn(banAdapter);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("Target");
        when(player.isOnline()).thenReturn(true);

        BanEnforcementService service = new BanEnforcementService(pluginAccessor);
        try (MockedStatic<SchedulerAdapter> schedulerAdapter = org.mockito.Mockito.mockStatic(SchedulerAdapter.class)) {
            schedulerAdapter.when(() -> SchedulerAdapter.run(eq(plugin), any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        invocation.<Runnable>getArgument(1).run();
                        return null;
                    });

            service.applyBanWithStorage(player, "reason", "kick");

            verify(banRepository).saveBan(any());
            verify(banAdapter).addBan(eq(playerId), eq("Target"), eq("reason"), eq("EzLifesteal"), isNull());
            verify(player).kickPlayer("kick");
        }
    }

    @Test
    void applyBanWithStorageLogsAndContinuesWhenRepositoryThrows() throws Exception {
        PluginAccessor pluginAccessor = mock(PluginAccessor.class);
        JavaPlugin plugin = mock(JavaPlugin.class);
        BanRepository banRepository = mock(BanRepository.class);
        PlatformBanAdapter banAdapter = mock(PlatformBanAdapter.class);
        Logger logger = mock(Logger.class);
        Player player = mock(Player.class);

        when(pluginAccessor.getPlugin()).thenReturn(plugin);
        when(pluginAccessor.getPluginName()).thenReturn("EzLifesteal");
        when(pluginAccessor.getBanRepository()).thenReturn(banRepository);
        when(pluginAccessor.getBanAdapter()).thenReturn(banAdapter);
        when(pluginAccessor.getLogger()).thenReturn(logger);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Target");
        when(player.isOnline()).thenReturn(false);
        org.mockito.Mockito.doThrow(new StorageException("boom")).when(banRepository).saveBan(any());

        BanEnforcementService service = new BanEnforcementService(pluginAccessor);
        try (MockedStatic<SchedulerAdapter> schedulerAdapter = org.mockito.Mockito.mockStatic(SchedulerAdapter.class)) {
            schedulerAdapter.when(() -> SchedulerAdapter.run(eq(plugin), any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        invocation.<Runnable>getArgument(1).run();
                        return null;
                    });

            service.applyBanWithStorage(player, "reason", "kick");

            verify(logger).warning(org.mockito.ArgumentMatchers.contains("Failed to persist ban record"));
            verify(banAdapter).addBan(any(UUID.class), eq("Target"), eq("reason"), eq("EzLifesteal"), isNull());
            verify(player, never()).kickPlayer(anyString());
        }
    }

    @Test
    void applyBanWithStorageCallsBanAdapterAndKicksOnlinePlayerWithoutRepository() {
        PluginAccessor pluginAccessor = mock(PluginAccessor.class);
        JavaPlugin plugin = mock(JavaPlugin.class);
        PlatformBanAdapter banAdapter = mock(PlatformBanAdapter.class);
        Player player = mock(Player.class);

        UUID playerId = UUID.randomUUID();
        when(pluginAccessor.getPlugin()).thenReturn(plugin);
        when(pluginAccessor.getPluginName()).thenReturn("EzLifesteal");
        when(pluginAccessor.getBanRepository()).thenReturn(null);
        when(pluginAccessor.getBanAdapter()).thenReturn(banAdapter);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("Target");
        when(player.isOnline()).thenReturn(true);

        BanEnforcementService service = new BanEnforcementService(pluginAccessor);
        try (MockedStatic<SchedulerAdapter> schedulerAdapter = org.mockito.Mockito.mockStatic(SchedulerAdapter.class)) {
            schedulerAdapter.when(() -> SchedulerAdapter.run(eq(plugin), any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        invocation.<Runnable>getArgument(1).run();
                        return null;
                    });

            service.applyBanWithStorage(player, "reason", "kick");

            verify(banAdapter).addBan(eq(playerId), eq("Target"), eq("reason"), eq("EzLifesteal"), isNull());
            verify(player).kickPlayer("kick");
        }
    }
}
