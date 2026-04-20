package com.skyblockexp.ezlifesteal.command;

import com.skyblockexp.ezlifesteal.service.BeaconReviveService;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReviveCommand implements CommandExecutor {

    private final BeaconReviveService beaconReviveService;

    public ReviveCommand(BeaconReviveService beaconReviveService) {
        this.beaconReviveService = beaconReviveService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
        if (args.length != 1) {
            beaconReviveService.sendReviveUsageInstructions(player);
            return true;
        }

        final String targetName = args[0];
        if (targetName == null || targetName.isBlank()) {
            beaconReviveService.sendReviveUsageInstructions(player);
            return true;
        }

        beaconReviveService.selectReviveTarget(player, targetName.trim());
        return true;
    }
}
