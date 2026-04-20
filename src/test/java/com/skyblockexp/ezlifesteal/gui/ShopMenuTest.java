package com.skyblockexp.ezlifesteal.gui;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.heart.Heart;
import com.skyblockexp.ezlifesteal.heart.HeartRegistry;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShopMenuTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void populateSkipsInvalidEntriesAndStillPlacesValidItemsWithFallbacksAndMetadata() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        HeartRegistry registry = mock(HeartRegistry.class);
        Heart heart = mock(Heart.class);

        when(heart.getId()).thenReturn("basic");
        when(heart.getTier()).thenReturn(3);
        when(heart.getHearts()).thenReturn(2.5);
        when(heart.getDisplayName()).thenReturn("&aRegistry Heart");
        when(heart.getTexture()).thenReturn("");
        when(heart.createItemStack()).thenReturn(new ItemStack(Material.EMERALD));
        when(registry.getById("basic")).thenReturn(heart);
        when(registry.getById("ghost")).thenReturn(null);

        YamlConfiguration shopConfig = new YamlConfiguration();
        shopConfig.set("size", 27);
        shopConfig.set("title", "&6Shop");
        shopConfig.set("items", List.of(
                Map.of("slot", -1, "heart", "basic", "price", 100.0),
                Map.of("slot", 1, "price", 50.0),
                Map.of("slot", 2, "heart", "ghost", "price", 80.0),
                Map.of(
                        "slot", 4,
                        "heart", "basic",
                        "price", 125.5,
                        "quantity", 2,
                        "lore", List.of("Custom lore line"),
                        "commands", List.of("say hi", "give %player% emerald 1"),
                        "icon", "NOT_A_REAL_MATERIAL"
                ),
                Map.of("slot", 99, "heart", "basic", "price", 999.0),
                Map.of(
                        "slot", 6,
                        "heart", "basic",
                        "price", 10.0,
                        "icon", "DIAMOND"
                )
        ));

        when(plugin.getShopConfig()).thenReturn(shopConfig);
        when(plugin.getHeartRegistry()).thenReturn(registry);

        PlayerMock player = server.addPlayer();
        ShopGuiManager.openShop(plugin, player);

        InventoryView openInventory = player.getOpenInventory();
        assertNotNull(openInventory);

        Inventory topInventory = openInventory.getTopInventory();
        assertNull(topInventory.getItem(1));
        assertNull(topInventory.getItem(2));

        ItemStack slot4 = topInventory.getItem(4);
        assertNotNull(slot4);
        assertEquals(Material.EMERALD, slot4.getType());

        ItemMeta slot4Meta = slot4.getItemMeta();
        assertNotNull(slot4Meta);
        assertEquals("§aRegistry Heart", slot4Meta.getDisplayName());
        assertNotNull(slot4Meta.getLore());
        assertTrue(slot4Meta.getLore().contains("Custom lore line"));
        assertTrue(slot4Meta.getLore().contains("§7Price: §6125.50"));

        ItemStack slot6 = topInventory.getItem(6);
        assertNotNull(slot6);
        assertEquals(Material.DIAMOND, slot6.getType());
    }
}
