package com.skyblockexp.ezlifesteal.config;

import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class LifestealConfigAdapterTest {

    @Test
    void primaryConfigValueOverridesAdditionalAndPluginLegacy() {
        YamlConfiguration primaryConfig = new YamlConfiguration();
        primaryConfig.set("hearts.start", 20);

        YamlConfiguration additionalConfig = new YamlConfiguration();
        additionalConfig.set("hearts.start", 15);

        YamlConfiguration pluginConfig = new YamlConfiguration();
        pluginConfig.set("lifesteal.hearts.start", 10);

        LifestealConfigAdapter adapter = new LifestealConfigAdapter(
                primaryConfig,
                List.of(additionalConfig),
                pluginConfig
        );

        assertEquals(20, adapter.getInt("hearts.start", 5));
    }

    @Test
    void additionalConfigUsedWhenPrimaryMissingPath() {
        YamlConfiguration primaryConfig = new YamlConfiguration();

        YamlConfiguration additionalConfig = new YamlConfiguration();
        additionalConfig.set("messages.death", "from-additional");

        YamlConfiguration pluginConfig = new YamlConfiguration();
        pluginConfig.set("lifesteal.messages.death", "from-legacy");

        LifestealConfigAdapter adapter = new LifestealConfigAdapter(
                primaryConfig,
                List.of(additionalConfig),
                pluginConfig
        );

        assertEquals("from-additional", adapter.getString("messages.death", "default"));
    }

    @Test
    void legacyPluginConfigUsedWhenLifestealConfigsMissing() {
        YamlConfiguration pluginConfig = new YamlConfiguration();
        pluginConfig.set("lifesteal.combat.enabled", true);

        LifestealConfigAdapter adapter = new LifestealConfigAdapter(
                new YamlConfiguration(),
                List.of(),
                pluginConfig
        );

        assertEquals(true, adapter.getBoolean("combat.enabled", false));
    }

    @Test
    void defaultValuesReturnedWhenAllSourcesMissing() {
        LifestealConfigAdapter adapter = new LifestealConfigAdapter(
                new YamlConfiguration(),
                List.of(),
                new YamlConfiguration()
        );

        assertEquals(3.5D, adapter.getDouble("missing.double", 3.5D));
        assertEquals(12, adapter.getInt("missing.int", 12));
        assertEquals(false, adapter.getBoolean("missing.boolean", false));
        assertEquals("fallback", adapter.getString("missing.string", "fallback"));
    }

    @Test
    void getSectionReturnsFirstNonNullSectionInSourceOrder() {
        YamlConfiguration primaryConfig = new YamlConfiguration();
        primaryConfig.set("rewards.win.amount", 5);

        YamlConfiguration additionalConfig = new YamlConfiguration();
        additionalConfig.set("rewards.win.amount", 7);

        YamlConfiguration pluginConfig = new YamlConfiguration();
        pluginConfig.set("lifesteal.rewards.win.amount", 9);

        LifestealConfigAdapter adapter = new LifestealConfigAdapter(
                primaryConfig,
                List.of(additionalConfig),
                pluginConfig
        );

        ConfigurationSection section = adapter.getSection("rewards.win");
        assertNotNull(section);
        assertEquals(5, section.getInt("amount"));

        LifestealConfigAdapter additionalFirstAdapter = new LifestealConfigAdapter(
                new YamlConfiguration(),
                List.of(additionalConfig),
                pluginConfig
        );
        ConfigurationSection additionalSection = additionalFirstAdapter.getSection("rewards.win");
        assertNotNull(additionalSection);
        assertEquals(7, additionalSection.getInt("amount"));

        LifestealConfigAdapter legacyOnlyAdapter = new LifestealConfigAdapter(
                new YamlConfiguration(),
                List.of(),
                pluginConfig
        );
        ConfigurationSection legacySection = legacyOnlyAdapter.getSection("rewards.win");
        assertNotNull(legacySection);
        assertEquals(9, legacySection.getInt("amount"));

        LifestealConfigAdapter missingAdapter = new LifestealConfigAdapter(
                new YamlConfiguration(),
                List.of(),
                new YamlConfiguration()
        );
        assertNull(missingAdapter.getSection("rewards.win"));
    }
}
