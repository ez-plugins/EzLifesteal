package com.skyblockexp.ezlifesteal.config;

import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

class SmurfConfigAdapterTest {

    @Test
    void dedicatedSmurfConfigKeyWinsOverLegacySmurfDetection() {
        YamlConfiguration smurfConfig = new YamlConfiguration();
        smurfConfig.set("alerts.enabled", true);

        YamlConfiguration pluginConfig = new YamlConfiguration();
        pluginConfig.set("smurf-detection.alerts.enabled", false);

        SmurfConfigAdapter adapter = new SmurfConfigAdapter(smurfConfig, pluginConfig);

        assertEquals(true, adapter.getBoolean("alerts.enabled", false));
    }

    @Test
    void legacyKeyUsedWhenDedicatedConfigMissing() {
        YamlConfiguration smurfConfig = new YamlConfiguration();

        YamlConfiguration pluginConfig = new YamlConfiguration();
        pluginConfig.set("smurf-detection.window-seconds", 90);

        SmurfConfigAdapter adapter = new SmurfConfigAdapter(smurfConfig, pluginConfig);

        assertEquals(90, adapter.getInt("window-seconds", 30));
    }

    @Test
    void defaultsUsedWhenNeitherSourceContainsKey() {
        SmurfConfigAdapter adapter = new SmurfConfigAdapter(new YamlConfiguration(), new YamlConfiguration());

        assertEquals(false, adapter.getBoolean("missing.enabled", false));
        assertEquals(120L, adapter.getLong("missing.long", 120L));
        assertEquals("fallback", adapter.getString("missing.string", "fallback"));
    }

    @Test
    void getStringListReturnsEmptyListForFullyMissingPath() {
        SmurfConfigAdapter adapter = new SmurfConfigAdapter(new YamlConfiguration(), new YamlConfiguration());

        assertIterableEquals(List.of(), adapter.getStringList("missing.list"));
    }
}
