package com.skyblockexp.ezlifesteal.gui;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.detector.SmurfDetector;
import com.skyblockexp.ezlifesteal.test.MockBukkitTestHelper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SmurfKillHistoryMenuTest {

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
    void capsDisplayToFirstPageAndKeepsPagingBoundariesSafe() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        SmurfDetector detector = mock(SmurfDetector.class);
        when(detector.getKillHistory()).thenReturn(killRecords(60));
        when(plugin.getSmurfDetector()).thenReturn(detector);

        PlayerMock viewer = server.addPlayer();
        SmurfKillHistoryMenu menu = new SmurfKillHistoryMenu(plugin, viewer);
        viewer.openInventory(menu.getInventory());

        assertEquals(Material.BOOK, menu.getInventory().getItem(44).getType(),
                "Last content slot on page 1 should contain a kill entry");
        assertEquals(Material.ARROW, menu.getInventory().getItem(45).getType());
        assertEquals(Material.BARRIER, menu.getInventory().getItem(49).getType());

        InventoryClickEvent nextBoundary = mockClick(menu, 50);
        menu.handleClick(nextBoundary);
        assertTrue(nextBoundary.isCancelled(), "Boundary click should still be cancelled");
        assertInstanceOf(SmurfKillHistoryMenu.class, viewer.getOpenInventory().getTopInventory().getHolder(),
                "Out-of-range paging clicks should not navigate to another menu");
    }

    @Test
    void backAndCloseSlotsMapCorrectly() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getSmurfDetector()).thenReturn(null);

        PlayerMock viewer = server.addPlayer();
        SmurfKillHistoryMenu menu = new SmurfKillHistoryMenu(plugin, viewer);
        viewer.openInventory(menu.getInventory());

        InventoryClickEvent backClick = mockClick(menu, 45);
        menu.handleClick(backClick);
        assertTrue(backClick.isCancelled());
        assertInstanceOf(SmurfManagementMenu.class, viewer.getOpenInventory().getTopInventory().getHolder());

        viewer.openInventory(menu.getInventory());
        InventoryClickEvent closeClick = mockClick(menu, 49);
        menu.handleClick(closeClick);
        assertTrue(closeClick.isCancelled());
        assertSame(null, viewer.getOpenInventory().getTopInventory());
    }

    @Test
    void nullItemAndNullMetaClicksAreSafelyIgnored() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getSmurfDetector()).thenReturn(null);

        PlayerMock viewer = server.addPlayer();
        SmurfKillHistoryMenu menu = new SmurfKillHistoryMenu(plugin, viewer);

        InventoryClickEvent click = mockClick(menu, 10);
        when(click.getCurrentItem()).thenReturn(null);

        menu.handleClick(click);

        assertTrue(click.isCancelled());
    }

    private InventoryClickEvent mockClick(SmurfKillHistoryMenu menu, int rawSlot) {
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

    private List<SmurfDetector.KillRecord> killRecords(int count) {
        List<SmurfDetector.KillRecord> records = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            records.add(new SmurfDetector.KillRecord(
                    UUID.randomUUID(),
                    "killer-" + i,
                    UUID.randomUUID(),
                    "victim-" + i,
                    Instant.now().minusSeconds(i)
            ));
        }
        return records;
    }
}
