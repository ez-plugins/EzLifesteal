package com.skyblockexp.ezlifesteal.killstreak;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KillStreakManagerTest {

    @Test
    void applySettingsNullSetsDisabledAndClearsStreakMap() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        KillStreakManager manager = new KillStreakManager(plugin);
        Player player = mock(Player.class);
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000311");
        when(player.getUniqueId()).thenReturn(playerId);

        manager.applySettings(new KillStreakSettings(true, true, Map.of()));
        manager.handleKill(player);
        assertEquals(1, manager.getCurrentStreak(playerId));

        manager.applySettings(null);

        assertFalse(manager.isEnabled());
        assertEquals(0, manager.getCurrentStreak(playerId));
        assertFalse(manager.getSettings().enabled());
    }

    @Test
    void applySettingsDisabledClearsExistingStreaks() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        KillStreakManager manager = new KillStreakManager(plugin);
        Player player = mock(Player.class);
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000312");
        when(player.getUniqueId()).thenReturn(playerId);

        manager.applySettings(new KillStreakSettings(true, true, Map.of()));
        manager.handleKill(player);
        manager.handleKill(player);
        assertEquals(2, manager.getCurrentStreak(playerId));

        manager.applySettings(KillStreakSettings.disabled());

        assertFalse(manager.isEnabled());
        assertEquals(0, manager.getCurrentStreak(playerId));
    }

    @Test
    void handleKillIsNoOpWhenPlayerNullOrSettingsDisabled() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        KillStreakManager manager = new KillStreakManager(plugin);
        Player player = mock(Player.class);
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000313");
        when(player.getUniqueId()).thenReturn(playerId);

        KillStreakReward reward = mock(KillStreakReward.class);
        KillStreakSettings enabled = new KillStreakSettings(true, true, Map.of(1, List.of(reward)));

        manager.applySettings(enabled);
        manager.handleKill(null);

        assertEquals(0, manager.getCurrentStreak(playerId));
        verify(reward, never()).apply(plugin, player);

        manager.applySettings(KillStreakSettings.disabled());
        manager.handleKill(player);

        assertEquals(0, manager.getCurrentStreak(playerId));
        verify(reward, never()).apply(plugin, player);
    }

    @Test
    void handleKillIncrementsStreakAndAppliesAllRewardsForCurrentStreak() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        KillStreakManager manager = new KillStreakManager(plugin);
        Player player = mock(Player.class);
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000314");
        when(player.getUniqueId()).thenReturn(playerId);

        KillStreakReward first = mock(KillStreakReward.class);
        KillStreakReward second = mock(KillStreakReward.class);
        KillStreakReward third = mock(KillStreakReward.class);
        KillStreakSettings settings = new KillStreakSettings(true, true, Map.of(
                1, List.of(first, second),
                2, List.of(third)
        ));
        manager.applySettings(settings);

        manager.handleKill(player);
        manager.handleKill(player);

        assertEquals(2, manager.getCurrentStreak(playerId));
        verify(first, times(1)).apply(plugin, player);
        verify(second, times(1)).apply(plugin, player);
        verify(third, times(1)).apply(plugin, player);
    }

    @Test
    void handleDeathRemovesStreakOnlyWhenResetOnDeathIsTrue() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        KillStreakManager manager = new KillStreakManager(plugin);
        Player player = mock(Player.class);
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000315");
        when(player.getUniqueId()).thenReturn(playerId);

        manager.applySettings(new KillStreakSettings(true, false, Map.of()));
        manager.handleKill(player);
        manager.handleDeath(player);
        assertEquals(1, manager.getCurrentStreak(playerId));

        manager.applySettings(new KillStreakSettings(true, true, Map.of()));
        manager.handleKill(player);
        assertEquals(2, manager.getCurrentStreak(playerId));
        manager.handleDeath(player);
        assertEquals(0, manager.getCurrentStreak(playerId));
    }

    @Test
    void handleQuitAlwaysRemovesPlayerStreak() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        KillStreakManager manager = new KillStreakManager(plugin);
        Player player = mock(Player.class);
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000316");
        when(player.getUniqueId()).thenReturn(playerId);

        manager.applySettings(new KillStreakSettings(true, true, Map.of()));
        manager.handleKill(player);
        assertEquals(1, manager.getCurrentStreak(playerId));

        manager.handleQuit(player);

        assertEquals(0, manager.getCurrentStreak(playerId));
    }

    @Test
    void getCurrentStreakReturnsZeroForNullOrUnknownUuid() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        KillStreakManager manager = new KillStreakManager(plugin);

        assertEquals(0, manager.getCurrentStreak(null));

        UUID unknown = UUID.fromString("00000000-0000-0000-0000-000000000317");
        assertEquals(0, manager.getCurrentStreak(unknown));

        Player player = mock(Player.class);
        UUID known = UUID.fromString("00000000-0000-0000-0000-000000000318");
        when(player.getUniqueId()).thenReturn(known);

        manager.applySettings(new KillStreakSettings(true, true, Map.of()));
        manager.handleKill(player);

        assertTrue(manager.getCurrentStreak(known) > 0);
    }
}
