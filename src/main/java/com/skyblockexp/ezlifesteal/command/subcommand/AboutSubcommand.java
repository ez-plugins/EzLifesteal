package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AboutSubcommand implements Subcommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args,
            LifestealCommand context) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        final PluginAccessor plugin = context.getPluginAccessorPublic();
        final String pluginName = plugin == null ? "EzLifesteal" : plugin.getPluginName();
        final String pluginVersion = plugin == null ? "unknown" : plugin.getPluginVersion();
        final String pluginAuthors = plugin == null ? "unknown" : plugin.getPluginAuthors();
        final MessageService messageService = plugin == null ? null : plugin.getMessageService();

        if (messageService != null && !messageService.getMessage("about-header").isBlank()) {
            messageService.sendMessage(sender, "about-header", Map.of("plugin", pluginName));
            messageService.sendMessage(sender, "about-version", Map.of("version", pluginVersion));
            messageService.sendMessage(sender, "about-authors", Map.of("authors", pluginAuthors));
            messageService.sendMessage(sender, "about-description");
            messageService.sendMessage(sender, "about-help-hint");
            return true;
        }

        sender
                .sendMessage(ChatColor.RED + "---------- " + ChatColor.DARK_RED + pluginName
                        + ChatColor.RED + " ----------");
        sender.sendMessage(ChatColor.GRAY + "Version: " + ChatColor.WHITE + pluginVersion);
        sender.sendMessage(ChatColor.GRAY + "Authors: " + ChatColor.WHITE + pluginAuthors);
        sender
                .sendMessage(ChatColor.GRAY + "Core gameplay: " + ChatColor.WHITE + "Lifesteal hearts with storage,"
                        + " admin tools, and integrations.");
        sender.sendMessage(ChatColor.GRAY + "Quick start: " + ChatColor.WHITE + "/lifesteal help");
        return true;
    }
}
