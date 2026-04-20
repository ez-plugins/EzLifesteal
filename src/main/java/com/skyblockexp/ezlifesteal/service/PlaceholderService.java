package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.placeholder.PlaceholderHook;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import java.lang.reflect.Constructor;
import java.util.logging.Level;

public class PlaceholderService {
    private static final class IncompatiblePlaceholderExpansionException extends ReflectiveOperationException {
        private IncompatiblePlaceholderExpansionException() {
            super("Incompatible PlaceholderAPI expansion type.");
        }
    }

    @FunctionalInterface
    interface PlaceholderExpansionFactory {
        PlaceholderHook create(PluginAccessor pluginAccessor)
                throws ClassNotFoundException, NoClassDefFoundError, ReflectiveOperationException;
    }

    private final DefaultPluginRuntimeServices runtime;

    private final PlaceholderExpansionFactory expansionFactory;


    public PlaceholderService(DefaultPluginRuntimeServices runtime,
            com.skyblockexp.ezlifesteal.EzLifestealPlugin plugin) {
        this(runtime, plugin, PlaceholderService::createPlaceholderHook);
    }

    PlaceholderService(DefaultPluginRuntimeServices runtime,
                       com.skyblockexp.ezlifesteal.EzLifestealPlugin plugin,
                       PlaceholderExpansionFactory expansionFactory) {
        this.runtime = runtime;
        this.expansionFactory = expansionFactory;
    }

    public void setupPlaceholderExpansion(PluginAccessor pluginAccessor) {
        if (runtime.getPlaceholderExpansion() != null) {
            runtime.getPlaceholderExpansion().unregisterExpansion();
            runtime.setPlaceholderExpansion(null);
        }
        if (!org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }
        try {
            final PlaceholderHook expansion = expansionFactory.create(pluginAccessor);
            if (expansion.register()) {
                runtime.setPlaceholderExpansion(expansion);
                runtime.getRegistry().getManagerState().setPlaceholderExpansion(expansion);
                runtime.getLogger().info("Registered PlaceholderAPI placeholders.");
            }
            else {
                runtime.getLogger().warning("Failed to register PlaceholderAPI placeholders.");
            }
        }
        catch (ClassNotFoundException ignored) {
            runtime.getLogger().warning("PlaceholderAPI support is not available.");
        }
        catch (NoClassDefFoundError error) {
            runtime.getLogger().warning("PlaceholderAPI was not found; placeholders will remain disabled.");
        }
        catch (IncompatiblePlaceholderExpansionException exception) {
            runtime.getLogger().warning("PlaceholderAPI support is not compatible with this version of EzLifesteal.");
        }
        catch (ReflectiveOperationException | RuntimeException exception) {
            runtime.getLogger().log(Level.WARNING, "Failed to register PlaceholderAPI placeholders.", exception);
        }
    }

    private static PlaceholderHook createPlaceholderHook(PluginAccessor pluginAccessor)
            throws ClassNotFoundException, ReflectiveOperationException {
        final Class<?> expansionClass =
                Class.forName("com.skyblockexp.ezlifesteal.placeholder.LifestealPlaceholderExpansion");
        return createPlaceholderHookFromClass(expansionClass, pluginAccessor);
    }

    static PlaceholderHook createPlaceholderHookFromClass(Class<?> expansionClass, PluginAccessor pluginAccessor)
            throws ReflectiveOperationException {
        if (!PlaceholderHook.class.isAssignableFrom(expansionClass)) {
            throw new IncompatiblePlaceholderExpansionException();
        }
        @SuppressWarnings("unchecked")
        final Class<? extends PlaceholderHook> hookType = (Class<? extends PlaceholderHook>) expansionClass;
        final Constructor<? extends PlaceholderHook> constructor =
                hookType.getDeclaredConstructor(PluginAccessor.class);
        return constructor.newInstance(pluginAccessor);
    }
}
