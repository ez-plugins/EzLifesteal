package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.hologram.TopHologramManager;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import com.skyblockexp.ezlifesteal.runtime.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HologramServiceTest {

    @Test
    void setupHologramCreatesManagerWhenMissingAndReloads() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Registry registry = mock(Registry.class);
        Registry.ManagerState managerState = mock(Registry.ManagerState.class);
        ConfigurationSection section = mock(ConfigurationSection.class);

        when(runtime.getRegistry()).thenReturn(registry);
        when(registry.getManagerState()).thenReturn(managerState);
        when(runtime.getHologramSection(false)).thenReturn(section);

        try (MockedConstruction<TopHologramManager> construction
                = org.mockito.Mockito.mockConstruction(TopHologramManager.class)) {
            HologramService service = new HologramService(runtime, plugin);

            service.setupHologram();

            TopHologramManager created = construction.constructed().getFirst();
            verify(managerState).setTopHologramManager(created);
            verify(created).reload(section);
        }
    }

    @Test
    void setupHologramReusesExistingManager() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Registry registry = mock(Registry.class);
        Registry.ManagerState managerState = mock(Registry.ManagerState.class);
        TopHologramManager manager = mock(TopHologramManager.class);

        when(runtime.getRegistry()).thenReturn(registry);
        when(registry.getManagerState()).thenReturn(managerState);
        when(managerState.getTopHologramManager()).thenReturn(manager);
        when(runtime.getHologramSection(false)).thenReturn(mock(ConfigurationSection.class));

        HologramService service = new HologramService(runtime, plugin);
        service.setupHologram();

        verify(managerState, never()).setTopHologramManager(org.mockito.ArgumentMatchers.any());
        verify(manager).reload(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void requestShutdownAndStateQueriesHandleNullManagerGracefully() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        Registry registry = mock(Registry.class);
        Registry.ManagerState managerState = mock(Registry.ManagerState.class);

        when(runtime.getRegistry()).thenReturn(registry);
        when(registry.getManagerState()).thenReturn(managerState);
        when(managerState.getTopHologramManager()).thenReturn(null);

        HologramService service = new HologramService(runtime, mock(EzLifestealPlugin.class));

        service.requestUpdate();
        service.shutdown();

        assertFalse(service.hasHologram());
    }

    @Test
    void hasHologramDelegatesToManager() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        Registry registry = mock(Registry.class);
        Registry.ManagerState managerState = mock(Registry.ManagerState.class);
        TopHologramManager manager = mock(TopHologramManager.class);

        when(runtime.getRegistry()).thenReturn(registry);
        when(registry.getManagerState()).thenReturn(managerState);
        when(managerState.getTopHologramManager()).thenReturn(manager);
        when(manager.hasHologram()).thenReturn(true);

        HologramService service = new HologramService(runtime, mock(EzLifestealPlugin.class));

        assertTrue(service.hasHologram());
    }
}
