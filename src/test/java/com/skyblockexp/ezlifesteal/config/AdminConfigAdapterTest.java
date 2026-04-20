package com.skyblockexp.ezlifesteal.config;

import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

class AdminConfigAdapterTest {

    @Test
    void readsValueFromAdminConfigWhenKeyExists() {
        YamlConfiguration adminConfig = new YamlConfiguration();
        adminConfig.set("notify.enabled", true);

        YamlConfiguration pluginConfig = new YamlConfiguration();
        pluginConfig.set("admin-detection.notify.enabled", false);

        AdminConfigAdapter adapter = new AdminConfigAdapter(adminConfig, pluginConfig);

        assertEquals(true, adapter.getBoolean("notify.enabled", false));
    }

    @Test
    void fallsBackToLegacyPluginConfigWhenAdminKeyMissing() {
        YamlConfiguration adminConfig = new YamlConfiguration();
        YamlConfiguration pluginConfig = new YamlConfiguration();
        pluginConfig.set("admin-detection.alert-message", "legacy-value");

        AdminConfigAdapter adapter = new AdminConfigAdapter(adminConfig, pluginConfig);

        assertEquals("legacy-value", adapter.getString("alert-message", "default-value"));
    }

    @Test
    void usesProvidedDefaultsWhenBothConfigsMissingKey() {
        YamlConfiguration adminConfig = new YamlConfiguration();
        YamlConfiguration pluginConfig = new YamlConfiguration();

        AdminConfigAdapter adapter = new AdminConfigAdapter(adminConfig, pluginConfig);

        assertEquals(false, adapter.getBoolean("missing.enabled", false));
        assertEquals("fallback", adapter.getString("missing.value", "fallback"));
    }

    @Test
    void getStringListReturnsEmptyListWhenAbsentInBothConfigs() {
        YamlConfiguration adminConfig = new YamlConfiguration();
        YamlConfiguration pluginConfig = new YamlConfiguration();

        AdminConfigAdapter adapter = new AdminConfigAdapter(adminConfig, pluginConfig);

        assertIterableEquals(List.of(), adapter.getStringList("missing-list"));
    }
}
