package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.TeamBankService;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class TeamBankSubcommand implements Subcommand {

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args, LifestealCommand context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        final PluginAccessor plugin = context.getPluginAccessorPublic();
        final TeamBankService service = plugin.getTeamBankService();
        if (service == null) {
            sendOrFallback(plugin, sender, "team-bank-storage-unavailable", "Team bank storage is unavailable.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("Usage: /" + label + " teambank <balance|deposit|withdraw> [amount]");
            return true;
        }

        final String action = args[1].toLowerCase(java.util.Locale.ROOT);
        switch (action) {
            case "balance":
                if (!context.requirePermissionPublic(sender, "lifesteal.teambank.balance", "lifesteal.admin")) {
                    return true;
                }
                service.balance(player).thenAccept(result ->
                        context.getMainThreadExecutorPublic().execute(() -> sendResult(sender, result, "balance", 0.0D, plugin)))
                        .exceptionally(throwable -> {
                            context.getMainThreadExecutorPublic()
                                    .execute(() -> context.handleAsyncFailure(sender, throwable, "loading team bank balance"));
                            return null;
                        });
                return true;
            case "deposit":
                if (!context.requirePermissionPublic(sender, "lifesteal.teambank.deposit", "lifesteal.admin")) {
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("Usage: /" + label + " teambank deposit <amount>");
                    return true;
                }
                final double depositAmount = parseAmount(args[2], plugin, sender);
                if (depositAmount <= 0.0D) {
                    return true;
                }
                service.deposit(player, depositAmount).thenAccept(result ->
                        context.getMainThreadExecutorPublic().execute(() -> {
                            sendResult(sender, result, "deposit", depositAmount, plugin);
                            if (result.status() == TeamBankService.Status.SUCCESS) {
                                plugin.sendHeartStatus(player, result.playerHearts());
                                plugin.requestTopHologramUpdate();
                            }
                        }))
                        .exceptionally(throwable -> {
                            context.getMainThreadExecutorPublic()
                                    .execute(() -> context.handleAsyncFailure(sender, throwable, "depositing team bank hearts"));
                            return null;
                        });
                return true;
            case "withdraw":
                if (!context.requirePermissionPublic(sender, "lifesteal.teambank.withdraw", "lifesteal.admin")) {
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage("Usage: /" + label + " teambank withdraw <amount>");
                    return true;
                }
                final double withdrawAmount = parseAmount(args[2], plugin, sender);
                if (withdrawAmount <= 0.0D) {
                    return true;
                }
                service.withdraw(player, withdrawAmount).thenAccept(result ->
                        context.getMainThreadExecutorPublic().execute(() -> {
                            sendResult(sender, result, "withdraw", withdrawAmount, plugin);
                            if (result.status() == TeamBankService.Status.SUCCESS) {
                                plugin.sendHeartStatus(player, result.playerHearts());
                                plugin.requestTopHologramUpdate();
                            }
                        }))
                        .exceptionally(throwable -> {
                            context.getMainThreadExecutorPublic()
                                    .execute(() -> context.handleAsyncFailure(sender, throwable, "withdrawing team bank hearts"));
                            return null;
                        });
                return true;
            default:
                sender.sendMessage("Usage: /" + label + " teambank <balance|deposit|withdraw> [amount]");
                return true;
        }
    }

    private double parseAmount(String raw, PluginAccessor plugin, CommandSender sender) {
        final double amount;
        try {
            amount = Double.parseDouble(raw);
        }
        catch (NumberFormatException exception) {
            sendOrFallback(plugin, sender, "team-bank-invalid-amount",
                    "Please enter a valid heart amount greater than zero.");
            return -1.0D;
        }
        if (!Double.isFinite(amount) || amount <= 0.0D) {
            sendOrFallback(plugin, sender, "team-bank-invalid-amount",
                    "Please enter a valid heart amount greater than zero.");
            return -1.0D;
        }
        return amount;
    }

    private void sendResult(CommandSender sender,
                            TeamBankService.Result result,
                            String action,
                            double amount,
                            PluginAccessor plugin) {
        switch (result.status()) {
            case SUCCESS -> {
                if ("balance".equals(action)) {
                    plugin.getMessageService().sendMessage(sender, "team-bank-balance", Map.of(
                            "team", result.teamName(),
                            "bank", Double.toString(result.bankHearts()),
                            "player", Double.toString(result.playerHearts())
                    ));
                }
                else if ("deposit".equals(action)) {
                    plugin.getMessageService().sendMessage(sender, "team-bank-deposit-success", Map.of(
                            "team", result.teamName(),
                            "amount", Double.toString(amount),
                            "bank", Double.toString(result.bankHearts()),
                            "player", Double.toString(result.playerHearts())
                    ));
                }
                else {
                    plugin.getMessageService().sendMessage(sender, "team-bank-withdraw-success", Map.of(
                            "team", result.teamName(),
                            "amount", Double.toString(amount),
                            "bank", Double.toString(result.bankHearts()),
                            "player", Double.toString(result.playerHearts())
                    ));
                }
            }
            case DISABLED -> sendOrFallback(plugin, sender, "team-bank-disabled", "Team heart bank is disabled.");
            case INVALID_AMOUNT -> sendOrFallback(plugin, sender, "team-bank-invalid-amount",
                    "Please enter a valid heart amount greater than zero.");
            case TEAM_UNAVAILABLE -> sendOrFallback(plugin, sender, "team-bank-team-unavailable",
                    "You are not in a team, or TeamsAPI is unavailable.");
            case PROFILE_UNAVAILABLE -> sendOrFallback(plugin, sender, "team-bank-profile-unavailable",
                    "Your profile is not available right now.");
            case STORAGE_UNAVAILABLE -> sendOrFallback(plugin, sender, "team-bank-storage-unavailable",
                    "Team bank storage is unavailable.");
            case INSUFFICIENT_PLAYER_HEARTS -> sendOrFallback(plugin, sender, "team-bank-insufficient-player-hearts",
                    "You do not have enough hearts to deposit that amount.");
            case INSUFFICIENT_BANK_HEARTS -> sendOrFallback(plugin, sender, "team-bank-insufficient-bank-hearts",
                    "Your team bank does not have enough hearts.");
            case BANK_CAP_EXCEEDED -> {
                final String configured = plugin.getMessageService().getMessage("team-bank-cap-exceeded");
                if (configured != null && !configured.isBlank()) {
                    plugin.getMessageService().sendMessage(sender, "team-bank-cap-exceeded",
                            Map.of("max", Double.toString(plugin.getTeamBankMaxHearts())));
                }
                else {
                    sender.sendMessage("Deposit exceeds the team bank limit (" + plugin.getTeamBankMaxHearts() + ").");
                }
            }
            default -> plugin.getMessageService().sendMessage(sender, "storage-error");
        }
    }

    private void sendOrFallback(PluginAccessor plugin, CommandSender sender, String key, String fallback) {
        final String configured = plugin.getMessageService().getMessage(key);
        if (configured != null && !configured.isBlank()) {
            plugin.getMessageService().sendMessage(sender, key);
            return;
        }
        sender.sendMessage(fallback);
    }
}
