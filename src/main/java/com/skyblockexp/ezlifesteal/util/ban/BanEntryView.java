package com.skyblockexp.ezlifesteal.util.ban;

import java.util.Date;
import java.util.UUID;

/**
 * Platform-neutral view of a single ban entry from Bukkit's ban list.
 *
 * <p>On Paper/Folia, {@link #getPlayerId()} is always populated.
 * On Spigot (name-based banning), it may be {@code null}; callers must null-check.</p>
 */
public final class BanEntryView {

    private final UUID playerId;

    private final String playerName;

    private final String reason;

    private final String source;

    private final Date created;

    private final Date expiration;

    public BanEntryView(UUID playerId, String playerName, String reason, String source,
            Date created, Date expiration) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.reason = reason;
        this.source = source;
        this.created = created;
        this.expiration = expiration;
    }

    /** UUID of the banned player, or {@code null} when using Spigot name-based banning. */
    public UUID getPlayerId() {
        return playerId;
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

    public Date getCreated() {
        return created;
    }

    public Date getExpiration() {
        return expiration;
    }
}
