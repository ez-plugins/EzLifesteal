package com.skyblockexp.ezlifesteal.util.ban;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Platform-safe abstraction over Bukkit's ban list.
 *
 * <p>Use {@link #create()} to obtain the correct implementation at runtime:
 * {@link PaperBanAdapter} on Paper/Folia and {@link SpigotBanAdapter} on Spigot.</p>
 *
 * <p>Spigot limitation: the name-based fallback cannot reliably associate UUIDs with ban
 * entries, so {@link BanEntryView#getPlayerId()} may be {@code null} on Spigot.</p>
 */
public interface PlatformBanAdapter {

    /**
     * Adds a ban for the given player if one does not already exist.
     *
     * @param playerId   the player's UUID
     * @param playerName the player's current name
     * @param reason     ban reason, or {@code null}
     * @param source     ban source / issuer name, or {@code null}
     * @param expiry     ban expiry instant, or {@code null} for permanent
     */
    void addBan(UUID playerId, String playerName, String reason, String source, Instant expiry);

    /**
     * Removes any active ban for the given player.
     *
     * @param playerId   the player's UUID (used on Paper/Folia)
     * @param playerName the player's name (used on Spigot as fallback)
     */
    void removeBan(UUID playerId, String playerName);

    /**
     * Returns {@code true} when the player currently has an active ban.
     *
     * @param playerId   the player's UUID (may be {@code null} on Spigot)
     * @param playerName the player's name; used as fallback on Spigot
     */
    boolean isBanned(UUID playerId, String playerName);

    /**
     * Returns a snapshot of all active ban entries from Bukkit's ban list.
     */
    Set<BanEntryView> getBanEntries();

    /**
     * Factory method — picks the appropriate implementation based on class availability.
     *
     * <p>{@link PaperBanAdapter} is selected when {@code com.destroystokyo.paper.profile.PlayerProfile}
     * is on the classpath (Paper/Folia). {@link SpigotBanAdapter} is used otherwise.</p>
     */
    static PlatformBanAdapter create() {
        try {
            Class.forName("com.destroystokyo.paper.profile.PlayerProfile");
            return new PaperBanAdapter();
        } catch (ClassNotFoundException ignored) {
            return new SpigotBanAdapter();
        }
    }
}
