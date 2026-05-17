package com.skyblockexp.ezlifesteal.util.ban;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;

/**
 * Ban adapter that uses Spigot's {@link BanList.Type#NAME} (string-based) ban list.
 *
 * <p>This implementation is used on Spigot where the Paper profile API is absent.
 * Because name-based bans do not store UUIDs, {@link BanEntryView#getPlayerId()}
 * returns {@code null} for all entries returned by {@link #getBanEntries()}.
 * Callers that require UUIDs should handle this gracefully.</p>
 */
final class SpigotBanAdapter implements PlatformBanAdapter {

    @Override
    @SuppressWarnings("rawtypes")
    public void addBan(UUID playerId, String playerName, String reason, String source, Instant expiry) {
        if (playerName == null || playerName.isBlank()) {
            return;
        }
        final BanList raw = Bukkit.getBanList(BanList.Type.NAME);
        if (!raw.isBanned(playerName)) {
            raw.addBan(playerName, reason, expiry == null ? null : Date.from(expiry), source);
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void removeBan(UUID playerId, String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return;
        }
        final BanList raw = Bukkit.getBanList(BanList.Type.NAME);
        raw.pardon(playerName);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isBanned(UUID playerId, String playerName) {
        if (playerName != null && !playerName.isBlank()) {
            final BanList raw = Bukkit.getBanList(BanList.Type.NAME);
            return raw.isBanned(playerName);
        }
        return false;
    }

    @Override
    public Set<BanEntryView> getBanEntries() {
        final BanList<String> banList = Bukkit.getBanList(BanList.Type.NAME);
        final Set<BanEntryView> views = new LinkedHashSet<>();
        for (BanEntry<String> entry : banList.getBanEntries()) {
            final String name = entry.getBanTarget();
            if (name == null || name.isBlank()) {
                continue;
            }
            views.add(new BanEntryView(
                    null,
                    name,
                    entry.getReason(),
                    entry.getSource(),
                    entry.getCreated(),
                    entry.getExpiration()
            ));
        }
        return views;
    }
}
