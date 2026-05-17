package com.skyblockexp.ezlifesteal.util.ban;

import com.destroystokyo.paper.profile.PlayerProfile;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;

/**
 * Ban adapter that uses Paper's {@code com.destroystokyo.paper.profile.PlayerProfile}
 * and {@link BanList.Type#PROFILE}.
 *
 * <p>This class is only loaded on Paper / Folia where the Paper profile API is present.
 * It must never be referenced directly from code that runs on Spigot; access it via
 * {@link PlatformBanAdapter#create()} only.</p>
 */
final class PaperBanAdapter implements PlatformBanAdapter {

    @Override
    public void addBan(UUID playerId, String playerName, String reason, String source, Instant expiry) {
        final BanList<PlayerProfile> banList = Bukkit.getBanList(BanList.Type.PROFILE);
        final PlayerProfile profile = Bukkit.createProfile(playerId, playerName);
        if (!banList.isBanned(profile)) {
            banList.addBan(profile, reason, expiry, source);
        }
    }

    @Override
    public void removeBan(UUID playerId, String playerName) {
        final PlayerProfile profile = Bukkit.createProfile(playerId, playerName);
        final BanList<PlayerProfile> banList = Bukkit.getBanList(BanList.Type.PROFILE);
        banList.pardon(profile);
    }

    @Override
    public boolean isBanned(UUID playerId, String playerName) {
        final PlayerProfile profile = Bukkit.createProfile(playerId, playerName);
        final BanList<PlayerProfile> banList = Bukkit.getBanList(BanList.Type.PROFILE);
        return banList.isBanned(profile);
    }

    @Override
    public Set<BanEntryView> getBanEntries() {
        final BanList<PlayerProfile> banList = Bukkit.getBanList(BanList.Type.PROFILE);
        final Set<BanEntryView> views = new LinkedHashSet<>();
        for (BanEntry<PlayerProfile> entry : banList.getBanEntries()) {
            final PlayerProfile profile = entry.getBanTarget();
            if (profile == null) {
                continue;
            }
            views.add(new BanEntryView(
                    profile.getId(),
                    profile.getName(),
                    entry.getReason(),
                    entry.getSource(),
                    entry.getCreated(),
                    entry.getExpiration()
            ));
        }
        return views;
    }
}
