package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class ReloadSubcommand implements Subcommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args,
            LifestealCommand context) {
        if (!context.requirePermissionPublic(sender, "lifesteal.reload", "lifesteal.admin")) {
            return true;
        }
        final PluginAccessor plugin = context.getPluginAccessorPublic();
        if (plugin != null) {
            plugin.reloadPlugin(sender);
        }
        return true;
    }
}
