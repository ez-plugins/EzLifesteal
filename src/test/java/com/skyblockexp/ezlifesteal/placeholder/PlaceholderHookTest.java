package com.skyblockexp.ezlifesteal.placeholder;

import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class PlaceholderHookTest {

    @Test
    void hookExposesStableMetadataAndFlags() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        when(plugin.getPluginAuthors()).thenReturn("test-author");
        when(plugin.getPluginVersion()).thenReturn("2.3.4");

        LifestealPlaceholderExpansion expansion = new LifestealPlaceholderExpansion(plugin);

        assertInstanceOf(PlaceholderHook.class, expansion);
        assertTrue(expansion.canRegister());
        assertTrue(expansion.persist());
        assertEquals("ezlifesteal", expansion.getIdentifier());
        assertEquals("test-author", expansion.getAuthor());
        assertEquals("2.3.4", expansion.getVersion());
    }

    @Test
    void clearCacheInvalidatesExpansionStateViaHookContract() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealManager manager = mock(LifestealManager.class);
        OfflinePlayer player = mock(OfflinePlayer.class);

        UUID playerId = UUID.randomUUID();
        UUID topId = UUID.randomUUID();

        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("placeholder-test"));
        when(player.getUniqueId()).thenReturn(playerId);

        when(manager.getDefaultHearts()).thenReturn(10.0);
        when(manager.getLoadedProfile(playerId)).thenReturn(
                Optional.of(new LifestealProfile(playerId, 13.0)),
                Optional.of(new LifestealProfile(playerId, 3.0))
        );
        when(manager.loadTopProfilesAsync(anyInt())).thenReturn(
                CompletableFuture.completedFuture(List.of(new LifestealProfile(topId, 9.0))),
                CompletableFuture.completedFuture(List.of(new LifestealProfile(topId, 4.0)))
        );

        LifestealPlaceholderExpansion expansion = new LifestealPlaceholderExpansion(plugin, Duration.ofSeconds(30),
                Duration.ofSeconds(30));
        PlaceholderHook hook = expansion;

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            OfflinePlayer topOffline = mock(OfflinePlayer.class);
            bukkit.when(() -> Bukkit.getOfflinePlayer(topId)).thenReturn(topOffline);
            when(topOffline.getName()).thenReturn("Top");

            assertEquals("13", expansion.onRequest(player, "hearts"));
            assertEquals("9", expansion.onRequest(player, "top_1_hearts"));

            hook.clearCache();

            assertEquals("3", expansion.onRequest(player, "hearts"));
            assertEquals("4", expansion.onRequest(player, "top_1_hearts"));
        }
    }

    @Test
    void registrationAndUnregistrationGuardsCanBeAppliedByHookImplementations() {
        class GuardedHook implements PlaceholderHook {
            private final AtomicBoolean registered = new AtomicBoolean(false);

            @Override
            public boolean register() {
                return registered.compareAndSet(false, true);
            }

            @Override
            public boolean unregisterExpansion() {
                return registered.compareAndSet(true, false);
            }

            @Override
            public void clearCache() {
                // no-op
            }
        }

        PlaceholderHook hook = new GuardedHook();
        assertTrue(hook.register());
        assertFalse(hook.register());
        assertTrue(hook.unregisterExpansion());
        assertFalse(hook.unregisterExpansion());
    }
}
