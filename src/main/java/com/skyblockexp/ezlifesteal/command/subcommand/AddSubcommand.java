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

public class AddSubcommand implements Subcommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args,
            LifestealCommand context) {
        if (!context.requirePermissionPublic(sender, "lifesteal.manage.modify", "lifesteal.admin")) {
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("Usage: /" + label + " add <player> <hearts>");
            return true;
        }
        final double addAmount;
        try {
            addAmount = Double.parseDouble(args[2]);
        }
        catch (NumberFormatException exception) {
            context.getPluginAccessorPublic().getMessageService().sendMessage(sender, "modify-invalid-amount");
            return true;
        }
        if (addAmount <= 0) {
            context.getPluginAccessorPublic().getMessageService().sendMessage(sender, "modify-invalid-amount");
            return true;
        }

        final PluginAccessor plugin = context.getPluginAccessorPublic();
        final LifestealManager addManager = plugin.getLifestealManager();
        final double finalAddAmount = addAmount;
        final String lookupTarget = args[1];

        context.getPlayerLookupServicePublic().lookupUniqueId(lookupTarget)
                .thenAcceptAsync(optionalUuid -> {
                    if (optionalUuid.isEmpty()) {
                        context.sendPlayerNotFoundPublic(sender, lookupTarget);
                        return;
                    }
                    final UUID uuid = optionalUuid.get();
                    final OfflinePlayer addTarget = Bukkit.getOfflinePlayer(uuid);
                    addManager.loadProfileAsync(uuid).whenComplete((profile, throwable) -> {
                        if (throwable != null) {
                            context.handleAsyncFailure(sender, throwable, "load profile for add command");
                            return;
                        }
                        final double before = profile.getHearts();
                        profile.addHearts(finalAddAmount, addManager.getMaxHearts());
                        final double after = profile.getHearts();
                        final double applied = Math.max(0.0, after - before);
                        addManager.saveProfileAsync(profile);
                        final Runnable uiTask = () -> {
                            if (addTarget.isOnline()) {
                                final Player p = addTarget.getPlayer();
                                if (p != null) {
                                    plugin.getMessageService().sendMessage(p, "receive-add-hearts", Map.of(
                                            "player", p.getName(),
                                            "amount", context.formatPublic(applied),
                                            "total", context.formatPublic(after)
                                    ));
                                }
                            }
                            plugin.getMessageService().sendMessage(sender, "add-hearts", Map.of(
                                    "player", context.resolvePlayerNamePublic(addTarget, lookupTarget),
                                    "amount", context.formatPublic(applied),
                                    "total", context.formatPublic(after)
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
