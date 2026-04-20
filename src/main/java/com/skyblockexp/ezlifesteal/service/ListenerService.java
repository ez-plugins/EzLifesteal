package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.gui.ShopGuiListener;
import com.skyblockexp.ezlifesteal.gui.SmurfGuiListener;
import com.skyblockexp.ezlifesteal.listener.MobListener;
import com.skyblockexp.ezlifesteal.listener.PlayerListener;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

public class ListenerService {
    private final DefaultPluginRuntimeServices runtime;

    private final EzLifestealPlugin plugin;

    public ListenerService(DefaultPluginRuntimeServices runtime, EzLifestealPlugin plugin) {
        this.runtime = runtime;
        this.plugin = plugin;
    }

    public void setupCoreListeners(PluginAccessor pluginAccessor) {
        final BanEnforcementService banEnforcementService = new BanEnforcementService(pluginAccessor);
        final CombatTagService combatTagService = new CombatTagService(pluginAccessor, banEnforcementService,
                runtime.getZeroHeartBanMessage(), runtime.getZeroHeartKickMessage(),
                runtime.isCombatLogoutProtectionEnabled(), runtime.getCombatLogoutTagDurationMillis());
        final PlayerDeathService playerDeathService = new PlayerDeathService(pluginAccessor, banEnforcementService);
        final ParticleEffectService particleEffectService = new ParticleEffectService(pluginAccessor);
        final PlayerListener playerListener = new PlayerListener(pluginAccessor, banEnforcementService,
                combatTagService, playerDeathService, particleEffectService, null,
                runtime.getZeroHeartBanMessage(), runtime.getZeroHeartKickMessage(),
                runtime.isCombatLogoutProtectionEnabled(), runtime.getCombatLogoutTagDurationMillis());
        runtime.setPlayerListener(playerListener);
        Bukkit.getPluginManager().registerEvents(playerListener, plugin);
        Bukkit.getPluginManager().registerEvents(new MobListener(pluginAccessor), plugin);
        Bukkit.getPluginManager().registerEvents(new SmurfGuiListener(), plugin);
        Bukkit.getPluginManager().registerEvents(new ShopGuiListener(), plugin);
    }

    public void shutdownListeners() {
        try {
            HandlerList.unregisterAll(plugin);
        }
        catch (Throwable ignored) {
        }
    }
}
