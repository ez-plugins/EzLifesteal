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

public class HeartsSubcommand implements Subcommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args,
            LifestealCommand context) {
        // If no player target provided and sender is a player, show self hearts.
        if (args.length < 2) {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage("Usage: /" + label + " hearts <player>");
                return true;
            }
            if (!context.requirePermissionPublic(sender, "lifesteal.manage.view", "lifesteal.admin")) {
                return true;
            }
            final PluginAccessor plugin = context.getPluginAccessorPublic();
            final LifestealManager manager = plugin.getLifestealManager();
            manager.getLoadedProfile(player.getUniqueId()).ifPresentOrElse(profile -> {
                plugin.getMessageService().sendMessage(sender, "self-hearts",
                        Map.of("hearts", context.formatPublic(profile.getHearts())));
            }, () -> plugin.getMessageService().sendMessage(sender, "self-hearts",
                    Map.of("hearts", context.formatPublic(manager.getDefaultHearts()))));
            return true;
        }

        if (!context.requirePermissionPublic(sender, "lifesteal.manage.view", "lifesteal.admin")) {
            return true;
        }
        final PluginAccessor plugin = context.getPluginAccessorPublic();
        final LifestealManager manager = plugin.getLifestealManager();
        final String lookupTarget = args[1];

        context.getPlayerLookupServicePublic().lookupUniqueId(lookupTarget)
                .thenAcceptAsync(optionalUuid -> {
                    if (optionalUuid.isEmpty()) {
                        context.sendPlayerNotFoundPublic(sender, lookupTarget);
                        return;
                    }
                    final UUID uuid = optionalUuid.get();
                    final OfflinePlayer target = Bukkit.getOfflinePlayer(uuid);
                    manager.getLoadedProfile(uuid).ifPresentOrElse(profile ->
                            context.sendHeartMessage(sender, target, profile.getHearts()), () ->
                            manager.loadProfileAsync(uuid).whenComplete((profile, throwable) -> {
                                if (throwable != null) {
                                    plugin.getLogger()
                                            .severe("Failed to load profile for command: " + throwable.getMessage());
                                    context.getMainThreadExecutorPublic()
                                            .execute(() -> plugin.getMessageService()
                                                    .sendMessage(sender, "storage-error"));
                                    return;
                                }
                                context.getMainThreadExecutorPublic()
                                        .execute(() -> context.sendHeartMessage(sender, target, profile.getHearts()));
                            })
                    );
                }, context.getMainThreadExecutorPublic())
                .exceptionally(throwable -> {
                    context.getMainThreadExecutorPublic()
                            .execute(() -> context.handleLookupFailure(sender, lookupTarget, throwable));
                    return null;
                });
        return true;
    }
}
