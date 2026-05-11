package com.skyblockexp.ezlifesteal.command;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

/**
 * Standalone {@code /beacon} command that mirrors {@code /lifesteal beacon <...>}.
 *
 * <p>All execution is delegated to {@link LifestealCommand} with {@code "beacon"} prepended to
 * the argument array. Tab-completion at depth 1 restricts suggestions to beacon-specific
 * subcommands; deeper completions are forwarded to {@link LifestealTabCompleter}.</p>
 */
public class BeaconCommand implements CommandExecutor, TabCompleter {

    private static final List<String> BEACON_SUBCOMMANDS =
            List.of("add", "remove", "list", "clear", "spawn", "despawn", "spawns");

    private final LifestealCommand executor;
    private final LifestealTabCompleter tabCompleter;

    public BeaconCommand(LifestealCommand executor, LifestealTabCompleter tabCompleter) {
        this.executor = executor;
        this.tabCompleter = tabCompleter;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final String[] full = new String[args.length + 1];
        full[0] = "beacon";
        System.arraycopy(args, 0, full, 1, args.length);
        return executor.onCommand(sender, command, label, full);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args == null || args.length <= 1) {
            final String prefix = (args == null || args.length == 0 || args[0] == null) ? "" : args[0];
            return filter(BEACON_SUBCOMMANDS, prefix);
        }
        // Depth 2+: prepend "beacon" and delegate to the shared tab-completer
        final String[] full = new String[args.length + 1];
        full[0] = "beacon";
        System.arraycopy(args, 0, full, 1, args.length);
        return tabCompleter.onTabComplete(sender, command, alias, full);
    }

    private List<String> filter(List<String> source, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return source;
        }
        final String p = prefix.toLowerCase(Locale.ROOT);
        return source.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(p))
                .collect(Collectors.toList());
    }
}
