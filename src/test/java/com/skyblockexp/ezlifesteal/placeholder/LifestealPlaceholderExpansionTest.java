package com.skyblockexp.ezlifesteal.placeholder;

import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class LifestealPlaceholderExpansionTest {

    @Test
    void knownTokensReturnExpectedValuesAndUnknownsAreEmpty() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealManager manager = mock(LifestealManager.class);
        OfflinePlayer player = mock(OfflinePlayer.class);
        BanList banList = mock(BanList.class);

        UUID playerId = UUID.randomUUID();
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getPluginAuthors()).thenReturn("author");
        when(plugin.getPluginVersion()).thenReturn("1.0.0");
        when(plugin.getLogger()).thenReturn(Logger.getLogger("placeholder-test"));

        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("Alpha");
        when(manager.getLoadedProfile(playerId)).thenReturn(Optional.of(new LifestealProfile(playerId, 12.5)));
        when(manager.getDefaultHearts()).thenReturn(10.0);
        when(manager.getMinHearts()).thenReturn(5.0);
        when(manager.getMaxHearts()).thenReturn(20.0);

        LifestealPlaceholderExpansion expansion = new LifestealPlaceholderExpansion(plugin, Duration.ofSeconds(5),
                Duration.ofSeconds(5));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.NAME)).thenReturn(banList);
            when(banList.isBanned("Alpha")).thenReturn(true);

            assertEquals("12.5", expansion.onRequest(player, "hearts"));
            assertEquals("10", expansion.onRequest(player, "default_hearts"));
            assertEquals("5", expansion.onRequest(player, "min_hearts"));
            assertEquals("20", expansion.onRequest(player, "max_hearts"));
            assertEquals("true", expansion.onRequest(player, "is_banned"));

            assertEquals("", expansion.onRequest(player, "does_not_exist"));
            assertEquals("", expansion.onRequest(player, null));
            assertEquals("", expansion.onRequest(player, "top_x_name"));
            assertEquals("", expansion.onRequest(player, "top_0_name"));
        }
    }

    @Test
    void nullAndOfflinePlayerCasesFallBackToContractDefaults() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealManager manager = mock(LifestealManager.class);
        OfflinePlayer playerWithoutName = mock(OfflinePlayer.class);
        OfflinePlayer unknownTopPlayer = mock(OfflinePlayer.class);

        UUID topPlayerId = UUID.randomUUID();

        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("placeholder-test"));
        when(manager.getDefaultHearts()).thenReturn(9.0);
        when(manager.loadTopProfilesAsync(anyInt())).thenReturn(
                CompletableFuture.completedFuture(List.of(new LifestealProfile(topPlayerId, 7.0)))
        );

        when(playerWithoutName.getName()).thenReturn(null);

        LifestealPlaceholderExpansion expansion = new LifestealPlaceholderExpansion(plugin, Duration.ofMillis(100),
                Duration.ofSeconds(2));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            BanList banList = mock(BanList.class);
            bukkit.when(() -> Bukkit.getBanList(BanList.Type.NAME)).thenReturn(banList);
            bukkit.when(() -> Bukkit.getOfflinePlayer(topPlayerId)).thenReturn(unknownTopPlayer);
            when(unknownTopPlayer.getName()).thenReturn(null);

            assertEquals("9", expansion.onRequest(null, "hearts"));
            assertEquals("", expansion.onRequest(playerWithoutName, "is_banned"));
            assertEquals("Unknown", expansion.onRequest(null, "top_1_name"));
        }
    }

    @Test
    void playerAndTopCachesReuseValuesAndCanBeInvalidated() throws Exception {
        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealManager manager = mock(LifestealManager.class);
        OfflinePlayer player = mock(OfflinePlayer.class);

        UUID playerId = UUID.randomUUID();
        UUID topId = UUID.randomUUID();

        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("placeholder-test"));
        when(player.getUniqueId()).thenReturn(playerId);

        AtomicInteger topCalls = new AtomicInteger();
        CompletableFuture<List<LifestealProfile>> firstTopLoad = new CompletableFuture<>();
        when(manager.loadTopProfilesAsync(anyInt())).thenAnswer(invocation -> {
            if (topCalls.incrementAndGet() == 1) {
                return firstTopLoad;
            }
            return CompletableFuture.completedFuture(List.of(new LifestealProfile(topId, 18.0)));
        });

        LifestealProfile initial = new LifestealProfile(playerId, 12.0);
        LifestealProfile updated = new LifestealProfile(playerId, 6.0);
        when(manager.getDefaultHearts()).thenReturn(10.0);
        when(manager.getLoadedProfile(playerId)).thenReturn(Optional.of(initial), Optional.of(updated),
                Optional.of(updated));

        LifestealPlaceholderExpansion expansion = new LifestealPlaceholderExpansion(plugin, Duration.ofMillis(200),
                Duration.ofMillis(200));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            OfflinePlayer topOffline = mock(OfflinePlayer.class);
            bukkit.when(() -> Bukkit.getOfflinePlayer(topId)).thenReturn(topOffline);
            when(topOffline.getName()).thenReturn("TopPlayer");

            assertEquals("12", expansion.onRequest(player, "hearts"));
            assertEquals("12", expansion.onRequest(player, "hearts"));
            Thread.sleep(250);
            assertEquals("6", expansion.onRequest(player, "hearts"));

            assertEquals("", expansion.onRequest(player, "top_1_hearts"));
            firstTopLoad.complete(List.of(new LifestealProfile(topId, 15.0)));
            assertEquals("15", expansion.onRequest(player, "top_1_hearts"));
            assertEquals(1, topCalls.get());

            expansion.clearCache();
            assertEquals("18", expansion.onRequest(player, "top_1_hearts"));
            assertEquals(2, topCalls.get());
        }
    }
}
