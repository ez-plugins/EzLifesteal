package com.skyblockexp.ezlifesteal;

import com.skyblockexp.ezlifesteal.compat.AdapterSupport;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.detector.SmurfDetector;
import com.skyblockexp.ezlifesteal.heart.HeartRegistry;
import com.skyblockexp.ezlifesteal.runtime.Bootstrap;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.runtime.Registry;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bukkit plugin entrypoint responsible only for lifecycle handoff to bootstrap runtime services.
 */
public class EzLifestealPlugin extends JavaPlugin {

    public static NamespacedKey HEART_KEY;

    private final Registry registry = new Registry();

    @Override
    public void onEnable() {
        getLogger().info("Selected runtime adapter: " + AdapterSupport.resolveRuntimeAdapterId());
        final Bootstrap bootstrap = new Bootstrap(this, registry);
        registry.setBootstrap(bootstrap);
        bootstrap.start();
    }

    @Override
    public void onDisable() {
        final Bootstrap bootstrap = registry.getBootstrap();
        if (bootstrap != null) {
            bootstrap.stop();
            registry.setBootstrap(null);
        }
    }

    public void reloadPlugin(CommandSender initiator) {
        final Bootstrap bootstrap = registry.getBootstrap();
        if (bootstrap != null) {
            bootstrap.reload(initiator);
        }
    }

    public PluginAccessor getPluginAccessor() {
        final Bootstrap bootstrap = registry.getBootstrap();
        return bootstrap == null ? null : bootstrap.getPluginAccessor();
    }

    private DefaultPluginRuntimeServices runtime() {
        final Bootstrap bootstrap = registry.getBootstrap();
        return bootstrap == null ? null : bootstrap.getRuntimeServices();
    }

    public Registry getRegistry() {
        return registry;
    }

    public record SanitizedHeartBounds(double minHearts, double defaultHearts, double maxHearts, boolean adjusted) { }

    public static SanitizedHeartBounds sanitizeHeartBounds(double minHearts, double defaultHearts, double maxHearts) {
        double min = minHearts;
        double def = defaultHearts;
        double max = maxHearts;
        boolean adjusted = false;

        if (min < 0) {
            min = 0;
            adjusted = true;
        }
        if (max < 0) {
            max = 0;
            adjusted = true;
        }
        if (def < 0) {
            def = 0;
            adjusted = true;
        }
        if (max < min) {
            max = min;
            adjusted = true;
        }
        if (def < min) {
            def = min;
            adjusted = true;
        }
        if (def > max) {
            def = max;
            adjusted = true;
        }

        return new SanitizedHeartBounds(min, def, max, adjusted);
    }

    public SmurfDetector getSmurfDetector() {
        return runtime() == null ? null : runtime().getSmurfDetector();
    }

    public boolean addSmurfExemption(UUID uniqueId) {
        return runtime() != null
            && runtime().addSmurfExemption(uniqueId);
    }

    public boolean removeSmurfExemption(UUID uniqueId) {
        return runtime() != null
            && runtime().removeSmurfExemption(uniqueId);
    }

    public MessageService getMessageService() {
        return runtime() == null ? null : runtime().getMessageService();
    }

    public ExecutorService getStorageExecutor() {
        return runtime() == null ? null : runtime().getStorageExecutor();
    }

    public void saveHologramSettings() {
        if (runtime() != null) {
            runtime().saveHologramSettings();
        }
    }

    public ConfigurationSection getHologramSection(boolean create) {
        return runtime() == null ? null : runtime().getHologramSection(create);
    }

    public LifestealManager getLifestealManager() {
        return runtime() == null ? null : runtime().getLifestealManager();
    }

    public Optional<Economy> getEconomy() {
        return runtime() == null ? Optional.empty() : runtime().getEconomy();
    }

    public YamlConfiguration getShopConfig() {
        return runtime() == null ? null : runtime().getShopConfig();
    }

    public HeartRegistry getHeartRegistry() {
        return runtime() == null ? null : runtime().getHeartRegistry();
    }

    public void requestTopHologramUpdate() {
        if (runtime() != null) {
            runtime().requestTopHologramUpdate();
        }
    }

    public void sendHeartStatus(Player player, double hearts) {
        if (runtime() != null) {
            runtime().sendHeartStatus(player, hearts);
        }
    }

}
