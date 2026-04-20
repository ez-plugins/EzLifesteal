package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public interface Subcommand {
    boolean execute(CommandSender sender, Command command, String label, String[] args, LifestealCommand context);
}
