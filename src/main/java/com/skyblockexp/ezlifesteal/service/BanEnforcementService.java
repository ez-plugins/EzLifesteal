package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.storage.BanRecord;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import com.skyblockexp.ezlifesteal.storage.repository.BanRepository;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import java.time.Instant;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Applies bans at runtime and persists ban metadata to storage.
 */
public class BanEnforcementService {
    private final PluginAccessor plugin;

    public BanEnforcementService(PluginAccessor plugin) {
        this.plugin = plugin;
    }

    public void applyBanWithStorage(Player player, String banReason, String kickReason) {
        final Instant now = Instant.now();
        final BanRepository banRepository = plugin.getBanRepository();
        if (banRepository != null) {
            final BanRecord record = new BanRecord(
                    player.getUniqueId(),
                    player.getName(),
                    banReason,
                    plugin.getPluginName(),
                    now,
                    null,
                    true
            );
            try {
                banRepository.saveBan(record);
            }
            catch (StorageException exception) {
                plugin.getLogger().warning("Failed to persist ban record for "
                        + player.getUniqueId() + ": " + exception.getMessage());
            }
        }

        SchedulerAdapter.run(plugin.getPlugin(), () -> {
            final BanList banList = Bukkit.getBanList(BanList.Type.NAME);
            final String playerName = player.getName();
            if (playerName != null && !banList.isBanned(playerName)) {
                banList.addBan(playerName, banReason, (Instant) null, plugin.getPluginName());
            }
            if (player.isOnline()) {
                player.kickPlayer(kickReason);
            }
        });
    }
}
