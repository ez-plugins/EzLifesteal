package com.skyblockexp.ezlifesteal.config;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigLoader {

    private final JavaPlugin plugin;

    private final Logger logger;


    public ConfigLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void ensureResources(List<String> resources) {
        for (String resource : resources) {
            ensureResourceIfMissing(resource);
        }
    }

    public File resolveFile(String resource) {
        return new File(plugin.getDataFolder(), resource);
    }

    public YamlConfiguration load(String resource) {
        final File file = resolveFile(resource);
        if (!file.exists()) {
            ensureResourceIfMissing(resource);
        }
        if (!file.exists()) {
            logger.warning("Failed to load " + resource + " because the file could not be created.");
            return new YamlConfiguration();
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public void save(YamlConfiguration configuration, String resource) {
        if (configuration == null) {
            return;
        }
        final File file = resolveFile(resource);
        try {
            configuration.save(file);
        }
        catch (IOException exception) {
            logger.severe("Failed to save " + resource + ": " + exception.getMessage());
        }
    }

    private void ensureResourceIfMissing(String resource) {
        final File target = resolveFile(resource);
        if (target.exists()) {
            return;
        }
        final File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            logger.warning("Unable to create plugin data directory to save " + resource);
            return;
        }
        final File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            logger.warning("Unable to create directory " + parent + " to save " + resource);
            return;
        }
        try {
            plugin.saveResource(resource, false);
        }
        catch (IllegalArgumentException exception) {
            logger.warning("Default resource '" + resource + "' is missing from the plugin jar.");
        }
    }
}
