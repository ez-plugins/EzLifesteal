package com.skyblockexp.ezlifesteal.storage;

import com.skyblockexp.ezlifesteal.model.SpawnedBeacon;
import com.skyblockexp.ezlifesteal.model.SpawnedBeaconStatus;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.bukkit.Location;

/**
 * In-memory repository for {@link SpawnedBeacon} instances.
 *
 * <p>All mutations are thread-safe via a {@link ConcurrentHashMap}.</p>
 */
public final class SpawnedBeaconRepository {

    private final ConcurrentHashMap<UUID, SpawnedBeacon> store = new ConcurrentHashMap<>();

    /** Adds a newly spawned beacon. */
    public void add(SpawnedBeacon beacon) {
        store.put(beacon.getId(), beacon);
    }

    /** Removes a beacon by id, returning the removed instance (or empty if absent). */
    public Optional<SpawnedBeacon> remove(UUID id) {
        return Optional.ofNullable(store.remove(id));
    }

    /** Finds a beacon by its unique id. */
    public Optional<SpawnedBeacon> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    /**
     * Finds a beacon whose block location matches the given location.
     *
     * <p>Comparison is block-level (integer coordinates + world name).</p>
     */
    public Optional<SpawnedBeacon> findByLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        final String worldName = location.getWorld().getName();
        final int bx = location.getBlockX();
        final int by = location.getBlockY();
        final int bz = location.getBlockZ();

        return store.values().stream()
                .filter(b -> {
                    final Location bl = b.getLocation();
                    return bl.getWorld() != null
                            && bl.getWorld().getName().equals(worldName)
                            && bl.getBlockX() == bx
                            && bl.getBlockY() == by
                            && bl.getBlockZ() == bz;
                })
                .findFirst();
    }

    /** Returns an immutable snapshot of all stored beacons. */
    public Collection<SpawnedBeacon> getAll() {
        return Collections.unmodifiableCollection(store.values());
    }

    /** Returns only beacons that currently have a given status. */
    public Collection<SpawnedBeacon> getAllWithStatus(SpawnedBeaconStatus status) {
        return store.values().stream()
                .filter(b -> b.getStatus() == status)
                .collect(Collectors.toUnmodifiableList());
    }

    /** Returns the total number of stored beacons regardless of status. */
    public int size() {
        return store.size();
    }

    /** Removes all beacons — used on plugin shutdown. */
    public void clear() {
        store.clear();
    }
}
