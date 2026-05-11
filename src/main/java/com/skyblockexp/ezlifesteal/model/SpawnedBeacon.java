package com.skyblockexp.ezlifesteal.model;

import java.util.UUID;
import org.bukkit.Location;

/**
 * Represents a beacon that was physically spawned in the world by the plugin.
 *
 * <p>One instance is created per spawn event and its lifecycle follows
 * {@link SpawnedBeaconStatus}: COUNTDOWN → AVAILABLE → USED/EXPIRED.</p>
 */
public final class SpawnedBeacon {

    private final UUID id;

    private final Location location;

    /** WorldGuard region identifier created for this beacon, or {@code null} when WG is absent. */
    private final String wgRegionId;

    /** EzCountdown countdown identifier, or {@code null} when EzCountdown is absent. */
    private final String countdownId;

    private final long spawnedAtMillis;

    private SpawnedBeaconStatus status;

    /** Epoch-millisecond timestamp when the beacon should be removed, or 0 for "never expire". */
    private long expiresAtMillis;

    /** UUID of the invisible glowing Shulker entity placed on the beacon block for visual effect. */
    private UUID glowEntityId;

    public SpawnedBeacon(
            UUID id,
            Location location,
            String wgRegionId,
            String countdownId,
            long spawnedAtMillis,
            SpawnedBeaconStatus status,
            long expiresAtMillis
    ) {
        this.id = id;
        this.location = location;
        this.wgRegionId = wgRegionId;
        this.countdownId = countdownId;
        this.spawnedAtMillis = spawnedAtMillis;
        this.status = status;
        this.expiresAtMillis = expiresAtMillis;
    }

    public UUID getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public String getWgRegionId() {
        return wgRegionId;
    }

    public String getCountdownId() {
        return countdownId;
    }

    public long getSpawnedAtMillis() {
        return spawnedAtMillis;
    }

    public SpawnedBeaconStatus getStatus() {
        return status;
    }

    public void setStatus(SpawnedBeaconStatus status) {
        this.status = status;
    }

    public long getExpiresAtMillis() {
        return expiresAtMillis;
    }

    public void setExpiresAtMillis(long expiresAtMillis) {
        this.expiresAtMillis = expiresAtMillis;
    }

    public UUID getGlowEntityId() {
        return glowEntityId;
    }

    public void setGlowEntityId(UUID glowEntityId) {
        this.glowEntityId = glowEntityId;
    }

    /** Returns {@code true} if the beacon has already passed its expiry time. */
    public boolean isExpired() {
        return expiresAtMillis > 0 && System.currentTimeMillis() > expiresAtMillis;
    }

    /** Short hex prefix of the UUID, used in WG region and countdown IDs. */
    public String shortId() {
        return id.toString().substring(0, 8);
    }
}
