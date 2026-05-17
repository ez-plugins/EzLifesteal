package com.skyblockexp.ezlifesteal.placeholder;

import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.util.ban.BanEntryView;
import com.skyblockexp.ezlifesteal.util.ban.PlatformBanAdapter;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        PlatformBanAdapter banAdapter = mock(PlatformBanAdapter.class);

        UUID playerId = UUID.randomUUID();
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getPluginAuthors()).thenReturn("author");
        when(plugin.getPluginVersion()).thenReturn("1.0.0");
        when(plugin.getLogger()).thenReturn(Logger.getLogger("placeholder-test"));
        when(plugin.getBanAdapter()).thenReturn(banAdapter);

        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("Alpha");
        when(manager.getLoadedProfile(playerId)).thenReturn(Optional.of(new LifestealProfile(playerId, 12.5)));
        when(manager.getDefaultHearts()).thenReturn(10.0);
        when(manager.getMinHearts()).thenReturn(5.0);
        when(manager.getMaxHearts()).thenReturn(20.0);
        when(banAdapter.isBanned(playerId, "Alpha")).thenReturn(true);

        LifestealPlaceholderExpansion expansion = new LifestealPlaceholderExpansion(plugin, Duration.ofSeconds(5),
                Duration.ofSeconds(5));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
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

    @Test
    void isBannedByUuidParam_returnsTrueWhenBanned() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("placeholder-test"));
        PlatformBanAdapter banAdapter = mock(PlatformBanAdapter.class);
        when(plugin.getBanAdapter()).thenReturn(banAdapter);
        LifestealPlaceholderExpansion expansion = new LifestealPlaceholderExpansion(plugin, Duration.ofSeconds(5),
                Duration.ofSeconds(5));

        UUID targetId = UUID.randomUUID();
        when(banAdapter.isBanned(targetId, null)).thenReturn(true);

        assertEquals("true", expansion.onRequest(null, "is_banned_" + targetId));
    }

    @Test
    void isBannedByNameParam_returnsTrueWhenNameMatchFound() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("placeholder-test"));
        PlatformBanAdapter banAdapter = mock(PlatformBanAdapter.class);
        when(plugin.getBanAdapter()).thenReturn(banAdapter);
        BanEntryView bannedEntry = new BanEntryView(null, "BannedPlayer", null, null, null, null);
        when(banAdapter.getBanEntries()).thenReturn(Set.of(bannedEntry));
        LifestealPlaceholderExpansion expansion = new LifestealPlaceholderExpansion(plugin, Duration.ofSeconds(5),
                Duration.ofSeconds(5));

        assertEquals("true", expansion.onRequest(null, "is_banned_BannedPlayer"));
    }

    @Test
    void isBannedByNameParam_returnsFalseWhenNotFound() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("placeholder-test"));
        PlatformBanAdapter banAdapter = mock(PlatformBanAdapter.class);
        when(plugin.getBanAdapter()).thenReturn(banAdapter);
        when(banAdapter.getBanEntries()).thenReturn(Set.of());
        LifestealPlaceholderExpansion expansion = new LifestealPlaceholderExpansion(plugin, Duration.ofSeconds(5),
                Duration.ofSeconds(5));

        assertEquals("false", expansion.onRequest(null, "is_banned_Unknown"));
    }

    @Test
    void topUuidPlaceholder_returnsUuidStringAfterCacheLoad() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        LifestealManager manager = mock(LifestealManager.class);
        when(plugin.getLifestealManager()).thenReturn(manager);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("placeholder-test"));

        UUID topId = UUID.randomUUID();
        when(manager.loadTopProfilesAsync(anyInt())).thenReturn(
                CompletableFuture.completedFuture(List.of(new LifestealProfile(topId, 20.0)))
        );

        // topCacheTtl=0 ensures cache always expires; with completedFuture thenAccept runs
        // synchronously so topCache is populated before the return of onRequest.
        LifestealPlaceholderExpansion expansion = new LifestealPlaceholderExpansion(plugin, Duration.ofSeconds(5),
                Duration.ofMillis(0));

        assertEquals(topId.toString(), expansion.onRequest(null, "top_1_uuid"));
    }

    @Test
    void lifecycleMethods_returnExpectedValues() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        when(plugin.getPluginAuthors()).thenReturn("TestAuthor");
        when(plugin.getPluginVersion()).thenReturn("2.0.0");
        when(plugin.getLogger()).thenReturn(Logger.getLogger("placeholder-test"));
        LifestealPlaceholderExpansion expansion = new LifestealPlaceholderExpansion(plugin, Duration.ofSeconds(5),
                Duration.ofSeconds(5));

        assertTrue(expansion.canRegister());
        assertTrue(expansion.persist());
        assertEquals("ezlifesteal", expansion.getIdentifier());
    }
}
