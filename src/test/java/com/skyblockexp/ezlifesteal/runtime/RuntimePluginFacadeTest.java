package com.skyblockexp.ezlifesteal.runtime;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.heart.HeartRegistry;
import com.skyblockexp.ezlifesteal.hologram.TopHologramManager;
import com.skyblockexp.ezlifesteal.killstreak.KillStreakManager;
import com.skyblockexp.ezlifesteal.model.MobReward;
import com.skyblockexp.ezlifesteal.storage.Storage;
import com.skyblockexp.ezlifesteal.util.PlayerLookupService;
import java.util.List;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RuntimePluginFacadeTest {

    @Test
    void representativeGetterPassthroughsReturnPluginRuntimeAndRegistryValues() {
        Collaborators c = collaborators();
        RuntimePluginFacade facade = new RuntimePluginFacade(c.plugin, c.runtime);

        assertSame(c.plugin, facade.getPlugin());
        assertEquals("EzLifesteal", facade.getPluginName());
        assertEquals("1.2.3", facade.getPluginVersion());
        assertEquals("Alice, Bob", facade.getPluginAuthors());
        assertSame(c.heartRegistry, facade.getHeartRegistry());
        assertSame(c.lifestealManager, facade.getLifestealManager());
        assertSame(c.messageService, facade.getMessageService());
        assertSame(c.playerLookupService, facade.getPlayerLookupService());
        assertSame(c.topHologramManager, facade.getTopHologramManager());
        assertSame(c.killStreakManager, facade.getKillStreakManager());
        assertSame(c.storage, facade.getStorage());
    }


    @Test
    void facadeMethodsReturnSameValuesUsingSplitGameplayDomainState() {
        Collaborators c = collaborators();
        RuntimePluginFacade facade = new RuntimePluginFacade(c.plugin, c.runtime);

        Registry registry = c.runtime.getRegistry();
        registry.getGameplayState().getHeartRulesState().setGlobalLifestealEnabled(true);
        registry.getGameplayState().getHeartRulesState().setAdminBypassHeartLoss(true);
        registry.getGameplayState().getHeartRulesState().setAdminBypassHeartGain(true);
        registry.getGameplayState().getDropRulesState().setDropHeartOnDeath(true);
        registry.getGameplayState().getDropRulesState().setDropHeartOnlyWhenKilledByPlayer(true);
        registry.getGameplayState().getDropRulesState().setDropHeartId("basic");
        registry.getGameplayState().getDropRulesState().setDropHeartAmount(2);
        registry.getGameplayState().getReviveBeaconState().setReviveBeaconEnabled(true);
        registry.getGameplayState().getReviveBeaconState().setReviveBeaconVoucherHeartId("voucher");
        registry.getGameplayState().getReviveBeaconState().setReviveBeaconRequireSneak(true);
        registry.getGameplayState().getReviveBeaconState().setReviveBeaconMaxDistance(8.5D);
        registry.getGameplayState().getReviveBeaconState().setReviveBeaconConsumeOnFail(true);
        registry.getGameplayState().getReviveBeaconState().setReviveBeaconRequireVoucherInBeacon(true);
        registry.getGameplayState().getReviveBeaconState().setReviveBeaconVoucherHoldSeconds(3.5D);
        registry.getGameplayState().getReviveBeaconState().setReviveBeaconWhitelistEnabled(true);
        registry.getGameplayState().getReviveBeaconState().setReviveBeaconWhitelistedBeacons(List.of("beacon_one"));
        registry.getGameplayState().getReviveBeaconState().setReviveBeaconBroadcastEnabled(true);
        registry.getGameplayState().getReviveBeaconState().setReviveBeaconBroadcastHoldStartMessageKey("hold");
        registry.getGameplayState().getReviveBeaconState().setReviveBeaconBroadcastCompleteMessageKey("done");
        registry.getGameplayState().getMobRulesState().setDontRemoveHeartsFromMobs(true);
        registry.getGameplayState().getMobRulesState().setMobRemoveHeartsGreaterThan(5.0D);
        registry.getGameplayState().getMobRulesState().setMobRewards(java.util.Map.of(
                org.bukkit.entity.EntityType.ZOMBIE,
                new MobReward(org.bukkit.entity.EntityType.ZOMBIE, 1.0, java.util.Set.of(), java.util.Set.of(), null)
        ));

        assertTrue(facade.isGlobalLifestealEnabled());
        assertTrue(facade.isAdminBypassHeartLoss());
        assertTrue(facade.isAdminBypassHeartGain());
        assertTrue(facade.isDropHeartOnDeath());
        assertTrue(facade.isDropHeartOnlyWhenKilledByPlayer());
        assertEquals("basic", facade.getDropHeartId());
        assertEquals(2, facade.getDropHeartAmount());
        assertTrue(facade.isReviveBeaconEnabled());
        assertEquals("voucher", facade.getReviveBeaconVoucherHeartId());
        assertTrue(facade.isReviveBeaconRequireSneak());
        assertEquals(8.5D, facade.getReviveBeaconMaxDistance());
        assertTrue(facade.isReviveBeaconConsumeOnFail());
        assertTrue(facade.isReviveBeaconRequireVoucherInBeacon());
        assertEquals(3.5D, facade.getReviveBeaconVoucherHoldSeconds());
        assertTrue(facade.isReviveBeaconWhitelistEnabled());
        assertEquals(List.of("beacon_one"), facade.getReviveBeaconWhitelistedBeacons());
        assertTrue(facade.isReviveBeaconBroadcastEnabled());
        assertEquals("hold", facade.getReviveBeaconBroadcastHoldStartMessageKey());
        assertEquals("done", facade.getReviveBeaconBroadcastCompleteMessageKey());
        assertTrue(facade.isDontRemoveHeartsFromMobs());
        assertEquals(5.0D, facade.getMobRemoveHeartsGreaterThan());
        assertEquals(1.0, facade.getMobReward(org.bukkit.entity.EntityType.ZOMBIE).getHearts());
    }

    @Test
    void actionDelegatesForwardToRuntimeAndPlugin() {
        Collaborators c = collaborators();
        RuntimePluginFacade facade = new RuntimePluginFacade(c.plugin, c.runtime);
        UUID playerId = UUID.randomUUID();
        Player player = mock(Player.class);
        Player victim = mock(Player.class);
        Player killer = mock(Player.class);
        CommandSender initiator = mock(CommandSender.class);

        facade.clearHeartStatus(playerId);
        facade.sendHeartStatus(player, 8.5);
        facade.reloadPlugin(initiator);
        facade.simulatePlayerDeath(victim, killer);
        facade.simulatePlayerKill(killer);

        verify(c.runtime).clearHeartStatus(playerId);
        verify(c.runtime).sendHeartStatus(player, 8.5);
        verify(c.plugin).reloadPlugin(initiator);
        verify(c.runtime).simulatePlayerDeath(victim, killer);
        verify(c.runtime).simulatePlayerKill(killer);
    }

    @Test
    void worldDependentPassthroughsInvokeRuntimeUsingProvidedWorldName() {
        Collaborators c = collaborators();
        RuntimePluginFacade facade = new RuntimePluginFacade(c.plugin, c.runtime);

        facade.getHeartsLostOnDeath("world_nether");
        facade.isBanWhenZeroHearts("spawn");
        facade.getHeartsPerKill("world_the_end");

        verify(c.runtime).getHeartsLostOnDeath("world_nether");
        verify(c.runtime).isBanWhenZeroHearts("spawn");
        verify(c.runtime).getHeartsPerKill("world_the_end");
    }

    private static Collaborators collaborators() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        Registry registry = new Registry();
        PluginDescriptionFile description = mock(PluginDescriptionFile.class);

        HeartRegistry heartRegistry = mock(HeartRegistry.class);
        com.skyblockexp.ezlifesteal.service.LifestealManager lifestealManager =
                mock(com.skyblockexp.ezlifesteal.service.LifestealManager.class);
        MessageService messageService = mock(MessageService.class);
        PlayerLookupService playerLookupService = mock(PlayerLookupService.class);
        TopHologramManager topHologramManager = mock(TopHologramManager.class);
        KillStreakManager killStreakManager = mock(KillStreakManager.class);
        Storage storage = mock(Storage.class);

        registry.getManagerState().setLifestealManager(lifestealManager);
        registry.getManagerState().setPlayerLookupService(playerLookupService);
        registry.getManagerState().setTopHologramManager(topHologramManager);
        registry.getManagerState().setKillStreakManager(killStreakManager);

        when(plugin.getName()).thenReturn("EzLifesteal");
        when(plugin.getDescription()).thenReturn(description);
        when(description.getVersion()).thenReturn("1.2.3");
        when(description.getAuthors()).thenReturn(List.of("Alice", "Bob"));

        when(runtime.getRegistry()).thenReturn(registry);
        when(runtime.getHeartRegistry()).thenReturn(heartRegistry);
        when(runtime.getMessageService()).thenReturn(messageService);
        when(runtime.getStorage()).thenReturn(storage);

        return new Collaborators(plugin, runtime, heartRegistry, lifestealManager, messageService,
                playerLookupService, topHologramManager, killStreakManager, storage);
    }

    private record Collaborators(EzLifestealPlugin plugin,
                                 DefaultPluginRuntimeServices runtime,
                                 HeartRegistry heartRegistry,
                                 com.skyblockexp.ezlifesteal.service.LifestealManager lifestealManager,
                                 MessageService messageService,
                                 PlayerLookupService playerLookupService,
                                 TopHologramManager topHologramManager,
                                 KillStreakManager killStreakManager,
                                 Storage storage) {
    }
}
