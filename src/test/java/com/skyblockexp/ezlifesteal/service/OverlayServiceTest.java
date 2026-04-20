package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.overlay.HeartOverlayManager;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import com.skyblockexp.ezlifesteal.runtime.Registry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OverlayServiceTest {

    @Test
    void setupOverlayCreatesManagerAndReloadsWithFallbackMaxHearts() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Registry registry = mock(Registry.class);
        Registry.ManagerState managerState = mock(Registry.ManagerState.class);

        when(runtime.getRegistry()).thenReturn(registry);
        when(registry.getManagerState()).thenReturn(managerState);
        when(runtime.getActionBarSection()).thenReturn(mock(ConfigurationSection.class));
        when(runtime.getLifestealManager()).thenReturn(null);

        try (MockedConstruction<HeartOverlayManager> construction
                = org.mockito.Mockito.mockConstruction(HeartOverlayManager.class)) {
            OverlayService service = new OverlayService(runtime, plugin);
            service.setupOverlay();

            HeartOverlayManager created = construction.constructed().getFirst();
            verify(managerState).setHeartOverlayManager(created);
            verify(created).reload(any(ConfigurationSection.class), org.mockito.Mockito.eq(20.0));
        }
    }

    @Test
    void setupOverlaySendsOrClearsStatusForOnlinePlayersBasedOnWorldAndGlobalFlags() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Registry registry = mock(Registry.class);
        Registry.ManagerState managerState = mock(Registry.ManagerState.class);
        HeartOverlayManager manager = mock(HeartOverlayManager.class);
        LifestealManager lifestealManager = mock(LifestealManager.class);
        Player player = mock(Player.class);
        World world = mock(World.class);
        UUID playerId = UUID.randomUUID();

        when(runtime.getRegistry()).thenReturn(registry);
        when(registry.getManagerState()).thenReturn(managerState);
        when(managerState.getHeartOverlayManager()).thenReturn(manager);
        when(runtime.getActionBarSection()).thenReturn(mock(ConfigurationSection.class));
        when(runtime.getLifestealManager()).thenReturn(lifestealManager);
        when(lifestealManager.getMaxHearts()).thenReturn(40.0);
        when(manager.isEnabled()).thenReturn(true);
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("world");
        when(lifestealManager.getLoadedProfile(playerId)).thenReturn(Optional.of(new LifestealProfile(playerId, 6.0)));

        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(player));

            when(runtime.isGlobalLifestealEnabled()).thenReturn(false);
            OverlayService service = new OverlayService(runtime, plugin);
            service.setupOverlay();
            verify(manager).clear(playerId);

            org.mockito.Mockito.reset(manager);
            when(manager.isEnabled()).thenReturn(true);
            when(runtime.isGlobalLifestealEnabled()).thenReturn(true);
            when(runtime.isLifestealEnabledInWorld("world")).thenReturn(true);

            service.setupOverlay();
            verify(manager).sendHeartStatus(player, 6.0);
        }
    }

    @Test
    void sendHeartStatusHandlesMissingManagerAndDisabledGlobalLifesteal() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        Registry registry = mock(Registry.class);
        Registry.ManagerState managerState = mock(Registry.ManagerState.class);
        HeartOverlayManager manager = mock(HeartOverlayManager.class);
        Player player = mock(Player.class);
        UUID playerId = UUID.randomUUID();

        when(runtime.getRegistry()).thenReturn(registry);
        when(registry.getManagerState()).thenReturn(managerState);
        when(player.getUniqueId()).thenReturn(playerId);

        OverlayService service = new OverlayService(runtime, mock(EzLifestealPlugin.class));
        when(managerState.getHeartOverlayManager()).thenReturn(null);
        service.sendHeartStatus(player, 5.0);

        when(managerState.getHeartOverlayManager()).thenReturn(manager);
        when(runtime.isGlobalLifestealEnabled()).thenReturn(false);
        service.sendHeartStatus(player, 5.0);

        verify(manager).clear(playerId);
        verify(manager, never()).sendHeartStatus(player, 5.0);
    }

    @Test
    void stateAndLifecycleMethodsDelegateSafely() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        Registry registry = mock(Registry.class);
        Registry.ManagerState managerState = mock(Registry.ManagerState.class);
        HeartOverlayManager manager = mock(HeartOverlayManager.class);

        when(runtime.getRegistry()).thenReturn(registry);
        when(registry.getManagerState()).thenReturn(managerState);
        when(managerState.getHeartOverlayManager()).thenReturn(manager);
        when(manager.isEnabled()).thenReturn(true);

        OverlayService service = new OverlayService(runtime, mock(EzLifestealPlugin.class));
        service.clear(UUID.randomUUID());
        service.shutdown();

        assertTrue(service.isEnabled());

        when(managerState.getHeartOverlayManager()).thenReturn(null);
        assertFalse(service.isEnabled());
    }
}
