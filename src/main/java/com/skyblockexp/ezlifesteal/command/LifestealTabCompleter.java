package com.skyblockexp.ezlifesteal.command;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class LifestealTabCompleter implements TabCompleter {

    private final EzLifestealPlugin plugin;

    public LifestealTabCompleter(EzLifestealPlugin plugin) {
        this.plugin = plugin;
    }

    private List<String> filter(List<String> source, String prefix) {
        if (prefix == null) {
            prefix = "";
        }
        final String p = prefix.toLowerCase(Locale.ROOT);
        if (p.isEmpty()) {
            return source;
        }
        return source.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(p)).collect(Collectors.toList());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args == null) {
            return Collections.emptyList();
        }
        if (args.length == 0 || args.length == 1) {
            {
            }
            final String prefix = args.length == 0 ? "" : (args[0] == null ? "" : args[0].trim());
            final List<String> allowed = LifestealCommand.listAllowedSubcommands(sender);
            return filter(allowed, prefix);
        }

        final String sub = args[0] == null ? "" : args[0].toLowerCase(Locale.ROOT);
        // For subcommands that take a player as the next arg, suggest online player names
        if (args.length == 2) {
            final String prefix = args[1] == null ? "" : args[1];
            switch (sub) {
                case "hearts":
                case "set":
                case "add":
                case "remove":
                case "reset":
                case "revive":
                case "transfer":
                case "giveheart": {
                    final List<String> players =
                            Bukkit.getOnlinePlayers().stream().map(p -> p.getName()).collect(Collectors.toList());
                    return filter(players, prefix);
                }
                case "hologram": {
                    return filter(List.of("place", "remove"), prefix);
                }
                case "beacon": {
                    return filter(List.of("add", "remove", "list", "clear", "spawn", "despawn", "spawns"), prefix);
                }
                case "test": {
                    return filter(List.of("kill", "death"), prefix);
                }
                case "teambank": {
                    return filter(List.of("balance", "deposit", "withdraw"), prefix);
                }
                case "top": {
                    return filter(List.of("1", "2", "3", "4", "5"), prefix);
                }
                default:
                    return Collections.emptyList();
            }
        }

        // beacon: third arg is world (spawn) or short-id / "all" (despawn)
        if (sub.equals("beacon") && args.length == 3) {
            final String prefix = args[2] == null ? "" : args[2];
            if ("despawn".equalsIgnoreCase(args[1])) {
                final List<String> ids = new ArrayList<>();
                ids.add("all");
                final var accessor = plugin.getPluginAccessor();
                if (accessor != null) {
                    final var svc = accessor.getBeaconSpawnService();
                    if (svc != null) {
                        svc.getActiveBeacons().stream().map(b -> b.shortId()).forEach(ids::add);
                    }
                }
                return filter(ids, prefix);
            }
            if ("spawn".equalsIgnoreCase(args[1])) {
                return filter(
                        Bukkit.getWorlds().stream().map(w -> w.getName()).collect(Collectors.toList()),
                        prefix);
            }
        }

        // giveheart: third arg is heart id or tier
        if (sub.equals("giveheart") && args.length == 3) {
            final String prefix = args[2] == null ? "" : args[2];
            final var registry = plugin.getHeartRegistry();
            if (registry == null) {
                return Collections.emptyList();
            }
            final List<String> ids = new ArrayList<>(registry.getAll().keySet());
            // also suggest tier numbers
            final List<String> tiers = registry.getAll().values().stream().map(h -> Integer.toString(h.getTier()))
                    .distinct().collect(Collectors.toList());
            ids.addAll(tiers);
            return filter(ids, prefix);
        }

        if (sub.equals("teambank") && args.length == 3 && ("deposit".equalsIgnoreCase(args[1])
                || "withdraw".equalsIgnoreCase(args[1]))) {
            return filter(List.of("1", "2", "5", "10"), args[2] == null ? "" : args[2]);
        }

        return Collections.emptyList();
    }
}
