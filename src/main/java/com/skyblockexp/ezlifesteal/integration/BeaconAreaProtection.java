package com.skyblockexp.ezlifesteal.integration;

import com.skyblockexp.ezlifesteal.config.BeaconSpawnSettings;
import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Abstraction over WorldGuard region management for spawned beacons.
 *
 * <p>Implementations may be no-ops when WorldGuard is absent.</p>
 */
public interface BeaconAreaProtection {

    /**
     * Creates a protected region around the beacon centre.
     *
     * @param center   the beacon block location
     * @param settings WorldGuard flag settings from config
     * @return the region identifier if creation succeeded, or empty when WorldGuard is unavailable
     */
    Optional<String> protect(Location center, BeaconSpawnSettings.WorldGuardSettings settings);

    /**
     * Removes a previously created protection region.
     *
     * @param regionId the region id returned by {@link #protect}, may be {@code null}
     * @param world    the world the region was created in
     */
    void unprotect(String regionId, World world);
}
