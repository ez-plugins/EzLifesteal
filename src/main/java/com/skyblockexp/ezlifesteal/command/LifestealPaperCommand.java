package com.skyblockexp.ezlifesteal.command;

import java.util.List;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;

public class LifestealPaperCommand extends Command implements PluginIdentifiableCommand {

    private final Plugin plugin;

    private final LifestealCommand executor;

    private final org.bukkit.command.TabCompleter tabCompleter;


    public LifestealPaperCommand(Plugin plugin, LifestealCommand executor,
            org.bukkit.command.TabCompleter tabCompleter) {
        super("lifesteal");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.tabCompleter = tabCompleter;
        setDescription("Main command for EzLifesteal");
        setUsage("/lifesteal"
                + " <hearts|set|add|remove|reset|revive|resetall|transfer|withdraw|top|smurf|reload|test|hologram>");
        setPermission("lifesteal.command.base");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!testPermission(sender)) {
            return true;
        }
        return executor.onCommand(sender, this, commandLabel, args);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (tabCompleter != null) {
            final List<String> completions = tabCompleter.onTabComplete(sender, this, alias, args);
            if (completions != null) {
                return completions;
            }
        }
        return super.tabComplete(sender, alias, args);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location) {
        if (tabCompleter != null) {
            final List<String> completions = tabCompleter.onTabComplete(sender, this, alias, args);
            if (completions != null) {
                return completions;
            }
        }
        return super.tabComplete(sender, alias, args, location);
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }
}
