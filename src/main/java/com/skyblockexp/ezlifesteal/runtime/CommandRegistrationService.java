package com.skyblockexp.ezlifesteal.runtime;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.command.HeartsCommand;
import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.command.LifestealPaperCommand;
import com.skyblockexp.ezlifesteal.command.LifestealTabCompleter;
import com.skyblockexp.ezlifesteal.command.BeaconCommand;
import com.skyblockexp.ezlifesteal.command.ReviveCommand;
import com.skyblockexp.ezlifesteal.command.SubcommandRegistry;
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
import com.skyblockexp.ezlifesteal.command.subcommand.TestSubcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.TopSubcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.TransferSubcommand;
import com.skyblockexp.ezlifesteal.command.subcommand.WithdrawSubcommand;
import com.skyblockexp.ezlifesteal.service.BeaconReviveService;
import com.skyblockexp.ezlifesteal.service.ReviveAnimationService;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.CommandMap;

public final class CommandRegistrationService {

    private final EzLifestealPlugin plugin;

    private final PluginAccessor pluginAccessor;


    public CommandRegistrationService(EzLifestealPlugin plugin, PluginAccessor pluginAccessor) {
        this.plugin = plugin;
        this.pluginAccessor = pluginAccessor;
    }

    public void start() {
        final SubcommandRegistry subcommandRegistry = createSubcommandRegistry();
        final LifestealCommand executor = new LifestealCommand(pluginAccessor, subcommandRegistry);
        final LifestealTabCompleter tabCompleter = new LifestealTabCompleter(plugin);
        final var pluginCommand = plugin.getCommand("lifesteal");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(executor);
            pluginCommand.setTabCompleter(tabCompleter);
        }
        else {
            final boolean paperRegistered = tryRegisterPaperCommand(executor, tabCompleter);
            if (!paperRegistered) {
                plugin.getLogger()
                        .severe("Unable to register /lifesteal command; command definition is missing from plugin.yml");
            }
        }

        final var heartsCommand = plugin.getCommand("hearts");
        if (heartsCommand != null) {
            heartsCommand.setExecutor(new HeartsCommand(executor));
        }

        final var reviveCommand = plugin.getCommand("revive");
        if (reviveCommand != null) {
            reviveCommand.setExecutor(new ReviveCommand(
                    new BeaconReviveService(pluginAccessor, new ReviveAnimationService(pluginAccessor))
            ));
        }

        final var beaconPluginCommand = plugin.getCommand("beacon");
        if (beaconPluginCommand != null) {
            final BeaconCommand beaconCmd = new BeaconCommand(executor, tabCompleter);
            beaconPluginCommand.setExecutor(beaconCmd);
            beaconPluginCommand.setTabCompleter(beaconCmd);
        }
    }

    public void reload() {
        start();
    }

    public void stop() {
        // No-op
    }

    private SubcommandRegistry createSubcommandRegistry() {
        final SubcommandRegistry registry = new SubcommandRegistry();
        registry.register("banlist", new BanlistSubcommand(),
                requirement(false, "lifesteal.admin.banlist", "lifesteal.admin"));
        registry.register("transfer", new TransferSubcommand(),
                requirement(true, "lifesteal.transfer", "lifesteal.admin"));
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

    private LifestealCommand.SubcommandRequirement requirement(boolean playerOnly, String... permissions) {
        return new LifestealCommand.SubcommandRequirement(
                playerOnly,
                permissions == null ? Collections.emptyList() : List.of(permissions),
                Collections.emptyMap()
        );
    }

    private boolean tryRegisterPaperCommand(LifestealCommand executor, org.bukkit.command.TabCompleter tabCompleter) {
        try {
            registerCommand(new LifestealPaperCommand(plugin, executor, tabCompleter));
            return true;
        }
        catch (NoSuchMethodError | UnsupportedOperationException exception) {
            return false;
        }
        catch (Throwable throwable) {
            plugin.getLogger().warning("Failed to register Paper command handler: " + throwable.getMessage());
            return false;
        }
    }

    private void registerCommand(LifestealPaperCommand command) throws ReflectiveOperationException {
        final Object commandMapHandle =
                plugin.getServer().getClass().getMethod("getCommandMap").invoke(plugin.getServer());
        if (!(commandMapHandle instanceof CommandMap commandMap)) {
            throw new UnsupportedOperationException("Server does not expose a command map instance");
        }
        commandMap.register(plugin.getDescription().getName().toLowerCase(Locale.ROOT), command);
    }
}
