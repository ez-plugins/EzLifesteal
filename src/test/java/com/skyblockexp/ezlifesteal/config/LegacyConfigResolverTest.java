package com.skyblockexp.ezlifesteal.config;

import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyConfigResolverTest {

    @Test
    void prefersPrimaryValuesOverLegacySection() {
        YamlConfiguration root = new YamlConfiguration();
        root.set("legacy.enabled", false);
        root.set("legacy.count", 4);
        root.set("legacy.timeout", 15L);
        root.set("legacy.name", "legacy-value");
        root.set("legacy.worlds", List.of("old_world"));

        YamlConfiguration primary = new YamlConfiguration();
        primary.set("enabled", true);
        primary.set("count", 7);
        primary.set("timeout", 25L);
        primary.set("name", "primary-value");
        primary.set("worlds", List.of("world", "world_nether"));

        LegacyConfigResolver resolver = new LegacyConfigResolver(root);

        assertTrue(resolver.getBoolean(primary, "legacy", "enabled", false));
        assertEquals(7, resolver.getInt(primary, "legacy", "count", 0));
        assertEquals(25L, resolver.getLong(primary, "legacy", "timeout", 0));
        assertEquals("primary-value", resolver.getString(primary, "legacy", "name", "fallback"));
        assertIterableEquals(List.of("world", "world_nether"), resolver.getStringList(primary, "legacy", "worlds"));
    }

    @Test
    void fallsBackToLegacyAndDefaultsWhenPrimaryMissing() {
        YamlConfiguration root = new YamlConfiguration();
        root.set("legacy.enabled", true);
        root.set("legacy.count", 9);
        root.set("legacy.timeout", 60L);
        root.set("legacy.name", "legacy-name");
        root.set("legacy.worlds", List.of("spawn", "arena"));

        LegacyConfigResolver resolver = new LegacyConfigResolver(root);

        assertTrue(resolver.getBoolean(null, "legacy", "enabled", false));
        assertEquals(9, resolver.getInt(null, "legacy", "count", 3));
        assertEquals(60L, resolver.getLong(null, "legacy", "timeout", 10L));
        assertEquals("legacy-name", resolver.getString(null, "legacy", "name", "fallback"));
        assertIterableEquals(List.of("spawn", "arena"), resolver.getStringList(null, "legacy", "worlds"));

        assertFalse(resolver.getBoolean(null, "missing", "enabled", false));
        assertEquals(3, resolver.getInt(null, "missing", "count", 3));
        assertEquals(10L, resolver.getLong(null, "missing", "timeout", 10L));
        assertEquals("fallback", resolver.getString(null, "missing", "name", "fallback"));
        assertIterableEquals(List.of(), resolver.getStringList(null, "missing", "worlds"));
    }
}
