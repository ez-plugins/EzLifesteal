package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.api.event.BeaconAvailableEvent;
import com.skyblockexp.ezlifesteal.api.event.BeaconExpiredEvent;
import com.skyblockexp.ezlifesteal.api.event.BeaconSpawnEvent;
import com.skyblockexp.ezlifesteal.api.event.BeaconUsedEvent;
import com.skyblockexp.ezlifesteal.config.BeaconSpawnSettings;
import com.skyblockexp.ezlifesteal.integration.BeaconAreaProtection;
import com.skyblockexp.ezlifesteal.integration.BeaconCountdownProvider;
import com.skyblockexp.ezlifesteal.model.SpawnedBeacon;
import com.skyblockexp.ezlifesteal.model.SpawnedBeaconStatus;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.storage.SpawnedBeaconRepository;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Shulker;

/**
 * Central service that manages the lifecycle of plugin-spawned revive beacons.
 *
 * <p>Spawn flow: place block → protect region → start countdown or internal timer →
 * mark AVAILABLE (fires event) → player uses beacon or expiry removes it.</p>
 */
public final class BeaconSpawnService {

    private static final Random RANDOM = new Random();

    private final SpawnedBeaconRepository repository;
    private final BeaconAvailabilityService availabilityService;
    private final Logger logger;

    /** Optional — may be null when WorldGuard is absent. */
    private BeaconAreaProtection areaProtection;

    /** Optional — may be null when EzCountdown is absent. */
    private BeaconCountdownProvider countdownProvider;

    /** Timestamp of the last beacon completion (used, expired, or despawned). */
    private volatile long lastBeaconEndedAtMillis = 0;

    public BeaconSpawnService(
            SpawnedBeaconRepository repository,
            BeaconAvailabilityService availabilityService,
            Logger logger
    ) {
        this.repository = repository;
        this.availabilityService = availabilityService;
        this.logger = logger;
    }

    public void setAreaProtection(BeaconAreaProtection areaProtection) {
        this.areaProtection = areaProtection;
    }

    public void setCountdownProvider(BeaconCountdownProvider countdownProvider) {
        this.countdownProvider = countdownProvider;
    }

    // -------------------------------------------------------------------------
    // Spawn / despawn
    // -------------------------------------------------------------------------

    /**
     * Spawns a beacon at the given location, sets up protection and countdown.
     *
     * @param location the block location for the BEACON block
     * @param accessor plugin accessor for scheduling and settings
     * @return the spawned beacon model, or empty when spawn was rejected (e.g. concurrent limit reached)
     */
    public Optional<SpawnedBeacon> spawnBeacon(Location location, PluginAccessor accessor) {
        final BeaconSpawnSettings settings = accessor.getBeaconSpawnSettings();
        if (!settings.enabled()) {
            return Optional.empty();
        }

        final int activeCount = repository.getAllWithStatus(SpawnedBeaconStatus.COUNTDOWN).size()
                + repository.getAllWithStatus(SpawnedBeaconStatus.AVAILABLE).size();
        if (activeCount >= settings.maxConcurrent()) {
            return Optional.empty();
        }

        // Cooldown check
        final int cooldownMinutes = settings.cooldownMinutes();
        if (cooldownMinutes > 0 && lastBeaconEndedAtMillis > 0) {
            final long cooldownMillis = (long) cooldownMinutes * 60_000L;
            final long elapsed = System.currentTimeMillis() - lastBeaconEndedAtMillis;
            if (elapsed < cooldownMillis) {
                final long remainingSeconds = (cooldownMillis - elapsed) / 1000L;
                logger.info("Beacon spawn rejected: cooldown active (" + remainingSeconds + "s remaining).");
                return Optional.empty();
            }
        }

        // Place the beacon block
        final World world = location.getWorld();
        if (world == null) {
            logger.warning("Cannot spawn beacon: location has no world.");
            return Optional.empty();
        }
        location.getBlock().setType(Material.BEACON, false);

        // Spawn invisible glowing Shulker for visual outline effect
        final UUID glowEntityId = spawnGlowEntity(location, world);

        // Create WG region if available
        final String regionId = areaProtection != null
                ? areaProtection.protect(location, settings.worldGuard()).orElse(null)
                : null;

        final UUID beaconId = UUID.randomUUID();
        final BeaconSpawnSettings.CountdownSettings countdownSettings = settings.countdown();
        final boolean countdownEnabled = countdownSettings.enabled() && countdownSettings.durationSeconds() > 0;

        // Attempt EzCountdown (or null if provider absent)
        String countdownId = null;
        if (countdownEnabled && countdownProvider != null) {
            countdownId = countdownProvider.startCountdown(
                    beaconId.toString().substring(0, 8),
                    countdownSettings
            ).orElse(null);
        }

        // Always start in COUNTDOWN so markAvailable() can perform the full
        // transition (fire event, schedule expiry) regardless of whether a
        // timed countdown is configured.
        final SpawnedBeaconStatus initialStatus = SpawnedBeaconStatus.COUNTDOWN;

        final long expiryMillis = 0; // set when beacon becomes AVAILABLE
        final SpawnedBeacon beacon = new SpawnedBeacon(
                beaconId,
                location.clone(),
                regionId,
                countdownId,
                System.currentTimeMillis(),
                initialStatus,
                expiryMillis
        );
        if (glowEntityId != null) {
            beacon.setGlowEntityId(glowEntityId);
        }
        // Fire BeaconSpawnEvent — listeners may cancel the spawn
        final BeaconSpawnEvent spawnEvent = new BeaconSpawnEvent(beacon);
        Bukkit.getPluginManager().callEvent(spawnEvent);
        if (spawnEvent.isCancelled()) {
            cleanupBeacon(beacon);
            logger.info("Beacon spawn at " + world.getName() + " "
                    + location.getBlockX() + " " + location.getBlockY() + " "
                    + location.getBlockZ() + " was cancelled by a plugin event listener.");
            return Optional.empty();
        }

        repository.add(beacon);

        if (countdownEnabled) {
            // Fallback internal timer when EzCountdown did not supply a countdown id
            if (countdownId == null) {
                final long delayTicks = (long) countdownSettings.durationSeconds() * 20L;
                SchedulerAdapter.runLater(accessor.getPlugin(), () -> markAvailable(beaconId, accessor), delayTicks);
            }
            // When EzCountdown IS present, availability is triggered by CountdownEndEvent in SpawnedBeaconListener.
        } else {
            // No countdown configured — fire availability event immediately
            markAvailable(beaconId, accessor);
        }

        logger.info("Spawned beacon " + beacon.shortId()
                + " at " + world.getName() + " " + location.getBlockX() + " "
                + location.getBlockY() + " " + location.getBlockZ()
                + (countdownEnabled ? " (countdown: " + countdownSettings.durationSeconds() + "s)" : " (immediate)")
                + (regionId != null ? " | WG region: " + regionId : ""));

        return Optional.of(beacon);
    }

