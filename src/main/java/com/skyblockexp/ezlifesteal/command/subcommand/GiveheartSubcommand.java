package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.heart.Heart;
import com.skyblockexp.ezlifesteal.heart.HeartRegistry;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GiveheartSubcommand implements Subcommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args,
            LifestealCommand context) {
        if (!context.requirePermissionPublic(sender, "lifesteal.manage.modify", "lifesteal.admin")) {
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("Usage: /" + label + " giveheart <player> <heartId|tier> [amount]");
            return true;
        }
        final String lookupTarget = args[1];
        final String heartArg = args[2];
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            }
            catch (NumberFormatException ex) {
                context.getPluginAccessorPublic().getMessageService().sendMessage(sender, "transfer-invalid-amount");
                return true;
            }
        }
        if (amount <= 0) {
            context.getPluginAccessorPublic().getMessageService().sendMessage(sender, "transfer-invalid-amount");
            return true;
        }

        final PluginAccessor plugin = context.getPluginAccessorPublic();
        final HeartRegistry registry = plugin.getHeartRegistry();

        final String lookupTargetFinal = lookupTarget;
        final String heartArgFinal = heartArg;
        final int amountFinal = amount;
        final PluginAccessor pluginFinal = plugin;
        final HeartRegistry registryFinal = registry;

        context.getPlayerLookupServicePublic().lookupUniqueId(lookupTargetFinal)
                .thenAcceptAsync(optionalUuid -> {
                    if (optionalUuid.isEmpty()) {
                        context.sendPlayerNotFoundPublic(sender, lookupTargetFinal);
                        return;
                    }
                    final UUID targetId = optionalUuid.get();
                    final OfflinePlayer offline = Bukkit.getOfflinePlayer(targetId);
                    Heart heart = registryFinal.getById(heartArgFinal);
                    if (heart == null) {
                        try {
                            final int tier = Integer.parseInt(heartArgFinal);
                            heart = registryFinal.getByTier(tier);
                        }
                        catch (NumberFormatException ignored) {
                        }
                    }
                    if (heart == null) {
                        pluginFinal.getMessageService().sendMessage(sender, "heart-not-found");
                        return;
                    }

                    if (offline.isOnline()) {
                        final Player target = offline.getPlayer();
                        if (target != null) {
                            for (int i = 0; i < amountFinal; i++) {
                                target.getInventory().addItem(heart.createItemStack());
                            }
                            pluginFinal.getMessageService().sendMessage(target, "giveheart-received", java.util.Map.of(
                                    "player", target.getName(),
                                    "amount", Integer.toString(amountFinal),
                                    "heart", heart.getId()
                            ));
                        }
                    }
                    else {
                        pluginFinal.getMessageService().sendMessage(sender, "player-not-online");
                        return;
                    }

                    final String targetName = offline.getName() != null ? offline.getName() : lookupTargetFinal;
                    pluginFinal.getMessageService().sendMessage(sender, "giveheart-success", java.util.Map.of(
                            "player", targetName,
                            "amount", Integer.toString(amountFinal),
                            "heart", heart.getId()
                    ));
                }, context.getMainThreadExecutorPublic())
                .exceptionally(throwable -> {
                    context.getMainThreadExecutorPublic()
                            .execute(() -> context.handleLookupFailure(sender, lookupTarget, throwable));
                    return null;
                });

        return true;
    }
}
