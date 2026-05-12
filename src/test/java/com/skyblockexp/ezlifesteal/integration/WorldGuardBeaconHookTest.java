package com.skyblockexp.ezlifesteal.integration;

import com.skyblockexp.ezlifesteal.config.BeaconSpawnSettings;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link WorldGuardBeaconHook}.
 *
 * <p>WorldGuard is not on the test classpath. All tests exercise the "exception → empty/graceful"
 * path triggered when WorldGuard classes are unavailable.</p>
 */
class WorldGuardBeaconHookTest {

    private ServerMock server;
    private WorldMock world;
    private Logger logger;
    private WorldGuardBeaconHook hook;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        logger = mock(Logger.class);
        hook = new WorldGuardBeaconHook(logger);
    }

    @AfterEach
    void tearDown() {
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    private BeaconSpawnSettings.WorldGuardSettings enabledSettings() {
        return new BeaconSpawnSettings.WorldGuardSettings(true, 10, true, false, false, false);
    }

    private BeaconSpawnSettings.WorldGuardSettings disabledSettings() {
        return new BeaconSpawnSettings.WorldGuardSettings(false, 10, true, false, false, false);
    }

    @Test
    void protect_settingsDisabled_returnsEmpty() {
        Location loc = new Location(world, 0, 64, 0);
        Optional<String> result = hook.protect(loc, disabledSettings());
        assertTrue(result.isEmpty());
    }

    @Test
    void protect_worldGuardNotAvailable_returnsEmpty() {
        // WorldGuard not on classpath → exception → empty
        Location loc = new Location(world, 0, 64, 0);
        Optional<String> result = hook.protect(loc, enabledSettings());
        assertTrue(result.isEmpty());
    }

    @Test
    void unprotect_nullRegionId_doesNotThrow() {
        // regionId is null → should return immediately without exception
        hook.unprotect(null, world);
    }

    @Test
    void unprotect_worldGuardNotAvailable_doesNotThrow() {
        // WorldGuard not on classpath → exception caught, logs warning
        hook.unprotect("ezls-beacon-abcd1234", world);
    }

    @Test
    void unprotect_nullWorld_doesNotThrow() {
        hook.unprotect("ezls-beacon-test", null);
    }
}
