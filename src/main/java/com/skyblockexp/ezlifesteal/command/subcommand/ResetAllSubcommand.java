package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ResetAllSubcommand implements Subcommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args,
            LifestealCommand context) {
        if (!context.requirePermissionPublic(sender, "lifesteal.manage.resetall", "lifesteal.admin")) {
            return true;
        }
        final PluginAccessor plugin = context.getPluginAccessorPublic();
        final LifestealManager resetAllManager = plugin.getLifestealManager();
        resetAllManager.resetAllHeartsAsync().whenComplete((unused, throwable) -> {
            if (throwable != null) {
                context.handleAsyncFailure(sender, throwable, "reset all hearts");
                return;
            }
            final Runnable uiTask = () -> {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    resetAllManager.getLoadedProfile(online.getUniqueId()).ifPresent(profile -> {
                        resetAllManager.applyHearts(online, profile);
                        plugin.sendHeartStatus(online, profile.getHearts());
                    });
                }
                plugin.getMessageService().sendMessage(sender, "reset-all-hearts", java.util.Map.of(
                        "hearts", context.formatPublic(resetAllManager.getDefaultHearts())
                ));
                plugin.requestTopHologramUpdate();
            };
            context.getMainThreadExecutorPublic().execute(uiTask);
        });
        return true;
    }
}
