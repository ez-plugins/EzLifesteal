package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.config.ConfigLoader;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ConfigService {

    private final EzLifestealPlugin plugin;

    private final ConfigLoader loader;

    public ConfigService(EzLifestealPlugin plugin, ConfigLoader loader) {
        this.plugin = plugin;
        this.loader = loader;
    }

    public YamlConfiguration loadLanguageFile(String language) {
        final String resourcePath = "languages/" + language + ".yml";
        final java.io.File target = new java.io.File(plugin.getDataFolder(), resourcePath);
        if (!target.exists()) {
            loader.ensureResources(java.util.List.of(resourcePath));
        }
        if (!target.exists()) {
            return null;
        }
        return YamlConfiguration.loadConfiguration(target);
    }

    public String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "en";
        }
        String normalized = language.trim().toLowerCase(java.util.Locale.ROOT).replace('-', '_');
        final int separatorIndex = normalized.indexOf('_');
        if (separatorIndex > 0) {
            normalized = normalized.substring(0, separatorIndex);
        }
        return normalized;
    }

    public void ensureAdditionalConfigFiles() {
        loader.ensureResources(List.of(
                "admin.yml",
                "smurf.yml",
                "storage.yml",
                "lifesteal-core.yml",
                "lifesteal-drops.yml",
                "lifesteal-worlds.yml",
                "lifesteal-mobs.yml",
                "lifesteal-killstreaks.yml",
                "hearts.yml",
                "shop.yml",
                "features.yml",
                "revive-beacon.yml",
                "revive-beacon-whitelist.yml"
        ));
        loader.ensureResources(List.of(
                "languages/en.yml",
                "languages/nl.yml",
                "languages/es.yml",
                "languages/fr.yml",
                "languages/de.yml",
                "languages/pt.yml"
        ));
    }
}
