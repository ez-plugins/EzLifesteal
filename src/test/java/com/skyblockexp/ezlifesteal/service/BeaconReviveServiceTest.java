package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.config.ReviveAnimationSettings;
import com.skyblockexp.ezlifesteal.heart.Heart;
import com.skyblockexp.ezlifesteal.heart.HeartRegistry;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.storage.repository.BanRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BeaconReviveServiceTest {

    @Test
    void resolveStrategyFallsBackToCommandSelectionForInvalidValue() {
        BeaconReviveService service = new BeaconReviveService(Mockito.mock(PluginAccessor.class));

        assertEquals(BeaconReviveService.ReviveTargetStrategy.COMMAND_SELECTION, service.resolveStrategy("invalid"));
        assertEquals(BeaconReviveService.ReviveTargetStrategy.COMMAND_SELECTION, service.resolveStrategy(null));
    }

    @Test
    void resolveStrategyParsesNearestBannedCaseInsensitive() {
        BeaconReviveService service = new BeaconReviveService(Mockito.mock(PluginAccessor.class));

        assertEquals(BeaconReviveService.ReviveTargetStrategy.NEAREST_BANNED,
                service.resolveStrategy("nearest_banned"));
    }

    @Test
    void resolveStrategyParsesCommandSelectionCaseInsensitive() {
        BeaconReviveService service = new BeaconReviveService(Mockito.mock(PluginAccessor.class));

        assertEquals(BeaconReviveService.ReviveTargetStrategy.COMMAND_SELECTION,
                service.resolveStrategy("command_selection"));
    }

    @Test
    void tryHandleBeaconInteractRevivesTargetAndConsumesVoucher() throws Exception {
        PluginAccessor plugin = basePlugin();
        ReviveAnimationService reviveAnimationService = mock(ReviveAnimationService.class);
        BeaconReviveService service = new BeaconReviveService(plugin, reviveAnimationService);
        Player interactor = mockPlayer("interactor", UUID.randomUUID());
        Player target = mockPlayer("target", UUID.randomUUID());
        World world = mock(World.class);
        Block beacon = mock(Block.class);
        Location beaconLocation = mock(Location.class);
        Location targetLocation = mock(Location.class);
        ItemStack heldItem = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer container = mock(PersistentDataContainer.class);

        LifestealManager manager = mock(LifestealManager.class);
        EzLifestealPlugin javaPlugin = mock(EzLifestealPlugin.class);
        BanRepository banRepository = mock(BanRepository.class);
        LifestealProfile profile = new LifestealProfile(target.getUniqueId(), 2.0D);
        HeartRegistry registry = mock(HeartRegistry.class);
        Heart heart = mock(Heart.class);
        BanList nameBanList = mock(BanList.class);
        BanList profileBanList = mock(BanList.class);
        OfflinePlayer offlineTarget = mock(OfflinePlayer.class);

        when(plugin.isReviveBeaconEnabled()).thenReturn(true);
        when(plugin.isReviveBeaconRequireSneak()).thenReturn(false);
        when(plugin.getReviveBeaconVoucherHeartId()).thenReturn("revive");
        when(plugin.getHeartRegistry()).thenReturn(registry);
        when(registry.getById("revive")).thenReturn(heart);
        when(plugin.getHeartIdFrom(container)).thenReturn("revive");
        when(plugin.getReviveBeaconMaxDistance()).thenReturn(8.0D);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getReviveAnimationSettings()).thenReturn(ReviveAnimationSettings.defaults());
        when(manager.getDefaultHearts()).thenReturn(10.0D);
        when(manager.loadProfileAsync(target.getUniqueId())).thenReturn(CompletableFuture.completedFuture(profile));
        when(manager.saveProfileAsync(profile)).thenReturn(CompletableFuture.completedFuture(null));
        when(manager.getPlugin()).thenReturn(javaPlugin);
        ExecutorService storageExecutor = mock(ExecutorService.class);
        when(storageExecutor.submit(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return CompletableFuture.completedFuture(null);
        });
        Mockito.doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(storageExecutor).execute(any(Runnable.class));
        when(javaPlugin.getStorageExecutor()).thenReturn(storageExecutor);
        when(plugin.getBanRepository()).thenReturn(banRepository);

        when(beacon.getType()).thenReturn(Material.BEACON);
        when(beacon.getLocation()).thenReturn(beaconLocation);
        when(interactor.getWorld()).thenReturn(world);
        when(world.getPlayers()).thenReturn(List.of(interactor, target));
        when(target.getLocation()).thenReturn(targetLocation);
        when(beaconLocation.distanceSquared(targetLocation)).thenReturn(1.0D);

        when(heldItem.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(container);
        when(heldItem.getAmount()).thenReturn(2);

        when(nameBanList.isBanned("target")).thenReturn(true);
        when(profileBanList.isBanned(target.getUniqueId().toString())).thenReturn(false);
        when(offlineTarget.isOnline()).thenReturn(false);

        try (MockedStatic<Bukkit> bukkit = mockBukkit(nameBanList, profileBanList)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer(target.getUniqueId())).thenReturn(offlineTarget);
            service.selectReviveTarget(interactor, target.getName());

            assertTrue(service.tryHandleBeaconInteract(interactor, heldItem, beacon));

            verify(manager).saveProfileAsync(profile);
            verify(banRepository).removeBan(target.getUniqueId());
            verify(profileBanList).pardon(target.getUniqueId().toString());
            verify(nameBanList).pardon("target");
            verify(heldItem).setAmount(1);
            verify(plugin).requestTopHologramUpdate();
            verify(reviveAnimationService).playReviveAnimation(eq(beaconLocation), eq(interactor),
                    any(ReviveAnimationSettings.class));
            assertEquals(10.0D, profile.getHearts());
        }
    }

    @Test
    void tryHandleBeaconInteractReturnsFalseWhenFeatureDisabled() {
        PluginAccessor plugin = basePlugin();
        BeaconReviveService service = new BeaconReviveService(plugin);
        Player player = mockPlayer("interactor", UUID.randomUUID());
        Block beacon = mock(Block.class);

        when(beacon.getType()).thenReturn(Material.BEACON);
        when(plugin.isReviveBeaconEnabled()).thenReturn(false);

        assertFalse(service.tryHandleBeaconInteract(player, mock(ItemStack.class), beacon));
        verify(plugin, never()).getLifestealManager();
    }

    @Test
    void tryHandleBeaconInteractReturnsFalseForWrongVoucherId() {
        PluginAccessor plugin = basePlugin();
        BeaconReviveService service = new BeaconReviveService(plugin);
        Player player = mockPlayer("interactor", UUID.randomUUID());
        Block beacon = mock(Block.class);
        ItemStack item = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer container = mock(PersistentDataContainer.class);
        HeartRegistry registry = mock(HeartRegistry.class);

        when(beacon.getType()).thenReturn(Material.BEACON);
        when(plugin.isReviveBeaconEnabled()).thenReturn(true);
        when(plugin.isReviveBeaconRequireSneak()).thenReturn(false);
        when(plugin.getReviveBeaconVoucherHeartId()).thenReturn("revive");
        when(plugin.getHeartRegistry()).thenReturn(registry);
        when(registry.getById("revive")).thenReturn(mock(Heart.class));
        when(item.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(container);
        when(plugin.getHeartIdFrom(container)).thenReturn("other");

        assertFalse(service.tryHandleBeaconInteract(player, item, beacon));
        verify(plugin, never()).getLifestealManager();
    }

    @Test
    void tryHandleBeaconInteractReportsStorageErrorWhenProfileLoadFails() {
        PluginAccessor plugin = basePlugin();
        BeaconReviveService service = new BeaconReviveService(plugin);
        Player interactor = mockPlayer("interactor", UUID.randomUUID());
        Player target = mockPlayer("target", UUID.randomUUID());
        World world = mock(World.class);
        Block beacon = mock(Block.class);
        Location beaconLocation = mock(Location.class);
        Location targetLocation = mock(Location.class);
        ItemStack heldItem = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer container = mock(PersistentDataContainer.class);

        LifestealManager manager = mock(LifestealManager.class);
        EzLifestealPlugin javaPlugin = mock(EzLifestealPlugin.class);
        HeartRegistry registry = mock(HeartRegistry.class);
        MessageService messageService = mock(MessageService.class);
        BanList nameBanList = mock(BanList.class);
        BanList profileBanList = mock(BanList.class);

        when(plugin.isReviveBeaconEnabled()).thenReturn(true);
        when(plugin.isReviveBeaconRequireSneak()).thenReturn(false);
        when(plugin.getReviveBeaconVoucherHeartId()).thenReturn("revive");
        when(plugin.getHeartRegistry()).thenReturn(registry);
        when(registry.getById("revive")).thenReturn(mock(Heart.class));
        when(plugin.getHeartIdFrom(container)).thenReturn("revive");
        when(plugin.getReviveBeaconMaxDistance()).thenReturn(8.0D);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(manager.getPlugin()).thenReturn(javaPlugin);
        ExecutorService storageExecutor = mock(ExecutorService.class);
        when(storageExecutor.submit(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return CompletableFuture.completedFuture(null);
        });
        Mockito.doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(storageExecutor).execute(any(Runnable.class));
        when(javaPlugin.getStorageExecutor()).thenReturn(storageExecutor);
        when(manager.loadProfileAsync(target.getUniqueId()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("load failure")));
        when(plugin.getMessageService()).thenReturn(messageService);
        when(messageService.getMessage("beacon-revive-failure")).thenReturn("storage");

        when(beacon.getType()).thenReturn(Material.BEACON);
        when(beacon.getLocation()).thenReturn(beaconLocation);
        when(interactor.getWorld()).thenReturn(world);
        when(world.getPlayers()).thenReturn(List.of(interactor, target));
        when(target.getLocation()).thenReturn(targetLocation);
        when(beaconLocation.distanceSquared(targetLocation)).thenReturn(1.0D);

        when(heldItem.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(container);

        when(nameBanList.isBanned("target")).thenReturn(true);
        when(profileBanList.isBanned(target.getUniqueId().toString())).thenReturn(false);

        try (MockedStatic<Bukkit> ignored = mockBukkit(nameBanList, profileBanList)) {
            service.selectReviveTarget(interactor, target.getName());
            assertTrue(service.tryHandleBeaconInteract(interactor, heldItem, beacon));
            verify(messageService).sendMessage(interactor, "beacon-revive-failure");
            verify(plugin.getLogger()).warning(Mockito.contains("Beacon revive failed"));
        }
    }

    @Test
    void tryHandleBeaconInteractWarnsWhenConfiguredVoucherIsMissing() {
        PluginAccessor plugin = basePlugin();
        BeaconReviveService service = new BeaconReviveService(plugin);
        Player player = mockPlayer("interactor", UUID.randomUUID());
        Block beacon = mock(Block.class);
        ItemStack item = mock(ItemStack.class);
        HeartRegistry registry = mock(HeartRegistry.class);

        when(beacon.getType()).thenReturn(Material.BEACON);
        when(plugin.isReviveBeaconEnabled()).thenReturn(true);
        when(plugin.isReviveBeaconRequireSneak()).thenReturn(false);
        when(plugin.getReviveBeaconVoucherHeartId()).thenReturn("missing-heart");
        when(plugin.getHeartRegistry()).thenReturn(registry);
        when(registry.getById("missing-heart")).thenReturn(null);

        assertFalse(service.tryHandleBeaconInteract(player, item, beacon));

        verify(plugin.getLogger()).warning(Mockito.contains("not registered in HeartRegistry"));
        verify(plugin, never()).getLifestealManager();
    }

    private static PluginAccessor basePlugin() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        JavaPlugin javaPlugin = mock(JavaPlugin.class);
        Logger logger = mock(Logger.class);
        when(plugin.getPlugin()).thenReturn(javaPlugin);
        when(plugin.getLogger()).thenReturn(logger);
        return plugin;
    }

    private static Player mockPlayer(String name, UUID uuid) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(player.getUniqueId()).thenReturn(uuid);
        return player;
    }

    private static MockedStatic<Bukkit> mockBukkit(BanList nameBanList, BanList profileBanList) {
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
        bukkit.when(() -> Bukkit.getBanList(BanList.Type.NAME)).thenReturn(nameBanList);
        bukkit.when(() -> Bukkit.getBanList(BanList.Type.PROFILE)).thenReturn(profileBanList);
        return bukkit;
    }
}
