package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HologramSubcommand implements Subcommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args,
            LifestealCommand context) {
        if (args.length < 2) {
            sender.sendMessage("Usage: /" + label + " hologram <place|remove|cleanup>");
            return true;
        }
        final String hologramAction = args[1].toLowerCase(Locale.ROOT);
        final PluginAccessor plugin = context.getPluginAccessorPublic();
        switch (hologramAction) {
            case "place":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("This command can only be used by players.");
                    return true;
                }
                if (!context.requirePermissionPublic(player,
                        "lifesteal.scoreboard.place", "lifesteal.hologram", "lifesteal.admin")) {
                    return true;
                }
                final Location location = player.getLocation().clone().add(0.0, 2.0, 0.0);
                location.setX(Math.floor(location.getX()) + 0.5);
                location.setZ(Math.floor(location.getZ()) + 0.5);
                location.setYaw(player.getLocation().getYaw());
                final boolean placed = plugin.getTopHologramManager().place(location);
                if (placed) {
                    final Location base = plugin.getTopHologramManager().getLocation();
                    if (base != null && base.getWorld() != null) {
                        plugin.getMessageService().sendMessage(player, "hologram-placed", java.util.Map.of(
                                "world", base.getWorld().getName(),
                                "x", context.formatCoordinate(base.getX()),
                                "y", context.formatCoordinate(base.getY()),
                                "z", context.formatCoordinate(base.getZ())
                        ));
                    }
                    else {
                        plugin.getMessageService().sendMessage(player, "hologram-placed", java.util.Map.of(
                                "world", "unknown",
                                "x", context.formatCoordinate(location.getX()),
                                "y", context.formatCoordinate(location.getY()),
                                "z", context.formatCoordinate(location.getZ())
                        ));
                    }
                }
                else {
                    plugin.getMessageService().sendMessage(player, "hologram-place-failed");
                }
                return true;
            case "remove":
                if (!context.requirePermissionPublic(sender,
                        "lifesteal.scoreboard.remove", "lifesteal.hologram", "lifesteal.admin")) {
                    return true;
                }
                final boolean removed = plugin.getTopHologramManager().remove();
                if (removed) {
                    plugin.getMessageService().sendMessage(sender, "hologram-removed");
                }
                else {
                    plugin.getMessageService().sendMessage(sender, "hologram-not-found");
                }
                return true;
            case "cleanup":
                if (!(sender instanceof Player cleanupPlayer)) {
                    sender.sendMessage("This command can only be used by players.");
                    return true;
                }
                if (!context.requirePermissionPublic(cleanupPlayer,
                        "lifesteal.scoreboard.remove", "lifesteal.hologram", "lifesteal.admin")) {
                    return true;
                }
                double radius = 10.0;
                if (args.length >= 3) {
                    try {
                        radius = Double.parseDouble(args[2]);
                        radius = Math.max(1.0, Math.min(64.0, radius));
                    }
                    catch (NumberFormatException ignored) {
                        sender.sendMessage("Usage: /" + label + " hologram cleanup [radius]");
                        return true;
                    }
                }
                final int count = plugin.getTopHologramManager()
                        .removeNearbyOrphans(cleanupPlayer.getLocation(), radius);
                final java.util.Map<String, String> cleanupPlaceholders = java.util.Map.of(
                        "count", Integer.toString(count),
                        "radius", context.formatCoordinate(radius)
                );
                if (count > 0) {
                    plugin.getMessageService().sendMessage(cleanupPlayer, "hologram-cleanup-removed", cleanupPlaceholders);
                }
                else {
                    plugin.getMessageService().sendMessage(cleanupPlayer, "hologram-cleanup-none", cleanupPlaceholders);
                }
                return true;
            default:
                sender.sendMessage("Usage: /" + label + " hologram <place|remove|cleanup>");
                return true;
        }
    }
}
