package com.skyblockexp.ezlifesteal.config;

import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class SmurfConfigAdapter {
    private final FileConfiguration smurfConfig;

    private final FileConfiguration pluginConfig;

    public SmurfConfigAdapter(FileConfiguration smurfConfig, FileConfiguration pluginConfig) {
        this.smurfConfig = smurfConfig;
        this.pluginConfig = pluginConfig;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        if (smurfConfig != null && smurfConfig.contains(key)) {
            return smurfConfig.getBoolean(key);
        }
        final ConfigurationSection legacy = pluginConfig.getConfigurationSection("smurf-detection");
        if (legacy != null && legacy.contains(key)) {
            return legacy.getBoolean(key);
        }
        return defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        if (smurfConfig != null && smurfConfig.contains(key)) {
            return smurfConfig.getInt(key);
        }
        final ConfigurationSection legacy = pluginConfig.getConfigurationSection("smurf-detection");
        if (legacy != null && legacy.contains(key)) {
            return legacy.getInt(key);
        }
        return defaultValue;
    }

    public long getLong(String key, long defaultValue) {
        if (smurfConfig != null && smurfConfig.contains(key)) {
            return smurfConfig.getLong(key);
        }
        final ConfigurationSection legacy = pluginConfig.getConfigurationSection("smurf-detection");
        if (legacy != null && legacy.contains(key)) {
            return legacy.getLong(key);
        }
        return defaultValue;
    }

    public String getString(String key, String defaultValue) {
        if (smurfConfig != null && smurfConfig.contains(key)) {
            return smurfConfig.getString(key, defaultValue);
        }
        final ConfigurationSection legacy = pluginConfig.getConfigurationSection("smurf-detection");
        if (legacy != null && legacy.contains(key)) {
            return legacy.getString(key, defaultValue);
        }
        return defaultValue;
    }

    public List<String> getStringList(String key) {
        if (smurfConfig != null && smurfConfig.contains(key)) {
            return smurfConfig.getStringList(key);
        }
        final ConfigurationSection legacy = pluginConfig.getConfigurationSection("smurf-detection");
        if (legacy != null && legacy.contains(key)) {
            return legacy.getStringList(key);
        }
        return List.of();
    }
}
