package com.skyblockexp.ezlifesteal.killstreak;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class KillStreakSettings {

    private static final KillStreakSettings DISABLED = new KillStreakSettings(false, true, Map.of());

    private final boolean enabled;

    private final boolean resetOnDeath;

    private final Map<Integer, List<KillStreakReward>> rewards;

    public KillStreakSettings(boolean enabled,
                              boolean resetOnDeath,
                              Map<Integer, List<KillStreakReward>> rewards) {
        this.enabled = enabled;
        this.resetOnDeath = resetOnDeath;
        if (rewards == null || rewards.isEmpty()) {
            this.rewards = Map.of();
        }
        else {
            this.rewards = Collections.unmodifiableMap(rewards);
        }
    }

    public static KillStreakSettings disabled() {
        return DISABLED;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean resetOnDeath() {
        return resetOnDeath;
    }

    public List<KillStreakReward> rewardsFor(int streak) {
        final List<KillStreakReward> rewardsForStreak = rewards.get(streak);
        if (rewardsForStreak == null || rewardsForStreak.isEmpty()) {
            return List.of();
        }
        return rewardsForStreak;
    }

    public boolean hasRewards() {
        return rewards.values().stream().anyMatch(list -> list != null && !list.isEmpty());
    }

    public Set<Integer> configuredStreaks() {
        if (rewards.isEmpty()) {
            return Set.of();
        }
        return Collections.unmodifiableSet(new TreeSet<>(rewards.keySet()));
    }

    public String summary() {
        if (!enabled) {
            return "Kill streaks: disabled.";
        }
        if (!hasRewards()) {
            return "Kill streaks: enabled (no rewards configured).";
        }
        return "Kill streaks: enabled (thresholds: " + configuredStreaks().stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", ")) + ").";
    }
}
