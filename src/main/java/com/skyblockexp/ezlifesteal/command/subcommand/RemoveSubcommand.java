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

public class RemoveSubcommand implements Subcommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args,
            LifestealCommand context) {
        if (!context.requirePermissionPublic(sender, "lifesteal.manage.modify", "lifesteal.admin")) {
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("Usage: /" + label + " remove <player> <hearts>");
            return true;
        }
        final double removeAmount;
        try {
            removeAmount = Double.parseDouble(args[2]);
        }
        catch (NumberFormatException exception) {
            context.getPluginAccessorPublic().getMessageService().sendMessage(sender, "modify-invalid-amount");
            return true;
        }
        if (removeAmount <= 0) {
            context.getPluginAccessorPublic().getMessageService().sendMessage(sender, "modify-invalid-amount");
            return true;
        }

        final PluginAccessor plugin = context.getPluginAccessorPublic();
        final LifestealManager removeManager = plugin.getLifestealManager();
        final double finalRemoveAmount = removeAmount;
        final String lookupTarget = args[1];

        context.getPlayerLookupServicePublic().lookupUniqueId(lookupTarget)
                .thenAcceptAsync(optionalUuid -> {
                    if (optionalUuid.isEmpty()) {
                        context.sendPlayerNotFoundPublic(sender, lookupTarget);
                        return;
                    }
                    final UUID uuid = optionalUuid.get();
                    final OfflinePlayer removeTarget = Bukkit.getOfflinePlayer(uuid);
                    removeManager.loadProfileAsync(uuid).whenComplete((profile, throwable) -> {
                        if (throwable != null) {
                            context.handleAsyncFailure(sender, throwable, "load profile for remove command");
                            return;
                        }
                        final double before = profile.getHearts();
                        profile.removeHearts(finalRemoveAmount, removeManager.getMinHearts());
                        final double after = profile.getHearts();
                        final double removed = Math.max(0.0, before - after);
                        removeManager.saveProfileAsync(profile);
                        final Runnable uiTask = () -> {
                            if (removeTarget.isOnline()) {
                                final Player online = removeTarget.getPlayer();
                                if (online != null) {
                                    removeManager.applyHearts(online, profile);
                                    plugin.sendHeartStatus(online, profile.getHearts());
                                }
                            }
                            plugin.getMessageService().sendMessage(sender, "remove-hearts", Map.of(
                                    "player", context.resolvePlayerNamePublic(removeTarget, lookupTarget),
                                    "amount", context.formatPublic(removed),
                                    "remaining", context.formatPublic(after)
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
