package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetSubcommand implements Subcommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args,
            LifestealCommand context) {
        if (!context.requirePermissionPublic(sender, "lifesteal.manage.modify", "lifesteal.admin")) {
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("Usage: /" + label + " set <player> <hearts>");
            return true;
        }
        double hearts;
        try {
            hearts = Double.parseDouble(args[2]);
        }
        catch (NumberFormatException exception) {
            context.getPluginAccessorPublic().getMessageService().sendMessage(sender, "set-invalid-amount");
            return true;
        }
        final PluginAccessor plugin = context.getPluginAccessorPublic();
        final LifestealManager setManager = plugin.getLifestealManager();
        hearts = Math.max(setManager.getMinHearts(), Math.min(setManager.getMaxHearts(), hearts));
        final double finalHearts = hearts;
        final String lookupTarget = args[1];

        context.getPlayerLookupServicePublic().lookupUniqueId(lookupTarget)
                .thenAcceptAsync(optionalUuid -> {
                    if (optionalUuid.isEmpty()) {
                        context.sendPlayerNotFoundPublic(sender, lookupTarget);
                        return;
                    }
                    final UUID uuid = optionalUuid.get();
                    final OfflinePlayer setTarget = Bukkit.getOfflinePlayer(uuid);
                    setManager.loadProfileAsync(uuid).whenComplete((profileToUpdate, throwable) -> {
                        if (throwable != null) {
                            plugin.getLogger()
                                    .severe("Failed to load profile for set command: " + throwable.getMessage());
                            context.getMainThreadExecutorPublic()
                                    .execute(() -> plugin.getMessageService().sendMessage(sender, "storage-error"));
                            return;
                        }
                        profileToUpdate.setHearts(finalHearts);
                        setManager.saveProfileAsync(profileToUpdate);
                        final Runnable uiTask = () -> {
                            if (setTarget.isOnline()) {
                                final Player online = setTarget.getPlayer();
                                if (online != null) {
                                    setManager.applyHearts(online, profileToUpdate);
                                    plugin.sendHeartStatus(online, profileToUpdate.getHearts());
                                }
                            }
                            plugin.getMessageService().sendMessage(sender, "set-hearts", Map.of(
                                    "player", context.resolvePlayerNamePublic(setTarget, lookupTarget),
                                    "hearts", context.formatPublic(finalHearts)
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
