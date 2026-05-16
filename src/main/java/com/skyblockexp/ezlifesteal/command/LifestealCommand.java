package com.skyblockexp.ezlifesteal.command;

import com.skyblockexp.ezlifesteal.command.subcommand.AboutSubcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.AddSubcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.BanlistSubcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.BeaconSubcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.GiveheartSubcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.HeartsSubcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.HelpSubcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.HologramSubcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.ReloadSubcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.RemoveSubcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.ResetAllSubcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.ResetSubcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.ReviveSubcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.SetSubcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.ShopSubcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.SmurfSubcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.Subcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.TestSubcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.TopSubcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.TransferSubcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.TeamBankAdminSubcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.TeamBankSubcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.WithdrawSubcommand;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.util.PlayerLookupService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

public class LifestealCommand implements org.bukkit.command.CommandExecutor {
    private final PluginAccessor plugin;

    private final SubcommandRegistry subcommandRegistry;


    public LifestealCommand() {
        this(null, createDefaultSubcommandRegistry());
    }

    public LifestealCommand(PluginAccessor plugin) {
        this(plugin, createDefaultSubcommandRegistry());
    }

    public LifestealCommand(PluginAccessor plugin, SubcommandRegistry subcommandRegistry) {
        this.plugin = plugin;
        this.subcommandRegistry = subcommandRegistry;
    }

    public boolean requirePermission(Object senderObj, Object permissionObj, Object... fallbackObjs) {
        if (permissionObj == null) {
            return true;
        }
        final String permission = permissionObj.toString();
        String[] fallback = null;
        if (fallbackObjs != null && fallbackObjs.length > 0) {
            fallback = new String[fallbackObjs.length];
            for (int i = 0; i < fallbackObjs.length; i++) {
                fallback[i] = fallbackObjs[i] == null ? null : fallbackObjs[i].toString();
            }
        }
        final CommandSender sender = senderObj instanceof CommandSender ? (CommandSender) senderObj : null;
        return requirePermission(sender, permission, fallback == null ? new String[0] : fallback);
    }

    public boolean requirePermission(CommandSender sender, String permission, String... fallback) {
        if (sender == null) {
            return false;
        }
        if (permission != null && sender.hasPermission(permission)) {
            return true;
        }
        if (fallback != null) {
            for (String p : fallback) {
                if (p != null && sender.hasPermission(p)) {
                    return true;
                }
            }
        }
        if (plugin != null && plugin.getMessageService() != null) {
            plugin.getMessageService().sendMessage(sender, "no-permission");
        }
        else {
            sender.sendMessage("You do not have permission to do that.");
        }
        return false;
    }

    public PluginAccessor getPluginAccessor() {
        return plugin;
    }

    public boolean requirePermissionPublic(Object senderObj, String permission, Object... fallbackObjs) {
        if (fallbackObjs == null || fallbackObjs.length == 0) {
            return requirePermission(senderObj, permission);
        }
        final String[] fallback = new String[fallbackObjs.length];
        for (int i = 0; i < fallbackObjs.length; i++) {
            fallback[i] = fallbackObjs[i] == null ? null : fallbackObjs[i].toString();
        }
        return requirePermission(senderObj, permission, (Object[]) fallback);
    }

    public PluginAccessor getPluginAccessorPublic() {
        return getPluginAccessor();
    }

    public PlayerLookupService getPlayerLookupService() {
        return plugin == null ? null : plugin.getPlayerLookupService();
    }

    public PlayerLookupService getPlayerLookupServicePublic() {
        return getPlayerLookupService();
    }

    public Executor getMainThreadExecutor() {
        return Runnable::run;
    }

    public Executor getMainThreadExecutorPublic() {
        return getMainThreadExecutor();
    }

    public org.bukkit.plugin.java.JavaPlugin getSchedulerPlugin() {
        return plugin == null ? null : plugin.getPlugin();
    }

