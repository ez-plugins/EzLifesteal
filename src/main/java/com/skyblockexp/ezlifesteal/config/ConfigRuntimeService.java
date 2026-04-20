package com.skyblockexp.ezlifesteal.config;

import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;

public final class ConfigRuntimeService {
    private final DefaultPluginRuntimeServices runtime;

    public ConfigRuntimeService(DefaultPluginRuntimeServices runtime) {
        this.runtime = runtime;
    }

    public void start() {
        runtime.saveDefaultConfig();
        runtime.ensureAdditionalConfigFiles();
        runtime.reloadAdditionalConfigs();
        runtime.setupMessages();
    }

    public void reload() {
        runtime.reloadConfig();
        runtime.reloadAdditionalConfigs();
        runtime.setupMessages();
    }

    public void stop() { }
}
