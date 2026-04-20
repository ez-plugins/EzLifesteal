package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;


public class TopSubcommand implements Subcommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args,
            LifestealCommand context) {
        if (!context.requirePermissionPublic(sender, "lifesteal.top", "lifesteal.admin")) {
            return true;
        }
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            }
            catch (NumberFormatException exception) {
                context.getPluginAccessorPublic().getMessageService().sendMessage(sender, "top-invalid-page");
                return true;
            }
        }
        if (page <= 0) {
            context.getPluginAccessorPublic().getMessageService().sendMessage(sender, "top-invalid-page");
            return true;
        }
        final int pageSize = 10;
        final long requested = (long) page * pageSize;
        final int limit = requested > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) requested;
        final PluginAccessor plugin = context.getPluginAccessorPublic();
        final LifestealManager topManager = plugin.getLifestealManager();
        final int finalPage = page;
        topManager.loadTopProfilesAsync(limit).whenComplete((profiles, throwable) -> {
            if (throwable != null) {
                plugin.getLogger().severe("Failed to load top profiles: " + throwable.getMessage());
                context.getMainThreadExecutorPublic()
                        .execute(() -> plugin.getMessageService().sendMessage(sender, "storage-error"));
                return;
            }
            final Runnable uiTask = () -> {
                if (profiles == null || profiles.isEmpty()) {
                    plugin.getMessageService().sendMessage(sender, "top-empty");
                    return;
                }
                final int total = profiles.size();
                final int startIndex = Math.min(total, (finalPage - 1) * pageSize);
                if (startIndex >= total) {
                    plugin.getMessageService().sendMessage(sender, "top-no-page",
                            java.util.Map.of("page", Integer.toString(finalPage)));
                    return;
                }
                final int endIndex = Math.min(total, startIndex + pageSize);
                int totalPages = (int) Math.ceil(total / (double) pageSize);
                if (totalPages < finalPage) {
                    totalPages = finalPage;
                }
                plugin.getMessageService().sendMessage(sender, "top-header", java.util.Map.of(
                        "page", Integer.toString(finalPage),
                        "pages", Integer.toString(Math.max(totalPages, 1))
                ));
                for (int index = startIndex; index < endIndex; index++) {
                    final LifestealProfile profile = profiles.get(index);
                    plugin.getMessageService().sendMessage(sender, "top-entry", java.util.Map.of(
                            "rank", Integer.toString(index + 1),
                            "player", resolvePlayerName(profile),
                            "hearts", Double.toString(profile.getHearts())
                    ));
                }
                if (total == limit) {
                    plugin.getMessageService().sendMessage(sender, "top-footer",
                            java.util.Map.of("next_page", Integer.toString(finalPage + 1)));
                }
                else {
                    plugin.getMessageService().sendMessage(sender, "top-footer-end");
                }
            };
            context.getMainThreadExecutorPublic().execute(uiTask);
        });
        return true;
    }

    private String resolvePlayerName(LifestealProfile profile) {
        try {
            final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(profile.getUniqueId());
            final String name = offlinePlayer != null ? offlinePlayer.getName() : null;
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        catch (Throwable ignored) {
            // Bukkit lookup may be unavailable in isolated unit tests; fall through to UUID fallback.
        }
        return profile.getUniqueId().toString();
    }
}
