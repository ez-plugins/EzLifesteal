package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.model.SpawnedBeacon;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.BeaconReviveService;
import com.skyblockexp.ezlifesteal.service.BeaconSpawnService;
import com.skyblockexp.ezlifesteal.service.ReviveAnimationService;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Admin subcommand used to manage revive beacon whitelist entries and plugin-spawned beacons.
 */
public class BeaconSubcommand implements Subcommand {

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args,
            LifestealCommand context) {
        if (!context.requirePermissionPublic(sender, "lifesteal.manage.modify", "lifesteal.admin")) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED
                    + "Usage: /" + label + " beacon <add|remove|list|clear|spawn|despawn|spawns>");
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
            case "spawn" -> handleSpawn(sender, args, plugin);
            case "despawn" -> handleDespawn(sender, args, plugin);
            case "spawns" -> handleSpawns(sender, plugin);
            default -> {
                sender.sendMessage(ChatColor.RED + "Unknown action. Use add, remove, list, clear, spawn, despawn, or spawns.");
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

    // -------------------------------------------------------------------------
    // Plugin-spawned beacon actions
    // -------------------------------------------------------------------------

    private boolean handleSpawn(CommandSender sender, String[] args, PluginAccessor plugin) {
        final BeaconSpawnService spawnService = plugin.getBeaconSpawnService();
        if (spawnService == null || !plugin.getBeaconSpawnSettings().enabled()) {
            sender.sendMessage(ChatColor.RED + "The beacon spawn feature is not enabled.");
            return true;
        }

        Location spawnLocation = null;
        if (args.length >= 6) {
            // /lifesteal beacon spawn <world> <x> <y> <z>
            final World world = Bukkit.getWorld(args[2]);
            if (world == null) {
                sender.sendMessage(ChatColor.RED + "World not found: " + args[2]);
                return true;
            }
            try {
                final int x = Integer.parseInt(args[3]);
                final int y = Integer.parseInt(args[4]);
                final int z = Integer.parseInt(args[5]);
                spawnLocation = new Location(world, x, y, z);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Invalid coordinates. Usage: beacon spawn <world> <x> <y> <z>");
                return true;
            }
        } else {
            // Random spawn
            final Optional<Location> randomLoc =
                    spawnService.findRandomSpawnLocation(plugin.getBeaconSpawnSettings());
            if (randomLoc.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Could not find a valid random spawn location. Configure random-spawn bounds in revive-beacon.yml.");
                return true;
            }
            spawnLocation = randomLoc.get();
        }

        final Optional<SpawnedBeacon> result = spawnService.spawnBeacon(spawnLocation, plugin);
        if (result.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Could not spawn beacon — max concurrent limit may be reached.");
            return true;
        }
        final SpawnedBeacon beacon = result.get();
        sender.sendMessage(ChatColor.GREEN + "Spawned beacon [" + beacon.shortId() + "] at "
                + format(beacon.getLocation()) + " (status=" + beacon.getStatus() + ")");
        return true;
    }

    private boolean handleDespawn(CommandSender sender, String[] args, PluginAccessor plugin) {
        final BeaconSpawnService spawnService = plugin.getBeaconSpawnService();
        if (spawnService == null) {
            sender.sendMessage(ChatColor.RED + "The beacon spawn feature is not active.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: beacon despawn <id|all>");
            return true;
        }
        final String target = args[2];
        if ("all".equalsIgnoreCase(target)) {
            spawnService.despawnAll(plugin);
            sender.sendMessage(ChatColor.GREEN + "Despawned all active beacons.");
            return true;
        }
        // Find by short id
        final Optional<SpawnedBeacon> match = spawnService.getActiveBeacons().stream()
                .filter(b -> b.shortId().equalsIgnoreCase(target) || b.getId().toString().equalsIgnoreCase(target))
                .findFirst();
        if (match.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No active beacon found with id: " + target);
            return true;
        }
        spawnService.despawnBeacon(match.get().getId(), plugin);
        sender.sendMessage(ChatColor.GREEN + "Despawned beacon [" + target + "].");
        return true;
    }

    private boolean handleSpawns(CommandSender sender, PluginAccessor plugin) {
        final BeaconSpawnService spawnService = plugin.getBeaconSpawnService();
        if (spawnService == null) {
            sender.sendMessage(ChatColor.RED + "The beacon spawn feature is not active.");
            return true;
        }
        final Collection<SpawnedBeacon> active = spawnService.getActiveBeacons();
        if (active.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No active spawned beacons.");
            return true;
        }
        sender.sendMessage(ChatColor.GOLD + "Active spawned beacons (" + active.size() + "):");
        for (SpawnedBeacon beacon : active) {
            sender.sendMessage(ChatColor.GRAY + "- [" + beacon.shortId() + "] "
                    + ChatColor.WHITE + format(beacon.getLocation())
                    + ChatColor.GRAY + " status=" + beacon.getStatus());
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

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
