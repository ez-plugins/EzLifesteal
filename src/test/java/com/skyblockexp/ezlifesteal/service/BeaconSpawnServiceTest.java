package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.config.BeaconSpawnSettings;
import com.skyblockexp.ezlifesteal.integration.BeaconAreaProtection;
import com.skyblockexp.ezlifesteal.integration.BeaconCountdownProvider;
import com.skyblockexp.ezlifesteal.model.SpawnedBeacon;
import com.skyblockexp.ezlifesteal.model.SpawnedBeaconStatus;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.storage.SpawnedBeaconRepository;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BeaconSpawnServiceTest {

    private ServerMock server;
    private JavaPlugin javaPlugin;
    private WorldMock world;
    private Logger logger;
    private SpawnedBeaconRepository repository;
    private BeaconAvailabilityService availabilityService;
    private BeaconSpawnService service;
    private PluginAccessor accessor;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        javaPlugin = MockBukkit.createMockPlugin();
        world = server.addSimpleWorld("world");
        logger = mock(Logger.class);
        repository = new SpawnedBeaconRepository();
        availabilityService = mock(BeaconAvailabilityService.class);
        service = new BeaconSpawnService(repository, availabilityService, logger);

        accessor = mock(PluginAccessor.class);
        when(accessor.getPlugin()).thenReturn(javaPlugin);
    }

    @AfterEach
    void tearDown() {
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    private BeaconSpawnSettings settingsEnabled(boolean countdownEnabled, int countdownSeconds, int maxConcurrent) {
        return new BeaconSpawnSettings(
                true,
                maxConcurrent,
                BeaconSpawnSettings.WorldGuardSettings.defaults(),
                new BeaconSpawnSettings.CountdownSettings(
                        countdownEnabled,
                        countdownSeconds,
                        java.util.List.of(),
                        null,
                        null,
                        null,
                        "ezls-beacon-",
                        java.util.Map.of()),
                BeaconSpawnSettings.RandomSpawnSettings.defaults(),
                java.util.List.of(),
                0,
                BeaconSpawnSettings.ScheduleSettings.defaults(),
                new BeaconSpawnSettings.ExpirySettings(0),
                new BeaconSpawnSettings.AvailabilityEventSettings(
                        false, "key", false, "key", "key", false, false)
        );
    }

    @Test
    void spawnBeacon_featureDisabled_returnsEmpty() {
        when(accessor.getBeaconSpawnSettings()).thenReturn(BeaconSpawnSettings.disabled());

        Location loc = new Location(world, 0, 64, 0);
        Optional<SpawnedBeacon> result = service.spawnBeacon(loc, accessor);

        assertTrue(result.isEmpty());
        assertEquals(0, repository.size());
    }

    @Test
    void spawnBeacon_maxConcurrentReached_returnsEmpty() {
        BeaconSpawnSettings settings = settingsEnabled(false, 0, 0);
        when(accessor.getBeaconSpawnSettings()).thenReturn(settings);

        Location loc = new Location(world, 0, 64, 0);
        Optional<SpawnedBeacon> result = service.spawnBeacon(loc, accessor);

        assertTrue(result.isEmpty());
    }

    @Test
    void spawnBeacon_noCountdown_immediatelyAvailable() {
        BeaconSpawnSettings settings = settingsEnabled(false, 0, 10);
        when(accessor.getBeaconSpawnSettings()).thenReturn(settings);

        Location loc = new Location(world, 10, 64, 10);
        Optional<SpawnedBeacon> result = service.spawnBeacon(loc, accessor);

        assertTrue(result.isPresent());
        assertEquals(SpawnedBeaconStatus.AVAILABLE, result.get().getStatus());
        assertEquals(1, repository.size());
    }

    /**
     * Regression test for bug: when countdown is disabled, markAvailable() had an early-return guard
     * ({@code if (beacon.getStatus() != COUNTDOWN) return}) that caused the availability event and
     * expiry timer to be silently skipped for immediately-available beacons.
     */
    @Test
    void spawnBeacon_noCountdown_firesAvailabilityEvent() {
        BeaconSpawnSettings settings = settingsEnabled(false, 0, 10);
        when(accessor.getBeaconSpawnSettings()).thenReturn(settings);

        Location loc = new Location(world, 10, 64, 10);
        Optional<SpawnedBeacon> result = service.spawnBeacon(loc, accessor);

        assertTrue(result.isPresent());
        verify(availabilityService).fireAvailabilityEvent(any(), any(), any());
    }

    @Test
    void spawnBeacon_withCountdown_startsInCountdownStatus() {
        BeaconSpawnSettings settings = settingsEnabled(true, 300, 10);
        when(accessor.getBeaconSpawnSettings()).thenReturn(settings);

        Location loc = new Location(world, 20, 64, 20);
        Optional<SpawnedBeacon> result = service.spawnBeacon(loc, accessor);

        assertTrue(result.isPresent());
        assertEquals(SpawnedBeaconStatus.COUNTDOWN, result.get().getStatus());
        assertEquals(1, repository.size());
    }

    @Test
    void despawnBeacon_removesFromRepository() {
        BeaconSpawnSettings settings = settingsEnabled(true, 300, 10);
        when(accessor.getBeaconSpawnSettings()).thenReturn(settings);

        Location loc = new Location(world, 5, 64, 5);
        Optional<SpawnedBeacon> spawned = service.spawnBeacon(loc, accessor);
        assertTrue(spawned.isPresent());
        assertEquals(1, repository.size());

        service.despawnBeacon(spawned.get().getId(), accessor);
        assertEquals(0, repository.size());
    }

    @Test
    void despawnBeacon_unknownId_doesNothing() {
        // Should not throw
        service.despawnBeacon(UUID.randomUUID(), accessor);
        assertEquals(0, repository.size());
    }

    @Test
    void despawnAll_removesAllBeacons() {
        BeaconSpawnSettings settings = settingsEnabled(true, 300, 10);
        when(accessor.getBeaconSpawnSettings()).thenReturn(settings);

        service.spawnBeacon(new Location(world, 1, 64, 1), accessor);
        service.spawnBeacon(new Location(world, 2, 64, 2), accessor);
        assertEquals(2, repository.size());

        service.despawnAll(accessor);
        assertEquals(0, repository.size());
    }

    @Test
    void findByLocation_returnsBeaconAtThatLocation() {
        BeaconSpawnSettings settings = settingsEnabled(true, 300, 10);
        when(accessor.getBeaconSpawnSettings()).thenReturn(settings);

        Location loc = new Location(world, 50, 64, 50);
        service.spawnBeacon(loc, accessor);

        Optional<SpawnedBeacon> found = service.findByLocation(loc);
        assertTrue(found.isPresent());
    }

    @Test
    void findByLocation_differentLocation_returnsEmpty() {
        BeaconSpawnSettings settings = settingsEnabled(true, 300, 10);
        when(accessor.getBeaconSpawnSettings()).thenReturn(settings);

        service.spawnBeacon(new Location(world, 50, 64, 50), accessor);

        Optional<SpawnedBeacon> found = service.findByLocation(new Location(world, 99, 64, 99));
        assertTrue(found.isEmpty());
    }

    @Test
    void getActiveBeacons_returnsAllActive() {
        BeaconSpawnSettings settings = settingsEnabled(true, 300, 10);
        when(accessor.getBeaconSpawnSettings()).thenReturn(settings);

        service.spawnBeacon(new Location(world, 1, 64, 1), accessor);
        service.spawnBeacon(new Location(world, 2, 64, 2), accessor);

        Collection<SpawnedBeacon> beacons = service.getActiveBeacons();
        assertEquals(2, beacons.size());
    }

    @Test
    void markAvailable_transitionsCountdownBeaconToAvailable() {
        BeaconSpawnSettings settings = settingsEnabled(true, 300, 10);
        when(accessor.getBeaconSpawnSettings()).thenReturn(settings);

        Location loc = new Location(world, 30, 64, 30);
        Optional<SpawnedBeacon> spawned = service.spawnBeacon(loc, accessor);
        assertTrue(spawned.isPresent());
        assertEquals(SpawnedBeaconStatus.COUNTDOWN, spawned.get().getStatus());

        service.markAvailable(spawned.get().getId(), accessor);
        assertEquals(SpawnedBeaconStatus.AVAILABLE, spawned.get().getStatus());
    }

    @Test
    void markAvailable_alreadyAvailable_doesNothing() {
        BeaconSpawnSettings settings = settingsEnabled(true, 300, 10);
        when(accessor.getBeaconSpawnSettings()).thenReturn(settings);

        Location loc = new Location(world, 40, 64, 40);
        Optional<SpawnedBeacon> spawned = service.spawnBeacon(loc, accessor);
        assertTrue(spawned.isPresent());

        // First transition
        service.markAvailable(spawned.get().getId(), accessor);
        assertEquals(SpawnedBeaconStatus.AVAILABLE, spawned.get().getStatus());

        // Second call — already AVAILABLE, should not throw
        service.markAvailable(spawned.get().getId(), accessor);
        assertEquals(SpawnedBeaconStatus.AVAILABLE, spawned.get().getStatus());
    }

    @Test
    void markAvailable_unknownId_doesNothing() {
        // Should not throw
        when(accessor.getBeaconSpawnSettings()).thenReturn(BeaconSpawnSettings.disabled());
        service.markAvailable(UUID.randomUUID(), accessor);
    }

    @Test
    void markUsedByLocation_availableBeacon_removesIt() {
        BeaconSpawnSettings settings = settingsEnabled(true, 300, 10);
        when(accessor.getBeaconSpawnSettings()).thenReturn(settings);

        Location loc = new Location(world, 60, 64, 60);
        Optional<SpawnedBeacon> spawned = service.spawnBeacon(loc, accessor);
        assertTrue(spawned.isPresent());
        service.markAvailable(spawned.get().getId(), accessor);

        service.markUsedByLocation(loc, accessor);
        assertEquals(0, repository.size());
    }

    @Test
    void findRandomSpawnLocation_disabled_returnsEmpty() {
        BeaconSpawnSettings.RandomSpawnSettings disabled =
                new BeaconSpawnSettings.RandomSpawnSettings(false, "world", -100, 100, 0, 0, -100, 100);

        Optional<Location> result = service.findRandomSpawnLocation(disabled);
        assertTrue(result.isEmpty());
    }

    @Test
    void spawnBeacon_withAreaProtection_callsProtect() {
        BeaconSpawnSettings settings = settingsEnabled(true, 300, 10);
        when(accessor.getBeaconSpawnSettings()).thenReturn(settings);

        BeaconAreaProtection protection = mock(BeaconAreaProtection.class);
        when(protection.protect(any(), any())).thenReturn(Optional.of("ezls-region-1"));
        service.setAreaProtection(protection);

        Location loc = new Location(world, 70, 64, 70);
        service.spawnBeacon(loc, accessor);

        verify(protection).protect(any(), any());
    }

    @Test
    void despawnBeacon_withAreaProtection_callsUnprotect() {
        BeaconSpawnSettings settings = settingsEnabled(true, 300, 10);
        when(accessor.getBeaconSpawnSettings()).thenReturn(settings);

        BeaconAreaProtection protection = mock(BeaconAreaProtection.class);
        when(protection.protect(any(), any())).thenReturn(Optional.of("ezls-region-test"));
        service.setAreaProtection(protection);

        Location loc = new Location(world, 80, 64, 80);
        Optional<SpawnedBeacon> spawned = service.spawnBeacon(loc, accessor);
        assertTrue(spawned.isPresent());

        service.despawnBeacon(spawned.get().getId(), accessor);
        verify(protection).unprotect(anyString(), any());
    }

    @Test
    void spawnBeacon_withCountdownProvider_startsCountdown() {
        BeaconSpawnSettings settings = settingsEnabled(true, 60, 10);
        when(accessor.getBeaconSpawnSettings()).thenReturn(settings);

        BeaconCountdownProvider countdownProvider = mock(BeaconCountdownProvider.class);
        when(countdownProvider.startCountdown(anyString(), any())).thenReturn(Optional.of("ezls-beacon-abc123"));
        service.setCountdownProvider(countdownProvider);

        Location loc = new Location(world, 90, 64, 90);
        Optional<SpawnedBeacon> result = service.spawnBeacon(loc, accessor);

        assertTrue(result.isPresent());
        verify(countdownProvider).startCountdown(anyString(), any());
    }

    @Test
    void spawnBeacon_locationWithNullWorld_returnsEmpty() {
        BeaconSpawnSettings settings = settingsEnabled(false, 0, 10);
        when(accessor.getBeaconSpawnSettings()).thenReturn(settings);

        Location locNullWorld = new Location(null, 0, 64, 0);
        Optional<SpawnedBeacon> result = service.spawnBeacon(locNullWorld, accessor);

        assertTrue(result.isEmpty());
        assertEquals(0, repository.size());
    }

    @Test
    void spawnBeacon_cooldownActive_returnsEmpty() throws Exception {
        // Trigger a previous beacon end so lastBeaconEndedAtMillis is set
        Field field = BeaconSpawnService.class.getDeclaredField("lastBeaconEndedAtMillis");
        field.setAccessible(true);
        field.set(service, System.currentTimeMillis()); // set to now

        // cooldownMinutes = 60 → 1h cooldown; since we just set lastBeaconEndedAtMillis to now, elapsed << 3600s
        BeaconSpawnSettings settings = new BeaconSpawnSettings(
                true, 10,
                BeaconSpawnSettings.WorldGuardSettings.defaults(),
                new BeaconSpawnSettings.CountdownSettings(false, 0, List.of(), null, null, null, "ezls-beacon-", java.util.Map.of()),
                BeaconSpawnSettings.RandomSpawnSettings.defaults(),
                List.of(),
                60, // cooldownMinutes
                BeaconSpawnSettings.ScheduleSettings.defaults(),
                new BeaconSpawnSettings.ExpirySettings(0),
                new BeaconSpawnSettings.AvailabilityEventSettings(false, "key", false, "key", "key", false, false)
        );
        when(accessor.getBeaconSpawnSettings()).thenReturn(settings);

        Location loc = new Location(world, 5, 64, 5);
        Optional<SpawnedBeacon> result = service.spawnBeacon(loc, accessor);

        assertTrue(result.isEmpty());
    }

    @Test
    void spawnBeacon_eventCancelled_returnsEmpty() {
        BeaconSpawnSettings settings = settingsEnabled(false, 0, 10);
        when(accessor.getBeaconSpawnSettings()).thenReturn(settings);

        // Register a listener that cancels BeaconSpawnEvent
        server.getPluginManager().registerEvents(
                new org.bukkit.event.Listener() {
                    @org.bukkit.event.EventHandler
                    public void onBeaconSpawn(com.skyblockexp.ezlifesteal.api.event.BeaconSpawnEvent event) {
                        event.setCancelled(true);
                    }
                },
                javaPlugin
        );

        Location loc = new Location(world, 5, 64, 5);
        Optional<SpawnedBeacon> result = service.spawnBeacon(loc, accessor);

        assertTrue(result.isEmpty());
        assertEquals(0, repository.size());
    }

    @Test
    void markUsedByLocation_nonAvailableBeacon_doesNotRemove() {
        BeaconSpawnSettings settings = settingsEnabled(true, 300, 10);
        when(accessor.getBeaconSpawnSettings()).thenReturn(settings);

        Location loc = new Location(world, 100, 64, 100);
        Optional<SpawnedBeacon> spawned = service.spawnBeacon(loc, accessor);
        assertTrue(spawned.isPresent());
        // Beacon is in COUNTDOWN status (not AVAILABLE), so markUsedByLocation must not remove it
        assertEquals(SpawnedBeaconStatus.COUNTDOWN, spawned.get().getStatus());

        service.markUsedByLocation(loc, accessor);

        assertEquals(1, repository.size()); // still present
    }

    @Test
    void findRandomSpawnLocationFromRegions_nullList_returnsEmpty() {
        Optional<Location> result = service.findRandomSpawnLocationFromRegions(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void findRandomSpawnLocationFromRegions_emptyList_returnsEmpty() {
        Optional<Location> result = service.findRandomSpawnLocationFromRegions(List.of());
        assertTrue(result.isEmpty());
    }

    @Test
    void findRandomSpawnLocationFromRegions_allDisabled_returnsEmpty() {
        BeaconSpawnSettings.RandomSpawnRegion disabled =
                new BeaconSpawnSettings.RandomSpawnRegion(false, "world", -100, 100, 0, 0, -100, 100, 1);
        Optional<Location> result = service.findRandomSpawnLocationFromRegions(List.of(disabled));
        assertTrue(result.isEmpty());
    }

    @Test
    void findRandomSpawnLocationFromRegions_worldNotFound_returnsEmpty() {
        BeaconSpawnSettings.RandomSpawnRegion badWorld =
                new BeaconSpawnSettings.RandomSpawnRegion(true, "nonexistent_world", -100, 100, 0, 0, -100, 100, 1);
        Optional<Location> result = service.findRandomSpawnLocationFromRegions(List.of(badWorld));
        assertTrue(result.isEmpty());
    }

    @Test
    void findRandomSpawnLocationFromRegions_validRegion_returnsLocation() {
        // Use a 1x1 range so x=0, z=0, y=64 is deterministic; set solid block below
        world.getBlockAt(0, 63, 0).setType(Material.STONE);
        BeaconSpawnSettings.RandomSpawnRegion region =
                new BeaconSpawnSettings.RandomSpawnRegion(true, "world", 0, 1, 64, 65, 0, 1, 1);
        Optional<Location> result = service.findRandomSpawnLocationFromRegions(List.of(region));
        assertTrue(result.isPresent());
    }

    @Test
    void findRandomSpawnLocation_withRegions_usesRegions() {
        world.getBlockAt(0, 63, 0).setType(Material.STONE);
        BeaconSpawnSettings.RandomSpawnRegion region =
                new BeaconSpawnSettings.RandomSpawnRegion(true, "world", 0, 1, 64, 65, 0, 1, 1);
        BeaconSpawnSettings settings = new BeaconSpawnSettings(
                true, 10,
                BeaconSpawnSettings.WorldGuardSettings.defaults(),
                BeaconSpawnSettings.CountdownSettings.defaults(),
                BeaconSpawnSettings.RandomSpawnSettings.defaults(),
                List.of(region),
                0,
                BeaconSpawnSettings.ScheduleSettings.defaults(),
                new BeaconSpawnSettings.ExpirySettings(0),
                new BeaconSpawnSettings.AvailabilityEventSettings(false, "key", false, "key", "key", false, false)
        );

        Optional<Location> result = service.findRandomSpawnLocation(settings);
        assertTrue(result.isPresent());
    }

    @Test
    void findRandomSpawnLocation_worldNotFound_returnsEmpty() {
        BeaconSpawnSettings.RandomSpawnSettings randomSettings =
                new BeaconSpawnSettings.RandomSpawnSettings(true, "nonexistent_world", -100, 100, 64, 65, -100, 100);
        Optional<Location> result = service.findRandomSpawnLocation(randomSettings);
        assertTrue(result.isEmpty());
    }
}
