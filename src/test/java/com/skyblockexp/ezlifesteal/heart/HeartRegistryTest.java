package com.skyblockexp.ezlifesteal.heart;

import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeartRegistryTest {

    @Test
    void supportsFindSemanticsForConfiguredIdsAndTreatsMissingAsRemoved() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("hearts.red.display", "&cRed Heart");
        config.set("hearts.red.tier", 1);
        config.set("hearts.red.hearts", 1.0);
        config.set("hearts.blue.display", "&9Blue Heart");
        config.set("hearts.blue.tier", 2);
        config.set("hearts.blue.hearts", 2.0);

        HeartRegistry registry = new HeartRegistry(config);

        assertNotNull(registry.getById("red"));
        assertNotNull(registry.getById("RED"));
        assertNotNull(registry.getById("blue"));
        assertEquals(2, registry.getAll().size());

        // No explicit remove API exists; unknown IDs are effectively absent/removed.
        assertNull(registry.getById("green"));
    }

    @Test
    void duplicateRegistrationKeyUsesLastConfiguredValue() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("hearts.red.display", "&cFirst");
        config.set("hearts.red.tier", 1);
        config.set("hearts.red.hearts", 1.0);

        // Re-setting the same key path mimics duplicate registration by replacing prior values.
        config.set("hearts.red.display", "&4Second");
        config.set("hearts.red.tier", 3);
        config.set("hearts.red.hearts", 3.5);
        config.set("hearts.red.material", "diamond");

        HeartRegistry registry = new HeartRegistry(config);

        Heart red = registry.getById("red");
        assertNotNull(red);
        assertEquals("&4Second", red.getDisplayName());
        assertEquals(3, red.getTier());
        assertEquals(3.5, red.getHearts());
        assertSame(Material.DIAMOND, red.getMaterial());
    }

    @Test
    void returnsNullForNullOrBlankIdLookups() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("hearts.red.display", "&cRed");
        HeartRegistry registry = new HeartRegistry(config);

        assertNull(registry.getById(null));
        assertNull(registry.getById(""));
        assertNull(registry.getById("   "));
    }

    @Test
    void exposesImmutableViewForAllRegistryEntries() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("hearts.red.display", "&cRed");
        HeartRegistry registry = new HeartRegistry(config);

        Map<String, Heart> all = registry.getAll();
        assertEquals(1, all.size());
        assertTrue(all.containsKey("red"));

        assertThrows(UnsupportedOperationException.class,
                () -> all.put("blue", new Heart("blue", "Blue", 2, 2.0, Material.NETHER_STAR, "", Map.of())));
        assertThrows(UnsupportedOperationException.class, all::clear);
    }

    @Test
    void resolvesTierUsingFirstRegisteredEntryForEachTier() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("hearts.alpha.display", "Alpha");
        config.set("hearts.alpha.tier", 5);
        config.set("hearts.alpha.hearts", 1.0);

        config.set("hearts.beta.display", "Beta");
        config.set("hearts.beta.tier", 5);
        config.set("hearts.beta.hearts", 9.0);

        HeartRegistry registry = new HeartRegistry(config);

        Heart tierHeart = registry.getByTier(5);
        assertNotNull(tierHeart);
        assertEquals("alpha", tierHeart.getId());
    }
}
