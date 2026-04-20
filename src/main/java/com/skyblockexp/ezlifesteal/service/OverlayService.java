package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.overlay.HeartOverlayManager;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class OverlayService {
    private final DefaultPluginRuntimeServices runtime;

    private final EzLifestealPlugin plugin;


    public OverlayService(DefaultPluginRuntimeServices runtime, EzLifestealPlugin plugin) {
        this.runtime = runtime;
        this.plugin = plugin;
    }

    public void setupOverlay() {
        HeartOverlayManager manager = runtime.getRegistry().getManagerState().getHeartOverlayManager();
        if (manager == null) {
            manager = new HeartOverlayManager(plugin);
            runtime.getRegistry().getManagerState().setHeartOverlayManager(manager);
        }
        final ConfigurationSection section = runtime.getActionBarSection();
        final double maxHearts =
                runtime.getLifestealManager() != null ? runtime.getLifestealManager().getMaxHearts() : 20.0;
        manager.reload(section, maxHearts);
        final HeartOverlayManager finalManager = manager;
        if (finalManager.isEnabled() && runtime.getLifestealManager() != null) {
            for (Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
                runtime.getLifestealManager().getLoadedProfile(online.getUniqueId()).ifPresent(profile -> {
                    if (runtime.isGlobalLifestealEnabled()
                            && runtime.isLifestealEnabledInWorld(online.getWorld().getName())) {
                        finalManager.sendHeartStatus(online, profile.getHearts());
                    }
                    else {
                        finalManager.clear(online.getUniqueId());
                    }
                });
            }
        }
    }

    public void sendHeartStatus(org.bukkit.entity.Player player, double hearts) {
        final HeartOverlayManager manager = runtime.getRegistry().getManagerState().getHeartOverlayManager();
        if (manager == null) {
            return;
        }
        if (!runtime.isGlobalLifestealEnabled()) {
            manager.clear(player.getUniqueId());
            return;
        }
        manager.sendHeartStatus(player, hearts);
    }

    public void clear(UUID uniqueId) {
        final HeartOverlayManager manager = runtime.getRegistry().getManagerState().getHeartOverlayManager();
        if (manager != null) {
            manager.clear(uniqueId);
        }
    }

    public void shutdown() {
        final HeartOverlayManager manager = runtime.getRegistry().getManagerState().getHeartOverlayManager();
        if (manager != null) {
            manager.shutdown();
        }
    }

    public boolean isEnabled() {
        final HeartOverlayManager manager = runtime.getRegistry().getManagerState().getHeartOverlayManager();
        return manager != null && manager.isEnabled();
    }

    public HeartOverlayManager getManager() {
        return runtime.getRegistry().getManagerState().getHeartOverlayManager();
    }
}
