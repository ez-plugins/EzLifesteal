package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TestSubcommand implements Subcommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args,
            LifestealCommand context) {
        if (!context.requirePermissionPublic(sender, "lifesteal.test", "lifesteal.admin")) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("Usage: /" + label + " test <kill|death> ...");
            return true;
        }
        final String action = args[1].toLowerCase(Locale.ROOT);
        final PluginAccessor plugin = context.getPluginAccessorPublic();
        switch (action) {
            case "kill":
                if (args.length < 3) {
                    sender.sendMessage("Usage: /" + label + " test kill <killer> [victim]");
                    return true;
                }
                final Player killer = Bukkit.getPlayer(args[2]);
                if (killer == null) {
                    sender.sendMessage("Player not found: " + args[2]);
                    return true;
                }
                if (!plugin.isGlobalLifestealEnabled()) {
                    sender.sendMessage("Global lifesteal is currently disabled.");
                    return true;
                }
                if (!plugin.isLifestealEnabledInWorld(killer.getWorld().getName())) {
                    sender.sendMessage("Lifesteal is disabled in the killer's world.");
                    return true;
                }
                if (args.length >= 4) {
                    final Player victim = Bukkit.getPlayer(args[3]);
                    if (victim == null) {
                        sender.sendMessage("Player not found: " + args[3]);
                        return true;
                    }
                    if (killer.getUniqueId().equals(victim.getUniqueId())) {
                        sender.sendMessage("Killer and victim must be different players.");
                        return true;
                    }
                    if (!plugin.isLifestealEnabledInWorld(victim.getWorld().getName())) {
                        sender.sendMessage("Lifesteal is disabled in the victim's world.");
                        return true;
                    }
                    plugin.simulatePlayerDeath(victim, killer);
                    sender.sendMessage("Simulated kill: " + killer.getName() + " -> " + victim.getName());
                }
                else {
                    plugin.simulatePlayerKill(killer);
                    sender.sendMessage("Simulated kill rewards for " + killer.getName());
                }
                return true;
            case "death":
                if (args.length < 3) {
                    sender.sendMessage("Usage: /" + label + " test death <victim> [killer]");
                    return true;
                }
                final Player testVictim = Bukkit.getPlayer(args[2]);
                if (testVictim == null) {
                    sender.sendMessage("Player not found: " + args[2]);
                    return true;
                }
                Player testKiller = null;
                if (args.length >= 4) {
                    testKiller = Bukkit.getPlayer(args[3]);
                    if (testKiller == null) {
                        sender.sendMessage("Player not found: " + args[3]);
                        return true;
                    }
                    if (testKiller.getUniqueId().equals(testVictim.getUniqueId())) {
                        sender.sendMessage("Killer and victim must be different players.");
                        return true;
                    }
                    if (!plugin.isLifestealEnabledInWorld(testKiller.getWorld().getName())) {
                        sender.sendMessage("Lifesteal is disabled in the killer's world.");
                        return true;
                    }
                }
                if (!plugin.isGlobalLifestealEnabled()) {
                    sender.sendMessage("Global lifesteal is currently disabled.");
                    return true;
                }
                if (!plugin.isLifestealEnabledInWorld(testVictim.getWorld().getName())) {
                    sender.sendMessage("Lifesteal is disabled in the victim's world.");
                    return true;
                }
                plugin.simulatePlayerDeath(testVictim, testKiller);
                sender.sendMessage("Simulated death for " + testVictim.getName());
                return true;
            default:
                sender.sendMessage("Usage: /" + label + " test <kill|death> ...");
                return true;
        }
    }
}
