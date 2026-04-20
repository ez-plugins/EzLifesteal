package com.skyblockexp.ezlifesteal.config;

import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public final class LegacyConfigResolver {

    private final FileConfiguration rootConfig;

    public LegacyConfigResolver(FileConfiguration rootConfig) {
        this.rootConfig = rootConfig;
    }

    public boolean getBoolean(YamlConfiguration primaryConfig, String legacySectionPath, String key,
            boolean defaultValue) {
        if (hasPrimaryValue(primaryConfig, key)) {
            return primaryConfig.getBoolean(key, defaultValue);
        }
        final ConfigurationSection legacySection = getLegacySection(legacySectionPath);
        if (legacySection != null && legacySection.contains(key)) {
            return legacySection.getBoolean(key, defaultValue);
        }
        return defaultValue;
    }

    public int getInt(YamlConfiguration primaryConfig, String legacySectionPath, String key, int defaultValue) {
        if (hasPrimaryValue(primaryConfig, key)) {
            return primaryConfig.getInt(key, defaultValue);
        }
        final ConfigurationSection legacySection = getLegacySection(legacySectionPath);
        if (legacySection != null && legacySection.contains(key)) {
            return legacySection.getInt(key, defaultValue);
        }
        return defaultValue;
    }

    public long getLong(YamlConfiguration primaryConfig, String legacySectionPath, String key, long defaultValue) {
        if (hasPrimaryValue(primaryConfig, key)) {
            return primaryConfig.getLong(key, defaultValue);
        }
        final ConfigurationSection legacySection = getLegacySection(legacySectionPath);
        if (legacySection != null && legacySection.contains(key)) {
            return legacySection.getLong(key, defaultValue);
        }
        return defaultValue;
    }

    public String getString(YamlConfiguration primaryConfig, String legacySectionPath, String key,
            String defaultValue) {
        String value = getStringFromSection(primaryConfig, key);
        if (value != null) {
            return value;
        }
        final ConfigurationSection legacySection = getLegacySection(legacySectionPath);
        value = getStringFromSection(legacySection, key);
        return value == null ? defaultValue : value;
    }

    public List<String> getStringList(YamlConfiguration primaryConfig, String legacySectionPath, String key) {
        if (hasPrimaryValue(primaryConfig, key)) {
            return primaryConfig.getStringList(key);
        }
        final ConfigurationSection legacySection = getLegacySection(legacySectionPath);
        if (legacySection != null && legacySection.contains(key)) {
            return legacySection.getStringList(key);
        }
        return List.of();
    }

    private boolean hasPrimaryValue(YamlConfiguration configuration, String key) {
        return configuration != null && configuration.contains(key);
    }

    private ConfigurationSection getLegacySection(String path) {
        return rootConfig.getConfigurationSection(path);
    }

    private String getStringFromSection(ConfigurationSection section, String key) {
        if (section == null) {
            return null;
        }
        return section.getString(key);
    }
}
