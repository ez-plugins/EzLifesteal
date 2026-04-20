package com.skyblockexp.ezlifesteal.listener;

import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.plugin.Plugin;

public final class SeasonsServiceListener implements Listener {

    private static final String SEASONS_API_CLASS_NAME = "com.skyblockexp.lifesteal.seasons.api.SeasonsApi";

    private final Plugin schedulerPlugin;

    private final Runnable hookAction;

    private final Runnable unhookAction;


    public SeasonsServiceListener(Plugin schedulerPlugin, Runnable hookAction, Runnable unhookAction) {
        this.schedulerPlugin = Objects.requireNonNull(schedulerPlugin, "schedulerPlugin");
        this.hookAction = Objects.requireNonNull(hookAction, "hookAction");
        this.unhookAction = Objects.requireNonNull(unhookAction, "unhookAction");
    }

    @EventHandler
    public void onServiceRegister(ServiceRegisterEvent event) {
        if (isSeasonsService(event.getProvider().getService())) {
            SchedulerAdapter.run(schedulerPlugin, hookAction);
        }
    }

    @EventHandler
    public void onServiceUnregister(ServiceUnregisterEvent event) {
        if (isSeasonsService(event.getProvider().getService())) {
            SchedulerAdapter.run(schedulerPlugin, unhookAction);
        }
    }

    private boolean isSeasonsService(Class<?> serviceClass) {
        return serviceClass != null && SEASONS_API_CLASS_NAME.equals(serviceClass.getName());
    }
}
