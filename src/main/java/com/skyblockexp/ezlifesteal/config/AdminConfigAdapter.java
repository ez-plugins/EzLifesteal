package com.skyblockexp.ezlifesteal.config;

import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class AdminConfigAdapter {
    private final FileConfiguration adminConfig;

    private final FileConfiguration pluginConfig;

    public AdminConfigAdapter(FileConfiguration adminConfig, FileConfiguration pluginConfig) {
        this.adminConfig = adminConfig;
        this.pluginConfig = pluginConfig;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        if (adminConfig != null && adminConfig.contains(key)) {
            return adminConfig.getBoolean(key);
        }
        final ConfigurationSection legacy = pluginConfig.getConfigurationSection("admin-detection");
        if (legacy != null && legacy.contains(key)) {
            return legacy.getBoolean(key);
        }
        return defaultValue;
    }

    public String getString(String key, String defaultValue) {
        if (adminConfig != null && adminConfig.contains(key)) {
            return adminConfig.getString(key, defaultValue);
        }
        final ConfigurationSection legacy = pluginConfig.getConfigurationSection("admin-detection");
        if (legacy != null && legacy.contains(key)) {
            return legacy.getString(key, defaultValue);
        }
        return defaultValue;
    }

    public List<String> getStringList(String key) {
        if (adminConfig != null && adminConfig.contains(key)) {
            return adminConfig.getStringList(key);
        }
        final ConfigurationSection legacy = pluginConfig.getConfigurationSection("admin-detection");
        if (legacy != null && legacy.contains(key)) {
            return legacy.getStringList(key);
        }
        return List.of();
    }
}
