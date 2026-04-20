package com.skyblockexp.ezlifesteal.killstreak;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public class KillStreakManager {

    private final EzLifestealPlugin plugin;

    private final Map<UUID, Integer> streaks = new ConcurrentHashMap<>();

    private volatile KillStreakSettings settings = KillStreakSettings.disabled();


    public KillStreakManager(EzLifestealPlugin plugin) {
        this.plugin = plugin;
    }

    public void applySettings(KillStreakSettings newSettings) {
        if (newSettings == null) {
            settings = KillStreakSettings.disabled();
            streaks.clear();
            return;
        }
        settings = newSettings;
        if (!settings.enabled()) {
            streaks.clear();
        }
    }

    public void handleKill(Player player) {
        if (player == null || !settings.enabled()) {
            return;
        }
        final UUID uniqueId = player.getUniqueId();
        final int newStreak = streaks.merge(uniqueId, 1, Integer::sum);
        final List<KillStreakReward> rewards = settings.rewardsFor(newStreak);
        if (rewards.isEmpty()) {
            return;
        }
        for (KillStreakReward reward : rewards) {
            reward.apply(plugin, player);
        }
    }

    public void handleDeath(Player player) {
        if (player == null || !settings.enabled()) {
            return;
        }
        final UUID uniqueId = player.getUniqueId();
        if (settings.resetOnDeath()) {
            streaks.remove(uniqueId);
        }
    }

    public void handleQuit(Player player) {
        if (player == null) {
            return;
        }
        streaks.remove(player.getUniqueId());
    }

    public int getCurrentStreak(UUID uniqueId) {
        if (uniqueId == null) {
            return 0;
        }
        return streaks.getOrDefault(uniqueId, 0);
    }

    public boolean isEnabled() {
        return settings.enabled();
    }

    public KillStreakSettings getSettings() {
        return settings;
    }

    public void clear() {
        streaks.clear();
    }
}
