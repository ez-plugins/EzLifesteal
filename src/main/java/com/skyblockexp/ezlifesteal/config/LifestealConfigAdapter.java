package com.skyblockexp.ezlifesteal.config;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class LifestealConfigAdapter {
    private final List<FileConfiguration> lifestealConfigs;

    private final FileConfiguration pluginConfig;


    public LifestealConfigAdapter(FileConfiguration lifestealConfig, FileConfiguration pluginConfig) {
        this(lifestealConfig, List.of(), pluginConfig);
    }

    public LifestealConfigAdapter(FileConfiguration primaryConfig, List<FileConfiguration> additionalConfigs,
            FileConfiguration pluginConfig) {
        this.lifestealConfigs = new ArrayList<>();
        if (primaryConfig != null) {
            this.lifestealConfigs.add(primaryConfig);
        }
        if (additionalConfigs != null) {
            for (FileConfiguration configuration : additionalConfigs) {
                if (configuration != null) {
                    this.lifestealConfigs.add(configuration);
                }
            }
        }
        this.pluginConfig = pluginConfig;
    }

    public double getDouble(String path, double defaultValue) {
        for (FileConfiguration config : lifestealConfigs) {
            if (config.contains(path)) {
                return config.getDouble(path);
            }
        }
        if (pluginConfig != null && pluginConfig.contains("lifesteal." + path)) {
            return pluginConfig.getDouble("lifesteal." + path);
        }
        return defaultValue;
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        for (FileConfiguration config : lifestealConfigs) {
            if (config.contains(path)) {
                return config.getBoolean(path);
            }
        }
        if (pluginConfig != null && pluginConfig.contains("lifesteal." + path)) {
            return pluginConfig.getBoolean("lifesteal." + path);
        }
        return defaultValue;
    }

    public String getString(String path, String defaultValue) {
        for (FileConfiguration config : lifestealConfigs) {
            if (config.contains(path)) {
                return config.getString(path);
            }
        }
        if (pluginConfig != null && pluginConfig.contains("lifesteal." + path)) {
            return pluginConfig.getString("lifesteal." + path);
        }
        return defaultValue;
    }

    public List<String> getStringList(String path) {
        for (FileConfiguration config : lifestealConfigs) {
            if (config.contains(path)) {
                return config.getStringList(path);
            }
        }
        if (pluginConfig != null) {
            return pluginConfig.getStringList("lifesteal." + path);
        }
        return List.of();
    }

    public int getInt(String path, int defaultValue) {
        for (FileConfiguration config : lifestealConfigs) {
            if (config.contains(path)) {
                return config.getInt(path);
            }
        }
        if (pluginConfig != null && pluginConfig.contains("lifesteal." + path)) {
            return pluginConfig.getInt("lifesteal." + path);
        }
        return defaultValue;
    }

    public ConfigurationSection getSection(String path) {
        ConfigurationSection section = null;
        for (FileConfiguration config : lifestealConfigs) {
            section = config.getConfigurationSection(path);
            if (section != null) {
                return section;
            }
        }
        if (pluginConfig != null) {
            final ConfigurationSection legacy = pluginConfig.getConfigurationSection("lifesteal");
            if (legacy != null) {
                section = legacy.getConfigurationSection(path);
            }
        }
        return section;
    }
}
