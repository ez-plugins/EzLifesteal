package com.skyblockexp.ezlifesteal;

import com.skyblockexp.ezlifesteal.gui.ShopGuiListener;
import com.skyblockexp.ezlifesteal.gui.SmurfGuiListener;
import com.skyblockexp.ezlifesteal.listener.MobListener;
import com.skyblockexp.ezlifesteal.listener.PlayerListener;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.ListenerService;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListenerServiceTest {

    @Test
    void setupCoreListenersRegistersExpectedListenersAndSetsPlayerListener() {
        PluginManager pluginManager = mock(PluginManager.class);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);

            DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
            EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
            PluginAccessor accessor = mock(PluginAccessor.class);

            when(runtime.getZeroHeartBanMessage()).thenReturn("ban");
            when(runtime.getZeroHeartKickMessage()).thenReturn("kick");
            when(runtime.isCombatLogoutProtectionEnabled()).thenReturn(true);
            when(runtime.getCombatLogoutTagDurationMillis()).thenReturn(1500L);

            ListenerService service = new ListenerService(runtime, plugin);
            service.setupCoreListeners(accessor);

            // verify setPlayerListener called with a PlayerListener
            verify(runtime, times(1)).setPlayerListener(org.mockito.ArgumentMatchers.any(PlayerListener.class));

            // verify plugin manager registered 4 listeners
            ArgumentCaptor<Listener> captor = ArgumentCaptor.forClass(Listener.class);
            verify(pluginManager, times(4)).registerEvents(captor.capture(), eq(plugin));
            Set<Class<?>> listenerTypes =
                    captor.getAllValues().stream().map(Object::getClass).collect(Collectors.toSet());

            assertEquals(Set.of(PlayerListener.class, MobListener.class, SmurfGuiListener.class, ShopGuiListener.class),
                    listenerTypes);
        }
    }
}
