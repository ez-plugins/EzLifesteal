package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import java.util.UUID;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReviveSubcommand implements Subcommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args,
            LifestealCommand context) {
        if (!context.requirePermissionPublic(sender, "lifesteal.manage.modify", "lifesteal.admin")) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("Usage: /" + label + " revive <player>");
            return true;
        }
        final PluginAccessor plugin = context.getPluginAccessorPublic();
        final LifestealManager reviveManager = plugin.getLifestealManager();
        final double reviveHearts = reviveManager.getDefaultHearts();
        final String lookupTarget = args[1];

        context.getPlayerLookupServicePublic().lookupUniqueId(lookupTarget)
                .thenAcceptAsync(optionalUuid -> {
                    if (optionalUuid.isEmpty()) {
                        context.sendPlayerNotFoundPublic(sender, lookupTarget);
                        return;
                    }
                    final UUID uuid = optionalUuid.get();
                    final OfflinePlayer reviveTarget = Bukkit.getOfflinePlayer(uuid);
                    reviveManager.loadProfileAsync(uuid).whenComplete((profile, throwable) -> {
                        if (throwable != null) {
                            context.handleAsyncFailure(sender, throwable, "load profile for revive command");
                            return;
                        }
                        profile.setHearts(reviveHearts);
                        reviveManager.saveProfileAsync(profile);
                        final Runnable uiTask = () -> {
                            if (reviveTarget.isOnline()) {
                                final Player online = reviveTarget.getPlayer();
                                if (online != null) {
                                    reviveManager.applyHearts(online, profile);
                                    plugin.sendHeartStatus(online, profile.getHearts());
                                }
                            }
                            final String resolvedName = context.resolvePlayerNamePublic(reviveTarget, lookupTarget);
                            final com.destroystokyo.paper.profile.PlayerProfile banProfile =
                                    Bukkit.createProfile(reviveTarget.getUniqueId(), resolvedName);
                            final org.bukkit.BanList<com.destroystokyo.paper.profile.PlayerProfile> reviveBanList =
                                    Bukkit.getBanList(BanList.Type.PROFILE);
                            reviveBanList.pardon(banProfile);
                            if (plugin.getBanRepository() != null) {
                                try {
                                    plugin.getBanRepository().removeBan(reviveTarget.getUniqueId());
                                }
                                catch (StorageException exception) {
                                    plugin.getLogger().warning("Failed to remove persisted ban record for "
                                            + reviveTarget.getUniqueId() + ": " + exception.getMessage());
                                }
                            }
                            plugin.getMessageService().sendMessage(sender, "revive-success", java.util.Map.of(
                                    "player", context.resolvePlayerNamePublic(reviveTarget, lookupTarget),
                                    "hearts", context.formatPublic(reviveHearts)
                            ));
                            plugin.requestTopHologramUpdate();
                        };
                        context.getMainThreadExecutorPublic().execute(uiTask);
                    });
                }, context.getMainThreadExecutorPublic())
                .exceptionally(throwable -> {
                    context.getMainThreadExecutorPublic()
                            .execute(() -> context.handleLookupFailure(sender, lookupTarget, throwable));
                    return null;
                });
        return true;
    }
}
