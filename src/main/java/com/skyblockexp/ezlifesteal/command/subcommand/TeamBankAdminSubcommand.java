package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.TeamBankAdminService;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/**
 * Thin command delegating admin team-bank operations to {@link TeamBankAdminService}.
 *
 * <p>Subcommands: {@code balance <team>}, {@code deposit <team> <amount>},
 * {@code withdraw <team> <amount>}, {@code reset <team>},
 * {@code transfer <fromTeam> <toTeam> <amount>}.</p>
 *
 * <p>Permission: {@code lifesteal.teambank.admin} or {@code lifesteal.admin}.</p>
 */
public final class TeamBankAdminSubcommand implements Subcommand {

    private static final String ADMIN_PERMISSION = "lifesteal.teambank.admin";
    private static final String BASE_USAGE = "/lifesteal teambank-admin <balance|deposit|withdraw|reset|transfer> ...";

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args, LifestealCommand context) {
        if (!context.requirePermissionPublic(sender, ADMIN_PERMISSION, "lifesteal.admin")) {
            return true;
        }
        final PluginAccessor plugin = context.getPluginAccessorPublic();
        final TeamBankAdminService service = plugin.getTeamBankAdminService();
        if (service == null) {
            sender.sendMessage("Team bank admin service is unavailable.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("Usage: " + BASE_USAGE);
            return true;
        }
        final String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "balance" -> {
                if (args.length < 3) {
                    sender.sendMessage("Usage: /" + label + " teambank-admin balance <team>");
                    return true;
                }
                service.adminBalance(args[2])
                        .thenAccept(result -> context.getMainThreadExecutorPublic().execute(
                                () -> sendAdminResult(sender, result, action, 0.0D, plugin)))
                        .exceptionally(throwable -> {
                            context.getMainThreadExecutorPublic()
                                    .execute(() -> context.handleAsyncFailure(sender, throwable, "loading team bank balance"));
                            return null;
                        });
            }
            case "deposit" -> {
                if (args.length < 4) {
                    sender.sendMessage("Usage: /" + label + " teambank-admin deposit <team> <amount>");
                    return true;
                }
                final double amount = parseAmount(args[3], sender);
                if (amount <= 0.0D) {
                    return true;
                }
                service.adminDeposit(args[2], amount)
                        .thenAccept(result -> context.getMainThreadExecutorPublic().execute(
                                () -> sendAdminResult(sender, result, action, amount, plugin)))
                        .exceptionally(throwable -> {
                            context.getMainThreadExecutorPublic()
                                    .execute(() -> context.handleAsyncFailure(sender, throwable, "depositing team bank hearts"));
                            return null;
                        });
            }
            case "withdraw" -> {
                if (args.length < 4) {
                    sender.sendMessage("Usage: /" + label + " teambank-admin withdraw <team> <amount>");
                    return true;
                }
                final double amount = parseAmount(args[3], sender);
                if (amount <= 0.0D) {
                    return true;
                }
                service.adminWithdraw(args[2], amount)
                        .thenAccept(result -> context.getMainThreadExecutorPublic().execute(
                                () -> sendAdminResult(sender, result, action, amount, plugin)))
                        .exceptionally(throwable -> {
                            context.getMainThreadExecutorPublic()
                                    .execute(() -> context.handleAsyncFailure(sender, throwable, "withdrawing team bank hearts"));
                            return null;
                        });
            }
            case "reset" -> {
                if (args.length < 3) {
                    sender.sendMessage("Usage: /" + label + " teambank-admin reset <team>");
                    return true;
                }
                service.adminReset(args[2])
                        .thenAccept(result -> context.getMainThreadExecutorPublic().execute(
                                () -> sendAdminResult(sender, result, action, 0.0D, plugin)))
                        .exceptionally(throwable -> {
                            context.getMainThreadExecutorPublic()
                                    .execute(() -> context.handleAsyncFailure(sender, throwable, "resetting team bank"));
                            return null;
                        });
            }
            case "transfer" -> {
                if (args.length < 5) {
                    sender.sendMessage("Usage: /" + label + " teambank-admin transfer <fromTeam> <toTeam> <amount>");
                    return true;
                }
                final double amount = parseAmount(args[4], sender);
                if (amount <= 0.0D) {
                    return true;
                }
                service.adminTransfer(args[2], args[3], amount)
                        .thenAccept(result -> context.getMainThreadExecutorPublic().execute(
                                () -> sendTransferResult(sender, result, amount, plugin)))
                        .exceptionally(throwable -> {
                            context.getMainThreadExecutorPublic()
                                    .execute(() -> context.handleAsyncFailure(sender, throwable, "transferring team bank hearts"));
                            return null;
                        });
            }
            default -> sender.sendMessage("Usage: " + BASE_USAGE);
        }
        return true;
    }

    private void sendAdminResult(CommandSender sender, TeamBankAdminService.AdminResult result,
                                 String action, double amount, PluginAccessor plugin) {
        switch (result.status()) {
            case SUCCESS -> {
                if ("balance".equals(action)) {
                    plugin.getMessageService().sendMessage(sender, "team-bank-admin-balance", Map.of(
                            "team", result.teamName(),
                            "bank", Double.toString(result.bankHearts())
                    ));
                } else if ("reset".equals(action)) {
                    plugin.getMessageService().sendMessage(sender, "team-bank-admin-reset", Map.of(
                            "team", result.teamName()
                    ));
                } else if ("deposit".equals(action)) {
                    plugin.getMessageService().sendMessage(sender, "team-bank-admin-deposit", Map.of(
                            "team", result.teamName(),
                            "amount", Double.toString(amount),
                            "bank", Double.toString(result.bankHearts())
                    ));
                } else {
                    plugin.getMessageService().sendMessage(sender, "team-bank-admin-withdraw", Map.of(
                            "team", result.teamName(),
                            "amount", Double.toString(amount),
                            "bank", Double.toString(result.bankHearts())
                    ));
                }
            }
            case DISABLED -> sender.sendMessage("Team heart bank is disabled.");
            case INVALID_AMOUNT -> sender.sendMessage("Please enter a valid heart amount greater than zero.");
            case TEAM_NOT_FOUND -> plugin.getMessageService().sendMessage(sender, "team-bank-admin-team-not-found",
                    Map.of("team", result.teamName()));
            case STORAGE_UNAVAILABLE -> sender.sendMessage("Team bank storage is unavailable.");
            case INSUFFICIENT_BANK_HEARTS -> sender.sendMessage("Team bank does not have enough hearts.");
            case BANK_CAP_EXCEEDED -> sender.sendMessage("Operation would exceed the team bank cap.");
            case TEAMS_API_UNAVAILABLE -> plugin.getMessageService().sendMessage(
                    sender, "team-bank-admin-teams-api-unavailable");
            default -> plugin.getMessageService().sendMessage(sender, "storage-error");
        }
    }

    private void sendTransferResult(CommandSender sender, TeamBankAdminService.TransferResult result,
                                    double amount, PluginAccessor plugin) {
        switch (result.status()) {
            case SUCCESS -> plugin.getMessageService().sendMessage(sender, "team-bank-admin-transfer", Map.of(
                    "from", result.fromTeam(),
                    "to", result.toTeam(),
                    "amount", Double.toString(amount),
                    "from_bank", Double.toString(result.fromHearts()),
                    "to_bank", Double.toString(result.toHearts())
            ));
            case DISABLED -> sender.sendMessage("Team heart bank is disabled.");
            case INVALID_AMOUNT -> sender.sendMessage("Please enter a valid heart amount greater than zero.");
            case FROM_TEAM_NOT_FOUND -> plugin.getMessageService().sendMessage(
                    sender, "team-bank-admin-team-not-found", Map.of("team", result.fromTeam()));
            case TO_TEAM_NOT_FOUND -> plugin.getMessageService().sendMessage(
                    sender, "team-bank-admin-team-not-found", Map.of("team", result.toTeam()));
            case STORAGE_UNAVAILABLE -> sender.sendMessage("Team bank storage is unavailable.");
            case INSUFFICIENT_BANK_HEARTS -> sender.sendMessage("Source team bank does not have enough hearts.");
            case BANK_CAP_EXCEEDED -> sender.sendMessage("Transfer would exceed the target team bank cap.");
            case TEAMS_API_UNAVAILABLE -> plugin.getMessageService().sendMessage(
                    sender, "team-bank-admin-teams-api-unavailable");
            default -> plugin.getMessageService().sendMessage(sender, "storage-error");
        }
    }

    private double parseAmount(String raw, CommandSender sender) {
        try {
            final double amount = Double.parseDouble(raw);
            if (!Double.isFinite(amount) || amount <= 0.0D) {
                sender.sendMessage("Please enter a valid heart amount greater than zero.");
                return -1.0D;
            }
            return amount;
        } catch (NumberFormatException exception) {
            sender.sendMessage("Please enter a valid heart amount greater than zero.");
            return -1.0D;
        }
    }
}
