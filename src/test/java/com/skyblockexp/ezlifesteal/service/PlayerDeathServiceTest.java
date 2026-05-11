package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.detector.AdminDetector;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerDeathServiceTest {

    @Test
    void handlePlayerDeathBansVictimWhenHeartsReachZero() {
        PluginAccessor plugin = basePlugin();
        BanEnforcementService banService = mock(BanEnforcementService.class);
        LifestealManager manager = mock(LifestealManager.class);
        Player victim = player("victim", "world");
        UUID victimId = victim.getUniqueId();

        LifestealProfile victimProfile = new LifestealProfile(victimId, 1.0);

        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getHeartsLostOnDeath("world")).thenReturn(2.0);
        when(plugin.isBanWhenZeroHearts("world")).thenReturn(true);
        when(plugin.getAdminDetector()).thenReturn(null);

        when(manager.getOrCreateProfile(victimId)).thenReturn(victimProfile);
        when(manager.getMinHearts()).thenReturn(0.0);
        when(manager.saveProfileAsync(victimProfile)).thenReturn(CompletableFuture.completedFuture(null));

        PlayerDeathService service = new PlayerDeathService(plugin, banService);
        service.handlePlayerDeath(victim, null, false, "", "");

        verify(banService).applyBanWithStorage(eq(victim), anyString(), anyString());
        verify(plugin, never()).executeZeroHeartCommands(any(), any(), anyDouble());
    }

    @Test
    void handlePlayerDeathReturnsEarlyWhenGlobalLifestealDisabled() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        Player victim = player("victim", "world");

        when(plugin.isGlobalLifestealEnabled()).thenReturn(false);

        PlayerDeathService service = new PlayerDeathService(plugin, mock(BanEnforcementService.class));
        service.handlePlayerDeath(victim, null, false, "", "");

        verify(plugin, never()).getLifestealManager();
        verify(plugin, never()).requestTopHologramUpdate();
    }

    @Test
    void handlePlayerDeathReturnsEarlyWhenVictimWorldDisabled() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        Player victim = player("victim", "world_nether");

        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
        when(plugin.isLifestealEnabledInWorld("world_nether")).thenReturn(false);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));

        PlayerDeathService service = new PlayerDeathService(plugin, mock(BanEnforcementService.class));
        service.handlePlayerDeath(victim, null, false, "", "");

        verify(plugin, never()).getLifestealManager();
        verify(plugin, never()).requestTopHologramUpdate();
    }

    @Test
    void handlePlayerDeathSkipsLifestealForTeamKillBypass() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        Player victim = player("victim", "world");
        Player killer = player("killer", "world");

        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
        when(plugin.isLifestealEnabledInWorld("world")).thenReturn(true);
        when(plugin.isTeamKillBypassEnabled()).thenReturn(true);
        when(plugin.shouldBypassForTeamKill(killer, victim)).thenReturn(true);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));

        PlayerDeathService service = new PlayerDeathService(plugin, mock(BanEnforcementService.class));
        service.handlePlayerDeath(victim, killer, false, "", "");

        verify(plugin, never()).getLifestealManager();
        verify(plugin, never()).requestTopHologramUpdate();
    }

    @Test
    void handlePlayerDeathSkipsMobHeartLossWhenDontRemoveHeartsFromMobsEnabled() {
        PluginAccessor plugin = basePlugin();
        LifestealManager manager = mock(LifestealManager.class);
        Player victim = player("victim", "world");
        UUID victimId = victim.getUniqueId();
        LifestealProfile victimProfile = new LifestealProfile(victimId, 8.0);

        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.isDontRemoveHeartsFromMobs()).thenReturn(true);
        when(plugin.getAdminDetector()).thenReturn(null);
        when(manager.getOrCreateProfile(victimId)).thenReturn(victimProfile);

        try (MockedStatic<SchedulerAdapter> scheduler = org.mockito.Mockito.mockStatic(SchedulerAdapter.class)) {
            scheduler.when(() -> SchedulerAdapter.run(any(), any())).then(invocation -> {
                invocation.<Runnable>getArgument(1).run();
                return null;
            });

            PlayerDeathService service = new PlayerDeathService(plugin, mock(BanEnforcementService.class));
            service.handlePlayerDeath(victim, null, true, "", "");

            verify(manager, never()).saveProfileAsync(any());
            verify(plugin, never()).executeZeroHeartCommands(any(), any(), anyDouble());
        }
    }

    @Test
    void handlePlayerDeathSkipsMobHeartLossWhenBelowThreshold() {
        PluginAccessor plugin = basePlugin();
        LifestealManager manager = mock(LifestealManager.class);
        Player victim = player("victim", "world");
        UUID victimId = victim.getUniqueId();
        LifestealProfile victimProfile = new LifestealProfile(victimId, 4.0);

        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.isDontRemoveHeartsFromMobs()).thenReturn(false);
        when(plugin.getMobRemoveHeartsGreaterThan()).thenReturn(5.0);
        when(plugin.getAdminDetector()).thenReturn(null);
        when(manager.getOrCreateProfile(victimId)).thenReturn(victimProfile);

        try (MockedStatic<SchedulerAdapter> scheduler = org.mockito.Mockito.mockStatic(SchedulerAdapter.class)) {
            scheduler.when(() -> SchedulerAdapter.run(any(), any())).then(invocation -> {
                invocation.<Runnable>getArgument(1).run();
                return null;
            });

            PlayerDeathService service = new PlayerDeathService(plugin, mock(BanEnforcementService.class));
            service.handlePlayerDeath(victim, null, true, "", "");

            verify(manager, never()).saveProfileAsync(any());
        }
    }

    @ParameterizedTest
    @CsvSource({
        "true,false,8.0,9.0",
        "false,true,6.0,7.0"
    })
    void handlePlayerDeathAppliesAdminBypassForVictimAndKiller(boolean bypassVictimLoss,
                                                                boolean bypassKillerGain,
                                                                double expectedVictimHearts,
                                                                double expectedKillerHearts) {
        PluginAccessor plugin = basePlugin();
        BanEnforcementService banService = mock(BanEnforcementService.class);
        LifestealManager manager = mock(LifestealManager.class);
        AdminDetector adminDetector = mock(AdminDetector.class);

        Player victim = player("victim", "world");
        Player killer = player("killer", "world");

        LifestealProfile victimProfile = new LifestealProfile(victim.getUniqueId(), 8.0);
        LifestealProfile killerProfile = new LifestealProfile(killer.getUniqueId(), 7.0);

        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getAdminDetector()).thenReturn(adminDetector);
        when(plugin.isAdminBypassHeartLoss()).thenReturn(bypassVictimLoss);
        when(plugin.isAdminBypassHeartGain()).thenReturn(bypassKillerGain);
        when(plugin.getHeartsPerKill("world")).thenReturn(2.0);
        when(plugin.getHeartsLostOnDeath("world")).thenReturn(2.0);
        when(plugin.isDropHeartOnDeath()).thenReturn(false);

        when(adminDetector.isAdmin(victim)).thenReturn(bypassVictimLoss);
        when(adminDetector.isAdmin(killer)).thenReturn(bypassKillerGain);

        when(manager.getOrCreateProfile(victim.getUniqueId())).thenReturn(victimProfile);
        when(manager.getOrCreateProfile(killer.getUniqueId())).thenReturn(killerProfile);
        when(manager.getMinHearts()).thenReturn(0.0);
        when(manager.getMaxHearts()).thenReturn(40.0);
        when(manager.saveProfileAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        try (MockedStatic<SchedulerAdapter> scheduler = org.mockito.Mockito.mockStatic(SchedulerAdapter.class)) {
            scheduler.when(() -> SchedulerAdapter.run(any(), any())).then(invocation -> {
                invocation.<Runnable>getArgument(1).run();
                return null;
            });

            PlayerDeathService service = new PlayerDeathService(plugin, banService);
            service.handlePlayerDeath(victim, killer, false, "", "");

            assertEquals(expectedVictimHearts, victimProfile.getHearts());
            assertEquals(expectedKillerHearts, killerProfile.getHearts());
            verify(plugin, never()).executeZeroHeartCommands(any(), any(), anyDouble());
        }
    }

    @Test
    void handlePlayerDeathFallsBackToNumericGainWhenDropHeartEnabledAndRegistryMissing() {
        PluginAccessor plugin = basePlugin();
        LifestealManager manager = mock(LifestealManager.class);
        Player victim = player("victim", "world");
        Player killer = player("killer", "world");

        LifestealProfile victimProfile = new LifestealProfile(victim.getUniqueId(), 10.0);
        LifestealProfile killerProfile = new LifestealProfile(killer.getUniqueId(), 5.0);

        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getAdminDetector()).thenReturn(null);
        when(plugin.getHeartsPerKill("world")).thenReturn(2.0);
        when(plugin.getHeartsLostOnDeath("world")).thenReturn(1.0);
        when(plugin.isDropHeartOnDeath()).thenReturn(true);
        when(plugin.getHeartRegistry()).thenReturn(null);
        when(manager.getOrCreateProfile(victim.getUniqueId())).thenReturn(victimProfile);
        when(manager.getOrCreateProfile(killer.getUniqueId())).thenReturn(killerProfile);
        when(manager.getMinHearts()).thenReturn(0.0);
        when(manager.getMaxHearts()).thenReturn(40.0);
        when(manager.saveProfileAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        try (MockedStatic<SchedulerAdapter> scheduler = org.mockito.Mockito.mockStatic(SchedulerAdapter.class)) {
            scheduler.when(() -> SchedulerAdapter.run(any(), any())).then(invocation -> {
                invocation.<Runnable>getArgument(1).run();
                return null;
            });

            PlayerDeathService service = new PlayerDeathService(plugin, mock(BanEnforcementService.class));
            service.handlePlayerDeath(victim, killer, false, "", "");

            assertEquals(7.0, killerProfile.getHearts());
            verify(manager, times(2)).saveProfileAsync(any());
        }
    }

    @Test
    void handlePlayerDeathUsesBanMessageFallbackWhenTemplatesBlank() {
        PluginAccessor plugin = basePlugin();
        BanEnforcementService banService = mock(BanEnforcementService.class);
        LifestealManager manager = mock(LifestealManager.class);
        Player victim = player("victim", "world");
        LifestealProfile victimProfile = new LifestealProfile(victim.getUniqueId(), 1.0);

        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getAdminDetector()).thenReturn(null);
        when(plugin.getHeartsLostOnDeath("world")).thenReturn(2.0);
        when(plugin.isBanWhenZeroHearts("world")).thenReturn(true);
        when(manager.getOrCreateProfile(victim.getUniqueId())).thenReturn(victimProfile);
        when(manager.getMinHearts()).thenReturn(0.0);
        when(manager.saveProfileAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        PlayerDeathService service = new PlayerDeathService(plugin, banService);
        service.handlePlayerDeath(victim, null, false, "  ", "");

        verify(banService).applyBanWithStorage(
                eq(victim),
                eq("You have run out of hearts."),
                eq("You have run out of hearts.")
        );
    }

    @Test
    void handlePlayerDeathExecutesZeroHeartCommandsWhenBanDisabled() {
        PluginAccessor plugin = basePlugin();
        LifestealManager manager = mock(LifestealManager.class);
        Player victim = player("victim", "world");
        Player killer = player("killer", "world");

        LifestealProfile victimProfile = new LifestealProfile(victim.getUniqueId(), 1.0);
        LifestealProfile killerProfile = new LifestealProfile(killer.getUniqueId(), 3.0);

        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getAdminDetector()).thenReturn(null);
        when(plugin.getHeartsLostOnDeath("world")).thenReturn(2.0);
        when(plugin.isBanWhenZeroHearts("world")).thenReturn(false);
        when(plugin.getHeartsPerKill("world")).thenReturn(0.0);
        when(plugin.isDropHeartOnDeath()).thenReturn(false);

        when(manager.getOrCreateProfile(victim.getUniqueId())).thenReturn(victimProfile);
        when(manager.getOrCreateProfile(killer.getUniqueId())).thenReturn(killerProfile);
        when(manager.getMinHearts()).thenReturn(0.0);
        when(manager.getMaxHearts()).thenReturn(40.0);
        when(manager.saveProfileAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        try (MockedStatic<SchedulerAdapter> scheduler = org.mockito.Mockito.mockStatic(SchedulerAdapter.class)) {
            scheduler.when(() -> SchedulerAdapter.run(any(), any())).then(invocation -> {
                invocation.<Runnable>getArgument(1).run();
                return null;
            });

            PlayerDeathService service = new PlayerDeathService(plugin, mock(BanEnforcementService.class));
            service.handlePlayerDeath(victim, killer, false, "", "");

            verify(plugin).executeZeroHeartCommands(victim, killer, 0.0);
        }
    }

    private PluginAccessor basePlugin() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
        when(plugin.isLifestealEnabledInWorld(anyString())).thenReturn(true);
        when(plugin.isDontRemoveHeartsFromMobs()).thenReturn(false);
        when(plugin.getMobRemoveHeartsGreaterThan()).thenReturn(-1.0);
        when(plugin.isAdminBypassHeartLoss()).thenReturn(false);
        when(plugin.isAdminBypassHeartGain()).thenReturn(false);
        when(plugin.isBanWhenZeroHearts(anyString())).thenReturn(false);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test"));
        when(plugin.getPlugin()).thenReturn(mock(JavaPlugin.class));
        return plugin;
    }

    private Player player(String name, String worldName) {
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn(name);
        when(player.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn(worldName);
        return player;
    }
}
