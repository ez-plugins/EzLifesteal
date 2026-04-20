package com.skyblockexp.ezlifesteal.runtime;

import org.bukkit.command.CommandSender;

public final class PluginContext {

    private final Registry registry;

    private CommandSender lastReloadInitiator;


    public PluginContext(Registry registry) {
        this.registry = registry;
    }

    public Registry getRegistry() {
        return registry;
    }

    public CommandSender getLastReloadInitiator() {
        return lastReloadInitiator;
    }

    public void setLastReloadInitiator(CommandSender lastReloadInitiator) {
        this.lastReloadInitiator = lastReloadInitiator;
    }
}
