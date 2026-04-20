package com.skyblockexp.ezlifesteal.runtime;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import org.bukkit.command.CommandSender;

public final class Bootstrap {

    private final PluginRuntimeCoordinator coordinator;

    private final Registry registry;

    public Bootstrap(EzLifestealPlugin plugin, Registry registry) {
        this.coordinator = new PluginRuntimeCoordinator(plugin, registry);
        this.registry = registry;
    }

    public void start() {
        registry.setBootstrap(this);
        coordinator.initializePlugin();
    }

    public void stop() {
        coordinator.shutdownPlugin();
        registry.setBootstrap(null);
    }

    public void reload(CommandSender initiator) {
        coordinator.reloadPlugin(initiator);
    }

    public PluginAccessor getPluginAccessor() {
        return coordinator.getPluginAccessor();
    }

    public DefaultPluginRuntimeServices getRuntimeServices() {
        return coordinator.getRuntimeServices();
    }
}
