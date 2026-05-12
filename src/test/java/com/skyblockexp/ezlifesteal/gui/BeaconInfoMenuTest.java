package com.skyblockexp.ezlifesteal.gui;

import com.skyblockexp.ezlifesteal.model.SpawnedBeacon;
import com.skyblockexp.ezlifesteal.model.SpawnedBeaconStatus;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BeaconInfoMenuTest {

    private ServerMock server;
    private WorldMock world;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    private SpawnedBeacon availableBeacon() {
        SpawnedBeacon beacon = mock(SpawnedBeacon.class);
        when(beacon.getId()).thenReturn(UUID.randomUUID());
        when(beacon.shortId()).thenReturn("abc12345");
        when(beacon.getStatus()).thenReturn(SpawnedBeaconStatus.AVAILABLE);
        when(beacon.getLocation()).thenReturn(new Location(world, 10, 64, 10));
        when(beacon.getSpawnedAtMillis()).thenReturn(System.currentTimeMillis() - 120_000L);
        when(beacon.getExpiresAtMillis()).thenReturn(0L);
        return beacon;
    }

    private SpawnedBeacon countdownBeacon() {
        SpawnedBeacon beacon = mock(SpawnedBeacon.class);
        when(beacon.getId()).thenReturn(UUID.randomUUID());
        when(beacon.shortId()).thenReturn("def56789");
        when(beacon.getStatus()).thenReturn(SpawnedBeaconStatus.COUNTDOWN);
        when(beacon.getLocation()).thenReturn(new Location(world, 20, 70, 20));
        when(beacon.getSpawnedAtMillis()).thenReturn(System.currentTimeMillis() - 30_000L);
        when(beacon.getExpiresAtMillis()).thenReturn(0L);
        return beacon;
    }

    @Test
    void constructor_availableBeacon_inventoryCreatedWithCorrectSize() {
        BeaconInfoMenu menu = new BeaconInfoMenu(player, availableBeacon());
        Inventory inv = menu.getInventory();

        assertNotNull(inv);
        assertEquals(27, inv.getSize());
    }

    @Test
    void constructor_countdownBeacon_inventoryCreatedWithCorrectSize() {
        BeaconInfoMenu menu = new BeaconInfoMenu(player, countdownBeacon());
        Inventory inv = menu.getInventory();

        assertNotNull(inv);
        assertEquals(27, inv.getSize());
    }

    @Test
    void constructor_availableBeacon_slot11HasEmerald() {
        BeaconInfoMenu menu = new BeaconInfoMenu(player, availableBeacon());
        Inventory inv = menu.getInventory();

        assertNotNull(inv.getItem(11));
        assertEquals(Material.EMERALD, inv.getItem(11).getType());
    }

    @Test
    void constructor_countdownBeacon_slot11HasClock() {
        BeaconInfoMenu menu = new BeaconInfoMenu(player, countdownBeacon());
        Inventory inv = menu.getInventory();

        assertNotNull(inv.getItem(11));
        assertEquals(Material.CLOCK, inv.getItem(11).getType());
    }

    @Test
    void constructor_availableBeacon_slot13HasBeaconMaterial() {
        BeaconInfoMenu menu = new BeaconInfoMenu(player, availableBeacon());
        Inventory inv = menu.getInventory();

        assertNotNull(inv.getItem(13));
        assertEquals(Material.BEACON, inv.getItem(13).getType());
    }

    @Test
    void constructor_countdownBeacon_slot13HasGlassPane() {
        BeaconInfoMenu menu = new BeaconInfoMenu(player, countdownBeacon());
        Inventory inv = menu.getInventory();

        assertNotNull(inv.getItem(13));
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, inv.getItem(13).getType());
    }

    @Test
    void constructor_slot15HasCompass() {
        BeaconInfoMenu menu = new BeaconInfoMenu(player, availableBeacon());
        Inventory inv = menu.getInventory();

        assertNotNull(inv.getItem(15));
        assertEquals(Material.COMPASS, inv.getItem(15).getType());
    }

    @Test
    void constructor_borderSlotsFilledWithGlassPane() {
        BeaconInfoMenu menu = new BeaconInfoMenu(player, availableBeacon());
        Inventory inv = menu.getInventory();

        int[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 18, 19, 20, 21, 22, 23, 24, 25, 26, 9, 17};
        for (int slot : borderSlots) {
            assertNotNull(inv.getItem(slot), "Border slot " + slot + " should not be null");
            assertEquals(Material.PURPLE_STAINED_GLASS_PANE, inv.getItem(slot).getType(),
                    "Slot " + slot + " should be purple glass pane");
        }
    }

    @Test
    void constructor_withExpiryTime_inventoryCreated() {
        SpawnedBeacon beacon = mock(SpawnedBeacon.class);
        when(beacon.getId()).thenReturn(UUID.randomUUID());
        when(beacon.shortId()).thenReturn("exp12345");
        when(beacon.getStatus()).thenReturn(SpawnedBeaconStatus.AVAILABLE);
        when(beacon.getLocation()).thenReturn(new Location(world, 5, 64, 5));
        when(beacon.getSpawnedAtMillis()).thenReturn(System.currentTimeMillis());
        when(beacon.getExpiresAtMillis()).thenReturn(System.currentTimeMillis() + 3_600_000L); // 1h from now

        BeaconInfoMenu menu = new BeaconInfoMenu(player, beacon);
        assertNotNull(menu.getInventory());
    }

    @Test
    void getInventory_returnsNonNull() {
        BeaconInfoMenu menu = new BeaconInfoMenu(player, availableBeacon());
        assertNotNull(menu.getInventory());
    }

    @Test
    void open_opensInventoryForViewer() {
        BeaconInfoMenu menu = new BeaconInfoMenu(player, availableBeacon());
        menu.open();

        // Player's open inventory should now be the menu's inventory
        assertTrue(player.getOpenInventory().getTopInventory().equals(menu.getInventory())
                || menu.getInventory().equals(player.getOpenInventory().getTopInventory()));
    }

    @Test
    void handleClick_cancelsEvent() {
        BeaconInfoMenu menu = new BeaconInfoMenu(player, availableBeacon());
        InventoryClickEvent event = mock(InventoryClickEvent.class);

        menu.handleClick(event);

        verify(event).setCancelled(true);
    }

    @Test
    void constructor_spawnedJustNow_inventoryCreated() {
        SpawnedBeacon beacon = mock(SpawnedBeacon.class);
        when(beacon.getId()).thenReturn(UUID.randomUUID());
        when(beacon.shortId()).thenReturn("new12345");
        when(beacon.getStatus()).thenReturn(SpawnedBeaconStatus.AVAILABLE);
        when(beacon.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        // Spawned less than 60 seconds ago → formatRelativeTime returns "just now"
        when(beacon.getSpawnedAtMillis()).thenReturn(System.currentTimeMillis() - 1_000L);
        when(beacon.getExpiresAtMillis()).thenReturn(0L);

        BeaconInfoMenu menu = new BeaconInfoMenu(player, beacon);
        assertNotNull(menu.getInventory());
    }
}
