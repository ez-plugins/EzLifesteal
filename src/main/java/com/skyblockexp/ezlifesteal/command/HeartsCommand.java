package com.skyblockexp.ezlifesteal.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class HeartsCommand implements CommandExecutor {

    private final LifestealCommand lifestealCommand;

    public HeartsCommand(LifestealCommand lifestealCommand) {
        this.lifestealCommand = lifestealCommand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        final String[] delegatedArgs = new String[(args == null ? 0 : args.length) + 1];
        delegatedArgs[0] = "hearts";
        if (args != null && args.length > 0) {
            System.arraycopy(args, 0, delegatedArgs, 1, args.length);
        }
        return lifestealCommand.onCommand(sender, command, label, delegatedArgs);
    }
}
