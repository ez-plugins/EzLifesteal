package com.skyblockexp.ezlifesteal.listener;

import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.detector.AdminDetector;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.model.MobReward;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import com.skyblockexp.ezlifesteal.util.ActionBarHelper;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class MobListener implements Listener {

    private final PluginAccessor plugin;

    public MobListener(PluginAccessor plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!plugin.isGlobalLifestealEnabled()) {
            return;
        }
        if (event.getEntityType() == EntityType.PLAYER) {
            return;
        }
        final Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        final MobReward reward = plugin.getMobReward(event.getEntityType());
        if (reward == null) {
            return;
        }

        final String worldName = event.getEntity().getWorld().getName();
        if (!plugin.isLifestealEnabledInWorld(worldName) || !reward.isWorldAllowed(worldName)) {
            return;
        }

        final String permission = reward.getPermission();
        if (permission != null && !permission.isBlank() && !killer.hasPermission(permission)) {
            return;
        }

        final AdminDetector adminDetector = plugin.getAdminDetector();
        if (adminDetector != null && adminDetector.isAdmin(killer) && plugin.isAdminBypassHeartGain()) {
            return;
        }

        final double heartsDelta = reward.getHearts();
        if (heartsDelta == 0.0d) {
            return;
        }

        final LifestealManager manager = plugin.getLifestealManager();
        final LifestealProfile profile = manager.getOrCreateProfile(killer.getUniqueId());
        if (heartsDelta > 0) {
            profile.addHearts(heartsDelta, manager.getMaxHearts());
        }
        else {
            profile.removeHearts(-heartsDelta, manager.getMinHearts());
        }

        manager.saveProfileAsync(profile).whenComplete((unused, throwable) -> {
            if (throwable != null) {
                plugin.getLogger()
                        .severe("Failed to save mob reward profile for " + killer.getName()
                                + ": " + throwable.getMessage());
            }
        });

        final Map<String, String> placeholders = createPlaceholders(killer, event.getEntityType(), worldName,
                heartsDelta, profile.getHearts());
        SchedulerAdapter.run(plugin.getPlugin(), () -> {
            if (!killer.isOnline()) {
                return;
            }
            manager.applyHearts(killer, profile);
            sendFeedback(killer, placeholders);
            plugin.requestTopHologramUpdate();
        });
    }

    private void sendFeedback(Player player, Map<String, String> placeholders) {
        final MessageService messageService = plugin.getMessageService();
        if (messageService == null) {
            return;
        }
        messageService.sendMessage(player, "mob-reward-chat", placeholders);
        final String actionBarMessage = messageService.render("mob-reward-actionbar", placeholders);
        if (!actionBarMessage.isEmpty()) {
            ActionBarHelper.sendActionBar(player, actionBarMessage);
        }
    }

    private Map<String, String> createPlaceholders(Player killer,
                                                   EntityType entityType,
                                                   String worldName,
                                                   double heartsDelta,
                                                   double newTotal) {
        final Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", killer.getName());
        placeholders.put("killer", killer.getName());
        placeholders.put("world", worldName);
        placeholders.put("entity", formatEntityName(entityType));
        placeholders.put("delta", formatSignedHearts(heartsDelta));
        placeholders.put("change", formatSignedHearts(heartsDelta));
        placeholders.put("delta_abs", formatHearts(Math.abs(heartsDelta)));
        placeholders.put("hearts", formatHearts(newTotal));
        placeholders.put("total", formatHearts(newTotal));
        return placeholders;
    }

    private String formatEntityName(EntityType entityType) {
        final String name = entityType.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        final String[] parts = name.split(" ");
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) {
                builder.append(parts[i].substring(1));
            }
        }
        return builder.toString();
    }

    private String formatHearts(double value) {
        return value % 1 == 0 ? Integer.toString((int) value) : String.format(Locale.US, "%.2f", value);
    }

    private String formatSignedHearts(double value) {
        final String formatted = formatHearts(Math.abs(value));
        return value >= 0 ? "+" + formatted : "-" + formatted;
    }
}
