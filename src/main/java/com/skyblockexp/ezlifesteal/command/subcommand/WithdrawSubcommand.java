package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.HeartWithdrawService;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WithdrawSubcommand implements Subcommand {

    private final HeartWithdrawService heartWithdrawService;

    public WithdrawSubcommand() {
        this(new HeartWithdrawService());
    }

    public WithdrawSubcommand(HeartWithdrawService heartWithdrawService) {
        this.heartWithdrawService = heartWithdrawService;
    }

    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args,
            LifestealCommand context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        if (!context.requirePermissionPublic(sender, "lifesteal.withdraw", "lifesteal.admin")) {
            return true;
        }

        final PluginAccessor plugin = context.getPluginAccessorPublic();
        final LifestealManager manager = plugin.getLifestealManager();

        final HeartWithdrawService.WithdrawResult result = heartWithdrawService.withdraw(player, manager,
                plugin.getHeartRegistry());
        switch (result.status()) {
            case SUCCESS -> {
                plugin.sendHeartStatus(player, result.remainingHearts());
                player.sendMessage(ChatColor.GRAY + "Withdrew " + ChatColor.RED + "1x " + result.heartId()
                        + ChatColor.GRAY + " voucher. Remaining hearts: " + ChatColor.RED
                        + context.formatPublic(result.remainingHearts()));
            }
            case INSUFFICIENT_HEARTS -> plugin.getMessageService().sendMessage(player, "transfer-insufficient-hearts");
            case PROFILE_NOT_LOADED -> manager.loadProfileAsync(player.getUniqueId())
                    .thenAccept(profile -> context.getMainThreadExecutorPublic().execute(() ->
                            execute(player, command, label, args, context)))
                    .exceptionally(throwable -> {
                        context.getMainThreadExecutorPublic().execute(() ->
                                context.handleAsyncFailure(player, throwable, "loading withdraw profile"));
                        return null;
                    });
            case UNAVAILABLE -> player.sendMessage(ChatColor.RED + "Withdraw voucher is currently unavailable.");
            default -> {
            }
        }
        return true;
    }
}
