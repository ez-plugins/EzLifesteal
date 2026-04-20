package com.skyblockexp.ezlifesteal.gui;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.detector.SmurfDetector;
import com.skyblockexp.ezlifesteal.test.MockBukkitTestHelper;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmurfTrustedMenuTest {

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
    void emptyTrustedListRendersExpectedPlaceholder() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        SmurfDetector detector = mock(SmurfDetector.class);
        when(detector.getExemptPlayers()).thenReturn(Set.of());
        when(plugin.getSmurfDetector()).thenReturn(detector);

        PlayerMock viewer = server.addPlayer();
        SmurfTrustedMenu menu = new SmurfTrustedMenu(plugin, viewer);

        assertEquals(Material.GRAY_DYE, menu.getInventory().getItem(13).getType());
        assertEquals("§7No trusted players", menu.getInventory().getItem(13).getItemMeta().getDisplayName());
    }

    @Test
    void clickingTrustedEntryRemovesExemptionAndSendsSuccessMessage() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        SmurfDetector detector = mock(SmurfDetector.class);
        MessageService messageService = mock(MessageService.class);
        UUID trustedId = UUID.randomUUID();

        when(detector.getExemptPlayers()).thenReturn(Set.of(trustedId));
        when(plugin.getSmurfDetector()).thenReturn(detector);
        when(plugin.removeSmurfExemption(trustedId)).thenReturn(true);
        when(plugin.getMessageService()).thenReturn(messageService);

        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(offlinePlayer.getName()).thenReturn("TrustedOne");

        PlayerMock viewer = server.addPlayer();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer(trustedId)).thenReturn(offlinePlayer);

            SmurfTrustedMenu menu = new SmurfTrustedMenu(plugin, viewer);
            viewer.openInventory(menu.getInventory());

            InventoryClickEvent click = mockClick(menu, 0);
            menu.handleClick(click);

            verify(plugin).removeSmurfExemption(trustedId);
            verify(messageService).sendMessage(eq(viewer), eq("smurf-gui-removed"), anyMap());
            assertTrue(click.isCancelled());
            assertInstanceOf(SmurfTrustedMenu.class, viewer.getOpenInventory().getTopInventory().getHolder());
        }
    }

    @Test
    void navigationSlotsBackCloseAndAddTrustedRouteCorrectly() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        SmurfDetector detector = mock(SmurfDetector.class);
        when(detector.getExemptPlayers()).thenReturn(Set.of());
        when(plugin.getSmurfDetector()).thenReturn(detector);

        PlayerMock viewer = server.addPlayer();
        SmurfTrustedMenu menu = new SmurfTrustedMenu(plugin, viewer);
        viewer.openInventory(menu.getInventory());

        InventoryClickEvent backClick = mockClick(menu, 45);
        menu.handleClick(backClick);
        assertTrue(backClick.isCancelled());
        assertInstanceOf(SmurfManagementMenu.class, viewer.getOpenInventory().getTopInventory().getHolder());

        viewer.openInventory(menu.getInventory());
        InventoryClickEvent addClick = mockClick(menu, 53);
        menu.handleClick(addClick);
        assertTrue(addClick.isCancelled());
        assertInstanceOf(SmurfAddTrustedMenu.class, viewer.getOpenInventory().getTopInventory().getHolder());

        viewer.openInventory(menu.getInventory());
        InventoryClickEvent closeClick = mockClick(menu, 49);
        menu.handleClick(closeClick);
        assertTrue(closeClick.isCancelled());
        assertSame(null, viewer.getOpenInventory().getTopInventory());
    }

    private InventoryClickEvent mockClick(SmurfTrustedMenu menu, int rawSlot) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        when(event.getRawSlot()).thenReturn(rawSlot);
        when(event.getInventory()).thenReturn(menu.getInventory());
        doAnswer(invocation -> {
            cancelled.set(invocation.getArgument(0));
            return null;
        }).when(event).setCancelled(org.mockito.ArgumentMatchers.anyBoolean());
        when(event.isCancelled()).thenAnswer(invocation -> cancelled.get());
        return event;
    }
}
