package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class KillerRewardService {
    private final PluginAccessor plugin;

    public KillerRewardService(PluginAccessor plugin) {
        this.plugin = plugin;
    }

    public void applyKillerReward(Player victim,
                                  Player killer,
                                  LifestealProfile killerProfile,
                                  LifestealManager manager,
                                  KillerOutcome killerOutcome) {
        if (killerOutcome.rewardMode() == KillerRewardMode.HEART_ITEM) {
            giveHeartItemOrFallback(victim, killer, killerProfile, manager, killerOutcome.numericHeartGain());
            return;
        }
        applyNumericHeartGain(killer, killerProfile, manager, killerOutcome.numericHeartGain());
    }

    private void giveHeartItemOrFallback(Player victim,
                                         Player killer,
                                         LifestealProfile killerProfile,
                                         LifestealManager manager,
                                         double fallbackGain) {
        final var registry = plugin.getHeartRegistry();
        if (registry == null) {
            applyNumericHeartGain(killer, killerProfile, manager, fallbackGain);
            return;
        }

        com.skyblockexp.ezlifesteal.heart.Heart heart = registry.getById(plugin.getDropHeartId());
        if (heart == null) {
            final int tierGuess = (int) Math.max(1, Math.round(fallbackGain));
            heart = registry.getByTier(tierGuess);
        }
        if (heart == null) {
            applyNumericHeartGain(killer, killerProfile, manager, fallbackGain);
            return;
        }

        final com.skyblockexp.ezlifesteal.heart.Heart selected = heart;
        final int giveAmount = Math.max(1, plugin.getDropHeartAmount());
        SchedulerAdapter.run(plugin.getPlugin(), () -> {
            final ItemStack stack = selected.createItemStack();
            for (int i = 0; i < giveAmount; i++) {
                final ItemStack toGive = stack.clone();
                final Map<Integer, ItemStack> leftover = killer.getInventory().addItem(toGive);
                if (!leftover.isEmpty()) {
                    killer.getWorld().dropItemNaturally(killer.getLocation(), toGive);
                }
            }
        });
    }

    private void applyNumericHeartGain(Player killer,
                                       LifestealProfile killerProfile,
                                       LifestealManager manager,
                                       double heartGain) {
        killerProfile.addHearts(heartGain, manager.getMaxHearts());
        manager.saveProfileAsync(killerProfile).whenComplete((unused, throwable) -> {
            if (throwable != null) {
                plugin.getLogger().severe("Failed to save killer profile for "
                        + killer.getName() + ": " + throwable.getMessage());
            }
        });
        SchedulerAdapter.run(plugin.getPlugin(), () -> {
            manager.applyHearts(killer, killerProfile);
            plugin.sendHeartStatus(killer, killerProfile.getHearts());
        });
    }
}
