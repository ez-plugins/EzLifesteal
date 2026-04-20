package com.skyblockexp.ezlifesteal.gui;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.test.MockBukkitTestHelper;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SmurfManagementMenuTest {

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
    void slotActionsMapToExpectedMenusAndCancelClicks() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        PlayerMock viewer = server.addPlayer();

        SmurfManagementMenu menu = new SmurfManagementMenu(plugin, viewer);
        viewer.openInventory(menu.getInventory());

        InventoryClickEvent trustedClick = mockClick(menu, 11);
        menu.handleClick(trustedClick);
        assertTrue(trustedClick.isCancelled(), "Trusted slot click should be cancelled");
        assertInstanceOf(SmurfTrustedMenu.class, viewer.getOpenInventory().getTopInventory().getHolder());

        viewer.openInventory(menu.getInventory());
        InventoryClickEvent alertClick = mockClick(menu, 13);
        menu.handleClick(alertClick);
        assertTrue(alertClick.isCancelled(), "Alert history slot click should be cancelled");
        assertInstanceOf(SmurfAlertHistoryMenu.class, viewer.getOpenInventory().getTopInventory().getHolder());

        viewer.openInventory(menu.getInventory());
        InventoryClickEvent killClick = mockClick(menu, 15);
        menu.handleClick(killClick);
        assertTrue(killClick.isCancelled(), "Kill history slot click should be cancelled");
        assertInstanceOf(SmurfKillHistoryMenu.class, viewer.getOpenInventory().getTopInventory().getHolder());

        viewer.openInventory(menu.getInventory());
        InventoryClickEvent closeClick = mockClick(menu, 22);
        menu.handleClick(closeClick);
        assertTrue(closeClick.isCancelled(), "Close slot click should be cancelled");
        assertSame(null, viewer.getOpenInventory().getTopInventory());
    }

    @Test
    void openStateMismatchSlotsAreBlockedWithoutSideEffects() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        PlayerMock viewer = server.addPlayer();

        SmurfManagementMenu menu = new SmurfManagementMenu(plugin, viewer);
        viewer.openInventory(menu.getInventory());

        InventoryClickEvent playerInventorySlot = mockClick(menu, 30);
        menu.handleClick(playerInventorySlot);

        assertTrue(playerInventorySlot.isCancelled(), "Clicks should still be cancelled for GUI context");
        assertSame(menu, viewer.getOpenInventory().getTopInventory().getHolder(),
                "Unexpected/raw slot clicks must not navigate away from management menu");
    }

    @Test
    void nullItemAndNullMetaClicksAreSafelyIgnored() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        PlayerMock viewer = server.addPlayer();
        SmurfManagementMenu menu = new SmurfManagementMenu(plugin, viewer);

        InventoryClickEvent click = mockClick(menu, 26);
        when(click.getCurrentItem()).thenReturn(null);

        menu.handleClick(click);

        assertTrue(click.isCancelled());
    }

    private InventoryClickEvent mockClick(SmurfManagementMenu menu, int rawSlot) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getRawSlot()).thenReturn(rawSlot);
        when(event.getInventory()).thenReturn(menu.getInventory());
        AtomicBoolean cancelled = new AtomicBoolean(false);
        doAnswer(invocation -> {
            cancelled.set(invocation.getArgument(0));
            return null;
        }).when(event).setCancelled(org.mockito.ArgumentMatchers.anyBoolean());
        when(event.isCancelled()).thenAnswer(invocation -> cancelled.get());
        return event;
    }
}
