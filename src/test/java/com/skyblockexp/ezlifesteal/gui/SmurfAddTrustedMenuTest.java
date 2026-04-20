package com.skyblockexp.ezlifesteal.gui;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.detector.SmurfDetector;
import com.skyblockexp.ezlifesteal.test.MockBukkitTestHelper;
import java.util.List;
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

class SmurfAddTrustedMenuTest {

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
    void emptyCandidateListRendersExpectedPlaceholder() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);

        PlayerMock viewer = server.addPlayer();
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of());

            SmurfAddTrustedMenu menu = new SmurfAddTrustedMenu(plugin, viewer);

            assertEquals(Material.GRAY_DYE, menu.getInventory().getItem(22).getType());
            assertEquals("§7No eligible players", menu.getInventory().getItem(22).getItemMeta().getDisplayName());
        }
    }

    @Test
    void clickingCandidateAddsExemptionAndSendsSuccessMessage() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        SmurfDetector detector = mock(SmurfDetector.class);
        MessageService messageService = mock(MessageService.class);
        when(plugin.getSmurfDetector()).thenReturn(detector);
        when(plugin.getMessageService()).thenReturn(messageService);

        OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
        when(offlinePlayer.getName()).thenReturn("CandidateOne");

        PlayerMock viewer = server.addPlayer();
        PlayerMock candidate = server.addPlayer();
        UUID candidateId = candidate.getUniqueId();
        when(detector.isExempt(candidateId)).thenReturn(false);
        when(plugin.addSmurfExemption(candidateId)).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer(candidateId)).thenReturn(offlinePlayer);

            SmurfAddTrustedMenu menu = new SmurfAddTrustedMenu(plugin, viewer);
            viewer.openInventory(menu.getInventory());

            int candidateSlot = findSlotWithPlayerName(menu, candidate.getName());
            InventoryClickEvent click = mockClick(menu, candidateSlot);
            menu.handleClick(click);

            verify(plugin).addSmurfExemption(candidateId);
            verify(messageService).sendMessage(eq(viewer), eq("smurf-gui-added"), anyMap());
            assertTrue(click.isCancelled());
            assertInstanceOf(SmurfTrustedMenu.class, viewer.getOpenInventory().getTopInventory().getHolder());
        }
    }

    @Test
    void navigationSlotsBackAndCloseRouteCorrectly() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        PlayerMock viewer = server.addPlayer();

        SmurfAddTrustedMenu menu = new SmurfAddTrustedMenu(plugin, viewer);
        viewer.openInventory(menu.getInventory());

        InventoryClickEvent backClick = mockClick(menu, 45);
        menu.handleClick(backClick);
        assertTrue(backClick.isCancelled());
        assertInstanceOf(SmurfTrustedMenu.class, viewer.getOpenInventory().getTopInventory().getHolder());

        viewer.openInventory(menu.getInventory());
        InventoryClickEvent closeClick = mockClick(menu, 49);
        menu.handleClick(closeClick);
        assertTrue(closeClick.isCancelled());
        assertSame(null, viewer.getOpenInventory().getTopInventory());
    }

    private int findSlotWithPlayerName(SmurfAddTrustedMenu menu, String name) {
        for (int slot = 0; slot < 45; slot++) {
            if (menu.getInventory().getItem(slot) == null || menu.getInventory().getItem(slot).getItemMeta() == null) {
                continue;
            }
            String displayName = menu.getInventory().getItem(slot).getItemMeta().getDisplayName();
            if (("§a" + name).equals(displayName)) {
                return slot;
            }
        }
        throw new AssertionError("Expected to find candidate entry for " + name);
    }

    private InventoryClickEvent mockClick(SmurfAddTrustedMenu menu, int rawSlot) {
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
