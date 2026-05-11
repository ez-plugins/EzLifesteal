package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class HelpSubcommand implements Subcommand {

    private static final int PAGE_SIZE = 6;

    private static final Map<String, String> FALLBACK_DESCRIPTIONS = createFallbackDescriptions();

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args,
            LifestealCommand context) {
        final List<String> allowedSubcommands = LifestealCommand.listAllowedSubcommands(sender);
        final int requestedPage = parseRequestedPage(sender, args, context);
        if (requestedPage < 0) {
            return true;
        }

        final PluginAccessor plugin = context.getPluginAccessorPublic();
        final MessageService messageService = plugin == null ? null : plugin.getMessageService();
        final int totalPages = Math.max(1, (int) Math.ceil(allowedSubcommands.size() / (double) PAGE_SIZE));
        if (requestedPage > totalPages) {
            sendNoPage(sender, messageService, requestedPage, totalPages);
            return true;
        }

        final int startIndex = (requestedPage - 1) * PAGE_SIZE;
        final int endIndex = Math.min(startIndex + PAGE_SIZE, allowedSubcommands.size());

        sendHeader(sender, messageService, requestedPage, totalPages);
        for (int index = startIndex; index < endIndex; index++) {
            final String subcommandName = allowedSubcommands.get(index);
            final String description = resolveDescription(subcommandName, messageService);
            sender.sendMessage(ChatColor.GRAY + "/lifesteal " + ChatColor.WHITE + subcommandName
                    + ChatColor.DARK_GRAY + " - " + ChatColor.GRAY + description);
        }
        sendFooter(sender, messageService, requestedPage, totalPages);
        return true;
    }

    private int parseRequestedPage(CommandSender sender, String[] args, LifestealCommand context) {
        if (args == null || args.length < 2) {
            return 1;
        }
        try {
            final int page = Integer.parseInt(args[1]);
            if (page > 0) {
                return page;
            }
        }
        catch (NumberFormatException ignored) {
        }

        final PluginAccessor plugin = context.getPluginAccessorPublic();
        final MessageService messageService = plugin == null ? null : plugin.getMessageService();
        if (messageService != null && !messageService.getMessage("help-invalid-page").isBlank()) {
            messageService.sendMessage(sender, "help-invalid-page");
        }
        else {
            sender.sendMessage(ChatColor.RED + "Please provide a valid positive page number.");
        }
        return -1;
    }

    private void sendHeader(CommandSender sender, MessageService messageService, int page, int totalPages) {
        if (messageService != null && !messageService.getMessage("help-header").isBlank()) {
            messageService.sendMessage(sender, "help-header", Map.of(
                    "page", Integer.toString(page),
                    "pages", Integer.toString(totalPages)
            ));
            return;
        }
        sender.sendMessage(ChatColor.RED + "---------- " + ChatColor.DARK_RED + "EzLifesteal Help "
                + ChatColor.RED + "(Page " + page + "/" + totalPages + ") ----------");
    }

    private void sendFooter(CommandSender sender, MessageService messageService, int page, int totalPages) {
        if (page < totalPages) {
            if (messageService != null && !messageService.getMessage("help-footer").isBlank()) {
                messageService.sendMessage(sender, "help-footer", Map.of("next_page", Integer.toString(page + 1)));
            }
            else {
                sender.sendMessage(ChatColor.GRAY + "Use " + ChatColor.WHITE + "/lifesteal help " + (page + 1)
                        + ChatColor.GRAY + " for the next page.");
            }
            return;
        }

        if (messageService != null && !messageService.getMessage("help-footer-end").isBlank()) {
            messageService.sendMessage(sender, "help-footer-end");
        }
        else {
            sender.sendMessage(ChatColor.GRAY + "Use " + ChatColor.WHITE + "/lifesteal about "
                    + ChatColor.GRAY + "for plugin information.");
        }
    }

    private void sendNoPage(CommandSender sender, MessageService messageService, int page, int totalPages) {
        if (messageService != null && !messageService.getMessage("help-no-page").isBlank()) {
            messageService.sendMessage(sender, "help-no-page", Map.of(
                    "page", Integer.toString(page),
                    "pages", Integer.toString(totalPages)
            ));
            return;
        }
        sender.sendMessage(ChatColor.RED + "There is no help page " + page + ". Max page is " + totalPages + ".");
    }

    private String resolveDescription(String subcommandName, MessageService messageService) {
        final String key = "help-desc-" + subcommandName;
        if (messageService != null) {
            final String configured = messageService.getMessage(key);
            if (!configured.isBlank()) {
                return configured;
            }
        }
        return FALLBACK_DESCRIPTIONS.getOrDefault(subcommandName, "No description available.");
    }

    private static Map<String, String> createFallbackDescriptions() {
        final Map<String, String> descriptions = new LinkedHashMap<>();
        descriptions.put("help", "Show available subcommands you can run.");
        descriptions.put("about", "Show plugin information and version details.");
        descriptions.put("shop", "Open the heart shop menu.");
        descriptions.put("hearts", "View a player's hearts.");
        descriptions.put("transfer", "Transfer hearts to another player.");
        descriptions.put("teambank", "Manage your team's shared heart bank.");
        descriptions.put("top", "Show the top hearts leaderboard.");
        descriptions.put("banlist", "List active lifesteal bans.");
        descriptions.put("set", "Set a player's heart count.");
        descriptions.put("add", "Add hearts to a player.");
        descriptions.put("remove", "Remove hearts from a player.");
        descriptions.put("reset", "Reset a player's hearts to default.");
        descriptions.put("resetall", "Reset all stored player hearts.");
        descriptions.put("revive", "Revive a player and clear lifesteal ban state.");
        descriptions.put("giveheart", "Give configured heart items to a player.");
        descriptions.put("withdraw", "Withdraw one heart into a voucher item.");
        descriptions.put("reload", "Reload plugin configuration and services.");
        descriptions.put("hologram", "Manage the top leaderboard hologram.");
        descriptions.put("beacon", "Manage revive beacon whitelist and runtime requirements.");
        descriptions.put("smurf", "Open smurf management tools.");
        descriptions.put("test", "Run lifesteal simulation commands.");
        return descriptions;
    }
}
