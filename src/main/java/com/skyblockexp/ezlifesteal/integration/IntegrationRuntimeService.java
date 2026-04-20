package com.skyblockexp.ezlifesteal.integration;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.listener.SeasonsServiceListener;
import com.skyblockexp.ezlifesteal.listener.VaultServiceListener;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.runtime.Registry;
import com.skyblockexp.ezlifesteal.util.PluginLifecycleSupport;
import java.lang.reflect.Method;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class IntegrationRuntimeService {

    private static final String SEASONS_PLUGIN_NAME = "EzSeasons";

    private final EzLifestealPlugin plugin;

    private final PluginAccessor pluginAccessor;

    private final DefaultPluginRuntimeServices runtime;

    private final Registry registry;


    public IntegrationRuntimeService(EzLifestealPlugin plugin, PluginAccessor pluginAccessor,
            DefaultPluginRuntimeServices runtime, Registry registry) {
        this.plugin = plugin;
        this.pluginAccessor = pluginAccessor;
        this.runtime = runtime;
        this.registry = registry;
    }

    public void start() {
        registerServiceListeners();
        refreshVaultIntegration();
        registerSeasonResetListener();
        hookIntoEzSeasons();
    }

    public void reload() {
        refreshVaultIntegration();
        hookIntoEzSeasons();
    }

    public void stop() {
        unhookFromEzSeasons();
        unregisterServiceListeners();
        unregisterSeasonResetListener();
    }

    public void refreshVaultIntegration() {
        runtime.setupVault();
    }

    public void hookIntoEzSeasons() {
        final var integrationState = registry.getSeasonsIntegrationState();
        if (integrationState.getIntegrationProxy() != null) {
            return;
        }
        final Object api = findSeasonsApi();
        if (api == null) {
            return;
        }
        if (!runtime.ensureSeasonsClasses()) {
            return;
        }
        try {
            final Method register = resolveIntegrationMethod(
                    integrationState.getApiClass(),
                    null,
                    "registerIntegration",
                    "registerLifestealIntegration"
            );
            final Object integration = createSeasonsIntegrationProxy(register.getParameterTypes()[0]);
            if (integration == null) {
                plugin.getLogger().warning("Failed to create EzSeasons integration proxy.");
                return;
            }
            final Object result = register.invoke(api, integration);
            if (!(result instanceof Boolean) || Boolean.TRUE.equals(result)) {
                integrationState.setApiInstance(api);
                integrationState.setIntegrationProxy(integration);
                plugin.getLogger().info("Hooked into EzSeasons. Hearts will reset automatically at season end.");
            }
            else {
                plugin.getLogger()
                        .warning("EzSeasons rejected the integration registration. Another plugin may already control"
                                + " resets.");
            }
        }
        catch (IllegalStateException | ReflectiveOperationException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to register EzSeasons integration.", exception);
        }
    }

    public void unhookFromEzSeasons() {
        final var integrationState = registry.getSeasonsIntegrationState();
        if (integrationState.getApiInstance() != null
                && integrationState.getIntegrationProxy() != null
                && integrationState.getApiClass() != null
                && integrationState.getIntegrationClass() != null) {
            try {
                final Method unregister = resolveIntegrationMethod(
                        integrationState.getApiClass(),
                        integrationState.getIntegrationProxy(),
                        "unregisterIntegration",
                        "unregisterLifestealIntegration"
                );
                unregister.invoke(integrationState.getApiInstance(), integrationState.getIntegrationProxy());
            }
            catch (IllegalStateException | ReflectiveOperationException exception) {
                plugin.getLogger().log(Level.WARNING, "Failed to unregister EzSeasons integration cleanly.", exception);
            }
        }
        integrationState.clearIntegrationRegistration();
    }

    private void registerServiceListeners() {
        final VaultServiceListener vaultServiceListener =
                new VaultServiceListener(runtime::ensureVaultEconomyClassesAvailable,
                this::refreshVaultIntegration);
        registry.getVaultIntegrationState().setServiceListener(vaultServiceListener);
        registry.setVaultServiceListener(vaultServiceListener);
        Bukkit.getPluginManager().registerEvents(vaultServiceListener, plugin);

        final SeasonsServiceListener seasonsServiceListener = new SeasonsServiceListener(plugin,
                this::hookIntoEzSeasons, this::unhookFromEzSeasons);
        registry.getSeasonsIntegrationState().setServiceListener(seasonsServiceListener);
        registry.setSeasonsServiceListener(seasonsServiceListener);
        Bukkit.getPluginManager().registerEvents(seasonsServiceListener, plugin);
    }

    private void unregisterServiceListeners() {
        final VaultServiceListener vaultServiceListener = registry.getVaultIntegrationState().getServiceListener();
        if (vaultServiceListener != null) {
            PluginLifecycleSupport.unregisterListener(vaultServiceListener);
            registry.getVaultIntegrationState().setServiceListener(null);
            registry.setVaultServiceListener(null);
        }
        final SeasonsServiceListener seasonsServiceListener =
                registry.getSeasonsIntegrationState().getServiceListener();
        if (seasonsServiceListener != null) {
            PluginLifecycleSupport.unregisterListener(seasonsServiceListener);
            registry.getSeasonsIntegrationState().setServiceListener(null);
            registry.setSeasonsServiceListener(null);
        }
    }

    private Object createSeasonsIntegrationProxy(Class<?> integrationInterface) {
        final var integrationState = registry.getSeasonsIntegrationState();
        if (integrationInterface == null) {
            return null;
        }
        if (supportsLifecycleCallbacks(integrationInterface)) {
            return new SeasonsIntegration(plugin).createLifecycleProxy(integrationInterface);
        }
        if (integrationState.getProfileClass() == null) {
            return null;
        }
        try {
            return new SeasonsIntegration(plugin).createProxy(integrationInterface, integrationState.getProfileClass());
        }
        catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to create EzSeasons integration proxy.", exception);
            return null;
        }
    }

    private Method resolveIntegrationMethod(Class<?> apiClass, Object integrationProxy, String... methodNames) {
        Method fallback = null;
        final Class<?> integrationClass = registry.getSeasonsIntegrationState().getIntegrationClass();
        for (String methodName : methodNames) {
            for (Method method : apiClass.getMethods()) {
                if (!methodName.equals(method.getName()) || method.getParameterCount() != 1) {
                    continue;
                }
                if (fallback == null) {
                    fallback = method;
                }
                final Class<?> parameterType = method.getParameterTypes()[0];
                if (integrationProxy != null && parameterType.isInstance(integrationProxy)) {
                    return method;
                }
                if (integrationClass != null && (parameterType.isAssignableFrom(integrationClass)
                        || integrationClass.isAssignableFrom(parameterType))) {
                    return method;
                }
            }
        }
        if (fallback != null) {
            return fallback;
        }
        throw new IllegalStateException("Unable to resolve EzSeasons integration registration methods.");
    }

    private boolean supportsLifecycleCallbacks(Class<?> integrationInterface) {
        for (Method method : integrationInterface.getMethods()) {
            if ("onRegister".equals(method.getName()) && method.getParameterCount() == 1) {
                return true;
            }
        }
        return false;
    }

    private Object findSeasonsApi() {
        if (!runtime.ensureSeasonsClasses()) {
            return null;
        }
        final Class<?> seasonsApiClass = registry.getSeasonsIntegrationState().getApiClass();
        @SuppressWarnings({"rawtypes", "unchecked"})
        final RegisteredServiceProvider<?> provider =
                Bukkit.getServicesManager().getRegistration((Class) seasonsApiClass);
        if (provider == null) {
            return null;
        }
        final Object api = provider.getProvider();
        if (api == null || !seasonsApiClass.isInstance(api)) {
            return null;
        }
        registry.getSeasonsIntegrationState().setApiInstance(api);
        return api;
    }

    private void registerSeasonResetListener() {
        final var integrationState = registry.getSeasonsIntegrationState();
        final Class<? extends Event> eventClass = loadSeasonsResetEventClass();
        if (eventClass == null) {
            return;
        }
        if (integrationState.getSeasonResetListener() != null) {
            return;
        }
        try {
            final SeasonResetListener seasonResetListener = new SeasonResetListener(pluginAccessor);
            integrationState.setSeasonResetListener(seasonResetListener);
            Bukkit.getPluginManager().registerEvent(
                    eventClass, seasonResetListener, EventPriority.NORMAL,
                    (listener, event) -> ((SeasonResetListener) listener).onSeasonReset(event),
                    plugin);
            plugin.getLogger().info("Registered EzSeasons season reset listener.");
        }
        catch (LinkageError | RuntimeException exception) {
            integrationState.setSeasonResetListener(null);
            plugin.getLogger().log(Level.WARNING, "Failed to register EzSeasons season reset listener.", exception);
        }
    }

    private void unregisterSeasonResetListener() {
        final SeasonResetListener seasonResetListener = registry.getSeasonsIntegrationState().getSeasonResetListener();
        if (seasonResetListener == null) {
            return;
        }
        HandlerList.unregisterAll(seasonResetListener);
        registry.getSeasonsIntegrationState().setSeasonResetListener(null);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Event> loadSeasonsResetEventClass() {
        final Plugin seasonsPlugin = Bukkit.getPluginManager().getPlugin(SEASONS_PLUGIN_NAME);
        if (seasonsPlugin == null) {
            return null;
        }
        final ClassLoader loader = seasonsPlugin.getClass().getClassLoader();
        try {
            return (Class<? extends Event>) Class.forName(
                    "com.skyblockexp.lifesteal.seasons.api.events.SeasonResetEvent", false, loader);
        }
        catch (ClassNotFoundException exception) {
            return null;
        }
    }
}
