package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.hologram.TopHologramManager;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import org.bukkit.configuration.ConfigurationSection;

public class HologramService {
    private final DefaultPluginRuntimeServices runtime;

    private final EzLifestealPlugin plugin;

    public HologramService(DefaultPluginRuntimeServices runtime, EzLifestealPlugin plugin) {
        this.runtime = runtime;
        this.plugin = plugin;
    }

    public void setupHologram() {
        TopHologramManager manager = runtime.getRegistry().getManagerState().getTopHologramManager();
        if (manager == null) {
            manager = new TopHologramManager(plugin);
            runtime.getRegistry().getManagerState().setTopHologramManager(manager);
        }
        final ConfigurationSection section = runtime.getHologramSection(false);
        manager.reload(section);
    }

    public void requestUpdate() {
        final TopHologramManager mgr = runtime.getRegistry().getManagerState().getTopHologramManager();
        if (mgr != null) {
            mgr.requestUpdate();
        }
    }

    public void shutdown() {
        final TopHologramManager mgr = runtime.getRegistry().getManagerState().getTopHologramManager();
        if (mgr != null) {
            mgr.shutdown();
        }
    }

    public boolean hasHologram() {
        final TopHologramManager mgr = runtime.getRegistry().getManagerState().getTopHologramManager();
        return mgr != null && mgr.hasHologram();
    }

    public TopHologramManager getManager() {
        return runtime.getRegistry().getManagerState().getTopHologramManager();
    }
}
