package com.skyblockexp.ezlifesteal.runtime;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.config.ConfigRuntimeService;
import com.skyblockexp.ezlifesteal.heart.RecipeRuntimeService;
import com.skyblockexp.ezlifesteal.integration.IntegrationRuntimeService;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.CommandSender;

public final class PluginRuntimeCoordinator {
    private static final int BSTATS_PLUGIN_ID = 27720;

    private final EzLifestealPlugin plugin;

    private final DefaultPluginRuntimeServices runtime;

    private final PluginAccessor pluginAccessor;

    private final ConfigRuntimeService configRuntimeService;

    private final StorageRuntimeService storageRuntimeService;

    private final IntegrationRuntimeService integrationRuntimeService;

    private final RecipeRuntimeService recipeRuntimeService;

    private final CommandRegistrationService commandRegistrationService;


    public PluginRuntimeCoordinator(EzLifestealPlugin plugin, Registry registry) {

        this.plugin = plugin;

        this.runtime = new DefaultPluginRuntimeServices(plugin, registry);

        this.pluginAccessor = new RuntimePluginFacade(plugin, runtime);
        this.configRuntimeService = new ConfigRuntimeService(runtime);
        this.storageRuntimeService = new StorageRuntimeService(runtime);
        this.integrationRuntimeService = new IntegrationRuntimeService(plugin, pluginAccessor, runtime, registry);
        this.recipeRuntimeService = new RecipeRuntimeService(runtime);
        this.commandRegistrationService = new CommandRegistrationService(plugin, pluginAccessor);
    }

    public PluginAccessor getPluginAccessor() {
        return pluginAccessor;
    }

    public DefaultPluginRuntimeServices getRuntimeServices() {
        return runtime;
    }

    public void initializePlugin() {
        runtime.initializeCoreState();
        configRuntimeService.start();
        storageRuntimeService.start();
        runtime.reloadManagerState();
        registerCoreListeners();
        integrationRuntimeService.start();
        runtime.startBeaconSpawnFeature(pluginAccessor);
        recipeRuntimeService.start();
        commandRegistrationService.start();
        runtime.setupPlaceholderExpansion(pluginAccessor);
        setupMetrics();
        runtime.logStartupSummary();
    }

    public void shutdownPlugin() {
        recipeRuntimeService.stop();
        integrationRuntimeService.stop();
        runtime.stopBeaconSpawnFeature();
        runtime.shutdownManagers();
        storageRuntimeService.stop();
    }

    public void reloadPlugin(CommandSender initiator) {
        runtime.getPluginContext().setLastReloadInitiator(initiator);
        configRuntimeService.reload();
        storageRuntimeService.reload();
        runtime.reloadManagerState();
        integrationRuntimeService.reload();
        runtime.reloadBeaconSpawnFeature(pluginAccessor);
        recipeRuntimeService.reload();
        commandRegistrationService.reload();
        runtime.setupPlaceholderExpansion(pluginAccessor);
        runtime.logStartupSummary();
        if (initiator != null) {
            runtime.getMessageService().sendMessage(initiator, "reloaded");
            final String overrideSummary = runtime.getWorldOverrideSummary();
            if (!overrideSummary.isEmpty()) {
                initiator.sendMessage(runtime.getMessageService().getPrefix() + overrideSummary);
            }
        }
    }

    private void registerCoreListeners() {
        runtime.setupCoreListeners(pluginAccessor);
    }

    private void setupMetrics() {
        try {
            new Metrics(plugin, BSTATS_PLUGIN_ID);
        }
        catch (Throwable t) {
            plugin.getLogger().warning("Failed to initialise bStats metrics: " + t.getMessage());
        }
    }
}
