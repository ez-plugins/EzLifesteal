package com.skyblockexp.ezlifesteal.killstreak;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class KillStreakSettingsTest {

    @Test
    void disabledReturnsDisabledSettingsWithNoRewards() {
        KillStreakSettings settings = KillStreakSettings.disabled();

        assertFalse(settings.enabled());
        assertTrue(settings.resetOnDeath());
        assertEquals(List.of(), settings.rewardsFor(1));
        assertEquals(Set.of(), settings.configuredStreaks());
        assertFalse(settings.hasRewards());
    }

    @Test
    void rewardsForReturnsEmptyWhenMissingAndNonEmptyWhenConfigured() {
        KillStreakReward reward = mock(KillStreakReward.class);
        KillStreakSettings settings = new KillStreakSettings(true, true, Map.of(3, List.of(reward)));

        assertEquals(List.of(), settings.rewardsFor(2));
        assertEquals(List.of(reward), settings.rewardsFor(3));
    }

    @Test
    void configuredStreaksReturnsSortedUnmodifiableSet() {
        KillStreakReward reward = mock(KillStreakReward.class);
        KillStreakSettings settings = new KillStreakSettings(true, true, Map.of(
                10, List.of(reward),
                1, List.of(reward),
                5, List.of(reward)
        ));

        Set<Integer> configuredStreaks = settings.configuredStreaks();

        assertEquals(Set.of(1, 5, 10), configuredStreaks);
        assertEquals(List.of(1, 5, 10), configuredStreaks.stream().toList());
        assertThrows(UnsupportedOperationException.class, () -> configuredStreaks.add(99));
    }

    @Test
    void hasRewardsTrueOnlyWhenAtLeastOneRewardListIsNonEmpty() {
        KillStreakReward reward = mock(KillStreakReward.class);

        KillStreakSettings none = new KillStreakSettings(true, true, Map.of());
        KillStreakSettings emptyOnly = new KillStreakSettings(true, true, Map.of(
                2, List.of(),
                4, List.of()
        ));
        KillStreakSettings mixed = new KillStreakSettings(true, true, Map.of(
                2, List.of(),
                4, List.of(reward)
        ));

        assertFalse(none.hasRewards());
        assertFalse(emptyOnly.hasRewards());
        assertTrue(mixed.hasRewards());
    }

    @Test
    void summaryOutputsForDisabledEnabledWithoutRewardsAndEnabledWithThresholds() {
        KillStreakReward reward = mock(KillStreakReward.class);

        KillStreakSettings disabled = KillStreakSettings.disabled();
        KillStreakSettings enabledNoRewards = new KillStreakSettings(true, true, Map.of(
                2, List.of(),
                7, List.of()
        ));
        KillStreakSettings enabledWithRewards = new KillStreakSettings(true, true, Map.of(
                10, List.of(reward),
                3, List.of(reward)
        ));

        assertEquals("Kill streaks: disabled.", disabled.summary());
        assertEquals("Kill streaks: enabled (no rewards configured).", enabledNoRewards.summary());
        assertEquals("Kill streaks: enabled (thresholds: 3, 10).", enabledWithRewards.summary());
    }
}
