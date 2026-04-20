package com.skyblockexp.ezlifesteal.heart;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.test.MockBukkitTestHelper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class HeartTest {

    @BeforeEach
    void setUp() {
        MockBukkitTestHelper.startServer();
        EzLifestealPlugin.HEART_KEY = null;
    }

    @AfterEach
    void tearDown() {
        EzLifestealPlugin.HEART_KEY = null;
        MockBukkitTestHelper.stopServer();
    }

    @Test
    void preservesConstructorValuesForCoreFields() {
        Map<String, Object> nbt = new HashMap<>();
        nbt.put("custom", "value");

        Heart heart = new Heart("starter", "&cStarter Heart", 2, 1.5, Material.DIAMOND, "texture-id", nbt);

        assertEquals("starter", heart.getId());
        assertEquals("&cStarter Heart", heart.getDisplayName());
        assertEquals(2, heart.getTier());
        assertEquals(1.5, heart.getHearts());
        assertSame(Material.DIAMOND, heart.getMaterial());
        assertEquals("texture-id", heart.getTexture());
        assertSame(nbt, heart.getNbt());
    }

    @Test
    void defaultsMaterialToNetherStarWhenNullMaterialIsProvided() {
        Heart heart = new Heart("starter", "Heart", 1, 1.0, null, "", Map.of());

        assertSame(Material.NETHER_STAR, heart.getMaterial());
    }

    @Test
    void createItemStackPopulatesDisplayLoreAndPersistentIdWhenKeyIsConfigured() {
        NamespacedKey heartKey = new NamespacedKey("ezlifesteal", "heart_id");
        EzLifestealPlugin.HEART_KEY = heartKey;

        Heart heart = new Heart("starter", "&cStarter Heart", 3, 2.5, Material.DIAMOND, "texture-id",
                Map.of("secret", "value"));
        ItemStack item = heart.createItemStack();

        assertEquals(Material.DIAMOND, item.getType());

        ItemMeta meta = item.getItemMeta();
        assertNotNull(meta);
        assertEquals(ChatColor.RED + "Starter Heart", meta.getDisplayName());
        assertEquals(List.of("Tier: 3", "Hearts: 2.5", "Texture: texture-id"), meta.getLore());
        assertEquals("starter", meta.getPersistentDataContainer().get(heartKey, PersistentDataType.STRING));
    }

    @Test
    void createItemStackOmitsBlankDisplayAndTextureAndSkipsPersistentIdWithoutKey() {
        Heart heart = new Heart("starter", "   ", 1, 1.0, Material.NETHER_STAR, "   ", Map.of());

        ItemStack item = heart.createItemStack();
        ItemMeta meta = item.getItemMeta();

        assertNotNull(meta);
        assertEquals("", meta.getDisplayName());
        assertEquals(List.of("Tier: 1", "Hearts: 1.0"), meta.getLore());
        assertNull(meta.getPersistentDataContainer().get(
                new NamespacedKey("ezlifesteal", "heart_id"), PersistentDataType.STRING));
    }

    @Test
    void doesNotImplementValueEqualityByDefault() {
        Heart first = new Heart("same", "Heart", 1, 1.0, Material.NETHER_STAR, "", Map.of());
        Heart second = new Heart("same", "Heart", 1, 1.0, Material.NETHER_STAR, "", Map.of());

        assertNotSame(first, second);
        assertNotEquals(first, second);
        assertNotEquals(first.hashCode(), second.hashCode());
    }
}
