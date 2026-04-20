package com.skyblockexp.ezlifesteal.storage;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class BanRecord {
    private final UUID uniqueId;

    private final String playerName;

    private final String reason;

    private final String source;

    private final Instant createdAt;

    private final Instant expiresAt;

    private final boolean active;

    public BanRecord(UUID uniqueId,
                     String playerName,
                     String reason,
                     String source,
                     Instant createdAt,
                     Instant expiresAt,
                     boolean active) {
        this.uniqueId = Objects.requireNonNull(uniqueId, "uniqueId");
        this.playerName = playerName;
        this.reason = reason;
        this.source = source;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.expiresAt = expiresAt;
        this.active = active;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getReason() {
        return reason;
    }

    public String getSource() {
        return source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isActive() {
        return active;
    }
}
