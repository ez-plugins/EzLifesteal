package com.skyblockexp.ezlifesteal.gui;

import com.skyblockexp.ezlifesteal.model.SpawnedBeacon;
import com.skyblockexp.ezlifesteal.model.SpawnedBeaconStatus;
import com.skyblockexp.ezlifesteal.service.BeaconReviveService;
import java.util.List;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
        assertEquals(54, inv.getSize());
    }

    @Test
    void constructor_countdownBeacon_inventoryCreatedWithCorrectSize() {
        BeaconInfoMenu menu = new BeaconInfoMenu(player, countdownBeacon());
        Inventory inv = menu.getInventory();

        assertNotNull(inv);
        assertEquals(54, inv.getSize());
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

        // Fixed structural borders + conditional borders present in read-only/no-ban-list mode
        int[] borderSlots = {
                // Row 1 top border
                0, 1, 2, 3, 4, 5, 6, 7, 8,
                // Row 2 sides
                9, 17,
                // Row 3 separator
                18, 19, 20, 21, 22, 23, 24, 25, 26,
                // Rows 4–5 sides
                27, 35, 36, 44,
                // Row 6 fixed borders
                45, 47, 48, 50, 51, 53,
                // Row 6 nav/action: show borders in read-only mode (no revive service, page 0, no players)
                46, 49, 52
        };
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

        assertTrue(player.getOpenInventory().getTopInventory().equals(menu.getInventory())
                || menu.getInventory().equals(player.getOpenInventory().getTopInventory()));
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

    @Test
    void constructor_emptyBannedList_slot31HasBarrier() {
        BeaconInfoMenu menu = new BeaconInfoMenu(player, availableBeacon(), null, List.of());
        Inventory inv = menu.getInventory();

        // PLAYER_SLOTS[3] = slot 31 — "no eliminated players" placeholder
        assertNotNull(inv.getItem(31));
        assertEquals(Material.BARRIER, inv.getItem(31).getType());
    }

    @Test
    void constructor_withBannedPlayers_playerSlotsHaveSkullsStartingAt28() {
        List<String> banned = List.of("Alpha", "Beta", "Gamma");
        BeaconReviveService reviveService = mock(BeaconReviveService.class);
        BeaconInfoMenu menu = new BeaconInfoMenu(player, availableBeacon(), reviveService, banned);
        Inventory inv = menu.getInventory();

        assertEquals(Material.SKELETON_SKULL, inv.getItem(28).getType(), "Slot 28 should show first banned player");
        assertEquals(Material.SKELETON_SKULL, inv.getItem(29).getType(), "Slot 29 should show second banned player");
        assertEquals(Material.SKELETON_SKULL, inv.getItem(30).getType(), "Slot 30 should show third banned player");
        // Slot 31 onwards should be empty (only 3 players)
        assertNotEquals(Material.SKELETON_SKULL, inv.getItem(31) == null ? Material.AIR : inv.getItem(31).getType(),
                "Slot 31 should not have a fourth skull");
    }

    @Test
    void constructor_availableBeacon_withReviveService_slot49HasNetherStar() {
        BeaconReviveService reviveService = mock(BeaconReviveService.class);
        BeaconInfoMenu menu = new BeaconInfoMenu(player, availableBeacon(), reviveService, List.of());
        Inventory inv = menu.getInventory();

        assertNotNull(inv.getItem(49));
        assertEquals(Material.NETHER_STAR, inv.getItem(49).getType(),
                "Use Beacon button (slot 49) should be a NETHER_STAR when beacon is available");
    }

    @Test
    void constructor_countdownBeacon_withReviveService_slot49IsBorder() {
        BeaconReviveService reviveService = mock(BeaconReviveService.class);
        BeaconInfoMenu menu = new BeaconInfoMenu(player, countdownBeacon(), reviveService, List.of());
        Inventory inv = menu.getInventory();

        assertNotNull(inv.getItem(49));
        assertEquals(Material.PURPLE_STAINED_GLASS_PANE, inv.getItem(49).getType(),
                "Use Beacon button (slot 49) should be hidden when beacon is in countdown");
    }

    @Test
    void handleClick_cancelsEvent_alwaysEvenForPlayerInventoryClicks() {
        BeaconInfoMenu menu = new BeaconInfoMenu(player, availableBeacon());
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        // getClickedInventory() returns null by default — simulates player-inventory click
        when(event.getClickedInventory()).thenReturn(null);

        menu.handleClick(event);

        verify(event).setCancelled(true);
    }
}