    public String format(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public String formatPublic(double value) {
        return format(value);
    }

    public String formatCoordinate(double coord) {
        if (coord == (long) coord) {
            return String.format("%d", (long) coord);
        }
        return String.format("%s", coord);
    }

    public CompletableFuture<LifestealProfile> ensureProfileLoaded(LifestealManager manager, UUID uniqueId) {
        return manager == null ? CompletableFuture.completedFuture(null) : manager.loadProfileAsync(uniqueId);
    }

    public String resolvePlayerName(OfflinePlayer player, String fallback) {
        if (player != null) {
            return player.getName() == null ? fallback : player.getName();
        }
        return fallback;
    }

    public String resolvePlayerNamePublic(Object playerObj, String fallback) {
        if (playerObj instanceof OfflinePlayer offlinePlayer) {
            return resolvePlayerName(offlinePlayer, fallback);
        }
        return fallback;
    }

    public void sendPlayerNotFound(Object senderObj, Object identifierObj) {
        if (senderObj instanceof CommandSender sender) {
            sender.sendMessage("Player not found: " + identifierObj);
        }
    }

    public void sendPlayerNotFoundPublic(CommandSender sender, String identifier) {
        sendPlayerNotFound(sender, identifier);
    }

    public void sendHeartMessage(Object senderObj, OfflinePlayer target, double hearts) {
        if (senderObj instanceof CommandSender sender) {
            sender.sendMessage("Hearts: " + hearts);
        }
    }

    public void handleLookupFailure(Object senderObj, Object identifierObj, Throwable throwable) {
        if (senderObj instanceof CommandSender sender) {
            sender.sendMessage("Lookup failed for: " + identifierObj);
        }
    }

    public void handleAsyncFailure(Object senderObj, Throwable throwable, String contextMsg) {
        if (senderObj instanceof CommandSender sender) {
            sender.sendMessage("Async failure while " + contextMsg + ": " + throwable);
        }
    }


    private static SubcommandRegistry createDefaultSubcommandRegistry() {
        final SubcommandRegistry registry = new SubcommandRegistry();
        registry.register("banlist", new BanlistSubcommand(),
                requirement(false, "lifesteal.admin.banlist", "lifesteal.admin"));
        registry.register("transfer", new TransferSubcommand(),
                requirement(true, "lifesteal.transfer", "lifesteal.admin"));
        registry.register("teambank", new TeamBankSubcommand(),
                requirement(true, "lifesteal.teambank.balance", "lifesteal.teambank.deposit",
                        "lifesteal.teambank.withdraw", "lifesteal.admin"), "tb");
        registry.register("teambank-admin", new TeamBankAdminSubcommand(),
                requirement(false, "lifesteal.teambank.admin", "lifesteal.admin"), "tba");
        registry.register("top", new TopSubcommand(), requirement(false, "lifesteal.top", "lifesteal.admin"));
        registry.register("shop", new ShopSubcommand(), requirement(true));
        registry.register("help", new HelpSubcommand(), requirement(false, "lifesteal.command.base"));
        registry.register("about", new AboutSubcommand(), requirement(true));
        registry.register("hearts", new HeartsSubcommand(),
                requirement(false, "lifesteal.manage.view", "lifesteal.admin"));
        registry.register("set", new SetSubcommand(), requirement(false, "lifesteal.manage.modify", "lifesteal.admin"));
        registry.register("add", new AddSubcommand(), requirement(false, "lifesteal.manage.modify", "lifesteal.admin"));
        registry.register("remove", new RemoveSubcommand(),
                requirement(false, "lifesteal.manage.modify", "lifesteal.admin"));
        registry.register("reset", new ResetSubcommand(),
                requirement(false, "lifesteal.manage.modify", "lifesteal.admin"));
        registry.register("resetall", new ResetAllSubcommand(),
                requirement(false, "lifesteal.manage.resetall", "lifesteal.admin"));
        registry.register("revive", new ReviveSubcommand(),
                requirement(false, "lifesteal.manage.modify", "lifesteal.admin"));
        registry.register("giveheart", new GiveheartSubcommand(),
                requirement(true, "lifesteal.manage.modify", "lifesteal.admin"));
        registry.register("withdraw", new WithdrawSubcommand(),
                requirement(true, "lifesteal.withdraw", "lifesteal.admin"));
        registry.register("reload", new ReloadSubcommand(), requirement(false, "lifesteal.reload", "lifesteal.admin"));
        registry.register("hologram", new HologramSubcommand(),
                requirement(false, "lifesteal.hologram", "lifesteal.admin"));
        registry.register("beacon", new BeaconSubcommand(),
                requirement(false, "lifesteal.manage.modify", "lifesteal.admin"));
        registry.register("smurf", new SmurfSubcommand(),
                requirement(true, "lifesteal.smurf.manage", "lifesteal.admin"), "sm");
        registry.register("test", new TestSubcommand(), requirement(false, "lifesteal.test", "lifesteal.admin"));
        return registry;
    }

    private static SubcommandRequirement requirement(boolean playerOnly, String... permissions) {
        return new SubcommandRequirement(
                playerOnly,
                permissions == null ? Collections.emptyList() : List.of(permissions),
                Collections.emptyMap()
        );
    }

    private static final Map<String, SubcommandRequirement> DEFAULT_REQUIREMENTS = Map.ofEntries(
            Map.entry("help", new SubcommandRequirement(false, List.of("lifesteal.command.base"), null)),
            Map.entry("about", new SubcommandRequirement(true, Collections.emptyList(), null)),
            Map.entry("hearts",
                    new SubcommandRequirement(false, List.of("lifesteal.manage.view", "lifesteal.admin"), null)),
            Map.entry("shop", new SubcommandRequirement(true, Collections.emptyList(), null)),
            Map.entry("transfer",
                    new SubcommandRequirement(true, List.of("lifesteal.transfer", "lifesteal.admin"), null)),
            Map.entry("teambank",
                    new SubcommandRequirement(true, List.of(
                            "lifesteal.teambank.balance",
                            "lifesteal.teambank.deposit",
                            "lifesteal.teambank.withdraw",
                            "lifesteal.admin"
                    ), null)),
            Map.entry("teambank-admin",
                    new SubcommandRequirement(false, List.of("lifesteal.teambank.admin", "lifesteal.admin"), null)),
            Map.entry("smurf",
                    new SubcommandRequirement(true, List.of("lifesteal.smurf.manage", "lifesteal.admin"), null)),
            Map.entry("top", new SubcommandRequirement(false, List.of("lifesteal.top", "lifesteal.admin"), null)),
            Map.entry("banlist",
                    new SubcommandRequirement(false, List.of("lifesteal.admin.banlist", "lifesteal.admin"), null)),
            Map.entry("set",
                    new SubcommandRequirement(false, List.of("lifesteal.manage.modify", "lifesteal.admin"), null)),
            Map.entry("add",
                    new SubcommandRequirement(false, List.of("lifesteal.manage.modify", "lifesteal.admin"), null)),
            Map.entry("remove",
                    new SubcommandRequirement(false, List.of("lifesteal.manage.modify", "lifesteal.admin"), null)),
            Map.entry("reset",
                    new SubcommandRequirement(false, List.of("lifesteal.manage.modify", "lifesteal.admin"), null)),
            Map.entry("resetall",
                    new SubcommandRequirement(false, List.of("lifesteal.manage.resetall", "lifesteal.admin"), null)),
            Map.entry("revive",
                    new SubcommandRequirement(false, List.of("lifesteal.manage.modify", "lifesteal.admin"), null)),
            Map.entry("giveheart",
                    new SubcommandRequirement(true, List.of("lifesteal.manage.modify", "lifesteal.admin"), null)),
            Map.entry("withdraw",
                    new SubcommandRequirement(true, List.of("lifesteal.withdraw", "lifesteal.admin"), null)),
            Map.entry("reload", new SubcommandRequirement(false, List.of("lifesteal.reload", "lifesteal.admin"), null)),
            Map.entry("hologram",
                    new SubcommandRequirement(false, List.of("lifesteal.hologram", "lifesteal.admin"), null)),
            Map.entry("beacon",
                    new SubcommandRequirement(false, List.of("lifesteal.manage.modify", "lifesteal.admin"), null)),
            Map.entry("test", new SubcommandRequirement(false, List.of("lifesteal.test", "lifesteal.admin"), null))
    );

    public static List<String> listAllowedSubcommands(CommandSender sender) {
        if (sender == null) {
            return Collections.emptyList();
        }
        final List<String> allowed = new ArrayList<>();
        for (Map.Entry<String, SubcommandRequirement> entry : DEFAULT_REQUIREMENTS.entrySet()) {
            final SubcommandRequirement requirement = entry.getValue();
            if (requirement.playerOnly && !(sender instanceof org.bukkit.entity.Player)) {
                continue;
            }
            if (requirement.permissions.isEmpty()) {
                allowed.add(entry.getKey());
                continue;
            }
            for (String permission : requirement.permissions) {
                if (sender.hasPermission(permission)) {
                    allowed.add(entry.getKey());
                    break;
                }
            }
        }
        allowed.sort(String::compareTo);
        return allowed;
    }

    public static List<String> listAllowedSubcommands(Object senderObj) {
        if (senderObj instanceof CommandSender sender) {
            return listAllowedSubcommands(sender);
        }
        return Collections.emptyList();

    }


    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (args == null || args.length == 0) {
            args = new String[]{"help"};
        }

        final String subcommandName = args[0].toLowerCase(Locale.ROOT);
        final Subcommand subcommand = subcommandRegistry.resolve(subcommandName).orElse(null);
        if (subcommand == null) {
            sender.sendMessage("Unknown subcommand: " + subcommandName);
            return true;
        }

        return subcommand.execute(sender, command, label, args, this);
    }

    public static final class SubcommandRequirement {
        private final boolean playerOnly;

        private final List<String> permissions;

        private final Map<String, SubcommandRequirement> children;


        public SubcommandRequirement(boolean playerOnly, List<String> permissions, Map<String,
                SubcommandRequirement> children) {
            this.playerOnly = playerOnly;
            this.permissions = permissions == null ? Collections.emptyList() : permissions;
            this.children = children == null ? Collections.emptyMap() : children;
        }

        public boolean canExecute(CommandSender sender) {
            return true;
        }

        public List<String> getAccessibleChildren(CommandSender sender) {
            return new ArrayList<>(children.keySet());
        }

        public SubcommandRequirement getChild(String name) {
            if (name == null) {
                return null;
            }
            return children.get(name.toLowerCase(Locale.ROOT));
        }

        public boolean isPlayerOnly() {
            return playerOnly;
        }

        public List<String> getPermissions() {
            return permissions;
        }
    }
}
