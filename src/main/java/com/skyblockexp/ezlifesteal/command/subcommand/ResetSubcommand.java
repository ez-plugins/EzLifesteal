package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ResetSubcommand implements Subcommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args,
            LifestealCommand context) {
        if (!context.requirePermissionPublic(sender, "lifesteal.manage.modify", "lifesteal.admin")) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("Usage: /" + label + " reset <player>");
            return true;
        }
        final PluginAccessor plugin = context.getPluginAccessorPublic();
        final LifestealManager resetManager = plugin.getLifestealManager();
        final double resetHearts = resetManager.getDefaultHearts();
        final String lookupTarget = args[1];

        context.getPlayerLookupServicePublic().lookupUniqueId(lookupTarget)
                .thenAcceptAsync(optionalUuid -> {
                    if (optionalUuid.isEmpty()) {
                        context.sendPlayerNotFoundPublic(sender, lookupTarget);
                        return;
                    }
                    final UUID uuid = optionalUuid.get();
                    final OfflinePlayer resetTarget = Bukkit.getOfflinePlayer(uuid);
                    resetManager.loadProfileAsync(uuid).whenComplete((profile, throwable) -> {
                        if (throwable != null) {
                            context.handleAsyncFailure(sender, throwable, "load profile for reset command");
                            return;
                        }
                        profile.setHearts(resetHearts);
                        resetManager.saveProfileAsync(profile);
                        final Runnable uiTask = () -> {
                            if (resetTarget.isOnline()) {
                                final Player online = resetTarget.getPlayer();
                                if (online != null) {
                                    resetManager.applyHearts(online, profile);
                                    plugin.sendHeartStatus(online, profile.getHearts());
                                }
                            }
                            plugin.getMessageService().sendMessage(sender, "reset-hearts", java.util.Map.of(
                                    "player", context.resolvePlayerNamePublic(resetTarget, lookupTarget),
                                    "hearts", context.formatPublic(resetHearts)
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
