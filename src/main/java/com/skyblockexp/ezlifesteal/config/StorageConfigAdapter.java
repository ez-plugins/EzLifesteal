package com.skyblockexp.ezlifesteal.config;

import java.util.Locale;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class StorageConfigAdapter {
    private final YamlConfiguration storageConfig;

    private final YamlConfiguration pluginConfig;


    public StorageConfigAdapter(YamlConfiguration storageConfig, YamlConfiguration pluginConfig) {
        this.storageConfig = storageConfig;
        this.pluginConfig = pluginConfig;
    }

    public String getType(String defaultValue) {
        if (storageConfig != null && storageConfig.contains("type")) {
            return storageConfig.getString("type");
        }
        if (pluginConfig != null && pluginConfig.contains("storage.type")) {
            return pluginConfig.getString("storage.type");
        }
        return defaultValue;
    }

    public ConfigurationSection getSection(String path) {
        if (storageConfig != null) {
            return storageConfig.getConfigurationSection(path);
        }
        if (pluginConfig != null) {
            final ConfigurationSection legacy = pluginConfig.getConfigurationSection("storage");
            if (legacy != null) {
                return legacy.getConfigurationSection(path);
            }
        }
        return null;
    }

    public String getLegacyStorageType() {
        if (pluginConfig == null || !pluginConfig.contains("storage.type")) {
            return null;
        }

        final String value = pluginConfig.getString("storage.type");
        if (value == null || value.isBlank()) {
            return null;
        }

        switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "mysql":
                return "MYSQL";
            case "yaml":
            case "yml":
                return "YAML";
            default:
                return null;
        }
    }
}
