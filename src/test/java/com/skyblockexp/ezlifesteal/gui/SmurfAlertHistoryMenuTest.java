package com.skyblockexp.ezlifesteal.gui;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.detector.SmurfDetector;
import com.skyblockexp.ezlifesteal.test.MockBukkitTestHelper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SmurfAlertHistoryMenuTest {

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
    void nullOrEmptyDetectorHistoryShowsNoAlertsPlaceholderAtSlot22() {
        EzLifestealPlugin nullDetectorPlugin = mock(EzLifestealPlugin.class);
        when(nullDetectorPlugin.getSmurfDetector()).thenReturn(null);

        PlayerMock nullDetectorViewer = server.addPlayer();
        SmurfAlertHistoryMenu nullDetectorMenu = new SmurfAlertHistoryMenu(nullDetectorPlugin, nullDetectorViewer);
        assertNoAlertsPlaceholder(nullDetectorMenu);

        EzLifestealPlugin emptyHistoryPlugin = mock(EzLifestealPlugin.class);
        SmurfDetector detector = mock(SmurfDetector.class);
        when(detector.getAlertHistory()).thenReturn(List.of());
        when(emptyHistoryPlugin.getSmurfDetector()).thenReturn(detector);

        PlayerMock emptyHistoryViewer = server.addPlayer();
        SmurfAlertHistoryMenu emptyHistoryMenu = new SmurfAlertHistoryMenu(emptyHistoryPlugin, emptyHistoryViewer);
        assertNoAlertsPlaceholder(emptyHistoryMenu);
    }

    @Test
    void moreThanFortyFiveAlertsOnlyPopulatesFirstPageSlots() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        SmurfDetector detector = mock(SmurfDetector.class);
        when(detector.getAlertHistory()).thenReturn(alerts(60));
        when(plugin.getSmurfDetector()).thenReturn(detector);

        PlayerMock viewer = server.addPlayer();
        SmurfAlertHistoryMenu menu = new SmurfAlertHistoryMenu(plugin, viewer);

        for (int slot = 0; slot < 45; slot++) {
            ItemStack item = menu.getInventory().getItem(slot);
            assertNotNull(item, "Content slot " + slot + " should be populated on first page");
            assertEquals(Material.PAPER, item.getType(), "Content slot " + slot + " should be an alert item");
        }
    }

    @Test
    void backAndCloseSlotsOpenManagementOrCloseInventory() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getSmurfDetector()).thenReturn(null);

        PlayerMock viewer = server.addPlayer();
        SmurfAlertHistoryMenu menu = new SmurfAlertHistoryMenu(plugin, viewer);
        viewer.openInventory(menu.getInventory());

        InventoryClickEvent backClick = mockClick(menu, 45);
        menu.handleClick(backClick);
        assertTrue(backClick.isCancelled(), "Back slot click should be cancelled");
        assertInstanceOf(SmurfManagementMenu.class, viewer.getOpenInventory().getTopInventory().getHolder());

        viewer.openInventory(menu.getInventory());
        InventoryClickEvent closeClick = mockClick(menu, 49);
        menu.handleClick(closeClick);
        assertTrue(closeClick.isCancelled(), "Close slot click should be cancelled");
        assertSame(null, viewer.getOpenInventory().getTopInventory());
    }

    @Test
    void nonActionSlotsKeepInventoryOpenAndStillCancelClicks() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getSmurfDetector()).thenReturn(null);

        PlayerMock viewer = server.addPlayer();
        SmurfAlertHistoryMenu menu = new SmurfAlertHistoryMenu(plugin, viewer);
        viewer.openInventory(menu.getInventory());

        InventoryClickEvent neutralClick = mockClick(menu, 10);
        menu.handleClick(neutralClick);

        assertTrue(neutralClick.isCancelled(), "Non-action slot clicks should be cancelled");
        assertSame(menu, viewer.getOpenInventory().getTopInventory().getHolder(),
                "Non-action slots should keep the alert history menu open");
    }

    private void assertNoAlertsPlaceholder(SmurfAlertHistoryMenu menu) {
        ItemStack item = menu.getInventory().getItem(22);
        assertNotNull(item, "No alerts placeholder should exist at slot 22");
        assertEquals(Material.GRAY_DYE, item.getType());
        assertNotNull(item.getItemMeta());
        assertEquals("§7No alerts", item.getItemMeta().getDisplayName());
    }

    private InventoryClickEvent mockClick(SmurfAlertHistoryMenu menu, int rawSlot) {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getRawSlot()).thenReturn(rawSlot);
        when(event.getInventory()).thenReturn(menu.getInventory());
        doAnswer(invocation -> {
            cancelled.set(invocation.getArgument(0));
            return null;
        }).when(event).setCancelled(org.mockito.ArgumentMatchers.anyBoolean());
        when(event.isCancelled()).thenAnswer(invocation -> cancelled.get());
        return event;
    }

    private List<SmurfDetector.SmurfAlert> alerts(int count) {
        List<SmurfDetector.SmurfAlert> alerts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            alerts.add(new SmurfDetector.SmurfAlert(
                    "killer-" + i,
                    "victim-" + i,
                    5 + i,
                    2,
                    Instant.now().minusSeconds(i)
            ));
        }
        return alerts;
    }
}
