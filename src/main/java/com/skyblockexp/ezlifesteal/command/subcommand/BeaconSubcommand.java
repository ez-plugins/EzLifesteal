package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.BeaconReviveService;
import com.skyblockexp.ezlifesteal.service.ReviveAnimationService;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Admin subcommand used to manage revive beacon whitelist entries.
 */
public class BeaconSubcommand implements Subcommand {

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args,
            LifestealCommand context) {
        if (!context.requirePermissionPublic(sender, "lifesteal.manage.modify", "lifesteal.admin")) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " beacon <add|remove|list|clear>");
            return true;
        }

        final PluginAccessor plugin = context.getPluginAccessorPublic();
        final BeaconReviveService beaconReviveService = new BeaconReviveService(plugin,
                new ReviveAnimationService(plugin));
        final String action = args[1].toLowerCase();

        return switch (action) {
            case "add" -> handleAdd(sender, beaconReviveService);
            case "remove" -> handleRemove(sender, beaconReviveService);
            case "list" -> handleList(sender, beaconReviveService, plugin);
            case "clear" -> handleClear(sender, beaconReviveService);
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown action. Use add, remove, list, or clear.");
                yield true;
            }
        };
    }

    private boolean handleAdd(CommandSender sender, BeaconReviveService beaconReviveService) {
        final Location location = resolveBeaconLocation(sender);
        if (location == null) {
            sender.sendMessage(ChatColor.RED + "Look at a beacon block to add it to the whitelist.");
            return true;
        }
        if (!beaconReviveService.whitelistBeacon(location)) {
            sender.sendMessage(ChatColor.YELLOW + "That beacon is already whitelisted.");
            return true;
        }
        beaconReviveService.saveWhitelist();
        sender.sendMessage(ChatColor.GREEN + "Whitelisted beacon at " + format(location));
        return true;
    }

    private boolean handleRemove(CommandSender sender, BeaconReviveService beaconReviveService) {
        final Location location = resolveBeaconLocation(sender);
        if (location == null) {
            sender.sendMessage(ChatColor.RED + "Look at a beacon block to remove it from the whitelist.");
            return true;
        }
        if (!beaconReviveService.removeWhitelistedBeacon(location)) {
            sender.sendMessage(ChatColor.YELLOW + "That beacon is not whitelisted.");
            return true;
        }
        beaconReviveService.saveWhitelist();
        sender.sendMessage(ChatColor.GREEN + "Removed beacon whitelist entry at " + format(location));
        return true;
    }

    private boolean handleList(CommandSender sender, BeaconReviveService beaconReviveService, PluginAccessor plugin) {
        final List<String> beacons = beaconReviveService.listWhitelistedBeacons();
        sender
                .sendMessage(ChatColor.GOLD + "Revive beacon whitelist (enabled="
                        + plugin.isReviveBeaconWhitelistEnabled() + ")");
        sender.sendMessage(ChatColor.GOLD + "Voucher-in-beacon=" + plugin.isReviveBeaconRequireVoucherInBeacon()
                + ", hold-seconds=" + plugin.getReviveBeaconVoucherHoldSeconds());
        if (beacons.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "- none");
            return true;
        }
        for (String beacon : beacons) {
            sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + beacon);
        }
        return true;
    }

    private boolean handleClear(CommandSender sender, BeaconReviveService beaconReviveService) {
        beaconReviveService.clearWhitelistedBeacons();
        beaconReviveService.saveWhitelist();
        sender.sendMessage(ChatColor.GREEN + "Cleared revive beacon whitelist.");
        return true;
    }

    private Location resolveBeaconLocation(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return null;
        }
        final Block target = player.getTargetBlockExact(6);
        if (target == null || target.getType() != Material.BEACON) {
            return null;
        }
        return target.getLocation();
    }

    private String format(Location location) {
        return location.getWorld().getName() + " " + location.getBlockX() + " " + location.getBlockY() + " "
                + location.getBlockZ();
    }
}