    /**
     * Removes a spawned beacon from the world and cleans up its resources.
     *
     * @param beaconId the UUID of the beacon to remove
     * @param accessor plugin accessor for cancelling tasks if needed
     */
    public void despawnBeacon(UUID beaconId, PluginAccessor accessor) {
        final Optional<SpawnedBeacon> optBeacon = repository.remove(beaconId);
        if (optBeacon.isEmpty()) {
            return;
        }
        final SpawnedBeacon beacon = optBeacon.get();
        cleanupBeacon(beacon);
        lastBeaconEndedAtMillis = System.currentTimeMillis();
        logger.info("Despawned beacon " + beacon.shortId()
                + " (status=" + beacon.getStatus() + ").");
    }

    /**
     * Removes all tracked beacons — called on plugin shutdown (best-effort).
     *
     * @param accessor plugin accessor
     */
    public void despawnAll(PluginAccessor accessor) {
        repository.getAll().stream()
                .map(SpawnedBeacon::getId)
                .toList()
                .forEach(id -> despawnBeacon(id, accessor));
    }

    // -------------------------------------------------------------------------
    // Status transitions
    // -------------------------------------------------------------------------

    /**
     * Marks a beacon as AVAILABLE and fires the availability event.
     *
     * @param beaconId the UUID of the beacon
     * @param accessor plugin accessor
     */
    public void markAvailable(UUID beaconId, PluginAccessor accessor) {
        final Optional<SpawnedBeacon> optBeacon = repository.findById(beaconId);
        if (optBeacon.isEmpty()) {
            return;
        }
        final SpawnedBeacon beacon = optBeacon.get();
        if (beacon.getStatus() != SpawnedBeaconStatus.COUNTDOWN) {
            return;
        }
        beacon.setStatus(SpawnedBeaconStatus.AVAILABLE);

        // Apply expiry timer
        final BeaconSpawnSettings settings = accessor.getBeaconSpawnSettings();
        final int expiryMinutes = settings.expiry().expiryMinutes();
        if (expiryMinutes > 0) {
            final long expiryMillis = System.currentTimeMillis() + (long) expiryMinutes * 60_000L;
            beacon.setExpiresAtMillis(expiryMillis);
            final long expiryTicks = (long) expiryMinutes * 60L * 20L;
            SchedulerAdapter.runLater(accessor.getPlugin(), () -> {
                final Optional<SpawnedBeacon> current = repository.findById(beaconId);
                if (current.isPresent() && current.get().getStatus() == SpawnedBeaconStatus.AVAILABLE) {
                    expireBeacon(beaconId, accessor);
                }
            }, expiryTicks);
        }

        // Fire the cool availability event
        availabilityService.fireAvailabilityEvent(beacon, accessor, settings.availabilityEvent());
        Bukkit.getPluginManager().callEvent(new BeaconAvailableEvent(beacon));
        logger.info("Beacon " + beacon.shortId() + " is now AVAILABLE.");
    }

