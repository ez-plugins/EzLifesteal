package com.skyblockexp.ezlifesteal.gui;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.heart.Heart;
import com.skyblockexp.ezlifesteal.heart.HeartRegistry;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShopGuiManagerTest {

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
    void openShopUsesConfiguredTitleAndItemSlots() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        YamlConfiguration shopConfig = new YamlConfiguration();
        shopConfig.set("size", 27);
        shopConfig.set("title", "&6Custom Heart Shop");
        shopConfig.set("items", List.of(
                java.util.Map.of(
                        "slot", 11,
                        "heart", "basic",
                        "price", 125.0,
                        "quantity", 2,
                        "display-name", "&cBasic Heart",
                        "lore", List.of("&7Buy one life"),
                        "icon", "NETHER_STAR"
                )
        ));

        HeartRegistry registry = mock(HeartRegistry.class);
        Heart heart = mock(Heart.class);
        when(heart.getId()).thenReturn("basic");
        when(heart.getTier()).thenReturn(1);
        when(heart.getHearts()).thenReturn(1.0);
        when(heart.getDisplayName()).thenReturn("&cBasic Heart");
        when(heart.getTexture()).thenReturn("");
        when(heart.createItemStack()).thenReturn(new ItemStack(Material.NETHER_STAR));
        when(registry.getById("basic")).thenReturn(heart);

        when(plugin.getShopConfig()).thenReturn(shopConfig);
        when(plugin.getHeartRegistry()).thenReturn(registry);

        PlayerMock player = server.addPlayer();

        ShopGuiManager.openShop(plugin, player);

        InventoryView openInventory = player.getOpenInventory();
        assertNotNull(openInventory);
        assertEquals("§6Custom Heart Shop", openInventory.getTitle());
        assertNotNull(openInventory.getTopInventory().getItem(11));
        assertEquals(Material.NETHER_STAR, openInventory.getTopInventory().getItem(11).getType());
    }
}
