package com.skyblockexp.ezlifesteal.listener;

import com.skyblockexp.ezlifesteal.util.ban.PlatformBanAdapter;
import com.skyblockexp.ezlifesteal.config.LifestealConfigAdapter;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.detector.AdminDetector;
import com.skyblockexp.ezlifesteal.heart.Heart;
import com.skyblockexp.ezlifesteal.heart.HeartRegistry;
import com.skyblockexp.ezlifesteal.killstreak.KillStreakManager;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerListenerTest {

    @Test
    void onPlayerJoinSendsStorageErrorWhenAsyncLoadFails() {
        PluginAccessor plugin = basePlugin();
        MessageService messageService = mock(MessageService.class);
        LifestealManager manager = mock(LifestealManager.class);
        Player player = mockPlayer("joiner", "world");
        PlayerJoinEvent event = mock(PlayerJoinEvent.class);
        when(event.getPlayer()).thenReturn(player);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
        when(manager.loadProfileAsync(any())).thenReturn(CompletableFuture.failedFuture(new RuntimeException("boom")));

        try (MockedStatic<Bukkit> bukkit = mockPaperScheduler()) {
            PlayerListener listener = new PlayerListener(plugin, "", "", false, 0L);
            listener.onPlayerJoin(event);

            verify(messageService).sendMessage(player, "storage-error");
        }
    }

    @Test
    void onPlayerJoinReturnsEarlyInDisabledWorld() {
        PluginAccessor plugin = basePlugin();
        MessageService messageService = mock(MessageService.class);
        LifestealManager manager = mock(LifestealManager.class);
        UUID playerId = UUID.randomUUID();
        LifestealProfile profile = new LifestealProfile(playerId, 6.0);
        Player player = mockPlayer("joiner", "disabled");
        when(player.getUniqueId()).thenReturn(playerId);
        PlayerJoinEvent event = mock(PlayerJoinEvent.class);
        when(event.getPlayer()).thenReturn(player);

        when(plugin.getMessageService()).thenReturn(messageService);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
        when(plugin.isLifestealEnabledInWorld("disabled")).thenReturn(false);
        when(manager.loadProfileAsync(playerId)).thenReturn(CompletableFuture.completedFuture(profile));

        try (MockedStatic<Bukkit> bukkit = mockPaperScheduler()) {
            PlayerListener listener = new PlayerListener(plugin, "", "", false, 0L);
            listener.onPlayerJoin(event);

            verify(plugin).clearHeartStatus(playerId);
            verify(manager, never()).applyHearts(any(), any());
        }
    }

    @Test
    void onPlayerJoinAppliesHeartsAndSendsStatusOnSuccess() {
        PluginAccessor plugin = basePlugin();
        MessageService messageService = mock(MessageService.class);
        LifestealManager manager = mock(LifestealManager.class);
        UUID playerId = UUID.randomUUID();
        LifestealProfile profile = new LifestealProfile(playerId, 8.0);
        Player player = mockPlayer("joiner", "world");
        when(player.getUniqueId()).thenReturn(playerId);
        PlayerJoinEvent event = mock(PlayerJoinEvent.class);
        when(event.getPlayer()).thenReturn(player);

        when(plugin.getMessageService()).thenReturn(messageService);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
        when(plugin.isLifestealEnabledInWorld("world")).thenReturn(true);
        when(manager.loadProfileAsync(playerId)).thenReturn(CompletableFuture.completedFuture(profile));

        try (MockedStatic<Bukkit> bukkit = mockPaperScheduler()) {
            PlayerListener listener = new PlayerListener(plugin, "", "", false, 0L);
            listener.onPlayerJoin(event);

            verify(plugin).sendHeartStatus(player, 8.0);
        }
    }

    @Test
    void onPlayerQuitAppliesCombatLogoutPenaltyWhenTagged() throws Exception {
        PluginAccessor plugin = basePlugin();
        LifestealManager manager = mock(LifestealManager.class);
        MessageService messageService = mock(MessageService.class);
        UUID playerId = UUID.randomUUID();
        LifestealProfile profile = new LifestealProfile(playerId, 5.0);
        Player player = mockPlayer("combat", "world");
        when(player.getUniqueId()).thenReturn(playerId);
        PlayerQuitEvent event = mock(PlayerQuitEvent.class);
        when(event.getPlayer()).thenReturn(player);

        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
        when(plugin.isLifestealEnabledInWorld("world")).thenReturn(true);
        when(plugin.getHeartsLostOnDeath("world")).thenReturn(2.0);
        when(plugin.isBanWhenZeroHearts("world")).thenReturn(false);
        when(manager.getLoadedProfile(playerId)).thenReturn(Optional.of(profile));
        when(manager.getMinHearts()).thenReturn(0.0);
        when(manager.saveProfileAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        PlayerListener listener = new PlayerListener(plugin, "", "", true, 5_000L);
        setCombatTag(listener, playerId, System.currentTimeMillis() + 60_000L);

        listener.onPlayerQuit(event);

        verify(messageService).sendMessage(eq(player), eq("combat-logout-penalty"), any());
    }

    @Test
    void onPlayerQuitSkipsCombatPenaltyWhenNotTagged() {
        PluginAccessor plugin = basePlugin();
        LifestealManager manager = mock(LifestealManager.class);
        MessageService messageService = mock(MessageService.class);
        UUID playerId = UUID.randomUUID();
        LifestealProfile profile = new LifestealProfile(playerId, 5.0);
        Player player = mockPlayer("normal", "world");
        when(player.getUniqueId()).thenReturn(playerId);
        PlayerQuitEvent event = mock(PlayerQuitEvent.class);
        when(event.getPlayer()).thenReturn(player);

        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(manager.getLoadedProfile(playerId)).thenReturn(Optional.of(profile));
        when(manager.saveProfileAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        PlayerListener listener = new PlayerListener(plugin, "", "", true, 5_000L);
        listener.onPlayerQuit(event);

        verify(messageService, never()).sendMessage(eq(player), eq("combat-logout-penalty"), any());
    }

    @Test
    void onPlayerQuitLogsProfileSaveErrors() {
        PluginAccessor plugin = basePlugin();
        LifestealManager manager = mock(LifestealManager.class);
        Logger logger = mock(Logger.class);
        UUID playerId = UUID.randomUUID();
        LifestealProfile profile = new LifestealProfile(playerId, 5.0);
        Player player = mockPlayer("normal", "world");
        when(player.getUniqueId()).thenReturn(playerId);
        PlayerQuitEvent event = mock(PlayerQuitEvent.class);
        when(event.getPlayer()).thenReturn(player);

        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.getLoadedProfile(playerId)).thenReturn(Optional.of(profile));
        when(manager.saveProfileAsync(any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("save failed")));

        PlayerListener listener = new PlayerListener(plugin, "", "", false, 0L);
        listener.onPlayerQuit(event);

        verify(logger).severe(anyString());
    }

    @Test
    void onPlayerQuitCleansUpKillstreakState() {
        PluginAccessor plugin = basePlugin();
        LifestealManager manager = mock(LifestealManager.class);
        KillStreakManager killStreak = mock(KillStreakManager.class);
        UUID playerId = UUID.randomUUID();
        Player player = mockPlayer("normal", "world");
        when(player.getUniqueId()).thenReturn(playerId);
        PlayerQuitEvent event = mock(PlayerQuitEvent.class);
        when(event.getPlayer()).thenReturn(player);

        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.getLoadedProfile(playerId)).thenReturn(Optional.empty());
        when(plugin.getKillStreakManager()).thenReturn(killStreak);

        PlayerListener listener = new PlayerListener(plugin, "", "", false, 0L);
        listener.onPlayerQuit(event);

        verify(killStreak).handleQuit(player);
    }

    @Test
    void handlePlayerDeathReturnsEarlyWhenGlobalLifestealDisabled() {
        PluginAccessor plugin = basePlugin();
        when(plugin.isGlobalLifestealEnabled()).thenReturn(false);

        PlayerListener listener = new PlayerListener(plugin, "", "", false, 0L);
        listener.handlePlayerDeath(mockPlayer("victim", "world"), null);

        verify(plugin, never()).getLifestealManager();
    }

    @Test
    void handlePlayerDeathReturnsEarlyWhenWorldDisabled() {
        PluginAccessor plugin = basePlugin();
        LifestealManager manager = mock(LifestealManager.class);
        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
        when(plugin.isLifestealEnabledInWorld("disabled")).thenReturn(false);
        when(plugin.getLifestealManager()).thenReturn(manager);

        PlayerListener listener = new PlayerListener(plugin, "", "", false, 0L);
        listener.handlePlayerDeath(mockPlayer("victim", "disabled"), null);

        verify(manager, never()).getOrCreateProfile(any());
    }

    @Test
    void handlePlayerDeathSkipsVictimHeartLossWhenAdminBypassEnabled() {
        PluginAccessor plugin = basePlugin();
        LifestealManager manager = mock(LifestealManager.class);
        AdminDetector adminDetector = mock(AdminDetector.class);
        UUID victimId = UUID.randomUUID();
        LifestealProfile victimProfile = new LifestealProfile(victimId, 10.0);
        Player victim = mockPlayer("victim", "world");
        when(victim.getUniqueId()).thenReturn(victimId);

        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
        when(plugin.isLifestealEnabledInWorld("world")).thenReturn(true);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.getOrCreateProfile(victimId)).thenReturn(victimProfile);
        when(plugin.getAdminDetector()).thenReturn(adminDetector);
        when(adminDetector.isAdmin(victim)).thenReturn(true);
        when(plugin.isAdminBypassHeartLoss()).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = mockPaperScheduler()) {
            PlayerListener listener = new PlayerListener(plugin, "", "", false, 0L);
            listener.handlePlayerDeath(victim, null);

            assertEquals(10.0, victimProfile.getHearts());
        }
    }

    @Test
    void handlePlayerDeathSkipsKillerHeartGainWhenAdminBypassEnabled() {
        PluginAccessor plugin = basePlugin();
        LifestealManager manager = mock(LifestealManager.class);
        AdminDetector adminDetector = mock(AdminDetector.class);
        UUID victimId = UUID.randomUUID();
        UUID killerId = UUID.randomUUID();
        LifestealProfile victimProfile = new LifestealProfile(victimId, 10.0);
        LifestealProfile killerProfile = new LifestealProfile(killerId, 10.0);
        Player victim = mockPlayer("victim", "world");
        Player killer = mockPlayer("killer", "world");
        when(victim.getUniqueId()).thenReturn(victimId);
        when(killer.getUniqueId()).thenReturn(killerId);

        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
        when(plugin.isLifestealEnabledInWorld("world")).thenReturn(true);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.getOrCreateProfile(victimId)).thenReturn(victimProfile);
        when(manager.getOrCreateProfile(killerId)).thenReturn(killerProfile);
        when(plugin.getAdminDetector()).thenReturn(adminDetector);
        when(adminDetector.isAdmin(killer)).thenReturn(true);
        when(plugin.isAdminBypassHeartGain()).thenReturn(true);
        when(plugin.getHeartsLostOnDeath("world")).thenReturn(1.0);
        when(manager.getMinHearts()).thenReturn(0.0);
        when(manager.saveProfileAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        try (MockedStatic<Bukkit> bukkit = mockPaperScheduler()) {
            PlayerListener listener = new PlayerListener(plugin, "", "", false, 0L);
            listener.handlePlayerDeath(victim, killer);

            assertEquals(10.0, killerProfile.getHearts());
        }
    }

    @Test
    void handlePlayerDeathSkipsMobHeartLossWhenConfigured() throws Exception {
        PluginAccessor plugin = basePlugin();
        LifestealManager manager = mock(LifestealManager.class);
        UUID victimId = UUID.randomUUID();
        LifestealProfile victimProfile = new LifestealProfile(victimId, 7.0);
        Player victim = mockPlayer("victim", "world");
        when(victim.getUniqueId()).thenReturn(victimId);

        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
        when(plugin.isLifestealEnabledInWorld("world")).thenReturn(true);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.getOrCreateProfile(victimId)).thenReturn(victimProfile);
        when(plugin.isDontRemoveHeartsFromMobs()).thenReturn(true);
        when(plugin.getHeartsLostOnDeath("world")).thenReturn(0.0);
        when(manager.getMinHearts()).thenReturn(0.0);
        when(manager.saveProfileAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        PlayerListener listener = new PlayerListener(plugin, "", "", false, 0L);
        setLastDamager(listener, victimId, mock(LivingEntity.class));

        try (MockedStatic<Bukkit> bukkit = mockPaperScheduler()) {
            listener.handlePlayerDeath(victim, null);

            assertEquals(7.0, victimProfile.getHearts());
        }
    }

    @Test
    void handlePlayerDeathSkipsMobHeartLossAtThresholdGate() throws Exception {
        PluginAccessor plugin = basePlugin();
        LifestealManager manager = mock(LifestealManager.class);
        UUID victimId = UUID.randomUUID();
        LifestealProfile victimProfile = new LifestealProfile(victimId, 4.0);
        Player victim = mockPlayer("victim", "world");
        when(victim.getUniqueId()).thenReturn(victimId);

        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
        when(plugin.isLifestealEnabledInWorld("world")).thenReturn(true);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.getOrCreateProfile(victimId)).thenReturn(victimProfile);
        when(plugin.isDontRemoveHeartsFromMobs()).thenReturn(false);
        when(plugin.getMobRemoveHeartsGreaterThan()).thenReturn(4.0);
        when(plugin.getHeartsLostOnDeath("world")).thenReturn(0.0);
        when(manager.getMinHearts()).thenReturn(0.0);
        when(manager.saveProfileAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        PlayerListener listener = new PlayerListener(plugin, "", "", false, 0L);
        setLastDamager(listener, victimId, mock(LivingEntity.class));

        try (MockedStatic<Bukkit> bukkit = mockPaperScheduler()) {
            listener.handlePlayerDeath(victim, null);

            assertEquals(4.0, victimProfile.getHearts());
        }
    }

    @Test
    void handlePlayerDeathExecutesZeroHeartCommandsWhenNotBanned() {
        PluginAccessor plugin = basePlugin();
        LifestealManager manager = mock(LifestealManager.class);
        UUID victimId = UUID.randomUUID();
        LifestealProfile victimProfile = new LifestealProfile(victimId, 1.0);
        Player victim = mockPlayer("victim", "world");
        when(victim.getUniqueId()).thenReturn(victimId);

        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
        when(plugin.isLifestealEnabledInWorld("world")).thenReturn(true);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.getOrCreateProfile(victimId)).thenReturn(victimProfile);
        when(plugin.getHeartsLostOnDeath("world")).thenReturn(1.0);
        when(manager.getMinHearts()).thenReturn(0.0);
        when(plugin.isBanWhenZeroHearts("world")).thenReturn(false);
        when(manager.saveProfileAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        try (MockedStatic<Bukkit> bukkit = mockPaperScheduler()) {
            PlayerListener listener = new PlayerListener(plugin, "", "", false, 0L);
            listener.handlePlayerDeath(victim, null);

            verify(plugin).executeZeroHeartCommands(victim, null, 0.0);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void handlePlayerDeathUsesDefaultBanMessageWhenTemplatesAreEmpty() {
        PluginAccessor plugin = basePlugin();
        LifestealManager manager = mock(LifestealManager.class);
        UUID victimId = UUID.randomUUID();
        LifestealProfile victimProfile = new LifestealProfile(victimId, 1.0);
        Player victim = mockPlayer("victim", "world");
        when(victim.getUniqueId()).thenReturn(victimId);
        when(victim.getName()).thenReturn("victim");

        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
        when(plugin.isLifestealEnabledInWorld("world")).thenReturn(true);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.getOrCreateProfile(victimId)).thenReturn(victimProfile);
        when(plugin.getHeartsLostOnDeath("world")).thenReturn(1.0);
        when(manager.getMinHearts()).thenReturn(0.0);
        when(plugin.isBanWhenZeroHearts("world")).thenReturn(true);
        when(manager.saveProfileAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        try (MockedStatic<Bukkit> bukkit = mockPaperScheduler()) {
            PlayerListener listener = new PlayerListener(plugin, "", "", false, 0L);
            listener.handlePlayerDeath(victim, null);

            verify(victim).kickPlayer("You have run out of hearts.");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void handlePlayerDeathUsesKickTemplateAsBanFallback() {
        PluginAccessor plugin = basePlugin();
        LifestealManager manager = mock(LifestealManager.class);
        UUID victimId = UUID.randomUUID();
        LifestealProfile victimProfile = new LifestealProfile(victimId, 1.0);
        Player victim = mockPlayer("victim", "world");
        when(victim.getUniqueId()).thenReturn(victimId);
        when(victim.getName()).thenReturn("victim");

        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
        when(plugin.isLifestealEnabledInWorld("world")).thenReturn(true);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.getOrCreateProfile(victimId)).thenReturn(victimProfile);
        when(plugin.getHeartsLostOnDeath("world")).thenReturn(1.0);
        when(manager.getMinHearts()).thenReturn(0.0);
        when(plugin.isBanWhenZeroHearts("world")).thenReturn(true);
        when(manager.saveProfileAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        try (MockedStatic<Bukkit> bukkit = mockPaperScheduler()) {
            PlayerListener listener = new PlayerListener(plugin, "", "&cKick only", false, 0L);
            listener.handlePlayerDeath(victim, null);

            verify(victim).kickPlayer("§cKick only");
        }
    }

    @Test
    void handlePlayerDeathFallsBackToNumericRewardWhenHeartRegistryMissing() {
        PluginAccessor plugin = basePlugin();
        LifestealManager manager = mock(LifestealManager.class);
        UUID victimId = UUID.randomUUID();
        UUID killerId = UUID.randomUUID();
        LifestealProfile victimProfile = new LifestealProfile(victimId, 10.0);
        LifestealProfile killerProfile = new LifestealProfile(killerId, 5.0);
        Player victim = mockPlayer("victim", "world");
        Player killer = mockPlayer("killer", "world");
        when(victim.getUniqueId()).thenReturn(victimId);
        when(killer.getUniqueId()).thenReturn(killerId);

        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
        when(plugin.isLifestealEnabledInWorld("world")).thenReturn(true);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.getOrCreateProfile(victimId)).thenReturn(victimProfile);
        when(manager.getOrCreateProfile(killerId)).thenReturn(killerProfile);
        when(plugin.getHeartsLostOnDeath("world")).thenReturn(1.0);
        when(plugin.getHeartsPerKill("world")).thenReturn(2.0);
        when(manager.getMaxHearts()).thenReturn(20.0);
        when(manager.getMinHearts()).thenReturn(0.0);
        when(manager.saveProfileAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(plugin.isDropHeartOnDeath()).thenReturn(true);
        when(plugin.getHeartRegistry()).thenReturn(null);

        try (MockedStatic<Bukkit> bukkit = mockPaperScheduler()) {
            PlayerListener listener = new PlayerListener(plugin, "", "", false, 0L);
            listener.handlePlayerDeath(victim, killer);

            assertEquals(7.0, killerProfile.getHearts());
        }
    }

    @Test
    void handlePlayerDeathUsesTierFallbackWhenDropHeartIdMissing() {
        PluginAccessor plugin = basePlugin();
        LifestealManager manager = mock(LifestealManager.class);
        HeartRegistry registry = mock(HeartRegistry.class);
        Heart heart = mock(Heart.class);
        UUID victimId = UUID.randomUUID();
        UUID killerId = UUID.randomUUID();
        LifestealProfile victimProfile = new LifestealProfile(victimId, 10.0);
        LifestealProfile killerProfile = new LifestealProfile(killerId, 5.0);
        Player victim = mockPlayer("victim", "world");
        Player killer = mockPlayer("killer", "world");
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack reward = mock(ItemStack.class);
        ItemStack cloned = mock(ItemStack.class);
        when(victim.getUniqueId()).thenReturn(victimId);
        when(killer.getUniqueId()).thenReturn(killerId);
        when(killer.getInventory()).thenReturn(inventory);

        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
        when(plugin.isLifestealEnabledInWorld("world")).thenReturn(true);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.getOrCreateProfile(victimId)).thenReturn(victimProfile);
        when(manager.getOrCreateProfile(killerId)).thenReturn(killerProfile);
        when(plugin.getHeartsLostOnDeath("world")).thenReturn(1.0);
        when(plugin.getHeartsPerKill("world")).thenReturn(2.0);
        when(manager.getMinHearts()).thenReturn(0.0);
        when(manager.saveProfileAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(plugin.isDropHeartOnDeath()).thenReturn(true);
        when(plugin.getHeartRegistry()).thenReturn(registry);
        when(plugin.getDropHeartId()).thenReturn("missing");
        when(registry.getById("missing")).thenReturn(null);
        when(registry.getByTier(2)).thenReturn(heart);
        when(plugin.getDropHeartAmount()).thenReturn(1);
        when(heart.createItemStack()).thenReturn(reward);
        when(reward.clone()).thenReturn(cloned);
        when(inventory.addItem(cloned)).thenReturn(new HashMap<>());

        try (MockedStatic<Bukkit> bukkit = mockPaperScheduler()) {
            PlayerListener listener = new PlayerListener(plugin, "", "", false, 0L);
            listener.handlePlayerDeath(victim, killer);

            verify(inventory).addItem(cloned);
        }
    }

    @Test
    void handlePlayerDeathFallsBackToNumericRewardWhenHeartIdAndTierMissing() {
        PluginAccessor plugin = basePlugin();
        LifestealManager manager = mock(LifestealManager.class);
        HeartRegistry registry = mock(HeartRegistry.class);
        UUID victimId = UUID.randomUUID();
        UUID killerId = UUID.randomUUID();
        LifestealProfile victimProfile = new LifestealProfile(victimId, 10.0);
        LifestealProfile killerProfile = new LifestealProfile(killerId, 5.0);
        Player victim = mockPlayer("victim", "world");
        Player killer = mockPlayer("killer", "world");
        when(victim.getUniqueId()).thenReturn(victimId);
        when(killer.getUniqueId()).thenReturn(killerId);

        when(plugin.isGlobalLifestealEnabled()).thenReturn(true);
        when(plugin.isLifestealEnabledInWorld("world")).thenReturn(true);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.getOrCreateProfile(victimId)).thenReturn(victimProfile);
        when(manager.getOrCreateProfile(killerId)).thenReturn(killerProfile);
        when(plugin.getHeartsLostOnDeath("world")).thenReturn(1.0);
        when(plugin.getHeartsPerKill("world")).thenReturn(2.0);
        when(manager.getMaxHearts()).thenReturn(20.0);
        when(manager.getMinHearts()).thenReturn(0.0);
        when(manager.saveProfileAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(plugin.isDropHeartOnDeath()).thenReturn(true);
        when(plugin.getHeartRegistry()).thenReturn(registry);
        when(plugin.getDropHeartId()).thenReturn("missing");
        when(registry.getById("missing")).thenReturn(null);
        when(registry.getByTier(2)).thenReturn(null);

        try (MockedStatic<Bukkit> bukkit = mockPaperScheduler()) {
            PlayerListener listener = new PlayerListener(plugin, "", "", false, 0L);
            listener.handlePlayerDeath(victim, killer);

            assertEquals(7.0, killerProfile.getHearts());
        }
    }

    @Test
    void onPlayerInteractReturnsWhenHeartIdIsAbsent() {
        PluginAccessor plugin = basePlugin();
        Player player = mockPlayer("redeemer", "world");
        ItemStack item = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer container = mock(PersistentDataContainer.class);
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);

        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        when(event.getPlayer()).thenReturn(player);
        when(event.getItem()).thenReturn(item);
        when(item.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(container);
        when(plugin.getHeartIdFrom(container)).thenReturn(null);

        PlayerListener listener = new PlayerListener(plugin, "", "", false, 0L);
        listener.onPlayerInteract(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    void onPlayerInteractStopsAtMaxHearts() {
        PluginAccessor plugin = basePlugin();
        LifestealManager manager = mock(LifestealManager.class);
        MessageService messageService = mock(MessageService.class);
        HeartRegistry registry = mock(HeartRegistry.class);
        Heart heart = mock(Heart.class);
        UUID playerId = UUID.randomUUID();
        LifestealProfile profile = new LifestealProfile(playerId, 10.0);
        Player player = mockPlayer("redeemer", "world");
        ItemStack item = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer container = mock(PersistentDataContainer.class);
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);

        when(player.getUniqueId()).thenReturn(playerId);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        when(event.getPlayer()).thenReturn(player);
        when(event.getItem()).thenReturn(item);
        when(item.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(container);
        when(plugin.getHeartIdFrom(container)).thenReturn("basic");
        when(plugin.getHeartRegistry()).thenReturn(registry);
        when(registry.getById("basic")).thenReturn(heart);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.getLoadedProfile(playerId)).thenReturn(Optional.of(profile));
        when(manager.getMaxHearts()).thenReturn(10.0);
        when(plugin.getMessageService()).thenReturn(messageService);
        when(messageService.getMessage("already-at-max")).thenReturn("configured");

        try (MockedStatic<Bukkit> bukkit = mockPaperScheduler()) {
            PlayerListener listener = new PlayerListener(plugin, "", "", false, 0L);
            listener.onPlayerInteract(event);

            verify(messageService).sendMessage(player, "already-at-max");
        }
    }

    @Test
    void onPlayerInteractRedeemsHeartAndConsumesItem() {
        PluginAccessor plugin = basePlugin();
        LifestealManager manager = mock(LifestealManager.class);
        MessageService messageService = mock(MessageService.class);
        LifestealConfigAdapter config = mock(LifestealConfigAdapter.class);
        HeartRegistry registry = mock(HeartRegistry.class);
        Heart heart = mock(Heart.class);
        UUID playerId = UUID.randomUUID();
        LifestealProfile profile = new LifestealProfile(playerId, 5.0);
        Player player = mockPlayer("redeemer", "world");
        ItemStack item = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer container = mock(PersistentDataContainer.class);
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);

        when(player.getUniqueId()).thenReturn(playerId);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        when(event.getPlayer()).thenReturn(player);
        when(event.getItem()).thenReturn(item);
        when(item.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(container);
        when(item.getAmount()).thenReturn(2);

        when(plugin.getHeartIdFrom(container)).thenReturn("basic");
        when(plugin.getHeartRegistry()).thenReturn(registry);
        when(registry.getById("basic")).thenReturn(heart);
        when(heart.getHearts()).thenReturn(2.0);
        when(heart.getId()).thenReturn("basic");
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.getLoadedProfile(playerId)).thenReturn(Optional.of(profile));
        when(manager.getMaxHearts()).thenReturn(20.0);
        when(manager.saveProfileAsync(profile)).thenReturn(CompletableFuture.completedFuture(null));
        when(plugin.getMessageService()).thenReturn(messageService);
        when(plugin.getLifestealConfigAdapter()).thenReturn(config);
        when(config.getBoolean("heart-consumption-effects.enabled", true)).thenReturn(false);

        try (MockedStatic<Bukkit> bukkit = mockPaperScheduler()) {
            PlayerListener listener = new PlayerListener(plugin, "", "", false, 0L);
            listener.onPlayerInteract(event);

            verify(item).setAmount(1);
        }
    }

    @Test
    void onPlayerInteractUsesLegacyStackingParticlesWhenSectionMissing() {
        PluginAccessor plugin = basePlugin();
        setupInteractSuccessForParticles(plugin, true);
        World world = mock(World.class);
        Player player = mockPlayer("redeemer", "world");
        Location location = mock(Location.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(location);
        when(location.add(anyDouble(), anyDouble(), anyDouble())).thenReturn(location);

        PlayerInteractEvent event = buildParticleEvent(plugin, player);

        try (MockedStatic<Bukkit> bukkit = mockPaperSchedulerWithRunLater()) {
            PlayerListener listener = new PlayerListener(plugin, "", "", false, 0L);
            listener.onPlayerInteract(event);

            verify(world, atLeastOnce()).spawnParticle(eq(Particle.DUST), eq(location), anyInt(), anyDouble(),
                    anyDouble(), anyDouble(), anyDouble(), any(Particle.DustOptions.class));
        }
    }

    @Test
    void onPlayerInteractUsesLegacyBurstParticlesWhenSectionMissing() {
        PluginAccessor plugin = basePlugin();
        setupInteractSuccessForParticles(plugin, false);
        World world = mock(World.class);
        Player player = mockPlayer("redeemer", "world");
        Location location = mock(Location.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(location);
        when(location.add(anyDouble(), anyDouble(), anyDouble())).thenReturn(location);

        PlayerInteractEvent event = buildParticleEvent(plugin, player);

        try (MockedStatic<Bukkit> bukkit = mockPaperScheduler()) {
            PlayerListener listener = new PlayerListener(plugin, "", "", false, 0L);
            listener.onPlayerInteract(event);

            verify(world, atLeastOnce()).spawnParticle(eq(Particle.DUST), eq(location), anyInt(), anyDouble(),
                    anyDouble(), anyDouble(), anyDouble(), any(Particle.DustOptions.class));
        }
    }

    private static PluginAccessor basePlugin() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        JavaPlugin javaPlugin = mock(JavaPlugin.class);
        Logger logger = mock(Logger.class);
        PlatformBanAdapter banAdapter = mock(PlatformBanAdapter.class);
        when(plugin.getPlugin()).thenReturn(javaPlugin);
        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getPluginName()).thenReturn("EzLifesteal");
        when(plugin.getBanAdapter()).thenReturn(banAdapter);
        return plugin;
    }

    private static Player mockPlayer(String name, String worldName) {
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(world.getName()).thenReturn(worldName);
        when(player.getWorld()).thenReturn(world);
        when(player.getName()).thenReturn(name);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        return player;
    }

    private static MockedStatic<Bukkit> mockPaperScheduler() {
        MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask task = mock(BukkitTask.class);
        bukkit.when(Bukkit::getServer).thenReturn(server);
        when(server.getName()).thenReturn("Paper");
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        when(scheduler.runTask(any(), any(Runnable.class))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return task;
        });
        return bukkit;
    }

    private static MockedStatic<Bukkit> mockPaperSchedulerWithRunLater() {
        MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        BukkitTask task = mock(BukkitTask.class);
        bukkit.when(Bukkit::getServer).thenReturn(server);
        when(server.getName()).thenReturn("Paper");
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        when(scheduler.runTask(any(), any(Runnable.class))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return task;
        });
        when(scheduler.runTaskLater(any(), any(Runnable.class), anyLong())).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return task;
        });
        return bukkit;
    }

    @SuppressWarnings("unchecked")
    private static void setCombatTag(PlayerListener listener, UUID playerId, long expiresAt) throws Exception {
        Field field = PlayerListener.class.getDeclaredField("combatTags");
        field.setAccessible(true);
        Map<UUID, Long> tags = (Map<UUID, Long>) field.get(listener);
        tags.put(playerId, expiresAt);
    }

    @SuppressWarnings("unchecked")
    private static void setLastDamager(PlayerListener listener, UUID playerId, LivingEntity damager) throws Exception {
        Field field = PlayerListener.class.getDeclaredField("lastDamagers");
        field.setAccessible(true);
        Map<UUID, Object> damagers = (Map<UUID, Object>) field.get(listener);
        damagers.put(playerId, damager);
    }

    private static PlayerInteractEvent buildParticleEvent(PluginAccessor plugin, Player player) {
        UUID playerId = UUID.randomUUID();
        LifestealManager manager = mock(LifestealManager.class);
        MessageService messageService = mock(MessageService.class);
        HeartRegistry registry = mock(HeartRegistry.class);
        Heart heart = mock(Heart.class);
        ItemStack item = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer container = mock(PersistentDataContainer.class);
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);

        when(player.getUniqueId()).thenReturn(playerId);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_AIR);
        when(event.getPlayer()).thenReturn(player);
        when(event.getItem()).thenReturn(item);
        when(item.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(container);
        when(item.getAmount()).thenReturn(1);

        when(plugin.getHeartIdFrom(container)).thenReturn("basic");
        when(plugin.getHeartRegistry()).thenReturn(registry);
        when(registry.getById("basic")).thenReturn(heart);
        when(heart.getHearts()).thenReturn(1.0);
        when(heart.getId()).thenReturn("basic");
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.getLoadedProfile(playerId)).thenReturn(Optional.of(new LifestealProfile(playerId, 3.0)));
        when(manager.getMaxHearts()).thenReturn(20.0);
        when(manager.saveProfileAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(plugin.getMessageService()).thenReturn(messageService);

        return event;
    }

    private static void setupInteractSuccessForParticles(PluginAccessor plugin, boolean stacking) {
        LifestealConfigAdapter config = mock(LifestealConfigAdapter.class);
        ConfigurationSection section = mock(ConfigurationSection.class);
        when(plugin.getLifestealConfigAdapter()).thenReturn(config);
        when(config.getBoolean("heart-consumption-effects.enabled", true)).thenReturn(true);
        when(config.getBoolean("heart-consumption-effects.stacking-particles", true)).thenReturn(stacking);
        when(config.getInt("heart-consumption-effects.stacking-duration-ticks", 40)).thenReturn(2);
        when(config.getSection("heart-consumption-effects.particles")).thenReturn(null);
        when(config.getString("heart-consumption-effects.particle-type", "DUST")).thenReturn("DUST");
        when(config.getInt("heart-consumption-effects.particle-count", 50)).thenReturn(10);
        when(config.getDouble("heart-consumption-effects.particle-speed", 0.1)).thenReturn(0.1);
    }
}
