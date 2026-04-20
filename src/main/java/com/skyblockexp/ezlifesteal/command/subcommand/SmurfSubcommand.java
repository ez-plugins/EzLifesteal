package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.gui.SmurfGuiManager;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SmurfSubcommand implements Subcommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args,
            LifestealCommand context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        if (!context.requirePermissionPublic(sender, "lifesteal.smurf.manage", "lifesteal.admin")) {
            return true;
        }
        final PluginAccessor plugin = context.getPluginAccessorPublic();
        if (plugin instanceof EzLifestealPlugin ez) {
            SmurfGuiManager.openManagement(ez, player);
        }
        return true;
    }
}
