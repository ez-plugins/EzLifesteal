package com.skyblockexp.ezlifesteal.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class StorageConfigAdapterTest {

    @Test
    void getTypePrefersStorageConfigType() {
        YamlConfiguration storageConfig = new YamlConfiguration();
        storageConfig.set("type", "mysql");

        YamlConfiguration pluginConfig = new YamlConfiguration();
        pluginConfig.set("storage.type", "yaml");

        StorageConfigAdapter adapter = new StorageConfigAdapter(storageConfig, pluginConfig);

        assertEquals("mysql", adapter.getType("sqlite"));
    }

    @Test
    void getTypeFallsBackToPluginStorageType() {
        YamlConfiguration storageConfig = new YamlConfiguration();
        YamlConfiguration pluginConfig = new YamlConfiguration();
        pluginConfig.set("storage.type", "yaml");

        StorageConfigAdapter adapter = new StorageConfigAdapter(storageConfig, pluginConfig);

        assertEquals("yaml", adapter.getType("sqlite"));
    }

    @Test
    void getTypeUsesDefaultWhenNeitherConfigHasType() {
        YamlConfiguration storageConfig = new YamlConfiguration();
        YamlConfiguration pluginConfig = new YamlConfiguration();

        StorageConfigAdapter adapter = new StorageConfigAdapter(storageConfig, pluginConfig);

        assertEquals("sqlite", adapter.getType("sqlite"));
    }

    @Test
    void getSectionResolvesStorageThenLegacyThenNull() {
        YamlConfiguration storageConfigWithSection = new YamlConfiguration();
        storageConfigWithSection.set("credentials.host", "db.example");

        YamlConfiguration pluginConfigWithLegacy = new YamlConfiguration();
        pluginConfigWithLegacy.set("storage.credentials.host", "legacy.example");

        StorageConfigAdapter primaryAdapter = new StorageConfigAdapter(storageConfigWithSection,
                pluginConfigWithLegacy);
        ConfigurationSection primary = primaryAdapter.getSection("credentials");
        assertNotNull(primary);
        assertEquals("db.example", primary.getString("host"));

        StorageConfigAdapter legacyAdapter = new StorageConfigAdapter(null, pluginConfigWithLegacy);
        ConfigurationSection legacy = legacyAdapter.getSection("credentials");
        assertNotNull(legacy);
        assertEquals("legacy.example", legacy.getString("host"));

        StorageConfigAdapter missingAdapter = new StorageConfigAdapter(new YamlConfiguration(),
                new YamlConfiguration());
        assertNull(missingAdapter.getSection("credentials"));
    }

    @Test
    void getLegacyStorageTypeReturnsNullWhenPathMissing() {
        StorageConfigAdapter adapter = new StorageConfigAdapter(new YamlConfiguration(), new YamlConfiguration());

        assertNull(adapter.getLegacyStorageType());
    }

    @Test
    void getLegacyStorageTypeMapsRecognizedValuesCaseInsensitively() {
        YamlConfiguration pluginConfig = new YamlConfiguration();
        StorageConfigAdapter adapter = new StorageConfigAdapter(new YamlConfiguration(), pluginConfig);

        pluginConfig.set("storage.type", "MySQL");
        assertEquals("MYSQL", adapter.getLegacyStorageType());

        pluginConfig.set("storage.type", "YamL");
        assertEquals("YAML", adapter.getLegacyStorageType());

        pluginConfig.set("storage.type", "yMl");
        assertEquals("YAML", adapter.getLegacyStorageType());
    }

    @Test
    void getLegacyStorageTypeTreatsBlankAndWhitespaceAsUnset() {
        YamlConfiguration pluginConfig = new YamlConfiguration();
        StorageConfigAdapter adapter = new StorageConfigAdapter(new YamlConfiguration(), pluginConfig);

        pluginConfig.set("storage.type", "");
        assertNull(adapter.getLegacyStorageType());

        pluginConfig.set("storage.type", "   ");
        assertNull(adapter.getLegacyStorageType());
    }

    @Test
    void getLegacyStorageTypeReturnsNullForUnexpectedValues() {
        YamlConfiguration pluginConfig = new YamlConfiguration();
        pluginConfig.set("storage.type", "postgres");
        StorageConfigAdapter adapter = new StorageConfigAdapter(new YamlConfiguration(), pluginConfig);

        assertNull(adapter.getLegacyStorageType());
    }
}
