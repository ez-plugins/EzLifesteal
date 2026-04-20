package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.hologram.TopHologramManager;
import com.skyblockexp.ezlifesteal.killstreak.KillStreakManager;
import com.skyblockexp.ezlifesteal.killstreak.KillStreakSettings;
import com.skyblockexp.ezlifesteal.overlay.HeartOverlayManager;
import com.skyblockexp.ezlifesteal.runtime.Registry;
import com.skyblockexp.ezlifesteal.storage.repository.ProfileRepository;
import com.skyblockexp.ezlifesteal.util.BukkitHealthAttributeResolver;

/**
 * Creates and wires manager instances into runtime manager state.
 */
public class ManagerStateInitializer {

    private final EzLifestealPlugin plugin;

    public ManagerStateInitializer(EzLifestealPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize(
            Registry.ManagerState managerState,
            ProfileRepository profileRepository,
            LifestealSettingsFactory.LifestealSettings lifestealSettings,
            KillStreakSettings killStreakSettings,
            KillStreakManager existingKillStreakManager
    ) {
        final LifestealManager lifestealManager = new LifestealManager(
                plugin,
                profileRepository,
                new BukkitHealthAttributeResolver(),
                lifestealSettings.defaultHearts(),
                lifestealSettings.minHearts(),
                lifestealSettings.maxHearts(),
                lifestealSettings.applyHealthScale(),
                lifestealSettings.healthScale()
        );

        KillStreakManager killStreakManager = existingKillStreakManager;
        if (killStreakManager == null) {
            killStreakManager = new KillStreakManager(plugin);
        }
        killStreakManager.applySettings(killStreakSettings);

        managerState.setLifestealManager(lifestealManager);
        managerState.setKillStreakManager(killStreakManager);
        managerState.setHeartOverlayManager(new HeartOverlayManager(plugin));
        managerState.setTopHologramManager(new TopHologramManager(plugin));
    }
}
