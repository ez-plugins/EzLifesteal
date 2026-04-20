package com.skyblockexp.ezlifesteal.integration;

import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class SeasonResetListener implements Listener {

    private final PluginAccessor plugin;

    public SeasonResetListener(PluginAccessor plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSeasonReset(Event event) {
        final String eventClassName = event.getClass().getName();
        if (!eventClassName.contains("SeasonResetEvent")) {
            return;
        }
        final LifestealManager manager = plugin.getLifestealManager();
        if (manager == null) {
            plugin.getLogger().warning("Skipping EzSeasons reset sync: Lifesteal manager is not ready.");
            return;
        }

        manager.resetAllHeartsAsync().whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                final Throwable cause = throwable instanceof CompletionException completionException
                        && completionException.getCause() != null
                        ? completionException.getCause()
                        : throwable;
                plugin.getLogger().log(Level.SEVERE, "Failed to reset all hearts after EzSeasons season reset event.",
                        cause);
                return;
            }
            SchedulerAdapter.run(plugin.getPlugin(), () -> {
                plugin.requestTopHologramUpdate();
                Bukkit.broadcastMessage(plugin.getMessageService().format("season-reset-broadcast", null));
            });
        });
    }
}
