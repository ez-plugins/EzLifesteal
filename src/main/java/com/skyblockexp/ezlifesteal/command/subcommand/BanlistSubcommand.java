package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.storage.BanRecord;
import com.skyblockexp.ezlifesteal.storage.StorageException;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class BanlistSubcommand implements Subcommand {
    @Override
    public boolean execute(CommandSender sender, Command command, String label, String[] args,
            LifestealCommand context) {
        if (!context.requirePermissionPublic(sender, "lifesteal.admin.banlist", "lifesteal.admin")) {
            return true;
        }
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            }
            catch (NumberFormatException ignored) {
                context.getPluginAccessorPublic().getMessageService().sendMessage(sender, "top-invalid-page");
                return true;
            }
        }
        if (page <= 0) {
            context.getPluginAccessorPublic().getMessageService().sendMessage(sender, "top-invalid-page");
            return true;
        }
        final int pageSize = 10;
        final int start = (page - 1) * pageSize;
        final int pageCopy = page;
        final int startCopy = start;

        final PluginAccessor plugin = context.getPluginAccessorPublic();

        // If there's no BanRepository configured, fall back to the server BanList (Bukkit).
        if (plugin.getBanRepository() == null) {
            try {
                final org.bukkit.BanList nameBanList = org.bukkit.Bukkit.getBanList(org.bukkit.BanList.Type.NAME);
                final java.util.Set<org.bukkit.BanEntry> entries = nameBanList.getBanEntries();
                final java.util.List<org.bukkit.BanEntry> filtered = new java.util.ArrayList<>();
                final String sourceName = plugin.getPluginName();
                for (org.bukkit.BanEntry e : entries) {
                    try {
                        final String src = e.getSource();
                        if (src != null && src.equals(sourceName)) {
                            filtered.add(e);
                        }
                    }
                    catch (Throwable ignored) {
                    }
                }
                if (filtered.isEmpty()) {
                    plugin.getMessageService().sendMessage(sender, "banlist-empty");
                    return true;
                }
                // Sort by created date descending (newest first)
                filtered.sort((a, b) -> {
                    final java.util.Date da = a.getCreated();
                    final java.util.Date db = b.getCreated();
                    if (da == null && db == null) {
                        return 0;
                    }
                    if (da == null) {
                        return 1;
                    }
                    if (db == null) {
                        return -1;
                    }
                    return db.compareTo(da);
                });

                final int total = filtered.size();
                final int totalPages = (int) Math.ceil(total / (double) pageSize);
                plugin.getMessageService().sendMessage(sender, "banlist-header", java.util.Map.of(
                        "count", Integer.toString(total),
                        "page", Integer.toString(pageCopy),
                        "pages", Integer.toString(Math.max(totalPages, 1))
                ));

                final int end = Math.min(total, startCopy + pageSize);
                for (int i = startCopy; i < end; i++) {
                    final org.bukkit.BanEntry e = filtered.get(i);
                    final String target = e.getTarget() == null ? "" : e.getTarget();
                    final String reason = e.getReason() == null ? "" : e.getReason();
                    final String created = e.getCreated() == null ? "" : e.getCreated().toString();
                    final String expires = e.getExpiration() == null ? "never" : e.getExpiration().toString();
                    plugin.getMessageService().sendMessage(sender, "banlist-entry", java.util.Map.of(
                            "target", target,
                            "reason", reason,
                            "created", created,
                            "expires", expires
                    ));
                }
                if (end < total) {
                    plugin.getMessageService().sendMessage(sender, "banlist-footer",
                            java.util.Map.of("next_page", Integer.toString(pageCopy + 1)));
                }
                else {
                    plugin.getMessageService().sendMessage(sender, "banlist-footer-end");
                }
                return true;
            }
            catch (Throwable t) {
                plugin.getLogger().severe("Failed to read server ban list: " + t.getMessage());
                plugin.getMessageService().sendMessage(sender, "storage-error");
                return true;
            }
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                return plugin.getBanRepository().loadActiveBans();
            }
            catch (StorageException e) {
                throw new RuntimeException(e);
            }
        }).whenComplete((records, throwable) -> {
            if (throwable != null) {
                plugin.getLogger().severe("Failed to load banlist: " + throwable.getMessage());
                context.getMainThreadExecutorPublic()
                        .execute(() -> plugin.getMessageService().sendMessage(sender, "storage-error"));
                return;
            }
            final Runnable ui = () -> {
                if (records == null || records.isEmpty()) {
                    plugin.getMessageService().sendMessage(sender, "banlist-empty");
                    return;
                }
                final int total = records.size();
                final int totalPages = (int) Math.ceil(total / (double) pageSize);
                plugin.getMessageService().sendMessage(sender, "banlist-header", java.util.Map.of(
                    "count", Integer.toString(total),
                    "page", Integer.toString(pageCopy),
                    "pages", Integer.toString(Math.max(totalPages, 1))
                ));
                final int end = Math.min(total, startCopy + pageSize);
                for (int i = startCopy; i < end; i++) {
                    final BanRecord r = records.get(i);
                    final String target = r.getPlayerName() != null
                            && !r.getPlayerName().isBlank() ? r.getPlayerName() : r.getUniqueId().toString();
                    final String reason = r.getReason() == null ? "" : r.getReason();
                    final String created = toIso(r.getCreatedAt());
                    final String expires = r.getExpiresAt() == null ? "never" : toIso(r.getExpiresAt());
                    plugin.getMessageService().sendMessage(sender, "banlist-entry", java.util.Map.of(
                            "target", target,
                            "reason", reason,
                            "created", created,
                            "expires", expires
                    ));
                }
                if (end < total) {
                    plugin.getMessageService().sendMessage(sender, "banlist-footer",
                            java.util.Map.of("next_page", Integer.toString(pageCopy + 1)));
                }
                else {
                    plugin.getMessageService().sendMessage(sender, "banlist-footer-end");
                }
            };
            context.getMainThreadExecutorPublic().execute(ui);
        });

        return true;
    }

    private static String toIso(Instant instant) {
        if (instant == null) {
            return "";
        }
        return instant.toString();
    }
}