    /**
     * Marks a beacon as USED — called after a successful revive at the beacon location.
     *
     * @param blockLocation the beacon block location
     * @param accessor      plugin accessor
     */
    public void markUsedByLocation(Location blockLocation, PluginAccessor accessor) {
        repository.findByLocation(blockLocation).ifPresent(beacon -> {
            if (beacon.getStatus() == SpawnedBeaconStatus.AVAILABLE) {
                beacon.setStatus(SpawnedBeaconStatus.USED);
                cleanupBeacon(beacon);
                repository.remove(beacon.getId());
                lastBeaconEndedAtMillis = System.currentTimeMillis();
                Bukkit.getPluginManager().callEvent(new BeaconUsedEvent(beacon));
                logger.info("Beacon " + beacon.shortId() + " was used and removed.");
            }
        });
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public Optional<SpawnedBeacon> findByLocation(Location location) {
        return repository.findByLocation(location);
    }

    public Collection<SpawnedBeacon> getActiveBeacons() {
        return repository.getAll();
    }

    // -------------------------------------------------------------------------
    // Random spawn helpers
    // -------------------------------------------------------------------------

    /**
     * Selects a random spawn location from the configured {@link BeaconSpawnSettings.RandomSpawnSettings}.
     *
     * @param settings random spawn settings
     * @return a valid location, or empty if no suitable position was found
     */
    public Optional<Location> findRandomSpawnLocation(BeaconSpawnSettings.RandomSpawnSettings settings) {
        if (!settings.enabled()) {
            return Optional.empty();
        }
        final World world = Bukkit.getWorld(settings.worldName());
        if (world == null) {
            logger.warning("Random spawn world '" + settings.worldName() + "' not found.");
            return Optional.empty();
        }
        final int rangeX = Math.max(1, settings.maxX() - settings.minX());
        final int rangeZ = Math.max(1, settings.maxZ() - settings.minZ());
        final boolean useYBounds = settings.minY() != 0 || settings.maxY() != 0;

        for (int attempt = 0; attempt < 15; attempt++) {
            final int x = settings.minX() + RANDOM.nextInt(rangeX);
            final int z = settings.minZ() + RANDOM.nextInt(rangeZ);
            final int y;
            if (useYBounds) {
                final int rangeY = Math.max(1, settings.maxY() - settings.minY());
                y = settings.minY() + RANDOM.nextInt(rangeY);
            } else {
                y = world.getHighestBlockYAt(x, z) + 1;
            }
            final Location candidate = new Location(world, x, y, z);
            if (isValidSpawnLocation(candidate)) {
                return Optional.of(candidate);
            }
        }
        logger.warning("Could not find a valid random spawn location after 15 attempts.");
        return Optional.empty();
    }

    /**
     * Selects a spawn location from a list of weighted regions using weighted random selection.
     *
     * @param regions the list of candidate regions (only enabled regions are considered)
     * @return a valid location, or empty if no suitable position was found
     */
    public Optional<Location> findRandomSpawnLocationFromRegions(List<BeaconSpawnSettings.RandomSpawnRegion> regions) {
        if (regions == null || regions.isEmpty()) {
            return Optional.empty();
        }
        final List<BeaconSpawnSettings.RandomSpawnRegion> enabled = regions.stream()
                .filter(BeaconSpawnSettings.RandomSpawnRegion::enabled)
                .toList();
        if (enabled.isEmpty()) {
            return Optional.empty();
        }
        // Weighted selection
        final int totalWeight = enabled.stream().mapToInt(BeaconSpawnSettings.RandomSpawnRegion::weight).sum();
        int roll = RANDOM.nextInt(Math.max(1, totalWeight));
        BeaconSpawnSettings.RandomSpawnRegion selected = enabled.get(enabled.size() - 1);
        for (BeaconSpawnSettings.RandomSpawnRegion region : enabled) {
            roll -= region.weight();
            if (roll < 0) {
                selected = region;
                break;
            }
        }
        final World world = Bukkit.getWorld(selected.worldName());
        if (world == null) {
            logger.warning("Random spawn region world '" + selected.worldName() + "' not found.");
            return Optional.empty();
        }
        final int rangeX = Math.max(1, selected.maxX() - selected.minX());
        final int rangeZ = Math.max(1, selected.maxZ() - selected.minZ());
        final boolean useYBounds = selected.minY() != 0 || selected.maxY() != 0;

        for (int attempt = 0; attempt < 15; attempt++) {
            final int x = selected.minX() + RANDOM.nextInt(rangeX);
            final int z = selected.minZ() + RANDOM.nextInt(rangeZ);
            final int y;
            if (useYBounds) {
                final int rangeY = Math.max(1, selected.maxY() - selected.minY());
                y = selected.minY() + RANDOM.nextInt(rangeY);
            } else {
                y = world.getHighestBlockYAt(x, z) + 1;
            }
            final Location candidate = new Location(world, x, y, z);
            if (isValidSpawnLocation(candidate)) {
                return Optional.of(candidate);
            }
        }
        logger.warning("Could not find a valid random spawn location from regions after 15 attempts.");
        return Optional.empty();
    }

    /**
     * Finds a random spawn location using regions if configured, or the legacy single-region settings.
     *
     * @param settings the full beacon spawn settings
     * @return a valid location, or empty if no suitable position was found
     */
    public Optional<Location> findRandomSpawnLocation(BeaconSpawnSettings settings) {
        final List<BeaconSpawnSettings.RandomSpawnRegion> regions = settings.randomSpawnRegions();
        if (regions != null && !regions.isEmpty()) {
            return findRandomSpawnLocationFromRegions(regions);
        }
        return findRandomSpawnLocation(settings.randomSpawn());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void expireBeacon(UUID beaconId, PluginAccessor accessor) {
        final Optional<SpawnedBeacon> optBeacon = repository.remove(beaconId);
        if (optBeacon.isEmpty()) {
            return;
        }
        final SpawnedBeacon beacon = optBeacon.get();
        beacon.setStatus(SpawnedBeaconStatus.EXPIRED);
        cleanupBeacon(beacon);
        lastBeaconEndedAtMillis = System.currentTimeMillis();
        Bukkit.getPluginManager().callEvent(new BeaconExpiredEvent(beacon));
        logger.info("Beacon " + beacon.shortId() + " expired and was removed.");
    }

    private void cleanupBeacon(SpawnedBeacon beacon) {
        // Remove beacon block
        final Location loc = beacon.getLocation();
        final World world = loc.getWorld();
        if (world != null && loc.getBlock().getType() == Material.BEACON) {
            loc.getBlock().setType(Material.AIR, false);
        }
        // Remove glowing Shulker entity
        removeGlowEntity(beacon, world);
        // Unprotect WG region
        if (areaProtection != null && beacon.getWgRegionId() != null) {
            areaProtection.unprotect(beacon.getWgRegionId(), world);
        }
        // Cancel EzCountdown timer
        if (countdownProvider != null && beacon.getCountdownId() != null) {
            countdownProvider.cancelCountdown(beacon.getCountdownId());
        }
    }

    private UUID spawnGlowEntity(Location location, World world) {
        try {
            final Shulker shulker = (Shulker) world.spawnEntity(
                    location.clone().add(0.5, 0, 0.5), EntityType.SHULKER);
            shulker.setInvisible(true);
            shulker.setGlowing(true);
            shulker.setSilent(true);
            shulker.setInvulnerable(true);
            shulker.setAI(false);
            shulker.setGravity(false);
            shulker.setCollidable(false);
            shulker.setCustomNameVisible(false);
            shulker.setPersistent(false);
            return shulker.getUniqueId();
        } catch (Exception exception) {
            logger.warning("Failed to spawn glow entity for beacon: " + exception.getMessage());
            return null;
        }
    }

    private void removeGlowEntity(SpawnedBeacon beacon, World world) {
        if (beacon.getGlowEntityId() == null || world == null) {
            return;
        }
        try {
            final org.bukkit.entity.Entity entity = world.getEntity(beacon.getGlowEntityId());
            if (entity != null) {
                entity.remove();
            }
        } catch (Exception exception) {
            logger.warning("Failed to remove glow entity for beacon " + beacon.shortId()
                    + ": " + exception.getMessage());
        }
    }

    private boolean isValidSpawnLocation(Location location) {
        final Material below = location.clone().add(0, -1, 0).getBlock().getType();
        final Material at = location.getBlock().getType();
        return below.isSolid() && (at.isAir() || !at.isSolid());
    }
}
