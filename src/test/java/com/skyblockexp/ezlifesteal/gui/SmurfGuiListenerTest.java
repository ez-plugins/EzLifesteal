package com.skyblockexp.ezlifesteal.gui;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.test.MockBukkitTestHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmurfGuiListenerTest {

    private ServerMock server;

    @BeforeEach
    void setUp() {
        server = MockBukkitTestHelper.startServer();
    }

    @AfterEach
    void tearDown() {
        MockBukkitTestHelper.stopServer();
    }

    @Test
    void onInventoryClickRoutesToSmurfMenusButSkipsShopMenu() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Player viewer = server.addPlayer();

        TrackingMenu menu = new TrackingMenu(plugin, viewer);
        InventoryClickEvent menuEvent = mock(InventoryClickEvent.class);
        when(menuEvent.getInventory()).thenReturn(menu.getInventory());

        SmurfGuiListener listener = new SmurfGuiListener();
        listener.onInventoryClick(menuEvent);
        assertFalse(menu.handleClickCount == 0, "Smurf menu click should route into menu.handleClick");

        Inventory shopInventory = Bukkit.createInventory(mock(ShopMenu.class), 9, "Shop");
        InventoryClickEvent shopEvent = mock(InventoryClickEvent.class);
        when(shopEvent.getInventory()).thenReturn(shopInventory);

        listener.onInventoryClick(shopEvent);
        verify(shopEvent, never()).setCancelled(true);
    }

    @Test
    void onInventoryDragCancelsForSmurfMenuInventoriesOnly() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Player viewer = server.addPlayer();
        TrackingMenu menu = new TrackingMenu(plugin, viewer);

        InventoryDragEvent menuDrag = mock(InventoryDragEvent.class);
        when(menuDrag.getInventory()).thenReturn(menu.getInventory());

        SmurfGuiListener listener = new SmurfGuiListener();
        listener.onInventoryDrag(menuDrag);
        verify(menuDrag).setCancelled(true);

        Inventory nonSmurfInventory = Bukkit.createInventory(null, 9, "Other");
        InventoryDragEvent otherDrag = mock(InventoryDragEvent.class);
        when(otherDrag.getInventory()).thenReturn(nonSmurfInventory);

        listener.onInventoryDrag(otherDrag);
        verify(otherDrag, never()).setCancelled(true);
    }

    private static final class TrackingMenu extends AbstractSmurfMenu {

        private int handleClickCount;

        private TrackingMenu(EzLifestealPlugin plugin, Player viewer) {
            super(plugin, viewer, 9, "Test");
            setItem(0, new ItemStack(Material.STONE));
        }

        @Override
        public void handleClick(InventoryClickEvent event) {
            handleClickCount++;
            event.setCancelled(true);
        }
    }
}
